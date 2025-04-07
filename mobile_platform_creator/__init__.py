"""
移动端自主创建应用平台 (MobilePlatformCreator)
====================================

这是一个基于Python开发的移动端应用平台生成工具，允许用户安装后自主创建个性化应用平台。
该项目支持Android平台，通过安全沙箱技术提供安全隔离的运行环境，并使用WebAssembly实现多语言程序支持。

主要模块:
- sandbox: 安全沙箱模块，实现系统调用过滤与进程隔离
- wasm_runtime: WebAssembly运行时，支持多语言程序运行
- builder: 平台构建工具，提供可视化界面配置功能
- deployment: 私有化部署组件，支持专属域名生成与NAS设备绑定
"""

import os
import sys
import logging
import json
from typing import Dict, Any, Optional

# 设置版本号
__version__ = "0.1.0"
__author__ = "MobilePlatformCreator Team"
__license__ = "MIT"

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger("mobile_platform_creator")

# 全局配置
CONFIG = {
    "data_dir": os.path.expanduser("~/mobile_platform_creator"),
    "temp_dir": os.path.join(os.path.expanduser("~/mobile_platform_creator"), "temp"),
    "cache_dir": os.path.join(os.path.expanduser("~/mobile_platform_creator"), "cache"),
    "apps_dir": os.path.join(os.path.expanduser("~/mobile_platform_creator"), "apps"),
    "debug": False,
    "platform": None,  # "android"
    "sandbox_level": "strict",  # "strict", "standard", "minimal"
}

def init(platform: str, debug: bool = False) -> bool:
    """
    初始化移动平台创建工具
    
    Args:
        platform: 目标平台，"android"
        debug: 是否开启调试模式
        
    Returns:
        bool: 初始化是否成功
    """
    global CONFIG
    
    # 检查平台
    platform = platform.lower()
    if platform not in ["android"]:
        logger.error("不支持的平台: %s，仅支持 'android'", platform)
        return False
    
    # 设置配置
    CONFIG["platform"] = platform
    CONFIG["debug"] = debug
    
    # 创建目录
    os.makedirs(CONFIG["data_dir"], exist_ok=True)
    os.makedirs(CONFIG["temp_dir"], exist_ok=True)
    os.makedirs(CONFIG["cache_dir"], exist_ok=True)
    os.makedirs(CONFIG["apps_dir"], exist_ok=True)
    
    # 平台特定目录
    if platform == "android":
        # Android数据目录
        android_dir = os.path.join(CONFIG["data_dir"], "android")
        os.makedirs(android_dir, exist_ok=True)
    
    # 设置日志级别
    if debug:
        logging.getLogger("mobile_platform_creator").setLevel(logging.DEBUG)
    else:
        logging.getLogger("mobile_platform_creator").setLevel(logging.INFO)
    
    logger.info("初始化完成，平台: %s，调试模式: %s", platform, debug)
    return True

def get_version() -> str:
    """获取当前版本号"""
    return __version__

def get_config() -> Dict[str, Any]:
    """获取当前配置"""
    config_path = os.path.expanduser("~/MobilePlatform/config.json")
    if os.path.exists(config_path):
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"读取配置文件失败: {e}")
    
    # 返回默认配置
    return {
        "platform": "desktop",
        "debug": False,
        "theme": "default",
        "layout": "grid",
        "sandbox_level": "normal",
        "auto_update": False
    }

# 导入核心模块
from . import core
from . import utils

# 导入其他模块
try:
    from . import sandbox, wasm_runtime, builder, deployment
except ImportError as e:
    logger.warning("部分模块导入失败: %s", e) 