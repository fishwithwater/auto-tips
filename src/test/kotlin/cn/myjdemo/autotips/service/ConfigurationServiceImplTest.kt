package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.model.TipStyle
import cn.myjdemo.autotips.model.TipsConfiguration
import com.intellij.openapi.components.service

/**
 * ConfigurationService单元测试
 * 
 * 测试配置管理服务的基本功能，包括：
 * - 启用/禁用插件
 * - 修改显示时长
 * - 选择提示样式
 * - 自定义注释模式
 * - 重置配置
 * 
 * **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5**
 */
class ConfigurationServiceImplTest : TestBase() {
    
    private lateinit var configService: ConfigurationService
    
    override fun setUp() {
        super.setUp()
        configService = service<ConfigurationService>()
        // 重置到默认状态
        configService.resetToDefaults()
    }
    
    /**
     * 测试默认配置
     * **Validates: Requirement 6.1**
     */
    fun testDefaultConfiguration() {
        val config = configService.getCurrentConfiguration()
        
        assertTrue("插件应该默认启用", config.enabled)
        assertEquals("默认显示时长应该是5000毫秒", 5000, config.displayDuration)
        assertEquals("默认样式应该是BALLOON", TipStyle.BALLOON, config.style)
        assertTrue("默认自定义模式列表应该为空", config.customPatterns.isEmpty())
        assertFalse("Javadoc模式应该默认禁用", config.javadocModeEnabled)
    }
    
    /**
     * 测试启用/禁用插件
     * **Validates: Requirement 6.1**
     */
    fun testEnableDisablePlugin() {
        // 测试禁用
        configService.setPluginEnabled(false)
        assertFalse("插件应该被禁用", configService.isPluginEnabled())
        
        // 测试启用
        configService.setPluginEnabled(true)
        assertTrue("插件应该被启用", configService.isPluginEnabled())
    }
    
    /**
     * 测试修改显示时长
     * **Validates: Requirement 6.2**
     */
    fun testModifyDisplayDuration() {
        // 测试设置不同的时长
        val durations = listOf(1000, 3000, 5000, 10000)
        
        for (duration in durations) {
            configService.setTipDisplayDuration(duration)
            assertEquals(
                "显示时长应该被正确设置为 $duration",
                duration,
                configService.getTipDisplayDuration()
            )
        }
    }
    
    /**
     * 测试修改提示样式
     * **Validates: Requirement 6.3**
     */
    fun testModifyTipStyle() {
        // 测试所有样式
        for (style in TipStyle.values()) {
            configService.setTipStyle(style)
            assertEquals(
                "提示样式应该被正确设置为 $style",
                style,
                configService.getTipStyle()
            )
        }
    }
    
    /**
     * 测试添加自定义注释模式
     * **Validates: Requirement 6.4**
     */
    fun testAddCustomAnnotationPattern() {
        val pattern1 = "@customTips"
        val pattern2 = "@hint"
        
        // 添加第一个模式
        configService.addCustomAnnotationPattern(pattern1)
        val patterns1 = configService.getCustomAnnotationPatterns()
        assertTrue("应该包含添加的模式", patterns1.contains(pattern1))
        assertEquals("应该只有一个模式", 1, patterns1.size)
        
        // 添加第二个模式
        configService.addCustomAnnotationPattern(pattern2)
        val patterns2 = configService.getCustomAnnotationPatterns()
        assertTrue("应该包含第一个模式", patterns2.contains(pattern1))
        assertTrue("应该包含第二个模式", patterns2.contains(pattern2))
        assertEquals("应该有两个模式", 2, patterns2.size)
        
        // 尝试添加重复的模式
        configService.addCustomAnnotationPattern(pattern1)
        val patterns3 = configService.getCustomAnnotationPatterns()
        assertEquals("重复添加不应该增加模式数量", 2, patterns3.size)
    }
    
    /**
     * 测试移除自定义注释模式
     * **Validates: Requirement 6.4**
     */
    fun testRemoveCustomAnnotationPattern() {
        val pattern1 = "@customTips"
        val pattern2 = "@hint"
        
        // 添加两个模式
        configService.addCustomAnnotationPattern(pattern1)
        configService.addCustomAnnotationPattern(pattern2)
        
        // 移除第一个模式
        configService.removeCustomAnnotationPattern(pattern1)
        val patterns1 = configService.getCustomAnnotationPatterns()
        assertFalse("不应该包含已移除的模式", patterns1.contains(pattern1))
        assertTrue("应该仍然包含第二个模式", patterns1.contains(pattern2))
        assertEquals("应该只剩一个模式", 1, patterns1.size)
        
        // 移除第二个模式
        configService.removeCustomAnnotationPattern(pattern2)
        val patterns2 = configService.getCustomAnnotationPatterns()
        assertTrue("所有模式都被移除后应该为空", patterns2.isEmpty())
        
        // 尝试移除不存在的模式
        configService.removeCustomAnnotationPattern("@nonexistent")
        assertTrue("移除不存在的模式不应该报错", patterns2.isEmpty())
    }
    
