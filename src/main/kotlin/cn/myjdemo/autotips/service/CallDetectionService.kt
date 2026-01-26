package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.MethodCallInfo
import cn.myjdemo.autotips.model.MethodCallContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression

/**
 * 方法调用检测服务接口
 * 负责检测和分析方法调用，确定调用的目标方法
 */
interface CallDetectionService {
    
    /**
     * 在指定位置检测方法调用
     * @param editor 编辑器实例
     * @param offset 光标位置偏移量
     * @return 方法调用信息，如果没有检测到有效调用则返回null
     */
    fun detectMethodCall(editor: Editor, offset: Int): MethodCallInfo?
    
    /**
     * 解析方法调用表达式的引用
     * @param callExpression PSI方法调用表达式
     * @return 解析到的方法，如果解析失败则返回null
     */
    fun resolveMethodReference(callExpression: PsiMethodCallExpression): PsiMethod?
    
    /**
     * 检查元素是否在有效的调用上下文中
     * @param element PSI元素
     * @return 是否为有效的调用上下文
     */
    fun isValidCallContext(element: PsiElement): Boolean
    
    /**
     * 获取方法调用的完整上下文信息
     * @param callExpression PSI方法调用表达式
     * @return 方法调用上下文，如果无法获取则返回null
     */
    fun getMethodCallContext(callExpression: PsiMethodCallExpression): MethodCallContext?
    
    /**
     * 检测链式调用中的所有方法
     * @param callExpression PSI方法调用表达式
     * @return 链式调用中的所有方法列表
     */
    fun detectChainedCalls(callExpression: PsiMethodCallExpression): List<MethodCallInfo>
    
    /**
     * 检查方法调用语法是否完整
     * @param element PSI元素
     * @return 语法是否完整
     */
    fun isCallSyntaxComplete(element: PsiElement): Boolean
}