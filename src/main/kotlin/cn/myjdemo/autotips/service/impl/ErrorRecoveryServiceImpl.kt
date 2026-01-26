package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.TipsContent
import cn.myjdemo.autotips.service.*
import com.intellij.openapi.diagnostic.Logger

/**
 * 错误恢复服务实现类
 * 负责处理各种异常情况和错误恢复
 * 
 * 实现需求:
 * - 7.3: 当解析过程出现异常时，记录错误日志但不崩溃IDE
 */
class ErrorRecoveryServiceImpl : ErrorRecoveryService {
    
    companion object {
        private val LOG = Logger.getInstance(ErrorRecoveryServiceImpl::class.java)
        
        // 错误计数器，用于跟踪错误频率
        private val errorCounters = mutableMapOf<String, Int>()
        
        // 错误阈值，超过此值将采取更严格的恢复动作
        private const val ERROR_THRESHOLD = 10
        
        // 性能问题阈值
        private const val MEMORY_THRESHOLD_MB = 100
        private const val RESPONSE_TIME_THRESHOLD_MS = 1000
    }
    
    /**
     * 处理解析错误
     * 
     * 需求 7.3: 当解析过程出现异常时，记录错误日志但不崩溃IDE
     * 
     * @param exception 异常实例
     * @param context 解析上下文
     * @return 恢复动作
     */
    override fun handleParsingError(exception: Exception, context: ParsingContext): RecoveryAction {
        // 记录错误
        val errorKey = "parsing_${context.className}_${context.methodName}"
        val errorCount = incrementErrorCounter(errorKey)
        
        // 记录详细的错误日志
        logError(
            "Parsing error in ${context.className}.${context.methodName} " +
            "at ${context.filePath}:${context.lineNumber} (occurrence: $errorCount)",
            exception
        )
        
        // 根据错误类型和频率决定恢复动作
        return when {
            // 如果是空指针异常，跳过当前解析
            exception is NullPointerException -> {
                logWarning("Null pointer encountered during parsing, skipping")
                RecoveryAction.SKIP
            }
            
            // 如果是类型转换异常，使用回退方案
            exception is ClassCastException -> {
                logWarning("Type cast error during parsing, using fallback")
                RecoveryAction.FALLBACK
            }
            
            // 如果错误频率过高，禁用该功能
            errorCount > ERROR_THRESHOLD -> {
                logError("Error threshold exceeded for $errorKey, disabling feature", null)
                RecoveryAction.DISABLE_FEATURE
            }
            
            // 其他情况，跳过当前解析
            else -> {
                logWarning("Unknown parsing error, skipping")
                RecoveryAction.SKIP
            }
        }
    }
    
    /**
     * 处理显示错误
     * 
     * 需求 7.3: 确保UI异常不影响IDE稳定性
     * 
     * @param exception 异常实例
     * @param content 提示内容
     * @return 恢复动作
     */
    override fun handleDisplayError(exception: Exception, content: TipsContent): RecoveryAction {
        // 记录错误
        val errorKey = "display_error"
        val errorCount = incrementErrorCounter(errorKey)
        
        // 记录错误日志
        logError(
            "Display error for content: ${content.content.take(50)}... (occurrence: $errorCount)",
            exception
        )
        
        // 根据错误类型决定恢复动作
        return when {
            // 如果是UI相关异常，使用回退显示方式
            exception.message?.contains("UI") == true ||
            exception.message?.contains("Swing") == true ||
            exception.message?.contains("AWT") == true -> {
                logWarning("UI error detected, using fallback display method")
                RecoveryAction.FALLBACK
            }
            
            // 如果错误频率过高，暂时禁用显示功能
            errorCount > ERROR_THRESHOLD -> {
                logError("Display error threshold exceeded, disabling display temporarily", null)
                RecoveryAction.DISABLE_FEATURE
            }
            
            // 其他情况，跳过当前显示
            else -> {
                logWarning("Unknown display error, skipping")
                RecoveryAction.SKIP
            }
        }
    }
    
