package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsAnnotation
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.service.ErrorRecoveryService
import cn.myjdemo.autotips.service.LanguageSpecificParser
import cn.myjdemo.autotips.service.ParsingContext
import cn.myjdemo.autotips.service.RecoveryAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag

/**
 * 注释解析器实现类
 * 负责解析方法注释，提取@tips标记内容
 * 
 * 实现需求:
 * - 1.1: 提取@tips后的完整文本内容
 * - 1.2: 保留文本的原始格式和换行
 * - 1.5: 合并多个@tips标记的内容
 * - 5.1: 支持Java语言
 * - 5.2: 支持Kotlin语言
 * - 5.3: 根据文件类型选择合适的解析器
 * - 6.4: 支持自定义注释模式
 * - 7.3: 异常处理和错误恢复
 */
class AnnotationParserImpl : AnnotationParser {
    
    private val configService = service<ConfigurationService>()
    private val errorRecoveryService = ErrorRecoveryServiceImpl()
    
    // 语言特定解析器列表
    private val languageParsers: List<LanguageSpecificParser> = listOf(
        JavaLanguageParser(),
        KotlinLanguageParser()
    )
    
    companion object {
        private const val TIPS_TAG = "tips"
        private val LOG = Logger.getInstance(AnnotationParserImpl::class.java)
        
        // 用于检测HTML标签的简单模式
        private val HTML_TAG_PATTERN = Regex("<[^>]+>")
    }
    
