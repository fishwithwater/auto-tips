# Auto-Tips IntelliJ IDEA 插件

一个智能的、非中断式的代码提示插件，当开发者调用带有 `@tips` 注释的方法时，自动显示提示信息。

## 功能特性

### 核心功能

- **智能提示显示**：在输入方法调用的右括号 `)` 时自动显示提示
- **非中断式设计**：提示以非模态弹窗显示，不阻塞代码编辑
- **多语言支持**：支持 Java 和 Kotlin
- **多种触发方式**：
  - 手动输入右括号
  - IDE 自动补全（按 Enter）
  - 输入法自动补全括号

### 高级特性

- **自定义 Javadoc 标签**：`@tips` 标签支持代码补全和语法高亮
- **智能缓存**：LRU 缓存策略提升性能
- **配置管理**：可自定义提示显示时长、样式等
- **外部依赖支持**：可解析第三方库中的 `@tips` 注释
- **错误恢复机制**：异常不会影响 IDE 稳定性

## 工作原理

### 1. 注释标记

在方法的 Javadoc/KDoc 注释中使用 `@tips` 标签：

```java
/**
 * 执行数据库查询
 * 
 * @tips 这是一个耗时操作，建议在后台线程中调用
 * @tips 返回值可能为 null，请进行空值检查
 * 
 * @param query SQL 查询语句
 * @return 查询结果
 */
public ResultSet executeQuery(String query) {
    // 实现代码
}
```

### 2. 自动触发

当开发者调用该方法时：

```java
ResultSet result = executeQuery("SELECT * FROM users");
//                                                    ^ 输入右括号时触发
```

插件会自动显示提示弹窗，内容为 `@tips` 标记后的文本。

### 3. 技术架构

```
用户输入 → 事件监听 → 方法检测 → 注释解析 → 提示显示
    ↓           ↓           ↓           ↓           ↓
  输入 )    监听器捕获   PSI 分析   提取 @tips   非模态弹窗
```

**核心组件**：

- **TipsTypedActionHandler**：监听用户输入事件
- **AutoTipsEditorFactoryListener**：监听文档变更和光标移动
- **CallDetectionService**：检测方法调用
- **AnnotationParser**：解析 `@tips` 注释
- **TipDisplayService**：显示提示弹窗
- **CacheService**：缓存解析结果

## 安装和使用

### 从 JetBrains Marketplace 安装（推荐）

1. 打开 IntelliJ IDEA
2. `File` → `Settings` → `Plugins`
3. 搜索 "Auto-Tips"
4. 点击 `Install` 安装
5. 重启 IDE

### 从源码构建

#### 构建插件包

**Windows:**
```bash
build-plugin.bat
```

**Linux/Mac:**
```bash
chmod +x build-plugin.sh
./build-plugin.sh
```

或使用 Gradle 命令：
```bash
./gradlew buildPlugin
```

生成的插件文件位于 `build/distributions/auto-tips-1.0.0.zip`

#### 手动安装

1. 打开 IntelliJ IDEA
2. `File` → `Settings` → `Plugins`
3. 点击齿轮图标 → `Install Plugin from Disk...`
4. 选择 `build/distributions/auto-tips-1.0.0.zip`
5. 重启 IDE

### 运行测试 IDE

```bash
./gradlew runIde
```

### 配置插件

1. 打开 IDE 设置：`File` → `Settings` → `Tools` → `Auto-Tips`
2. 可配置：
   - 启用/禁用插件
   - 提示显示时长（默认 5000ms）
   - 提示样式（BALLOON/TOOLTIP/NOTIFICATION）

## 使用示例

### Java

```java
public class Example {
    /**
     * @tips 注意：此方法会修改原数组
     */
    public void sortArray(int[] arr) {
        Arrays.sort(arr);
    }
    
    public void test() {
        int[] data = {3, 1, 2};
        sortArray(data);  // 输入 ) 时显示提示
    }
}
```

### Kotlin

```kotlin
class Example {
    /**
     * @tips 注意：此方法会修改原数组
     */
    fun sortArray(arr: IntArray) {
        arr.sort()
    }
    
    fun test() {
        val data = intArrayOf(3, 1, 2)
        sortArray(data)  // 输入 ) 时显示提示
    }
}
```

## 技术栈

- **语言**：Kotlin 1.9+
- **框架**：IntelliJ Platform SDK 2024.2+
- **构建工具**：Gradle 8.12+
- **测试框架**：JUnit 4, Kotest

## 性能指标

- **初始化时间**：< 1 秒
- **响应时间**：< 200ms（缓存命中时）
- **内存占用**：LRU 缓存限制 1000 条目
- **稳定性**：异常不影响 IDE

## 项目结构

```
src/main/kotlin/cn/myjdemo/autotips/
├── handler/              # 输入事件处理
├── javadoc/              # Javadoc 标签支持
├── lifecycle/            # 生命周期管理
├── model/                # 数据模型
├── service/              # 核心服务
│   ├── AnnotationParser.kt
│   ├── CallDetectionService.kt
│   ├── TipDisplayService.kt
│   ├── CacheService.kt
│   └── impl/             # 服务实现
└── settings/             # 设置界面
```

## 开发文档

- **需求文档**：`.kiro/specs/auto-tips/requirements.md`
- **设计文档**：`.kiro/specs/auto-tips/design.md`
- **任务计划**：`.kiro/specs/auto-tips/tasks.md`
- **发布指南**：`PUBLISH_GUIDE.md`

## 发布到 JetBrains Marketplace

详细的发布流程请参考 [PUBLISH_GUIDE.md](PUBLISH_GUIDE.md)

快速步骤：
1. 构建插件：`./gradlew buildPlugin`
2. 注册 JetBrains 账号：https://plugins.jetbrains.com/
3. 上传插件包：`build/distributions/auto-tips-1.0.0.zip`
4. 填写插件信息并提交审核
5. 等待审核通过（1-3 个工作日）

## 许可证

[待定]

## 贡献

欢迎提交 Issue 和 Pull Request。

## 联系方式

- **项目主页**: [待定]
- **问题反馈**: [待定]
- **邮箱**: your-email@example.com

