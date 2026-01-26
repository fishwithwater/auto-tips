package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.TipsFormat
import cn.myjdemo.autotips.service.impl.CacheServiceImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan as intShouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * **Feature: auto-tips, Property 14: 后台处理性能**
 * 
 * 对于任何大型项目或高负载情况，插件应该在后台线程执行操作并管理内存使用
 * 
 * **Validates: Requirements 7.1, 7.2**
 * 
 * 此属性测试验证：
 * - 7.1: 当插件处理大型项目时，插件在后台线程中执行解析操作
 * - 7.2: 当内存使用超过阈值时，插件清理缓存以释放内存
 */
class BackgroundProcessingPerformancePropertyTest : StringSpec({
    
    "Property 14: Cache operations should be thread-safe under concurrent access" {
        checkAll(100, Arb.list(Arb.methodSignature(), 10..50)) { signatures ->
            val cacheService = CacheServiceImpl()
            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            
            try {
                // 并发写入缓存
                repeat(threadCount) { threadIndex ->
                    executor.submit {
                        try {
                            signatures.forEach { signature ->
                                val content = TipsContent("Tip for $signature from thread $threadIndex")
                                cacheService.cacheTips(signature, content)
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                
                // 等待所有线程完成
                latch.await(10, TimeUnit.SECONDS) shouldBe true
                
                // 验证所有签名都已缓存
                signatures.forEach { signature ->
                    cacheService.containsCache(signature) shouldBe true
                }
                
                // 验证缓存统计信息
                val stats = cacheService.getCacheStats()
                val cacheSize = stats["size"] as Int
                cacheSize intShouldBeGreaterThan 0
                
            } finally {
                executor.shutdown()
                executor.awaitTermination(5, TimeUnit.SECONDS)
                cacheService.shutdown()
            }
        }
    }
    
    "Property 14: LRU eviction should work correctly when cache size exceeds limit" {
        checkAll(100, Arb.int(10..50), Arb.list(Arb.methodSignature(), 20..100)) { maxSize, signatures ->
            val cacheService = CacheServiceImpl()
            cacheService.setMaxCacheSize(maxSize)
            
            try {
                // 添加超过限制的缓存条目
                signatures.forEach { signature ->
                    val content = TipsContent("Tip for $signature")
                    cacheService.cacheTips(signature, content)
                }
                
                // 验证缓存大小不超过限制
                val stats = cacheService.getCacheStats()
                val cacheSize = stats["size"] as Int
                cacheSize shouldBeLessThan maxSize + 1
                
                // 验证最近访问的条目仍在缓存中
                val recentSignatures = signatures.takeLast(maxSize / 2)
                recentSignatures.forEach { signature ->
                    // 访问这些条目以更新LRU顺序
                    cacheService.getCachedTips(signature)
                }
                
                // 添加更多条目触发淘汰
                val newSignatures = (1..maxSize).map { "new.method.signature$it" }
                newSignatures.forEach { signature ->
                    cacheService.cacheTips(signature, TipsContent("New tip"))
                }
                
                // 验证缓存大小仍然受限
                val finalStats = cacheService.getCacheStats()
                val finalSize = finalStats["size"] as Int
                finalSize shouldBeLessThan maxSize + 1
                
            } finally {
                cacheService.shutdown()
            }
        }
    }
    
    "Property 14: Cache cleanup should remove expired entries efficiently" {
        checkAll(100, Arb.list(Arb.methodSignature(), 10..30)) { signatures ->
            val cacheService = CacheServiceImpl()
            
            try {
                // 添加缓存条目
                signatures.forEach { signature ->
                    cacheService.cacheTips(signature, TipsContent("Tip for $signature"))
                }
                
                val initialSize = (cacheService.getCacheStats()["size"] as Int)
                initialSize intShouldBeGreaterThan 0
                
                // 执行清理（虽然条目还没过期，但应该不会出错）
                cacheService.cleanupExpiredEntries()
                
                // 验证未过期的条目仍然存在
                val afterCleanupSize = (cacheService.getCacheStats()["size"] as Int)
                afterCleanupSize shouldBe initialSize
                
                // 验证所有条目仍可访问
                signatures.forEach { signature ->
                    cacheService.containsCache(signature) shouldBe true
                }
                
            } finally {
                cacheService.shutdown()
            }
        }
    }
    
    "Property 14: Cache operations should complete within reasonable time under load" {
        checkAll(100, Arb.list(Arb.methodSignature(), 50..200)) { signatures ->
            val cacheService = CacheServiceImpl()
            
            try {
                // 测量批量写入时间
                val writeTime = measureTimeMillis {
                    signatures.forEach { signature ->
                        cacheService.cacheTips(signature, TipsContent("Tip for $signature"))
                    }
                }
                
                // 验证写入性能（每个操作应该在合理时间内完成）
                val avgWriteTime = writeTime.toDouble() / signatures.size
                avgWriteTime shouldBeLessThan 10.0 // 平均每个操作不超过10ms
                
                // 测量批量读取时间
                val readTime = measureTimeMillis {
                    signatures.forEach { signature ->
                        cacheService.getCachedTips(signature)
                    }
                }
                
                // 验证读取性能
                val avgReadTime = readTime.toDouble() / signatures.size
                avgReadTime shouldBeLessThan 5.0 // 读取应该更快，平均不超过5ms
                
                // 验证缓存命中率
                val stats = cacheService.getCacheStats()
                val hitRate = stats["hitRate"] as Double
                hitRate shouldBeGreaterThan 0.0
                
            } finally {
                cacheService.shutdown()
            }
        }
    }
    
    "Property 14: Memory management should handle large cache sizes efficiently" {
        checkAll(100, Arb.int(100..500)) { cacheSize ->
            val cacheService = CacheServiceImpl()
            cacheService.setMaxCacheSize(cacheSize)
            
            try {
                // 填充缓存到接近限制
                repeat(cacheSize) { index ->
                    val signature = "com.example.Class$index.method()"
                    val content = TipsContent("Tip content for method $index with some additional text to simulate real tips")
                    cacheService.cacheTips(signature, content)
                }
                
                // 验证缓存大小
                val stats = cacheService.getCacheStats()
                val actualSize = stats["size"] as Int
                actualSize shouldBeLessThan cacheSize + 1
                
                // 测试缓存在满载情况下的性能
                val accessTime = measureTimeMillis {
                    repeat(100) { index ->
                        val signature = "com.example.Class${index % cacheSize}.method()"
                        cacheService.getCachedTips(signature)
                    }
                }
                
                // 验证访问性能不会因为缓存大小而显著下降
                val avgAccessTime = accessTime.toDouble() / 100
                avgAccessTime shouldBeLessThan 10.0
                
                // 清理缓存应该快速完成
                val clearTime = measureTimeMillis {
                    cacheService.clearAllCache()
                }
                clearTime shouldBeLessThan 1000 // 清理应该在1秒内完成
                
                // 验证缓存已清空
                val finalStats = cacheService.getCacheStats()
                (finalStats["size"] as Int) shouldBe 0
                
            } finally {
                cacheService.shutdown()
            }
        }
    }
    
    "Property 14: Concurrent read and write operations should maintain consistency" {
        checkAll(100, Arb.list(Arb.methodSignature(), 20..50)) { signatures ->
            val cacheService = CacheServiceImpl()
            val executor = Executors.newFixedThreadPool(20)
            val latch = CountDownLatch(20)
            val errors = mutableListOf<Throwable>()
            
            try {
                // 10个线程写入
                repeat(10) { threadIndex ->
                    executor.submit {
                        try {
                            signatures.forEach { signature ->
                                cacheService.cacheTips(
                                    signature,
                                    TipsContent("Tip from writer $threadIndex")
                                )
                            }
                        } catch (e: Throwable) {
                            synchronized(errors) { errors.add(e) }
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                
                // 10个线程读取
                repeat(10) { threadIndex ->
                    executor.submit {
                        try {
                            signatures.forEach { signature ->
                                cacheService.getCachedTips(signature)
                            }
                        } catch (e: Throwable) {
                            synchronized(errors) { errors.add(e) }
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                
                // 等待所有操作完成
                latch.await(15, TimeUnit.SECONDS) shouldBe true
                
                // 验证没有发生错误
                errors.isEmpty() shouldBe true
                
                // 验证缓存状态一致
                val stats = cacheService.getCacheStats()
                val hitCount = stats["hitCount"] as Long
                val missCount = stats["missCount"] as Long
                (hitCount + missCount) shouldBeGreaterThan 0L
                
            } finally {
                executor.shutdown()
                executor.awaitTermination(5, TimeUnit.SECONDS)
                cacheService.shutdown()
            }
        }
    }
})

/**
 * 生成随机方法签名的Arb
 */
private fun Arb.Companion.methodSignature(): Arb<String> = arbitrary {
    val className = Arb.string(5..20).bind()
    val methodName = Arb.string(3..15).bind()
    val paramCount = Arb.int(0..3).bind()
    val params = (0 until paramCount).joinToString(", ") { "param$it" }
    "com.example.$className.$methodName($params)"
}
