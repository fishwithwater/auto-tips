package cn.myjdemo.autotips.lifecycle

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
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import cn.myjdemo.autotips.handler.TriggerDeduplicator
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager

/**
 * 编辑器工厂监听器
 * 监听编辑器创建事件，为每个编辑器添加文档监听器
 * 用于处理IDE自动补全和输入法自动补全括号的场景
 */
class AutoTipsEditorFactoryListener : EditorFactoryListener {
    
    companion object {
        private val LOG = Logger.getInstance(AutoTipsEditorFactoryListener::class.java)
        private val DOCUMENT_LISTENER_KEY = Key.create<DocumentListener>("AutoTipsEditorFactoryDocumentListener")
        private const val METHOD_CALL_COMPLETION_CHAR = ')'
    }
    
    /**
     * 当编辑器被创建时
     */
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        
        try {
            // 检查是否已经添加过监听器
            if (editor.getUserData(DOCUMENT_LISTENER_KEY) != null) {
                return
            }
            
            // 创建文档监听器（用于捕获右括号插入）
            val documentListener = object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    handleDocumentChanged(editor, event)
                }
            }
            
            // 添加监听器，绑定到 project 的生命周期，project 关闭时自动移除
            editor.document.addDocumentListener(documentListener, project)
            editor.putUserData(DOCUMENT_LISTENER_KEY, documentListener)
            
            LOG.info("Added document listener to editor: ${editor.document.hashCode()}")
        } catch (e: Exception) {
            LOG.warn("Failed to add listener to editor", e)
        }
    }
    
    /**
     * 处理文档变更（捕获右括号插入）
     */
    private fun handleDocumentChanged(editor: Editor, event: DocumentEvent) {
        try {
            val project = editor.project ?: return
            
            // 检查插件是否启用
            val configService = com.intellij.openapi.application.ApplicationManager.getApplication().service<ConfigurationService>()
            if (!configService.isPluginEnabled()) {
                return
            }
            
            val newFragment = event.newFragment
            if (newFragment.isEmpty()) {
                return
            }
            
            // 检查是否插入了右括号
            val fragmentStr = newFragment.toString()
            if (!fragmentStr.contains(METHOD_CALL_COMPLETION_CHAR)) {
                return
            }
            
            // 找到右括号的位置
            val parenIndex = fragmentStr.lastIndexOf(METHOD_CALL_COMPLETION_CHAR)
            val parenOffset = event.offset + parenIndex + 1
            
            // 先检查是否是重复触发（在文档变更时立即检查）
            if (!TriggerDeduplicator.shouldTrigger(editor, parenOffset)) {
                return
            }
            
            // 延迟执行，让PSI有时间更新
            ApplicationManager.getApplication().invokeLater({
                // 提交文档到PSI
                val psiDocumentManager = PsiDocumentManager.getInstance(project)
                psiDocumentManager.commitDocument(editor.document)
                
                // 触发提示检测
                handleMethodCallCompletion(editor, project, parenOffset)
            }, com.intellij.openapi.application.ModalityState.defaultModalityState())
        } catch (e: Exception) {
            LOG.warn("Error handling document change", e)
        }
    }
    
    /**
     * 当编辑器被释放时
     */
    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        
        try {
            // 移除文档监听器
            val docListener = editor.getUserData(DOCUMENT_LISTENER_KEY)
            if (docListener != null) {
                editor.document.removeDocumentListener(docListener)
                editor.putUserData(DOCUMENT_LISTENER_KEY, null)
            }
            
            LOG.debug("Removed listener from editor")
        } catch (e: Exception) {
            LOG.warn("Failed to remove listener from editor", e)
        }
    }
    
    /**
     * 处理方法调用完成
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
                                LOG.debug("No method call detected at offset $offset (document listener)")
                                return@runReadAction
                            }

                            LOG.debug("Detected method call from document listener: ${methodCallInfo.methodName}")

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
                            
                            LOG.debug("Found @tips content from document listener: ${tipsContent.content.take(50)}...")
                            
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

                                    LOG.info("Successfully displayed tip from document listener for method: ${methodCallInfo.methodName}")
                                } catch (e: Exception) {
                                    LOG.warn("Failed to display tip from document listener", e)
                                }
                            }
                        } catch (e: Exception) {
                            LOG.warn("Error in read action during document listener tip detection", e)
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("Error in background tip detection from document listener", e)
                }
            }
        } catch (e: Exception) {
            LOG.error("Critical error in handleMethodCallCompletion from document listener", e)
        }
    }
}
