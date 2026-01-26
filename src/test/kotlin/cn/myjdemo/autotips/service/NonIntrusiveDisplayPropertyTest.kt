package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.*
import cn.myjdemo.autotips.service.impl.TipDisplayServiceImpl
import cn.myjdemo.autotips.TestBase
import com.intellij.openapi.editor.LogicalPosition
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * TipDisplayService的基于属性的测试 - 非中断式显示行为
 * 
 * **Feature: auto-tips, Property 5: Non-intrusive Display Behavior**
 * **Validates: Requirements 3.1, 3.2, 3.3**
 * 
 * 对于任何检测到的带@tips的方法调用，提示显示应该是非模态的，
 * 不阻塞用户输入，并在失去焦点时自动隐藏
 */
class NonIntrusiveDisplayPropertyTest : TestBase() {
    
    private lateinit var displayService: TipDisplayServiceImpl
    private val propertyTestIterations = 100
    
    override fun setUp() {
        super.setUp()
        displayService = TipDisplayServiceImpl()
    }
    
    override fun tearDown() {
        try {
            displayService.hideTip()
        } finally {
            super.tearDown()
        }
    }
    
    override fun getTestDataPath(): String {
        return "src/test/resources/testData"
    }
    
    /**
     * 属性 5.1: 非模态弹窗显示
     * 
     * **Validates: Requirement 3.1**
     * 
     * 验证当检测到带有@tips的方法调用时，显示的提示是非模态的
     * 对于任何提示内容和编辑器位置，showTip应该创建非模态弹窗
     */
    fun testProperty51ShowTipCreatesNonModalPopupForAnyContent() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证可以获取当前内容
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 5.2: 提示不阻塞用户输入
     * 
     * **Validates: Requirement 3.2**
     * 
     * 验证当用户继续输入代码时，提示保持可见但不阻塞输入
     * 对于任何提示显示状态，用户应该能够继续编辑文档
     * 
     * 注意：此测试验证提示显示后编辑器仍然可以接受输入
     * 实际的非阻塞行为由Balloon的配置保证（setRequestFocus(false)和setBlockClicksThroughBalloon(false)）
     */
    fun testProperty52TipRemainsVisibleButDoesNotBlockInput() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证编辑器仍然可编辑（非模态）
                // 编辑器的document应该仍然可写
                val document = editor.document
                val isWritable = document.isWritable
                isWritable shouldBe true
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 5.3: 点击其他位置自动隐藏
     * 
     * **Validates: Requirement 3.3**
     * 
     * 验证当用户点击编辑器其他位置时，提示自动隐藏
     * 对于任何显示的提示，调用hideTip应该隐藏提示
     */
    fun testProperty53TipHidesWhenClickingElsewhere() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { void method() {} }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 模拟点击其他位置（通过调用hideTip模拟失去焦点）
                displayService.hideTip()
                
