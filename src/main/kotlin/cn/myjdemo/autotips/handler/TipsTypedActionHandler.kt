package cn.myjdemo.autotips.handler

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * 提示输入动作处理器接口
 * 监听用户输入事件，特别是方法调用完成标志（右括号")"）
 */
interface TipsTypedActionHandler {
    
    /**
     * 处理字符输入后的逻辑
     * @param c 输入的字符
     * @param project 项目实例
     * @param editor 编辑器实例
     * @param file PSI文件
     * @return 处理结果
     */
    fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): TypedHandlerDelegate.Result
    
    /**
     * 检查是否应该处理该字符输入
     * @param charTyped 输入的字符
     * @param editor 编辑器实例
     * @return 是否应该处理
     */
    fun shouldHandle(charTyped: Char, editor: Editor): Boolean
    
    /**
     * 处理方法调用完成事件
     * @param editor 编辑器实例
     * @param project 项目实例
     */
    fun handleMethodCallCompletion(editor: Editor, project: Project)
}