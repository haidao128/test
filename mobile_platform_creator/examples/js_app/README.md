# JavaScript 示例应用 (js_app)

这是一个简单的JavaScript应用示例，用于演示如何在MPK框架内运行JavaScript代码，并与框架提供的API进行交互。

## 功能

本示例主要演示以下功能：

- **启动和初始化**: 应用启动时打印基本信息。
- **平台API调用**: 获取平台信息，如版本号、操作系统等 (如果可用)。
- **文件系统API**: 尝试创建、读取、写入和删除文件 (如果权限允许且API可用)。
- **存储API**: 尝试使用键值对存储数据 (如果权限允许且API可用)。
- **网络API**: 尝试发起一个简单的HTTP GET请求 (如果权限允许且API可用)。
- **控制台输出**: 使用 `console.log` 输出日志信息。
- **错误处理**: 对可能失败的操作（如API不可用或权限不足）进行基本的错误处理和日志记录。

## 文件结构

- `main.js`: 应用的入口JavaScript文件，包含所有演示逻辑。
- `manifest.json`: 应用的清单文件，定义了应用的元数据、入口点和所需权限。

## 如何运行

1. **打包应用**: 使用MPK打包工具将此目录打包成 `.mpk` 文件。
   ```bash
   # 假设打包工具命令为 mpk-pack
   mpk-pack ./mobile_platform_creator/examples/js_app -o js_app_example.mpk
   ```
2. **加载和运行**: 使用MPK运行时加载并运行生成的 `js_app_example.mpk` 文件。
   ```python
   from mobile_platform_creator.core import MpkRuntime

   runtime = MpkRuntime()
   app_id = runtime.load_app("js_app_example.mpk")
   runtime.start_app(app_id)

   # ... (观察应用输出或进行其他交互) ...

   runtime.stop_app(app_id)
   runtime.unload_app(app_id)
   ```
   或者使用项目提供的测试脚本 `test_js_runtime.py` 来运行。

## 注意事项

- 应用请求了 `storage`, `network`, 和 `file` 权限。确保运行环境已授予这些权限，否则相关API调用会失败。
- 示例中的API调用 (`mpk.platform`, `mpk.fs`, `mpk.storage`, `mpk.network`) 依赖于MPK运行时提供的具体实现。如果运行时未提供这些API，应用会记录相应的错误信息。 