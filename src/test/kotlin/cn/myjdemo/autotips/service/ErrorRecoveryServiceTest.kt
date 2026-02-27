package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.impl.ErrorRecoveryServiceImpl
import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * ErrorRecoveryService 单元测试
 * 
 * **验证需求: 7.3**
 * 
 * 测试异常处理稳定性，确保：
 * 1. 解析异常不会导致IDE崩溃
 * 2. 显示异常能够被正确处理
 * 3. 性能问题能够触发适当的恢复动作
 * 4. 错误计数器正常工作
 */
class ErrorRecoveryServiceTest {
    
    private lateinit var errorRecoveryService: ErrorRecoveryServiceImpl
    
    @Before
    fun setUp() {
        errorRecoveryService = ErrorRecoveryServiceImpl()
        // 重置错误计数器
        errorRecoveryService.resetErrorCounter()
    }
    
    /**
     * 测试处理空指针异常
     * 
     * 需求 7.3: 解析异常应该被捕获并返回适当的恢复动作
     */
    @Test
    fun testHandleNullPointerException() {
        val exception = NullPointerException("Test null pointer")
        val context = ParsingContext(
            methodName = "testMethod",
            className = "TestClass",
            filePath = "/test/path",
            lineNumber = 10
        )
        
        val action = errorRecoveryService.handleParsingError(exception, context)
        
        // 空指针异常应该返回 SKIP
        assertEquals(RecoveryAction.SKIP, action)
    }
    
    /**
     * 测试处理类型转换异常
     * 
     * 需求 7.3: 类型转换异常应该使用回退方案
     */
    @Test
    fun testHandleClassCastException() {
        val exception = ClassCastException("Test class cast")
        val context = ParsingContext(
            methodName = "testMethod",
            className = "TestClass",
            filePath = "/test/path",
            lineNumber = 10
        )
        
        val action = errorRecoveryService.handleParsingError(exception, context)
        
        // 类型转换异常应该返回 FALLBACK
        assertEquals(RecoveryAction.FALLBACK, action)
    }
    
    /**
     * 测试错误阈值机制
     * 
     * 需求 7.3: 当错误频率过高时，应该禁用功能
     */
    @Test
    fun testErrorThresholdMechanism() {
        val context = ParsingContext(
            methodName = "testMethod",
            className = "TestClass",
            filePath = "/test/path",
            lineNumber = 10
        )
        
        // 触发多次错误
        for (i in 1..11) {
            val exception = RuntimeException("Test error $i")
            val action = errorRecoveryService.handleParsingError(exception, context)
            
            if (i <= 10) {
                // 前10次应该返回 SKIP
                assertEquals("Error $i should return SKIP", RecoveryAction.SKIP, action)
            } else {
                // 第11次应该返回 DISABLE_FEATURE
                assertEquals("Error $i should return DISABLE_FEATURE", RecoveryAction.DISABLE_FEATURE, action)
            }
        }
    }
    
    /**
     * 测试处理显示错误
     * 
     * 需求 7.3: UI异常应该被正确处理
     */
    @Test
    fun testHandleDisplayError() {
        val exception = RuntimeException("Display error")
        val content = TipsContent(
            content = "Test tips content",
            format = TipsFormat.PLAIN_TEXT
        )
        
        val action = errorRecoveryService.handleDisplayError(exception, content)
        
        // 应该返回 SKIP
        assertEquals(RecoveryAction.SKIP, action)
    }
    
    /**
     * 测试处理UI相关异常
     * 
     * 需求 7.3: UI相关异常应该使用回退显示方式
     */
    @Test
    fun testHandleUIRelatedError() {
        val exception = IllegalStateException("component not on EDT")
        val content = TipsContent(
            content = "Test tips content",
            format = TipsFormat.PLAIN_TEXT
        )
        
        val action = errorRecoveryService.handleDisplayError(exception, content)
        
        // UI相关错误应该返回 FALLBACK
        assertEquals(RecoveryAction.FALLBACK, action)
    }
    
    /**
     * 测试处理高内存使用
     * 
     * 需求 7.2: 当内存使用超过阈值时，应该清理缓存
     */
    @Test
    fun testHandleHighMemoryUsage() {
        val issue = PerformanceIssue(
            type = PerformanceIssueType.MEMORY_USAGE_HIGH,
            description = "Memory usage exceeded threshold",
            severity = Severity.HIGH
        )
        
        val action = errorRecoveryService.handlePerformanceIssue(issue)
        
        // 高内存使用应该返回 RESET_STATE
        assertEquals(RecoveryAction.RESET_STATE, action)
    }
    
    /**
     * 测试处理中等内存使用
     * 
     * 需求 7.2: 中等内存使用应该清理缓存
     */
    @Test
    fun testHandleMediumMemoryUsage() {
        val issue = PerformanceIssue(
            type = PerformanceIssueType.MEMORY_USAGE_HIGH,
            description = "Memory usage is medium",
            severity = Severity.MEDIUM
        )
        
        val action = errorRecoveryService.handlePerformanceIssue(issue)
        
        // 中等内存使用应该返回 FALLBACK
        assertEquals(RecoveryAction.FALLBACK, action)
    }
    
