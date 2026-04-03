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
import com.atu.tools.stringsync.util.SupportedLanguages
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
    private val modulePanel = ModuleSelectionPanel(
        onAutoDetect = { detectModules() },
        onSelectionChanged = { selected ->
            settings.state.lastSelectedModules = selected.joinToString(",")
        }
    )
    private val languagePanel = LanguageSelectionPanel(
        onDetectExisting = { detectExistingLocales() },
        onSelectFromSheet = { selectFromSheetLocales() }
    )
    private val keysPanel = KeysInputPanel()

    private val root = JPanel(BorderLayout())

    init {
        title = "Đồng bộ String từ Google Sheet"
        keysPanel.setText(settings.state.lastKeys)
        runCatching {
            modeCombo.selectedItem = SyncMode.valueOf(settings.state.lastMode)
        }

        root.preferredSize = Dimension(1200, 760)
        root.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val top = JPanel(GridLayout(2, 1, 8, 8))
        val sourcePanel = JPanel()
        sourcePanel.border = BorderFactory.createTitledBorder("Nguồn dữ liệu từ Sheet")
        urlField.columns = 70

        val testButton = JButton("Kiểm tra kết nối")
        val loadButton = JButton("Tải ngôn ngữ")

        testButton.addActionListener { loadPayload(testOnly = true) }
        loadButton.addActionListener { loadPayload(testOnly = false) }

        sourcePanel.add(JBLabel("URL Google Apps Script:"))
        sourcePanel.add(urlField)
        sourcePanel.add(testButton)
        sourcePanel.add(loadButton)

        val modePanel = JPanel()
        modePanel.border = BorderFactory.createTitledBorder("Chế độ đồng bộ")
        modePanel.add(JBLabel("Chế độ:"))
        modePanel.add(modeCombo)

        top.add(sourcePanel)
        top.add(modePanel)

        val center = JPanel(GridLayout(1, 3, 8, 8))
        val wrappedModules = JPanel(BorderLayout())
        wrappedModules.border = BorderFactory.createTitledBorder("Module nguồn")
        wrappedModules.add(modulePanel, BorderLayout.CENTER)

        val wrappedLanguages = JPanel(BorderLayout())
        wrappedLanguages.border = BorderFactory.createTitledBorder("Ngôn ngữ đích")
        wrappedLanguages.add(languagePanel, BorderLayout.CENTER)

        val wrappedKeys = JPanel(BorderLayout())
        wrappedKeys.border = BorderFactory.createTitledBorder("Danh sách key")
        wrappedKeys.add(keysPanel, BorderLayout.CENTER)

        center.add(wrappedModules)
        center.add(wrappedLanguages)
        center.add(wrappedKeys)

        root.add(top, BorderLayout.NORTH)
        root.add(center, BorderLayout.CENTER)

        init()
        languagePanel.setLanguages(SupportedLanguages.all)
        detectModules()
        if (settings.state.lastSelectedLocales.isNotBlank()) {
            languagePanel.setSelectedLocales(settings.state.lastSelectedLocales.split(',').map { it.trim() }.toSet())
        } else {
            detectExistingLocales()
        }
    }

    override fun createCenterPanel(): JComponent = root

    override fun createActions(): Array<javax.swing.Action> {
        val previewAction = object : DialogWrapperAction("Xem trước thay đổi") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                runSync(previewOnly = true)
            }
        }
        val applyAction = object : DialogWrapperAction("Áp dụng") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                runSync(previewOnly = false)
            }
        }
        return arrayOf(previewAction, applyAction, cancelAction)
    }

    private fun detectModules() {
        val modules = scanner.scan(project)
        modulePanel.setModules(modules)
        val remembered = settings.state.lastSelectedModules
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        if (remembered.isNotEmpty()) {
            modulePanel.setSelectedModuleNames(remembered)
        }
    }

    private fun detectExistingLocales() {
        languagePanel.setSelectedLocales(SupportedLanguages.defaultSelectedCodes(modulePanel.existingLocalesOfSelection()))
    }

    private fun selectFromSheetLocales() {
        val localPayload = payload
        if (localPayload == null) {
            setErrorText("Vui lòng tải ngôn ngữ từ sheet trước")
            return
        }
        val selected = SupportedLanguages.selectedCodesFromSheetLocales(localPayload.locales())
        languagePanel.setSelectedLocales(selected)
        NotificationUtils.info(project, "Đồng bộ String", "Đã chọn ${selected.size} ngôn ngữ theo dữ liệu sheet.")
    }

    private fun loadPayload(testOnly: Boolean) {
        val url = urlField.text.trim()
        if (url.isBlank()) {
            setErrorText("Vui lòng nhập URL")
            return
        }
        if (!url.endsWith("/exec")) {
            setErrorText("URL phải kết thúc bằng /exec")
            return
        }

        setErrorText(null)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Đang tải dữ liệu từ sheet", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val loaded = sheetApiClient.fetch(url)
                    ApplicationManager.getApplication().invokeLater {
                        payload = loaded
                        settings.state.lastUrl = url
                        if (!testOnly) detectExistingLocales()
                        NotificationUtils.info(project, "Đồng bộ String", "Kết nối thành công. Đã tải ${loaded.locales().size} ngôn ngữ.")
                    }
                } catch (t: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        setErrorText("Không tải được sheet: ${t.message}")
                        NotificationUtils.error(project, "Đồng bộ String", "Không tải được sheet: ${t.message}")
                    }
                }
            }
        })
    }

    private fun runSync(previewOnly: Boolean) {
        val localPayload = payload
        if (localPayload == null) {
            setErrorText("Vui lòng tải dữ liệu sheet trước")
            return
        }

        val modules = modulePanel.selectedModules()
        if (modules.isEmpty()) {
            setErrorText("Vui lòng chọn ít nhất 1 module")
            return
        }

        val locales = languagePanel.selectedLocales()
        if (locales.isEmpty()) {
            setErrorText("Vui lòng chọn ít nhất 1 ngôn ngữ đích")
            return
        }

        val keys = KeyParser.parse(keysPanel.text())
        val selectedMode = modeCombo.selectedItem as? SyncMode ?: SyncMode.UPDATE_ALL

        val request = SyncRequest(
            sourceUrl = urlField.text.trim(),
            modules = modules,
            targetLocales = locales,
            keys = keys,
            mode = selectedMode,
            payload = localPayload
        )

        settings.state.lastMode = (modeCombo.selectedItem as? SyncMode ?: SyncMode.UPDATE_ALL).name
        settings.state.lastKeys = keysPanel.text()
        settings.state.lastSelectedLocales = locales.joinToString(",")

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Đang đồng bộ string", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val previewResult = syncService.preview(request)

                    ApplicationManager.getApplication().invokeLater {
                        if (previewOnly) {
                            PreviewChangesDialog(previewResult.changes).show()
                            return@invokeLater
                        }

                        val confirm = PreviewChangesDialog(previewResult.changes).showAndGet()
                        if (!confirm) return@invokeLater

                        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Đang áp dụng thay đổi", true) {
                            override fun run(indicator: ProgressIndicator) {
                                try {
                                    var applied: SyncResult? = null
                                    WriteCommandAction.runWriteCommandAction(project) {
                                        applied = syncService.apply(request)
                                    }
                                    val result = applied ?: previewResult
                                    ApplicationManager.getApplication().invokeLater {
                                        val summary = "File thay đổi: ${result.filesChanged}, Thêm mới: ${result.keysAdded}, Cập nhật: ${result.keysUpdated}, Bỏ qua: ${result.skipped}, Lỗi: ${result.errors.size}"
                                        NotificationUtils.info(project, "Áp dụng đồng bộ thành công", summary)
                                    }
                                } catch (t: Throwable) {
                                    ApplicationManager.getApplication().invokeLater {
                                        setErrorText("Đồng bộ thất bại: ${t.message}")
                                        NotificationUtils.error(project, "Đồng bộ String", "Đồng bộ thất bại: ${t.message}")
                                    }
                                }
                            }
                        })
                    }
                } catch (t: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        setErrorText("Đồng bộ thất bại: ${t.message}")
                        NotificationUtils.error(project, "Đồng bộ String", "Đồng bộ thất bại: ${t.message}")
                    }
                }
            }
        })
    }
}
