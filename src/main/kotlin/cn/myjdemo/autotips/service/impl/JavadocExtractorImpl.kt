package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.JavadocExtractor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment

/**
 * Javadoc 提取器实现类
 * 负责从 PSI 元素中提取标准 Javadoc 内容，并过滤掉 @tips 标签
 * 
 * 实现需求:
 * - 2.1: 从方法声明中提取 Javadoc 注释
 * - 2.2: 从字段声明中提取 Javadoc 注释
 * - 2.3: 从显示内容中排除 @tips 注解
 * - 2.4: 当方法或字段没有 Javadoc 时，不显示弹窗
 * - 4.1: 从输出中排除所有 @tips 标签内容
 * - 4.2: 包含标准 Javadoc 标签（@param、@return、@throws 等）
 * - 4.3: 保留 Javadoc 描述的格式
 */
class JavadocExtractorImpl : JavadocExtractor {
    
    companion object {
        private val LOG = Logger.getInstance(JavadocExtractorImpl::class.java)
        
        // 用于检测 HTML 标签的模式
        private val HTML_TAG_PATTERN = Regex("<[^>]+>")
    }
    
    /**
     * 从方法中提取 Javadoc 内容
     * 
     * 需求 2.1: 当 Javadoc 模式启用且正在输入方法时，从方法声明中提取 Javadoc 注释
     * 需求 2.4: 当方法没有 Javadoc 时，不显示弹窗（返回 null）
     * 需求 4.4: 当 Javadoc 注释仅包含 @tips 标签时，不显示弹窗（返回 null）
     * 
     * @param method PSI 方法元素
     * @return 提取的 Javadoc 内容，如果没有 Javadoc 或内容为空则返回 null
     */
    override fun extractJavadocFromMethod(method: PsiMethod): TipsContent? {
        try {
            LOG.info("=== JavadocExtractor.extractJavadocFromMethod ===")
            LOG.info("Method name: ${method.name}")
            LOG.info("Method class: ${method.containingClass?.qualifiedName}")
            
            // 获取方法的文档注释
            val docComment = method.docComment
            LOG.info("DocComment found: ${docComment != null}")
            
            if (docComment == null) {
                LOG.info("No docComment found, returning null")
                return null
            }
            
            LOG.info("DocComment text length: ${docComment.text.length}")
            
            // 过滤 Javadoc 内容，移除 @tips 标签
            val filteredContent = filterJavadocContent(docComment)
            LOG.info("Filtered content length: ${filteredContent.length}")
            LOG.info("Filtered content: [$filteredContent]")
            
            // 如果过滤后内容为空，返回 null（需求 2.4, 4.4）
            if (filteredContent.isBlank()) {
                LOG.info("Filtered content is blank, returning null")
                return null
            }
            
            // 检测内容格式
            val format = detectContentFormat(filteredContent)
            
            LOG.info("Extracted Javadoc from method: ${method.name}, format: $format")
            val result = TipsContent(content = filteredContent, format = format)
            LOG.info("Returning TipsContent: ${result.content.take(100)}")
            return result
        } catch (e: Exception) {
            LOG.warn("Failed to extract Javadoc from method: ${method.name}", e)
            return null
        }
    }
    
