cmake_minimum_required(VERSION 3.10.2)

# 设置项目名称
project(mobile_platform_creator)

# 设置C/C++标准
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 14)

# 添加include目录
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/seccomp
    ${CMAKE_CURRENT_SOURCE_DIR}/wasm
)

# 添加seccomp过滤器库
add_library(seccomp_filter SHARED
    seccomp/seccomp_filter.c
)

# 添加wasm运行时库
add_library(wasm_runtime SHARED
    wasm/wasm_runtime.c
)

# 链接库
target_link_libraries(seccomp_filter
    log
    jnigraphics
    android
)

target_link_libraries(wasm_runtime
    log
    jnigraphics
    android
)

# 添加预处理器定义
target_compile_definitions(seccomp_filter PRIVATE
    ANDROID_PLATFORM
    DEBUG=$<CONFIG:Debug>
)

target_compile_definitions(wasm_runtime PRIVATE
    ANDROID_PLATFORM
    DEBUG=$<CONFIG:Debug>
)

# 设置输出目录
set_target_properties(seccomp_filter PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/lib/${ANDROID_ABI}
)

set_target_properties(wasm_runtime PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/lib/${ANDROID_ABI}
)

# 设置安装规则
install(TARGETS seccomp_filter wasm_runtime
    LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib/${ANDROID_ABI}
) 