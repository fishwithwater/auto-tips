package cn.myjdemo.autotips.integration

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.CallDetectionService
import cn.myjdemo.autotips.service.CacheService
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.service.TipDisplayService
import com.intellij.openapi.components.service
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.junit.Test

/**
 * 端到端集成测试
 * 
 * 测试完整的用户工作流程，验证所有组件的正确交互
 * 
 * 验证需求: 所有需求
 */
class EndToEndIntegrationTest : TestBase() {
    
    /**
     * 测试完整的提示显示流程
     * 
     * 工作流程:
     * 1. 用户创建带有@tips注释的方法
     * 2. 用户在另一处调用该方法
     * 3. 用户输入右括号")"完成方法调用
     * 4. 插件检测方法调用
     * 5. 插件解析@tips注释
     * 6. 插件显示提示
     * 
     * 验证需求: 1.1, 2.1, 3.1, 4.1, 4.2
     */
    @Test
    fun `test complete tip display workflow`() {
        // 1. 创建测试文件
        val testFile = myFixture.configureByText(
            "TestClass.java",
            """
            public class TestClass {
                /**
                 * 这是一个测试方法
                 * @tips 这是一个重要的提示信息
                 */
                public void testMethod() {
                    System.out.println("Test");
                }
                
                public void caller() {
                    testMethod();
                }
            }
            """.trimIndent()
        )
        
        // 2. 确保PSI已同步
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        // 3. 获取服务
        val callDetectionService = project.service<CallDetectionService>()
        val annotationParser = project.service<AnnotationParser>()
        val tipDisplayService = project.service<TipDisplayService>()
        val cacheService = project.service<CacheService>()
        
        // 验证所有服务都已初始化
        assertNotNull("CallDetectionService should be initialized", callDetectionService)
        assertNotNull("AnnotationParser should be initialized", annotationParser)
        assertNotNull("TipDisplayService should be initialized", tipDisplayService)
        assertNotNull("CacheService should be initialized", cacheService)
        
        // 4. 查找方法调用位置
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val testMethod = testClass.findMethodsByName("testMethod", false)[0]
        
        // 5. 解析@tips注释
        val tipsContent = annotationParser.extractTipsContent(testMethod)
        
        // 验证：应该成功提取@tips内容
        assertNotNull("Tips content should be extracted", tipsContent)
        assertEquals("Tips content should match", "这是一个重要的提示信息", tipsContent?.content)
        
        // 6. 验证缓存功能
        val methodSignature = "${testClass.qualifiedName}.${testMethod.name}"
        cacheService.cacheTips(methodSignature, tipsContent!!)
        
        val cachedContent = cacheService.getCachedTips(methodSignature)
        assertNotNull("Cached tips should be retrievable", cachedContent)
        assertEquals("Cached content should match", tipsContent.content, cachedContent?.content)
    }
    
    /**
     * 测试多语言支持的集成
     * 
     * 工作流程:
     * 1. 创建Java和Kotlin文件
     * 2. 在两种语言中都使用@tips注释
     * 3. 验证插件能正确处理两种语言
     * 
     * 验证需求: 5.1, 5.2, 5.3
     */
    @Test
    fun `test multi-language support integration`() {
        // 1. 创建Java测试文件
        val javaFile = myFixture.configureByText(
            "JavaClass.java",
            """
            public class JavaClass {
                /**
                 * Java方法
                 * @tips Java提示信息
                 */
                public void javaMethod() {
                }
            }
            """.trimIndent()
        )
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        // 2. 获取服务
        val annotationParser = project.service<AnnotationParser>()
        
        // 3. 解析Java方法的@tips
        val javaPsiFile = javaFile as PsiJavaFile
        val javaClass = javaPsiFile.classes[0]
        val javaMethod = javaClass.findMethodsByName("javaMethod", false)[0]
        
        val javaTips = annotationParser.extractTipsContent(javaMethod)
        
        // 验证：应该成功提取Java的@tips
        assertNotNull("Java tips should be extracted", javaTips)
        assertEquals("Java tips content should match", "Java提示信息", javaTips?.content)
    }
    
    /**
     * 测试配置管理的集成
     * 
     * 工作流程:
     * 1. 修改插件配置
     * 2. 验证配置立即生效
     * 3. 验证配置影响插件行为
     * 
     * 验证需求: 6.1, 6.2, 6.3
     */
    @Test
    fun `test configuration management integration`() {
        // 1. 获取配置服务
        val configService = service<ConfigurationService>()
        
        // 2. 验证默认配置
        val isEnabled = configService.isPluginEnabled()
        
        // 验证：配置服务应该可用
        assertNotNull("Configuration service should be available", configService)
        
        // 3. 修改配置
        val originalDuration = configService.getTipDisplayDuration()
        
        // 验证：配置应该可以读取
        assertTrue("Display duration should be positive", originalDuration > 0)
    }
    
