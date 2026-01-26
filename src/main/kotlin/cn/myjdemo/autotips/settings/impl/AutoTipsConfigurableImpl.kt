package cn.myjdemo.autotips.settings.impl

import cn.myjdemo.autotips.model.TipStyle
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.settings.AutoTipsConfigurable
import com.intellij.openapi.components.service
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.*

/**
 * Auto-Tips插件设置配置界面实现类
 * 实现IntelliJ Platform的Configurable接口
 */
class AutoTipsConfigurableImpl : AutoTipsConfigurable {
    
    private var mainPanel: JPanel? = null
    private var enabledCheckBox: JBCheckBox? = null
    private var displayDurationField: JBTextField? = null
    private var styleComboBox: JComboBox<TipStyle>? = null
    
    private val configService = service<ConfigurationService>()
    
    override fun getDisplayName(): String = "Auto-Tips"
    
    override fun getHelpTopic(): String? = null
    
    override fun createComponent(): JComponent? {
        if (mainPanel == null) {
            createUI()
        }
        return mainPanel
    }
    
    override fun isModified(): Boolean {
        val currentConfig = configService.getCurrentConfiguration()
        
        return enabledCheckBox?.isSelected != currentConfig.enabled ||
                displayDurationField?.text?.toIntOrNull() != currentConfig.displayDuration ||
                styleComboBox?.selectedItem != currentConfig.style
    }
    
    override fun apply() {
        val enabled = enabledCheckBox?.isSelected ?: true
        val duration = displayDurationField?.text?.toIntOrNull() ?: 5000
        val style = styleComboBox?.selectedItem as? TipStyle ?: TipStyle.BALLOON
        
        configService.setPluginEnabled(enabled)
        configService.setTipDisplayDuration(duration)
        configService.setTipStyle(style)
    }
    
    override fun reset() {
        val currentConfig = configService.getCurrentConfiguration()
        
        enabledCheckBox?.isSelected = currentConfig.enabled
        displayDurationField?.text = currentConfig.displayDuration.toString()
        styleComboBox?.selectedItem = currentConfig.style
    }
    
    override fun disposeUIResources() {
        mainPanel = null
        enabledCheckBox = null
        displayDurationField = null
        styleComboBox = null
    }
    
    /**
     * 创建设置界面UI
     */
    private fun createUI() {
        enabledCheckBox = JBCheckBox("启用 Auto-Tips 插件")
        displayDurationField = JBTextField()
        styleComboBox = JComboBox(TipStyle.values())
        
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox!!)
            .addLabeledComponent("提示显示时长 (毫秒):", displayDurationField!!)
            .addLabeledComponent("提示样式:", styleComboBox!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        // 加载当前配置
        reset()
    }
}