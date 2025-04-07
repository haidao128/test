"""
平台构建工具模块
=============

提供可视化平台构建工具，允许用户自定义主题、布局和预装应用。

主要功能：
1. 平台主题自定义
2. 应用布局配置
3. 预装应用管理
4. 交互式界面设计
"""

import logging
import os
import json
import time
import shutil
from typing import Dict, List, Any, Optional, Tuple, Union, Set

logger = logging.getLogger("mobile_platform_creator.core.builder")

# 全局变量
_initialized = False
_config_dir = ""
_themes_dir = ""
_layouts_dir = ""
_templates_dir = ""
_current_theme = "default"
_current_layout = "grid"
_available_apps: Dict[str, Dict[str, Any]] = {}

# 默认主题配置
DEFAULT_THEME = {
    "name": "默认主题",
    "id": "default",
    "primary_color": "#1976D2",
    "secondary_color": "#03DAC6",
    "background_color": "#F5F5F5",
    "text_color": "#333333",
    "accent_color": "#FF4081",
    "dark_mode": False,
    "font_family": "Roboto",
    "card_style": "rounded",
    "animations": "enabled",
    "icon_pack": "material",
}

# 支持的布局类型
LAYOUT_TYPES = ["grid", "list", "card", "dashboard", "custom"]

# 默认布局配置
DEFAULT_LAYOUTS = {
    "grid": {
        "name": "网格布局",
        "id": "grid",
        "columns": 4,
        "spacing": 8,
        "alignment": "center",
        "icon_size": "medium",
        "show_labels": True,
        "show_categories": True,
        "animations": "slide",
    },
    "list": {
        "name": "列表布局",
        "id": "list",
        "show_icons": True,
        "icon_position": "left",
        "divider_style": "line",
        "show_descriptions": True,
        "grouping": "category",
    },
    "card": {
        "name": "卡片布局",
        "id": "card",
        "card_width": 160,
        "card_height": 180,
        "card_radius": 8,
        "show_shadows": True,
        "card_spacing": 12,
    },
    "dashboard": {
        "name": "仪表盘布局",
        "id": "dashboard",
        "widgets": ["calendar", "weather", "notes", "shortcuts", "news"],
        "allow_reordering": True,
        "responsive": True,
    }
}

def init(config_dir: Optional[str] = None) -> bool:
    """
    初始化平台构建工具
    
    Args:
        config_dir: 配置文件目录，默认为None(自动选择)
        
    Returns:
        bool: 初始化是否成功
    """
    global _initialized, _config_dir, _themes_dir, _layouts_dir, _templates_dir
    
    if _initialized:
        logger.warning("平台构建工具已经初始化，请勿重复初始化")
        return True
    
    try:
        # 初始化配置目录
        if config_dir:
            _config_dir = config_dir
        else:
            # 自动选择配置目录
            from .. import is_android, is_ios
            
            if is_android():
                _config_dir = "/data/data/com.mobilecreator.app/files/config"
            elif is_ios():
                _config_dir = os.path.expanduser("~/Documents/MobilePlatformCreator/config")
            else:
                _config_dir = os.path.join(os.path.dirname(__file__), "..", "..", "..", "config")
        
        # 创建必要的子目录
        _themes_dir = os.path.join(_config_dir, "themes")
        _layouts_dir = os.path.join(_config_dir, "layouts")
        _templates_dir = os.path.join(_config_dir, "templates")
        
        os.makedirs(_themes_dir, exist_ok=True)
        os.makedirs(_layouts_dir, exist_ok=True)
        os.makedirs(_templates_dir, exist_ok=True)
        
        logger.info("平台构建工具配置目录: %s", _config_dir)
        
        # 初始化默认主题和布局
        _init_default_themes()
        _init_default_layouts()
        
        # 扫描可用的应用
        _scan_available_apps()
        
        _initialized = True
        logger.info("平台构建工具初始化成功")
        return True
    except Exception as e:
        logger.error("平台构建工具初始化失败: %s", e)
        return False

