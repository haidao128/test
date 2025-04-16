# MPK框架 API 参考文档

本目录包含MPK框架各个模块的详细API文档。

文档是基于源代码注释自动生成的，旨在提供准确、全面的接口信息，帮助开发者理解和使用MPK框架提供的各种功能。

## 如何使用

- 浏览左侧导航栏或下方列表，选择您感兴趣的模块。
- 每个模块文档包含类、方法、属性的详细说明、参数解释、返回值以及示例代码。

## 主要模块

- **核心 (core)**: 包含框架的核心逻辑，如运行时、沙箱、权限管理等。
  - `MpkRuntime`: 应用加载、生命周期管理。
  - `Sandbox`: 应用隔离和资源限制。
  - `PermissionManager`: 权限管理。
- **运行时 (runtimes)**: 不同代码类型的具体实现。
  - `JavaScriptRuntime`: JavaScript执行环境。
  - `PythonRuntime`: Python执行环境 (待实现)。
  - `WasmRuntime`: WebAssembly执行环境 (待实现)。
- **构建器 (builder)**: 用于创建MPK文件的工具。
  - `MpkBuilder`: 构建MPK包。
- **工具类 (utils)**: 提供各种辅助功能。

**注意**: 本文档仍在建设中，部分模块可能尚未完全覆盖。 