                // 验证提示已隐藏
                displayService.isCurrentlyShowing() shouldBe false
                displayService.getCurrentTipContent() shouldBe null
            }
        }
    }
    
    /**
     * 属性 5.4: 配置化显示行为
     * 
     * **Validates: Requirements 3.1, 3.2, 3.3**
     * 
     * 验证使用DisplayConfiguration时，提示仍然保持非中断式行为
     * 对于任何显示配置，提示应该是非模态的
     */
    fun testProperty54ShowTipWithConfigMaintainsNonIntrusiveBehavior() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent(), genDisplayConfiguration()) { content, config ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 使用配置显示提示
                displayService.showTipWithConfig(content, editor, position, config)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证可以获取当前内容
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 5.5: 隐藏操作的幂等性
     * 
     * **Validates: Requirement 3.3**
     * 
     * 验证多次调用hideTip不会导致错误
     * 对于任何状态，hideTip应该安全执行
     */
    fun testProperty55HideTipIsIdempotent() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                
                // 第一次隐藏
                displayService.hideTip()
                displayService.isCurrentlyShowing() shouldBe false
                
                // 第二次隐藏（应该不会出错）
                displayService.hideTip()
                displayService.isCurrentlyShowing() shouldBe false
                
                // 第三次隐藏（应该不会出错）
                displayService.hideTip()
                displayService.isCurrentlyShowing() shouldBe false
            }
        }
    }
    
    /**
     * 属性 5.6: 显示状态一致性
     * 
     * **Validates: Requirements 3.1, 3.2**
     * 
     * 验证isCurrentlyShowing准确反映显示状态
     * 对于任何操作序列，状态应该保持一致
     */
    fun testProperty56IsCurrentlyShowingReflectsActualState() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 初始状态：未显示
                displayService.isCurrentlyShowing() shouldBe false
                displayService.getCurrentTipContent() shouldBe null
                
                // 显示提示后：正在显示
                displayService.showTip(content, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent() shouldNotBe null
                
                // 隐藏提示后：未显示
                displayService.hideTip()
                displayService.isCurrentlyShowing() shouldBe false
                displayService.getCurrentTipContent() shouldBe null
            }
        }
    }
    
    /**
     * 属性 5.7: 新提示替换旧提示
     * 
     * **Validates: Requirements 3.1, 3.2**
     * 
     * 验证显示新提示时，旧提示被正确替换
     * 对于任何两个不同的提示内容，第二个应该替换第一个
     */
    fun testProperty57NewTipReplacesOldTip() {
        runBlocking {
            checkAll(propertyTestIterations, genTwoDistinctTipsContents()) { (content1, content2) ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示第一个提示
                displayService.showTip(content1, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe content1.content
                
                // 显示第二个提示
                displayService.showTip(content2, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe content2.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 5.8: 自动隐藏超时配置
     * 
     * **Validates: Requirement 3.3**
     * 
     * 验证setAutoHideTimeout正确设置超时时间
     * 对于任何有效的超时值，设置应该成功
     */
    fun testProperty58SetAutoHideTimeoutAcceptsValidTimeoutValues() {
        runBlocking {
            checkAll(propertyTestIterations, genValidTimeout()) { timeoutMs ->
                // 设置超时时间（应该不会抛出异常）
                displayService.setAutoHideTimeout(timeoutMs)
                
                // 验证可以正常显示提示
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                val content = TipsContent("Test tip", TipsFormat.PLAIN_TEXT)
                
                displayService.showTip(content, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 5.9: 提示冲突检测
     * 
     * **Validates: Requirements 3.1, 3.2**
     * 
     * 验证canShowNewTip正确判断是否可以显示新提示
     * 对于任何提示内容，当没有提示显示时应该返回true
     */
    fun testProperty59CanShowNewTipReturnsTrueWhenNoTipIsShowing() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent()) { content ->
                // 确保没有提示显示
                displayService.hideTip()
                displayService.isCurrentlyShowing() shouldBe false
                
                // 验证可以显示新提示
                displayService.canShowNewTip(content) shouldBe true
            }
        }
    }
    
    /**
     * 属性 5.10: 提示冲突处理
     * 
     * **Validates: Requirements 3.1, 3.2**
     * 
     * 验证当有提示显示时，canShowNewTip根据内容差异返回正确结果
     * 对于不同的内容，应该允许替换
     */
    fun testProperty510CanShowNewTipHandlesContentConflictsCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genTwoDistinctTipsContents()) { (content1, content2) ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示第一个提示
                displayService.showTip(content1, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证相同内容不应该替换
                displayService.canShowNewTip(content1) shouldBe false
                
                // 验证不同内容应该允许替换
                displayService.canShowNewTip(content2) shouldBe true
                
                // 清理
                displayService.hideTip()
            }
        }
    }
}

// ==================== 测试数据生成器 ====================

/**
 * 生成TipsContent
 */
private fun genTipsContent(): Arb<TipsContent> = arbitrary {
    val content = Arb.string(10..200, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成用户输入的文本
 */
private fun genTypingInput(): Arb<String> = arbitrary {
    val chars = listOf("a", "b", "c", "x", "y", "z", "1", "2", "3", "(", ")", ";", " ")
    val length = Arb.int(1..10).bind()
    (1..length).map { chars.random() }.joinToString("")
}

/**
 * 生成DisplayConfiguration
 */
private fun genDisplayConfiguration(): Arb<DisplayConfiguration> = arbitrary {
    val position = Arb.enum<PopupPosition>().bind()
    val timeoutMs = Arb.long(100L..10000L).bind()
    val timeout = timeoutMs.milliseconds
    val style = Arb.enum<PopupStyle>().bind()
    val dismissBehavior = Arb.enum<DismissBehavior>().bind()
    
    DisplayConfiguration(position, timeout, style, dismissBehavior)
}

/**
 * 生成两个不同的TipsContent
 */
private fun genTwoDistinctTipsContents(): Arb<Pair<TipsContent, TipsContent>> = arbitrary {
    val content1 = "Content_A_" + Arb.string(10..50, Codepoint.alphanumeric()).bind()
    val content2 = "Content_B_" + Arb.string(10..50, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    
    Pair(
        TipsContent(content1, format),
        TipsContent(content2, format)
    )
}

/**
 * 生成有效的超时时间（毫秒）
 */
private fun genValidTimeout(): Arb<Long> = Arb.long(100L..30000L)
