package cn.myjdemo.autotips.service.impl

import cn.myjdemo.autotips.model.MethodCallInfo
import cn.myjdemo.autotips.model.MethodCallContext
import cn.myjdemo.autotips.service.CallDetectionService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

/**
 * 方法调用检测服务实现类
 * 负责检测和分析方法调用，确定调用的目标方法
 * 
 * 实现需求:
 * - 2.1: 识别完整的方法调用（如a.b()）
 * - 2.2: 正确解析包含参数的方法签名
 * - 2.3: 识别链式调用的每个环节
 * - 2.4: 解析继承或接口实现的实际方法定义
 */
class CallDetectionServiceImpl : CallDetectionService {
    
    companion object {
        private val LOG = Logger.getInstance(CallDetectionServiceImpl::class.java)
    }
    
    /**
     * 在指定位置检测方法调用
     * 
     * 需求 2.1: 当开发者输入完整的方法调用（如a.b()）时，识别被调用的方法
     * 需求 2.5: 当方法调用语法不完整时，不触发提示显示
     * 
     * @param editor 编辑器实例
     * @param offset 光标位置偏移量
     * @return 方法调用信息，如果没有检测到有效调用则返回null
     */
    override fun detectMethodCall(editor: Editor, offset: Int): MethodCallInfo? {
        try {
            // 1. 获取当前位置的PSI文件
            val project = editor.project ?: return null
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
            
            // 2. 获取当前位置的PSI元素
            // 使用offset-1来获取刚输入的字符之前的元素（例如输入")"后，我们要检查")"之前的内容）
            val elementAtOffset = if (offset > 0) {
                psiFile.findElementAt(offset - 1)
            } else {
                psiFile.findElementAt(offset)
            } ?: return null
            
            // 3. 查找最近的方法调用表达式
            val callExpression = findMethodCallExpression(elementAtOffset) ?: return null
            
            // 4. 验证调用上下文是否有效
            if (!isValidCallContext(elementAtOffset)) {
                return null
            }
            
            // 5. 检查语法是否完整
            if (!isCallSyntaxComplete(callExpression)) {
                return null
            }
            
            // 6. 解析方法引用并返回调用信息
            return createMethodCallInfo(callExpression)
        } catch (e: Exception) {
            LOG.warn("Failed to detect method call at offset $offset", e)
            return null
        }
    }
    
    /**
     * 解析方法调用表达式的引用
     * 
     * 需求 2.4: 当方法调用通过继承或接口实现时，解析到实际的方法定义
     * 需求 5.4: 能够解析第三方库中的方法
     * 需求 5.5: 正确处理包导入和类路径解析
     * 
     * @param callExpression PSI方法调用表达式
     * @return 解析到的方法，如果解析失败则返回null
     */
    override fun resolveMethodReference(callExpression: PsiMethodCallExpression): PsiMethod? {
        try {
            // 使用PSI的resolveMethod()方法，它会自动处理多态和继承情况
            // 这个方法会解析到实际被调用的方法定义，包括：
            // - 直接方法调用
            // - 通过接口调用的实现方法
            // - 通过父类引用调用的子类方法
            // - 第三方库中的方法（通过类路径解析）
            // - Maven/Gradle依赖中的方法
            val resolvedMethod = callExpression.resolveMethod()
            
            if (resolvedMethod == null) {
                LOG.debug("Failed to resolve method reference for: ${callExpression.text}")
            }
            
            return resolvedMethod
        } catch (e: Exception) {
            LOG.warn("Exception while resolving method reference", e)
            return null
        }
    }
    
    /**
     * 检查元素是否在有效的调用上下文中
     * 
     * 需求 2.5: 确保不在注释或字符串中触发提示
     * 
     * @param element PSI元素
     * @return 是否为有效的调用上下文
     */
    override fun isValidCallContext(element: PsiElement): Boolean {
        try {
            // 1. 检查元素是否在注释中
            if (isInComment(element)) {
                return false
            }
            
            // 2. 检查元素是否在字符串字面量中
            if (isInStringLiteral(element)) {
                return false
            }
            
            // 3. 检查是否能找到方法调用表达式
            val callExpression = PsiTreeUtil.getParentOfType(
                element, 
                PsiMethodCallExpression::class.java,
                false
            )
            
            return callExpression != null
        } catch (e: Exception) {
            LOG.warn("Exception while validating call context", e)
            return false
        }
    }
    
    /**
     * 获取方法调用的完整上下文信息
     * 
     * 需求 2.2: 正确解析方法签名
     * 需求 2.4: 解析到实际的方法定义
     * 
     * @param callExpression PSI方法调用表达式
     * @return 方法调用上下文，如果无法获取则返回null
     */
    override fun getMethodCallContext(callExpression: PsiMethodCallExpression): MethodCallContext? {
        try {
            // 解析方法引用
            val resolvedMethod = resolveMethodReference(callExpression) ?: return null
            
            // 获取包含该方法的类
            val containingClass = resolvedMethod.containingClass ?: return null
            
            return MethodCallContext(
                callExpression = callExpression,
                resolvedMethod = resolvedMethod,
                containingClass = containingClass,
                callSite = callExpression
            )
        } catch (e: Exception) {
            LOG.warn("Failed to get method call context", e)
            return null
        }
    }
    
