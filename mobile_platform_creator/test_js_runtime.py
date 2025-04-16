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
from typing import Dict, Any, Optional

# 设置日志级别
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 导入相关模块
from mobile_platform_creator.core.js_runtime.js_launcher import (
    load_app, start_app, stop_app, unload_app, get_app_info, 
    call_app_function, is_app_running
)
from mobile_platform_creator.utils.mpk_package import MPKPackage

def create_test_mpk(output_path: str) -> str:
    """
    创建测试MPK包
    
    Args:
        output_path: 输出文件路径
        
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
    
    # 添加示例应用代码
    example_js_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "examples", "js_app")
    if os.path.exists(example_js_dir):
        mpk.add_directory(example_js_dir, "code")
    else:
        # 如果示例目录不存在，创建简单的示例代码
        code_content = """
/**
 * 简单的JavaScript测试应用
 */
console.log("Hello from JavaScript!");
console.log("APP_ID:", APP_ID);
console.log("APP_PATH:", APP_PATH);

// 测试文件操作
if (fs && fs.writeFile) {
    fs.mkdir("data");
    fs.writeFile("data/test.txt", "Hello, World!");
    console.log("写入文件成功");
    
    const content = fs.readFile("data/test.txt");
    console.log("读取文件内容:", content);
}

// 测试存储
if (storage && storage.setItem) {
    storage.setItem("test", "value");
    console.log("存储内容:", storage.getItem("test"));
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

def test_js_runtime(mpk_path: Optional[str] = None) -> None:
    """
    测试JavaScript运行时
    
    Args:
        mpk_path: MPK文件路径，为None时创建临时MPK
    """
    print("开始测试JavaScript运行时...")
    
    # 创建或使用MPK包
    if not mpk_path:
        output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "temp")
        os.makedirs(output_dir, exist_ok=True)
        mpk_path = os.path.join(output_dir, "test_jsapp.mpk")
        mpk_path = create_test_mpk(mpk_path)
    
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

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="测试JavaScript运行时功能")
    parser.add_argument("--mpk", help="指定MPK文件路径", default=None)
    args = parser.parse_args()
    
    test_js_runtime(args.mpk) 