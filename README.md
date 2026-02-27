# Auto-Tips

一个非中断式的代码提示插件，当开发者调用带有 `@tips` 注释的方法时，自动显示提示信息。

## 功能特性

- 输入方法调用的右括号 `)` 时自动显示提示
- 支持 IDE 自动补全、输入法自动补全等多种触发方式
- 提示以非模态弹窗显示，不阻塞代码编辑
- 支持 Java 和 Kotlin
- 支持解析第三方库中的 `@tips` 注释
- 可选择显示完整 Javadoc 而非仅 `@tips` 内容

## 使用方法

在方法的 Javadoc/KDoc 注释中添加 `@tips` 标签：

```java
/**
 * 执行数据库查询
 *
 * @tips 耗时操作，建议在后台线程中调用
 * @tips 返回值可能为 null，请进行空值检查
 */
public ResultSet executeQuery(String query) { ... }
```

调用该方法时，输入右括号 `)` 即会自动弹出提示。

```java
ResultSet result = executeQuery("SELECT * FROM users");
//                                                    ^ 触发提示
```

Kotlin 同理：

```kotlin
/**
 * @tips 注意：此方法会修改原数组
 */
fun sortArray(arr: IntArray) { arr.sort() }

sortArray(data)  // 输入 ) 时显示提示
```

## 安装

从 JetBrains Marketplace 搜索 **Auto-Tips** 安装，或前往 [插件主页](https://plugins.jetbrains.com/) 下载。

## 配置

`File` → `Settings` → `Tools` → `Auto-Tips`

- 启用/禁用插件
- 提示显示时长（默认 5000ms）
- 提示样式（BALLOON / TOOLTIP / NOTIFICATION）
- 显示完整 Javadoc 而非仅 `@tips` 内容

## 许可证

[MIT](LICENSE)