    /**
     * 从方法中提取@tips内容
     * 
     * 需求 1.1: 当方法注释包含@tips标记时，提取@tips后的完整文本内容
     * 需求 1.3: 当方法注释不包含@tips标记时，返回空结果
     * 需求 5.3: 根据文件类型选择合适的解析器
     * 需求 5.4: 能够解析第三方库中方法的@tips注释
     * 需求 5.5: 正确处理包导入和类路径解析
     * 需求 7.3: 异常处理和错误恢复
     * 
     * @param method PSI方法元素
     * @return 提取的提示内容，如果没有@tips标记则返回null
     */
    override fun extractTipsContent(method: PsiMethod): TipsContent? {
        try {
            // 尝试使用语言特定解析器
            for (parser in languageParsers) {
                if (parser.supports(method)) {
                    val content = parser.extractTipsFromMethod(method)
                    if (content != null) {
                        return content
                    }
                }
            }
            
            // 回退到默认的Java解析逻辑
            // 这个逻辑同时支持项目内部和外部库的方法
            // PSI API会自动处理类路径解析，包括：
            // - 项目源代码
            // - Maven/Gradle依赖的源代码附件
            // - 编译后的.class文件中的javadoc
            val docComment = method.docComment ?: return null
            val annotations = parseAnnotationText(docComment)
            
            return if (annotations.isNotEmpty()) {
                mergeTipsContent(annotations)
            } else {
                null
            }
        } catch (e: Exception) {
            // 需求 7.3: 处理解析异常
            val context = ParsingContext(
                methodName = method.name,
                className = method.containingClass?.qualifiedName ?: "Unknown",
                filePath = method.containingFile?.virtualFile?.path ?: "Unknown",
                lineNumber = 0
            )
            
            val recoveryAction = errorRecoveryService.handleParsingError(e, context)
            
            // 根据恢复动作决定返回值
            return when (recoveryAction) {
                RecoveryAction.SKIP, RecoveryAction.DISABLE_FEATURE -> null
                RecoveryAction.FALLBACK -> {
                    // 尝试简单的文本提取作为回退
                    try {
                        val docComment = method.docComment
                        if (docComment != null) {
                            val text = docComment.text
                            if (text.contains("@$TIPS_TAG")) {
                                TipsContent(content = "Error parsing tips content", format = TipsFormat.PLAIN_TEXT)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } catch (fallbackException: Exception) {
                        errorRecoveryService.logError("Fallback parsing also failed", fallbackException)
                        null
                    }
                }
                else -> null
            }
        }
    }
    
    /**
     * 解析文档注释中的@tips标记
     * 
     * 需求 1.1: 提取@tips后的完整文本内容
     * 需求 1.2: 保留文本的原始格式和换行
     * 需求 1.4: 当@tips标记格式不正确时，忽略该标记并记录警告
     * 需求 6.4: 支持自定义注释模式
     * 
     * @param docComment PSI文档注释元素
     * @return @tips注释列表
     */
    override fun parseAnnotationText(docComment: PsiDocComment): List<TipsAnnotation> {
        val annotations = mutableListOf<TipsAnnotation>()
        
        try {
            // 获取所有支持的标签名称（默认@tips + 自定义模式）
            val tagNames = getSupportedTagNames()
            
            for (tagName in tagNames) {
                // 查找该标签的所有实例
                val tags = docComment.findTagsByName(tagName)
                
                for ((index, tag) in tags.withIndex()) {
                    // 提取标签内容，保留格式
                    val content = extractTagContent(tag)
                    
                    // 验证格式
                    if (validateTipsFormat(content)) {
                        annotations.add(
                            TipsAnnotation(
                                marker = tagName,
                                content = content,
                                lineNumber = index + 1
                            )
                        )
                    } else {
                        // 需求 1.4: 格式不正确时记录警告
                        LOG.warn("Invalid @$tagName format detected at line ${index + 1}, content: '$content'")
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse annotation text", e)
        }
        
        return annotations
    }
    
    /**
     * 获取所有支持的标签名称
     * 包括默认的@tips和用户配置的自定义模式
     * 
     * 需求 6.4: 支持自定义注释模式
     * 
     * @return 支持的标签名称列表
     */
    private fun getSupportedTagNames(): List<String> {
        val tagNames = mutableListOf(TIPS_TAG)
        
        // 添加自定义模式（移除@前缀，因为findTagsByName不需要@）
        val customPatterns = configService.getCustomAnnotationPatterns()
        for (pattern in customPatterns) {
            val tagName = pattern.removePrefix("@").trim()
            if (tagName.isNotEmpty() && !tagNames.contains(tagName)) {
                tagNames.add(tagName)
            }
        }
        
        return tagNames
    }
    
    /**
     * 验证@tips格式是否正确
     * 
     * 需求 1.4: 检测格式不正确的@tips标记
     * 
     * 验证规则:
     * 1. 内容不能为空或只包含空白字符
     * 2. 内容应该包含有意义的文本
     * 
     * @param annotation 注释字符串
     * @return 格式是否有效
     */
    override fun validateTipsFormat(annotation: String): Boolean {
        // 检查是否为空或只包含空白字符
        if (annotation.isBlank()) {
            return false
        }
        
        // 去除空白后检查是否有实际内容
        val trimmed = annotation.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        
        // 基本验证通过
        return true
    }
    
    /**
     * 合并多个@tips标记的内容
     * 
     * 需求 1.5: 当同一方法包含多个@tips标记时，合并所有提示内容
     * 需求 1.2: 保留文本的原始格式
     * 
     * @param annotations @tips注释列表
     * @return 合并后的提示内容
     */
    override fun mergeTipsContent(annotations: List<TipsAnnotation>): TipsContent? {
        if (annotations.isEmpty()) {
            return null
        }
        
        // 合并所有注释内容，使用双换行分隔不同的@tips标记
        val mergedContent = annotations.joinToString("\n\n") { it.content }
        
        // 检测内容格式（简单检测是否包含HTML标签）
        val format = detectContentFormat(mergedContent)
        
        return TipsContent(
            content = mergedContent,
            format = format
        )
    }
    
    /**
     * 从文档标签中提取内容
     * 
     * 需求 1.2: 保留文本的原始格式和换行
     * 
     * 此方法直接从标签的文本中提取内容，保留原始格式。
     * 
     * @param tag PSI文档标签
     * @return 提取的内容字符串
     */
    private fun extractTagContent(tag: PsiDocTag): String {
        // 获取标签的完整文本
        val tagText = tag.text
        
        // 标签格式: @tips 内容...
        // 我们需要提取@tips之后的所有内容
        val tagName = "@${tag.name}"
        val startIndex = tagText.indexOf(tagName)
        
        if (startIndex == -1) {
            return ""
        }
        
        // 跳过标签名称
        var content = tagText.substring(startIndex + tagName.length)
        
        // 清理javadoc注释的格式字符（* 和前导空格）
        val lines = content.lines().map { line ->
            // 移除每行开头的 * 和空格
            line.trimStart().removePrefix("*").trimStart()
        }.filter { it.isNotEmpty() }
        
        // 用换行符连接所有行
        return lines.joinToString("\n").trim()
    }
    
    /**
     * 检测内容格式类型
     * 
     * 简单检测内容是否包含HTML标签来判断格式类型
     * 
     * @param content 内容字符串
     * @return 检测到的格式类型
     */
    private fun detectContentFormat(content: String): TipsFormat {
        return if (HTML_TAG_PATTERN.containsMatchIn(content)) {
            TipsFormat.HTML
        } else {
            TipsFormat.PLAIN_TEXT
        }
    }
}