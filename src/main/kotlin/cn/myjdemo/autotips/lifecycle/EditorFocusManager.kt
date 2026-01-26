package cn.myjdemo.autotips.lifecycle

import cn.myjdemo.autotips.handler.AutoCompletionDocumentListener
import cn.myjdemo.autotips.service.TipDisplayService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

/**
 * 编辑器焦点管理器
 * 
 * 负责监听编辑器焦点变化，并在适当时机隐藏提示
 * 同时为编辑器注册文档监听器以处理自动补全场景
 * 
 * 实现需求:
 * - 4.5: 当编辑器失去焦点时，暂停提示检测以节省资源
 * - 3.3: 当用户点击编辑器其他位置时，自动隐藏提示
 * - 处理IDE自动补全和输入法自动补全括号的场景
 */
class EditorFocusManager(private val project: Project) : FileEditorManagerListener {
    
    companion object {
        private val LOG = Logger.getInstance(EditorFocusManager::class.java)
        private val DOCUMENT_LISTENER_KEY = Key.create<AutoCompletionDocumentListener>("AutoTipsDocumentListener")
    }
    
    /**
     * 当选中的编辑器发生变化时
     * 
     * 需求 3.3: 当用户点击编辑器其他位置时，自动隐藏提示
     * 同时为新编辑器注册文档监听器
     * 
     * @param event 文件编辑器管理器事件
     */
    override fun selectionChanged(event: FileEditorManagerEvent) {
        try {
            // 当切换到不同的编辑器时，隐藏当前显示的提示
            val tipDisplayService = project.service<TipDisplayService>()
            if (tipDisplayService.isCurrentlyShowing()) {
                tipDisplayService.hideTip()
                LOG.debug("Tip hidden due to editor selection change")
            }
            
            // 为新编辑器注册文档监听器
            val newEditor = event.newEditor
            if (newEditor is TextEditor) {
                registerDocumentListener(newEditor.editor)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to handle editor selection change", e)
        }
    }
    
    /**
     * 当文件被打开时
     * 为编辑器注册文档监听器
     */
    override fun fileOpened(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
        try {
            val editors = source.getEditors(file)
            for (editor in editors) {
                if (editor is TextEditor) {
                    registerDocumentListener(editor.editor)
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to register document listener on file open", e)
        }
    }
    
    /**
     * 为编辑器注册文档监听器
     * 使用UserData确保每个编辑器只注册一次
     */
    private fun registerDocumentListener(editor: Editor) {
        try {
            // 检查是否已经注册过
            val existingListener = editor.getUserData(DOCUMENT_LISTENER_KEY)
            if (existingListener != null) {
                LOG.debug("Document listener already registered for this editor")
                return
            }
            
            // 创建并注册监听器
            val listener = AutoCompletionDocumentListener(editor)
            editor.document.addDocumentListener(listener)
            editor.putUserData(DOCUMENT_LISTENER_KEY, listener)
            
            LOG.info("Successfully registered document listener for editor: ${editor.document.hashCode()}")
        } catch (e: Exception) {
            LOG.warn("Failed to register document listener", e)
        }
    }
    
    /**
     * 公共方法：为编辑器注册文档监听器
     * 供外部调用（如项目初始化时）
     */
    fun registerDocumentListenerForEditor(editor: Editor) {
        registerDocumentListener(editor)
    }
    
    /**
     * 当文件被关闭时
     * 
     * 需求 3.3: 自动隐藏提示
     * 同时移除文档监听器
     * 
     * @param source 文件编辑器管理器
     */
    override fun fileClosed(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
        try {
            // 当文件关闭时，隐藏相关的提示
            val tipDisplayService = project.service<TipDisplayService>()
            if (tipDisplayService.isCurrentlyShowing()) {
                tipDisplayService.hideTip()
                LOG.debug("Tip hidden due to file close")
            }
        } catch (e: Exception) {
            LOG.warn("Failed to handle file close", e)
        }
    }
}
