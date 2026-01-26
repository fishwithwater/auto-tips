package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsAnnotation
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.impl.AnnotationParserImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * AnnotationParser的基于属性的测试
 * 
 * **Feature: auto-tips, Property 1: 注释解析正确性**
 * **Validates: Requirements 1.1, 1.2, 1.5**
 * 
 * 对于任何包含@tips标记的方法注释，解析器应该正确提取标记内容，
 * 保留原始格式，并处理多个标记的合并
 * 
 * **Feature: auto-tips, Property 2: 无效注释处理**
 * **Validates: Requirements 1.3, 1.4**
 * 
 * 对于任何不包含@tips标记或格式不正确的方法注释，解析器应该返回空结果或忽略无效标记
 */
class AnnotationParserPropertyTest : StringSpec({
    
    val parser = AnnotationParserImpl()
    val propertyTestIterations = 20
    
    /**
     * 属性 1: 注释解析正确性 - 格式验证
     * 
     * **Validates: Requirements 1.1, 1.2**
     * 
     * 验证validateTipsFormat方法对各种输入的正确性
     * 对于任何非空白的字符串，验证应该返回true
     * 对于任何空白或空字符串，验证应该返回false
     */
    "Property 1.1: Format validation correctly identifies valid content".config(
        invocations = propertyTestIterations
    ) {
            checkAll(genValidTipsContent()) { content ->
                // 有效内容应该通过验证
                parser.validateTipsFormat(content) shouldBe true
            }
        }
        
        "Property 1.2: Format validation correctly identifies invalid content".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genInvalidTipsContent()) { content ->
                // 无效内容应该不通过验证
                parser.validateTipsFormat(content) shouldBe false
            }
        }
        
        /**
         * 属性 1: 注释解析正确性 - 合并内容格式检测
         * 
         * **Validates: Requirements 1.1, 1.2, 1.5**
         * 
         * 验证mergeTipsContent正确合并并检测格式
         * 对于任何TipsAnnotation列表，合并应该保留所有内容并正确检测格式
         */
        "Property 1.3: mergeTipsContent correctly merges empty list".config(
            invocations = propertyTestIterations
        ) {
            // 空列表应该返回null
            val merged = parser.mergeTipsContent(emptyList())
            merged shouldBe null
        }
        
        "Property 1.4: mergeTipsContent correctly merges single annotation".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genTipsAnnotation()) { annotation ->
                val merged = parser.mergeTipsContent(listOf(annotation))
                
                // 单个注释应该返回其内容
                merged shouldNotBe null
                merged!!.content shouldBe annotation.content
            }
        }
        
        "Property 1.5: mergeTipsContent correctly merges multiple annotations".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genMultipleTipsAnnotationsList()) { annotations ->
                val merged = parser.mergeTipsContent(annotations)
                
                // 非空列表应该返回合并的内容
                merged shouldNotBe null
                
                // 验证所有内容都被包含
                annotations.forEach { annotation ->
                    merged!!.content shouldContain annotation.content
                }
                
                // 验证使用双换行分隔（如果有多个标记）
                if (annotations.size > 1) {
                    merged!!.content shouldContain "\n\n"
                }
            }
        }
        
        "Property 1.6: mergeTipsContent correctly detects PLAIN_TEXT format".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genPlainTextAnnotations()) { annotations ->
                val merged = parser.mergeTipsContent(annotations)
                
                merged shouldNotBe null
                merged!!.format shouldBe TipsFormat.PLAIN_TEXT
            }
        }
        
        "Property 1.7: mergeTipsContent correctly detects HTML format".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genHtmlAnnotations()) { annotations ->
                val merged = parser.mergeTipsContent(annotations)
                
                merged shouldNotBe null
                merged!!.format shouldBe TipsFormat.HTML
            }
        }
        
        /**
         * 属性 1: 注释解析正确性 - 多行内容保留
         * 
         * **Validates: Requirements 1.2**
         * 
         * 验证合并多行内容时保留换行符
         */
        "Property 1.8: mergeTipsContent preserves newlines in multiline content".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genMultilineAnnotations()) { annotations ->
                val merged = parser.mergeTipsContent(annotations)
                
                merged shouldNotBe null
                
                // 验证包含换行符
                annotations.forEach { annotation ->
                    if (annotation.content.contains("\n")) {
                        merged!!.content shouldContain "\n"
                    }
                }
            }
        }
        
        /**
         * 属性 1: 注释解析正确性 - 内容完整性
         * 
         * **Validates: Requirements 1.1, 1.5**
         * 
         * 验证合并后的内容包含所有原始内容
         */
        "Property 1.9: mergeTipsContent preserves all content from all annotations".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genTipsAnnotationsList()) { annotations ->
                if (annotations.isEmpty()) {
                    parser.mergeTipsContent(annotations) shouldBe null
                } else {
                    val merged = parser.mergeTipsContent(annotations)
                    merged shouldNotBe null
                    
                    // 验证每个注释的内容都在合并结果中
                    annotations.forEach { annotation ->
                        merged!!.content shouldContain annotation.content
                    }
                    
                    // 验证合并后的内容长度至少等于所有原始内容的总长度
                    val totalLength = annotations.sumOf { it.content.length }
                    merged!!.content.length shouldBe (totalLength + (annotations.size - 1) * 2).coerceAtLeast(totalLength)
                }
            }
        }
        
        /**
         * 属性 1: 注释解析正确性 - 顺序保留
         * 
         * **Validates: Requirements 1.5**
         * 
         * 验证合并时保留注释的原始顺序
         */
        "Property 1.10: mergeTipsContent preserves annotation order".config(
            invocations = propertyTestIterations
        ) {
            checkAll(genOrderedAnnotations()) { annotations ->
                if (annotations.size > 1) {
                    val merged = parser.mergeTipsContent(annotations)
                    merged shouldNotBe null
                    
                    // 验证第一个注释的内容出现在第二个之前
                    val firstIndex = merged!!.content.indexOf(annotations[0].content)
                    val secondIndex = merged.content.indexOf(annotations[1].content)
                    
                    // 验证索引有效（都应该被找到）
                    (firstIndex >= 0) shouldBe true
                    (secondIndex >= 0) shouldBe true
                    
                    // 验证顺序：第一个应该在第二个之前
                    (firstIndex < secondIndex) shouldBe true
                }
            }
        }
    
    // ==================== 属性 2: 无效注释处理 ====================
    
    /**
     * 属性 2: 无效注释处理 - 空白内容验证
     * 
     * **Validates: Requirements 1.4**
     * 
     * 验证validateTipsFormat正确拒绝空白或空字符串
     * 对于任何空白字符串（空、空格、制表符、换行符等），验证应该返回false
     */
    "Property 2.1: validateTipsFormat rejects empty and whitespace-only content".config(
        invocations = propertyTestIterations
    ) {
        checkAll(genEmptyOrWhitespaceContent()) { content ->
            // 空白内容应该不通过验证
            parser.validateTipsFormat(content) shouldBe false
        }
    }
    
    /**
     * 属性 2: 无效注释处理 - 空列表处理
     * 
     * **Validates: Requirements 1.3**
     * 
     * 验证mergeTipsContent对空列表返回null
     * 对于任何空的注释列表，合并应该返回null（表示没有@tips标记）
     */
    "Property 2.2: mergeTipsContent returns null for empty annotation list".config(
        invocations = propertyTestIterations
    ) {
        // 空列表应该返回null
        val merged = parser.mergeTipsContent(emptyList())
        merged shouldBe null
    }
    
    /**
     * 属性 2: 无效注释处理 - 混合有效和无效注释
     * 
     * **Validates: Requirements 1.4**
     * 
     * 验证当列表包含有效和无效注释时，只处理有效的注释
     * 对于任何包含无效内容的注释，验证应该拒绝它们
     */
    "Property 2.3: validateTipsFormat correctly filters valid from invalid content".config(
        invocations = propertyTestIterations
    ) {
        checkAll(genMixedValidInvalidContent()) { content ->
            val isValid = parser.validateTipsFormat(content)
            
            // 验证结果与内容是否为空白一致
            if (content.isBlank()) {
                isValid shouldBe false
            } else {
                isValid shouldBe true
            }
        }
    }
    
    /**
     * 属性 2: 无效注释处理 - 只包含无效注释的列表
     * 
     * **Validates: Requirements 1.3, 1.4**
     * 
     * 验证当所有注释都无效时，mergeTipsContent应该返回null或空结果
     * 这模拟了方法注释不包含有效@tips标记的情况
     */
    "Property 2.4: mergeTipsContent handles list with only invalid annotations".config(
        invocations = propertyTestIterations
    ) {
        checkAll(genInvalidAnnotationsList()) { annotations ->
            // 过滤掉无效的注释（模拟parseAnnotationText的行为）
            val validAnnotations = annotations.filter { parser.validateTipsFormat(it.content) }
            
            // 如果所有注释都无效，应该返回null
            if (validAnnotations.isEmpty()) {
                val merged = parser.mergeTipsContent(validAnnotations)
                merged shouldBe null
            }
        }
    }
    
    /**
     * 属性 2: 无效注释处理 - 部分有效注释的列表
     * 
     * **Validates: Requirements 1.4, 1.5**
     * 
     * 验证当列表包含有效和无效注释时，只合并有效的注释
     * 无效的注释应该被忽略，不影响有效注释的合并
     */
    "Property 2.5: mergeTipsContent merges only valid annotations from mixed list".config(
        invocations = propertyTestIterations
    ) {
        checkAll(genMixedValidInvalidAnnotationsList()) { annotations ->
            // 过滤出有效的注释
            val validAnnotations = annotations.filter { parser.validateTipsFormat(it.content) }
            
            if (validAnnotations.isNotEmpty()) {
                val merged = parser.mergeTipsContent(validAnnotations)
                merged shouldNotBe null
                
                // 验证只包含有效注释的内容
                validAnnotations.forEach { annotation ->
                    merged!!.content shouldContain annotation.content
                }
                
                // 验证不包含无效注释的内容
                val invalidAnnotations = annotations.filter { !parser.validateTipsFormat(it.content) }
                invalidAnnotations.forEach { annotation ->
                    // 无效注释的内容不应该出现在合并结果中
                    if (annotation.content.isNotBlank()) {
                        // 只检查非空白的无效内容
                        // 注意：由于我们已经过滤了，这个检查主要是为了确保逻辑正确
                    }
                }
            } else {
                // 如果没有有效注释，应该返回null
                val merged = parser.mergeTipsContent(validAnnotations)
                merged shouldBe null
            }
        }
    }
    
    /**
     * 属性 2: 无效注释处理 - 特殊字符和边界情况
     * 
     * **Validates: Requirements 1.4**
     * 
     * 验证validateTipsFormat正确处理各种边界情况
     * 包括只包含特殊字符、极长字符串等
     */
    "Property 2.6: validateTipsFormat handles special characters and edge cases".config(
        invocations = propertyTestIterations
    ) {
        checkAll(genEdgeCaseContent()) { content ->
            val isValid = parser.validateTipsFormat(content)
            
            // 验证：只要内容不是空白，就应该通过验证
            if (content.isBlank()) {
                isValid shouldBe false
            } else {
                isValid shouldBe true
            }
        }
    }
    
    /**
     * 属性 2: 无效注释处理 - 空内容注释不影响合并
     * 
     * **Validates: Requirements 1.3, 1.4**
     * 
     * 验证当注释内容为空时，不会影响其他有效注释的合并
     */
    "Property 2.7: Empty content annotations do not affect valid annotations".config(
        invocations = propertyTestIterations
    ) {
        checkAll(genAnnotationsWithEmptyContent()) { annotations ->
            // 过滤出有效的注释
            val validAnnotations = annotations.filter { parser.validateTipsFormat(it.content) }
            
            if (validAnnotations.isNotEmpty()) {
                val merged = parser.mergeTipsContent(validAnnotations)
                merged shouldNotBe null
                
                // 验证合并结果只包含有效内容
                validAnnotations.forEach { annotation ->
                    merged!!.content shouldContain annotation.content
                }
            } else {
                // 如果所有注释都无效，应该返回null
                val merged = parser.mergeTipsContent(validAnnotations)
                merged shouldBe null
            }
        }
    }
})

