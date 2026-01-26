package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.service.impl.AnnotationParserImpl
import cn.myjdemo.autotips.service.impl.CallDetectionServiceImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * 外部依赖解析属性测试
 * 
 * **Feature: auto-tips, Property 12: 外部依赖解析**
 * 
 * 对于任何使用第三方库的项目，插件应该能够解析库中方法的@tips注释
 * 
 * **验证需求: 5.4, 5.5**
 */
class ExternalDependencyParsingPropertyTest : StringSpec({
    
    lateinit var myFixture: CodeInsightTestFixture
    lateinit var annotationParser: AnnotationParser
    lateinit var callDetectionService: CallDetectionService
    
    val propertyTestIterations = 20
    
    beforeSpec {
        // 初始化测试环境
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createFixtureBuilder("test")
        myFixture = factory.createCodeInsightFixture(fixtureBuilder.fixture)
        myFixture.setUp()
        
        annotationParser = AnnotationParserImpl()
        callDetectionService = CallDetectionServiceImpl()
    }
    
    afterSpec {
        myFixture.tearDown()
    }
    
    /**
     * 属性 12: 外部依赖解析
     * 
     * 对于任何包含@tips注释的外部类方法，插件应该能够：
     * 1. 正确解析方法引用
     * 2. 提取@tips内容
     * 3. 处理不同的包结构
     */
    "Property 12.1: External dependency parsing - should resolve and extract tips from external library methods".config(
        invocations = propertyTestIterations
    ) {
        checkAll(externalMethodGenerator()) { externalMethod ->
            ApplicationManager.getApplication().runReadAction {
                // 创建外部库类
                myFixture.addFileToProject(
                    externalMethod.filePath,
                    externalMethod.classCode
                )
                
                // 创建调用该方法的文件
                val callerCode = """
                    package com.example;
                    
                    ${externalMethod.importStatement}
                    
                    public class Caller {
                        public void callExternal() {
                            ${externalMethod.callStatement}
                        }
                    }
                """.trimIndent()
                
                val callerFile = myFixture.configureByText("Caller.java", callerCode) as PsiJavaFile
                
                // 查找方法调用
                val callMethod = callerFile.classes[0].findMethodsByName("callExternal", false)[0]
                val methodCalls = PsiTreeUtil.findChildrenOfType(callMethod, PsiMethodCallExpression::class.java)
                
                if (methodCalls.isNotEmpty()) {
                    val methodCall = methodCalls.first()
                    
                    // 解析方法引用
                    val resolvedMethod = callDetectionService.resolveMethodReference(methodCall)
                    
                    // 验证能够解析方法
                    resolvedMethod.shouldNotBeNull()
                    resolvedMethod.name shouldBe externalMethod.methodName
                    
                    // 验证能够提取@tips内容
                    val tipsContent = annotationParser.extractTipsContent(resolvedMethod)
                    tipsContent.shouldNotBeNull()
                    tipsContent.content shouldContain externalMethod.tipsContent
                }
            }
        }
    }
    
    /**
     * 属性 12: 外部依赖解析 - 继承场景
     * 
     * 对于任何继承自外部类的方法调用，插件应该能够解析到基类方法并提取@tips
     */
    "Property 12.2: External dependency parsing - should resolve inherited methods from external classes".config(
        invocations = propertyTestIterations
    ) {
        checkAll(inheritanceScenarioGenerator()) { scenario ->
            ApplicationManager.getApplication().runReadAction {
                // 创建外部基类
                myFixture.addFileToProject(
                    scenario.baseClassPath,
                    scenario.baseClassCode
                )
                
                // 创建子类
                myFixture.addFileToProject(
                    scenario.derivedClassPath,
                    scenario.derivedClassCode
                )
                
                // 创建调用代码
                val callerCode = """
                    package com.example;
                    
                    ${scenario.importStatement}
                    
                    public class Caller {
                        public void callMethod() {
                            ${scenario.callStatement}
                        }
                    }
                """.trimIndent()
                
                val callerFile = myFixture.configureByText("Caller.java", callerCode) as PsiJavaFile
                
                // 查找方法调用
                val callMethod = callerFile.classes[0].findMethodsByName("callMethod", false)[0]
                val methodCalls = PsiTreeUtil.findChildrenOfType(callMethod, PsiMethodCallExpression::class.java)
                
                if (methodCalls.isNotEmpty()) {
                    val methodCall = methodCalls.first()
                    
                    // 解析方法引用（应该解析到基类方法）
                    val resolvedMethod = callDetectionService.resolveMethodReference(methodCall)
                    
                    // 验证解析到基类方法
                    resolvedMethod.shouldNotBeNull()
                    resolvedMethod.name shouldBe scenario.methodName
                    
                    // 验证能够提取基类的@tips内容
                    val tipsContent = annotationParser.extractTipsContent(resolvedMethod)
                    tipsContent.shouldNotBeNull()
                    tipsContent.content shouldContain scenario.tipsContent
                }
            }
        }
    }
    
    /**
     * 属性 12: 外部依赖解析 - 复杂包结构
     * 
     * 对于任何深层包结构中的方法，插件应该能够正确解析和提取@tips
     */
    "Property 12.3: External dependency parsing - should handle complex package structures".config(
        invocations = propertyTestIterations
    ) {
        checkAll(complexPackageGenerator()) { packageInfo ->
            ApplicationManager.getApplication().runReadAction {
                // 创建深层包结构中的类
                myFixture.addFileToProject(
                    packageInfo.filePath,
                    packageInfo.classCode
                )
                
                // 创建调用代码
                val callerCode = """
                    package com.example;
                    
                    ${packageInfo.importStatement}
                    
                    public class Caller {
                        public void callMethod() {
                            ${packageInfo.callStatement}
                        }
                    }
                """.trimIndent()
                
                val callerFile = myFixture.configureByText("Caller.java", callerCode) as PsiJavaFile
                
                // 查找方法调用
                val callMethod = callerFile.classes[0].findMethodsByName("callMethod", false)[0]
                val methodCalls = PsiTreeUtil.findChildrenOfType(callMethod, PsiMethodCallExpression::class.java)
                
                if (methodCalls.isNotEmpty()) {
                    val methodCall = methodCalls.first()
                    
                    // 解析方法引用
                    val resolvedMethod = callDetectionService.resolveMethodReference(methodCall)
                    
                    // 验证能够解析深层包中的方法
                    resolvedMethod.shouldNotBeNull()
                    resolvedMethod.name shouldBe packageInfo.methodName
                    resolvedMethod.containingClass?.qualifiedName shouldBe packageInfo.qualifiedClassName
                    
                    // 验证能够提取@tips内容
                    val tipsContent = annotationParser.extractTipsContent(resolvedMethod)
                    tipsContent.shouldNotBeNull()
                    tipsContent.content shouldContain packageInfo.tipsContent
                }
            }
        }
    }
}) {
    companion object {
        // ========== 测试数据生成器 ==========
        
        /**
         * 外部方法信息数据类
         */
        data class ExternalMethodInfo(
            val packageName: String,
            val className: String,
            val methodName: String,
            val tipsContent: String,
            val isStatic: Boolean
        ) {
            val qualifiedClassName: String
                get() = "$packageName.$className"
            
            val filePath: String
                get() = "${packageName.replace('.', '/')}/$className.java"
            
            val classCode: String
                get() = """
                    package $packageName;
                    
                    public class $className {
                        /**
                         * @tips $tipsContent
                         */
                        public ${if (isStatic) "static " else ""}void $methodName() {
                        }
                    }
                """.trimIndent()
            
            val importStatement: String
                get() = "import $qualifiedClassName;"
            
            val callStatement: String
                get() = if (isStatic) {
                    "$className.$methodName();"
                } else {
                    "$className obj = new $className(); obj.$methodName();"
                }
        }
        
        /**
         * 继承场景信息数据类
         */
        data class InheritanceScenario(
            val basePackage: String,
            val baseClassName: String,
            val derivedPackage: String,
            val derivedClassName: String,
            val methodName: String,
            val tipsContent: String
        ) {
            val baseClassPath: String
                get() = "${basePackage.replace('.', '/')}/$baseClassName.java"
            
            val derivedClassPath: String
                get() = "${derivedPackage.replace('.', '/')}/$derivedClassName.java"
            
            val baseClassCode: String
                get() = """
                    package $basePackage;
                    
                    public class $baseClassName {
                        /**
                         * @tips $tipsContent
                         */
                        public void $methodName() {
                        }
                    }
                """.trimIndent()
            
            val derivedClassCode: String
                get() = """
                    package $derivedPackage;
                    
                    import $basePackage.$baseClassName;
                    
                    public class $derivedClassName extends $baseClassName {
                        // Inherits $methodName()
                    }
                """.trimIndent()
            
            val importStatement: String
                get() = "import $derivedPackage.$derivedClassName;"
            
            val callStatement: String
                get() = "$derivedClassName obj = new $derivedClassName(); obj.$methodName();"
        }
        
        /**
         * 复杂包结构信息数据类
         */
        data class ComplexPackageInfo(
            val packageParts: List<String>,
            val className: String,
            val methodName: String,
            val tipsContent: String
        ) {
            val packageName: String
                get() = packageParts.joinToString(".")
            
            val qualifiedClassName: String
                get() = "$packageName.$className"
            
            val filePath: String
                get() = "${packageName.replace('.', '/')}/$className.java"
            
            val classCode: String
                get() = """
                    package $packageName;
                    
                    public class $className {
                        /**
                         * @tips $tipsContent
                         */
                        public static void $methodName() {
                        }
                    }
                """.trimIndent()
            
            val importStatement: String
                get() = "import $qualifiedClassName;"
            
            val callStatement: String
                get() = "$className.$methodName();"
        }
        
        /**
         * 生成外部方法测试数据
         */
        fun externalMethodGenerator(): Arb<ExternalMethodInfo> = arbitrary {
            val packageNames = listOf("com.external.lib", "org.third.party", "io.library.api")
            val classNames = listOf("ExternalClass", "LibraryUtil", "ApiHelper", "ServiceProvider")
            val methodNames = listOf("process", "execute", "handle", "perform", "calculate")
            val tipsContents = listOf(
                "This is an external library method",
                "Use with caution",
                "Important operation",
                "Handle errors properly"
            )
            
            ExternalMethodInfo(
                packageName = packageNames.random(),
                className = classNames.random(),
                methodName = methodNames.random(),
                tipsContent = tipsContents.random(),
                isStatic = listOf(true, false).random()
            )
        }
        
        /**
         * 生成继承场景测试数据
         */
        fun inheritanceScenarioGenerator(): Arb<InheritanceScenario> = arbitrary {
            val basePackages = listOf("com.external.base", "org.library.core")
            val baseClassNames = listOf("BaseClass", "AbstractService", "CoreComponent")
            val derivedPackages = listOf("com.example.impl", "com.app.service")
            val derivedClassNames = listOf("DerivedClass", "ConcreteService", "CustomComponent")
            val methodNames = listOf("baseMethod", "coreOperation", "inheritedFunction")
            val tipsContents = listOf(
                "Inherited from external base class",
                "Core functionality from library",
                "Base implementation"
            )
            
            InheritanceScenario(
                basePackage = basePackages.random(),
                baseClassName = baseClassNames.random(),
                derivedPackage = derivedPackages.random(),
                derivedClassName = derivedClassNames.random(),
                methodName = methodNames.random(),
                tipsContent = tipsContents.random()
            )
        }
        
        /**
         * 生成复杂包结构测试数据
         */
        fun complexPackageGenerator(): Arb<ComplexPackageInfo> = arbitrary {
            val packagePrefixes = listOf("com", "org", "io")
            val packageMiddles = listOf("company", "project", "library")
            val packageSuffixes = listOf("module", "component", "util", "service")
            val classNames = listOf("DeepClass", "NestedUtil", "ComplexService")
            val methodNames = listOf("deepMethod", "nestedOperation", "complexFunction")
            val tipsContents = listOf(
                "Method in deep package structure",
                "Complex package hierarchy",
                "Nested module function"
            )
            
            // 生成2-5层的包结构
            val depth = (2..5).random()
            val packageParts = mutableListOf<String>()
            packageParts.add(packagePrefixes.random())
            packageParts.add(packageMiddles.random())
            for (i in 2 until depth) {
                packageParts.add(packageSuffixes.random() + i)
            }
            
            ComplexPackageInfo(
                packageParts = packageParts,
                className = classNames.random(),
                methodName = methodNames.random(),
                tipsContent = tipsContents.random()
            )
        }
    }
}
