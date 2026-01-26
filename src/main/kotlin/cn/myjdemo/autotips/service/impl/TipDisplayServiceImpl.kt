package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.*
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.service.ErrorRecoveryService
import cn.myjdemo.autotips.service.RecoveryAction
import cn.myjdemo.autotips.service.TipDisplayService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Dimension
import java.awt.Point
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.ScrollPaneConstants

/**
 * 提示显示服务实现类
 * 负责管理提示的显示逻辑和UI交互
 * 
 * 实现需求:
 * - 3.1, 3.2, 3.3: 非中断式显示
 * - 3.4: 提示内容适配
 * - 3.5: 提示冲突处理
 * - 7.3: 异常处理和错误恢复
 */
class TipDisplayServiceImpl : TipDisplayService {
    
    companion object {
        private val LOG = Logger.getInstance(TipDisplayServiceImpl::class.java)
        private const val MAX_TIP_WIDTH = 400
        private const val MAX_TIP_HEIGHT = 300
        private const val MIN_TIP_WIDTH = 200
        private const val POSITION_OFFSET_Y = 30
    }
    
    private val configService = service<ConfigurationService>()
    private val errorRecoveryService = ErrorRecoveryServiceImpl()
    private var currentBalloon: Balloon? = null
    private var currentContent: TipsContent? = null
    private var currentEditor: Editor? = null
    private var currentPosition: LogicalPosition? = null
    private var autoHideTimeoutMs: Long = 5000L
    private var displayEnabled = true
    
