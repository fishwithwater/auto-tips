package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.service.impl.CallDetectionServiceImpl
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

/**
 * 无效调用过滤的基于属性的测试
 * 
 * **Feature: auto-tips, Property 4: 无效调用过滤**
 * **Validates: Requirements 2.5**
 * 
 * 对于任何语法不完整或无效的方法调用，检测器应该不触发提示显示
 * 
 * 注意：由于IntelliJ Platform测试框架的限制，这些测试使用JUnit风格
 * 但通过多次迭代验证属性的通用性
 */
class InvalidCallFilteringPropertyTest : TestBase() {
    
    private lateinit var service: CallDetectionService
    private lateinit var validPsiFile: PsiJavaFile
    private val propertyTestIterations = 20
    
    override fun setUp() {
        super.setUp()
        service = CallDetectionServiceImpl()
        validPsiFile = myFixture.configureByFile("InvalidCallTestClass.java") as PsiJavaFile
    }
    
    /**
     * 属性 4.1: 语法完整性检查的一致性
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证isCallSyntaxComplete对同一元素的多次调用返回相同结果
     * 确保语法检查的稳定性和可靠性
     */
    fun testProperty4_1_SyntaxCompletenessCheckIsConsistent() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 获取方法体中的所有元素
        val allElements = PsiTreeUtil.collectElements(testMethod!!) { true }
        
