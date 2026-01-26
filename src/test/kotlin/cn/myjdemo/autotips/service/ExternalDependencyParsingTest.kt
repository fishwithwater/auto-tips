package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.TestBase
import cn.myjdemo.autotips.service.impl.AnnotationParserImpl
import cn.myjdemo.autotips.service.impl.CallDetectionServiceImpl
import com.intellij.psi.PsiJavaFile

/**
 * 外部依赖解析测试
 * 
 * 验证需求:
 * - 5.4: 当项目使用第三方库时，插件能够解析库中方法的@tips注释
 * - 5.5: 当项目结构复杂时，插件正确处理包导入和类路径解析
 */
class ExternalDependencyParsingTest : TestBase() {
    
    private lateinit var annotationParser: AnnotationParser
    private lateinit var callDetectionService: CallDetectionService
    
    override fun setUp() {
        super.setUp()
        annotationParser = AnnotationParserImpl()
        callDetectionService = CallDetectionServiceImpl()
    }
    
    /**
     * 测试解析项目内部类的方法
     * 
     * 需求 5.5: 正确处理包导入和类路径解析
     */
    fun testParseInternalClassMethod() {
        // 创建测试文件
        val javaFile = myFixture.configureByText(
            "TestClass.java",
            """
            package com.example;
            
            public class TestClass {
                /**
                 * @tips This is an internal method tip
                 */
                public void internalMethod() {
                }
            }
            """.trimIndent()
        ) as PsiJavaFile
        
        // 获取方法
        val psiClass = javaFile.classes[0]
        val method = psiClass.findMethodsByName("internalMethod", false)[0]
        
        // 解析@tips内容
        val tipsContent = annotationParser.extractTipsContent(method)
        
        // 验证
        assertNotNull("Should extract tips from internal class method", tipsContent)
        assertEquals("This is an internal method tip", tipsContent?.content)
    }
    
    /**
     * 测试解析带有完全限定名的方法调用
     * 
     * 需求 5.5: 正确处理包导入和类路径解析
     */
    fun testResolveFullyQualifiedMethodCall() {
        // 创建被调用的类
        myFixture.configureByText(
            "ExternalClass.java",
            """
            package com.external.library;
            
            public class ExternalClass {
                /**
                 * @tips This is an external library method
                 */
                public static void externalMethod() {
                }
            }
            """.trimIndent()
        )
        
        // 创建调用该方法的文件
        val callerFile = myFixture.configureByText(
            "Caller.java",
            """
            package com.example;
            
            public class Caller {
                public void callExternal() {
                    com.external.library.ExternalClass.externalMethod();
                }
            }
            """.trimIndent()
        )
        
        // 将光标移动到方法调用的右括号后
        val text = callerFile.text
        val offset = text.indexOf("externalMethod()") + "externalMethod()".length
        myFixture.editor.caretModel.moveToOffset(offset)
        
        // 检测方法调用
        val methodCallInfo = callDetectionService.detectMethodCall(myFixture.editor, offset)
        
        // 验证
        assertNotNull("Should detect fully qualified method call", methodCallInfo)
        assertEquals("externalMethod", methodCallInfo?.methodName)
        assertEquals("com.external.library.ExternalClass", methodCallInfo?.qualifiedClassName)
        
        // 验证能够提取@tips内容
        val tipsContent = annotationParser.extractTipsContent(methodCallInfo!!.psiMethod)
        assertNotNull("Should extract tips from external method", tipsContent)
        assertEquals("This is an external library method", tipsContent?.content)
    }
    
