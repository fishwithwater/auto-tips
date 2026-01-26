package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.service.impl.KotlinLanguageParser
import com.intellij.psi.PsiMethod
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.mockito.kotlin.mock

/**
 * Kotlin语言支持的单元测试
 * 
 * **验证需求: 5.2**
 * 
 * 测试Kotlin特定的注释格式和方法调用解析
 */
class KotlinLanguageParserTest : StringSpec({
    
    "Kotlin parser should support KtNamedFunction elements" {
        val parser = KotlinLanguageParser()
        val kotlinFunction = mock<KtNamedFunction>()
        
        parser.supports(kotlinFunction) shouldBe true
    }
    
    "Kotlin parser should not support Java elements" {
        val parser = KotlinLanguageParser()
        val javaMethod = mock<PsiMethod>()
        
        parser.supports(javaMethod) shouldBe false
    }
    
    "Kotlin parser should return correct language name" {
        val parser = KotlinLanguageParser()
        
        parser.getLanguageName() shouldBe "Kotlin"
    }
    
    "Kotlin parser should return null for non-Kotlin elements" {
        val parser = KotlinLanguageParser()
        val javaMethod = mock<PsiMethod>()
        
        val result = parser.extractTipsFromMethod(javaMethod)
        result shouldBe null
    }
    
    "Kotlin parser should handle functions without KDoc comments" {
        val parser = KotlinLanguageParser()
        val kotlinFunction = mock<KtNamedFunction>()
        
        // Mock function without KDoc comment
        val result = parser.extractTipsFromMethod(kotlinFunction)
        result shouldBe null
    }
    
    "Multiple Kotlin parser instances should be independent" {
        val parser1 = KotlinLanguageParser()
        val parser2 = KotlinLanguageParser()
        
        parser1.getLanguageName() shouldBe parser2.getLanguageName()
        parser1 shouldNotBe parser2 // Different instances
    }
})