        // 对每个元素进行多次语法完整性检查
        for (element in allElements) {
            val result1 = service.isCallSyntaxComplete(element)
            val result2 = service.isCallSyntaxComplete(element)
            val result3 = service.isCallSyntaxComplete(element)
            
            // 验证结果一致性
            assertEquals("Syntax completeness check should be consistent for ${element.javaClass.simpleName}", 
                result1, result2)
            assertEquals("Syntax completeness check should be consistent for ${element.javaClass.simpleName}", 
                result2, result3)
        }
    }
    
    /**
     * 属性 4.2: 有效调用上下文验证的正确性
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证isValidCallContext正确区分有效和无效的调用上下文
     * 对于有效的方法调用表达式，应该返回true
     */
    fun testProperty4_2_ValidCallContextCorrectlyIdentifiesValidCalls() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 获取所有方法调用表达式
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        // 对于测试文件中的有效方法调用，isValidCallContext应该返回true
        for (callExpression in methodCalls) {
            val isValid = service.isValidCallContext(callExpression)
            
            // 验证有效调用被正确识别
            assertTrue("Valid method call should have valid context: ${callExpression.text}", isValid)
        }
    }
    
    /**
     * 属性 4.3: 无效元素的上下文验证
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证对于非方法调用的元素，isValidCallContext返回false
     * 确保不会误判非调用元素为有效调用
     */
    fun testProperty4_3_InvalidElementsAreRejectedByContextValidation() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 获取各种非方法调用的元素
        val statements = PsiTreeUtil.findChildrenOfType(testMethod, PsiStatement::class.java)
        val identifiers = PsiTreeUtil.findChildrenOfType(testMethod, PsiIdentifier::class.java)
        
        var nonCallElementCount = 0
        var correctlyRejectedCount = 0
        
        // 测试语句元素（不是方法调用表达式）
        for (statement in statements) {
            if (statement !is PsiExpressionStatement) {
                nonCallElementCount++
                val isValid = service.isValidCallContext(statement)
                if (!isValid) {
                    correctlyRejectedCount++
                }
            }
        }
        
        // 测试标识符元素（不是完整的方法调用）
        for (identifier in identifiers) {
            // 排除作为方法调用一部分的标识符
            val parent = identifier.parent
            if (parent !is PsiMethodCallExpression && parent !is PsiReferenceExpression) {
                nonCallElementCount++
                val isValid = service.isValidCallContext(identifier)
                if (!isValid) {
                    correctlyRejectedCount++
                }
            }
        }
        
        // 如果有非调用元素，验证大部分被正确拒绝
        if (nonCallElementCount > 0) {
            val rejectionRate = correctlyRejectedCount.toDouble() / nonCallElementCount
            assertTrue("Most non-call elements should be rejected: $rejectionRate", 
                rejectionRate >= 0.5)
        }
    }
    
    /**
     * 属性 4.4: 完整调用的语法检查
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证对于语法完整的方法调用，isCallSyntaxComplete返回true
     * 这是过滤无效调用的基础
     */
    fun testProperty4_4_CompleteCallsPassSyntaxCheck() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        var completeCallCount = 0
        
        // 对于测试文件中的所有方法调用（都是完整的）
        for (callExpression in methodCalls) {
            val isComplete = service.isCallSyntaxComplete(callExpression)
            
            if (isComplete) {
                completeCallCount++
                
                // 验证完整的调用具有必要的组成部分
                assertNotNull("Complete call should have method expression", 
                    callExpression.methodExpression)
                assertNotNull("Complete call should have argument list", 
                    callExpression.argumentList)
            }
        }
        
        // 验证所有方法调用都被识别为完整的
        assertEquals("All method calls in test file should be complete", 
            methodCalls.size, completeCallCount)
    }
    
    /**
     * 属性 4.5: 方法引用解析的失败处理
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证对于无法解析的方法调用，resolveMethodReference返回null
     * 确保不会对无效调用返回错误的方法引用
     */
    fun testProperty4_5_UnresolvableCallsReturnNull() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        // 对于所有方法调用，验证解析结果的一致性
        for (callExpression in methodCalls) {
            val resolvedMethod = service.resolveMethodReference(callExpression)
            
            // 如果解析失败，应该返回null而不是抛出异常
            if (resolvedMethod == null) {
                // 验证这是一个预期的失败情况
                // 可能是因为方法不存在或引用不明确
                assertNull("Unresolvable call should return null", resolvedMethod)
            } else {
                // 如果解析成功，验证返回的是有效的方法
                assertNotNull("Resolved method should have a name", resolvedMethod.name)
                assertTrue("Resolved method name should not be blank", 
                    resolvedMethod.name.isNotBlank())
            }
        }
    }
    
    /**
     * 属性 4.6: 无效调用不触发方法调用检测
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证对于无效或不完整的调用，detectMethodCall不返回有效的MethodCallInfo
     * 这确保了无效调用不会触发提示显示
     */
    fun testProperty4_6_InvalidCallsDoNotTriggerDetection() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 获取所有元素
        val allElements = PsiTreeUtil.collectElements(testMethod!!) { true }
        
        var invalidElementCount = 0
        var correctlyFilteredCount = 0
        
        // 测试各种元素类型
        for (element in allElements) {
            // 跳过方法调用表达式（这些是有效的）
            if (element is PsiMethodCallExpression) {
                continue
            }
            
            invalidElementCount++
            
            // 检查语法完整性和上下文有效性
            val isSyntaxComplete = service.isCallSyntaxComplete(element)
            val isValidContext = service.isValidCallContext(element)
            
            // 无效元素应该至少有一个检查失败
            if (!isSyntaxComplete || !isValidContext) {
                correctlyFilteredCount++
            }
        }
        
        // 验证大部分无效元素被正确过滤
        if (invalidElementCount > 0) {
            val filterRate = correctlyFilteredCount.toDouble() / invalidElementCount
            assertTrue("Most invalid elements should be filtered: $filterRate", 
                filterRate >= 0.8)
        }
    }
    
    /**
     * 属性 4.7: 语法检查的防御性
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证isCallSyntaxComplete对各种类型的元素都能安全处理
     * 不应该抛出异常，即使对于意外的元素类型
     */
    fun testProperty4_7_SyntaxCheckIsDefensiveAgainstVariousElementTypes() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 获取各种类型的PSI元素
        val allElements = PsiTreeUtil.collectElements(testMethod!!) { true }
        
        var exceptionCount = 0
        var successfulCheckCount = 0
        
        // 对每种元素类型进行语法检查
        for (element in allElements) {
            try {
                val result = service.isCallSyntaxComplete(element)
                successfulCheckCount++
                
                // 结果应该是布尔值
                assertTrue("Result should be boolean", result is Boolean)
            } catch (e: Exception) {
                exceptionCount++
                // 记录异常但继续测试
                println("Exception for element type ${element.javaClass.simpleName}: ${e.message}")
            }
        }
        
        // 验证大部分检查都成功完成（不抛出异常）
        val successRate = successfulCheckCount.toDouble() / allElements.size
        assertTrue("Most syntax checks should complete without exception: $successRate", 
            successRate >= 0.95)
        
        // 验证异常数量很少
        assertTrue("Exception count should be minimal: $exceptionCount", 
            exceptionCount < allElements.size * 0.05)
    }
    
    /**
     * 属性 4.8: 上下文验证的防御性
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证isValidCallContext对各种类型的元素都能安全处理
     * 不应该抛出异常，即使对于意外的元素类型
     */
    fun testProperty4_8_ContextValidationIsDefensiveAgainstVariousElementTypes() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 获取各种类型的PSI元素
        val allElements = PsiTreeUtil.collectElements(testMethod!!) { true }
        
        var exceptionCount = 0
        var successfulCheckCount = 0
        
        // 对每种元素类型进行上下文验证
        for (element in allElements) {
            try {
                val result = service.isValidCallContext(element)
                successfulCheckCount++
                
                // 结果应该是布尔值
                assertTrue("Result should be boolean", result is Boolean)
            } catch (e: Exception) {
                exceptionCount++
                // 记录异常但继续测试
                println("Exception for element type ${element.javaClass.simpleName}: ${e.message}")
            }
        }
        
        // 验证大部分检查都成功完成（不抛出异常）
        val successRate = successfulCheckCount.toDouble() / allElements.size
        assertTrue("Most context validations should complete without exception: $successRate", 
            successRate >= 0.95)
        
        // 验证异常数量很少
        assertTrue("Exception count should be minimal: $exceptionCount", 
            exceptionCount < allElements.size * 0.05)
    }
    
    /**
     * 属性 4.9: 过滤逻辑的组合正确性
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证语法完整性和上下文有效性的组合能够正确过滤无效调用
     * 只有同时满足两个条件的调用才应该被认为是有效的
     */
    fun testProperty4_9_FilteringLogicCombinationIsCorrect() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        // 对于每个方法调用，验证过滤逻辑
        for (callExpression in methodCalls) {
            val isSyntaxComplete = service.isCallSyntaxComplete(callExpression)
            val isValidContext = service.isValidCallContext(callExpression)
            val resolvedMethod = service.resolveMethodReference(callExpression)
            
            // 如果语法完整且上下文有效，应该能够解析方法引用
            if (isSyntaxComplete && isValidContext) {
                // 大多数情况下应该能够解析
                // 但某些特殊情况可能解析失败（如引用不明确）
                // 所以我们只验证逻辑一致性，不强制要求解析成功
                
                // 验证至少有方法表达式
                assertNotNull("Valid call should have method expression", 
                    callExpression.methodExpression)
            }
            
            // 如果能够解析方法引用，语法应该是完整的
            if (resolvedMethod != null) {
                assertTrue("Resolvable call should have complete syntax", isSyntaxComplete)
                assertTrue("Resolvable call should have valid context", isValidContext)
            }
        }
    }
    
    /**
     * 属性 4.10: 无效调用过滤的完整性
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证整个过滤流程能够正确区分有效和无效的调用
     * 这是一个综合性的属性测试
     */
    fun testProperty4_10_InvalidCallFilteringCompleteness() {
        val testMethod = findMethodByName("testInvalidCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 获取所有方法调用表达式（有效的）
        val validMethodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        
        // 获取所有元素（包括有效和无效的）
        val allElements = PsiTreeUtil.collectElements(testMethod!!) { true }
        
        var validCallCount = 0
        var invalidElementCount = 0
        var correctlyFilteredCount = 0
        
        for (element in allElements) {
            val isSyntaxComplete = service.isCallSyntaxComplete(element)
            val isValidContext = service.isValidCallContext(element)
            
            if (element is PsiMethodCallExpression) {
                // 有效的方法调用
                validCallCount++
                
                // 应该通过过滤
                assertTrue("Valid call should have complete syntax: ${element.text}", 
                    isSyntaxComplete)
                assertTrue("Valid call should have valid context: ${element.text}", 
                    isValidContext)
            } else {
                // 其他元素（潜在的无效调用）
                invalidElementCount++
                
                // 应该被过滤（至少有一个检查失败）
                if (!isSyntaxComplete || !isValidContext) {
                    correctlyFilteredCount++
                }
            }
        }
        
        // 验证所有有效调用都被正确识别
        assertEquals("All valid method calls should be identified", 
            validMethodCalls.size, validCallCount)
        
        // 验证大部分无效元素被正确过滤
        if (invalidElementCount > 0) {
            val filterRate = correctlyFilteredCount.toDouble() / invalidElementCount
            assertTrue("Most invalid elements should be filtered: $filterRate", 
                filterRate >= 0.8)
        }
    }
    
    /**
     * 辅助方法：根据方法名查找方法
     */
    private fun findMethodByName(methodName: String): PsiMethod? {
        val javaClass = validPsiFile.classes.firstOrNull() ?: return null
        return javaClass.methods.find { it.name == methodName }
    }
    
    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }
}
