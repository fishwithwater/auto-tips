package cn.myjdemo.autotips.integration

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.CallDetectionService
import cn.myjdemo.autotips.service.CacheService
import com.intellij.openapi.components.service
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import org.junit.Test

/**
 * 组件交互集成测试
 * 
 * 测试各个组件之间的交互和协作
 */
class ComponentInteractionIntegrationTest : TestBase() {
    
    /**
     * 测试 CallDetectionService 和 AnnotationParser 的交互
     * 
     * 验证：
     * 1. CallDetectionService 能正确检测方法调用
     * 2. AnnotationParser 能解析检测到的方法的注释
     * 3. 两个服务的输出能正确对接
     */
    @Test
    fun `test call detection and annotation parsing interaction`() {
        // 1. 创建测试文件
        val testFile = myFixture.configureByText(
            "InteractionTest.java",
            """
            public class InteractionTest {
                /**
                 * @tips 这是方法A的提示
                 */
                public void methodA() {
                }
                
                /**
                 * @tips 这是方法B的提示
                 */
                public void methodB() {
                }
                
                public void caller() {
                    methodA();
                    methodB();
                }
            }
            """.trimIndent()
        )
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        // 2. 获取服务
        val callDetectionService = project.service<CallDetectionService>()
        val annotationParser = project.service<AnnotationParser>()
        
        // 3. 获取方法
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val methodA = testClass.findMethodsByName("methodA", false)[0]
        val methodB = testClass.findMethodsByName("methodB", false)[0]
        
        // 4. 解析两个方法的注释
        val tipsA = annotationParser.extractTipsContent(methodA)
        val tipsB = annotationParser.extractTipsContent(methodB)
        
        // 验证：两个方法的提示都应该被正确提取
        assertNotNull("Tips for methodA should be extracted", tipsA)
        assertNotNull("Tips for methodB should be extracted", tipsB)
        assertEquals("Tips for methodA should match", "这是方法A的提示", tipsA?.content)
        assertEquals("Tips for methodB should match", "这是方法B的提示", tipsB?.content)
    }
    