    /**
     * 测试重置配置
     * **Validates: Requirement 6.5**
     */
    fun testResetToDefaults() {
        // 修改所有配置
        configService.setPluginEnabled(false)
        configService.setTipDisplayDuration(10000)
        configService.setTipStyle(TipStyle.TOOLTIP)
        configService.addCustomAnnotationPattern("@custom")
        
        // 验证配置已修改
        assertFalse("插件应该被禁用", configService.isPluginEnabled())
        assertEquals("显示时长应该被修改", 10000, configService.getTipDisplayDuration())
        assertEquals("样式应该被修改", TipStyle.TOOLTIP, configService.getTipStyle())
        assertFalse("自定义模式列表不应该为空", configService.getCustomAnnotationPatterns().isEmpty())
        
        // 重置配置
        configService.resetToDefaults()
        
        // 验证配置已恢复默认值
        val config = configService.getCurrentConfiguration()
        assertTrue("插件应该恢复为启用状态", config.enabled)
        assertEquals("显示时长应该恢复为默认值", 5000, config.displayDuration)
        assertEquals("样式应该恢复为默认值", TipStyle.BALLOON, config.style)
        assertTrue("自定义模式列表应该为空", config.customPatterns.isEmpty())
        assertFalse("Javadoc模式应该恢复为默认值", config.javadocModeEnabled)
    }
    
    /**
     * 测试保存和加载完整配置
     * **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
     */
    fun testSaveAndLoadConfiguration() {
        val customConfig = TipsConfiguration(
            enabled = false,
            displayDuration = 8000,
            style = TipStyle.NOTIFICATION,
            customPatterns = listOf("@custom1", "@custom2")
        )
        
        // 保存配置
        configService.saveConfiguration(customConfig)
        
        // 验证配置已保存
        val loadedConfig = configService.getCurrentConfiguration()
        assertEquals("启用状态应该匹配", customConfig.enabled, loadedConfig.enabled)
        assertEquals("显示时长应该匹配", customConfig.displayDuration, loadedConfig.displayDuration)
        assertEquals("样式应该匹配", customConfig.style, loadedConfig.style)
        assertEquals("自定义模式列表应该匹配", customConfig.customPatterns, loadedConfig.customPatterns)
    }
    
    /**
     * 测试配置的持久化
     * **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
     */
    fun testConfigurationPersistence() {
        // 修改配置
        configService.setPluginEnabled(false)
        configService.setTipDisplayDuration(7000)
        configService.setTipStyle(TipStyle.TOOLTIP)
        configService.addCustomAnnotationPattern("@persistent")
        
        // 获取当前配置
        val config = configService.getCurrentConfiguration()
        
        // 验证配置正确
        assertFalse("插件应该被禁用", config.enabled)
        assertEquals("显示时长应该是7000", 7000, config.displayDuration)
        assertEquals("样式应该是TOOLTIP", TipStyle.TOOLTIP, config.style)
        assertTrue("应该包含自定义模式", config.customPatterns.contains("@persistent"))
    }
    
    /**
     * 测试边界值 - 显示时长
     * **Validates: Requirement 6.2**
     */
    fun testDisplayDurationBoundaryValues() {
        // 测试最小值
        configService.setTipDisplayDuration(0)
        assertEquals("应该接受0作为显示时长", 0, configService.getTipDisplayDuration())
        
        // 测试较大值
        configService.setTipDisplayDuration(60000)
        assertEquals("应该接受60000作为显示时长", 60000, configService.getTipDisplayDuration())
        
        // 测试负值（虽然不推荐，但应该能设置）
        configService.setTipDisplayDuration(-1)
        assertEquals("应该能设置负值", -1, configService.getTipDisplayDuration())
    }
    
    /**
     * 测试空字符串模式
     * **Validates: Requirement 6.4**
     */
    fun testEmptyStringPattern() {
        configService.addCustomAnnotationPattern("")
        val patterns = configService.getCustomAnnotationPatterns()
        assertTrue("应该能添加空字符串模式", patterns.contains(""))
    }
}