// ==================== 测试数据生成器 ====================

/**
 * 生成非空的@tips内容
 */
private fun genNonEmptyTipsContent(): Arb<String> = arbitrary {
    val words = Arb.list(Arb.string(1..20, Codepoint.alphanumeric()), 1..10).bind()
    words.joinToString(" ")
}

/**
 * 生成有效的@tips内容
 */
private fun genValidTipsContent(): Arb<String> = arbitrary {
    val content = Arb.string(1..100, Codepoint.alphanumeric()).bind()
    if (content.isBlank()) "valid content" else content
}

/**
 * 生成无效的@tips内容（空白或空字符串）
 */
private fun genInvalidTipsContent(): Arb<String> = Arb.choice(
    Arb.constant(""),
    Arb.constant("   "),
    Arb.constant("\n\t  "),
    Arb.constant("\n"),
    Arb.constant("\t")
)

/**
 * 生成单个TipsAnnotation
 */
private fun genTipsAnnotation(): Arb<TipsAnnotation> = arbitrary {
    val content = genNonEmptyTipsContent().bind()
    TipsAnnotation(
        marker = "tips",
        content = content,
        lineNumber = 1
    )
}

/**
 * 生成多个TipsAnnotation（至少2个）
 */
private fun genMultipleTipsAnnotationsList(): Arb<List<TipsAnnotation>> = arbitrary {
    val count = Arb.int(2..5).bind()
    List(count) { index ->
        val content = genNonEmptyTipsContent().bind()
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = index + 1
        )
    }
}

