package cn.myjdemo.autotips.lifecycle

import cn.myjdemo.autotips.service.CacheService
import cn.myjdemo.autotips.service.TipDisplayService
import cn.myjdemo.autotips.service.impl.CacheServiceImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.util.messages.MessageBusConnection

/**
 * Auto-Tips 插件项目生命周期监听器
 * 
 * 负责处理插件在项目打开和关闭时的初始化和清理工作
 * 
 * 实现需求:
 * - 7.4: 当IDE启动时，在3秒内完成初始化
 * - 7.5: 当用户禁用插件时，完全停止所有后台处理并释放资源
 * - 4.4: 与IDE原生功能的兼容性处理
 */
class AutoTipsProjectLifecycleListener : ProjectManagerListener {
    
    companion object {
        private val LOG = Logger.getInstance(AutoTipsProjectLifecycleListener::class.java)
        private val projectConnections = mutableMapOf<Project, MessageBusConnection>()
    }
    
    /**
     * 项目打开时的初始化
     * 
     * 需求 7.4: 在3秒内完成初始化
     * 
     * 初始化内容:
     * 1. 预热缓存服务
     * 2. 初始化提示显示服务
     * 3. 验证配置有效性
     * 4. 注册编辑器焦点监听器
     * 5. 为已打开的编辑器注册文档监听器
     * 
     * @param project 打开的项目
     */
    override fun projectOpened(project: Project) {
        try {
            val startTime = System.currentTimeMillis()
            LOG.info("Auto-Tips plugin initializing for project: ${project.name}")
            
            // 1. 获取并初始化缓存服务
            // 缓存服务在首次访问时会自动初始化
            val cacheService = project.service<CacheService>()
            LOG.debug("Cache service initialized")
            
            // 2. 获取并初始化提示显示服务
            // 提示显示服务在首次访问时会自动初始化
            val tipDisplayService = project.service<TipDisplayService>()
            LOG.debug("Tip display service initialized")
            
            // 3. 注册编辑器焦点监听器
            // 需求 4.5: 当编辑器失去焦点时，暂停提示检测以节省资源
            val editorFocusManager = EditorFocusManager(project)
            val connection = project.messageBus.connect()
            connection.subscribe(
                com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
                editorFocusManager
            )
            projectConnections[project] = connection
            LOG.debug("Editor focus manager registered")
            
            // 4. 为已打开的编辑器注册文档监听器
            // 处理IDE自动补全和输入法自动补全括号的场景
            try {
                val fileEditorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                val allEditors = fileEditorManager.allEditors
                for (editor in allEditors) {
                    if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                        editorFocusManager.registerDocumentListenerForEditor(editor.editor)
                    }
                }
                LOG.debug("Document listeners registered for ${allEditors.size} existing editors")
            } catch (e: Exception) {
                LOG.warn("Failed to register document listeners for existing editors", e)
            }
            
            val elapsedTime = System.currentTimeMillis() - startTime
            LOG.info("Auto-Tips plugin initialized successfully in ${elapsedTime}ms for project: ${project.name}")
            
            // 需求 7.4: 确保在3秒内完成初始化
            if (elapsedTime > 3000) {
                LOG.warn("Plugin initialization took longer than expected: ${elapsedTime}ms")
            }
        } catch (e: Exception) {
            // 需求 7.3: 异常不应影响IDE稳定性
            LOG.error("Failed to initialize Auto-Tips plugin for project: ${project.name}", e)
        }
    }
    
    /**
     * 项目关闭前的清理
     * 
     * 需求 7.5: 完全停止所有后台处理并释放资源
     * 
     * 清理内容:
     * 1. 隐藏所有显示的提示
     * 2. 关闭缓存服务的后台线程
     * 3. 清理缓存数据
     * 4. 断开消息总线连接
     * 
     * @param project 即将关闭的项目
     */
    override fun projectClosing(project: Project) {
        try {
            LOG.info("Auto-Tips plugin cleaning up for project: ${project.name}")
            
            // 1. 隐藏所有显示的提示
            try {
                val tipDisplayService = project.service<TipDisplayService>()
                tipDisplayService.hideTip()
                LOG.debug("Tip display service cleaned up")
            } catch (e: Exception) {
                LOG.warn("Failed to clean up tip display service", e)
            }
            
            // 2. 关闭缓存服务
            try {
                val cacheService = project.service<CacheService>()
                if (cacheService is CacheServiceImpl) {
                    cacheService.shutdown()
                    LOG.debug("Cache service shut down")
                }
            } catch (e: Exception) {
                LOG.warn("Failed to shut down cache service", e)
            }
            
            // 3. 断开消息总线连接
            try {
                projectConnections[project]?.disconnect()
                projectConnections.remove(project)
                LOG.debug("Message bus connection disconnected")
            } catch (e: Exception) {
                LOG.warn("Failed to disconnect message bus", e)
            }
            
            LOG.info("Auto-Tips plugin cleaned up successfully for project: ${project.name}")
        } catch (e: Exception) {
            // 需求 7.3: 异常不应影响IDE稳定性
            LOG.error("Failed to clean up Auto-Tips plugin for project: ${project.name}", e)
        }
    }
}