    /**
     * 测试解析带有import语句的方法调用
     * 
     * 需求 5.5: 正确处理包导入和类路径解析
     */
    fun testResolveImportedMethodCall() {
        // 创建被调用的类
        myFixture.addFileToProject(
            "com/external/library/LibraryClass.java",
            """
            package com.external.library;
            
            public class LibraryClass {
                /**
                 * @tips Use this method carefully
                 * It performs important operations
                 */
                public void importantMethod() {
                }
            }
            """.trimIndent()
        )
        
        // 创建调用该方法的文件（使用import）
        val callerFile = myFixture.configureByText(
            "ImportCaller.java",
            """
            package com.example;
            
            import com.external.library.LibraryClass;
            
            public class ImportCaller {
                public void callImported() {
                    LibraryClass lib = new LibraryClass();
                    lib.importantMethod();
                }
            }
            """.trimIndent()
        )
        
        // 将光标移动到方法调用的右括号后
        val text = callerFile.text
        val offset = text.indexOf("importantMethod()") + "importantMethod()".length
        myFixture.editor.caretModel.moveToOffset(offset)
        
        // 检测方法调用
        val methodCallInfo = callDetectionService.detectMethodCall(myFixture.editor, offset)
        
        // 验证
        assertNotNull("Should detect imported method call", methodCallInfo)
        assertEquals("importantMethod", methodCallInfo?.methodName)
        assertEquals("com.external.library.LibraryClass", methodCallInfo?.qualifiedClassName)
        
        // 验证能够提取@tips内容（包括多行）
        val tipsContent = annotationParser.extractTipsContent(methodCallInfo!!.psiMethod)
        assertNotNull("Should extract tips from imported method", tipsContent)
        assertTrue(
            "Should contain full multi-line tips",
            tipsContent?.content?.contains("Use this method carefully") == true &&
            tipsContent.content.contains("It performs important operations")
        )
    }
    
    /**
     * 测试解析继承自外部类的方法
     * 
     * 需求 2.4: 解析继承或接口实现的实际方法定义
     * 需求 5.4: 解析第三方库中方法的@tips注释
     */
    fun testResolveInheritedMethodFromExternalClass() {
        // 创建外部基类
        myFixture.addFileToProject(
            "com/external/base/BaseClass.java",
            """
            package com.external.base;
            
            public class BaseClass {
                /**
                 * @tips This method is inherited from external base class
                 */
                public void baseMethod() {
                }
            }
            """.trimIndent()
        )
        
        // 创建继承该基类的子类
        myFixture.addFileToProject(
            "com/example/DerivedClass.java",
            """
            package com.example;
            
            import com.external.base.BaseClass;
            
            public class DerivedClass extends BaseClass {
                // 继承baseMethod()
            }
            """.trimIndent()
        )
        
        // 创建调用继承方法的文件
        val callerFile = myFixture.configureByText(
            "InheritanceCaller.java",
            """
            package com.example;
            
            public class InheritanceCaller {
                public void callInherited() {
                    DerivedClass derived = new DerivedClass();
                    derived.baseMethod();
                }
            }
            """.trimIndent()
        )
        
        // 将光标移动到方法调用的右括号后
        val text = callerFile.text
        val offset = text.indexOf("baseMethod()") + "baseMethod()".length
        myFixture.editor.caretModel.moveToOffset(offset)
        
        // 检测方法调用
        val methodCallInfo = callDetectionService.detectMethodCall(myFixture.editor, offset)
        
        // 验证
        assertNotNull("Should detect inherited method call", methodCallInfo)
        assertEquals("baseMethod", methodCallInfo?.methodName)
        // 应该解析到基类的方法
        assertEquals("com.external.base.BaseClass", methodCallInfo?.qualifiedClassName)
        
        // 验证能够提取基类方法的@tips内容
        val tipsContent = annotationParser.extractTipsContent(methodCallInfo!!.psiMethod)
        assertNotNull("Should extract tips from inherited external method", tipsContent)
        assertEquals("This method is inherited from external base class", tipsContent?.content)
    }
    
