package cn.myjdemo.autotips.handler.impl

import cn.myjdemo.autotips.handler.TipsTypedActionHandler
import cn.myjdemo.autotips.service.CallDetectionService
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.TipDisplayService
import cn.myjdemo.autotips.service.ConfigurationService
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiLiteralExpression
import com.intellij.openapi.util.TextRange

/**
 * 提示输入动作处理器实现类
 * 监听用户输入事件，特别是方法调用完成标志（右括号")"）
 * 
 * 实现需求:
 * - 4.1: 当开发者输入方法调用的右括号")"时，立即检查该方法是否有@tips标记
 * - 4.2: 当检测到@tips标记时，在500毫秒内显示提示
 */
class TipsTypedActionHandlerImpl : TypedHandlerDelegate(), TipsTypedActionHandler {
    
    companion object {
        private val LOG = Logger.getInstance(TipsTypedActionHandlerImpl::class.java)
        private const val METHOD_CALL_COMPLETION_CHAR = ')'
        private val LAST_TRIGGER_INFO_KEY = Key.create<Pair<Int, Long>>("AutoTipsLastTriggerInfo")
        private const val DUPLICATE_TRIGGER_THRESHOLD_MS = 500L
    }
    
    /**
     * 检查是否应该触发（避免重复触发）
     */
    private fun shouldTrigger(editor: Editor, offset: Int): Boolean {
        val now = System.currentTimeMillis()
        val lastTrigger = editor.getUserData(LAST_TRIGGER_INFO_KEY)
        
        if (lastTrigger != null) {
            val (lastOffset, lastTime) = lastTrigger
            val timeDiff = now - lastTime
            
            if (timeDiff < DUPLICATE_TRIGGER_THRESHOLD_MS && Math.abs(offset - lastOffset) <= 1) {
                LOG.debug("Duplicate trigger detected at offset $offset (TypedHandler), skipping")
                return false
            }
        }
        
        editor.putUserData(LAST_TRIGGER_INFO_KEY, Pair(offset, now))
        return true
    }
    
    /**
     * 处理字符输入后的逻辑
     * 
     * 需求 4.1: 当开发者输入方法调用的右括号")"时，立即检查该方法是否有@tips标记
     * 
     * @param c 输入的字符
     * @param project 项目实例
     * @param editor 编辑器实例
     * @param file PSI文件
     * @return 处理结果
     */
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        try {
            // 检查是否应该处理该字符
            if (!shouldHandle(c, editor)) {
                return Result.CONTINUE
            }
            
            // 检查插件是否启用
            val configService = service<ConfigurationService>()
            if (!configService.isPluginEnabled()) {
                LOG.debug("Plugin is disabled, skipping tip detection")
                return Result.CONTINUE
            }
            
            // 处理方法调用完成
            if (c == METHOD_CALL_COMPLETION_CHAR) {
                handleMethodCallCompletion(editor, project)
            }
        } catch (e: Exception) {
            // 捕获所有异常，确保不影响IDE正常运行
            LOG.warn("Error in TipsTypedActionHandler.charTyped", e)
        }
        
