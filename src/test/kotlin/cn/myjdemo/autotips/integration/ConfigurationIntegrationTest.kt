package cn.myjdemo.autotips.integration

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.model.TipStyle
import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.service.TipDisplayService
import com.intellij.openapi.components.service

/**
 * 配置集成测试
 * 
 * 测试配置服务与其他组件的集成，验证配置更改能够立即生效
 * 
 * **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
 */
class ConfigurationIntegrationTest : TestBase() {
    
    private lateinit var configService: ConfigurationService
    private lateinit var annotationParser: AnnotationParser
    private lateinit var tipDisplayService: TipDisplayService
    
    override fun setUp() {
        super.setUp()
        configService = service<ConfigurationService>()
        annotationParser = project.service<AnnotationParser>()
        tipDisplayService = project.service<TipDisplayService>()
        
        // 重置到默认状态
        configService.resetToDefaults()
    }
    
    /**
     * 测试插件启用/禁用立即生效
     * **Validates: Requirement 6.1**
     */
    fun testPluginEnableDisableImmediateEffect() {
        // 启用插件
        configService.setPluginEnabled(true)
        assertTrue("插件应该立即启用", configService.isPluginEnabled())
        
        // 禁用插件
        configService.setPluginEnabled(false)
        assertFalse("插件应该立即禁用", configService.isPluginEnabled())
        
        // 验证配置立即生效
        val currentConfig = configService.getCurrentConfiguration()
        assertFalse("当前配置应该反映禁用状态", currentConfig.enabled)
    }
    
    /**
     * 测试显示时长修改立即生效
     * **Validates: Requirement 6.2**
     */
    fun testDisplayDurationImmediateEffect() {
        // 修改显示时长
        val newDuration = 8000
        configService.setTipDisplayDuration(newDuration)
        
        // 验证立即生效
        assertEquals(
            "显示时长应该立即更新",
            newDuration,
            configService.getTipDisplayDuration()
        )
        
        // 验证TipDisplayService可以获取新的配置
        val currentConfig = configService.getCurrentConfiguration()
        assertEquals(
            "TipDisplayService应该能获取新的显示时长",
            newDuration,
            currentConfig.displayDuration
        )
    }
    
    /**
     * 测试提示样式修改立即生效
     * **Validates: Requirement 6.3**
     */
    fun testTipStyleImmediateEffect() {
        // 测试每种样式
        for (style in TipStyle.values()) {
            configService.setTipStyle(style)
            
            // 验证立即生效
            assertEquals(
                "样式应该立即更新为 $style",
                style,
                configService.getTipStyle()
            )
            
            // 验证配置对象也更新了
            val currentConfig = configService.getCurrentConfiguration()
            assertEquals(
                "配置对象应该反映新样式",
                style,
                currentConfig.style
            )
        }
    }
    
    /**
     * 测试自定义注释模式立即生效
     * **Validates: Requirement 6.4**
     */
    fun testCustomAnnotationPatternImmediateEffect() {
        // 创建测试Java文件，包含自定义注释
        val javaCode = """
            public class TestClass {
                /**
                 * Test method with custom annotation
                 * @customTip This is a custom tip
                 */
                public void testMethod() {
                }
            }
        """.trimIndent()
        
        val psiFile = myFixture.configureByText("TestClass.java", javaCode)
        
        // 获取方法
        val psiJavaFile = psiFile as com.intellij.psi.PsiJavaFile
        val psiClass = psiJavaFile.classes[0]
        val method = psiClass.findMethodsByName("testMethod", false)[0]
        
        // 在添加自定义模式之前，应该无法提取内容
        val contentBefore = annotationParser.extractTipsContent(method)
        assertNull("添加自定义模式前不应该提取到内容", contentBefore)
        
        // 添加自定义注释模式
        configService.addCustomAnnotationPattern("@customTip")
        
        // 验证立即生效 - 现在应该能提取到内容
        val contentAfter = annotationParser.extractTipsContent(method)
        assertNotNull("添加自定义模式后应该能提取到内容", contentAfter)
        assertEquals(
            "应该提取到正确的内容",
            "This is a custom tip",
            contentAfter?.content
        )
        
        // 移除自定义模式
        configService.removeCustomAnnotationPattern("@customTip")
        
        // 验证移除后立即生效
        val contentAfterRemoval = annotationParser.extractTipsContent(method)
        assertNull("移除自定义模式后不应该提取到内容", contentAfterRemoval)
    }
    
