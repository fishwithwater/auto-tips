plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "cn.myjdemo"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2024.2.5")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }
    
    // Testing dependencies
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "243.*"
        }

        changeNotes = """
            <h3>1.0.0</h3>
            <ul>
                <li>初始版本发布</li>
                <li>支持 @tips 注释自动提示功能</li>
                <li>支持 Java 和 Kotlin 语言</li>
                <li>支持多种触发方式（手动输入、IDE 自动补全、输入法补全）</li>
                <li>提供可配置的提示样式和显示时长</li>
                <li>实现智能缓存机制提升性能</li>
                <li>支持解析外部依赖库中的 @tips 注释</li>
                <li>完善的错误处理和恢复机制</li>
                <li>修复兼容性问题，支持更广泛的IDE版本</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
    
    // Configure test task to use both JUnit 4 (for IntelliJ Platform tests) and JUnit 5/Kotest
    test {
        useJUnitPlatform()
        useJUnit()
    }
}
