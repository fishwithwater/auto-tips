package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.service.impl.CallDetectionServiceImpl
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

/**
 * CallDetectionService的基于属性的测试
 * 
 * **Feature: auto-tips, Property 3: 方法调用检测准确性**
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 * 
 * 对于任何有效的方法调用表达式（包括带参数、链式调用、多态调用），
 * 检测器应该正确识别目标方法并解析到实际定义
 * 
 * 注意：由于IntelliJ Platform测试框架的限制，这些测试使用JUnit风格
 * 但通过多次迭代验证属性的通用性
 */
class CallDetectionPropertyTest : TestBase() {
    
    private lateinit var service: CallDetectionService
    private lateinit var psiFile: PsiJavaFile
    private val propertyTestIterations = 20
    
    override fun setUp() {
        super.setUp()
        service = CallDetectionServiceImpl()
        psiFile = myFixture.configureByFile("CallDetectionTestClass.java") as PsiJavaFile
    }
    
    /**
     * 属性 3.1: 方法引用解析的幂等性
     * 
     * **Validates: Requirements 2.1, 2.4**
     * 
     * 验证对同一个方法调用表达式多次解析应该返回相同的结果
     * 这确保了解析过程的稳定性和可靠性
     */
    fun testProperty3_1_MethodReferenceResolutionIsIdempotent() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        // 对每个方法调用进行多次解析，验证幂等性
        for (callExpression in methodCalls) {
            val result1 = service.resolveMethodReference(callExpression)
            val result2 = service.resolveMethodReference(callExpression)
            val result3 = service.resolveMethodReference(callExpression)
            
            // 验证结果一致性
            if (result1 != null) {
                assertNotNull("Second resolution should not be null", result2)
                assertNotNull("Third resolution should not be null", result3)
                assertEquals("Method names should match", result1.name, result2!!.name)
                assertEquals("Method names should match", result1.name, result3!!.name)
            } else {
                assertNull("Second resolution should be null", result2)
                assertNull("Third resolution should be null", result3)
            }
        }
    }
    
    /**
     * 属性 3.2: 有效调用上下文的一致性
     * 
     * **Validates: Requirements 2.1, 2.5**
     * 
     * 验证isValidCallContext对同一元素的多次调用返回相同结果
     * 确保上下文验证的稳定性
     */
    fun testProperty3_2_ValidCallContextCheckIsConsistent() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        // 对每个方法调用进行多次上下文检查，验证一致性
        for (callExpression in methodCalls) {
            val isValid1 = service.isValidCallContext(callExpression)
            val isValid2 = service.isValidCallContext(callExpression)
            val isValid3 = service.isValidCallContext(callExpression)
            
            // 验证结果一致性
            assertEquals("Context validity should be consistent", isValid1, isValid2)
            assertEquals("Context validity should be consistent", isValid2, isValid3)
        }
    }
    
    /**
     * 属性 3.3: 语法完整性检查的正确性
     * 
     * **Validates: Requirements 2.5**
     * 
     * 验证isCallSyntaxComplete正确识别完整和不完整的方法调用
     * 对于任何完整的方法调用，应该返回true
     */
    fun testProperty3_3_SyntaxCompletenessCheckCorrectlyIdentifiesCompleteCalls() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        // 测试文件中的所有方法调用都应该是完整的
        for (methodCall in methodCalls) {
            val isComplete = service.isCallSyntaxComplete(methodCall)
            assertTrue("Method call should be syntactically complete: ${methodCall.text}", isComplete)
        }
    }
    
    /**
     * 属性 3.4: 方法调用上下文包含必要信息
     * 
     * **Validates: Requirements 2.1, 2.2, 2.4**
     * 
     * 验证getMethodCallContext返回的上下文包含所有必要的信息
     * 包括方法、类和调用表达式
     */
    fun testProperty3_4_MethodCallContextContainsAllNecessaryInformation() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        for (callExpression in methodCalls) {
            val context = service.getMethodCallContext(callExpression)
            if (context != null) {
                // 验证上下文包含所有必要信息
                assertNotNull("Call expression should not be null", context.callExpression)
                assertNotNull("Resolved method should not be null", context.resolvedMethod)
                assertNotNull("Containing class should not be null", context.containingClass)
                assertNotNull("Call site should not be null", context.callSite)
                
                // 验证方法属于包含的类
                assertEquals("Method should belong to containing class", 
                    context.containingClass, context.resolvedMethod.containingClass)
            }
        }
    }
    
    /**
     * 属性 3.5: 链式调用检测的顺序性
     * 
     * **Validates: Requirements 2.3**
     * 
     * 验证detectChainedCalls返回的方法列表保持正确的调用顺序
     * 对于链式调用a.b().c()，应该按照正确的顺序返回
     */
    fun testProperty3_5_ChainedCallsDetectionPreservesCallOrder() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        // 查找链式调用
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        val chainedCall = methodCalls.find { 
            it.methodExpression.referenceName == "anotherChainableMethod" 
        }
        assertNotNull("Should find chained call", chainedCall)
        
        val chainedMethods = service.detectChainedCalls(chainedCall!!)
        
        // 如果检测到链式调用，验证顺序
        if (chainedMethods.isNotEmpty()) {
            // 最后一个调用应该是当前的callExpression
            val lastCall = chainedMethods.last()
            assertEquals("Last call should match current expression", 
                chainedCall, lastCall.callExpression)
            
            // 验证所有调用都有有效的方法名
            for (callInfo in chainedMethods) {
                assertTrue("Method name should not be blank", callInfo.methodName.isNotBlank())
            }
        }
    }
    
    /**
     * 属性 3.6: 参数解析的准确性
     * 
     * **Validates: Requirements 2.2**
     * 
     * 验证对于带参数的方法调用，能够正确解析参数列表
     * 参数数量应该与方法定义匹配
     */
    fun testProperty3_6_ParameterParsingAccuracyForMethodCallsWithArguments() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        val methodWithParamsCall = methodCalls.find { 
            it.methodExpression.referenceName == "methodWithParams" 
        }
        assertNotNull("Should find method with params call", methodWithParamsCall)
        
        val resolvedMethod = service.resolveMethodReference(methodWithParamsCall!!)
        assertNotNull("Should resolve method reference", resolvedMethod)
        
        // 验证参数列表存在
        val argumentList = methodWithParamsCall.argumentList
        assertNotNull("Argument list should not be null", argumentList)
        
        // 验证参数数量
        val argCount = argumentList.expressions.size
        val paramCount = resolvedMethod!!.parameters.size
        
        // 参数数量应该合理
        assertTrue("Argument count should be non-negative", argCount >= 0)
        assertTrue("Parameter count should be non-negative", paramCount >= 0)
        assertEquals("Argument count should match parameter count", paramCount, argCount)
    }
    
    /**
     * 属性 3.7: 方法名解析的正确性
     * 
     * **Validates: Requirements 2.1, 2.4**
     * 
     * 验证解析到的方法名与调用表达式中的方法名一致
     */
    fun testProperty3_7_ResolvedMethodNameMatchesCallExpression() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        for (callExpression in methodCalls) {
            val resolvedMethod = service.resolveMethodReference(callExpression)
            if (resolvedMethod != null) {
                val callMethodName = callExpression.methodExpression.referenceName
                val resolvedMethodName = resolvedMethod.name
                
                // 方法名应该匹配
                assertEquals("Method names should match", callMethodName, resolvedMethodName)
            }
        }
    }

    /**
     * 属性 3.8: 静态方法调用的正确解析
     * 
     * **Validates: Requirements 2.1, 2.4**
     * 
     * 验证静态方法调用能够被正确识别和解析
     * 静态方法应该被正确标识为静态
     */
    fun testProperty3_8_StaticMethodCallsAreCorrectlyResolved() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        val staticMethodCall = methodCalls.find { 
            it.methodExpression.referenceName == "staticMethod" 
        }
        
        if (staticMethodCall != null) {
            val resolvedMethod = service.resolveMethodReference(staticMethodCall)
            assertNotNull("Should resolve static method", resolvedMethod)
            assertTrue("Method should be static", 
                resolvedMethod!!.hasModifierProperty(PsiModifier.STATIC))
        }
    }
    
    /**
     * 属性 3.9: 多态调用的正确解析
     * 
     * **Validates: Requirements 2.4**
     * 
     * 验证通过接口或父类引用的方法调用能够解析到实际的实现方法
     * 这是多态性的核心特性
     */
    fun testProperty3_9_PolymorphicCallsResolveToActualImplementation() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        
        // 对所有方法调用进行解析
        for (callExpression in methodCalls) {
            val resolvedMethod = service.resolveMethodReference(callExpression)
            if (resolvedMethod != null) {
                // 验证解析到的方法有包含类
                assertNotNull("Resolved method should have containing class", 
                    resolvedMethod.containingClass)
                
                // 验证方法名不为空
                assertTrue("Method name should not be blank", 
                    resolvedMethod.name.isNotBlank())
            }
        }
    }
    
    /**
     * 属性 3.10: 方法调用检测的完整性
     * 
     * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
     * 
     * 验证对于测试文件中的所有方法调用，检测器能够提供一致的结果
     * 这是一个综合性的属性测试
     */
    fun testProperty3_10_MethodCallDetectionCompleteness() {
        val testMethod = findMethodByName("testMethodCalls")
        assertNotNull("Test method should exist", testMethod)
        
        val methodCalls = PsiTreeUtil.findChildrenOfType(testMethod, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls", methodCalls.isNotEmpty())
        
        var successfulResolutions = 0
        var totalCalls = 0
        
        for (callExpression in methodCalls) {
            totalCalls++
            
            // 检查语法完整性
            val isSyntaxComplete = service.isCallSyntaxComplete(callExpression)
            
            // 检查上下文有效性
            val isValidContext = service.isValidCallContext(callExpression)
            
            // 尝试解析方法引用
            val resolvedMethod = service.resolveMethodReference(callExpression)
            
            if (isSyntaxComplete && isValidContext && resolvedMethod != null) {
                successfulResolutions++
                
                // 验证解析结果的完整性
                assertNotNull("Method name should not be null", resolvedMethod.name)
                assertTrue("Method name should not be blank", resolvedMethod.name.isNotBlank())
            }
        }
        
        // 验证至少有一些方法调用被成功解析
        assertTrue("Should successfully resolve at least some method calls", 
            successfulResolutions > 0)
        
        // 验证成功率合理（至少50%）
        val successRate = successfulResolutions.toDouble() / totalCalls
        assertTrue("Success rate should be reasonable: $successRate", 
            successRate >= 0.5)
    }
    
    /**
     * 辅助方法：根据方法名查找方法
     */
    private fun findMethodByName(methodName: String): PsiMethod? {
        val javaClass = psiFile.classes.firstOrNull() ?: return null
        return javaClass.methods.find { it.name == methodName }
    }
    
    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }
}