    override fun showTip(content: TipsContent, editor: Editor, position: LogicalPosition) {
        // 需求 7.3: 检查显示功能是否被禁用
        if (!displayEnabled) {
            errorRecoveryService.logWarning("Tip display is currently disabled due to previous errors")
            return
        }
        
        try {
            // 如果当前正在显示相同内容的提示，不要隐藏它
            if (isCurrentlyShowing() && currentContent != null && currentContent!!.content == content.content) {
                LOG.debug("Same content is already showing, skipping")
                return
            }
            
            // 隐藏当前显示的提示（如果内容不同或没有显示）
            hideTip()
            
            // 从配置服务获取显示时长
            val displayDuration = configService.getTipDisplayDuration().toLong()
            
            // 创建提示组件
            val component = createTipComponent(content)
            
            // 创建非模态Balloon弹窗
            val balloon = JBPopupFactory.getInstance()
                .createBalloonBuilder(component)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setHideOnAction(false)
                .setBlockClicksThroughBalloon(false)
                .setRequestFocus(false)
                .setFadeoutTime(displayDuration)
                .createBalloon()
            
            // 添加dispose监听器，当balloon被隐藏/销毁时清除currentContent
            balloon.addListener(object : com.intellij.openapi.ui.popup.JBPopupListener {
                override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                    currentContent = null
                    currentEditor = null
                    currentPosition = null
                    currentBalloon = null
                }
            })
            
            // 计算显示位置
            val point = calculateDisplayPoint(editor, position)
            
            // 显示提示
            balloon.show(RelativePoint(editor.contentComponent, point), Balloon.Position.above)
            
            // 保存当前状态
            currentBalloon = balloon
            currentContent = content
            currentEditor = editor
            currentPosition = position
        } catch (e: Exception) {
            // 需求 7.3: 处理显示异常
            val recoveryAction = errorRecoveryService.handleDisplayError(e, content)
            
            when (recoveryAction) {
                RecoveryAction.FALLBACK -> {
                    // 尝试使用简单的文本显示作为回退
                    try {
                        showSimpleTip(content.content, editor, position)
                    } catch (fallbackException: Exception) {
                        errorRecoveryService.logError("Fallback display also failed", fallbackException)
                    }
                }
                RecoveryAction.DISABLE_FEATURE -> {
                    displayEnabled = false
                    errorRecoveryService.logError("Display feature disabled due to repeated errors", null)
                }
                else -> {
                    // SKIP or other actions - do nothing
                }
            }
        }
    }
    
    /**
     * 显示简单的文本提示（回退方案）
     * 
     * @param text 提示文本
     * @param editor 编辑器
     * @param position 位置
     */
    private fun showSimpleTip(text: String, editor: Editor, position: LogicalPosition) {
        val label = JLabel(text.take(100) + if (text.length > 100) "..." else "")
        
        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(label)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setRequestFocus(false)
            .createBalloon()
        
        // 添加dispose监听器
        balloon.addListener(object : com.intellij.openapi.ui.popup.JBPopupListener {
            override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                currentContent = null
                currentBalloon = null
            }
        })
        
        val point = calculateDisplayPoint(editor, position)
        balloon.show(RelativePoint(editor.contentComponent, point), Balloon.Position.above)
        
        currentBalloon = balloon
    }
    
    override fun hideTip() {
        currentBalloon?.hide()
        currentBalloon = null
        currentContent = null
        currentEditor = null
        currentPosition = null
    }
    
    override fun isCurrentlyShowing(): Boolean {
        return currentBalloon != null && !currentBalloon!!.isDisposed
    }
    
    override fun updateTipPosition(editor: Editor) {
        // 当编辑器滚动或窗口大小改变时更新提示位置
        if (isCurrentlyShowing() && currentEditor == editor && currentPosition != null) {
            // Balloon不支持直接更新位置，需要重新显示
            currentContent?.let { content ->
                showTip(content, editor, currentPosition!!)
            }
        }
    }
    
    override fun showTipWithConfig(content: TipsContent, editor: Editor, position: LogicalPosition, config: DisplayConfiguration) {
        // 隐藏当前显示的提示
        hideTip()
        
        // 创建提示组件
        val component = createTipComponent(content)
        
        // 根据配置创建Balloon
        val balloonBuilder = JBPopupFactory.getInstance()
            .createBalloonBuilder(component)
            .setBlockClicksThroughBalloon(false)
            .setRequestFocus(false)
        
        // 应用配置
        when (config.dismissBehavior) {
            DismissBehavior.ON_CLICK_OUTSIDE -> balloonBuilder.setHideOnClickOutside(true)
            DismissBehavior.ON_FOCUS_LOST -> balloonBuilder.setHideOnClickOutside(true)
            DismissBehavior.ON_ESCAPE -> balloonBuilder.setHideOnKeyOutside(true)
            DismissBehavior.ON_TIMEOUT -> balloonBuilder.setFadeoutTime(config.timeout.inWholeMilliseconds)
        }
        
        val balloon = balloonBuilder.createBalloon()
        
        // 添加dispose监听器，当balloon被隐藏/销毁时清除currentContent
        balloon.addListener(object : com.intellij.openapi.ui.popup.JBPopupListener {
            override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                currentContent = null
                currentEditor = null
                currentPosition = null
                currentBalloon = null
            }
        })
        
        // 计算显示位置
        val point = calculateDisplayPointWithConfig(editor, position, config.position)
        
        // 确定Balloon位置
        val balloonPosition = when (config.position) {
            PopupPosition.ABOVE_CARET -> Balloon.Position.above
            PopupPosition.BELOW_CARET -> Balloon.Position.below
            PopupPosition.RIGHT_OF_CARET -> Balloon.Position.atRight
            PopupPosition.SMART_POSITION -> Balloon.Position.above
        }
        
        // 显示提示
        balloon.show(RelativePoint(editor.contentComponent, point), balloonPosition)
        
        // 保存当前状态
        currentBalloon = balloon
        currentContent = content
        currentEditor = editor
        currentPosition = position
    }
    
    override fun canShowNewTip(content: TipsContent): Boolean {
        // 检查是否可以显示新提示
        if (!isCurrentlyShowing()) {
            return true
        }
        
        // 如果当前有提示显示，判断是否应该替换
        return shouldReplaceCurrentTip(content)
    }
    
    override fun getCurrentTipContent(): TipsContent? {
        return currentContent
    }
    
    override fun setAutoHideTimeout(timeoutMs: Long) {
        autoHideTimeoutMs = timeoutMs
    }
    
    /**
     * 创建提示显示组件
     * 支持长内容的滚动显示
     */
    private fun createTipComponent(content: TipsContent): JScrollPane {
        val textPane = when (content.format) {
            TipsFormat.HTML -> createHtmlTextPane(content.content)
            TipsFormat.MARKDOWN -> createMarkdownTextPane(content.content)
            TipsFormat.PLAIN_TEXT -> createPlainTextPane(content.content)
        }
        
        // 创建滚动面板
        val scrollPane = JScrollPane(textPane).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            border = null
        }
        
        // 设置首选大小
        val preferredSize = calculatePreferredSize(content.content)
        scrollPane.preferredSize = preferredSize
        
        return scrollPane
    }
    
    /**
     * 创建HTML格式的文本面板
     */
    private fun createHtmlTextPane(content: String): JTextPane {
        return JTextPane().apply {
            contentType = "text/html"
            text = "<html><body style='width: ${MAX_TIP_WIDTH - 40}px'>$content</body></html>"
            isEditable = false
            isOpaque = false
        }
    }
    
    /**
     * 创建Markdown格式的文本面板（简化处理，转换为HTML）
     */
    private fun createMarkdownTextPane(content: String): JTextPane {
        // 简单的Markdown到HTML转换
        val htmlContent = content
            .replace("\n", "<br>")
            .replace("**", "<b>", ignoreCase = false)
            .replace("*", "<i>", ignoreCase = false)
        
        return createHtmlTextPane(htmlContent)
    }
    
    /**
     * 创建纯文本面板
     */
    private fun createPlainTextPane(content: String): JTextPane {
        return JTextPane().apply {
            text = content
            isEditable = false
            isOpaque = false
        }
    }
    
    /**
     * 计算首选大小
     */
    private fun calculatePreferredSize(content: String): Dimension {
        val lines = content.lines()
        val maxLineLength = lines.maxOfOrNull { it.length } ?: 0
        
        // 根据内容长度计算宽度
        val width = minOf(maxOf(maxLineLength * 7, MIN_TIP_WIDTH), MAX_TIP_WIDTH)
        
        // 根据行数计算高度
        val height = minOf(lines.size * 20 + 20, MAX_TIP_HEIGHT)
        
        return Dimension(width, height)
    }
    
    /**
     * 计算显示位置
     */
    private fun calculateDisplayPoint(editor: Editor, position: LogicalPosition): Point {
        val visualPosition = editor.logicalToVisualPosition(position)
        val point = editor.visualPositionToXY(visualPosition)
        return Point(point.x, point.y - POSITION_OFFSET_Y)
    }
    
    /**
     * 根据配置计算显示位置
     */
    private fun calculateDisplayPointWithConfig(editor: Editor, position: LogicalPosition, popupPosition: PopupPosition): Point {
        val visualPosition = editor.logicalToVisualPosition(position)
        val point = editor.visualPositionToXY(visualPosition)
        
        return when (popupPosition) {
            PopupPosition.ABOVE_CARET -> Point(point.x, point.y - POSITION_OFFSET_Y)
            PopupPosition.BELOW_CARET -> Point(point.x, point.y + POSITION_OFFSET_Y)
            PopupPosition.RIGHT_OF_CARET -> Point(point.x + 50, point.y)
            PopupPosition.SMART_POSITION -> {
                // 智能定位：根据编辑器可见区域决定位置
                val visibleArea = editor.scrollingModel.visibleArea
                if (point.y - POSITION_OFFSET_Y < visibleArea.y) {
                    Point(point.x, point.y + POSITION_OFFSET_Y)
                } else {
                    Point(point.x, point.y - POSITION_OFFSET_Y)
                }
            }
        }
    }
    
    /**
     * 判断是否应该替换当前提示
     * 基于内容差异和时间戳决定
     */
    private fun shouldReplaceCurrentTip(newContent: TipsContent): Boolean {
        val current = currentContent ?: return true
        
        // 如果内容不同，允许替换
        if (current.content != newContent.content) {
            return true
        }
        
        // 如果内容相同，不替换
        return false
    }
}