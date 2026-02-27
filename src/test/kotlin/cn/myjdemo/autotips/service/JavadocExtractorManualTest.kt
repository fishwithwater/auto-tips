package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.service.impl.JavadocExtractorImpl
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 手动测试 JavadocExtractorImpl
 * 这个测试可以独立运行，不依赖其他测试文件
 */
class JavadocExtractorManualTest : BasePlatformTestCase() {
    
    private lateinit var extractor: JavadocExtractor
    
    override fun setUp() {
        super.setUp()
        extractor = JavadocExtractorImpl()
    }
    
    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }
    
    /**
     * 基本功能测试
     */
    fun testBasicFunctionality() {
        val psiFile = myFixture.configureByFile("JavadocTestClass.java") as PsiJavaFile
        val javaClass = psiFile.classes.firstOrNull()
        assertNotNull("Java class should exist", javaClass)
        
        // 测试简单的 Javadoc 提取
        val simpleMethod = javaClass!!.methods.find { it.name == "simpleJavadoc" }
        assertNotNull("Method simpleJavadoc should exist", simpleMethod)
        
        val content = extractor.extractJavadocFromMethod(simpleMethod!!)
        assertNotNull("Should extract Javadoc content", content)
        
        println("Extracted content: ${content!!.content}")
        println("Format: ${content.format}")
        
        // 基本验证
        assertTrue("Content should not be empty", content.content.isNotEmpty())
    }
    
    /**
     * 测试 @tips 过滤
     */
    fun testTipsFiltering() {
        val psiFile = myFixture.configureByFile("JavadocTestClass.java") as PsiJavaFile
        val javaClass = psiFile.classes.firstOrNull()
        assertNotNull("Java class should exist", javaClass)
        
        val method = javaClass!!.methods.find { it.name == "javadocWithTips" }
        assertNotNull("Method javadocWithTips should exist", method)
        
        val content = extractor.extractJavadocFromMethod(method!!)
        assertNotNull("Should extract Javadoc content", content)
        
        println("Content with tips filtered: ${content!!.content}")
        
        // Javadoc 模式保留完整注释，@tips 也被保留
        assertTrue("Should contain @tips in javadoc mode", content.content.contains("@tips"))
    }
}
