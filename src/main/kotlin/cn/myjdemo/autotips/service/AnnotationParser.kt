package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsAnnotation
import cn.myjdemo.autotips.model.DocumentationContent
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment

/**
 * 注释解析器接口
 * 负责解析方法注释，提取@tips标记内容
 */
interface AnnotationParser {
    
    /**
     * 从方法中提取@tips内容
     * @param method PSI方法元素
     * @return 提取的提示内容，如果没有@tips标记则返回null
     */
    fun extractTipsContent(method: PsiMethod): TipsContent?
    
    /**
     * 解析文档注释中的注释文本
     * @param docComment PSI文档注释元素
     * @return @tips注释列表
     */
    fun parseAnnotationText(docComment: PsiDocComment): List<TipsAnnotation>
    
    /**
     * 验证@tips格式是否正确
     * @param annotation 注释字符串
     * @return 格式是否有效
     */
    fun validateTipsFormat(annotation: String): Boolean
    
    /**
     * 合并多个@tips标记的内容
     * @param annotations @tips注释列表
     * @return 合并后的提示内容
     */
    fun mergeTipsContent(annotations: List<TipsAnnotation>): TipsContent?
}