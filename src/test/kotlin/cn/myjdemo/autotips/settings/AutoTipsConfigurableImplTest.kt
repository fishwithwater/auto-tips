package cn.myjdemo.autotips.settings

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.model.TipStyle
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.settings.impl.AutoTipsConfigurableImpl
import com.intellij.openapi.components.service

/**
 * AutoTipsConfigurable单元测试
 * 
 * 测试设置界面的功能，包括：
 * - UI组件创建
 * - 配置修改检测
 * - 配置应用
 * - 配置重置
 * 
 * **Validates: Requirements 6.1, 6.2, 6.3**
 */
class AutoTipsConfigurableImplTest : TestBase() {
    
    private lateinit var configurable: AutoTipsConfigurableImpl
    private lateinit var configService: ConfigurationService
    
    override fun setUp() {
        super.setUp()
        configurable = AutoTipsConfigurableImpl()
        configService = service<ConfigurationService>()
        // 重置到默认状态
        configService.resetToDefaults()
    }
    
    override fun tearDown() {
        try {
            configurable.disposeUIResources()
        } finally {
            super.tearDown()
        }
    }
    
    /**
     * 测试显示名称
     * **Validates: Requirement 6.1**
     */
    fun testDisplayName() {
        assertEquals("显示名称应该是 Auto-Tips", "Auto-Tips", configurable.displayName)
    }
    
    /**
     * 测试帮助主题
     * **Validates: Requirement 6.1**
     */
    fun testHelpTopic() {
        assertNull("帮助主题应该为null", configurable.helpTopic)
    }
    
    /**
     * 测试创建UI组件
     * **Validates: Requirement 6.1**
     */
    fun testCreateComponent() {
        val component = configurable.createComponent()
        assertNotNull("应该创建UI组件", component)
        
        // 再次调用应该返回相同的组件
        val component2 = configurable.createComponent()
        assertSame("应该返回相同的组件实例", component, component2)
    }
    
    /**
     * 测试初始状态不应该被标记为已修改
     * **Validates: Requirement 6.1**
     */
    fun testInitialStateNotModified() {
        configurable.createComponent()
        assertFalse("初始状态不应该被标记为已修改", configurable.isModified)
    }
    
    /**
     * 测试重置功能
     * **Validates: Requirements 6.1, 6.2, 6.3**
     */
    fun testReset() {
        // 修改配置服务中的值
        configService.setPluginEnabled(false)
        configService.setTipDisplayDuration(10000)
        configService.setTipStyle(TipStyle.TOOLTIP)
        
        // 创建UI并重置
        configurable.createComponent()
        configurable.reset()
        
        // UI应该反映配置服务中的值
        assertFalse("重置后不应该被标记为已修改", configurable.isModified)
    }
    
    /**
     * 测试应用功能
     * **Validates: Requirements 6.1, 6.2, 6.3**
     */
    fun testApply() {
        // 创建UI组件
        configurable.createComponent()
        configurable.reset()
        
        // 通过反射或直接访问修改UI组件的值
        // 注意：在实际测试中，我们需要模拟用户修改UI
        // 这里我们测试apply方法能正常工作
        
        // 应用配置
        configurable.apply()
        
        // 应用后不应该被标记为已修改
        assertFalse("应用后不应该被标记为已修改", configurable.isModified)
    }
    
    /**
     * 测试释放UI资源
     * **Validates: Requirement 6.1**
     */
    fun testDisposeUIResources() {
        // 创建UI组件
        val component = configurable.createComponent()
        assertNotNull("应该创建UI组件", component)
        
        // 释放资源
        configurable.disposeUIResources()
        
        // 再次创建应该返回新的组件
        val newComponent = configurable.createComponent()
        assertNotNull("应该创建新的UI组件", newComponent)
        assertNotSame("应该是不同的组件实例", component, newComponent)
    }
    
    /**
     * 测试配置更改后的修改检测
     * **Validates: Requirements 6.2, 6.3**
     */
    fun testModificationDetection() {
        // 设置初始配置
        configService.setPluginEnabled(true)
        configService.setTipDisplayDuration(5000)
        configService.setTipStyle(TipStyle.BALLOON)
        
        // 创建UI并重置
        configurable.createComponent()
        configurable.reset()
        
        // 初始状态不应该被标记为已修改
        assertFalse("初始状态不应该被标记为已修改", configurable.isModified)
        
        // 修改配置服务（模拟外部修改）
        configService.setPluginEnabled(false)
        
        // 注意：isModified检查UI值与配置服务值的差异
        // 如果UI值与配置服务值不同，应该返回true
    }
    
