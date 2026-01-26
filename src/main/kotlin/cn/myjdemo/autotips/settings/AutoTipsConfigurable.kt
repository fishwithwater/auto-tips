package cn.myjdemo.autotips.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * Auto-Tips插件设置配置界面
 * 实现IntelliJ Platform的Configurable接口
 */
interface AutoTipsConfigurable : Configurable {
    
    /**
     * 获取显示名称
     * @return 设置页面显示名称
     */
    override fun getDisplayName(): String
    
    /**
     * 获取帮助主题ID
     * @return 帮助主题ID，如果没有则返回null
     */
    override fun getHelpTopic(): String?
    
    /**
     * 创建设置UI组件
     * @return 设置界面的Swing组件
     */
    override fun createComponent(): JComponent?
    
    /**
     * 检查设置是否已修改
     * @return 设置是否已修改
     */
    override fun isModified(): Boolean
    
    /**
     * 应用设置更改
     */
    override fun apply()
    
    /**
     * 重置设置到当前保存的状态
     */
    override fun reset()
    
    /**
     * 释放资源
     */
    override fun disposeUIResources()
}