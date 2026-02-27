package cn.myjdemo.autotips.handler

import cn.myjdemo.autotips.service.CallDetectionService
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.TipDisplayService
import cn.myjdemo.autotips.service.ConfigurationService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiFile

/**
 * 自动补全文档监听器
 * 处理IDE自动补全和输入法自动补全括号的场景
 * 
 * 场景:
 * 1. IDE自动补全: 用户按Enter选择补全建议，IDE一次性插入方法调用
 * 2. 输入法补全: 用户输入左括号，IDE自动补全右括号
 */
class AutoCompletionDocumentListener(private val editor: Editor) : DocumentListener {
    
    companion object {
        private val LOG = Logger.getInstance(AutoCompletionDocumentListener::class.java)
        private const val METHOD_CALL_COMPLETION_CHAR = ')'
    }
    
    /**
     * 文档变更后的处理
     * 检测是否插入了右括号，如果是则触发提示检测
     */
    override fun documentChanged(event: DocumentEvent) {
        try {
            val project = editor.project ?: return
            
            LOG.debug("Document changed: offset=${event.offset}, newFragment='${event.newFragment}', oldFragment='${event.oldFragment}'")
            
            // 检查插件是否启用
            val configService = com.intellij.openapi.application.ApplicationManager.getApplication().service<ConfigurationService>()
            if (!configService.isPluginEnabled()) {
                LOG.debug("Plugin is disabled")
                return
            }
            
            // 检查是否插入了包含右括号的文本
            val newFragment = event.newFragment
            if (newFragment.isEmpty() || !newFragment.contains(METHOD_CALL_COMPLETION_CHAR)) {
                LOG.debug("No closing parenthesis in new fragment")
                return
            }
            
            LOG.debug("Closing parenthesis detected in new fragment: '$newFragment'")
            
            // 获取插入位置 - 找到右括号的实际位置
            val fragmentStr = newFragment.toString()
            val parenIndex = fragmentStr.lastIndexOf(METHOD_CALL_COMPLETION_CHAR)
            val offset = event.offset + parenIndex + 1
            
            LOG.debug("Calculated offset for closing parenthesis: $offset")
            
            // 延迟一小段时间，确保PSI已更新
            ApplicationManager.getApplication().invokeLater {
                // 提交文档变更到PSI
                val psiDocumentManager = PsiDocumentManager.getInstance(project)
                psiDocumentManager.commitDocument(editor.document)
                
                LOG.debug("Document committed to PSI")
                
                // 检查是否应该处理
                if (shouldHandleAutoCompletion(editor, offset)) {
                    LOG.debug("Should handle auto-completion, triggering tip detection")
                    handleMethodCallCompletion(editor, project, offset)
                } else {
                    LOG.debug("Should not handle auto-completion at offset $offset")
                }
            }
        } catch (e: Exception) {
            LOG.warn("Error in AutoCompletionDocumentListener.documentChanged", e)
        }
    }
    
    /**
     * 检查是否应该处理自动补全
     */
    private fun shouldHandleAutoCompletion(editor: Editor, offset: Int): Boolean {
        try {
            val project = editor.project ?: return false
            
            // 检查编辑器是否有焦点
            if (!editor.contentComponent.hasFocus()) {
                return false
            }
            
            // 获取PSI文件
            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            val psiFile = psiDocumentManager.getPsiFile(editor.document) ?: return false
            
            // 获取右括号位置的PSI元素
            val element = if (offset > 0) {
                psiFile.findElementAt(offset - 1)
            } else {
                psiFile.findElementAt(offset)
            } ?: return false
            
            // 确保不在注释中
            if (isInComment(element)) {
                LOG.debug("Auto-completion in comment, skipping tip detection")
                return false
            }
            
            // 确保不在字符串字面量中
            if (isInStringLiteral(element)) {
                LOG.debug("Auto-completion in string literal, skipping tip detection")
                return false
            }
            
            return true
        } catch (e: Exception) {
            LOG.warn("Error in shouldHandleAutoCompletion check", e)
            return false
        }
    }
    
