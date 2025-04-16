#!/usr/bin/env python
"""
JavaScript运行时测试脚本
=======================

用于测试JavaScript运行时功能，包括加载和运行JavaScript应用。
"""

import os
import sys
import json
import logging
import time
import argparse
import pytest
from typing import Dict, Any, Optional

# 设置日志级别
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

# 添加项目根目录到路径
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)

# 导入相关模块
from mobile_platform_creator.core.js_runtime.js_launcher import (
    load_app, start_app, stop_app, unload_app, get_app_info, 
    call_app_function, is_app_running
)
from mobile_platform_creator.utils.mpk_package import MPKPackage

# 测试数据目录
TEST_DATA_DIR = os.path.join(project_root, "tests", "test_data")
os.makedirs(TEST_DATA_DIR, exist_ok=True)

def create_test_mpk(output_path: str, use_example: bool = True) -> str:
    """
    创建测试MPK包
    
    Args:
        output_path: 输出文件路径
        use_example: 是否使用示例应用，否则创建临时示例代码
        
    Returns:
        str: MPK文件路径
    """
    print(f"创建测试MPK包: {output_path}")
    
    # 创建MPK包
    mpk = MPKPackage()
    
    # 设置清单数据
    manifest = {
        "id": "com.example.jsapp",
        "name": "JavaScript测试应用",
        "version": "1.0.0",
        "code_type": "javascript",
        "entry_point": "main.js",
        "platform": "desktop",
        "min_platform_version": "1.0.0",
        "description": "一个简单的JavaScript应用示例",
        "author": "MPC团队",
        "permissions": ["storage", "network", "file"]
    }
    mpk.set_manifest(manifest)
    
    if use_example:
        # 查找示例JavaScript应用
        example_js_dir = os.path.join(project_root, "mobile_platform_creator", "examples", "js_app")
        
        if os.path.exists(example_js_dir) and os.path.isdir(example_js_dir):
            print(f"找到示例应用目录: {example_js_dir}")
            # 检查是否有main.js文件
            if os.path.exists(os.path.join(example_js_dir, "main.js")):
                mpk.add_directory(example_js_dir, "code")
            else:
                print(f"示例目录中没有main.js文件")
                use_example = False
    
    if not use_example:
        print("创建临时示例代码")
        # 创建简单的示例代码
        code_content = """
/**
 * 简单的JavaScript测试应用
 */
console.log("Hello from JavaScript!");
console.log("APP_ID:", APP_ID);
console.log("APP_PATH:", APP_PATH);

// 测试文件操作
if (fs && fs.writeFile) {
    console.log("测试文件操作...");
    try {
        fs.mkdir("data");
        console.log("创建目录成功");
        
        fs.writeFile("data/test.txt", "Hello, World!");
        console.log("写入文件成功");
        
        const content = fs.readFile("data/test.txt");
        console.log("读取文件内容:", content);
    } catch (error) {
        console.error("文件操作出错:", error);
    }
}

// 测试存储
if (storage && storage.setItem) {
    console.log("测试存储功能...");
    try {
        storage.setItem("test", "value");
        storage.setItem("lastRun", new Date().toISOString());
        console.log("存储内容:", storage.getItem("test"));
        console.log("上次运行时间:", storage.getItem("lastRun"));
    } catch (error) {
        console.error("存储操作出错:", error);
    }
}

// 测试网络请求
if (network && network.httpGet) {
    console.log("测试网络请求...");
    try {
        const response = network.httpGet("https://httpbin.org/get");
        console.log("HTTP响应状态:", response.status);
    } catch (error) {
        console.error("网络请求出错:", error);
    }
}

// 导出函数
function getStatus() {
    return {
        time: new Date().toISOString(),
        message: "应用运行正常"
    };
}
"""
        temp_dir = os.path.join(os.path.dirname(output_path), "temp_code")
        os.makedirs(temp_dir, exist_ok=True)
        
        with open(os.path.join(temp_dir, "main.js"), "w", encoding="utf-8") as f:
            f.write(code_content)
        
        mpk.add_directory(temp_dir, "code")
    
    # 保存MPK包
    mpk.save(output_path)
    print(f"测试MPK包已创建: {output_path}")
    
    return output_path

@pytest.fixture
def test_mpk():
    """创建测试MPK包的fixture"""
    mpk_path = os.path.join(TEST_DATA_DIR, "test_jsapp.mpk")
    return create_test_mpk(mpk_path)

@pytest.fixture
def test_app(test_mpk):
    """加载测试应用的fixture"""
    app_id = load_app(test_mpk)
    yield app_id
    # 测试结束后清理
    try:
        if is_app_running(app_id):
            stop_app(app_id)
        unload_app(app_id)
    except Exception as e:
        print(f"清理测试应用时出错: {e}")

def test_load_app(test_mpk):
    """测试加载应用"""
    app_id = load_app(test_mpk)
    assert app_id is not None
    assert app_id == "com.example.jsapp"
    
    # 获取应用信息
    app_info = get_app_info(app_id)
    assert app_info is not None
    assert app_info["id"] == app_id
    assert app_info["name"] == "JavaScript测试应用"
    
    # 清理
    unload_app(app_id)

def test_start_stop_app(test_app):
    """测试启动和停止应用"""
    # 启动应用
    start_result = start_app(test_app)
    assert start_result is True
    
    # 检查应用状态
    assert is_app_running(test_app) is True
    
    # 等待应用运行
    time.sleep(2)
    
    # 停止应用
    stop_result = stop_app(test_app)
    assert stop_result is True
    assert is_app_running(test_app) is False