    /**
     * 从字段中提取 Javadoc 内容
     * 
     * 需求 2.2: 当 Javadoc 模式启用且正在输入字段时，从字段声明中提取 Javadoc 注释
     * 需求 2.4: 当字段没有 Javadoc 时，不显示弹窗（返回 null）
     * 需求 4.4: 当 Javadoc 注释仅包含 @tips 标签时，不显示弹窗（返回 null）
     * 
     * @param field PSI 字段元素
     * @return 提取的 Javadoc 内容，如果没有 Javadoc 或内容为空则返回 null
     */
    override fun extractJavadocFromField(field: PsiField): TipsContent? {
        try {
            // 获取字段的文档注释
            val docComment = field.docComment ?: return null
            
            // 过滤 Javadoc 内容，移除 @tips 标签
            val filteredContent = filterJavadocContent(docComment)
            
            // 如果过滤后内容为空，返回 null（需求 2.4, 4.4）
            if (filteredContent.isBlank()) {
                LOG.debug("No Javadoc content after filtering for field: ${field.name}")
                return null
            }
            
            // 检测内容格式
            val format = detectContentFormat(filteredContent)
            
            LOG.debug("Extracted Javadoc from field: ${field.name}, format: $format")
            return TipsContent(content = filteredContent, format = format)
        } catch (e: Exception) {
            LOG.warn("Failed to extract Javadoc from field: ${field.name}", e)
            return null
        }
    }
    
    /**
     * 过滤 Javadoc 内容，移除 @tips 标签
     * 
     * 需求 2.3: 从显示内容中排除 @tips 注解
     * 需求 4.1: 从输出中排除所有 @tips 标签内容
     * 需求 4.2: 包含标准 Javadoc 标签（@param、@return、@throws 等）
     * 需求 4.3: 保留 Javadoc 描述的格式
     * 
     * 该方法处理 Javadoc 文档注释，执行以下操作：
     * 1. 移除 Javadoc 注释标记（/** 和 */）
     * 2. 清理每行的 * 前缀
     * 3. 移除所有 @tips 标签及其内容
     * 4. 保留标准 Javadoc 标签（@param、@return、@throws 等）
     * 5. 保留原始格式和换行
     * 
     * @param docComment PSI 文档注释
     * @return 过滤后的内容字符串，已移除 @tips 标签
     */
    override fun filterJavadocContent(docComment: PsiDocComment): String {
        try {
            // 获取文档注释的文本
            val text = docComment.text
            LOG.debug("DEBUG filterJavadocContent: raw text = [$text]")
            
            // 移除 Javadoc 注释标记 (/** 和 */)
            var content = text
                .removePrefix("/**")
                .removeSuffix("*/")
                .trim()
            
            LOG.debug("DEBUG filterJavadocContent: after removing markers = [$content]")
            
            // 清理每行的 * 前缀，保留格式（需求 4.3）
            content = content.lines()
                .map { line: String ->
                    // 移除每行开头的 * 和一个空格（如果存在）
                    val trimmedStart = line.trimStart()
                    if (trimmedStart.startsWith("*")) {
                        val afterStar = trimmedStart.substring(1)
                        // 如果 * 后面有空格，也移除一个空格
                        if (afterStar.startsWith(" ")) {
                            afterStar.substring(1)
                        } else {
                            afterStar
                        }
                    } else {
                        line
                    }
                }
                .joinToString("\n")
            
            LOG.debug("DEBUG filterJavadocContent: after cleaning * = [$content]")
            
            // 清理多余的空行，但保留单个换行以维持格式（需求 4.3）
            content = content.lines()
                .filter { it.isNotBlank() }  // 移除空行
                .joinToString("\n")
                .trim()
            
            LOG.debug("DEBUG filterJavadocContent: final content = [$content]")
            
            return content
        } catch (e: Exception) {
            LOG.warn("Failed to filter Javadoc content", e)
            // 返回空字符串而不是抛出异常
            return ""
        }
    }
    
    /**
     * 检测内容格式类型
     * 
     * 通过检测内容是否包含 HTML 标签来判断格式类型。
     * 如果包含 HTML 标签，则认为是 HTML 格式，否则为纯文本格式。
     * 
     * @param content 内容字符串
     * @return 检测到的格式类型（HTML 或 PLAIN_TEXT）
     */
    private fun detectContentFormat(content: String): TipsFormat {
        return if (HTML_TAG_PATTERN.containsMatchIn(content)) {
            TipsFormat.HTML
        } else {
            TipsFormat.PLAIN_TEXT
        }
    }
}