def _init_default_themes() -> None:
    """初始化默认主题"""
    default_theme_path = os.path.join(_themes_dir, "default.json")
    dark_theme_path = os.path.join(_themes_dir, "dark.json")
    
    # 创建默认主题
    if not os.path.exists(default_theme_path):
        with open(default_theme_path, "w", encoding="utf-8") as f:
            json.dump(DEFAULT_THEME, f, ensure_ascii=False, indent=2)
    
    # 创建暗色主题
    if not os.path.exists(dark_theme_path):
        dark_theme = DEFAULT_THEME.copy()
        dark_theme.update({
            "name": "暗色主题",
            "id": "dark",
            "primary_color": "#BB86FC",
            "secondary_color": "#03DAC6",
            "background_color": "#121212",
            "text_color": "#FFFFFF",
            "accent_color": "#CF6679",
            "dark_mode": True
        })
        with open(dark_theme_path, "w", encoding="utf-8") as f:
            json.dump(dark_theme, f, ensure_ascii=False, indent=2)
    
    logger.debug("默认主题初始化完成")

def _init_default_layouts() -> None:
    """初始化默认布局"""
    for layout_id, layout_config in DEFAULT_LAYOUTS.items():
        layout_path = os.path.join(_layouts_dir, f"{layout_id}.json")
        
        if not os.path.exists(layout_path):
            with open(layout_path, "w", encoding="utf-8") as f:
                json.dump(layout_config, f, ensure_ascii=False, indent=2)
    
    logger.debug("默认布局初始化完成")

def _scan_available_apps() -> None:
    """扫描可用的应用"""
    global _available_apps
    
    # 在实际实现中，这里会扫描应用目录，读取应用元数据
    # 这里只是模拟一些预置应用
    _available_apps = {
        "calculator": {
            "name": "计算器",
            "id": "calculator",
            "icon": "calc.png",
            "version": "1.0.0",
            "category": "工具",
            "description": "简单易用的计算器应用",
            "size": 1024 * 1024,  # 1MB
            "preinstalled": True
        },
        "notepad": {
            "name": "记事本",
            "id": "notepad",
            "icon": "notepad.png",
            "version": "1.0.0",
            "category": "办公",
            "description": "记录笔记和待办事项",
            "size": 1.5 * 1024 * 1024,  # 1.5MB
            "preinstalled": True
        },
        "gallery": {
            "name": "相册",
            "id": "gallery",
            "icon": "gallery.png",
            "version": "1.0.0",
            "category": "多媒体",
            "description": "查看和管理照片与视频",
            "size": 2 * 1024 * 1024,  # 2MB
            "preinstalled": True
        },
        "weather": {
            "name": "天气",
            "id": "weather",
            "icon": "weather.png",
            "version": "1.0.0",
            "category": "生活",
            "description": "查看天气预报",
            "size": 1.8 * 1024 * 1024,  # 1.8MB
            "preinstalled": True
        },
        "game_2048": {
            "name": "2048游戏",
            "id": "game_2048",
            "icon": "2048.png",
            "version": "1.0.0",
            "category": "游戏",
            "description": "经典数字益智游戏",
            "size": 0.8 * 1024 * 1024,  # 0.8MB
            "preinstalled": False
        }
    }
    
    logger.debug("发现 %d 个可用应用", len(_available_apps))

def get_available_themes() -> List[Dict[str, Any]]:
    """
    获取所有可用的主题
    
    Returns:
        List[Dict[str, Any]]: 主题列表
    """
    themes = []
    
    for filename in os.listdir(_themes_dir):
        if filename.endswith(".json"):
            theme_path = os.path.join(_themes_dir, filename)
            try:
                with open(theme_path, "r", encoding="utf-8") as f:
                    theme = json.load(f)
                themes.append(theme)
            except Exception as e:
                logger.error("加载主题失败 %s: %s", filename, e)
    
    return themes

def get_available_layouts() -> List[Dict[str, Any]]:
    """
    获取所有可用的布局
    
    Returns:
        List[Dict[str, Any]]: 布局列表
    """
    layouts = []
    
    for filename in os.listdir(_layouts_dir):
        if filename.endswith(".json"):
            layout_path = os.path.join(_layouts_dir, filename)
            try:
                with open(layout_path, "r", encoding="utf-8") as f:
                    layout = json.load(f)
                layouts.append(layout)
            except Exception as e:
                logger.error("加载布局失败 %s: %s", filename, e)
    
    return layouts

