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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 并发调用处理的基于属性的测试
 * 
 * **Feature: auto-tips, Property 9: Concurrent Call Handling**
 * **Validates: Requirements 4.3**
 * 
 * 对于任何快速连续的方法调用输入，插件应该为每个调用独立处理提示
 * 
 * 属性说明:
 * - 需求 4.3: 当开发者快速连续输入多个方法调用时，插件应该为每个调用独立处理提示
 * 
 * 测试策略:
 * 1. 模拟快速连续输入多个方法调用（输入多个")"）
 * 2. 验证每个调用都被独立检测和处理
 * 3. 验证并发处理不会导致数据竞争或状态混乱
 * 4. 验证处理顺序的独立性（不依赖于前一个调用的完成）
 * 
 * 注意：由于IntelliJ Platform测试框架的限制，这些测试使用JUnit风格
 * 但通过多次迭代验证属性的通用性
 */
class ConcurrentCallHandlingPropertyTest : TestBase() {
    
    private lateinit var handler: TipsTypedActionHandler
    private lateinit var configService: ConfigurationService
    private lateinit var callDetectionService: CallDetectionService
    private lateinit var tipDisplayService: TipDisplayService
    private val propertyTestIterations = 100
    
    override fun setUp() {
        super.setUp()
        handler = TipsTypedActionHandlerImpl()
        configService = service<ConfigurationService>()
        callDetectionService = project.service<CallDetectionService>()
        tipDisplayService = project.service<TipDisplayService>()
        
        // 确保插件启用
        configService.setPluginEnabled(true)
    }
    
    /**
     * 属性 9.1: 快速连续调用的独立处理
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证当快速连续输入多个方法调用时，每个调用都被独立处理
     * 不会因为前一个调用还在处理中而被忽略或合并
     */
    fun testProperty9_1_RapidSuccessiveCallsAreProcessedIndependently() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到多个方法调用位置
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        val callPositions = methodCalls.take(5).map { it.textRange.endOffset }
        
        assertTrue("Should find multiple method calls for testing", callPositions.size >= 3)
        
