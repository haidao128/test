# MPK应用框架

MPK (Mobile Platform Kit) 是一个轻量级的移动应用容器框架，用于在移动平台上运行第三方应用。MPK框架提供了应用沙箱隔离、资源限制、权限管理和应用间通信等关键功能，确保第三方应用在安全、可控的环境中运行。

## 核心功能

- **应用打包**: 将应用代码、资源和元数据打包为标准化的`.mpk`文件
- **应用运行时**: 支持多种代码类型（JavaScript、Python、二进制等）的执行环境
- **沙箱环境**: 为每个应用提供隔离的文件系统、资源限制和安全策略
- **权限管理**: 精细化的权限控制和运行时权限申请机制
- **应用间通信**: 安全可控的应用间消息传递和事件机制
- **资源监控**: 实时监控和管理应用的资源使用情况

## MPK文件格式规范 (v2.1)

MPK文件是一种基于标准ZIP格式的应用容器，包含以下主要部分：

### 文件结构

```
example_app.mpk
├── manifest.json         # 应用清单（必需）
├── code/                 # 代码目录（必需）
│   └── ...               # 应用代码文件（至少一个）
├── assets/               # 资源目录（可选，但推荐包含）
│   └── ...               # 静态资源文件
├── config/               # 配置目录（可选）
│   └── ...               # 应用配置文件
└── signature.sig         # 数字签名文件（必需）
```

### manifest.json (必需)

应用清单文件，使用JSON格式，UTF-8编码。必须包含以下字段：

```json
{
  "format_version": "2.1",           // MPK格式版本号 (必需, 字符串)
  "id": "com.example.app",           // 应用唯一ID (必需, 字符串, 推荐使用反向域名)
  "name": "示例应用",                // 应用名称 (必需, 字符串)
  "version": "1.0.0",                // 应用版本名称 (必需, 字符串, 遵循语义化版本)
  "platform": "all",                 // 目标平台 (必需, 字符串, 如 "android", "ios", "desktop", "all")
  "min_platform_version": "1.0.0",   // 平台最低版本要求 (必需, 字符串)
  "code_type": "javascript",         // 主要代码类型 (必需, 字符串: "javascript", "python", "wasm", "binary", "web")
  "entry_point": "code/main.js",     // 应用入口点 (必需, 字符串, 相对于MPK包根目录的路径)

  // --- 可选字段 ---
  "version_code": 1,                 // 应用版本号 (可选, 整数)
  "description": "这是一个示例应用。", // 应用描述 (可选, 字符串)
  "author": {                        // 作者信息 (可选, 对象)
    "name": "作者名称",
    "email": "author@example.com"
  },
  "icon": "assets/icon.png",         // 应用图标路径 (可选, 字符串, 相对于MPK包根目录的路径)
  "splash": "assets/splash.png",     // 启动画面路径 (可选, 字符串)
  "permissions": [                   // 应用所需权限列表 (可选, 字符串数组)
    "storage",
    "network",
    "camera"
  ],
  "dependencies": [                  // 依赖列表 (可选, 对象数组)
    {
      "name": "依赖库名称",
      "version": ">=1.0.0"
    }
  ],
  "sandbox": {                       // 沙箱配置 (可选, 对象)
    "max_storage": 104857600,        // 最大存储空间（字节）
    "max_memory": 536870912          // 最大内存使用（字节）
  },
  "extra_data": {                    // 其他自定义元数据 (可选, 对象)
    "custom_key": "custom_value"
  }
}
```

### 代码目录 (code/) (必需)

包含应用的可执行代码。
- 必须包含`manifest.json`中`entry_point`指定的文件。
- 可以包含子目录和库文件。
- 结构取决于`code_type`(例如，JavaScript项目可能包含`node_modules`或打包后的文件，Python项目可能包含`.py`文件和依赖包)。

### 资源目录 (assets/) (可选)

包含应用使用的静态资源，如图像、音频、字体、本地化字符串文件等。
- 目录结构由应用自行定义。
- `manifest.json`中的`icon`和`splash`路径通常指向此目录下的文件。

### 配置目录 (config/) (可选)

包含应用的配置文件，例如默认设置、环境配置等。
- 结构和内容由应用自行定义。

### 签名文件 (signature.sig) (必需)

包含用于验证MPK包完整性和来源的数字签名信息。

**签名过程**：
1. 获取MPK包内除`signature.sig`文件外的所有文件的**相对路径**和**文件内容**。
2. 对文件列表按照**相对路径**进行**字典序排序**。
3. 依次计算每个文件的哈希值（推荐SHA-256）。为了确保一致性，计算哈希时应包含文件的相对路径字符串（UTF-8编码）和文件二进制内容。
4. 将所有文件的哈希值组合成一个最终的待签名数据摘要。
5. 使用开发者的私钥对该摘要进行签名（推荐RSA-SHA256或ECDSA-SHA256）。
6. 将签名结果（通常是Base64编码的字符串或二进制数据）写入`signature.sig`文件。

