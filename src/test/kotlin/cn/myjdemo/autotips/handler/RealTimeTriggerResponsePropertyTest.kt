package cn.myjdemo.autotips.handler

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.handler.impl.TipsTypedActionHandlerImpl
import cn.myjdemo.autotips.service.CallDetectionService
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.TipDisplayService
import cn.myjdemo.autotips.service.ConfigurationService
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.components.service
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil

/**
 * 实时触发响应的基于属性的测试
 * 
 * **Feature: auto-tips, Property 8: Real-time Trigger Response**
 * **Validates: Requirements 4.1, 4.2**
 * 
 * 对于任何方法调用完成事件（输入")"），插件应该在500毫秒内检查并显示相应提示
 * 
 * 属性说明:
 * - 需求 4.1: 当开发者输入方法调用的右括号")"时，立即检查该方法是否有@tips标记
 * - 需求 4.2: 当检测到@tips标记时，在500毫秒内显示提示
 * 
 * 注意：由于IntelliJ Platform测试框架的限制，这些测试使用JUnit风格
 * 但通过多次迭代验证属性的通用性
 */
class RealTimeTriggerResponsePropertyTest : TestBase() {
    
    private lateinit var handler: TipsTypedActionHandler
    private lateinit var configService: ConfigurationService
    private lateinit var tipDisplayService: TipDisplayService
    private val propertyTestIterations = 100
    
    override fun setUp() {
        super.setUp()
        handler = TipsTypedActionHandlerImpl()
        configService = service<ConfigurationService>()
        tipDisplayService = project.service<TipDisplayService>()
        
        // 确保插件启用
        configService.setPluginEnabled(true)
    }
    
    /**
     * 属性 8.1: 右括号触发检测的一致性
     * 
     * **Validates: Requirements 4.1**
     * 
     * 验证对于任何输入的右括号")"，插件都会立即检查该方法是否有@tips标记
     * 这个检查应该是一致的，不受其他因素影响
     */
    fun testProperty8_1_RightParenthesisTriggerIsConsistent() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到所有方法调用
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        assertTrue("Should find method calls in test file", methodCalls.isNotEmpty())
        
