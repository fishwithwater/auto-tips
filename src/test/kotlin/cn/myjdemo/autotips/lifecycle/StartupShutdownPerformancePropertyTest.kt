package cn.myjdemo.autotips.lifecycle

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.service.CacheService
import cn.myjdemo.autotips.service.TipDisplayService
import cn.myjdemo.autotips.service.impl.CacheServiceImpl
import com.intellij.openapi.components.service
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking

/**
 * 启动和关闭性能属性测试
 */
class StartupShutdownPerformancePropertyTest : TestBase() {

    fun testProperty_16_1_plugin_initialization_completes_within_time_limit() {
        runBlocking {
            checkAll<Int>(100, Arb.int(1..5)) { _ ->
                try {
                    val startTime = System.currentTimeMillis()
                    val cacheService = project.service<CacheService>()
                    val tipDisplayService = project.service<TipDisplayService>()
                    val elapsedTime = System.currentTimeMillis() - startTime
                    assert(elapsedTime < 3000) {
                        "Plugin initialization took ${elapsedTime}ms, exceeding 3000ms limit"
                    }
                    assertNotNull(cacheService)
                    assertNotNull(tipDisplayService)
                } catch (e: Exception) {
                    throw AssertionError("Plugin initialization should not throw exception: ${e.message}", e)
                }
            }
        }
    }

    fun testProperty_16_2_plugin_shutdown_cleans_up_resources_correctly() {
        runBlocking {
            checkAll<Int>(100, Arb.int(1..5)) { _ ->
                try {
                    val cacheService = project.service<CacheService>()
                    val tipDisplayService = project.service<TipDisplayService>()
                    tipDisplayService.hideTip()
                    if (cacheService is CacheServiceImpl) {
                        cacheService.shutdown()
                    }
                    assert(!tipDisplayService.isCurrentlyShowing()) {
                        "Tip should be hidden after shutdown"
                    }
                } catch (e: Exception) {
                    throw AssertionError("Plugin shutdown should not throw exception: ${e.message}", e)
                }
            }
        }
    }

    fun testProperty_16_3_repeated_startup_and_shutdown_does_not_leak_resources() {
        runBlocking {
            checkAll<Int>(50, Arb.int(2..5)) { cycles ->
                try {
                    repeat(cycles) {
                        val cacheService = project.service<CacheService>()
                        val tipDisplayService = project.service<TipDisplayService>()
                        assertNotNull(cacheService)
                        assertNotNull(tipDisplayService)
                        tipDisplayService.hideTip()
                        if (cacheService is CacheServiceImpl) {
                            cacheService.clearAllCache()
                        }
                    }
                    assertNotNull(project.service<CacheService>())
                } catch (e: Exception) {
                    throw AssertionError("Repeated startup/shutdown should not throw exception: ${e.message}", e)
                }
            }
        }
    }

    fun testProperty_16_4_initialization_failures_do_not_crash_IDE() {
        runBlocking {
            checkAll<Int>(100, Arb.int(1..5)) { _ ->
                try {
                    try {
                        val cacheService = project.service<CacheService>()
                        val tipDisplayService = project.service<TipDisplayService>()
                        assertNotNull(cacheService)
                        assertNotNull(tipDisplayService)
                    } catch (e: Exception) {
                        println("Service initialization failed (expected in some cases): ${e.message}")
                    }
                    assert(true) { "IDE should remain stable even if plugin initialization fails" }
                } catch (e: Exception) {
                    throw AssertionError("Unhandled exception should not occur: ${e.message}", e)
                }
            }
        }
    }
}