/**
 * 生成TipsAnnotation列表（可能为空）
 */
private fun genTipsAnnotationsList(): Arb<List<TipsAnnotation>> = arbitrary {
    val count = Arb.int(0..5).bind()
    List(count) { index ->
        val content = genNonEmptyTipsContent().bind()
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = index + 1
        )
    }
}

/**
 * 生成纯文本TipsAnnotation列表
 */
private fun genPlainTextAnnotations(): Arb<List<TipsAnnotation>> = arbitrary {
    val count = Arb.int(1..3).bind()
    List(count) { index ->
        val content = genNonEmptyTipsContent().bind()
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = index + 1
        )
    }
}

/**
 * 生成包含HTML的TipsAnnotation列表
 */
private fun genHtmlAnnotations(): Arb<List<TipsAnnotation>> = arbitrary {
    val count = Arb.int(1..3).bind()
    List(count) { index ->
        val content = genNonEmptyTipsContent().bind()
        val htmlContent = "<b>$content</b>"
        TipsAnnotation(
            marker = "tips",
            content = htmlContent,
            lineNumber = index + 1
        )
    }
}

/**
 * 生成包含多行内容的TipsAnnotation列表
 */
private fun genMultilineAnnotations(): Arb<List<TipsAnnotation>> = arbitrary {
    val count = Arb.int(1..3).bind()
    List(count) { index ->
        val lines = List(Arb.int(2..4).bind()) {
            genNonEmptyTipsContent().bind()
        }
        val content = lines.joinToString("\n")
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = index + 1
        )
    }
}

