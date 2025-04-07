# 开发环境设置指南

## 基础要求
- Python 3.8+ (推荐Python 3.10)
- Android Studio 2023.2+ (Android开发)
- Flutter 3.10+

## 环境配置步骤

### 1. Python环境配置
```bash
# 创建虚拟环境
python -m venv venv

# 激活虚拟环境
# Windows
venv\Scripts\activate
# macOS/Linux
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt
```

### 2. Android开发环境
1. 安装Android Studio
2. 安装Android SDK (API Level 31+)
3. 配置ANDROID_HOME环境变量
4. 安装NDK
5. 配置为开发者模式的Android设备

### 3. Flutter环境
1. 安装Flutter SDK
2. 配置Flutter环境变量
3. 运行`flutter doctor`确认环境正确

### 4. WebAssembly工具链
1. 安装Emscripten
2. 安装wasm-pack
3. 配置WASI SDK

## 编译与运行

### 构建Android APK
```bash
cd mobile_platform_creator
python build.py --platform android
```

## 调试

### Android应用调试
```bash
python debug.py --platform android --device <设备ID>
```

## 常见问题排查

### Android构建问题
- 签名密钥配置
- NDK路径问题
- Gradle依赖解析失败

## 性能分析工具
- Android Profiler
- Python性能分析器 