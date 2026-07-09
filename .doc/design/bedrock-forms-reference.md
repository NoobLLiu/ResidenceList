# 基岩版表单开发参考文档

本项目需要为 Geyser/Floodgate 互通服务器的基岩版玩家提供 Bedrock Forms 表单界面。
以下为开发所需的全部参考文档与资源链接。

## 1. 官方文档

### 1.1 Cumulus Forms 入门
- [Geyser Forms 文档（中文）](https://geysermc.cn/wiki/geyser/forms) - 三种表单类型入门
- [Geyser Forms 文档（英文）](https://geysermc.org/wiki/geyser/forms/) - 同上英文版
- [Floodgate Wiki - Forms](https://github.com/GeyserMC/Floodgate/wiki/Forms) - Floodgate 视角的表单说明

### 1.2 API 接入
- [Getting Started with the API](https://geysermc.org/wiki/geyser/getting-started-with-the-api/) - Maven 依赖与 API 初始化
- [Floodgate API 文档](https://geysermc.cn/wiki/floodgate/api/) - FloodgateApi 类说明
- [Geyser API 文档](https://geysermc.org/wiki/geyser/api/) - GeyserApi 类说明

## 2. 源码仓库

### 2.1 Cumulus（表单库）
- [Cumulus GitHub 仓库](https://github.com/GeyserMC/Cumulus) - 表单 API 源码
- [CustomForm.java](https://github.com/GeyserMC/Cumulus/blob/master/src/main/java/org/geysermc/cumulus/form/CustomForm.java) - CustomForm 接口与 Builder
- [SimpleForm.java](https://github.com/GeyserMC/Cumulus/blob/master/src/main/java/org/geysermc/cumulus/form/SimpleForm.java) - SimpleForm 接口与 Builder
- [ModalForm.java](https://github.com/GeyserMC/Cumulus/blob/master/src/main/java/org/geysermc/cumulus/form/ModalForm.java) - ModalForm 接口与 Builder
- [Components 目录](https://github.com/GeyserMC/Cumulus/tree/master/src/main/java/org/geysermc/cumulus/component) - 所有表单组件
- [SimpleFormResponse.java](https://github.com/GeyserMC/Cumulus/blob/master/src/main/java/org/geysermc/cumulus/response/SimpleFormResponse.java) - SimpleForm 响应
- [CustomFormResponse.java](https://github.com/GeyserMC/Cumulus/blob/master/src/main/java/org/geysermc/cumulus/response/CustomFormResponse.java) - CustomForm 响应

### 2.2 Floodgate（基岩版玩家检测）
- [Floodgate GitHub 仓库](https://github.com/GeyserMC/Floodgate) - Floodgate 源码
- [SimpleFloodgateApi.java](https://github.com/GeyserMC/Floodgate/blob/master/core/src/main/java/org/geysermc/floodgate/api/SimpleFloodgateApi.java) - FloodgateApi 实现

## 3. Maven 依赖配置

### 3.1 仓库
```xml
<repository>
    <id>opencollab-snapshot</id>
    <url>https://repo.opencollab.dev/main/</url>
</repository>
```

### 3.2 Floodgate API（用于后端直装模式）
```xml
<dependency>
    <groupId>org.geysermc.floodgate</groupId>
    <artifactId>api</artifactId>
    <version>2.2.5-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

> 注意：Cumulus API 包含在 Floodgate API 依赖中，无需单独引入。

## 4. 核心 API 速查

### 4.1 检测基岩版玩家
```java
FloodgateApi api = FloodgateApi.getInstance();
boolean isBedrock = api.isFloodgatePlayer(player.getUniqueId());
```

### 4.2 发送表单
```java
// 方式一：通过 UUID
FloodgateApi.getInstance().sendForm(uuid, formBuilder);

// 方式二：通过 FloodgatePlayer
FloodgatePlayer fp = FloodgateApi.getInstance().getPlayer(uuid);
fp.sendForm(formBuilder);
```

### 4.3 三种表单类型

| 类型 | 用途 | 对应本项目页面 |
|------|------|----------------|
| ModalForm | 二选一确认对话框 | 删除评价确认、切换公开/私有 |
| SimpleForm | 列表+按钮 | 领地列表、详情菜单 |
| CustomForm | 多组件表单 | 编辑领地、创建领地、评分评价 |

### 4.4 CustomForm 组件
- `label(text)` - 纯文本标签
- `input(text, placeholder)` - 文本输入框
- `toggle(text, defaultValue)` - 开关
- `slider(text, min, max, step, defaultValue)` - 滑块
- `stepSlider(text, options...)` - 步进滑块
- `dropdown(text, options...)` - 下拉选择

### 4.5 响应处理
```java
.validResultHandler(response -> {
    // 按顺序读取: response.next() 或 response.asInput(index) 等
})
.closedResultHandler(() -> { /* 玩家关闭表单 */ })
.closedOrInvalidResultHandler(() -> { /* 关闭或无效 */ })
```

## 5. 参考实现

- [ProjectE-plugin 基岩版 GUI PR](https://github.com/Little100/ProjectE-plugin/pull/20/files) - 一个完整的 BedrockFormUtil 实现，包含 Floodgate 检测、表单发送、Session Token 防重放

## 6. 注意事项

1. **Floodgate 依赖作用域为 `provided`**：服务器需安装 Floodgate，但插件在无 Floodgate 时应优雅降级（Java 版玩家不受影响）
2. **本项目为 Paper 后端直装模式**：直接使用 `FloodgateApi`，无需考虑代理端数据转发
3. **表单无法完全替代所有 GUI 交互**：如 `SelectIconGUI`（背包点击选图标）需用 `Dropdown` 或文字输入替代
4. **异步响应处理**：表单响应回调在 Netty 线程，涉及 Bukkit API 时需 `Bukkit.getScheduler().runTask()` 切回主线程