def get_available_apps() -> Dict[str, Dict[str, Any]]:
    """
    获取所有可用的应用
    
    Returns:
        Dict[str, Dict[str, Any]]: 应用字典，以应用ID为键
    """
    return _available_apps.copy()

def set_current_theme(theme_id: str) -> bool:
    """
    设置当前主题
    
    Args:
        theme_id: 主题ID
        
    Returns:
        bool: 设置是否成功
    """
    global _current_theme
    
    theme_path = os.path.join(_themes_dir, f"{theme_id}.json")
    if not os.path.exists(theme_path):
        logger.error("主题不存在: %s", theme_id)
        return False
    
    _current_theme = theme_id
    logger.info("已设置当前主题: %s", theme_id)
    return True

def set_current_layout(layout_id: str) -> bool:
    """
    设置当前布局
    
    Args:
        layout_id: 布局ID
        
    Returns:
        bool: 设置是否成功
    """
    global _current_layout
    
    layout_path = os.path.join(_layouts_dir, f"{layout_id}.json")
    if not os.path.exists(layout_path):
        logger.error("布局不存在: %s", layout_id)
        return False
    
    _current_layout = layout_id
    logger.info("已设置当前布局: %s", layout_id)
    return True

def create_custom_theme(theme_config: Dict[str, Any]) -> Optional[str]:
    """
    创建自定义主题
    
    Args:
        theme_config: 主题配置
        
    Returns:
        Optional[str]: 成功时返回主题ID，失败时返回None
    """
    if "id" not in theme_config or not theme_config["id"]:
        theme_id = f"custom_{int(time.time())}"
        theme_config["id"] = theme_id
    else:
        theme_id = theme_config["id"]
    
    if "name" not in theme_config or not theme_config["name"]:
        theme_config["name"] = f"自定义主题 {theme_id}"
    
    # 确保包含所有必要属性
    for key, value in DEFAULT_THEME.items():
        if key not in theme_config:
            theme_config[key] = value
    
    theme_path = os.path.join(_themes_dir, f"{theme_id}.json")
    try:
        with open(theme_path, "w", encoding="utf-8") as f:
            json.dump(theme_config, f, ensure_ascii=False, indent=2)
        
        logger.info("已创建自定义主题: %s", theme_id)
        return theme_id
    except Exception as e:
        logger.error("创建自定义主题失败: %s", e)
        return None

def create_custom_layout(layout_config: Dict[str, Any]) -> Optional[str]:
    """
    创建自定义布局
    
    Args:
        layout_config: 布局配置
        
    Returns:
        Optional[str]: 成功时返回布局ID，失败时返回None
    """
    if "id" not in layout_config or not layout_config["id"]:
        layout_id = f"custom_{int(time.time())}"
        layout_config["id"] = layout_id
    else:
        layout_id = layout_config["id"]
    
    if "name" not in layout_config or not layout_config["name"]:
        layout_config["name"] = f"自定义布局 {layout_id}"
    
    layout_path = os.path.join(_layouts_dir, f"{layout_id}.json")
    try:
        with open(layout_path, "w", encoding="utf-8") as f:
            json.dump(layout_config, f, ensure_ascii=False, indent=2)
        
        logger.info("已创建自定义布局: %s", layout_id)
        return layout_id
    except Exception as e:
        logger.error("创建自定义布局失败: %s", e)
        return None

