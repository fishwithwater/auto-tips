package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.*
import cn.myjdemo.autotips.service.TipDisplayService
import cn.myjdemo.autotips.service.impl.TipDisplayServiceImpl
import cn.myjdemo.autotips.TestBase
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.LogicalPosition
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * TipDisplayService的基于属性的测试 - 提示冲突处理
 * 
 * **Feature: auto-tips, Property 7: Tip Conflict Handling**
 * **Validates: Requirement 3.5**
 * 
 * 对于任何同时触发的多个提示，系统应该只显示最相关的一个
 */
class TipConflictHandlingPropertyTest : TestBase() {
    
    private lateinit var displayService: TipDisplayServiceImpl
    private val propertyTestIterations = 100
    
    override fun setUp() {
        super.setUp()
        displayService = project.service<TipDisplayService>() as TipDisplayServiceImpl
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
     * 属性 7.1: 单一提示显示原则
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证在任何时刻，系统最多只显示一个提示
     * 对于任何提示内容序列，isCurrentlyShowing应该反映只有一个提示在显示
     */
    fun testProperty71OnlyOneTipIsShownAtAnyTime() {
        runBlocking {
            checkAll(propertyTestIterations, genMultipleTipsContents()) { tipsContents ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 初始状态：没有提示显示
                displayService.isCurrentlyShowing() shouldBe false
                
                // 依次尝试显示多个提示
                for (content in tipsContents) {
                    displayService.showTip(content, editor, position)
                    
                    // 验证只有一个提示在显示
                    displayService.isCurrentlyShowing() shouldBe true
                    
                    // 验证当前显示的是最后一个提示
                    val currentContent = displayService.getCurrentTipContent()
                    currentContent shouldNotBe null
                    currentContent?.content shouldBe content.content
                }
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.2: 新提示自动替换旧提示
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证当新提示触发时，旧提示自动被替换
     * 对于任何两个不同的提示，第二个提示应该替换第一个
     */
    fun testProperty72NewTipAutomaticallyReplacesOldTip() {
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
                
                // 显示第二个提示（应该自动替换第一个）
                displayService.showTip(content2, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe content2.content
                
                // 验证第一个提示不再显示（通过内容验证）
                displayService.getCurrentTipContent()?.content shouldNotBe content1.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.3: 相同内容的提示不重复显示
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证当相同内容的提示再次触发时，不会重复显示
     * 对于任何提示内容，连续显示相同内容应该被识别并处理
     */
    fun testProperty73SameContentTipIsNotDuplicated() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示第一次提示
                displayService.showTip(content, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                val firstContent = displayService.getCurrentTipContent()
                
                // 尝试显示相同内容的提示
                displayService.showTip(content, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                val secondContent = displayService.getCurrentTipContent()
                
                // 验证内容相同
                secondContent?.content shouldBe firstContent?.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.4: canShowNewTip正确判断冲突
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证canShowNewTip方法能够正确判断是否可以显示新提示
     * 对于任何提示状态，该方法应该返回正确的判断结果
     */
    fun testProperty74CanShowNewTipCorrectlyDetectsConflicts() {
        runBlocking {
            checkAll(propertyTestIterations, genTwoDistinctTipsContents()) { (content1, content2) ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 初始状态：没有提示，应该可以显示任何新提示
                displayService.canShowNewTip(content1) shouldBe true
                displayService.canShowNewTip(content2) shouldBe true
                
                // 显示第一个提示
                displayService.showTip(content1, editor, position)
                
                // 相同内容不应该重复显示
                displayService.canShowNewTip(content1) shouldBe false
                
                // 不同内容应该可以替换
                displayService.canShowNewTip(content2) shouldBe true
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.5: 快速连续的提示触发
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证快速连续触发多个提示时，系统能够正确处理冲突
     * 对于任何提示序列，最后一个提示应该是最终显示的
     */
    fun testProperty75RapidSuccessiveTipsAreHandledCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genMultipleTipsContents()) { tipsContents ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 快速连续显示多个提示
                var lastContent: TipsContent? = null
                for (content in tipsContents) {
                    displayService.showTip(content, editor, position)
                    lastContent = content
                }
                
                // 验证最后一个提示是当前显示的
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe lastContent?.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.6: 不同位置的提示冲突
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证在不同编辑器位置触发的提示也遵循单一显示原则
     * 对于任何位置序列，只有最后一个位置的提示应该显示
     */
    fun testProperty76TipsAtDifferentPositionsFollowSingleDisplayRule() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContent(), genMultiplePositions()) { content, positions ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test {\n  void method1() {}\n  void method2() {}\n}")
                val editor = myFixture.editor
                
                // 在不同位置显示提示
                var lastPosition: LogicalPosition? = null
                for (position in positions) {
                    displayService.showTip(content, editor, position)
                    lastPosition = position
                    
                    // 验证只有一个提示在显示
                    displayService.isCurrentlyShowing() shouldBe true
                }
                
                // 验证提示仍在显示
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.7: 配置化显示的冲突处理
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证使用DisplayConfiguration显示提示时，冲突处理规则仍然适用
     * 对于任何配置，单一显示原则应该保持
     */
    fun testProperty77ConfiguredTipsFollowConflictHandlingRules() {
        runBlocking {
            checkAll(propertyTestIterations, genTwoDistinctTipsContents(), genDisplayConfiguration()) { (content1, content2), config ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 使用配置显示第一个提示
                displayService.showTipWithConfig(content1, editor, position, config)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe content1.content
                
                // 使用配置显示第二个提示（应该替换第一个）
                displayService.showTipWithConfig(content2, editor, position, config)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe content2.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.8: 隐藏后可以显示新提示
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证隐藏提示后，可以显示新的提示，不存在冲突
     * 对于任何提示序列，隐藏后应该清除冲突状态
     */
    fun testProperty78AfterHidingNewTipsCanBeShown() {
        runBlocking {
            checkAll(propertyTestIterations, genTwoDistinctTipsContents()) { (content1, content2) ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示第一个提示
                displayService.showTip(content1, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                
                // 隐藏提示
                displayService.hideTip()
                displayService.isCurrentlyShowing() shouldBe false
                
                // 验证可以显示新提示
                displayService.canShowNewTip(content2) shouldBe true
                
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
     * 属性 7.9: 空内容提示的冲突处理
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证空内容或极短内容的提示也遵循冲突处理规则
     * 对于任何内容长度，冲突处理应该一致
     */
    fun testProperty79EmptyOrShortContentFollowsConflictRules() {
        runBlocking {
            checkAll(propertyTestIterations, genShortOrEmptyTipsContent(), genTipsContent()) { shortContent, normalContent ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示短内容提示
                displayService.showTip(shortContent, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                
                // 显示正常内容提示（应该替换短内容）
                displayService.showTip(normalContent, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe normalContent.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 7.10: 多种格式提示的冲突处理
     * 
     * **Validates: Requirement 3.5**
     * 
     * 验证不同格式（PLAIN_TEXT, HTML, MARKDOWN）的提示冲突处理一致
     * 对于任何格式组合，冲突处理规则应该相同
     */
    fun testProperty710DifferentFormatTipsFollowSameConflictRules() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContentWithFormat(TipsFormat.PLAIN_TEXT), 
                     genTipsContentWithFormat(TipsFormat.HTML)) { plainContent, htmlContent ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示纯文本提示
                displayService.showTip(plainContent, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.format shouldBe TipsFormat.PLAIN_TEXT
                
                // 显示HTML提示（应该替换纯文本）
                displayService.showTip(htmlContent, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.format shouldBe TipsFormat.HTML
                
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
 * 生成多个TipsContent（3-5个）
 */
private fun genMultipleTipsContents(): Arb<List<TipsContent>> = arbitrary {
    val count = Arb.int(3..5).bind()
    (1..count).map {
        val content = "Tip_${it}_" + Arb.string(10..50, Codepoint.alphanumeric()).bind()
        val format = Arb.enum<TipsFormat>().bind()
        TipsContent(content, format)
    }
}

/**
 * 生成多个LogicalPosition（2-4个）
 */
private fun genMultiplePositions(): Arb<List<LogicalPosition>> = arbitrary {
    val count = Arb.int(2..4).bind()
    (1..count).map {
        val line = Arb.int(0..2).bind()
        val column = Arb.int(0..20).bind()
        LogicalPosition(line, column)
    }
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
 * 生成短内容或空内容的TipsContent
 */
private fun genShortOrEmptyTipsContent(): Arb<TipsContent> = arbitrary {
    val content = Arb.string(0..5, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成指定格式的TipsContent
 */
private fun genTipsContentWithFormat(format: TipsFormat): Arb<TipsContent> = arbitrary {
    val content = when (format) {
        TipsFormat.PLAIN_TEXT -> Arb.string(10..100, Codepoint.alphanumeric()).bind()
        TipsFormat.HTML -> "<p>" + Arb.string(10..100, Codepoint.alphanumeric()).bind() + "</p>"
        TipsFormat.MARKDOWN -> "**" + Arb.string(10..100, Codepoint.alphanumeric()).bind() + "**"
    }
    TipsContent(content, format)
}