    /**
     * 处理性能问题
     * 
     * 需求 7.1: 在后台线程中执行解析操作
     * 需求 7.2: 当内存使用超过阈值时，清理缓存以释放内存
     * 
     * @param issue 性能问题
     * @return 恢复动作
     */
    override fun handlePerformanceIssue(issue: PerformanceIssue): RecoveryAction {
        // 记录性能问题
        logWarning("Performance issue detected: ${issue.type} - ${issue.description} (severity: ${issue.severity})")
        
        // 根据问题类型和严重程度决定恢复动作
        return when (issue.type) {
            PerformanceIssueType.MEMORY_USAGE_HIGH -> {
                when (issue.severity) {
                    Severity.CRITICAL, Severity.HIGH -> {
                        logWarning("High memory usage detected, resetting state and clearing caches")
                        RecoveryAction.RESET_STATE
                    }
                    Severity.MEDIUM -> {
                        logWarning("Medium memory usage detected, clearing caches")
                        RecoveryAction.FALLBACK
                    }
                    else -> {
                        logWarning("Low memory usage detected, monitoring")
                        RecoveryAction.SKIP
                    }
                }
            }
            
            PerformanceIssueType.RESPONSE_TIME_SLOW -> {
                when (issue.severity) {
                    Severity.CRITICAL, Severity.HIGH -> {
                        logWarning("Critical response time issue, disabling feature temporarily")
                        RecoveryAction.DISABLE_FEATURE
                    }
                    else -> {
                        logWarning("Slow response time detected, using fallback")
                        RecoveryAction.FALLBACK
                    }
                }
            }
            
            PerformanceIssueType.THREAD_POOL_SATURATED -> {
                when (issue.severity) {
                    Severity.CRITICAL, Severity.HIGH -> {
                        logWarning("Thread pool saturated, skipping non-critical operations")
                        RecoveryAction.SKIP
                    }
                    else -> {
                        logWarning("Thread pool under pressure, monitoring")
                        RecoveryAction.RETRY
                    }
                }
            }
            
            PerformanceIssueType.CACHE_MISS_RATE_HIGH -> {
                logWarning("High cache miss rate detected, resetting cache")
                RecoveryAction.RESET_STATE
            }
        }
    }
    
    /**
     * 记录错误日志
     * 
     * 需求 7.3: 记录错误日志但不崩溃IDE
     * 
     * @param message 错误消息
     * @param exception 异常实例
     */
    override fun logError(message: String, exception: Exception?) {
        if (exception != null) {
            LOG.error(message, exception)
        } else {
            LOG.error(message)
        }
    }
    
    /**
     * 记录警告日志
     * 
     * @param message 警告消息
     */
    override fun logWarning(message: String) {
        LOG.warn(message)
    }
    
    /**
     * 增加错误计数器
     * 
     * @param errorKey 错误键
     * @return 当前错误计数
     */
    private fun incrementErrorCounter(errorKey: String): Int {
        synchronized(errorCounters) {
            val currentCount = errorCounters.getOrDefault(errorKey, 0) + 1
            errorCounters[errorKey] = currentCount
            return currentCount
        }
    }
    
    /**
     * 重置错误计数器
     * 
     * @param errorKey 错误键，如果为null则重置所有计数器
     */
    fun resetErrorCounter(errorKey: String? = null) {
        synchronized(errorCounters) {
            if (errorKey != null) {
                errorCounters.remove(errorKey)
            } else {
                errorCounters.clear()
            }
        }
    }
    
    /**
     * 获取错误计数
     * 
     * @param errorKey 错误键
     * @return 错误计数
     */
    fun getErrorCount(errorKey: String): Int {
        synchronized(errorCounters) {
            return errorCounters.getOrDefault(errorKey, 0)
        }
    }
}
