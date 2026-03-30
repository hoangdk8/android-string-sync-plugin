package com.atu.tools.stringsync.ui

import com.atu.tools.stringsync.model.SheetPayload
import com.atu.tools.stringsync.model.SyncResult
import com.atu.tools.stringsync.model.SyncMode
import com.atu.tools.stringsync.model.SyncRequest
import com.atu.tools.stringsync.services.AndroidModuleScanner
import com.atu.tools.stringsync.services.SheetApiClient
import com.atu.tools.stringsync.services.StringSyncService
import com.atu.tools.stringsync.services.StringSyncSettingsService
import com.atu.tools.stringsync.util.KeyParser
import com.atu.tools.stringsync.util.NotificationUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class StringSyncDialog(private val project: Project) : DialogWrapper(project) {

    private val settings = project.service<StringSyncSettingsService>()
    private val scanner = AndroidModuleScanner()
    private val sheetApiClient = SheetApiClient()
    private val syncService = StringSyncService()

    private var payload: SheetPayload? = null

    private val urlField = JBTextField(settings.state.lastUrl)
    private val modeModel = DefaultComboBoxModel(SyncMode.entries.toTypedArray())
    private val modeCombo = javax.swing.JComboBox(modeModel)
    private val modulePanel = ModuleSelectionPanel { detectModules() }
    private val languagePanel = LanguageSelectionPanel { detectExistingLocales() }
    private val keysPanel = KeysInputPanel()

    private val root = JPanel(BorderLayout())

    init {
        title = "String Sync from Google Sheet"
        keysPanel.setText(settings.state.lastKeys)
        runCatching {
            modeCombo.selectedItem = SyncMode.valueOf(settings.state.lastMode)
        }

        root.preferredSize = Dimension(1200, 760)
        root.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val top = JPanel(GridLayout(2, 1, 8, 8))
        val sourcePanel = JPanel()
        sourcePanel.border = BorderFactory.createTitledBorder("Sheet Source")
        urlField.columns = 70

        val testButton = JButton("Test Connection")
        val loadButton = JButton("Load Languages")

        testButton.addActionListener { loadPayload(testOnly = true) }
        loadButton.addActionListener { loadPayload(testOnly = false) }

        sourcePanel.add(JBLabel("Google Apps Script URL:"))
        sourcePanel.add(urlField)
        sourcePanel.add(testButton)
        sourcePanel.add(loadButton)

        val modePanel = JPanel()
        modePanel.border = BorderFactory.createTitledBorder("Sync Mode")
        modePanel.add(JBLabel("Mode:"))
        modePanel.add(modeCombo)

        top.add(sourcePanel)
        top.add(modePanel)

        val center = JPanel(GridLayout(1, 3, 8, 8))
        val wrappedModules = JPanel(BorderLayout())
        wrappedModules.border = BorderFactory.createTitledBorder("Source Modules")
        wrappedModules.add(modulePanel, BorderLayout.CENTER)

        val wrappedLanguages = JPanel(BorderLayout())
        wrappedLanguages.border = BorderFactory.createTitledBorder("Language Selection")
        wrappedLanguages.add(languagePanel, BorderLayout.CENTER)

        val wrappedKeys = JPanel(BorderLayout())
        wrappedKeys.border = BorderFactory.createTitledBorder("Keys Input")
        wrappedKeys.add(keysPanel, BorderLayout.CENTER)

        center.add(wrappedModules)
        center.add(wrappedLanguages)
        center.add(wrappedKeys)

        root.add(top, BorderLayout.NORTH)
        root.add(center, BorderLayout.CENTER)

        init()
        detectModules()
        if (settings.state.lastSelectedLocales.isNotBlank()) {
            languagePanel.setSelectedLocales(settings.state.lastSelectedLocales.split(',').map { it.trim() }.toSet())
        }
    }

    override fun createCenterPanel(): JComponent = root

    override fun createActions(): Array<javax.swing.Action> {
        val previewAction = object : DialogWrapperAction("Preview Changes") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                runSync(previewOnly = true)
            }
        }
        val applyAction = object : DialogWrapperAction("Apply") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                runSync(previewOnly = false)
            }
        }
        return arrayOf(previewAction, applyAction, cancelAction)
    }

    private fun detectModules() {
        val modules = scanner.scan(project)
        modulePanel.setModules(modules)
    }

    private fun detectExistingLocales() {
        languagePanel.setSelectedLocales(modulePanel.existingLocalesOfSelection())
    }

    private fun loadPayload(testOnly: Boolean) {
        val url = urlField.text.trim()
        if (url.isBlank()) {
            setErrorText("URL is required")
            return
        }
        if (!url.endsWith("/exec")) {
            setErrorText("URL must end with /exec")
            return
        }

        setErrorText(null)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading sheet payload", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val loaded = sheetApiClient.fetch(url)
                    ApplicationManager.getApplication().invokeLater {
                        payload = loaded
                        settings.state.lastUrl = url
                        if (!testOnly) {
                            languagePanel.setLanguages(loaded.locales().toList())
                            detectExistingLocales()
                        }
                        NotificationUtils.info(project, "String Sync", "Connection OK. Loaded ${loaded.locales().size} locales")
                    }
                } catch (t: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        setErrorText("Failed to load sheet: ${t.message}")
                        NotificationUtils.error(project, "String Sync", "Failed to load sheet: ${t.message}")
                    }
                }
            }
        })
    }

    private fun runSync(previewOnly: Boolean) {
        val localPayload = payload
        if (localPayload == null) {
            setErrorText("Please load sheet data first")
            return
        }

        val modules = modulePanel.selectedModules()
        if (modules.isEmpty()) {
            setErrorText("Please select at least one module")
            return
        }

        val locales = languagePanel.selectedLocales()
        if (locales.isEmpty()) {
            setErrorText("Please select at least one target language")
            return
        }

        val keys = KeyParser.parse(keysPanel.text())
        val selectedMode = if (previewOnly) SyncMode.PREVIEW_ONLY else (modeCombo.selectedItem as? SyncMode ?: SyncMode.ADD_OR_UPDATE)

        val request = SyncRequest(
            sourceUrl = urlField.text.trim(),
            modules = modules,
            targetLocales = locales,
            keys = keys,
            mode = selectedMode,
            payload = localPayload
        )

        settings.state.lastMode = (modeCombo.selectedItem as? SyncMode ?: SyncMode.ADD_OR_UPDATE).name
        settings.state.lastKeys = keysPanel.text()
        settings.state.lastSelectedLocales = locales.joinToString(",")

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Running String Sync", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val result = if (previewOnly) {
                        syncService.preview(request)
                    } else {
                        var output: SyncResult? = null
                        WriteCommandAction.runWriteCommandAction(project) {
                            output = syncService.apply(request)
                        }
                        output ?: syncService.preview(request)
                    }

                    ApplicationManager.getApplication().invokeLater {
                        PreviewChangesDialog(result.changes).show()
                        val summary = "Files: ${result.filesChanged}, Added: ${result.keysAdded}, Updated: ${result.keysUpdated}, Skipped: ${result.skipped}, Errors: ${result.errors.size}"
                        if (!previewOnly) {
                            NotificationUtils.info(project, "String Sync applied", summary)
                        }
                    }
                } catch (t: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        setErrorText("Sync failed: ${t.message}")
                        NotificationUtils.error(project, "String Sync", "Sync failed: ${t.message}")
                    }
                }
            }
        })
    }
}
