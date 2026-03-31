package com.atu.tools.stringsync.ui

import com.atu.tools.stringsync.model.SheetPayload
import com.atu.tools.stringsync.model.SyncMode
import com.atu.tools.stringsync.model.SyncRequest
import com.atu.tools.stringsync.model.SyncResult
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
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JPanel

class StringSyncToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val settings = project.service<StringSyncSettingsService>()
    private val scanner = AndroidModuleScanner()
    private val sheetApiClient = SheetApiClient()
    private val syncService = StringSyncService()

    private var payload: SheetPayload? = null

    private val urlField = JBTextField(settings.state.lastUrl)
    private val modeModel = DefaultComboBoxModel(SyncMode.entries.toTypedArray())
    private val modeCombo = javax.swing.JComboBox(modeModel)
    private val modulePanel = ModuleSelectionPanel { detectModules() }
    private val languagePanel = LanguageSelectionPanel(
        onDetectExisting = { detectExistingLocales() },
        onSelectFromSheet = { selectFromSheetLocales() }
    )
    private val keysPanel = KeysInputPanel()
    private val errorLabel = JBLabel("")

    init {
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        keysPanel.setText(settings.state.lastKeys)
        runCatching {
            modeCombo.selectedItem = SyncMode.valueOf(settings.state.lastMode)
        }

        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)

        val sourcePanel = JPanel()
        sourcePanel.border = BorderFactory.createTitledBorder("Nguồn dữ liệu từ Sheet")
        sourcePanel.layout = BoxLayout(sourcePanel, BoxLayout.Y_AXIS)

        val urlRow = JPanel(BorderLayout(6, 0))
        urlRow.add(JBLabel("URL Google Apps Script"), BorderLayout.WEST)
        urlRow.add(urlField, BorderLayout.CENTER)

        val sourceButtons = JPanel()
        val loadButton = JButton("Tải ngôn ngữ")
        loadButton.addActionListener { loadPayload() }
        sourceButtons.add(loadButton)

        sourcePanel.add(urlRow)
        sourcePanel.add(Box.createVerticalStrut(6))
        sourcePanel.add(sourceButtons)

        val modePanel = JPanel()
        modePanel.border = BorderFactory.createTitledBorder("Chế độ đồng bộ")
        modePanel.layout = BorderLayout(6, 0)
        modePanel.add(JBLabel("Chế độ"), BorderLayout.WEST)
        modePanel.add(modeCombo, BorderLayout.CENTER)

        val center = JPanel(GridLayout(3, 1, 0, 8))
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

        val bottom = JPanel(BorderLayout())
        val actions = JPanel()
        val applyButton = JButton("Áp dụng")
        applyButton.addActionListener { runSync() }
        actions.add(applyButton)

        errorLabel.foreground = JBColor.RED
        bottom.add(errorLabel, BorderLayout.NORTH)
        bottom.add(actions, BorderLayout.SOUTH)

        content.add(sourcePanel)
        content.add(Box.createVerticalStrut(8))
        content.add(modePanel)
        content.add(Box.createVerticalStrut(8))
        content.add(center)

        add(content, BorderLayout.CENTER)
        add(bottom, BorderLayout.SOUTH)

        languagePanel.setLanguages(SupportedLanguages.all)
        detectModules()
        if (settings.state.lastSelectedLocales.isNotBlank()) {
            languagePanel.setSelectedLocales(settings.state.lastSelectedLocales.split(',').map { it.trim() }.toSet())
        } else {
            detectExistingLocales()
        }
    }

    private fun showError(message: String?) {
        errorLabel.text = message ?: ""
    }

    private fun detectModules() {
        val modules = scanner.scan(project)
        modulePanel.setModules(modules)
    }

    private fun detectExistingLocales() {
        val existing = modulePanel.existingLocalesOfSelection()
        languagePanel.setSelectedLocales(SupportedLanguages.defaultSelectedCodes(existing))
    }

    private fun selectFromSheetLocales() {
        val localPayload = payload
        if (localPayload == null) {
            showError("Vui lòng tải ngôn ngữ từ sheet trước")
            return
        }
        val selected = SupportedLanguages.selectedCodesFromSheetLocales(localPayload.locales())
        languagePanel.setSelectedLocales(selected)
        NotificationUtils.info(project, "Đồng bộ String", "Đã chọn ${selected.size} ngôn ngữ theo dữ liệu sheet.")
    }

    private fun loadPayload() {
        val url = urlField.text.trim()
        if (url.isBlank()) {
            showError("Vui lòng nhập URL")
            return
        }
        if (!url.endsWith("/exec")) {
            showError("URL phải kết thúc bằng /exec")
            return
        }

        showError(null)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Đang tải dữ liệu từ sheet", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val loaded = sheetApiClient.fetch(url)
                    ApplicationManager.getApplication().invokeLater {
                        payload = loaded
                        settings.state.lastUrl = url
                        detectExistingLocales()
                        NotificationUtils.info(project, "Đồng bộ String", "Kết nối thành công. Đã tải ${loaded.locales().size} ngôn ngữ.")
                    }
                } catch (t: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        showError("Không tải được sheet: ${t.message}")
                        NotificationUtils.error(project, "Đồng bộ String", "Không tải được sheet: ${t.message}")
                    }
                }
            }
        })
    }

    private fun runSync() {
        val localPayload = payload
        if (localPayload == null) {
            showError("Vui lòng tải dữ liệu sheet trước")
            return
        }

        val modules = modulePanel.selectedModules()
        if (modules.isEmpty()) {
            showError("Vui lòng chọn ít nhất 1 module")
            return
        }

        val locales = languagePanel.selectedLocales()
        if (locales.isEmpty()) {
            showError("Vui lòng chọn ít nhất 1 ngôn ngữ đích")
            return
        }

        showError(null)
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
                                        showError("Đồng bộ thất bại: ${t.message}")
                                        NotificationUtils.error(project, "Đồng bộ String", "Đồng bộ thất bại: ${t.message}")
                                    }
                                }
                            }
                        })
                    }
                } catch (t: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        showError("Đồng bộ thất bại: ${t.message}")
                        NotificationUtils.error(project, "Đồng bộ String", "Đồng bộ thất bại: ${t.message}")
                    }
                }
            }
        })
    }
}