    /**
     * 处理方法调用完成事件
     */
    private fun handleMethodCallCompletion(editor: Editor, project: com.intellij.openapi.project.Project, offset: Int) {
        try {
            // 在后台线程中执行检测和解析
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    // 在读操作中访问编辑器和PSI
                    ApplicationManager.getApplication().runReadAction {
                        try {
                            // 获取服务实例
                            val callDetectionService = project.service<CallDetectionService>()
                            val configService = ApplicationManager.getApplication().service<ConfigurationService>()
                            val tipDisplayService = project.service<TipDisplayService>()

                            // 检测方法调用
                            val methodCallInfo = callDetectionService.detectMethodCall(editor, offset)

                            if (methodCallInfo == null) {
                                LOG.debug("No method call detected at offset $offset")
                                return@runReadAction
                            }

                            LOG.debug("Detected method call from auto-completion: ${methodCallInfo.methodName}")

                            // 根据配置选择提取器（与 TipsTypedActionHandlerImpl 保持一致）
                            val tipsContent = if (configService.isJavadocModeEnabled()) {
                                val javadocExtractor = project.service<cn.myjdemo.autotips.service.JavadocExtractor>()
                                javadocExtractor.extractJavadocFromMethod(methodCallInfo.psiMethod)
                            } else {
                                val annotationParser = project.service<AnnotationParser>()
                                annotationParser.extractTipsContent(methodCallInfo.psiMethod)
                            }
                            
                            if (tipsContent == null) {
                                LOG.debug("No @tips annotation found for method: ${methodCallInfo.methodName}")
                                return@runReadAction
                            }
                            
                            LOG.debug("Found @tips content from auto-completion: ${tipsContent.content.take(50)}...")
                            
                            // 在UI线程中显示提示，showTip创建Swing组件必须在EDT执行
                            ApplicationManager.getApplication().invokeLater {
                                try {
                                    // 检查是否可以显示新提示
                                    if (!tipDisplayService.canShowNewTip(tipsContent)) {
                                        LOG.debug("Cannot show new tip, current tip is still showing")
                                        return@invokeLater
                                    }

                                    val position = editor.caretModel.logicalPosition
                                    tipDisplayService.showTip(tipsContent, editor, position)

                                    LOG.info("Successfully displayed tip from auto-completion for method: ${methodCallInfo.methodName}")
                                } catch (e: Exception) {
                                    LOG.warn("Failed to display tip from auto-completion", e)
                                }
                            }
                        } catch (e: Exception) {
                            LOG.warn("Error in read action during auto-completion tip detection", e)
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("Error in background tip detection from auto-completion", e)
                }
            }
        } catch (e: Exception) {
            LOG.error("Critical error in handleMethodCallCompletion from auto-completion", e)
        }
    }
    
    /**
     * 检查PSI元素是否在注释中
     */
    private fun isInComment(element: com.intellij.psi.PsiElement): Boolean {
        var current: com.intellij.psi.PsiElement? = element
        while (current != null) {
            if (current is PsiComment) {
                return true
            }
            if (current.javaClass.simpleName.contains("DocComment")) {
                return true
            }
            if (current is com.intellij.psi.PsiMethod || 
                current is com.intellij.psi.PsiClass || 
                current is PsiFile) {
                break
            }
            current = current.parent
        }
        return false
    }
    
    /**
     * 检查PSI元素是否在字符串字面量中
     */
    private fun isInStringLiteral(element: com.intellij.psi.PsiElement): Boolean {
        var current: com.intellij.psi.PsiElement? = element
        while (current != null) {
            if (current is PsiLiteralExpression) {
                val value = current.value
                if (value is String) {
                    return true
                }
            }
            if (current is com.intellij.psi.PsiStatement || 
                current is com.intellij.psi.PsiMethod) {
                break
            }
            current = current.parent
        }
        return false
    }
}