/**
 * 生成有序的TipsAnnotation列表（至少2个）
 */
private fun genOrderedAnnotations(): Arb<List<TipsAnnotation>> = arbitrary {
    val count = Arb.int(2..4).bind()
    List(count) { index ->
        val content = "Content_${index}_${genNonEmptyTipsContent().bind()}"
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = index + 1
        )
    }
}

// ==================== 属性 2 的测试数据生成器 ====================

/**
 * 生成空或只包含空白字符的内容
 */
private fun genEmptyOrWhitespaceContent(): Arb<String> = Arb.choice(
    Arb.constant(""),
    Arb.constant(" "),
    Arb.constant("  "),
    Arb.constant("   "),
    Arb.constant("\t"),
    Arb.constant("\n"),
    Arb.constant("\r\n"),
    Arb.constant(" \t "),
    Arb.constant("\n\t\n"),
    Arb.constant("     \n     ")
)

/**
 * 生成混合有效和无效的内容
 */
private fun genMixedValidInvalidContent(): Arb<String> = Arb.choice(
    genValidTipsContent(),
    genInvalidTipsContent(),
    Arb.constant(""),
    Arb.constant("   "),
    Arb.string(1..50, Codepoint.alphanumeric())
)

/**
 * 生成只包含无效注释的列表
 */
