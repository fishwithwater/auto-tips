package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.service.impl.JavaLanguageParser
import cn.myjdemo.autotips.service.impl.KotlinLanguageParser
import com.intellij.psi.PsiMethod
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * **Feature: auto-tips, Property 11: 多语言文件处理**
 * 
 * 对于任何混合语言项目，插件应该根据文件类型选择正确的解析器
 * 
 * **Validates: Requirement 5.3**
 * 
 * 此属性测试验证：
 * - 5.3: 当项目包含混合语言文件时，插件根据文件类型选择合适的解析器
 */
class MultiLanguageFileHandlingPropertyTest : StringSpec({
    
    "Property 11: Java parser should only support Java PsiMethod elements" {
        checkAll(100, Arb.boolean()) { isJavaMethod ->
            val javaParser = JavaLanguageParser()
            
            if (isJavaMethod) {
                // 创建Java方法mock
                val javaMethod = mock<PsiMethod>()
                javaParser.supports(javaMethod) shouldBe true
                javaParser.getLanguageName() shouldBe "Java"
            } else {
                // 创建Kotlin函数mock
                val kotlinFunction = mock<KtNamedFunction>()
                javaParser.supports(kotlinFunction) shouldBe false
            }
        }
    }
    
    "Property 11: Kotlin parser should only support Kotlin KtNamedFunction elements" {
        checkAll(100, Arb.boolean()) { isKotlinFunction ->
            val kotlinParser = KotlinLanguageParser()
            
            if (isKotlinFunction) {
                // 创建Kotlin函数mock
                val kotlinFunction = mock<KtNamedFunction>()
                kotlinParser.supports(kotlinFunction) shouldBe true
                kotlinParser.getLanguageName() shouldBe "Kotlin"
            } else {
                // 创建Java方法mock
                val javaMethod = mock<PsiMethod>()
                kotlinParser.supports(javaMethod) shouldBe false
            }
        }
    }
    
    "Property 11: Language parsers should correctly identify their supported language" {
        checkAll(100, Arb.boolean()) { _ ->
            val javaParser = JavaLanguageParser()
            val kotlinParser = KotlinLanguageParser()
            
            // 验证语言名称
            javaParser.getLanguageName() shouldBe "Java"
            kotlinParser.getLanguageName() shouldBe "Kotlin"
            
            // 验证它们是不同的解析器
            javaParser.getLanguageName() shouldNotBe kotlinParser.getLanguageName()
        }
    }
    
    "Property 11: Parser selection should be deterministic based on element type" {
        checkAll(100, Arb.list(Arb.boolean(), 1..20)) { elementTypes ->
            val javaParser = JavaLanguageParser()
            val kotlinParser = KotlinLanguageParser()
            
            elementTypes.forEach { isJava ->
                if (isJava) {
                    val javaMethod = mock<PsiMethod>()
                    // Java解析器应该支持Java方法
                    javaParser.supports(javaMethod) shouldBe true
                    // Kotlin解析器不应该支持Java方法
                    kotlinParser.supports(javaMethod) shouldBe false
                } else {
                    val kotlinFunction = mock<KtNamedFunction>()
                    // Kotlin解析器应该支持Kotlin函数
                    kotlinParser.supports(kotlinFunction) shouldBe true
                    // Java解析器不应该支持Kotlin函数
                    javaParser.supports(kotlinFunction) shouldBe false
                }
            }
        }
    }
    
    "Property 11: Each parser should handle null or invalid elements gracefully" {
        checkAll(100, Arb.boolean()) { _ ->
            val javaParser = JavaLanguageParser()
            val kotlinParser = KotlinLanguageParser()
            
            // 测试Java解析器处理Kotlin元素
            val kotlinFunction = mock<KtNamedFunction>()
            val javaResult = javaParser.extractTipsFromMethod(kotlinFunction)
            javaResult shouldBe null
            
            // 测试Kotlin解析器处理Java元素
            val javaMethod = mock<PsiMethod>()
            val kotlinResult = kotlinParser.extractTipsFromMethod(javaMethod)
            kotlinResult shouldBe null
        }
    }
    
    "Property 11: Parser language names should be consistent across multiple calls" {
        checkAll(100, Arb.int(1..10)) { callCount ->
            val javaParser = JavaLanguageParser()
            val kotlinParser = KotlinLanguageParser()
            
            // 多次调用应该返回相同的语言名称
            val javaNames = (1..callCount).map { javaParser.getLanguageName() }
            val kotlinNames = (1..callCount).map { kotlinParser.getLanguageName() }
            
            // 验证一致性
            javaNames.all { it == "Java" } shouldBe true
            kotlinNames.all { it == "Kotlin" } shouldBe true
        }
    }
    
    "Property 11: Parsers should maintain type safety when checking support" {
        checkAll(100, Arb.languageElementType()) { elementType ->
            val javaParser = JavaLanguageParser()
            val kotlinParser = KotlinLanguageParser()
            
            when (elementType) {
                LanguageElementType.JAVA -> {
                    val javaMethod = mock<PsiMethod>()
                    javaParser.supports(javaMethod) shouldBe true
                    kotlinParser.supports(javaMethod) shouldBe false
                }
                LanguageElementType.KOTLIN -> {
                    val kotlinFunction = mock<KtNamedFunction>()
                    kotlinParser.supports(kotlinFunction) shouldBe true
                    javaParser.supports(kotlinFunction) shouldBe false
                }
            }
        }
    }
    
    "Property 11: Parser support checks should be mutually exclusive" {
        checkAll(100, Arb.boolean()) { _ ->
            val javaParser = JavaLanguageParser()
            val kotlinParser = KotlinLanguageParser()
            
            // 对于Java方法
            val javaMethod = mock<PsiMethod>()
            val javaSupportsJava = javaParser.supports(javaMethod)
            val kotlinSupportsJava = kotlinParser.supports(javaMethod)
            
            // 应该只有一个解析器支持
            (javaSupportsJava && !kotlinSupportsJava) shouldBe true
            
            // 对于Kotlin函数
            val kotlinFunction = mock<KtNamedFunction>()
            val javaSupportsKotlin = javaParser.supports(kotlinFunction)
            val kotlinSupportsKotlin = kotlinParser.supports(kotlinFunction)
            
            // 应该只有一个解析器支持
            (!javaSupportsKotlin && kotlinSupportsKotlin) shouldBe true
        }
    }
})

/**
 * 语言元素类型枚举
 */
enum class LanguageElementType {
    JAVA,
    KOTLIN
}

/**
 * 生成随机语言元素类型的Arb
 */
private fun Arb.Companion.languageElementType(): Arb<LanguageElementType> = arbitrary {
    if (Arb.boolean().bind()) {
        LanguageElementType.JAVA
    } else {
        LanguageElementType.KOTLIN
    }
}

/**
 * 生成随机整数的Arb
 */
private fun Arb.Companion.int(range: IntRange): Arb<Int> = arbitrary {
    range.random()
}