    /**
     * 检测链式调用中的所有方法
     * 
     * 需求 2.3: 当方法调用是链式调用的一部分时，识别每个调用环节
     * 
     * @param callExpression PSI方法调用表达式
     * @return 链式调用中的所有方法列表
     */
    override fun detectChainedCalls(callExpression: PsiMethodCallExpression): List<MethodCallInfo> {
        val chainedCalls = mutableListOf<MethodCallInfo>()
        
        try {
            // 从当前调用开始，向前遍历调用链
            var currentCall: PsiMethodCallExpression? = callExpression
            
            while (currentCall != null) {
                // 为当前调用创建MethodCallInfo
                val callInfo = createMethodCallInfo(currentCall)
                if (callInfo != null) {
                    // 添加到列表开头，因为我们是从后往前遍历的
                    chainedCalls.add(0, callInfo)
                }
                
                // 获取调用链中的下一个（前一个）方法调用
                currentCall = findPreviousCallInChain(currentCall)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to detect chained calls", e)
        }
        
        return chainedCalls
    }
    
    /**
     * 检查方法调用语法是否完整
     * 
     * 需求 2.5: 当方法调用语法不完整时，不触发提示显示
     * 
     * @param element PSI元素
     * @return 语法是否完整
     */
    override fun isCallSyntaxComplete(element: PsiElement): Boolean {
        try {
            // 1. 获取方法调用表达式
            val callExpression = if (element is PsiMethodCallExpression) {
                element
            } else {
                PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression::class.java)
            } ?: return false
            
            // 2. 检查是否包含错误元素
            if (hasErrorElements(callExpression)) {
                return false
            }
            
            // 3. 检查方法调用是否有完整的参数列表
            val argumentList = callExpression.argumentList
            if (argumentList == null) {
                return false
            }
            
            // 4. 检查参数列表是否完整（有左右括号）
            val text = argumentList.text
            if (!text.startsWith("(") || !text.endsWith(")")) {
                return false
            }
            
            // 5. 检查方法引用表达式是否存在
            val methodExpression = callExpression.methodExpression
            if (methodExpression.text.isBlank()) {
                return false
            }
            
            return true
        } catch (e: Exception) {
            LOG.warn("Exception while checking call syntax completeness", e)
            return false
        }
    }
    
    /**
     * 查找PSI元素对应的方法调用表达式
     * 
     * @param element PSI元素
     * @return 方法调用表达式，如果未找到则返回null
     */
    private fun findMethodCallExpression(element: PsiElement): PsiMethodCallExpression? {
        // 向上遍历PSI树，查找方法调用表达式
        return PsiTreeUtil.getParentOfType(
            element,
            PsiMethodCallExpression::class.java,
            false
        )
    }
    
    /**
     * 创建方法调用信息对象
     * 
     * 需求 2.2: 正确解析包含参数的方法签名
     * 
     * @param callExpression PSI方法调用表达式
     * @return 方法调用信息，如果无法创建则返回null
     */
    private fun createMethodCallInfo(callExpression: PsiMethodCallExpression): MethodCallInfo? {
        try {
            // 解析方法引用
            val psiMethod = resolveMethodReference(callExpression) ?: return null
            
            // 获取方法名
            val methodName = psiMethod.name
            
            // 获取完全限定类名
            val containingClass = psiMethod.containingClass
            val qualifiedClassName = containingClass?.qualifiedName ?: ""
            
            return MethodCallInfo(
                methodName = methodName,
                qualifiedClassName = qualifiedClassName,
                psiMethod = psiMethod,
                callExpression = callExpression
            )
        } catch (e: Exception) {
            LOG.warn("Failed to create MethodCallInfo", e)
            return null
        }
    }
    
    /**
     * 在调用链中查找前一个方法调用
     * 
     * 需求 2.3: 支持链式调用的识别
     * 
     * @param callExpression 当前方法调用表达式
     * @return 链中的前一个方法调用，如果没有则返回null
     */
    private fun findPreviousCallInChain(callExpression: PsiMethodCallExpression): PsiMethodCallExpression? {
        try {
            // 获取方法表达式的限定符（qualifier）
            // 例如在 a.b().c() 中，c()的qualifier是 a.b()
            val methodExpression = callExpression.methodExpression
            val qualifier = methodExpression.qualifierExpression
            
            // 如果qualifier本身是一个方法调用，则返回它
            return if (qualifier is PsiMethodCallExpression) {
                qualifier
            } else {
                // 否则，尝试在qualifier中查找方法调用
                PsiTreeUtil.findChildOfType(qualifier, PsiMethodCallExpression::class.java)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to find previous call in chain", e)
            return null
        }
    }
    
    /**
     * 检查PSI元素是否在注释中
     * 
     * @param element PSI元素
     * @return 是否在注释中
     */
    private fun isInComment(element: PsiElement): Boolean {
        // 检查元素本身或其父元素是否是注释
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiComment) {
                return true
            }
            // 检查是否是文档注释（使用类名字符串比较避免导入问题）
            if (current.javaClass.simpleName.contains("DocComment")) {
                return true
            }
            // 只向上检查几层，避免过度遍历
            if (current is PsiMethod || current is PsiClass || current is PsiFile) {
                break
            }
            current = current.parent
        }
        return false
    }
    
    /**
     * 检查PSI元素是否在字符串字面量中
     * 
     * @param element PSI元素
     * @return 是否在字符串字面量中
     */
    private fun isInStringLiteral(element: PsiElement): Boolean {
        // 检查元素本身或其父元素是否是字符串字面量
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiLiteralExpression) {
                // 检查是否是字符串类型的字面量
                val value = current.value
                if (value is String) {
                    return true
                }
            }
            // 只向上检查几层
            if (current is PsiStatement || current is PsiMethod) {
                break
            }
            current = current.parent
        }
        return false
    }
    
    /**
     * 检查PSI元素是否包含错误元素
     * 
     * @param element PSI元素
     * @return 是否包含错误元素
     */
    private fun hasErrorElements(element: PsiElement): Boolean {
        return PsiTreeUtil.hasErrorElements(element)
    }
}