package cn.myjdemo.autotips

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.core.spec.style.StringSpec

/**
 * 测试基类
 * 提供通用的测试设置和工具方法
 */
abstract class TestBase : BasePlatformTestCase() {
    
    /**
     * 设置测试环境
     */
    override fun setUp() {
        super.setUp()
        // 初始化测试环境
    }
    
    /**
     * 清理测试环境
     */
    override fun tearDown() {
        try {
            // 清理测试资源
        } finally {
            super.tearDown()
        }
    }
    
    /**
     * 获取测试数据路径
     * @return 测试数据目录路径
     */
    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }
}

/**
 * Kotest基础测试类
 * 用于基于属性的测试
 */
abstract class KotestBase : StringSpec() {
    
    companion object {
        const val PROPERTY_TEST_ITERATIONS = 100
    }
}