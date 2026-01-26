package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsConfiguration
import cn.myjdemo.autotips.model.TipStyle

/**
 * 配置管理服务接口
 * 负责管理插件配置和用户偏好设置
 */
interface ConfigurationService {
    
    /**
     * 检查插件是否启用
     * @return 插件是否启用
     */
    fun isPluginEnabled(): Boolean
    
    /**
     * 获取提示显示持续时间
     * @return 显示持续时间（毫秒）
     */
    fun getTipDisplayDuration(): Int
    
    /**
     * 获取提示样式
     * @return 提示样式
     */
    fun getTipStyle(): TipStyle
    
    /**
     * 获取自定义注释模式列表
     * @return 自定义注释模式列表
     */
    fun getCustomAnnotationPatterns(): List<String>
    
    /**
     * 保存配置
     * @param config 配置对象
     */
    fun saveConfiguration(config: TipsConfiguration)
    
    /**
     * 获取当前配置
     * @return 当前配置对象
     */
    fun getCurrentConfiguration(): TipsConfiguration
    
    /**
     * 重置配置到默认值
     */
    fun resetToDefaults()
    
    /**
     * 设置插件启用状态
     * @param enabled 是否启用
     */
    fun setPluginEnabled(enabled: Boolean)
    
    /**
     * 设置提示显示持续时间
     * @param duration 持续时间（毫秒）
     */
    fun setTipDisplayDuration(duration: Int)
    
    /**
     * 设置提示样式
     * @param style 提示样式
     */
    fun setTipStyle(style: TipStyle)
    
    /**
     * 添加自定义注释模式
     * @param pattern 注释模式
     */
    fun addCustomAnnotationPattern(pattern: String)
    
    /**
     * 移除自定义注释模式
     * @param pattern 注释模式
     */
    fun removeCustomAnnotationPattern(pattern: String)
}