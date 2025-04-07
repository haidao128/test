# Android平台支持

本目录包含针对Android平台的实现代码和配置文件。

## 目录结构

```
android/
├── native/                 # 本地代码（JNI/C++实现）
│   ├── seccomp/            # Seccomp沙箱实现
│   └── wasm/               # WebAssembly运行时本地实现
├── java/                   # Java代码
│   └── com/mobileplatform/creator/
│       ├── sandbox/        # 沙箱Java API
│       ├── runtime/        # 运行时实现
│       └── ui/             # 用户界面
├── res/                    # 资源文件
├── templates/              # 项目模板
│   ├── app/                # 应用模板
│   └── manifest/           # 清单文件模板
└── gradle/                 # Gradle配置
```

## 功能实现

### 安全沙箱

Android平台的安全沙箱基于以下技术实现：

1. **Seccomp过滤器**: 限制应用可以使用的系统调用
2. **SELinux策略**: 增强的权限控制
3. **进程隔离**: 应用在独立进程中运行
4. **资源限制**: 限制CPU、内存等资源使用

### WebAssembly运行时

支持在Android上运行WebAssembly代码：

1. **WASM加载器**: 加载和验证WASM模块
2. **内存管理**: 安全的内存隔离和管理
3. **API桥接**: 连接WASM代码与Android API

### 应用打包

为应用创建APK文件：

1. **APK构建系统**: 基于Gradle的构建流程
2. **资源打包**: 应用资源的处理与打包
3. **签名系统**: APK签名和验证

## 开发指南

### 构建原生库

```bash
cd mobile_platform_creator/android/native
./build.sh
```

### 编译Java代码

```bash
cd mobile_platform_creator/android
./gradlew assembleDebug
```

### 生成应用模板

```bash
python -m mobile_platform_creator.android.tools.generate_template \
    --app-id com.example.myapp \
    --template basic
```

## 注意事项

- 确保已安装Android SDK和NDK
- 需要API Level 21+支持
- 运行测试前需验证设备连接状态 