def test_call_app_function(test_app):
    """测试调用应用函数"""
    # 启动应用
    start_app(test_app)
    time.sleep(1)  # 等待应用初始化
    
    # 调用函数
    result = call_app_function(test_app, "getStatus")
    assert result is not None
    assert "time" in result
    assert "message" in result
    assert result["message"] == "应用运行正常"
    
    # 停止应用
    stop_app(test_app)

def test_app_lifecycle(test_mpk):
    """测试应用完整生命周期"""
    # 加载应用
    app_id = load_app(test_mpk)
    assert app_id is not None
    
    # 启动应用
    start_result = start_app(app_id)
    assert start_result is True
    assert is_app_running(app_id) is True
    
    # 等待应用运行
    time.sleep(2)
    
    # 调用函数
    result = call_app_function(app_id, "getStatus")
    assert result is not None
    
    # 停止应用
    stop_result = stop_app(app_id)
    assert stop_result is True
    assert is_app_running(app_id) is False
    
    # 卸载应用
    unload_result = unload_app(app_id)
    assert unload_result is True

def test_invalid_app_id():
    """测试无效的应用ID"""
    with pytest.raises(Exception):
        get_app_info("invalid_app_id")
    
    with pytest.raises(Exception):
        start_app("invalid_app_id")
    
    with pytest.raises(Exception):
        stop_app("invalid_app_id")
    
    with pytest.raises(Exception):
        unload_app("invalid_app_id")
    
    with pytest.raises(Exception):
        call_app_function("invalid_app_id", "getStatus")

def test_invalid_mpk():
    """测试无效的MPK包"""
    invalid_mpk_path = os.path.join(TEST_DATA_DIR, "invalid.mpk")
    with open(invalid_mpk_path, "w") as f:
        f.write("This is not a valid MPK file")
    
    with pytest.raises(Exception):
        load_app(invalid_mpk_path)
    
    os.remove(invalid_mpk_path)

def test_multiple_apps():
    """测试多个应用同时运行"""
    # 创建两个不同的MPK包
    mpk1_path = os.path.join(TEST_DATA_DIR, "test_app1.mpk")
    mpk2_path = os.path.join(TEST_DATA_DIR, "test_app2.mpk")
    
    # 修改第二个应用的ID
    mpk1 = create_test_mpk(mpk1_path)
    mpk2 = MPKPackage()
    manifest = {
        "id": "com.example.jsapp2",
        "name": "JavaScript测试应用2",
        "version": "1.0.0",
        "code_type": "javascript",
        "entry_point": "main.js",
        "platform": "desktop",
        "min_platform_version": "1.0.0",
        "description": "第二个测试应用",
        "author": "MPC团队",
        "permissions": ["storage", "network", "file"]
    }
    mpk2.set_manifest(manifest)
    mpk2.add_directory(os.path.dirname(mpk1), "code")
    mpk2.save(mpk2_path)
    
    # 加载两个应用
    app_id1 = load_app(mpk1_path)
    app_id2 = load_app(mpk2_path)
    
    # 启动两个应用
    start_app(app_id1)
    start_app(app_id2)
    
    # 验证两个应用都在运行
    assert is_app_running(app_id1) is True
    assert is_app_running(app_id2) is True
    
    # 调用两个应用的函数
    result1 = call_app_function(app_id1, "getStatus")
    result2 = call_app_function(app_id2, "getStatus")
    
    assert result1 is not None
    assert result2 is not None
    
    # 停止和卸载应用
    stop_app(app_id1)
    stop_app(app_id2)
    unload_app(app_id1)
    unload_app(app_id2)
    
    # 清理临时文件
    os.remove(mpk1_path)
    os.remove(mpk2_path)

def test_js_runtime(mpk_path: Optional[str] = None) -> None:
    """
    测试JavaScript运行时
    
    Args:
        mpk_path: MPK文件路径，为None时创建临时MPK
    """
    print("开始测试JavaScript运行时...")
    
    # 创建或使用MPK包
    if not mpk_path:
        # 创建临时目录用于测试
        temp_dir = os.path.join(project_root, "temp")
        os.makedirs(temp_dir, exist_ok=True)
        mpk_path = os.path.join(temp_dir, "test_jsapp.mpk")
        mpk_path = create_test_mpk(mpk_path)
    
    try:
        # 加载应用
        print("\n1. 加载JavaScript应用")
        app_id = load_app(mpk_path)
        print(f"应用已加载: {app_id}")
        
        # 获取应用信息
        app_info = get_app_info(app_id)
        print(f"应用信息: {json.dumps(app_info, indent=2)}")
        
        # 启动应用
        print("\n2. 启动JavaScript应用")
        start_result = start_app(app_id)
        print(f"应用启动结果: {start_result}")
        
        # 检查应用状态
        print(f"应用是否运行中: {is_app_running(app_id)}")
        
        # 等待一段时间让应用运行
        print("等待应用运行...")
        time.sleep(2)
        
        # 尝试调用应用函数
        print("\n3. 调用应用函数")
        try:
            result = call_app_function(app_id, "getStatus")
            print(f"函数调用结果: {result}")
        except Exception as e:
            print(f"调用函数失败: {e}")
        
        # 停止应用
        print("\n4. 停止JavaScript应用")
        stop_result = stop_app(app_id)
        print(f"应用停止结果: {stop_result}")
        
        # 卸载应用
        print("\n5. 卸载JavaScript应用")
        unload_result = unload_app(app_id)
        print(f"应用卸载结果: {unload_result}")
        
        print("\n测试完成!")
    except Exception as e:
        print(f"测试过程中出错: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="测试JavaScript运行时功能")
    parser.add_argument("--mpk", help="指定MPK文件路径", default=None)
    args = parser.parse_args()
    
    if args.mpk:
        # 如果指定了MPK文件，运行单个测试
        test_js_runtime(args.mpk)
    else:
        # 否则运行所有测试
        pytest.main([__file__]) 