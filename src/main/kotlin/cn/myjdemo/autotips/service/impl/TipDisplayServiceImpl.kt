package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.*
import cn.myjdemo.autotips.service.ConfigurationService
import cn.myjdemo.autotips.service.ErrorRecoveryService
import cn.myjdemo.autotips.service.RecoveryAction
import cn.myjdemo.autotips.service.TipDisplayService
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.messages.MessageBusConnection
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
class TipDisplayServiceImpl : TipDisplayService, Disposable {
    
    companion object {
        private val LOG = Logger.getInstance(TipDisplayServiceImpl::class.java)
        private const val MAX_TIP_WIDTH = 400
        private const val MAX_TIP_HEIGHT = 300
        private const val MIN_TIP_WIDTH = 200
        private const val POSITION_OFFSET_Y = 30
    }
    
    private val configService = com.intellij.openapi.application.ApplicationManager.getApplication().service<ConfigurationService>()
    private val errorRecoveryService = ErrorRecoveryServiceImpl()
    @Volatile private var currentBalloon: Balloon? = null
    @Volatile private var currentContent: TipsContent? = null
    @Volatile private var currentEditor: Editor? = null
    @Volatile private var currentPosition: LogicalPosition? = null
    @Volatile private var autoHideTimeoutMs: Long = 5000L
    @Volatile private var displayEnabled = true
    private var messageBusConnection: MessageBusConnection? = null
    private var project: Project? = null
    @Volatile private var notificationTimer: java.util.Timer? = null
    
    /**
     * 初始化服务（延迟初始化）
     */
    private fun initializeIfNeeded(project: Project) {
        if (this.project == null) {
            this.project = project
            setupConfigurationListener()
            updateFromConfiguration()
        }
    }
    
    /**
     * 设置配置更改监听器
     */
    private fun setupConfigurationListener() {
        try {
            // 使用应用级MessageBus，因为ConfigurationService是应用级服务
            val applicationMessageBus = com.intellij.openapi.application.ApplicationManager.getApplication().messageBus
            messageBusConnection = applicationMessageBus.connect()
            messageBusConnection?.subscribe(
                ConfigurationServiceImpl.CONFIGURATION_CHANGED_TOPIC,
                object : ConfigurationChangeListener {
                    override fun onConfigurationChanged(newConfiguration: TipsConfiguration) {
                        LOG.info("TipDisplayService received configuration change: enabled=${newConfiguration.enabled}, style=${newConfiguration.style}, javadocMode=${newConfiguration.javadocModeEnabled}")
                        updateFromConfiguration()
                        LOG.debug("Configuration updated: enabled=${newConfiguration.enabled}, duration=${newConfiguration.displayDuration}")
                    }
                }
            )
        } catch (e: Exception) {
            LOG.warn("Failed to setup configuration listener", e)
        }
    }
    
    /**
     * 从配置更新内部状态
     */
    private fun updateFromConfiguration() {
        val config = configService.getCurrentConfiguration()
        displayEnabled = config.enabled
        autoHideTimeoutMs = config.displayDuration.toLong()
        
        LOG.info("TipDisplayService configuration updated: enabled=$displayEnabled, duration=$autoHideTimeoutMs, style=${config.style}")
        
        // 如果插件被禁用，隐藏当前显示的提示
        if (!displayEnabled && isCurrentlyShowing()) {
            LOG.info("Plugin disabled, hiding current tip")
            hideTip()
        }
    }
    
