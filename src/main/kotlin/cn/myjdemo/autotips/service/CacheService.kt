package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent
import com.intellij.openapi.project.Project

/**
 * 缓存服务接口
 * 负责管理提示内容的缓存，提高性能
 */
interface CacheService {
    
    /**
     * 获取缓存的提示内容
     * @param methodSignature 方法签名
     * @return 缓存的提示内容，如果没有缓存则返回null
     */
    fun getCachedTips(methodSignature: String): TipsContent?
    
    /**
     * 缓存提示内容
     * @param methodSignature 方法签名
     * @param content 提示内容
     */
    fun cacheTips(methodSignature: String, content: TipsContent)
    
    /**
     * 使项目的缓存失效
     * @param project 项目实例
     */
    fun invalidateCache(project: Project)
    
    /**
     * 清理过期的缓存条目
     */
    fun cleanupExpiredEntries()
    
    /**
     * 获取缓存统计信息
     * @return 缓存统计信息映射
     */
    fun getCacheStats(): Map<String, Any>
    
    /**
     * 清空所有缓存
     */
    fun clearAllCache()
    
    /**
     * 设置缓存大小限制
     * @param maxSize 最大缓存条目数
     */
    fun setMaxCacheSize(maxSize: Int)
    
    /**
     * 检查缓存是否包含指定的方法签名
     * @param methodSignature 方法签名
     * @return 是否包含该缓存
     */
    fun containsCache(methodSignature: String): Boolean
    
    /**
     * 移除指定方法签名的缓存
     * @param methodSignature 方法签名
     * @return 是否成功移除
     */
    fun removeCache(methodSignature: String): Boolean
}