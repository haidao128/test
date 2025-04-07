"""
移动应用平台核心模块
================

提供移动应用平台的核心功能实现。
"""

import os
import sys
import platform as plt_module
import subprocess
import logging
from typing import Dict, Any, List, Optional

logger = logging.getLogger("mobile_platform_creator.core")

def get_system_info() -> Dict[str, Any]:
    """
    获取系统信息
    
    Returns:
        Dict[str, Any]: 系统信息
    """
    info = {
        "os": os.name,
        "platform": plt_module.system(),
        "platform_version": plt_module.version(),
        "machine": plt_module.machine(),
        "python_version": sys.version,
    }
    
    return info

def is_android() -> bool:
    """检查是否运行在Android平台"""
    # 检查是否在Termux或其他Android Python环境中运行
    if os.path.exists("/system/bin/adb") or os.environ.get("ANDROID_ROOT"):
        return True
    
    # 检查设备型号
    try:
        output = subprocess.check_output(["getprop", "ro.product.model"], 
                                       stderr=subprocess.DEVNULL, 
                                       universal_newlines=True)
        if output.strip():
            return True
    except (subprocess.SubprocessError, FileNotFoundError):
        pass
    
    return False

def is_pyto() -> bool:
    """检查是否运行在Pyto环境中"""
    return "Pyto" in os.environ.get("HOME", "")

def is_pythonista() -> bool:
    """检查是否运行在Pythonista环境中"""
    return "Pythonista" in os.environ.get("HOME", "")

def get_platform() -> str:
    """
    获取当前运行平台
    
    Returns:
        str: 平台名称，"desktop"或"android"
    """
    if is_android():
        return "android"
    else:
        return "desktop"

# 导入子模块
from . import sandbox
from . import wasm_runtime
from . import builder
from . import deployment

def init_core_modules() -> bool:
    """
    初始化所有核心模块
    
    Returns:
        bool: 初始化是否成功
    """
    success = True
    for module_name in CORE_MODULES:
        try:
            # 动态导入模块
            module = importlib.import_module(f"mobile_platform_creator.core.{module_name}")
            
            # 调用模块的init函数
            if hasattr(module, "init"):
                init_result = module.init()
                if not init_result:
                    logger.error(f"模块初始化失败: {module_name}")
                    success = False
            else:
                logger.warning(f"模块无初始化函数: {module_name}")
        except ImportError:
            logger.warning(f"无法导入模块: {module_name}")
            success = False
        except Exception as e:
            logger.error(f"初始化模块 {module_name} 失败: {e}")
            success = False
    
    return success

def get_platform_info() -> Dict[str, Any]:
    """
    获取平台信息
    
    Returns:
        Dict[str, Any]: 平台信息字典
    """
    import platform
    
    return {
        "system": platform.system(),
        "release": platform.release(),
        "version": platform.version(),
        "architecture": platform.architecture(),
        "machine": platform.machine(),
        "python_version": platform.python_version(),
        "platform": platform.platform()
    }

# 导出常用子模块接口
__all__ = ["sandbox", "wasm_runtime", "builder", "deployment"]

def is_platform_info_valid(info: Dict[str, Any]) -> bool:
    """检查平台信息是否有效"""
    return all(key in info for key in ["system", "release", "version", "architecture", "machine", "python_version", "platform"])

def platform_info_to_str(info: Dict[str, Any]) -> str:
    """将平台信息转换为字符串"""
    if not is_platform_info_valid(info):
        return "无效的平台信息"
    return f"{info['system']} {info['release']} ({info['architecture']})"

def platform_info_to_dict(info: Dict[str, Any]) -> Dict[str, Any]:
    """将平台信息转换为字典"""
    if not is_platform_info_valid(info):
        return {}
    return {
        "system": info["system"],
        "release": info["release"],
        "version": info["version"],
        "architecture": info["architecture"],
        "machine": info["machine"],
        "python_version": info["python_version"],
        "platform": info["platform"]
    } 