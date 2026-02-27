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
 * Auto-Tips 插件项目关闭监听器
 * 
 * 负责处理插件在项目关闭时的清理工作
 * 
 * 实现需求:
 * - 7.5: 当用户禁用插件时，完全停止所有后台处理并释放资源
 */
class AutoTipsProjectCloseListener : ProjectManagerListener {
    
    companion object {
        private val LOG = Logger.getInstance(AutoTipsProjectCloseListener::class.java)
        private val projectConnections = mutableMapOf<Project, MessageBusConnection>()
        
        fun addConnection(project: Project, connection: MessageBusConnection) {
            projectConnections[project] = connection
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