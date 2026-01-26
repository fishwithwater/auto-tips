package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent
import com.intellij.psi.PsiElement

/**
 * 语言特定解析器接口
 * 
 * 为不同编程语言提供特定的注释解析策略
 * 
 * 需求 5.1: 支持Java语言的方法注释和调用解析
 * 需求 5.2: 支持Kotlin语言的方法注释和调用解析
 * 需求 5.3: 根据文件类型选择合适的解析器
 */
interface LanguageSpecificParser {
    
    /**
     * 检查此解析器是否支持给定的PSI元素
     * 
     * @param element PSI元素
     * @return 是否支持
     */
    fun supports(element: PsiElement): Boolean
    
    /**
     * 从方法元素中提取@tips内容
     * 
     * @param methodElement 方法PSI元素（可能是PsiMethod或KtFunction）
     * @return 提取的提示内容，如果没有则返回null
     */
    fun extractTipsFromMethod(methodElement: PsiElement): TipsContent?
    
    /**
     * 获取语言名称
     * 
     * @return 语言名称（如"Java"、"Kotlin"）
     */
    fun getLanguageName(): String
}
