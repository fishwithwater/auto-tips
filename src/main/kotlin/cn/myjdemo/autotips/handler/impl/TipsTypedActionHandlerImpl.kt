package cn.myjdemo.autotips.handler.impl

import cn.myjdemo.autotips.handler.TipsTypedActionHandler
import cn.myjdemo.autotips.service.CallDetectionService
import cn.myjdemo.autotips.service.AnnotationParser
import cn.myjdemo.autotips.service.JavadocExtractor
import cn.myjdemo.autotips.service.TipDisplayService
import cn.myjdemo.autotips.service.ConfigurationService
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import cn.myjdemo.autotips.handler.TriggerDeduplicator
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiLiteralExpression

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
            val configService = ApplicationManager.getApplication().service<ConfigurationService>()
            val isEnabled = configService.isPluginEnabled()
            LOG.debug("Plugin enabled check: $isEnabled")
            if (!isEnabled) {
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
     * 需求 1.2, 1.3: 根据配置选择显示 @tips 或 Javadoc 内容
     * 需求 3.1, 3.2, 3.3: 两种模式使用相同的显示服务和逻辑
     * 需求 5.1, 5.2: 支持模式切换
     * 
     * 实现策略:
     * 1. 使用CallDetectionService检测方法调用
     * 2. 根据配置选择使用AnnotationParser或JavadocExtractor
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
                            if (!TriggerDeduplicator.shouldTrigger(editor, offset)) {
                                LOG.debug("TypedHandler trigger skipped due to duplicate detection")
                                return@runReadAction
                            }
                            
                            // 获取服务实例
                            val callDetectionService = project.service<CallDetectionService>()
                            val configService = ApplicationManager.getApplication().service<ConfigurationService>()
                            val tipDisplayService = project.service<TipDisplayService>()
                            
                            // 调试：输出当前配置状态
                            val currentConfig = configService.getCurrentConfiguration()
                            LOG.info("=== Configuration Debug ===")
                            LOG.info("Current configuration: enabled=${currentConfig.enabled}, javadocMode=${currentConfig.javadocModeEnabled}, style=${currentConfig.style}, duration=${currentConfig.displayDuration}")
                            LOG.info("ConfigService class: ${configService::class.simpleName}")
                            LOG.info("ConfigService instance: ${configService.hashCode()}")
                            
                            val methodCallInfo = callDetectionService.detectMethodCall(editor, offset)
                            
                            if (methodCallInfo == null) {
                                LOG.debug("No method call detected at offset $offset")
                                return@runReadAction
                            }
                            
                            LOG.debug("Detected method call: ${methodCallInfo.methodName} in ${methodCallInfo.qualifiedClassName}")
                            
                            // 根据配置选择提取器
                            // 需求 1.2, 1.3: 当复选框未勾选时显示 @tips，勾选时显示 Javadoc
                            val isJavadocMode = configService.isJavadocModeEnabled()
                            LOG.info("=== Mode Selection ===")
                            LOG.info("Javadoc mode enabled: $isJavadocMode")
                            LOG.info("Will use: ${if (isJavadocMode) "JavadocExtractor" else "AnnotationParser"}")
                            
                            val tipsContent = if (isJavadocMode) {
                                // 使用 Javadoc 提取器
                                LOG.info("Using Javadoc mode for method: ${methodCallInfo.methodName}")
                                val javadocExtractor = project.service<JavadocExtractor>()
                                LOG.info("JavadocExtractor class: ${javadocExtractor::class.simpleName}")
                                val result = javadocExtractor.extractJavadocFromMethod(methodCallInfo.psiMethod)
                                LOG.info("Javadoc extraction result: ${if (result != null) "SUCCESS (${result.content.length} chars)" else "NULL"}")
                                result
                            } else {
                                // 使用现有的 @tips 解析器
                                LOG.info("Using @tips mode for method: ${methodCallInfo.methodName}")
                                val annotationParser = project.service<AnnotationParser>()
                                LOG.info("AnnotationParser class: ${annotationParser::class.simpleName}")
                                val result = annotationParser.extractTipsContent(methodCallInfo.psiMethod)
                                LOG.info("Tips extraction result: ${if (result != null) "SUCCESS (${result.content.length} chars)" else "NULL"}")
                                result
                            }
                            
                            if (tipsContent == null) {
                                LOG.debug("No content found for method: ${methodCallInfo.methodName}")
                                return@runReadAction
                            }
                            
                            LOG.debug("Found content: ${tipsContent.content.take(50)}...")
                            
                            // 3. 在UI线程中显示提示
                            // 需求 4.2: 在500毫秒内显示提示
                            // 需求 3.1, 3.2, 3.3: 两种模式使用相同的显示服务和逻辑
                            ApplicationManager.getApplication().invokeLater {
                                try {
                                    // 检查是否可以显示新提示（处理冲突）
                                    if (!tipDisplayService.canShowNewTip(tipsContent)) {
                                        LOG.debug("Cannot show new tip, current tip is still showing")
                                        return@invokeLater
                                    }

                                    // 在EDT上直接读取位置并显示提示，showTip创建Swing组件必须在EDT执行
                                    val position = editor.caretModel.logicalPosition
                                    tipDisplayService.showTip(tipsContent, editor, position)

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