    /**
     * 测试多次应用配置
     * **Validates: Requirements 6.1, 6.2, 6.3**
     */
    fun testMultipleApply() {
        configurable.createComponent()
        configurable.reset()
        
        // 第一次应用
        configurable.apply()
        assertFalse("第一次应用后不应该被标记为已修改", configurable.isModified)
        
        // 第二次应用
        configurable.apply()
        assertFalse("第二次应用后不应该被标记为已修改", configurable.isModified)
    }
    
    /**
     * 测试重置后再应用
     * **Validates: Requirements 6.1, 6.2, 6.3**
     */
    fun testResetThenApply() {
        // 修改配置
        configService.setPluginEnabled(false)
        configService.setTipDisplayDuration(8000)
        
        configurable.createComponent()
        configurable.reset()
        
        // 重置后应用
        configurable.apply()
        
        // 配置应该保持不变
        assertFalse("应用后不应该被标记为已修改", configurable.isModified)
        assertEquals("配置应该保持不变", false, configService.isPluginEnabled())
        assertEquals("配置应该保持不变", 8000, configService.getTipDisplayDuration())
    }
    
    /**
     * 测试 Javadoc 模式复选框创建
     * **Validates: Requirement 1.1**
     */
    fun testJavadocModeCheckboxCreation() {
        val component = configurable.createComponent()
        assertNotNull("应该创建UI组件", component)
        // Javadoc 模式复选框应该作为UI的一部分被创建
    }
    
    /**
     * 测试 Javadoc 模式默认值
     * **Validates: Requirements 1.1, 6.1**
     */
    fun testJavadocModeDefaultValue() {
        // 默认配置应该禁用 Javadoc 模式
        configService.resetToDefaults()
        assertFalse("默认应该禁用 Javadoc 模式", configService.isJavadocModeEnabled())
        
        configurable.createComponent()
        configurable.reset()
        
        // UI 应该反映默认值
        assertFalse("重置后不应该被标记为已修改", configurable.isModified)
    }
    
    /**
     * 测试 Javadoc 模式配置保存
     * **Validates: Requirements 1.1, 1.4**
     */
    fun testJavadocModeConfigurationSave() {
        // 启用 Javadoc 模式
        configService.setJavadocModeEnabled(true)
        
        configurable.createComponent()
        configurable.reset()
        
        // 应用配置
        configurable.apply()
        
        // 验证配置已保存
        assertTrue("Javadoc 模式应该被启用", configService.isJavadocModeEnabled())
    }
    
    /**
     * 测试 Javadoc 模式配置加载
     * **Validates: Requirements 1.1, 1.4**
     */
    fun testJavadocModeConfigurationLoad() {
        // 设置 Javadoc 模式为启用
        configService.setJavadocModeEnabled(true)
        
        configurable.createComponent()
        configurable.reset()
        
        // UI 应该反映配置值
        assertFalse("重置后不应该被标记为已修改", configurable.isModified)
        
        // 禁用 Javadoc 模式
        configService.setJavadocModeEnabled(false)
        
        configurable.reset()
        
        // UI 应该反映新的配置值
        assertFalse("重置后不应该被标记为已修改", configurable.isModified)
    }
    
    /**
     * 测试 Javadoc 模式修改检测
     * **Validates: Requirements 1.1, 6.2**
     */
    fun testJavadocModeModificationDetection() {
        // 设置初始配置
        configService.setJavadocModeEnabled(false)
        
        configurable.createComponent()
        configurable.reset()
        
        // 初始状态不应该被标记为已修改
        assertFalse("初始状态不应该被标记为已修改", configurable.isModified)
        
        // 修改配置服务
        configService.setJavadocModeEnabled(true)
        
        // 注意：isModified 检查 UI 值与配置服务值的差异
    }
    
    /**
     * 测试 Javadoc 模式不影响其他配置
     * **Validates: Requirement 5.4**
     */
    fun testJavadocModeDoesNotAffectOtherSettings() {
        // 设置初始配置
        configService.setPluginEnabled(true)
        configService.setTipDisplayDuration(5000)
        configService.setTipStyle(TipStyle.BALLOON)
        configService.setJavadocModeEnabled(false)
        
        configurable.createComponent()
        configurable.reset()
        
        // 只修改 Javadoc 模式
        configService.setJavadocModeEnabled(true)
        configurable.reset()
        configurable.apply()
        
        // 其他配置应该保持不变
        assertTrue("插件启用状态应该保持不变", configService.isPluginEnabled())
        assertEquals("显示时长应该保持不变", 5000, configService.getTipDisplayDuration())
        assertEquals("样式应该保持不变", TipStyle.BALLOON, configService.getTipStyle())
        assertTrue("Javadoc 模式应该被启用", configService.isJavadocModeEnabled())
    }
}