    override fun showTip(content: TipsContent, editor: Editor, position: LogicalPosition) {
        // 初始化服务（如果需要）
        val editorProject = editor.project
        if (editorProject != null) {
            initializeIfNeeded(editorProject)
        }
        
        // 需求 7.3: 检查显示功能是否被禁用
        if (!displayEnabled) {
            LOG.debug("Tip display is disabled in configuration")
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
            
            // 从配置服务获取显示设置
            val config = configService.getCurrentConfiguration()
            val displayDuration = config.displayDuration.toLong()
            val tipStyle = config.style
            
            // 根据配置的样式显示提示
            LOG.info("Showing tip with style: $tipStyle, content: ${content.content.take(50)}...")
            when (tipStyle) {
                TipStyle.BALLOON -> showBalloonTip(content, editor, position, displayDuration)
                TipStyle.TOOLTIP -> showTooltipTip(content, editor, position, displayDuration)
                TipStyle.NOTIFICATION -> showNotificationTip(content, editor, position, displayDuration)
            }
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
        notificationTimer?.cancel()
        notificationTimer = null
        currentBalloon?.hide()
        currentBalloon = null
        currentContent = null
        currentEditor = null
        currentPosition = null
    }
    
    override fun isCurrentlyShowing(): Boolean {
        // 对于balloon样式，检查balloon是否存在且未销毁
        if (currentBalloon != null) {
            return !currentBalloon!!.isDisposed
        }
        
        // 对于notification样式，检查是否有当前内容
        return currentContent != null
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
        // 初始化服务（如果需要）
        val editorProject = editor.project
        if (editorProject != null) {
            initializeIfNeeded(editorProject)
        }
        
        // 检查显示功能是否被禁用
        if (!displayEnabled) {
            LOG.debug("Tip display is disabled in configuration")
            return
        }
        
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
     * 清理资源（由框架自动调用）
     */
    override fun dispose() {
        messageBusConnection?.disconnect()
        messageBusConnection = null
        hideTip()
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
        var html = content
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        html = Regex("""\*\*(.+?)\*\*""").replace(html) { "<b>${it.groupValues[1]}</b>" }
        html = Regex("""\*(.+?)\*""").replace(html) { "<i>${it.groupValues[1]}</i>" }
        html = html.replace("\n", "<br>")
        return createHtmlTextPane(html)
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
     * 计算tooltip的最佳显示位置
     */
    private fun calculateTooltipPosition(editor: Editor, position: LogicalPosition): Point {
        val visualPosition = editor.logicalToVisualPosition(position)
        val point = editor.visualPositionToXY(visualPosition)
        
        // 获取编辑器可见区域
        val visibleArea = editor.scrollingModel.visibleArea
        val lineHeight = editor.lineHeight
        
        // 检查上方是否有足够空间
        val spaceAbove = point.y - visibleArea.y
        val spaceBelow = visibleArea.y + visibleArea.height - point.y
        
        return if (spaceAbove >= lineHeight * 3) {
            // 上方有足够空间，显示在上方
            Point(point.x, point.y - POSITION_OFFSET_Y)
        } else if (spaceBelow >= lineHeight * 3) {
            // 下方有足够空间，显示在下方
            Point(point.x, point.y + lineHeight)
        } else {
            // 空间都不够，显示在右侧
            Point(point.x + 100, point.y - lineHeight)
        }
    }
    
    /**
     * 确定tooltip的最佳显示方向
     */
    private fun determineBestTooltipPosition(editor: Editor, position: LogicalPosition): Balloon.Position {
        val visualPosition = editor.logicalToVisualPosition(position)
        val point = editor.visualPositionToXY(visualPosition)
        
        // 获取编辑器可见区域
        val visibleArea = editor.scrollingModel.visibleArea
        val lineHeight = editor.lineHeight
        
        // 检查上方是否有足够空间
        val spaceAbove = point.y - visibleArea.y
        val spaceBelow = visibleArea.y + visibleArea.height - point.y
        
        return when {
            spaceAbove >= lineHeight * 3 -> Balloon.Position.above
            spaceBelow >= lineHeight * 3 -> Balloon.Position.below
            else -> Balloon.Position.atRight
        }
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
    
    /**
     * 显示Balloon样式的提示
     */
    private fun showBalloonTip(content: TipsContent, editor: Editor, position: LogicalPosition, displayDuration: Long) {
        val component = createTipComponent(content)
        
        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(component)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setHideOnAction(false)
            .setBlockClicksThroughBalloon(false)
            .setRequestFocus(false)
            .setFadeoutTime(displayDuration)
            .createBalloon()
        
        balloon.addListener(createBalloonListener())
        
        val point = calculateDisplayPoint(editor, position)
        balloon.show(RelativePoint(editor.contentComponent, point), Balloon.Position.above)
        
        saveCurrentState(balloon, content, editor, position)
    }
    
    /**
     * 显示Tooltip样式的提示
     */
    private fun showTooltipTip(content: TipsContent, editor: Editor, position: LogicalPosition, displayDuration: Long) {
        val component = createTipComponent(content)
        
        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(component)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setHideOnAction(false)
            .setBlockClicksThroughBalloon(false)
            .setRequestFocus(false)
            .setFadeoutTime(displayDuration)
            .setBorderColor(java.awt.Color.GRAY)
            .setFillColor(java.awt.Color(255, 255, 225)) // 浅黄色背景，类似tooltip
            .createBalloon()
        
        balloon.addListener(createBalloonListener())
        
        // 使用智能定位避免遮挡代码
        val point = calculateTooltipPosition(editor, position)
        val balloonPosition = determineBestTooltipPosition(editor, position)
        balloon.show(RelativePoint(editor.contentComponent, point), balloonPosition)
        
        saveCurrentState(balloon, content, editor, position)
    }
    
    /**
     * 显示Notification样式的提示
     */
    private fun showNotificationTip(content: TipsContent, editor: Editor, position: LogicalPosition, displayDuration: Long) {
        try {
            // 使用IntelliJ的通知系统
            val notification = com.intellij.notification.Notification(
                "Auto-Tips",
                "Method Tip",
                content.content,
                com.intellij.notification.NotificationType.INFORMATION
            )
            
            // 显示通知
            com.intellij.notification.Notifications.Bus.notify(notification, editor.project)

            // 保存状态（对于通知，我们不保存balloon引用）
            currentContent = content
            currentEditor = editor
            currentPosition = position
            currentBalloon = null

            // 设置自动过期定时器，过期后清除状态，否则 isCurrentlyShowing() 永远为 true
            notificationTimer?.cancel()
            notificationTimer = java.util.Timer().also { timer ->
                timer.schedule(object : java.util.TimerTask() {
                    override fun run() {
                        notification.expire()
                        currentContent = null
                        currentEditor = null
                        currentPosition = null
                        notificationTimer = null
                    }
                }, displayDuration)
            }
            
            LOG.debug("Notification tip displayed: ${content.content.take(50)}...")
        } catch (e: Exception) {
            LOG.warn("Failed to show notification tip, falling back to balloon", e)
            // 回退到balloon样式
            showBalloonTip(content, editor, position, displayDuration)
        }
    }
    
    /**
     * 创建Balloon监听器
     */
    private fun createBalloonListener(): com.intellij.openapi.ui.popup.JBPopupListener {
        return object : com.intellij.openapi.ui.popup.JBPopupListener {
            override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                currentContent = null
                currentEditor = null
                currentPosition = null
                currentBalloon = null
            }
        }
    }
    
    /**
     * 保存当前状态
     */
    private fun saveCurrentState(balloon: Balloon, content: TipsContent, editor: Editor, position: LogicalPosition) {
        currentBalloon = balloon
        currentContent = content
        currentEditor = editor
        currentPosition = position
    }
}