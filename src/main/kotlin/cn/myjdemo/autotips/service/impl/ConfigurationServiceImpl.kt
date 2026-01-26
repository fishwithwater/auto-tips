package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsConfiguration
import cn.myjdemo.autotips.model.TipStyle
import cn.myjdemo.autotips.service.ConfigurationService
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/**
 * 配置管理服务实现类
 * 负责管理插件配置和用户偏好设置的持久化存储
 */
@State(
    name = "AutoTipsConfiguration",
    storages = [Storage("autoTipsSettings.xml")]
)
class ConfigurationServiceImpl : ConfigurationService, PersistentStateComponent<TipsConfiguration> {
    
    private var configuration = TipsConfiguration()
    
    override fun getState(): TipsConfiguration = configuration
    
    override fun loadState(state: TipsConfiguration) {
        configuration = state
    }
    
    override fun isPluginEnabled(): Boolean = configuration.enabled
    
    override fun getTipDisplayDuration(): Int = configuration.displayDuration
    
    override fun getTipStyle(): TipStyle = configuration.style
    
    override fun getCustomAnnotationPatterns(): List<String> = configuration.customPatterns
    
    override fun saveConfiguration(config: TipsConfiguration) {
        configuration = config
    }
    
    override fun getCurrentConfiguration(): TipsConfiguration = configuration
    
    override fun resetToDefaults() {
        configuration = TipsConfiguration()
    }
    
    override fun setPluginEnabled(enabled: Boolean) {
        configuration = configuration.copy(enabled = enabled)
    }
    
    override fun setTipDisplayDuration(duration: Int) {
        configuration = configuration.copy(displayDuration = duration)
    }
    
    override fun setTipStyle(style: TipStyle) {
        configuration = configuration.copy(style = style)
    }
    
    override fun addCustomAnnotationPattern(pattern: String) {
        val patterns = configuration.customPatterns.toMutableList()
        if (!patterns.contains(pattern)) {
            patterns.add(pattern)
            configuration = configuration.copy(customPatterns = patterns)
        }
    }
    
    override fun removeCustomAnnotationPattern(pattern: String) {
        val patterns = configuration.customPatterns.toMutableList()
        if (patterns.remove(pattern)) {
            configuration = configuration.copy(customPatterns = patterns)
        }
    }
    
    companion object {
        fun getInstance(): ConfigurationService = service()
    }
}