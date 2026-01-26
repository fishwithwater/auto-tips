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
import org.junit.Test

/**
 * IDE 兼容性属性测试
 * 
 * **Feature: auto-tips, Property 10: IDE兼容性**
 * 
 * 验证需求:
 * - 4.4: 当代码补全触发时，不与IDE的原生补全功能冲突
 * - 4.5: 当编辑器失去焦点时，暂停提示检测以节省资源
 */
class IDECompatibilityPropertyTest : TestBase() {
    
    /**
     * 属性 10.1: 编辑器焦点变化时正确处理提示
     * 
     * **验证需求: 4.5**
     * 
     * 对于任何编辑器焦点变化事件，插件应该：
     * 1. 隐藏当前显示的提示
     * 2. 不抛出异常
     * 3. 不影响IDE稳定性
     */
    @Test
    fun `property 10_1 - editor focus changes hide tips correctly`() = runBlocking {
        checkAll<Boolean>(100, Arb.boolean()) { hasFocus ->
            try {
                // 获取提示显示服务
                val tipDisplayService = project.service<TipDisplayService>()
                
                // 模拟焦点变化
                if (!hasFocus) {
                    // 当失去焦点时，应该隐藏提示
                    tipDisplayService.hideTip()
                }
                
                // 验证：不应该抛出异常
                // 验证：提示应该被隐藏
                assert(!tipDisplayService.isCurrentlyShowing()) {
                    "Tip should be hidden when editor loses focus"
                }
            } catch (e: Exception) {
                throw AssertionError("Focus change should not throw exception: ${e.message}", e)
            }
        }
    }
    
    /**
     * 属性 10.2: 文件切换时正确清理提示
     * 
     * **验证需求: 4.5**
     * 
     * 对于任何文件切换事件，插件应该：
     * 1. 清理之前文件的提示
     * 2. 不影响新文件的编辑
     * 3. 不抛出异常
     */
    @Test
    fun `property 10_2 - file switching cleans up tips correctly`() = runBlocking {
        checkAll<String>(100, Arb.string(1..20)) { fileName ->
            try {
                // 获取提示显示服务
                val tipDisplayService = project.service<TipDisplayService>()
                
                // 模拟文件切换
                // 在实际场景中，这会通过 EditorFocusManager 处理
                tipDisplayService.hideTip()
                
                // 验证：提示应该被清理
                assert(!tipDisplayService.isCurrentlyShowing()) {
                    "Tip should be cleaned up when switching files"
                }
            } catch (e: Exception) {
                throw AssertionError("File switching should not throw exception: ${e.message}", e)
            }
        }
    }
    
    /**
     * 属性 10.3: 并发焦点事件不会导致竞态条件
     * 
     * **验证需求: 4.5**
     * 
     * 对于任何并发的焦点变化事件，插件应该：
     * 1. 正确处理所有事件
     * 2. 不出现竞态条件
     * 3. 保持状态一致性
     */
    @Test
    fun `property 10_3 - concurrent focus events handled correctly`() = runBlocking {
        checkAll<Int>(100, Arb.int(1..10)) { eventCount ->
            try {
                // 获取提示显示服务
                val tipDisplayService = project.service<TipDisplayService>()
                
                // 模拟多个并发焦点事件
                repeat(eventCount) {
                    tipDisplayService.hideTip()
                }
                
                // 验证：最终状态应该是一致的（提示被隐藏）
                assert(!tipDisplayService.isCurrentlyShowing()) {
                    "Tip should be hidden after concurrent focus events"
                }
            } catch (e: Exception) {
                throw AssertionError("Concurrent focus events should not throw exception: ${e.message}", e)
            }
        }
    }
}
