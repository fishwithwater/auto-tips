package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsConfiguration
import cn.myjdemo.autotips.model.TipStyle
import cn.myjdemo.autotips.service.ConfigurationService
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic

/**
 * 配置更改监听器接口
 */
interface ConfigurationChangeListener {
    fun onConfigurationChanged(newConfiguration: TipsConfiguration)
}

/**
 * 配置管理服务实现类
 * 负责管理插件配置和用户偏好设置的持久化存储
 */
@State(
    name = "AutoTipsConfiguration",
    storages = [Storage("autoTipsSettings.xml")]
)
class ConfigurationServiceImpl : ConfigurationService, PersistentStateComponent<TipsConfiguration> {
    
    companion object {
        private val LOG = Logger.getInstance(ConfigurationServiceImpl::class.java)
        val CONFIGURATION_CHANGED_TOPIC = Topic.create("AutoTips Configuration Changed", ConfigurationChangeListener::class.java)
        
        fun getInstance(): ConfigurationService = service()
    }
    
    private var configuration = TipsConfiguration()
    
    override fun getState(): TipsConfiguration = configuration
    
    override fun loadState(state: TipsConfiguration) {
        configuration = state
        notifyConfigurationChanged()
    }
    
    override fun isPluginEnabled(): Boolean = configuration.enabled
    
    override fun getTipDisplayDuration(): Int = configuration.displayDuration
    
    override fun getTipStyle(): TipStyle = configuration.style
    
    override fun getCustomAnnotationPatterns(): List<String> = configuration.customPatterns
    
    override fun saveConfiguration(config: TipsConfiguration) {
        configuration = config
        notifyConfigurationChanged()
    }
    
    override fun getCurrentConfiguration(): TipsConfiguration = configuration
    
    override fun resetToDefaults() {
        configuration = TipsConfiguration()
        notifyConfigurationChanged()
    }
    
    override fun setPluginEnabled(enabled: Boolean) {
        LOG.info("Setting plugin enabled: $enabled")
        configuration = configuration.copy(enabled = enabled)
        notifyConfigurationChanged()
    }
    
    override fun setTipDisplayDuration(duration: Int) {
        LOG.info("Setting tip display duration: $duration")
        configuration = configuration.copy(displayDuration = duration)
        notifyConfigurationChanged()
    }
    
    override fun setTipStyle(style: TipStyle) {
        LOG.info("Setting tip style: $style")
        configuration = configuration.copy(style = style)
        notifyConfigurationChanged()
    }
    
    override fun addCustomAnnotationPattern(pattern: String) {
        val patterns = configuration.customPatterns.toMutableList()
        if (!patterns.contains(pattern)) {
            patterns.add(pattern)
            configuration = configuration.copy(customPatterns = patterns)
            notifyConfigurationChanged()
        }
    }
    
    override fun removeCustomAnnotationPattern(pattern: String) {
        val patterns = configuration.customPatterns.toMutableList()
        if (patterns.remove(pattern)) {
            configuration = configuration.copy(customPatterns = patterns)
            notifyConfigurationChanged()
        }
    }
    
    override fun isJavadocModeEnabled(): Boolean = configuration.javadocModeEnabled
    
    override fun setJavadocModeEnabled(enabled: Boolean) {
        LOG.info("Setting javadoc mode enabled: $enabled")
        configuration = configuration.copy(javadocModeEnabled = enabled)
        notifyConfigurationChanged()
    }
    
    /**
     * 通知配置更改
     */
    private fun notifyConfigurationChanged() {
        try {
            LOG.info("Notifying configuration changed: enabled=${configuration.enabled}, style=${configuration.style}, javadocMode=${configuration.javadocModeEnabled}")
            val messageBus = com.intellij.openapi.application.ApplicationManager.getApplication().messageBus
            val publisher = messageBus.syncPublisher(CONFIGURATION_CHANGED_TOPIC)
            publisher.onConfigurationChanged(configuration)
            LOG.info("Configuration change notification sent successfully")
        } catch (e: Exception) {
            // 在测试环境中可能没有MessageBus，忽略异常
            LOG.warn("Failed to send configuration change notification", e)
        }
    }
}