package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.service.CacheService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 缓存服务实现类
 * 负责管理提示内容的缓存，提高性能
 * 
 * 实现特性：
 * - LRU (Least Recently Used) 淘汰策略
 * - 线程安全的并发访问
 * - 自动过期清理
 * - 缓存统计信息
 * - 内存限制管理
 */
class CacheServiceImpl : CacheService, Disposable {
    
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val accessOrder = ConcurrentHashMap<String, Long>()
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val accessCounter = AtomicLong(0)
    private var maxCacheSize = 1000
    
    // 后台清理任务调度器
    private val cleanupScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "AutoTips-CacheCleanup").apply {
            isDaemon = true
        }
    }
    
    init {
        // 每5分钟执行一次过期条目清理
        cleanupScheduler.scheduleAtFixedRate(
            { cleanupExpiredEntries() },
            5,
            5,
            TimeUnit.MINUTES
        )
    }
    
    data class CacheEntry(
        val content: TipsContent,
        val timestamp: Long = System.currentTimeMillis(),
        val accessCount: AtomicInteger = AtomicInteger(0),
        var lastAccessTime: Long = System.currentTimeMillis()
    )
    
    override fun getCachedTips(methodSignature: String): TipsContent? {
        val entry = cache[methodSignature]
        return if (entry != null) {
            // 更新访问信息
            entry.accessCount.incrementAndGet()
            entry.lastAccessTime = System.currentTimeMillis()
            accessOrder[methodSignature] = accessCounter.incrementAndGet()
            hitCount.incrementAndGet()
            entry.content
        } else {
            missCount.incrementAndGet()
            null
        }
    }
    
    override fun cacheTips(methodSignature: String, content: TipsContent) {
        // 检查缓存大小限制，如果超过限制则执行LRU淘汰
        if (cache.size >= maxCacheSize && !cache.containsKey(methodSignature)) {
            evictLeastRecentlyUsed()
        }
        
        // 存储新的缓存条目
        val entry = CacheEntry(content)
        cache[methodSignature] = entry
        accessOrder[methodSignature] = accessCounter.incrementAndGet()
    }
    
    override fun invalidateCache(project: Project) {
        // 清除与特定项目相关的缓存条目
        // 由于我们使用方法签名作为键，这里简单清除所有缓存
        // 在实际应用中，可以根据项目路径或其他标识符进行更精细的控制
        cache.clear()
        accessOrder.clear()
    }
    
    override fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        val expireTime = 30 * 60 * 1000L // 30分钟过期
        
        // 查找并移除过期条目
        val expiredKeys = cache.entries
            .filter { currentTime - it.value.lastAccessTime > expireTime }
            .map { it.key }
        
        expiredKeys.forEach { 
            cache.remove(it)
            accessOrder.remove(it)
        }
    }
    
    override fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "size" to cache.size,
            "maxSize" to maxCacheSize,
            "hitCount" to hitCount.get(),
            "missCount" to missCount.get(),
            "hitRate" to if (hitCount.get() + missCount.get() > 0) {
                hitCount.get().toDouble() / (hitCount.get() + missCount.get())
            } else 0.0
        )
    }
    
    override fun clearAllCache() {
        cache.clear()
        accessOrder.clear()
        hitCount.set(0)
        missCount.set(0)
        accessCounter.set(0)
    }
    
    override fun setMaxCacheSize(maxSize: Int) {
        maxCacheSize = maxSize
        // 如果当前缓存大小超过新的限制，清理超出的条目
        while (cache.size > maxSize) {
            evictLeastRecentlyUsed()
        }
    }
    
    override fun containsCache(methodSignature: String): Boolean {
        return cache.containsKey(methodSignature)
    }
    
    override fun removeCache(methodSignature: String): Boolean {
        val removed = cache.remove(methodSignature) != null
        if (removed) {
            accessOrder.remove(methodSignature)
        }
        return removed
    }
    
    /**
     * 淘汰最少最近使用的缓存条目 (LRU策略)
     * 
     * 策略：
     * 1. 优先淘汰访问顺序最早的条目
     * 2. 如果访问顺序相同，淘汰访问次数最少的条目
     * 3. 如果访问次数也相同，淘汰创建时间最早的条目
     */
    private fun evictLeastRecentlyUsed() {
        if (cache.isEmpty()) return
        
        // 找到访问顺序最小（最久未使用）的条目
        val lruKey = accessOrder.entries.minByOrNull { it.value }?.key
        
        lruKey?.let { 
            cache.remove(it)
            accessOrder.remove(it)
        }
    }
    
    override fun dispose() {
        shutdown()
    }

    /**
     * 关闭缓存服务，清理资源
     */
    fun shutdown() {
        cleanupScheduler.shutdown()
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            cleanupScheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
        clearAllCache()
    }
}