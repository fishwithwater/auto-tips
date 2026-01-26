package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.service.impl.JavaLanguageParser
import com.intellij.psi.PsiMethod
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.mockito.kotlin.mock

/**
 * Java语言支持的单元测试
 * 
 * **验证需求: 5.1**
 * 
 * 测试Java特定的注释格式和方法调用解析
 */
class JavaLanguageParserTest : StringSpec({
    
    "Java parser should support PsiMethod elements" {
        val parser = JavaLanguageParser()
        val javaMethod = mock<PsiMethod>()
        
        parser.supports(javaMethod) shouldBe true
    }
    
    "Java parser should not support Kotlin elements" {
        val parser = JavaLanguageParser()
        val kotlinFunction = mock<KtNamedFunction>()
        
        parser.supports(kotlinFunction) shouldBe false
    }
    
    "Java parser should return correct language name" {
        val parser = JavaLanguageParser()
        
        parser.getLanguageName() shouldBe "Java"
    }
    
    "Java parser should return null for non-Java elements" {
        val parser = JavaLanguageParser()
        val kotlinFunction = mock<KtNamedFunction>()
        
        val result = parser.extractTipsFromMethod(kotlinFunction)
        result shouldBe null
    }
    
    "Java parser should handle methods without doc comments" {
        val parser = JavaLanguageParser()
        val javaMethod = mock<PsiMethod>()
        
        // Mock method without doc comment
        val result = parser.extractTipsFromMethod(javaMethod)
        result shouldBe null
    }
    
    "Multiple Java parser instances should be independent" {
        val parser1 = JavaLanguageParser()
        val parser2 = JavaLanguageParser()
        
        parser1.getLanguageName() shouldBe parser2.getLanguageName()
        parser1 shouldNotBe parser2 // Different instances
    }
})
