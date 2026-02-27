package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment

/**
 * Javadoc 提取器接口
 * 负责从 PSI 元素中提取标准 Javadoc 内容
 * 
 * 该接口定义了从 Java 方法和字段中提取 Javadoc 文档的核心功能，
 * 并提供了过滤功能以移除 @tips 标签，确保在 Javadoc 模式下只显示标准文档内容。
 */
interface JavadocExtractor {
    /**
     * 从方法中提取 Javadoc 内容
     * 
     * 该方法从给定的 PSI 方法元素中提取 Javadoc 文档注释，
     * 并将其转换为 TipsContent 格式以便显示。
     * 
     * @param method PSI 方法元素
     * @return 提取的 Javadoc 内容，如果没有 Javadoc 或内容为空则返回 null
     */
    fun extractJavadocFromMethod(method: PsiMethod): TipsContent?
    
    /**
     * 从字段中提取 Javadoc 内容
     * 
     * 该方法从给定的 PSI 字段元素中提取 Javadoc 文档注释，
     * 并将其转换为 TipsContent 格式以便显示。
     * 
     * @param field PSI 字段元素
     * @return 提取的 Javadoc 内容，如果没有 Javadoc 或内容为空则返回 null
     */
    fun extractJavadocFromField(field: PsiField): TipsContent?
    
    /**
     * 过滤 Javadoc 内容，移除 @tips 标签
     * 
     * 该方法处理 Javadoc 文档注释，移除所有 @tips 标签及其内容，
     * 同时保留标准 Javadoc 标签（如 @param、@return、@throws 等）和格式。
     * 
     * @param docComment PSI 文档注释
     * @return 过滤后的内容字符串，已移除 @tips 标签
     */
    fun filterJavadocContent(docComment: PsiDocComment): String
}
