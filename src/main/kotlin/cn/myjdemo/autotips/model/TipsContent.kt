package cn.myjdemo.autotips.model

/**
 * 表示从@tips注释中提取的提示内容
 */
data class TipsContent(
    val content: String,
    val format: TipsFormat = TipsFormat.PLAIN_TEXT
)

/**
 * 提示内容的格式类型
 */
enum class TipsFormat {
    PLAIN_TEXT,
    HTML,
    MARKDOWN
}

/**
 * 表示单个@tips注释标记
 */
data class TipsAnnotation(
    val marker: String,
    val content: String,
    val lineNumber: Int
)

/**
 * 方法调用的上下文信息
 */
data class MethodCallInfo(
    val methodName: String,
    val qualifiedClassName: String,
    val psiMethod: com.intellij.psi.PsiMethod,
    val callExpression: com.intellij.psi.PsiMethodCallExpression
)

/**
 * 方法调用的完整上下文
 */
data class MethodCallContext(
    val callExpression: com.intellij.psi.PsiMethodCallExpression,
    val resolvedMethod: com.intellij.psi.PsiMethod,
    val containingClass: com.intellij.psi.PsiClass,
    val callSite: com.intellij.psi.PsiElement
)

/**
 * 文档内容模型
 */
data class DocumentationContent(
    val rawText: String,
    val tipsAnnotations: List<TipsAnnotation>,
    val formattedContent: String
)

/**
 * 显示配置
 */
data class DisplayConfiguration(
    val position: PopupPosition,
    val timeout: kotlin.time.Duration,
    val style: PopupStyle,
    val dismissBehavior: DismissBehavior
)

/**
 * 弹窗位置枚举
 */
enum class PopupPosition {
    ABOVE_CARET,
    BELOW_CARET,
    RIGHT_OF_CARET,
    SMART_POSITION
}

/**
 * 弹窗样式枚举
 */
enum class PopupStyle {
    BALLOON,
    TOOLTIP,
    NOTIFICATION
}

/**
 * 弹窗消失行为枚举
 */
enum class DismissBehavior {
    ON_FOCUS_LOST,
    ON_TIMEOUT,
    ON_ESCAPE,
    ON_CLICK_OUTSIDE
}

/**
 * 插件配置数据类
 */
data class TipsConfiguration(
    val enabled: Boolean = true,
    val displayDuration: Int = 5000,
    val style: TipStyle = TipStyle.BALLOON,
    val customPatterns: List<String> = emptyList()
)

/**
 * 提示样式枚举
 */
enum class TipStyle {
    BALLOON,
    TOOLTIP,
    NOTIFICATION
}