        return Result.CONTINUE
    }
    
    /**
     * 检查是否应该处理该字符输入
     * 
     * 需求 4.1: 只在输入右括号")"时触发检测
     * 需求 2.5: 确保不在注释或字符串中触发
     * 
     * @param charTyped 输入的字符
     * @param editor 编辑器实例
     * @return 是否应该处理
     */
    override fun shouldHandle(charTyped: Char, editor: Editor): Boolean {
        try {
            // 1. 检查是否为方法调用完成字符
            if (charTyped != METHOD_CALL_COMPLETION_CHAR) {
                return false
            }
            
            // 2. 验证编辑器状态
            if (editor.project == null) {
                return false
            }
            
            // 3. 检查编辑器是否有焦点
            if (!editor.contentComponent.hasFocus()) {
                LOG.debug("Editor does not have focus, skipping tip detection")
                return false
            }
            
            // 4. 确保文档已同步到PSI
            val project = editor.project!!
            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            if (psiDocumentManager.isUncommited(editor.document)) {
                // 文档未提交，需要先提交
                psiDocumentManager.commitDocument(editor.document)
            }
            
            // 5. 获取当前位置的PSI元素
            val psiFile = psiDocumentManager.getPsiFile(editor.document) ?: return false
            val offset = editor.caretModel.offset
            val element = if (offset > 0) {
                psiFile.findElementAt(offset - 1)
            } else {
                psiFile.findElementAt(offset)
            } ?: return false
            
            // 6. 确保不在注释中
            if (isInComment(element)) {
                LOG.debug("Character typed in comment, skipping tip detection")
                return false
            }
            
            // 7. 确保不在字符串字面量中
            if (isInStringLiteral(element)) {
                LOG.debug("Character typed in string literal, skipping tip detection")
                return false
            }
            
            return true
        } catch (e: Exception) {
            LOG.warn("Error in shouldHandle check", e)
            return false
        }
    }
    
    /**
     * 处理方法调用完成事件
     * 
     * 需求 4.1: 立即检查该方法是否有@tips标记
     * 需求 4.2: 在500毫秒内显示提示
     * 
     * 实现策略:
     * 1. 使用CallDetectionService检测方法调用
     * 2. 使用AnnotationParser解析@tips注释
     * 3. 使用TipDisplayService显示提示
     * 4. 在后台线程执行以避免阻塞UI
     * 
     * @param editor 编辑器实例
     * @param project 项目实例
     */
    override fun handleMethodCallCompletion(editor: Editor, project: Project) {
        try {
            // 在后台线程中执行检测和解析，避免阻塞UI
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    // 在读操作中访问编辑器和PSI
                    ApplicationManager.getApplication().runReadAction {
                        try {
                            val offset = editor.caretModel.offset
                            
                            // 检查是否是重复触发
                            if (!shouldTrigger(editor, offset)) {
                                LOG.debug("TypedHandler trigger skipped due to duplicate detection")
                                return@runReadAction
                            }
                            
                            // 获取服务实例
                            val callDetectionService = project.service<CallDetectionService>()
                            val annotationParser = project.service<AnnotationParser>()
                            val tipDisplayService = project.service<TipDisplayService>()
                            val methodCallInfo = callDetectionService.detectMethodCall(editor, offset)
                            
                            if (methodCallInfo == null) {
                                LOG.debug("No method call detected at offset $offset")
                                return@runReadAction
                            }
                            
                            LOG.debug("Detected method call: ${methodCallInfo.methodName} in ${methodCallInfo.qualifiedClassName}")
                            
                            // 2. 解析@tips注释
                            val tipsContent = annotationParser.extractTipsContent(methodCallInfo.psiMethod)
                            
                            if (tipsContent == null) {
                                LOG.debug("No @tips annotation found for method: ${methodCallInfo.methodName}")
                                return@runReadAction
                            }
                            
                            LOG.debug("Found @tips content: ${tipsContent.content.take(50)}...")
                            
                            // 3. 在UI线程中显示提示
                            // 需求 4.2: 在500毫秒内显示提示
                            ApplicationManager.getApplication().invokeLater {
                                try {
                                    // 检查是否可以显示新提示（处理冲突）
                                    if (!tipDisplayService.canShowNewTip(tipsContent)) {
                                        LOG.debug("Cannot show new tip, current tip is still showing")
                                        return@invokeLater
                                    }
                                    
                                    // 显示提示
                                    ApplicationManager.getApplication().runReadAction {
                                        val position = editor.caretModel.logicalPosition
                                        tipDisplayService.showTip(tipsContent, editor, position)
                                    }
                                    
                                    LOG.info("Successfully displayed tip for method: ${methodCallInfo.methodName}")
                                } catch (e: Exception) {
                                    LOG.warn("Failed to display tip", e)
                                }
                            }
                        } catch (e: Exception) {
                            LOG.warn("Error in read action during tip detection", e)
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("Error in background tip detection", e)
                }
            }
        } catch (e: Exception) {
            // 捕获所有异常，确保不影响IDE正常运行
            // 需求 7.3: 当解析过程出现异常时，记录错误日志但不崩溃IDE
            LOG.error("Critical error in handleMethodCallCompletion", e)
        }
    }
    
    /**
     * 检查PSI元素是否在注释中
     * 
     * @param element PSI元素
     * @return 是否在注释中
     */
    private fun isInComment(element: com.intellij.psi.PsiElement): Boolean {
        var current: com.intellij.psi.PsiElement? = element
        while (current != null) {
            if (current is PsiComment) {
                return true
            }
            // 检查是否是文档注释
            if (current.javaClass.simpleName.contains("DocComment")) {
                return true
            }
            // 只向上检查几层，避免过度遍历
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
     * 
     * @param element PSI元素
     * @return 是否在字符串字面量中
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
            // 只向上检查几层
            if (current is com.intellij.psi.PsiStatement || 
                current is com.intellij.psi.PsiMethod) {
                break
            }
            current = current.parent
        }
        return false
    }
}
