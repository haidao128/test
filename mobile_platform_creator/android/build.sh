#!/bin/bash
# Android构建脚本
# 用于编译和打包Android应用平台

set -e

# 基础目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_DIR/build/android"
OUTPUT_DIR="$PROJECT_DIR/dist"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 打印信息函数
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# 检查环境
check_environment() {
    info "检查Android构建环境..."
    
    # 检查ANDROID_HOME环境变量
    if [ -z "$ANDROID_HOME" ]; then
        error "ANDROID_HOME环境变量未设置。请安装Android SDK并设置ANDROID_HOME。"
    fi
    
    # 检查Gradle
    if ! command -v gradle &> /dev/null; then
        error "找不到gradle命令。请安装Gradle。"
    fi
    
    # 检查NDK
    if [ ! -d "$ANDROID_HOME/ndk" ]; then
        warn "在ANDROID_HOME中找不到NDK目录。请确保NDK已安装。"
    fi
    
    info "环境检查通过。"
}

# 清理构建目录
clean_build() {
    info "清理构建目录..."
    rm -rf "$BUILD_DIR"
    mkdir -p "$BUILD_DIR"
    mkdir -p "$OUTPUT_DIR"
}

# 编译本地库
build_native_libs() {
    info "编译本地库..."
    
    # 检查NDK路径
    NDK_PATH="$ANDROID_HOME/ndk-bundle"
    if [ ! -d "$NDK_PATH" ]; then
        # 尝试查找最新版本的NDK
        NDK_PATH=$(find "$ANDROID_HOME/ndk" -maxdepth 1 -type d | sort -r | head -n 1)
        if [ -z "$NDK_PATH" ]; then
            error "找不到NDK路径。请安装Android NDK。"
        fi
    fi
    
    info "使用NDK路径: $NDK_PATH"
    
    # 进入native目录
    cd "$SCRIPT_DIR/native"
    
    # 检查CMakeLists.txt
    if [ ! -f "CMakeLists.txt" ]; then
        error "找不到CMakeLists.txt。请确保native目录中包含CMakeLists.txt。"
    fi
    
    # 为每个ABI创建构建目录
    ABIS=("armeabi-v7a" "arm64-v8a" "x86" "x86_64")
    
    for ABI in "${ABIS[@]}"; do
        info "编译 $ABI 架构..."
        
        BUILD_ABI_DIR="$BUILD_DIR/native/$ABI"
        mkdir -p "$BUILD_ABI_DIR"
        
        # 使用CMake配置
        cmake -DCMAKE_TOOLCHAIN_FILE="$NDK_PATH/build/cmake/android.toolchain.cmake" \
              -DANDROID_ABI="$ABI" \
              -DANDROID_NATIVE_API_LEVEL=21 \
              -DCMAKE_BUILD_TYPE=Release \
              -B"$BUILD_ABI_DIR" \
              -H.
        
        # 编译
        cmake --build "$BUILD_ABI_DIR" -- -j4
        
        # 复制生成的库文件
        mkdir -p "$BUILD_DIR/jniLibs/$ABI"
        find "$BUILD_ABI_DIR" -name "*.so" -exec cp {} "$BUILD_DIR/jniLibs/$ABI/" \;
    done
    
    info "本地库编译完成。"
}

# 编译Java代码
build_java() {
    info "编译Java代码..."
    
    # 复制Java源码到构建目录
    mkdir -p "$BUILD_DIR/java"
    cp -r "$SCRIPT_DIR/java" "$BUILD_DIR/"
    
    # 使用Gradle编译
    cd "$SCRIPT_DIR"
    gradle clean assembleRelease
    
    # 检查APK是否生成
    APK_PATH="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
    if [ ! -f "$APK_PATH" ]; then
        error "APK构建失败，找不到输出文件。"
    fi
    
    # 复制APK到输出目录
    cp "$APK_PATH" "$OUTPUT_DIR/mobile_platform_creator.apk"
    
    info "Java代码编译完成。"
}

# 打包资源
package_resources() {
    info "打包资源文件..."
    
    # 复制资源文件
    mkdir -p "$BUILD_DIR/assets"
    cp -r "$PROJECT_DIR/assets" "$BUILD_DIR/"
    
    # 复制配置文件
    mkdir -p "$BUILD_DIR/res"
    cp -r "$SCRIPT_DIR/res" "$BUILD_DIR/"
    
    info "资源打包完成。"
}

# 创建最终的APK包
create_apk() {
    info "创建最终APK包..."
    
    # 签名APK
    APK_UNSIGNED="$OUTPUT_DIR/mobile_platform_creator-unsigned.apk"
    APK_FINAL="$OUTPUT_DIR/mobile_platform_creator.apk"
    KEYSTORE="$PROJECT_DIR/keystore/release.keystore"
    
    # 检查密钥库是否存在
    if [ ! -f "$KEYSTORE" ]; then
        warn "找不到发布密钥库。将使用调试密钥签名。"
        KEYSTORE="$HOME/.android/debug.keystore"
        KEY_ALIAS="androiddebugkey"
        STORE_PASSWORD="android"
        KEY_PASSWORD="android"
    else
        KEY_ALIAS="release"
        
        # 从环境变量或提示用户输入
        if [ -z "$KEYSTORE_PASSWORD" ]; then
            read -sp "请输入密钥库密码: " STORE_PASSWORD
            echo
            read -sp "请输入密钥密码: " KEY_PASSWORD
            echo
        else
            STORE_PASSWORD="$KEYSTORE_PASSWORD"
            KEY_PASSWORD="$KEY_PASSWORD"
        fi
    fi
    
    # 使用apksigner签名
    "$ANDROID_HOME/build-tools/30.0.3/apksigner" sign --ks "$KEYSTORE" \
        --ks-key-alias "$KEY_ALIAS" \
        --ks-pass "pass:$STORE_PASSWORD" \
        --key-pass "pass:$KEY_PASSWORD" \
        --out "$APK_FINAL" \
        "$APK_UNSIGNED"
    
    # 验证APK签名
    "$ANDROID_HOME/build-tools/30.0.3/apksigner" verify "$APK_FINAL"
    
    info "APK创建并签名完成: $APK_FINAL"
}

# 构建应用平台
build_platform() {
    info "开始构建Android应用平台..."
    
    # 执行构建步骤
    check_environment
    clean_build
    build_native_libs
    build_java
    package_resources
    create_apk
    
    info "Android应用平台构建完成。"
    info "APK文件位置: $OUTPUT_DIR/mobile_platform_creator.apk"
}

# 显示帮助信息
show_help() {
    echo "使用方法: $0 [选项]"
    echo "选项:"
    echo "  --clean       仅清理构建目录"
    echo "  --native      仅构建本地库"
    echo "  --java        仅构建Java代码"
    echo "  --resources   仅打包资源"
    echo "  --apk         仅创建APK包"
    echo "  --help        显示此帮助信息"
    echo "如果没有提供选项，将执行完整的构建过程。"
}

# 主函数
main() {
    # 解析命令行参数
    if [ $# -eq 0 ]; then
        build_platform
    else
        while [ $# -gt 0 ]; do
            case "$1" in
                --clean)
                    clean_build
                    ;;
                --native)
                    build_native_libs
                    ;;
                --java)
                    build_java
                    ;;
                --resources)
                    package_resources
                    ;;
                --apk)
                    create_apk
                    ;;
                --help)
                    show_help
                    exit 0
                    ;;
                *)
                    error "未知选项: $1"
                    ;;
            esac
            shift
        done
    fi
}

# 执行主函数
main "$@" 