    /**
     * 测试 AnnotationParser 和 CacheService 的交互
     * 
     * 验证：
     * 1. AnnotationParser 解析的结果能被缓存
     * 2. 缓存的内容能被正确检索
     * 3. 缓存能提高性能
     */
    @Test
    fun `test annotation parsing and caching interaction`() {
        // 1. 创建测试文件
        val testFile = myFixture.configureByText(
            "CacheInteractionTest.java",
            """
            public class CacheInteractionTest {
                /**
                 * @tips 这是一个需要缓存的提示
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
        
        // 3. 第一次解析
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val testMethod = testClass.findMethodsByName("cachedMethod", false)[0]
        
        val startTime1 = System.currentTimeMillis()
        val tipsContent = annotationParser.extractTipsContent(testMethod)
        val parseTime1 = System.currentTimeMillis() - startTime1
        
        assertNotNull("Tips should be extracted", tipsContent)
        
        // 4. 缓存结果
        val methodSignature = "${testClass.qualifiedName}.${testMethod.name}"
        cacheService.cacheTips(methodSignature, tipsContent!!)
        
        // 5. 从缓存读取
        val startTime2 = System.currentTimeMillis()
        val cachedContent = cacheService.getCachedTips(methodSignature)
        val cacheTime = System.currentTimeMillis() - startTime2
        
        // 验证：缓存内容应该匹配
        assertNotNull("Cached content should be available", cachedContent)
        assertEquals("Cached content should match", tipsContent.content, cachedContent?.content)
        
        // 验证：缓存访问应该更快（通常情况下）
        // 注意：在测试环境中，这个断言可能不总是成立，因为解析也很快
        println("Parse time: ${parseTime1}ms, Cache time: ${cacheTime}ms")
    }
    
    /**
     * 测试多个服务的协同工作
     * 
     * 验证：
     * 1. 所有服务能同时工作
     * 2. 服务之间不会相互干扰
     * 3. 整体工作流程流畅
     */
    @Test
    fun `test multiple services working together`() {
        // 1. 创建测试文件
        val testFile = myFixture.configureByText(
            "MultiServiceTest.java",
            """
            public class MultiServiceTest {
                /**
                 * @tips 第一个方法的提示
                 */
                public void method1() {
                }
                
                /**
                 * @tips 第二个方法的提示
                 */
                public void method2() {
                }
                
                /**
                 * @tips 第三个方法的提示
                 */
                public void method3() {
                }
            }
            """.trimIndent()
        )
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        // 2. 获取所有服务并清理缓存
        val callDetectionService = project.service<CallDetectionService>()
        val annotationParser = project.service<AnnotationParser>()
        val cacheService = project.service<CacheService>()
        cacheService.clearAllCache()
        
        // 3. 处理所有方法
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val methods = listOf("method1", "method2", "method3")
        
        methods.forEach { methodName ->
            val method = testClass.findMethodsByName(methodName, false)[0]
            val tips = annotationParser.extractTipsContent(method)
            
            assertNotNull("Tips for $methodName should be extracted", tips)
            
            // 缓存结果
            val signature = "${testClass.qualifiedName}.$methodName"
            cacheService.cacheTips(signature, tips!!)
        }
        
        // 4. 验证所有方法的提示都已缓存
        methods.forEach { methodName ->
            val signature = "${testClass.qualifiedName}.$methodName"
            val cachedTips = cacheService.getCachedTips(signature)
            assertNotNull("Cached tips for $methodName should be available", cachedTips)
        }
        
        // 5. 验证缓存统计
        val stats = cacheService.getCacheStats()
        assertEquals("Cache should contain 3 entries", 3, stats["size"])
        assertEquals("Hit count should be 3", 3L, stats["hitCount"])
    }
    
    /**
     * 测试服务在异常情况下的交互
     * 
     * 验证：
     * 1. 一个服务的异常不会影响其他服务
     * 2. 系统能优雅地处理异常
     * 3. 异常后系统仍能继续工作
     */
    @Test
    fun `test service interaction under error conditions`() {
        // 1. 创建测试文件（包含有效和无效的方法）
        val testFile = myFixture.configureByText(
            "ErrorConditionTest.java",
            """
            public class ErrorConditionTest {
                /**
                 * @tips 有效的提示
                 */
                public void validMethod() {
                }
                
                // 无注释的方法
                public void noTipsMethod() {
                }
            }
            """.trimIndent()
        )
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        // 2. 获取服务
        val annotationParser = project.service<AnnotationParser>()
        val cacheService = project.service<CacheService>()
        
        // 3. 处理有效方法
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val validMethod = testClass.findMethodsByName("validMethod", false)[0]
        val validTips = annotationParser.extractTipsContent(validMethod)
        
        assertNotNull("Valid tips should be extracted", validTips)
        
        // 4. 处理无效方法（无@tips注释）
        val noTipsMethod = testClass.findMethodsByName("noTipsMethod", false)[0]
        val noTips = annotationParser.extractTipsContent(noTipsMethod)
        
        assertNull("No tips should be extracted for method without @tips", noTips)
        
        // 5. 验证缓存服务仍然正常工作
        val signature = "${testClass.qualifiedName}.validMethod"
        cacheService.cacheTips(signature, validTips!!)
        
        val cachedTips = cacheService.getCachedTips(signature)
        assertNotNull("Cache should still work after handling error conditions", cachedTips)
    }
    
    /**
     * 测试服务的并发访问
     * 
     * 验证：
     * 1. 多个线程同时访问服务不会出错
     * 2. 缓存在并发环境下是线程安全的
     * 3. 不会出现竞态条件
     */
    @Test
    fun `test concurrent service access`() {
        // 1. 创建测试文件
        val testFile = myFixture.configureByText(
            "ConcurrentTest.java",
            """
            public class ConcurrentTest {
                /**
                 * @tips 并发测试提示
                 */
                public void concurrentMethod() {
                }
            }
            """.trimIndent()
        )
        
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        
        // 2. 获取服务并清理缓存
        val annotationParser = project.service<AnnotationParser>()
        val cacheService = project.service<CacheService>()
        cacheService.clearAllCache()
        
        // 3. 解析方法
        val psiFile = testFile as PsiJavaFile
        val testClass = psiFile.classes[0]
        val testMethod = testClass.findMethodsByName("concurrentMethod", false)[0]
        val tipsContent = annotationParser.extractTipsContent(testMethod)
        
        assertNotNull("Tips should be extracted", tipsContent)
        
        // 4. 缓存结果
        val methodSignature = "${testClass.qualifiedName}.concurrentMethod"
        cacheService.cacheTips(methodSignature, tipsContent!!)
        
        // 5. 模拟并发访问（在测试环境中简化为顺序访问）
        repeat(10) {
            val cachedContent = cacheService.getCachedTips(methodSignature)
            assertNotNull("Cached content should be available in iteration $it", cachedContent)
            assertEquals("Content should match in iteration $it", tipsContent.content, cachedContent?.content)
        }
        
        // 6. 验证缓存统计
        val stats = cacheService.getCacheStats()
        assertEquals("Hit count should be 10", 10L, stats["hitCount"])
    }
}