**验证过程**：
1. 读取`signature.sig`文件获取签名值。
2. 重复签名过程的步骤1-4，计算出包内容的摘要。
3. 使用签名者公钥验证签名值是否与计算出的摘要匹配。

## 框架结构

MPK框架主要由以下组件构成：

1. **MPK文件格式**: 基于ZIP的应用包格式，包含清单文件、代码、资源和签名
2. **MPK运行时**: 应用的生命周期管理、资源分配和运行环境
3. **MPK沙箱**: 资源限制、文件系统隔离和访问控制
4. **MPK权限管理器**: 权限申请、授权和验证
5. **MPK进程管理器**: 应用进程的创建、监控和终止
6. **MPK通信管理器**: 应用间的消息传递和事件分发

## 最新增强：沙箱资源管理

最近，我们对MPK框架进行了重要的沙箱资源管理优化，主要包括：

### 1. 沙箱事件通知系统

- 新增事件类型：资源警告、资源超限、沙箱创建/删除、资源清理
- 事件监听器机制：允许应用和系统组件注册监听器，接收资源状态变化通知
- 警告阈值设置：在资源接近限制时提前发出警告通知

### 2. 智能资源管理

- 资源使用预警系统：根据资源使用趋势预测潜在的资源问题
- 自动资源清理：在资源接近限制时自动执行清理操作
- 梯度响应策略：根据资源超限程度执行不同级别的处理

### 3. 资源监控可视化

- 实时资源使用情况展示
- 资源限制和使用率的图形化展示
- 交互式资源管理操作（清理缓存、停止应用等）

## 使用方法

### 创建MPK应用包

```java
// 创建一个简单的MPK文件
File outputFile = new File(context.getFilesDir(), "example.mpk");
MpkBuilder builder = new MpkBuilder();
builder.setAppName("示例应用")
        .setPackageName("com.example.simple")
        .setVersion(1, "1.0.0")
        .setAuthor("张三", "zhangsan@example.com")
        .setDescription("这是一个简单的示例应用");

// 设置代码和资源
String code = "console.log('Hello, MPK!');";
builder.setCodeData(code.getBytes("UTF-8"), "javascript", "main.js");
builder.setResourcesData(resources);

// 构建MPK文件
builder.build(outputFile);
```

### 加载和运行应用

```java
// 创建MPK运行时
MpkRuntime runtime = new MpkRuntime(context);

// 加载应用
String appId = runtime.loadApp(new File("/path/to/app.mpk"));

// 启动应用
runtime.startApp(appId);

// 停止应用
runtime.stopApp(appId);

// 卸载应用
runtime.uninstallApp(appId);
```

### 使用资源监控UI

```java
// 创建资源监控UI
MpkResourceMonitorUI monitorUI = runtime.createResourceMonitorUI(context, appId);
container.addView(monitorUI);

// 启动监控
monitorUI.startMonitoring();

// 停止监控
monitorUI.stopMonitoring();
```

## 安全考虑

### 沙箱机制

- 限制应用访问系统资源
- 实现权限控制系统
- 隔离应用数据和代码

### 代码验证

- 验证代码完整性
- 检查恶意代码模式
- 限制危险操作

### 资源访问控制

- 限制文件系统访问
- 控制网络访问
- 管理设备功能访问

## 开发工具

### MPK打包工具

- 命令行界面
- 图形界面（可选）
- 支持批量处理

### MPK查看器

- 查看.mpk文件内容
- 提取资源和代码
- 验证签名

### 开发文档和示例

- API文档
- 示例应用
- 最佳实践指南

## 示例应用

我们提供了一个完整的示例应用，用于演示MPK框架的功能，特别是沙箱资源限制的工作方式：

```java
// 运行完整示例
MpkExample.fullDemonstration(context, container);
```

这个示例会创建一个测试MPK文件，加载应用，启动资源监控UI，然后运行应用，测试各种资源限制。

## 未来规划

我们计划在以下方面继续改进MPK框架：

1. **性能优化**: 减少资源监控对系统性能的影响
2. **跨平台支持**: 扩展到更多平台（iOS、Web等）
3. **插件系统**: 可扩展的插件架构，支持第三方功能扩展
4. **开发者工具**: 更完善的开发、调试和分析工具
5. **安全增强**: 代码签名验证、运行时完整性检查等

## 贡献

欢迎贡献代码、报告问题或提出建议。请遵循项目的代码风格和贡献指南。

## 许可证

该项目采用[Apache 2.0许可证](LICENSE)。 