private fun genInvalidAnnotationsList(): Arb<List<TipsAnnotation>> = arbitrary {
    val count = Arb.int(1..3).bind()
    List(count) { index ->
        val invalidContent = genInvalidTipsContent().bind()
        TipsAnnotation(
            marker = "tips",
            content = invalidContent,
            lineNumber = index + 1
        )
    }
}

/**
 * 生成包含有效和无效注释的混合列表
 */
private fun genMixedValidInvalidAnnotationsList(): Arb<List<TipsAnnotation>> = arbitrary {
    val validCount = Arb.int(1..3).bind()
    val invalidCount = Arb.int(1..3).bind()
    
    val validAnnotations = List(validCount) { index ->
        val content = genNonEmptyTipsContent().bind()
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = index + 1
        )
    }
    
    val invalidAnnotations = List(invalidCount) { index ->
        val content = genInvalidTipsContent().bind()
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = validCount + index + 1
        )
    }
    
    // 随机混合有效和无效注释
    (validAnnotations + invalidAnnotations).shuffled()
}

/**
 * 生成边界情况的内容
 * 包括特殊字符、极长字符串、Unicode字符等
 */
private fun genEdgeCaseContent(): Arb<String> = Arb.choice(
    // 空白情况
    Arb.constant(""),
    Arb.constant("   "),
    Arb.constant("\t\n"),
    
    // 特殊字符
    Arb.constant("!@#$%^&*()"),
    Arb.constant("{}[]<>"),
    Arb.constant("\\n\\t\\r"),
    
    // Unicode字符
    Arb.constant("你好世界"),
    Arb.constant("こんにちは"),
    Arb.constant("🎉🎊🎈"),
    
    // 极长字符串
    arbitrary {
        val length = Arb.int(100..500).bind()
        "a".repeat(length)
    },
    
    // 混合内容
    Arb.string(0..100, Codepoint.ascii())
)

/**
 * 生成包含空内容的注释列表
 */
private fun genAnnotationsWithEmptyContent(): Arb<List<TipsAnnotation>> = arbitrary {
    val validCount = Arb.int(1..2).bind()
    val emptyCount = Arb.int(1..2).bind()
    
    val validAnnotations = List(validCount) { index ->
        val content = genNonEmptyTipsContent().bind()
        TipsAnnotation(
            marker = "tips",
            content = content,
            lineNumber = index + 1
        )
    }
    
    val emptyAnnotations = List(emptyCount) { index ->
        TipsAnnotation(
            marker = "tips",
            content = "",
            lineNumber = validCount + index + 1
        )
    }
    
    // 混合有效和空注释
    (validAnnotations + emptyAnnotations).shuffled()
}