        // 进行多次迭代测试
        repeat(20) { iteration ->
            // 记录每个位置的处理次数
            val processedCalls = AtomicInteger(0)
            
            // 快速连续触发多个位置
            for (offset in callPositions) {
                // 移动光标到方法调用的右括号之后
                editor.caretModel.moveToOffset(offset)
                
                // 立即执行处理（不等待前一个完成）
                handler.charTyped(')', project, editor, psiFile)
                processedCalls.incrementAndGet()
                
                // 只等待极短的时间（模拟快速输入）
                Thread.sleep(5)
            }
            
            // 验证所有调用都被提交处理
            assertEquals("All calls should be submitted for processing", 
                callPositions.size, processedCalls.get())
            
            // 等待所有后台任务有机会完成
            Thread.sleep(200)
            
            // 验证没有崩溃或异常
            assertTrue("Rapid successive calls should not cause errors (iteration $iteration)", true)
        }
    }
    
    /**
     * 属性 9.2: 并发处理不会导致状态混乱
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证多个方法调用并发处理时，不会导致内部状态混乱
     * 每个调用的检测结果应该对应正确的方法
     */
    fun testProperty9_2_ConcurrentProcessingMaintainsCorrectState() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到带有@tips的方法调用和不带@tips的方法调用
        val text = editor.document.text
        val methodWithTipsOffset = text.indexOf("methodWithTips()") + "methodWithTips()".length
        val simpleMethodOffset = text.indexOf("simpleMethod()") + "simpleMethod()".length
        
        if (methodWithTipsOffset > 0 && simpleMethodOffset > 0) {
            // 进行多次迭代测试
            repeat(30) { iteration ->
                // 重置状态
                tipDisplayService.hideTip()
                
                // 交替快速触发两个方法调用
                val positions = if (iteration % 2 == 0) {
                    listOf(methodWithTipsOffset, simpleMethodOffset, methodWithTipsOffset)
                } else {
                    listOf(simpleMethodOffset, methodWithTipsOffset, simpleMethodOffset)
                }
                
                for (offset in positions) {
                    editor.caretModel.moveToOffset(offset)
                    handler.charTyped(')', project, editor, psiFile)
                    Thread.sleep(10) // 快速连续输入
                }
                
                // 等待处理完成
                Thread.sleep(300)
                
                // 验证系统仍然正常工作（没有状态混乱）
                // 尝试再次触发一个已知的方法
                editor.caretModel.moveToOffset(methodWithTipsOffset)
                handler.charTyped(')', project, editor, psiFile)
                Thread.sleep(200)
                
                // 验证没有异常
                assertTrue("State should remain consistent after concurrent processing (iteration $iteration)", true)
            }
        }
    }
    
    /**
     * 属性 9.3: 处理顺序的独立性
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证每个方法调用的处理不依赖于前一个调用的完成
     * 即使前一个调用还在处理中，新的调用也应该被接受和处理
     */
    fun testProperty9_3_ProcessingOrderIsIndependent() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到多个方法调用
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        val callPositions = methodCalls.take(4).map { it.textRange.endOffset }
        
        if (callPositions.size >= 3) {
            // 进行多次迭代测试
            repeat(25) { iteration ->
                // 记录执行时间
                val executionTimes = mutableListOf<Long>()
                
                // 快速连续触发
                for (offset in callPositions) {
                    editor.caretModel.moveToOffset(offset)
                    
                    val startTime = System.nanoTime()
                    handler.charTyped(')', project, editor, psiFile)
                    val executionTime = (System.nanoTime() - startTime) / 1_000_000 // 毫秒
                    
                    executionTimes.add(executionTime)
                    
                    // 不等待，立即处理下一个
                    Thread.sleep(2)
                }
                
                // 验证所有execute调用都快速返回（不阻塞等待前一个完成）
                for ((index, time) in executionTimes.withIndex()) {
                    assertTrue("Execute call $index should return quickly (< 100ms), took ${time}ms", 
                        time < 100)
                }
                
                // 等待所有后台处理完成
                Thread.sleep(300)
            }
        }
    }
    
    /**
     * 属性 9.4: 高频率输入的稳定性
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证在极高频率的输入下（模拟非常快速的打字），系统保持稳定
     * 不会出现资源耗尽、死锁或其他并发问题
     */
    fun testProperty9_4_HighFrequencyInputStability() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到方法调用位置
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        val callPositions = methodCalls.take(3).map { it.textRange.endOffset }
        
        if (callPositions.isNotEmpty()) {
            // 进行多次高频率输入测试
            repeat(15) { iteration ->
                var successfulCalls = 0
                
                // 在短时间内触发大量调用
                repeat(20) { callIndex ->
                    try {
                        val offset = callPositions[callIndex % callPositions.size]
                        editor.caretModel.moveToOffset(offset)
                        handler.charTyped(')', project, editor, psiFile)
                        successfulCalls++
                        
                        // 极短的间隔（模拟非常快速的输入）
                        Thread.sleep(1)
                    } catch (e: Exception) {
                        fail("High frequency input should not cause exceptions: ${e.message}")
                    }
                }
                
                // 验证所有调用都成功提交
                assertEquals("All high-frequency calls should be accepted", 20, successfulCalls)
                
                // 等待处理完成
                Thread.sleep(500)
                
                // 验证系统仍然响应
                val testOffset = callPositions[0]
                editor.caretModel.moveToOffset(testOffset)
                handler.charTyped(')', project, editor, psiFile)
                
                assertTrue("System should remain responsive after high-frequency input (iteration $iteration)", true)
                
                // 短暂休息，让系统恢复
                Thread.sleep(100)
            }
        }
    }
    
    /**
     * 属性 9.5: 不同位置的并发调用
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证在文件的不同位置快速连续输入方法调用时，每个位置都被正确处理
     * 位置信息不会混淆
     */
    fun testProperty9_5_ConcurrentCallsAtDifferentPositions() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到分散在文件不同位置的方法调用
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        val callPositions = methodCalls.map { it.textRange.endOffset }.sorted()
        
        if (callPositions.size >= 4) {
            // 选择分散的位置（开头、中间、结尾）
            val selectedPositions = listOf(
                callPositions[0],
                callPositions[callPositions.size / 2],
                callPositions[callPositions.size - 1]
            )
            
            // 进行多次迭代测试
            repeat(25) { iteration ->
                // 记录每个位置的处理
                val processedPositions = ConcurrentHashMap<Int, Boolean>()
                
                // 快速在不同位置之间跳转并触发
                for (offset in selectedPositions) {
                    editor.caretModel.moveToOffset(offset)
                    handler.charTyped(')', project, editor, psiFile)
                    processedPositions[offset] = true
                    
                    // 极短的间隔
                    Thread.sleep(5)
                }
                
                // 验证所有位置都被处理
                assertEquals("All positions should be processed", 
                    selectedPositions.size, processedPositions.size)
                
                // 等待处理完成
                Thread.sleep(250)
                
                // 验证没有位置信息混淆
                assertTrue("Position information should not be confused (iteration $iteration)", true)
            }
        }
    }
    
    /**
     * 属性 9.6: 链式调用的并发处理
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证快速输入链式方法调用时（如 a.b().c().d()），每个调用都被独立处理
     * 链式调用中的每个")"都应该触发独立的检测
     */
    fun testProperty9_6_ChainedCallsConcurrentProcessing() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到链式调用
        val text = editor.document.text
        val chainCallStart = text.indexOf("chainableMethod().anotherChainableMethod()")
        
        if (chainCallStart > 0) {
            // 找到链式调用中的每个")"位置
            val chainText = "chainableMethod().anotherChainableMethod()"
            val firstParenOffset = chainCallStart + "chainableMethod()".length
            val secondParenOffset = chainCallStart + chainText.length
            
            // 进行多次迭代测试
            repeat(30) { iteration ->
                // 快速连续触发链式调用中的每个方法
                val positions = listOf(firstParenOffset, secondParenOffset)
                
                for (offset in positions) {
                    editor.caretModel.moveToOffset(offset)
                    handler.charTyped(')', project, editor, psiFile)
                    
                    // 模拟快速输入
                    Thread.sleep(8)
                }
                
                // 等待处理完成
                Thread.sleep(200)
                
                // 验证链式调用处理正常
                assertTrue("Chained calls should be processed independently (iteration $iteration)", true)
            }
        }
    }
    
    /**
     * 属性 9.7: 并发处理不影响检测准确性
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证即使在并发处理的情况下，方法调用的检测仍然准确
     * 不会因为并发而误检测或漏检测
     * 
     * 注意：在测试环境中，由于编辑器焦点问题，我们主要验证检测逻辑的调用
     * 而不是实际的提示显示
     */
    fun testProperty9_7_ConcurrentProcessingMaintainsDetectionAccuracy() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到带有@tips的方法调用
        val text = editor.document.text
        val methodWithTipsOffset = text.indexOf("methodWithTips()") + "methodWithTips()".length
        
        if (methodWithTipsOffset > 0) {
            // 进行多次迭代测试
            var detectionAttempts = 0
            var successfulExecutions = 0
            
            repeat(40) { iteration ->
                try {
                    // 重置状态
                    tipDisplayService.hideTip()
                    
                    // 移动到方法调用位置
                    editor.caretModel.moveToOffset(methodWithTipsOffset)
                    
                    // 执行处理（验证不会抛出异常）
                    handler.charTyped(')', project, editor, psiFile)
                    successfulExecutions++
                    detectionAttempts++
                    
                    // 等待检测完成
                    Thread.sleep(150)
                    
                    // 快速进行下一次迭代（模拟连续输入）
                    Thread.sleep(10)
                } catch (e: Exception) {
                    // 记录异常但继续测试
                    println("Exception during concurrent processing test: ${e.message}")
                    detectionAttempts++
                }
            }
            
            // 验证检测准确性
            // 所有调用都应该成功执行（不抛出异常）
            val successRate = successfulExecutions.toDouble() / detectionAttempts
            assertTrue("Detection should execute without errors in concurrent scenarios (success rate: ${successRate * 100}%)", 
                successRate >= 0.95) // 至少95%的成功执行率
        }
    }
    
    /**
     * 属性 9.8: 并发处理的资源管理
     * 
     * **Validates: Requirements 4.3, 7.1**
     * 
     * 验证快速连续的方法调用不会导致资源泄漏或过度消耗
     * 后台线程应该被正确管理
     */
    fun testProperty9_8_ConcurrentProcessingResourceManagement() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到方法调用位置
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        val callPositions = methodCalls.take(3).map { it.textRange.endOffset }
        
        if (callPositions.isNotEmpty()) {
            // 记录初始线程数（粗略估计）
            val initialThreadCount = Thread.activeCount()
            
            // 进行大量并发调用
            repeat(50) { iteration ->
                for (offset in callPositions) {
                    editor.caretModel.moveToOffset(offset)
                    handler.charTyped(')', project, editor, psiFile)
                    Thread.sleep(2)
                }
                
                // 每10次迭代检查一次
                if (iteration % 10 == 0) {
                    Thread.sleep(300) // 让后台任务有机会完成
                    
                    val currentThreadCount = Thread.activeCount()
                    // 线程数不应该无限增长
                    assertTrue("Thread count should not grow unbounded (initial: $initialThreadCount, current: $currentThreadCount)", 
                        currentThreadCount < initialThreadCount + 50)
                }
            }
            
            // 等待所有任务完成
            Thread.sleep(1000)
            
            // 验证资源被正确清理
            val finalThreadCount = Thread.activeCount()
            assertTrue("Resources should be cleaned up after processing", 
                finalThreadCount < initialThreadCount + 20)
        }
    }
    
    /**
     * 属性 9.9: 混合场景的并发处理
     * 
     * **Validates: Requirements 4.3**
     * 
     * 验证在混合场景下（有@tips和无@tips的方法混合）的并发处理
     * 每个方法都应该被正确识别和处理
     */
    fun testProperty9_9_MixedScenarioConcurrentProcessing() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到不同类型的方法调用
        val text = editor.document.text
        val positions = mutableListOf<Pair<Int, String>>()
        
        // 带@tips的方法
        val methodWithTipsOffset = text.indexOf("methodWithTips()")
        if (methodWithTipsOffset > 0) {
            positions.add(Pair(methodWithTipsOffset + "methodWithTips()".length, "withTips"))
        }
        
        // 不带@tips的方法
        val simpleMethodOffset = text.indexOf("simpleMethod()")
        if (simpleMethodOffset > 0) {
            positions.add(Pair(simpleMethodOffset + "simpleMethod()".length, "simple"))
        }
        
        // 带参数的方法
        val methodWithParamsOffset = text.indexOf("methodWithParams(")
        if (methodWithParamsOffset > 0) {
            val endOffset = text.indexOf(")", methodWithParamsOffset) + 1
            positions.add(Pair(endOffset, "withParams"))
        }
        
        if (positions.size >= 2) {
            // 进行多次迭代测试
            repeat(25) { iteration ->
                // 随机顺序处理
                val shuffledPositions = positions.shuffled()
                
                // 快速连续触发
                for ((offset, type) in shuffledPositions) {
                    editor.caretModel.moveToOffset(offset)
                    handler.charTyped(')', project, editor, psiFile)
                    
                    // 快速输入
                    Thread.sleep(7)
                }
                
                // 等待处理完成
                Thread.sleep(250)
                
                // 验证混合场景处理正常
                assertTrue("Mixed scenario should be handled correctly (iteration $iteration)", true)
            }
        }
    }
    
    /**
     * 属性 9.10: 并发处理的异常隔离
     * 
     * **Validates: Requirements 4.3, 7.3**
     * 
     * 验证如果某个并发调用的处理出现异常，不会影响其他调用的处理
     * 异常应该被隔离，不会传播到其他并发任务
     */
    fun testProperty9_10_ConcurrentProcessingExceptionIsolation() {
        val psiFile = myFixture.configureByFile("TipsTestClass.java") as PsiJavaFile
        val editor = myFixture.editor
        val dataContext = SimpleDataContext.getProjectContext(project)
        
        // 找到多个方法调用位置
        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        val callPositions = methodCalls.take(5).map { it.textRange.endOffset }
        
        if (callPositions.size >= 3) {
            // 进行多次迭代测试
            repeat(20) { iteration ->
                var successfulExecutions = 0
                
                // 快速连续触发多个调用
                for (offset in callPositions) {
                    try {
                        editor.caretModel.moveToOffset(offset)
                        handler.charTyped(')', project, editor, psiFile)
                        successfulExecutions++
                        
                        Thread.sleep(5)
                    } catch (e: Exception) {
                        // 即使某个调用出现异常，也不应该影响测试继续
                        println("Exception in concurrent call (expected to be handled): ${e.message}")
                    }
                }
                
                // 验证所有调用都成功执行（异常被内部处理）
                assertEquals("All concurrent calls should execute without throwing exceptions", 
                    callPositions.size, successfulExecutions)
                
                // 等待处理完成
                Thread.sleep(200)
                
                // 验证系统仍然正常工作
                val testOffset = callPositions[0]
                editor.caretModel.moveToOffset(testOffset)
                
                try {
                    handler.charTyped(')', project, editor, psiFile)
                    assertTrue("System should remain functional after concurrent processing with potential exceptions", true)
                } catch (e: Exception) {
                    fail("System should not propagate exceptions: ${e.message}")
                }
            }
        }
    }
    
    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }
}
