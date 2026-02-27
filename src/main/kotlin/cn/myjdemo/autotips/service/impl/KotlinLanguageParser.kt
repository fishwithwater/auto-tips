package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsAnnotation
import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.service.LanguageSpecificParser
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Kotlin语言特定解析器
 * 
 * 需求 5.2: 当项目使用Kotlin语言时，正确解析Kotlin方法注释和调用
 */
class KotlinLanguageParser : LanguageSpecificParser {
    
    private val configService = com.intellij.openapi.application.ApplicationManager.getApplication().service<ConfigurationService>()
    
    companion object {
        private const val TIPS_TAG = "tips"
        private val LOG = Logger.getInstance(KotlinLanguageParser::class.java)
        private val HTML_TAG_PATTERN = Regex("<[^>]+>")
    }
    
    override fun supports(element: PsiElement): Boolean {
        return element is KtNamedFunction
    }
    
    override fun extractTipsFromMethod(methodElement: PsiElement): TipsContent? {
        if (methodElement !is KtNamedFunction) {
            return null
        }
        
        try {
            val kdoc = methodElement.docComment ?: return null
            val annotations = parseKDocAnnotations(kdoc)
            
            return if (annotations.isNotEmpty()) {
                mergeTipsContent(annotations)
            } else {
                null
            }
        } catch (e: Exception) {
            LOG.warn("Failed to extract tips from Kotlin function: ${methodElement.name}", e)
            return null
        }
    }
    
    override fun getLanguageName(): String = "Kotlin"
    
    /**
     * 解析KDoc注释中的@tips标记
     */
    private fun parseKDocAnnotations(kdoc: KDoc): List<TipsAnnotation> {
        val annotations = mutableListOf<TipsAnnotation>()
        
        try {
            val tagNames = getSupportedTagNames()
            
            // 查找所有标签
            val tags = kdoc.children.filterIsInstance<KDocTag>()
            
            for ((index, tag) in tags.withIndex()) {
                val tagName = tag.name?.removePrefix("@") ?: continue
                
                if (tagNames.contains(tagName)) {
                    val content = extractKDocTagContent(tag)
                    
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
            LOG.warn("Failed to parse KDoc annotations", e)
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
     * 从KDoc标签中提取内容
     */
    private fun extractKDocTagContent(tag: KDocTag): String {
        // 获取标签的内容部分（去除标签名称）
        val content = tag.getContent()
        
        // 清理KDoc注释的格式字符
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
