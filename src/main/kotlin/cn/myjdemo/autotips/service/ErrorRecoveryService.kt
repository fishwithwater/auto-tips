package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipsContent

/**
 * 错误恢复服务接口
 * 负责处理各种异常情况和错误恢复
 */
interface ErrorRecoveryService {
    
    /**
     * 处理解析错误
     * @param exception 异常实例
     * @param context 解析上下文
     * @return 恢复动作
     */
    fun handleParsingError(exception: Exception, context: ParsingContext): RecoveryAction
    
    /**
     * 处理显示错误
     * @param exception 异常实例
     * @param content 提示内容
     * @return 恢复动作
     */
    fun handleDisplayError(exception: Exception, content: TipsContent): RecoveryAction
    
    /**
     * 处理性能问题
     * @param issue 性能问题
     * @return 恢复动作
     */
    fun handlePerformanceIssue(issue: PerformanceIssue): RecoveryAction
    
    /**
     * 记录错误日志
     * @param message 错误消息
     * @param exception 异常实例
     */
    fun logError(message: String, exception: Exception? = null)
    
    /**
     * 记录警告日志
     * @param message 警告消息
     */
    fun logWarning(message: String)
}

/**
 * 解析上下文数据类
 */
data class ParsingContext(
    val methodName: String,
    val className: String,
    val filePath: String,
    val lineNumber: Int
)

/**
 * 性能问题数据类
 */
data class PerformanceIssue(
    val type: PerformanceIssueType,
    val description: String,
    val severity: Severity
)

/**
 * 性能问题类型枚举
 */
enum class PerformanceIssueType {
    MEMORY_USAGE_HIGH,
    RESPONSE_TIME_SLOW,
    THREAD_POOL_SATURATED,
    CACHE_MISS_RATE_HIGH
}

/**
 * 严重程度枚举
 */
enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * 恢复动作枚举
 */
enum class RecoveryAction {
    RETRY,
    SKIP,
    FALLBACK,
    DISABLE_FEATURE,
    RESET_STATE
}