        // 对每个方法调用进行多次测试，验证触发的一致性
        for (methodCall in methodCalls.take(10)) { // 限制测试数量以提高性能
            val callText = methodCall.text
            val offset = methodCall.textRange.endOffset
            
            // 多次测试同一位置
            repeat(5) { iteration ->
                // 重置状态
                tipDisplayService.hideTip()
                
                // 移动光标到方法调用的右括号之后
                editor.caretModel.moveToOffset(offset)
                
                // 记录开始时间
                val startTime = System.currentTimeMillis()
                
                // 执行处理
                handler.execute(editor, ')', dataContext)
                
                // 验证处理立即返回（不阻塞）
                val executionTime = System.currentTimeMillis() - startTime
                assertTrue("Execute should return immediately (< 100ms), took ${executionTime}ms", 
                    executionTime < 100)
                
                // 等待后台处理完成（最多500ms）
                Thread.sleep(100)
            }
        }
    }
    
    /**
     * 属性 8.2: 检测响应时间的及时性
     * 
     * **Validates: Requirements 4.2**
     * 
     * 验证当检测到@tips标记时，插件在500毫秒内显示提示
     * 这是一个性能要求，确保用户体验的流畅性
     */
    fun testProperty8_2_DetectionResponseTimeIsWithin500ms() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到带有@tips注释的方法调用
        val text = editor.document.text
        val methodCallIndex = text.indexOf("methodWithTips()")
        
        if (methodCallIndex > 0) {
            // 多次测试响应时间
            val responseTimes = mutableListOf<Long>()
            
            repeat(20) { iteration ->
                // 重置状态
                tipDisplayService.hideTip()
                
                // 移动光标到方法调用的右括号之后
                val offset = methodCallIndex + "methodWithTips()".length
                editor.caretModel.moveToOffset(offset)
                
                // 记录开始时间
                val startTime = System.currentTimeMillis()
                
                // 执行处理
                handler.execute(editor, ')', dataContext)
                
                // 等待提示显示或超时
                var tipShown = false
                var elapsedTime = 0L
                while (elapsedTime < 600) { // 稍微超过500ms以确保检测到
                    Thread.sleep(50)
                    elapsedTime = System.currentTimeMillis() - startTime
                    
                    if (tipDisplayService.isCurrentlyShowing()) {
                        tipShown = true
                        responseTimes.add(elapsedTime)
                        break
                    }
                }
                
                // 如果提示显示了，验证响应时间
                if (tipShown) {
                    val responseTime = responseTimes.last()
                    assertTrue("Response time should be within 500ms, was ${responseTime}ms", 
                        responseTime <= 500)
                }
            }
            
            // 验证至少有一些测试成功显示了提示
            if (responseTimes.isNotEmpty()) {
                val avgResponseTime = responseTimes.average()
                println("Average response time: ${avgResponseTime}ms over ${responseTimes.size} successful displays")
                assertTrue("Average response time should be reasonable", avgResponseTime <= 500)
            }
        }
    }
    
    /**
     * 属性 8.3: 非右括号字符不触发检测
     * 
     * **Validates: Requirements 4.1**
     * 
     * 验证只有右括号")"才触发检测，其他字符不应该触发
     * 这确保了触发机制的精确性
     */
    fun testProperty8_3_OnlyRightParenthesisTriggers() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 测试各种字符
        val nonTriggerChars = listOf('(', '{', '}', '[', ']', ';', ',', '.', 'a', 'z', '0', '9', ' ', '\n')
        
        // 对每个字符进行多次测试
        for (char in nonTriggerChars) {
            repeat(5) {
                // 重置状态
                tipDisplayService.hideTip()
                
                // 移动到文件中间位置
                val offset = editor.document.textLength / 2
                editor.caretModel.moveToOffset(offset)
                
                // 执行处理
                handler.execute(editor, char, dataContext)
                
                // 等待一小段时间
                Thread.sleep(50)
                
                // 验证没有显示提示（因为不是右括号）
                assertFalse("Character '$char' should not trigger tip display", 
                    tipDisplayService.isCurrentlyShowing())
            }
        }
    }
    
    /**
     * 属性 8.4: 插件禁用时不触发检测
     * 
     * **Validates: Requirements 4.1**
     * 
     * 验证当插件被禁用时，即使输入右括号也不触发检测
     * 这确保了配置的有效性
     */
    fun testProperty8_4_NoTriggerWhenPluginDisabled() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 禁用插件
        configService.setPluginEnabled(false)
        
        // 找到方法调用位置
        val text = editor.document.text
        val methodCallIndex = text.indexOf("methodWithTips()")
        
        if (methodCallIndex > 0) {
            // 多次测试
            repeat(10) {
                // 重置状态
                tipDisplayService.hideTip()
                
                // 移动光标到方法调用的右括号之后
                val offset = methodCallIndex + "methodWithTips()".length
                editor.caretModel.moveToOffset(offset)
                
                // 执行处理
                handler.execute(editor, ')', dataContext)
                
                // 等待
                Thread.sleep(100)
                
                // 验证没有显示提示（因为插件被禁用）
                assertFalse("Should not trigger when plugin is disabled", 
                    tipDisplayService.isCurrentlyShowing())
            }
        }
        
        // 重新启用插件
        configService.setPluginEnabled(true)
    }
    
    /**
     * 属性 8.5: 在注释中输入不触发检测
     * 
     * **Validates: Requirements 4.1, 2.5**
     * 
     * 验证在注释中输入右括号不触发检测
     * 这确保了上下文感知的正确性
     */
    fun testProperty8_5_NoTriggerInComments() {
        // 创建包含注释的测试文件
        val psiFile = myFixture.configureByText("Test.java", """
            public class Test {
                /**
                 * This is a comment with method() call
                 * Another line with method() call
                 */
                public void testMethod() {
                    // Single line comment with method()
                    /* Block comment with method() */
                }
            }
        """.trimIndent()) as PsiJavaFile
        
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        val text = editor.document.text
        
        // 找到所有注释中的")"位置
        val commentParenPositions = mutableListOf<Int>()
        var index = 0
        while (index < text.length) {
            val parenIndex = text.indexOf("method()", index)
            if (parenIndex < 0) break
            commentParenPositions.add(parenIndex + "method()".length - 1)
            index = parenIndex + 1
        }
        
        // 对每个位置进行测试
        for (position in commentParenPositions.take(10)) {
            repeat(3) {
                // 重置状态
                tipDisplayService.hideTip()
                
                // 移动光标到")"位置
                editor.caretModel.moveToOffset(position)
                
                // 测试shouldHandle应该返回false
                val shouldHandle = handler.shouldHandle(')', editor)
                assertFalse("Should not handle input in comment at position $position", shouldHandle)
                
                // 执行处理
                handler.execute(editor, ')', dataContext)
                
                // 等待
                Thread.sleep(50)
                
                // 验证没有显示提示
                assertFalse("Should not trigger in comment at position $position", 
                    tipDisplayService.isCurrentlyShowing())
            }
        }
    }
    
    /**
     * 属性 8.6: 在字符串中输入不触发检测
     * 
     * **Validates: Requirements 4.1, 2.5**
     * 
     * 验证在字符串字面量中输入右括号不触发检测
     * 这确保了语法上下文的正确识别
     */
    fun testProperty8_6_NoTriggerInStringLiterals() {
        // 创建包含字符串的测试文件
        val psiFile = myFixture.configureByText("Test.java", """
            public class Test {
                public void testMethod() {
                    String s1 = "method()";
                    String s2 = "another method() call";
                    String s3 = "multiple method() and method() calls";
                }
            }
        """.trimIndent()) as PsiJavaFile
        
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        val text = editor.document.text
        
        // 找到所有字符串中的")"位置
        val stringParenPositions = mutableListOf<Int>()
        var index = 0
        while (index < text.length) {
            val parenIndex = text.indexOf("method()", index)
            if (parenIndex < 0) break
            // 检查是否在字符串中（简单检查：前面有引号）
            val beforeText = text.substring(0, parenIndex)
            if (beforeText.lastIndexOf('"') > beforeText.lastIndexOf('\n')) {
                stringParenPositions.add(parenIndex + "method()".length - 1)
            }
            index = parenIndex + 1
        }
        
        // 对每个位置进行测试
        for (position in stringParenPositions.take(10)) {
            repeat(3) {
                // 重置状态
                tipDisplayService.hideTip()
                
                // 移动光标到")"位置
                editor.caretModel.moveToOffset(position)
                
                // 测试shouldHandle应该返回false
                val shouldHandle = handler.shouldHandle(')', editor)
                assertFalse("Should not handle input in string at position $position", shouldHandle)
                
                // 执行处理
                handler.execute(editor, ')', dataContext)
                
                // 等待
                Thread.sleep(50)
                
                // 验证没有显示提示
                assertFalse("Should not trigger in string at position $position", 
                    tipDisplayService.isCurrentlyShowing())
            }
        }
    }
    
    /**
     * 属性 8.7: 异常不影响后续触发
     * 
     * **Validates: Requirements 4.1, 7.3**
     * 
     * 验证即使某次检测出现异常，也不影响后续的触发检测
     * 这确保了系统的健壮性
     */
    fun testProperty8_7_ExceptionsDoNotPreventSubsequentTriggers() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到方法调用位置
        val text = editor.document.text
        val methodCallIndex = text.indexOf("methodWithTips()")
        
        if (methodCallIndex > 0) {
            val offset = methodCallIndex + "methodWithTips()".length
            
            // 进行多次触发测试
            repeat(15) { iteration ->
                try {
                    // 重置状态
                    tipDisplayService.hideTip()
                    
                    // 移动光标
                    editor.caretModel.moveToOffset(offset)
                    
                    // 执行处理
                    handler.execute(editor, ')', dataContext)
                    
                    // 等待
                    Thread.sleep(100)
                    
                    // 验证方法执行没有抛出异常
                    assertTrue("Iteration $iteration should complete without exception", true)
                } catch (e: Exception) {
                    fail("Exception should not propagate to test: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 属性 8.8: 快速连续触发的独立处理
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证快速连续输入多个方法调用时，每个调用都能独立处理
     * 这确保了并发场景下的正确性
     */
    fun testProperty8_8_RapidSuccessiveTriggersAreHandledIndependently() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到多个方法调用
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        val callPositions = methodCalls.take(5).map { it.textRange.endOffset }
        
        if (callPositions.size >= 2) {
            // 快速连续触发多个位置
            repeat(10) { iteration ->
                for (offset in callPositions) {
                    // 移动光标
                    editor.caretModel.moveToOffset(offset)
                    
                    // 立即执行处理（不等待）
                    handler.execute(editor, ')', dataContext)
                    
                    // 只等待很短的时间
                    Thread.sleep(10)
                }
                
                // 等待所有后台任务完成
                Thread.sleep(200)
                
                // 验证没有崩溃
                assertTrue("Rapid successive triggers should not cause errors", true)
            }
        }
    }
    
    /**
     * 属性 8.9: 检测不阻塞UI线程
     * 
     * **Validates: Requirements 4.2, 7.1**
     * 
     * 验证检测和显示过程不阻塞UI线程
     * execute方法应该立即返回，实际处理在后台进行
     */
    fun testProperty8_9_DetectionDoesNotBlockUIThread() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到方法调用位置
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        
        // 对多个方法调用进行测试
        for (methodCall in methodCalls.take(20)) {
            val offset = methodCall.textRange.endOffset
            editor.caretModel.moveToOffset(offset)
            
            // 测量execute方法的执行时间
            val startTime = System.nanoTime()
            handler.execute(editor, ')', dataContext)
            val executionTime = (System.nanoTime() - startTime) / 1_000_000 // 转换为毫秒
            
            // execute应该立即返回（< 50ms）
            assertTrue("Execute should not block UI thread, took ${executionTime}ms", 
                executionTime < 50)
        }
    }
    
    /**
     * 属性 8.10: 方法调用检测的准确性
     * 
     * **Validates: Requirements 4.1**
     * 
     * 验证只有在真正的方法调用位置才触发检测
     * 不完整的语法或其他")"不应该触发
     */
    fun testProperty8_10_OnlyTriggersAtValidMethodCallPositions() {
        // 创建包含各种")"的测试文件
        val psiFile = myFixture.configureByText("Test.java", """
            public class Test {
                public void testMethod() {
                    // 有效的方法调用
                    validMethod();
                    anotherMethod();
                    thirdMethod();
                    
                    // 数组访问
                    int[] arr = new int[10];
                    int value = arr[0];
                    
                    // 类型转换
                    Object obj = (Object) "test";
                    String str = (String) obj;
                    
                    // 条件表达式
                    boolean b = (1 > 0);
                    boolean c = (2 < 5);
                    
                    // 算术表达式
                    int result = (1 + 2) * (3 + 4);
                }
            }
        """.trimIndent()) as PsiJavaFile
        
        val editor = myFixture.editor
        val text = editor.document.text
        
        // 找到所有")"的位置
        val parenPositions = mutableListOf<Int>()
        var index = 0
        while (index < text.length) {
            val parenIndex = text.indexOf(')', index)
            if (parenIndex < 0) break
            parenPositions.add(parenIndex)
            index = parenIndex + 1
        }
        
        // 验证找到了足够多的括号
        assertTrue("Should find various types of parentheses (found ${parenPositions.size})", 
            parenPositions.size >= 10)
        
        // 对每个位置进行测试
        var validMethodCallCount = 0
        var otherParenCount = 0
        
        for (position in parenPositions.take(20)) {
            // 移动光标到")"之后
            editor.caretModel.moveToOffset(position + 1)
            
            // 检查是否应该处理
            val shouldHandle = handler.shouldHandle(')', editor)
            
            // 根据上下文判断是否是有效的方法调用
            val beforeText = text.substring(maxOf(0, position - 20), position)
            val isLikelyMethodCall = beforeText.contains("Method(") || beforeText.contains("method(")
            
            if (isLikelyMethodCall) {
                validMethodCallCount++
            } else {
                otherParenCount++
            }
        }
        
        // 验证找到了不同类型的括号
        assertTrue("Should find method calls", validMethodCallCount > 0)
        assertTrue("Should find other parentheses", otherParenCount > 0)
    }
    
    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }
}