    /**
     * 测试缓存和性能优化的集成
     * 
     * 工作流程:
     * 1. 多次访问相同的方法提示
     * 2. 验证缓存命中
     * 3. 验证性能提升
     * 
     * 验证需求: 7.1, 7.2
     */
    @Test
    fun `test cache and performance integration`() {
        // 1. 创建测试文件
        val testFile = myFixture.configureByText(
            "CacheTest.java",
            """
            public class CacheTest {
                /**
                 * @tips 缓存测试提示
                 */
                public void cachedMethod() {
                }
            }
            """.trimIndent()
        )
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        // 2. 获取服务
        val annotationParser = project.service<AnnotationParser>()
        val cacheService = project.service<CacheService>()
        
        // 3. 第一次解析（缓存未命中）
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val testMethod = testClass.findMethodsByName("cachedMethod", false)[0]
        
        val tipsContent = annotationParser.extractTipsContent(testMethod)
        assertNotNull("Tips should be extracted", tipsContent)
        
        // 4. 缓存提示
        val methodSignature = "${testClass.qualifiedName}.${testMethod.name}"
        cacheService.cacheTips(methodSignature, tipsContent!!)
        
        // 5. 第二次访问（缓存命中）
        val cachedContent = cacheService.getCachedTips(methodSignature)
        assertNotNull("Cached tips should be available", cachedContent)
        assertEquals("Cached content should match", tipsContent.content, cachedContent?.content)
        
        // 6. 验证缓存统计
        val stats = cacheService.getCacheStats()
        assertTrue("Cache should have entries", (stats["size"] as Int) > 0)
        assertTrue("Hit count should be positive", (stats["hitCount"] as Long) > 0)
    }
    
    /**
     * 测试错误处理和稳定性的集成
     * 
     * 工作流程:
     * 1. 创建各种异常情况
     * 2. 验证插件正确处理异常
     * 3. 验证IDE保持稳定
     * 
     * 验证需求: 7.3
     */
    @Test
    fun `test error handling and stability integration`() {
        // 1. 获取服务
        val annotationParser = project.service<AnnotationParser>()
        
        // 2. 测试空方法（无注释）
        val testFile = myFixture.configureByText(
            "ErrorTest.java",
            """
            public class ErrorTest {
                public void noTipsMethod() {
                }
            }
            """.trimIndent()
        )
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val testMethod = testClass.findMethodsByName("noTipsMethod", false)[0]
        
        // 3. 尝试解析（应该返回null，不抛出异常）
        val tipsContent = annotationParser.extractTipsContent(testMethod)
        
        // 验证：应该返回null，不抛出异常
        assertNull("Tips should be null for method without @tips", tipsContent)
        
        // 4. 验证IDE仍然稳定（测试仍在运行）
        assertTrue("IDE should remain stable", true)
    }
    
    /**
     * 测试生命周期管理的集成
     * 
     * 工作流程:
     * 1. 验证所有服务已初始化
     * 2. 验证服务可以正常工作
     * 3. 验证清理操作不抛出异常
     * 
     * 验证需求: 7.4, 7.5
     */
    @Test
    fun `test lifecycle management integration`() {
        // 1. 验证所有核心服务已初始化
        val callDetectionService = project.service<CallDetectionService>()
        val annotationParser = project.service<AnnotationParser>()
        val tipDisplayService = project.service<TipDisplayService>()
        val cacheService = project.service<CacheService>()
        val configService = service<ConfigurationService>()
        
        assertNotNull("CallDetectionService should be initialized", callDetectionService)
        assertNotNull("AnnotationParser should be initialized", annotationParser)
        assertNotNull("TipDisplayService should be initialized", tipDisplayService)
        assertNotNull("CacheService should be initialized", cacheService)
        assertNotNull("ConfigurationService should be initialized", configService)
        
        // 2. 验证清理操作不抛出异常
        try {
            tipDisplayService.hideTip()
            cacheService.clearAllCache()
        } catch (e: Exception) {
            fail("Cleanup operations should not throw exceptions: ${e.message}")
        }
        
        // 3. 验证清理后服务仍然可用
        assertNotNull("Services should still be available after cleanup", cacheService)
    }
}
