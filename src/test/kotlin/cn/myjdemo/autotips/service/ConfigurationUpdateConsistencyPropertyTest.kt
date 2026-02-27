package cn.myjdemo.autotips.service

import cn.myjdemo.autotips.model.TipStyle
import cn.myjdemo.autotips.model.TipsConfiguration
import cn.myjdemo.autotips.service.impl.ConfigurationServiceImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * **Feature: auto-tips, Property 13: 配置更新一致性**
 * 
 * 对于任何用户配置修改，插件应该立即应用新配置并更新相应的行为或显示效果
 * 
 * **Validates: Requirements 6.2, 6.3, 6.4, 6.5**
 * 
 * 此属性测试验证：
 * - 6.2: 当用户修改提示显示时长时，插件应用新的配置并立即生效
 * - 6.3: 当用户选择不同的提示样式时，插件更新UI显示效果
 * - 6.4: 当用户配置自定义@tips标记格式时，插件支持自定义的注释模式
 * - 6.5: 当用户重置配置时，插件恢复所有设置到默认值
 */
class ConfigurationUpdateConsistencyPropertyTest : StringSpec({
    
    "Property 13: Configuration changes should be applied immediately and consistently" {
        checkAll<Boolean, Int, TipStyle>(100) { enabled, duration, style ->
            // 创建配置服务实例
            val configService = ConfigurationServiceImpl()
            
            // 确保持续时间在合理范围内
            val validDuration = duration.coerceIn(100, 60000)
            
            // 应用配置更改
            configService.setPluginEnabled(enabled)
            configService.setTipDisplayDuration(validDuration)
            configService.setTipStyle(style)
            
            // 验证配置立即生效
            configService.isPluginEnabled() shouldBe enabled
            configService.getTipDisplayDuration() shouldBe validDuration
            configService.getTipStyle() shouldBe style
            
            // 验证通过getCurrentConfiguration获取的配置也是一致的
            val currentConfig = configService.getCurrentConfiguration()
            currentConfig.enabled shouldBe enabled
            currentConfig.displayDuration shouldBe validDuration
            currentConfig.style shouldBe style
        }
    }
    
    "Property 13: saveConfiguration should apply all settings atomically" {
        checkAll(100, Arb.tipsConfiguration()) { config ->
            val configService = ConfigurationServiceImpl()
            
            // 保存完整配置
            configService.saveConfiguration(config)
            
            // 验证所有配置项都已应用
            val currentConfig = configService.getCurrentConfiguration()
            currentConfig.enabled shouldBe config.enabled
            currentConfig.displayDuration shouldBe config.displayDuration
            currentConfig.style shouldBe config.style
            currentConfig.customPatterns shouldBe config.customPatterns
            currentConfig.javadocModeEnabled shouldBe config.javadocModeEnabled
            
            // 验证通过单独的getter方法获取的值也一致
            configService.isPluginEnabled() shouldBe config.enabled
            configService.getTipDisplayDuration() shouldBe config.displayDuration
            configService.getTipStyle() shouldBe config.style
            configService.getCustomAnnotationPatterns() shouldBe config.customPatterns
            configService.isJavadocModeEnabled() shouldBe config.javadocModeEnabled
        }
    }
    
    "Property 13: Custom annotation patterns should be added and removed consistently" {
        checkAll(100, Arb.list(Arb.string(1..20), 0..10)) { patterns ->
            val configService = ConfigurationServiceImpl()
            
            // 添加所有模式
            patterns.forEach { pattern ->
                configService.addCustomAnnotationPattern(pattern)
            }
            
            // 验证所有唯一模式都已添加
            val uniquePatterns = patterns.distinct()
            val currentPatterns = configService.getCustomAnnotationPatterns()
            uniquePatterns.forEach { pattern ->
                currentPatterns.contains(pattern) shouldBe true
            }
            
            // 移除一半的模式
            val patternsToRemove = uniquePatterns.take(uniquePatterns.size / 2)
            patternsToRemove.forEach { pattern ->
                configService.removeCustomAnnotationPattern(pattern)
            }
            
            // 验证移除后的状态
            val remainingPatterns = configService.getCustomAnnotationPatterns()
            patternsToRemove.forEach { pattern ->
                remainingPatterns.contains(pattern) shouldBe false
            }
            
            val expectedRemaining = uniquePatterns.drop(uniquePatterns.size / 2)
            expectedRemaining.forEach { pattern ->
                remainingPatterns.contains(pattern) shouldBe true
            }
        }
    }
    
    "Property 13: resetToDefaults should restore all settings to default values" {
        checkAll(100, Arb.tipsConfiguration()) { config ->
            val configService = ConfigurationServiceImpl()
            
            // 应用自定义配置
            configService.saveConfiguration(config)
            
            // 重置到默认值
            configService.resetToDefaults()
            
            // 验证所有设置都恢复到默认值
            val defaultConfig = TipsConfiguration()
            val currentConfig = configService.getCurrentConfiguration()
            
            currentConfig.enabled shouldBe defaultConfig.enabled
            currentConfig.displayDuration shouldBe defaultConfig.displayDuration
            currentConfig.style shouldBe defaultConfig.style
            currentConfig.customPatterns shouldBe defaultConfig.customPatterns
            currentConfig.javadocModeEnabled shouldBe defaultConfig.javadocModeEnabled
        }
    }
    
    "Property 13: Multiple sequential configuration changes should maintain consistency" {
        checkAll(100, Arb.list(Arb.tipsConfiguration(), 1..10)) { configs ->
            val configService = ConfigurationServiceImpl()
            
            // 应用一系列配置更改
            configs.forEach { config ->
                configService.saveConfiguration(config)
                
                // 每次更改后立即验证一致性
                val currentConfig = configService.getCurrentConfiguration()
                currentConfig shouldBe config
                
                configService.isPluginEnabled() shouldBe config.enabled
                configService.getTipDisplayDuration() shouldBe config.displayDuration
                configService.getTipStyle() shouldBe config.style
                configService.getCustomAnnotationPatterns() shouldBe config.customPatterns
                configService.isJavadocModeEnabled() shouldBe config.javadocModeEnabled
            }
            
            // 验证最终状态与最后一个配置一致
            val finalConfig = configs.last()
            val currentConfig = configService.getCurrentConfiguration()
            currentConfig shouldBe finalConfig
        }
    }
    
    "Property 13: Individual setter methods should not affect other configuration values" {
        checkAll(100, Arb.tipsConfiguration()) { initialConfig ->
            val configService = ConfigurationServiceImpl()
            
            // 设置初始配置
            configService.saveConfiguration(initialConfig)
            
            // 只修改enabled状态
            val newEnabled = !initialConfig.enabled
            configService.setPluginEnabled(newEnabled)
            
            // 验证只有enabled改变，其他值保持不变
            configService.isPluginEnabled() shouldBe newEnabled
            configService.getTipDisplayDuration() shouldBe initialConfig.displayDuration
            configService.getTipStyle() shouldBe initialConfig.style
            configService.getCustomAnnotationPatterns() shouldBe initialConfig.customPatterns
            
            // 只修改displayDuration
            val newDuration = (initialConfig.displayDuration + 1000).coerceIn(100, 60000)
            configService.setTipDisplayDuration(newDuration)
            
            // 验证只有displayDuration改变
            configService.isPluginEnabled() shouldBe newEnabled
            configService.getTipDisplayDuration() shouldBe newDuration
            configService.getTipStyle() shouldBe initialConfig.style
            configService.getCustomAnnotationPatterns() shouldBe initialConfig.customPatterns
        }
    }
})

/**
 * 生成随机TipsConfiguration的Arb
 */
private fun Arb.Companion.tipsConfiguration(): Arb<TipsConfiguration> = arbitrary {
    TipsConfiguration(
        enabled = Arb.boolean().bind(),
        displayDuration = Arb.int(100..60000).bind(),
        style = Arb.enum<TipStyle>().bind(),
        customPatterns = Arb.list(Arb.string(1..20), 0..5).bind().distinct(),
        javadocModeEnabled = Arb.boolean().bind()
    )
}
