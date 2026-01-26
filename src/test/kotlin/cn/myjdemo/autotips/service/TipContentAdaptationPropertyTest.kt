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

/**
 * TipDisplayService的基于属性的测试 - 提示内容适配
 * 
 * **Feature: auto-tips, Property 6: Tip Content Adaptation**
 * **Validates: Requirement 3.4**
 * 
 * 对于任何长度的提示内容，显示组件应该提供适当的显示方式（滚动、截断等）
 */
class TipContentAdaptationPropertyTest : TestBase() {
    
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
     * 属性 6.1: 短内容正常显示
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于短内容，提示能够正常显示
     * 对于任何短内容（少于100个字符），显示应该成功且内容完整
     */
    fun testProperty61ShortContentDisplaysCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genShortTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容完整
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.2: 长内容可以显示
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于长内容，提示能够显示（通过滚动功能）
     * 对于任何长内容（超过500个字符），显示应该成功
     */
    fun testProperty62LongContentCanBeDisplayed() {
        runBlocking {
            checkAll(propertyTestIterations, genLongTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容完整（即使很长也应该保存完整内容）
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.3: 多行内容正确处理
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于多行内容，提示能够正确显示（保留换行）
     * 对于任何包含多行的内容，显示应该成功且保留格式
     */
    fun testProperty63MultilineContentHandledCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genMultilineTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容完整（包括换行符）
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 验证内容包含换行符
                if (content.content.contains("\n")) {
                    currentContent?.content?.contains("\n") shouldBe true
                }
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.4: 极长内容可以显示
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于极长内容（超过1000个字符），提示能够显示
     * 对于任何极长内容，显示应该成功且不会崩溃
     */
    fun testProperty64VeryLongContentCanBeDisplayed() {
        runBlocking {
            checkAll(propertyTestIterations, genVeryLongTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示（不应该抛出异常）
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容完整
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.5: 不同格式的内容都能适配
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于不同格式（PLAIN_TEXT, HTML, MARKDOWN）的内容，都能正确显示
     * 对于任何格式和长度的内容，显示应该成功
     */
    fun testProperty65DifferentFormatsAdaptCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContentWithAllFormats()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容和格式都被保存
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                currentContent?.format shouldBe content.format
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.6: 空内容也能处理
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于空内容或极短内容，提示也能正常显示
     * 对于任何空或极短的内容，显示应该成功
     */
    fun testProperty66EmptyOrMinimalContentHandled() {
        runBlocking {
            checkAll(propertyTestIterations, genMinimalTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容被保存（即使是空的）
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.7: 特殊字符内容正确处理
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于包含特殊字符的内容，提示能够正确显示
     * 对于任何包含特殊字符的内容，显示应该成功且内容完整
     */
    fun testProperty67SpecialCharactersHandledCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genTipsContentWithSpecialChars()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容完整（包括特殊字符）
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.8: 连续显示不同长度的内容
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证连续显示不同长度的内容时，每次都能正确适配
     * 对于任何内容序列，每次显示都应该成功
     */
    fun testProperty68ConsecutiveDisplaysWithDifferentLengths() {
        runBlocking {
            checkAll(propertyTestIterations, genThreeDifferentLengthContents()) { (short, medium, long) ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示短内容
                displayService.showTip(short, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe short.content
                displayService.hideTip()
                
                // 显示中等长度内容
                displayService.showTip(medium, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe medium.content
                displayService.hideTip()
                
                // 显示长内容
                displayService.showTip(long, editor, position)
                displayService.isCurrentlyShowing() shouldBe true
                displayService.getCurrentTipContent()?.content shouldBe long.content
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.9: HTML格式的长内容正确处理
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于HTML格式的长内容，提示能够正确显示
     * 对于任何HTML格式的长内容，显示应该成功
     */
    fun testProperty69HtmlLongContentHandledCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genLongHtmlTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容和格式
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                currentContent?.format shouldBe TipsFormat.HTML
                
                // 清理
                displayService.hideTip()
            }
        }
    }
    
    /**
     * 属性 6.10: Markdown格式的长内容正确处理
     * 
     * **Validates: Requirement 3.4**
     * 
     * 验证对于Markdown格式的长内容，提示能够正确显示
     * 对于任何Markdown格式的长内容，显示应该成功
     */
    fun testProperty610MarkdownLongContentHandledCorrectly() {
        runBlocking {
            checkAll(propertyTestIterations, genLongMarkdownTipsContent()) { content ->
                // 创建测试文件和编辑器
                myFixture.configureByText("Test.java", "class Test { }")
                val editor = myFixture.editor
                val position = LogicalPosition(0, 0)
                
                // 显示提示
                displayService.showTip(content, editor, position)
                
                // 验证提示正在显示
                displayService.isCurrentlyShowing() shouldBe true
                
                // 验证内容和格式
                val currentContent = displayService.getCurrentTipContent()
                currentContent shouldNotBe null
                currentContent?.content shouldBe content.content
                currentContent?.format shouldBe TipsFormat.MARKDOWN
                
                // 清理
                displayService.hideTip()
            }
        }
    }
}

// ==================== 测试数据生成器 ====================

/**
 * 生成短内容（10-100个字符）
 */
private fun genShortTipsContent(): Arb<TipsContent> = arbitrary {
    val content = Arb.string(10..100, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成长内容（500-1000个字符）
 */
private fun genLongTipsContent(): Arb<TipsContent> = arbitrary {
    val content = Arb.string(500..1000, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成极长内容（1000-2000个字符）
 */
private fun genVeryLongTipsContent(): Arb<TipsContent> = arbitrary {
    val content = Arb.string(1000..2000, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成多行内容
 */
private fun genMultilineTipsContent(): Arb<TipsContent> = arbitrary {
    val lines = Arb.int(2..10).bind()
    val lineContents = (1..lines).map {
        Arb.string(20..100, Codepoint.alphanumeric()).bind()
    }
    val content = lineContents.joinToString("\n")
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成所有格式的内容
 */
private fun genTipsContentWithAllFormats(): Arb<TipsContent> = arbitrary {
    val content = Arb.string(50..500, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成最小内容（0-10个字符）
 */
private fun genMinimalTipsContent(): Arb<TipsContent> = arbitrary {
    val content = Arb.string(0..10, Codepoint.alphanumeric()).bind()
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成包含特殊字符的内容
 */
private fun genTipsContentWithSpecialChars(): Arb<TipsContent> = arbitrary {
    val specialChars = listOf("<", ">", "&", "\"", "'", "\n", "\t", "\r")
    val baseContent = Arb.string(20..100, Codepoint.alphanumeric()).bind()
    val specialChar = specialChars.random()
    val content = "$baseContent$specialChar${Arb.string(10..50, Codepoint.alphanumeric()).bind()}"
    val format = Arb.enum<TipsFormat>().bind()
    TipsContent(content, format)
}

/**
 * 生成三个不同长度的内容
 */
private fun genThreeDifferentLengthContents(): Arb<Triple<TipsContent, TipsContent, TipsContent>> = arbitrary {
    val format = Arb.enum<TipsFormat>().bind()
    val shortContent = Arb.string(10..50, Codepoint.alphanumeric()).bind()
    val mediumContent = Arb.string(100..300, Codepoint.alphanumeric()).bind()
    val longContent = Arb.string(500..1000, Codepoint.alphanumeric()).bind()
    val short = TipsContent(shortContent, format)
    val medium = TipsContent(mediumContent, format)
    val long = TipsContent(longContent, format)
    Triple(short, medium, long)
}

/**
 * 生成长HTML内容
 */
private fun genLongHtmlTipsContent(): Arb<TipsContent> = arbitrary {
    val paragraphs = Arb.int(5..10).bind()
    val paragraphContents = (1..paragraphs).map {
        Arb.string(50..150, Codepoint.alphanumeric()).bind()
    }
    val content = paragraphContents.joinToString("") { "<p>$it</p>" }
    TipsContent(content, TipsFormat.HTML)
}

/**
 * 生成长Markdown内容
 */
private fun genLongMarkdownTipsContent(): Arb<TipsContent> = arbitrary {
    val sections = Arb.int(5..10).bind()
    val sectionContents = (1..sections).map {
        Arb.string(50..150, Codepoint.alphanumeric()).bind()
    }
    val content = sectionContents.mapIndexed { index, text ->
        "## Section ${index + 1}\n$text"
    }.joinToString("\n\n")
    TipsContent(content, TipsFormat.MARKDOWN)
}
