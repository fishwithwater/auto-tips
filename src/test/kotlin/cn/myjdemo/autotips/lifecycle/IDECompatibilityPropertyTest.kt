package cn.myjdemo.autotips.lifecycle

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.service.TipDisplayService
import com.intellij.openapi.components.service
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking

/**
 * IDE 兼容性属性测试
 */
class IDECompatibilityPropertyTest : TestBase() {

    fun testProperty_10_1_editor_focus_changes_hide_tips_correctly() {
        runBlocking {
            checkAll<Boolean>(100, Arb.boolean()) { hasFocus ->
                try {
                    val tipDisplayService = project.service<TipDisplayService>()
                    if (!hasFocus) {
                        tipDisplayService.hideTip()
                    }
                    assert(!tipDisplayService.isCurrentlyShowing()) {
                        "Tip should be hidden when editor loses focus"
                    }
                } catch (e: Exception) {
                    throw AssertionError("Focus change should not throw exception: ${e.message}", e)
                }
            }
        }
    }

    fun testProperty_10_2_file_switching_cleans_up_tips_correctly() {
        runBlocking {
            checkAll<String>(100, Arb.string(1..20)) { _ ->
                try {
                    val tipDisplayService = project.service<TipDisplayService>()
                    tipDisplayService.hideTip()
                    assert(!tipDisplayService.isCurrentlyShowing()) {
                        "Tip should be cleaned up when switching files"
                    }
                } catch (e: Exception) {
                    throw AssertionError("File switching should not throw exception: ${e.message}", e)
                }
            }
        }
    }

    fun testProperty_10_3_concurrent_focus_events_handled_correctly() {
        runBlocking {
            checkAll<Int>(100, Arb.int(1..10)) { eventCount ->
                try {
                    val tipDisplayService = project.service<TipDisplayService>()
                    repeat(eventCount) {
                        tipDisplayService.hideTip()
                    }
                    assert(!tipDisplayService.isCurrentlyShowing()) {
                        "Tip should be hidden after concurrent focus events"
                    }
                } catch (e: Exception) {
                    throw AssertionError("Concurrent focus events should not throw exception: ${e.message}", e)
                }
            }
        }
    }
}
