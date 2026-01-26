package cn.myjdemo.autotips.javadoc

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.javadoc.PsiDocTagValue

/**
 * @tips 标签的 Javadoc 标签信息
 * 
 * 这个类将 @tips 注册为 IDE 识别的自定义 Javadoc 标签，
 * 使开发者在编写注释时可以获得代码补全、语法高亮和验证支持。
 */
class TipsJavadocTagInfo : JavadocTagInfo {
    
    /**
     * 标签名称
     */
    override fun getName(): String = "tips"
    
    /**
     * 检查标签是否可以在指定上下文中使用
     * @tips 标签可以用于方法、类、字段等任何地方
     */
    override fun isValidInContext(element: PsiElement?): Boolean {
        // @tips 标签可以用于方法、类、字段等任何地方
        // 但主要用于方法注释
        return true
    }
    
    /**
     * 检查标签值是否有效
     * @tips 标签需要有内容
     */
    override fun checkTagValue(value: PsiDocTagValue?): String? {
        // 如果标签值为空或只包含空白，返回错误消息
        if (value == null || value.text.isBlank()) {
            return "@tips tag should have a description"
        }
        return null
    }
    
    /**
     * 标签是否内联
     * @tips 不是内联标签（如 {@link}），而是块标签
     */
    override fun isInline(): Boolean = false
    
    /**
     * 获取标签值的引用
     * @tips 标签不需要引用其他元素，返回 null
     */
    override fun getReference(value: PsiDocTagValue?): PsiReference? {
        // @tips 标签是纯文本描述，不需要引用
        return null
    }
}