    /**
     * 测试处理响应时间慢
     * 
     * 需求 7.1: 响应超时应该取消当前操作
     */
    @Test
    fun testHandleSlowResponseTime() {
        val issue = PerformanceIssue(
            type = PerformanceIssueType.RESPONSE_TIME_SLOW,
            description = "Response time exceeded threshold",
            severity = Severity.CRITICAL
        )
        
        val action = errorRecoveryService.handlePerformanceIssue(issue)
        
        // 严重的响应时间问题应该返回 DISABLE_FEATURE
        assertEquals(RecoveryAction.DISABLE_FEATURE, action)
    }
    
    /**
     * 测试处理线程池饱和
     * 
     * 需求 7.1: 线程池饱和应该跳过非关键操作
     */
    @Test
    fun testHandleThreadPoolSaturation() {
        val issue = PerformanceIssue(
            type = PerformanceIssueType.THREAD_POOL_SATURATED,
            description = "Thread pool is saturated",
            severity = Severity.HIGH
        )
        
        val action = errorRecoveryService.handlePerformanceIssue(issue)
        
        // 线程池饱和应该返回 SKIP
        assertEquals(RecoveryAction.SKIP, action)
    }
    
    /**
     * 测试处理缓存未命中率高
     * 
     * 需求 7.2: 高缓存未命中率应该重置缓存
     */
    @Test
    fun testHandleHighCacheMissRate() {
        val issue = PerformanceIssue(
            type = PerformanceIssueType.CACHE_MISS_RATE_HIGH,
            description = "Cache miss rate is high",
            severity = Severity.MEDIUM
        )
        
        val action = errorRecoveryService.handlePerformanceIssue(issue)
        
        // 高缓存未命中率应该返回 RESET_STATE
        assertEquals(RecoveryAction.RESET_STATE, action)
    }
    
    /**
     * 测试错误计数器
     * 
     * 需求 7.3: 错误计数器应该正确跟踪错误频率
     */
    @Test
    fun testErrorCounter() {
        val context = ParsingContext(
            methodName = "testMethod",
            className = "TestClass",
            filePath = "/test/path",
            lineNumber = 10
        )
        
        // 初始计数应该为0
        val errorKey = "parsing_TestClass_testMethod"
        assertEquals(0, errorRecoveryService.getErrorCount(errorKey))
        
        // 触发一次错误
        errorRecoveryService.handleParsingError(RuntimeException("Test"), context)
        assertEquals(1, errorRecoveryService.getErrorCount(errorKey))
        
        // 再触发一次错误
        errorRecoveryService.handleParsingError(RuntimeException("Test"), context)
        assertEquals(2, errorRecoveryService.getErrorCount(errorKey))
        
        // 重置计数器
        errorRecoveryService.resetErrorCounter(errorKey)
        assertEquals(0, errorRecoveryService.getErrorCount(errorKey))
    }
    
    /**
     * 测试不同错误键的独立计数
     * 
     * 需求 7.3: 不同的错误应该独立计数
     */
    @Test
    fun testIndependentErrorCounters() {
        val context1 = ParsingContext(
            methodName = "method1",
            className = "Class1",
            filePath = "/test/path1",
            lineNumber = 10
        )
        
        val context2 = ParsingContext(
            methodName = "method2",
            className = "Class2",
            filePath = "/test/path2",
            lineNumber = 20
        )
        
        // 触发不同的错误
        errorRecoveryService.handleParsingError(RuntimeException("Test1"), context1)
        errorRecoveryService.handleParsingError(RuntimeException("Test2"), context2)
        errorRecoveryService.handleParsingError(RuntimeException("Test1"), context1)
        
        // 验证计数独立
        val errorKey1 = "parsing_Class1_method1"
        val errorKey2 = "parsing_Class2_method2"
        
        assertEquals(2, errorRecoveryService.getErrorCount(errorKey1))
        assertEquals(1, errorRecoveryService.getErrorCount(errorKey2))
    }
    
    /**
     * 测试显示错误的阈值机制
     * 
     * 需求 7.3: 显示错误频率过高时应该禁用显示功能
     */
    @Test
    fun testDisplayErrorThreshold() {
        val content = TipsContent(
            content = "Test content",
            format = TipsFormat.PLAIN_TEXT
        )
        
        // 触发多次显示错误
        for (i in 1..11) {
            val exception = RuntimeException("Display error $i")
            val action = errorRecoveryService.handleDisplayError(exception, content)
            
            if (i <= 10) {
                // 前10次应该返回 SKIP
                assertEquals("Error $i should return SKIP", RecoveryAction.SKIP, action)
            } else {
                // 第11次应该返回 DISABLE_FEATURE
                assertEquals("Error $i should return DISABLE_FEATURE", RecoveryAction.DISABLE_FEATURE, action)
            }
        }
    }
    
    /**
     * 测试恢复动作不会抛出异常
     * 
     * 需求 7.3: 错误处理本身不应该导致崩溃
     */
    @Test
    fun testRecoveryActionsDoNotThrowExceptions() {
        // 测试各种异常类型都能被安全处理
        val exceptions = listOf(
            NullPointerException("test"),
            ClassCastException("test"),
            IllegalArgumentException("test"),
            IllegalStateException("test"),
            RuntimeException("test")
        )
        
        val context = ParsingContext(
            methodName = "testMethod",
            className = "TestClass",
            filePath = "/test/path",
            lineNumber = 10
        )
        
        for (exception in exceptions) {
            try {
                val action = errorRecoveryService.handleParsingError(exception, context)
                // 验证返回了有效的恢复动作
                assertNotNull(action)
            } catch (e: Exception) {
                fail("Error recovery should not throw exception for ${exception.javaClass.simpleName}: ${e.message}")
            }
        }
    }
}