    /**
     * 测试重置配置立即生效
     * **Validates: Requirement 6.5**
     */
    fun testResetConfigurationImmediateEffect() {
        // 修改所有配置
        configService.setPluginEnabled(false)
        configService.setTipDisplayDuration(10000)
        configService.setTipStyle(TipStyle.NOTIFICATION)
        configService.addCustomAnnotationPattern("@custom")
        
        // 验证配置已修改
        assertFalse("插件应该被禁用", configService.isPluginEnabled())
        assertEquals("显示时长应该被修改", 10000, configService.getTipDisplayDuration())
        assertEquals("样式应该被修改", TipStyle.NOTIFICATION, configService.getTipStyle())
        assertTrue("应该有自定义模式", configService.getCustomAnnotationPatterns().isNotEmpty())
        
        // 重置配置
        configService.resetToDefaults()
        
        // 验证立即恢复默认值
        assertTrue("插件应该立即恢复为启用状态", configService.isPluginEnabled())
        assertEquals("显示时长应该立即恢复为默认值", 5000, configService.getTipDisplayDuration())
        assertEquals("样式应该立即恢复为默认值", TipStyle.BALLOON, configService.getTipStyle())
        assertTrue("自定义模式列表应该立即清空", configService.getCustomAnnotationPatterns().isEmpty())
    }
    
    /**
     * 测试多个配置同时修改
     * **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
     */
    fun testMultipleConfigurationChanges() {
        // 同时修改多个配置
        configService.setPluginEnabled(false)
        configService.setTipDisplayDuration(7000)
        configService.setTipStyle(TipStyle.TOOLTIP)
        configService.addCustomAnnotationPattern("@hint")
        
        // 验证所有修改都立即生效
        val config = configService.getCurrentConfiguration()
        assertFalse("插件应该被禁用", config.enabled)
        assertEquals("显示时长应该是7000", 7000, config.displayDuration)
        assertEquals("样式应该是TOOLTIP", TipStyle.TOOLTIP, config.style)
        assertTrue("应该包含自定义模式", config.customPatterns.contains("@hint"))
    }
    
    /**
     * 测试配置持久化后重新加载
     * **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
     */
    fun testConfigurationPersistenceAndReload() {
        // 修改配置
        configService.setPluginEnabled(false)
        configService.setTipDisplayDuration(6000)
        configService.setTipStyle(TipStyle.NOTIFICATION)
        configService.addCustomAnnotationPattern("@note")
        
        // 获取当前配置
        val savedConfig = configService.getCurrentConfiguration()
        
        // 验证配置正确保存
        assertFalse("保存的配置应该显示插件禁用", savedConfig.enabled)
        assertEquals("保存的配置应该显示时长为6000", 6000, savedConfig.displayDuration)
        assertEquals("保存的配置应该显示样式为NOTIFICATION", TipStyle.NOTIFICATION, savedConfig.style)
        assertTrue("保存的配置应该包含自定义模式", savedConfig.customPatterns.contains("@note"))
        
        // 重新加载配置（通过重置然后重新设置来模拟）
        configService.saveConfiguration(savedConfig)
        val reloadedConfig = configService.getCurrentConfiguration()
        
        // 验证重新加载的配置与保存的配置一致
        assertEquals("重新加载的配置应该与保存的配置一致", savedConfig, reloadedConfig)
    }
    
    /**
     * 测试配置更改不影响正在显示的提示
     * **Validates: Requirement 6.2**
     */
    fun testConfigurationChangeDoesNotAffectCurrentTip() {
        // 创建测试编辑器
        myFixture.configureByText("Test.java", "class Test { }")
        val editor = myFixture.editor
        val position = editor.caretModel.logicalPosition
        
        // 显示一个提示
        val content = TipsContent("Test tip", TipsFormat.PLAIN_TEXT)
        tipDisplayService.showTip(content, editor, position)
        
        // 验证提示正在显示
        assertTrue("提示应该正在显示", tipDisplayService.isCurrentlyShowing())
        
        // 修改显示时长配置
        configService.setTipDisplayDuration(1000)
        
        // 验证当前提示仍然显示（配置更改不影响已显示的提示）
        assertTrue("修改配置后当前提示应该仍然显示", tipDisplayService.isCurrentlyShowing())
        
        // 清理
        tipDisplayService.hideTip()
    }
}