def get_current_theme() -> Dict[str, Any]:
    """获取当前主题配置"""
    theme_path = os.path.join(_themes_dir, f"{_current_theme}.json")
    try:
        with open(theme_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        logger.error("加载当前主题失败: %s", e)
        return DEFAULT_THEME.copy()

def get_current_layout() -> Dict[str, Any]:
    """获取当前布局配置"""
    layout_path = os.path.join(_layouts_dir, f"{_current_layout}.json")
    try:
        with open(layout_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        logger.error("加载当前布局失败: %s", e)
        return DEFAULT_LAYOUTS["grid"].copy()

def create_platform_configuration(config: Dict[str, Any]) -> str:
    """
    创建平台配置文件
    
    Args:
        config: 平台配置
        
    Returns:
        str: 配置文件路径
    """
    config_id = config.get("id", f"platform_{int(time.time())}")
    config_path = os.path.join(_config_dir, f"{config_id}.json")
    
    # 确保配置包含必要的信息
    if "theme" not in config:
        config["theme"] = _current_theme
    
    if "layout" not in config:
        config["layout"] = _current_layout
    
    if "apps" not in config:
        config["apps"] = [app_id for app_id, app in _available_apps.items() 
                          if app.get("preinstalled", False)]
    
    if "created_at" not in config:
        config["created_at"] = time.time()
    
    if "updated_at" not in config:
        config["updated_at"] = time.time()
    
    try:
        with open(config_path, "w", encoding="utf-8") as f:
            json.dump(config, f, ensure_ascii=False, indent=2)
        
        logger.info("已创建平台配置: %s", config_id)
        return config_path
    except Exception as e:
        logger.error("创建平台配置失败: %s", e)
        return ""

def export_platform(platform_id: str, output_dir: str) -> Optional[str]:
    """
    导出平台配置和资源
    
    Args:
        platform_id: 平台ID
        output_dir: 输出目录
        
    Returns:
        Optional[str]: 成功时返回导出路径，失败时返回None
    """
    platform_config_path = os.path.join(_config_dir, f"{platform_id}.json")
    if not os.path.exists(platform_config_path):
        logger.error("平台配置不存在: %s", platform_id)
        return None
    
    export_time = int(time.time())
    export_dir = os.path.join(output_dir, f"{platform_id}_{export_time}")
    os.makedirs(export_dir, exist_ok=True)
    
    try:
        # 复制平台配置
        shutil.copy2(platform_config_path, os.path.join(export_dir, "platform.json"))
        
        # 读取平台配置
        with open(platform_config_path, "r", encoding="utf-8") as f:
            platform_config = json.load(f)
        
        # 复制主题
        theme_id = platform_config.get("theme", "default")
        theme_path = os.path.join(_themes_dir, f"{theme_id}.json")
        if os.path.exists(theme_path):
            shutil.copy2(theme_path, os.path.join(export_dir, "theme.json"))
        
        # 复制布局
        layout_id = platform_config.get("layout", "grid")
        layout_path = os.path.join(_layouts_dir, f"{layout_id}.json")
        if os.path.exists(layout_path):
            shutil.copy2(layout_path, os.path.join(export_dir, "layout.json"))
        
        # 导出应用信息
        apps = platform_config.get("apps", [])
        apps_info = {}
        for app_id in apps:
            if app_id in _available_apps:
                apps_info[app_id] = _available_apps[app_id]
        
        with open(os.path.join(export_dir, "apps.json"), "w", encoding="utf-8") as f:
            json.dump(apps_info, f, ensure_ascii=False, indent=2)
        
        # 创建导出清单
        manifest = {
            "platform_id": platform_id,
            "exported_at": export_time,
            "theme": theme_id,
            "layout": layout_id,
            "apps_count": len(apps),
            "export_version": "1.0"
        }
        
        with open(os.path.join(export_dir, "manifest.json"), "w", encoding="utf-8") as f:
            json.dump(manifest, f, ensure_ascii=False, indent=2)
        
        logger.info("平台导出成功: %s", export_dir)
        return export_dir
    except Exception as e:
        logger.error("平台导出失败: %s", e)
        return None

def get_builder_info() -> Dict[str, Any]:
    """获取构建工具状态信息"""
    return {
        "initialized": _initialized,
        "config_dir": _config_dir,
        "current_theme": _current_theme,
        "current_layout": _current_layout,
        "themes_count": len(os.listdir(_themes_dir)) if os.path.exists(_themes_dir) else 0,
        "layouts_count": len(os.listdir(_layouts_dir)) if os.path.exists(_layouts_dir) else 0,
        "available_apps_count": len(_available_apps)
    } 