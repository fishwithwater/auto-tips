package cn.myjdemo.autotips.lifecycle

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.Logger

/**
 * Auto-Tips 插件应用级别生命周期监听器
 * 
 * 负责处理插件在 IDE 启动和关闭时的全局初始化和清理工作
 * 
 * 实现需求:
 * - 7.4: 当IDE启动时，在3秒内完成初始化
 * - 7.5: 当IDE关闭时，完全停止所有后台处理并释放资源
 */
class AutoTipsApplicationLifecycleListener : AppLifecycleListener {
    
    companion object {
        private val LOG = Logger.getInstance(AutoTipsApplicationLifecycleListener::class.java)
    }
    
    /**
     * IDE 启动完成后的回调
     * 
     * 需求 7.4: 在3秒内完成初始化
     */
    override fun appStarted() {
        try {
            LOG.info("Auto-Tips plugin application lifecycle started")
            // 应用级别的初始化（如果需要）
            // 当前实现中，大部分初始化在项目级别完成
        } catch (e: Exception) {
            LOG.error("Failed to initialize Auto-Tips plugin at application level", e)
        }
    }
    
    /**
     * IDE 即将关闭时的回调
     * 
     * 需求 7.5: 完全停止所有后台处理并释放资源
     */
    override fun appWillBeClosed(isRestart: Boolean) {
        try {
            LOG.info("Auto-Tips plugin application lifecycle closing (restart: $isRestart)")
            // 应用级别的清理（如果需要）
            // 当前实现中，大部分清理在项目级别完成
        } catch (e: Exception) {
            LOG.error("Failed to clean up Auto-Tips plugin at application level", e)
        }
    }
}