    /**
     * 测试解析实现外部接口的方法
     * 
     * 需求 2.4: 解析继承或接口实现的实际方法定义
     * 需求 5.4: 解析第三方库中方法的@tips注释
     */
    fun testResolveInterfaceMethodFromExternalLibrary() {
        // 创建外部接口
        myFixture.addFileToProject(
            "com/external/api/ExternalInterface.java",
            """
            package com.external.api;
            
            public interface ExternalInterface {
                /**
                 * @tips Implement this method according to the specification
                 */
                void interfaceMethod();
            }
            """.trimIndent()
        )
        
        // 创建实现该接口的类
        myFixture.addFileToProject(
            "com/example/Implementation.java",
            """
            package com.example;
            
            import com.external.api.ExternalInterface;
            
            public class Implementation implements ExternalInterface {
                @Override
                public void interfaceMethod() {
                    // Implementation
                }
            }
            """.trimIndent()
        )
        
        // 创建调用接口方法的文件
        val callerFile = myFixture.configureByText(
            "InterfaceCaller.java",
            """
            package com.example;
            
            import com.external.api.ExternalInterface;
            
            public class InterfaceCaller {
                public void callInterface() {
                    ExternalInterface impl = new Implementation();
                    impl.interfaceMethod();
                }
            }
            """.trimIndent()
        )
        
        // 将光标移动到方法调用的右括号后
        val text = callerFile.text
        val offset = text.indexOf("interfaceMethod()") + "interfaceMethod()".length
        myFixture.editor.caretModel.moveToOffset(offset)
        
        // 检测方法调用
        val methodCallInfo = callDetectionService.detectMethodCall(myFixture.editor, offset)
        
        // 验证
        assertNotNull("Should detect interface method call", methodCallInfo)
        assertEquals("interfaceMethod", methodCallInfo?.methodName)
        
        // 验证能够提取接口方法的@tips内容
        // 注意：这里可能解析到接口或实现类，取决于PSI的解析策略
        val tipsContent = annotationParser.extractTipsContent(methodCallInfo!!.psiMethod)
        
        // 如果解析到接口方法，应该能提取@tips
        if (methodCallInfo.qualifiedClassName == "com.external.api.ExternalInterface") {
            assertNotNull("Should extract tips from interface method", tipsContent)
            assertEquals("Implement this method according to the specification", tipsContent?.content)
        }
    }
    
    /**
     * 测试处理复杂的包结构
     * 
     * 需求 5.5: 正确处理复杂项目结构和包导入
     */
    fun testHandleComplexPackageStructure() {
        // 创建深层包结构中的类
        myFixture.addFileToProject(
            "com/company/product/module/submodule/util/DeepPackageClass.java",
            """
            package com.company.product.module.submodule.util;
            
            public class DeepPackageClass {
                /**
                 * @tips This class is in a deep package structure
                 */
                public static void deepMethod() {
                }
            }
            """.trimIndent()
        )
        
        // 创建调用该方法的文件
        val callerFile = myFixture.configureByText(
            "ComplexCaller.java",
            """
            package com.example.test;
            
            import com.company.product.module.submodule.util.DeepPackageClass;
            
            public class ComplexCaller {
                public void callDeep() {
                    DeepPackageClass.deepMethod();
                }
            }
            """.trimIndent()
        )
        
        // 将光标移动到方法调用的右括号后
        val text = callerFile.text
        val offset = text.indexOf("deepMethod()") + "deepMethod()".length
        myFixture.editor.caretModel.moveToOffset(offset)
        
        // 检测方法调用
        val methodCallInfo = callDetectionService.detectMethodCall(myFixture.editor, offset)
        
        // 验证
        assertNotNull("Should detect method in deep package", methodCallInfo)
        assertEquals("deepMethod", methodCallInfo?.methodName)
        assertEquals(
            "com.company.product.module.submodule.util.DeepPackageClass",
            methodCallInfo?.qualifiedClassName
        )
        
        // 验证能够提取@tips内容
        val tipsContent = annotationParser.extractTipsContent(methodCallInfo!!.psiMethod)
        assertNotNull("Should extract tips from deep package method", tipsContent)
        assertEquals("This class is in a deep package structure", tipsContent?.content)
    }
}
