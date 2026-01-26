package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.model.DisplayConfiguration
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition

/**
 * 提示显示服务接口
 * 负责管理提示的显示逻辑和UI交互
 */
interface TipDisplayService {
    
    /**
     * 显示提示
     * @param content 提示内容
     * @param editor 编辑器实例
     * @param position 显示位置
     */
    fun showTip(content: TipsContent, editor: Editor, position: LogicalPosition)
    
    /**
     * 隐藏当前显示的提示
     */
    fun hideTip()
    
    /**
     * 检查是否正在显示提示
     * @return 是否正在显示提示
     */
    fun isCurrentlyShowing(): Boolean
    
    /**
     * 更新提示位置
     * @param editor 编辑器实例
     */
    fun updateTipPosition(editor: Editor)
    
    /**
     * 显示带有自定义配置的提示
     * @param content 提示内容
     * @param editor 编辑器实例
     * @param position 显示位置
     * @param config 显示配置
     */
    fun showTipWithConfig(content: TipsContent, editor: Editor, position: LogicalPosition, config: DisplayConfiguration)
    
    /**
     * 检查是否可以显示新的提示（处理冲突）
     * @param content 新的提示内容
     * @return 是否可以显示
     */
    fun canShowNewTip(content: TipsContent): Boolean
    
    /**
     * 获取当前显示的提示内容
     * @return 当前提示内容，如果没有显示则返回null
     */
    fun getCurrentTipContent(): TipsContent?
    
    /**
     * 设置提示自动隐藏的超时时间
     * @param timeoutMs 超时时间（毫秒）
     */
    fun setAutoHideTimeout(timeoutMs: Long)
}