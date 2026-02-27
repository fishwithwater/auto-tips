package cn.myjdemo.autotips.handler

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key

/**
 * 触发去重工具，避免多个监听器对同一右括号重复触发提示
 */
object TriggerDeduplicator {

    private val LAST_TRIGGER_INFO_KEY = Key.create<Pair<Int, Long>>("AutoTipsLastTriggerInfo")
    private const val DUPLICATE_TRIGGER_THRESHOLD_MS = 500L

    /**
     * 检查是否应该触发。若在阈值时间内同一位置已触发过，返回 false。
     */
    fun shouldTrigger(editor: Editor, offset: Int): Boolean {
        val now = System.currentTimeMillis()
        val lastTrigger = editor.getUserData(LAST_TRIGGER_INFO_KEY)

        if (lastTrigger != null) {
            val (lastOffset, lastTime) = lastTrigger
            if (now - lastTime < DUPLICATE_TRIGGER_THRESHOLD_MS && Math.abs(offset - lastOffset) <= 1) {
                return false
            }
        }

        editor.putUserData(LAST_TRIGGER_INFO_KEY, Pair(offset, now))
        return true
    }
}
