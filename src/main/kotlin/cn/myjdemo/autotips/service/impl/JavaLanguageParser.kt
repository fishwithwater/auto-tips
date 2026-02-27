package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsAnnotation
import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.service.LanguageSpecificParser
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag

/**
 * Java语言特定解析器
 * 
 * 需求 5.1: 当项目使用Java语言时，正确解析Java方法注释和调用
 */
class JavaLanguageParser : LanguageSpecificParser {
    
    private val configService = com.intellij.openapi.application.ApplicationManager.getApplication().service<ConfigurationService>()
    
    companion object {
        private const val TIPS_TAG = "tips"
        private val LOG = Logger.getInstance(JavaLanguageParser::class.java)
        private val HTML_TAG_PATTERN = Regex("<[^>]+>")
    }
    
    override fun supports(element: PsiElement): Boolean {
        return element is PsiMethod
    }
    
    override fun extractTipsFromMethod(methodElement: PsiElement): TipsContent? {
        if (methodElement !is PsiMethod) {
            return null
        }
        
        try {
            val docComment = methodElement.docComment ?: return null
            val annotations = parseJavadocAnnotations(docComment)
            
            return if (annotations.isNotEmpty()) {
                mergeTipsContent(annotations)
            } else {
                null
            }
        } catch (e: Exception) {
            LOG.warn("Failed to extract tips from Java method: ${methodElement.name}", e)
            return null
        }
    }
    
    override fun getLanguageName(): String = "Java"
    
    /**
     * 解析Javadoc注释中的@tips标记
     */
    private fun parseJavadocAnnotations(docComment: PsiDocComment): List<TipsAnnotation> {
        val annotations = mutableListOf<TipsAnnotation>()
        
        try {
            val tagNames = getSupportedTagNames()
            
            for (tagName in tagNames) {
                val tags = docComment.findTagsByName(tagName)
                
                for ((index, tag) in tags.withIndex()) {
                    val content = extractTagContent(tag)
                    
                    if (content.isNotBlank()) {
                        annotations.add(
                            TipsAnnotation(
                                marker = tagName,
                                content = content,
                                lineNumber = index + 1
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse Javadoc annotations", e)
        }
        
        return annotations
    }
    
    /**
     * 获取所有支持的标签名称
     */
    private fun getSupportedTagNames(): List<String> {
        val tagNames = mutableListOf(TIPS_TAG)
        
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
     * 从文档标签中提取内容
     */
    private fun extractTagContent(tag: PsiDocTag): String {
        val tagText = tag.text
        val tagName = "@${tag.name}"
        val startIndex = tagText.indexOf(tagName)
        
        if (startIndex == -1) {
            return ""
        }
        
        var content = tagText.substring(startIndex + tagName.length)
        
        // 清理javadoc注释的格式字符
        val lines = content.lines().map { line ->
            line.trimStart().removePrefix("*").trimStart()
        }.filter { it.isNotEmpty() }
        
        return lines.joinToString("\n").trim()
    }
    
    /**
     * 合并多个@tips标记的内容
     */
    private fun mergeTipsContent(annotations: List<TipsAnnotation>): TipsContent? {
        if (annotations.isEmpty()) {
            return null
        }
        
        val mergedContent = annotations.joinToString("\n\n") { it.content }
        val format = if (HTML_TAG_PATTERN.containsMatchIn(mergedContent)) {
            TipsFormat.HTML
        } else {
            TipsFormat.PLAIN_TEXT
        }
        
        return TipsContent(
            content = mergedContent,
            format = format
        )
    }
}
