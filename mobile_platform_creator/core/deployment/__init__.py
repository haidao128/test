"""
私有化部署模块
===========

提供平台私有化部署能力，支持专属域名生成和NAS设备绑定。

主要功能：
1. 专属域名生成与管理
2. NAS设备绑定与同步
3. 自动备份与恢复
4. 更新与维护
"""

import logging
import os
import json
import time
import socket
import uuid
import hashlib
import random
import string
import urllib.parse
from typing import Dict, List, Any, Optional, Tuple, Union, Set

logger = logging.getLogger("mobile_platform_creator.core.deployment")

# 全局变量
_initialized = False
_deployment_config_dir = ""
_domain_config = {}
_nas_config = {}
_backup_config = {}
_update_config = {}
_device_id = ""
_platform_domain = ""

# 默认配置
DEFAULT_DOMAIN_CONFIG = {
    "enabled": False,
    "custom_domain": "",
    "subdomain": "",
    "base_domain": "mobileplatform.app",
    "ssl_enabled": True,
    "dns_provider": "cloudflare",
    "last_renewed": 0,
    "expiry_date": 0
}

DEFAULT_NAS_CONFIG = {
    "enabled": False,
    "type": "none",  # "none", "synology", "qnap", "western_digital", "custom"
    "address": "",
    "port": 0,
    "username": "",
    "password_hash": "",
    "sync_enabled": False,
    "sync_interval": 3600,  # 1小时
    "last_sync": 0,
    "sync_folders": ["apps", "data", "config"]
}

DEFAULT_BACKUP_CONFIG = {
    "enabled": False,
    "auto_backup": False,
    "backup_interval": 86400,  # 1天
    "max_backups": 5,
    "include_user_data": True,
    "include_apps": True,
    "backup_location": "local",  # "local", "nas", "cloud"
    "cloud_provider": "none",
    "last_backup": 0
}

DEFAULT_UPDATE_CONFIG = {
    "auto_check": True,
    "auto_update": False,
    "update_channel": "stable",  # "stable", "beta", "dev"
    "last_check": 0,
    "current_version": "0.1.0",
    "available_version": "",
    "update_server": "https://update.mobileplatform.app"
}

def init(config_dir: Optional[str] = None) -> bool:
    """
    初始化私有化部署模块
    
    Args:
        config_dir: 配置文件目录，默认为None(自动选择)
        
    Returns:
        bool: 初始化是否成功
    """
    global _initialized, _deployment_config_dir, _device_id
    global _domain_config, _nas_config, _backup_config, _update_config
    
    if _initialized:
        logger.warning("私有化部署模块已初始化，请勿重复初始化")
        return True
    
    try:
        # 初始化配置目录
        if config_dir:
            _deployment_config_dir = config_dir
        else:
            # 自动选择配置目录
            from .. import is_android, is_ios
            
            if is_android():
                _deployment_config_dir = "/data/data/com.mobilecreator.app/files/deployment"
            elif is_ios():
                _deployment_config_dir = os.path.expanduser("~/Documents/MobilePlatformCreator/deployment")
            else:
                _deployment_config_dir = os.path.join(os.path.dirname(__file__), "..", "..", "..", "deployment")
        
        # 确保目录存在
        os.makedirs(_deployment_config_dir, exist_ok=True)
        logger.info("私有化部署配置目录: %s", _deployment_config_dir)
        
        # 加载或创建设备ID
        _load_or_create_device_id()
        
        # 加载配置
        _load_config()
        
        _initialized = True
        logger.info("私有化部署模块初始化成功，设备ID: %s", _device_id)
        return True
    except Exception as e:
        logger.error("私有化部署模块初始化失败: %s", e)
        return False

def _load_or_create_device_id() -> None:
    """加载或创建设备ID"""
    global _device_id
    
    device_id_file = os.path.join(_deployment_config_dir, "device_id.txt")
    
    if os.path.exists(device_id_file):
        # 读取已有设备ID
        try:
            with open(device_id_file, "r") as f:
                _device_id = f.read().strip()
            logger.debug("已加载设备ID: %s", _device_id)
        except Exception as e:
            logger.error("读取设备ID失败: %s", e)
            _generate_device_id(device_id_file)
    else:
        # 生成新设备ID
        _generate_device_id(device_id_file)

def _generate_device_id(device_id_file: str) -> None:
    """生成并保存设备ID"""
    global _device_id
    
    # 生成设备ID
    try:
        # 尝试使用MAC地址和主机名作为种子
        seed = f"{uuid.getnode()}-{socket.gethostname()}-{time.time()}"
    except:
        # 如果失败，使用随机种子
        seed = f"{random.randint(0, 1000000)}-{time.time()}"
    
    # 生成ID
    _device_id = hashlib.sha256(seed.encode()).hexdigest()[:16]
    
    # 保存ID
    try:
        with open(device_id_file, "w") as f:
            f.write(_device_id)
        logger.info("已生成并保存新设备ID: %s", _device_id)
    except Exception as e:
        logger.error("保存设备ID失败: %s", e)

def _load_config() -> None:
    """加载配置文件"""
    global _domain_config, _nas_config, _backup_config, _update_config
    
    # 加载域名配置
    domain_config_file = os.path.join(_deployment_config_dir, "domain.json")
    if os.path.exists(domain_config_file):
        try:
            with open(domain_config_file, "r", encoding="utf-8") as f:
                _domain_config = json.load(f)
        except Exception as e:
            logger.error("加载域名配置失败: %s", e)
            _domain_config = DEFAULT_DOMAIN_CONFIG.copy()
    else:
        _domain_config = DEFAULT_DOMAIN_CONFIG.copy()
        _save_config("domain", _domain_config)
    
    # 加载NAS配置
    nas_config_file = os.path.join(_deployment_config_dir, "nas.json")
    if os.path.exists(nas_config_file):
        try:
            with open(nas_config_file, "r", encoding="utf-8") as f:
                _nas_config = json.load(f)
        except Exception as e:
            logger.error("加载NAS配置失败: %s", e)
            _nas_config = DEFAULT_NAS_CONFIG.copy()
    else:
        _nas_config = DEFAULT_NAS_CONFIG.copy()
        _save_config("nas", _nas_config)
    
    # 加载备份配置
    backup_config_file = os.path.join(_deployment_config_dir, "backup.json")
    if os.path.exists(backup_config_file):
        try:
            with open(backup_config_file, "r", encoding="utf-8") as f:
                _backup_config = json.load(f)
        except Exception as e:
            logger.error("加载备份配置失败: %s", e)
            _backup_config = DEFAULT_BACKUP_CONFIG.copy()
    else:
        _backup_config = DEFAULT_BACKUP_CONFIG.copy()
        _save_config("backup", _backup_config)
    
    # 加载更新配置
    update_config_file = os.path.join(_deployment_config_dir, "update.json")
    if os.path.exists(update_config_file):
        try:
            with open(update_config_file, "r", encoding="utf-8") as f:
                _update_config = json.load(f)
        except Exception as e:
            logger.error("加载更新配置失败: %s", e)
            _update_config = DEFAULT_UPDATE_CONFIG.copy()
    else:
        _update_config = DEFAULT_UPDATE_CONFIG.copy()
        _save_config("update", _update_config)
    
    logger.debug("已加载配置文件")

def _save_config(config_type: str, config: Dict[str, Any]) -> bool:
    """
    保存配置到文件
    
    Args:
        config_type: 配置类型
        config: 配置内容
        
    Returns:
        bool: 保存是否成功
    """
    config_file = os.path.join(_deployment_config_dir, f"{config_type}.json")
    try:
        with open(config_file, "w", encoding="utf-8") as f:
            json.dump(config, f, ensure_ascii=False, indent=2)
        logger.debug("已保存配置: %s", config_type)
        return True
    except Exception as e:
        logger.error("保存配置失败 %s: %s", config_type, e)
        return False

def setup_custom_domain(domain: str, use_ssl: bool = True) -> bool:
    """
    设置自定义域名
    
    Args:
        domain: 自定义域名
        use_ssl: 是否启用SSL
        
    Returns:
        bool: 设置是否成功
    """
    global _domain_config, _platform_domain
    
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法设置域名")
        return False
    
    # 验证域名格式
    if not _is_valid_domain(domain):
        logger.error("无效的域名格式: %s", domain)
        return False
    
    # 更新配置
    _domain_config["enabled"] = True
    _domain_config["custom_domain"] = domain
    _domain_config["ssl_enabled"] = use_ssl
    _domain_config["last_renewed"] = int(time.time())
    _domain_config["expiry_date"] = int(time.time()) + 90 * 24 * 3600  # 90天有效期
    
    # 更新平台域名
    _platform_domain = domain
    
    # 保存配置
    if _save_config("domain", _domain_config):
        logger.info("已设置自定义域名: %s, SSL: %s", domain, "启用" if use_ssl else "禁用")
        return True
    else:
        return False

def generate_subdomain(name: Optional[str] = None) -> Optional[str]:
    """
    生成子域名
    
    Args:
        name: 自定义名称，默认使用随机生成
        
    Returns:
        Optional[str]: 成功时返回完整域名，失败时返回None
    """
    global _domain_config, _platform_domain
    
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法生成子域名")
        return None
    
    # 生成子域名
    if name:
        # 检查名称合法性
        if not _is_valid_subdomain(name):
            logger.error("无效的子域名格式: %s", name)
            return None
        subdomain = name.lower()
    else:
        # 随机生成子域名
        prefix = _generate_random_string(8)
        subdomain = f"{prefix}-{_device_id[:6]}"
    
    # 更新配置
    _domain_config["enabled"] = True
    _domain_config["custom_domain"] = ""
    _domain_config["subdomain"] = subdomain
    _domain_config["ssl_enabled"] = True
    _domain_config["last_renewed"] = int(time.time())
    _domain_config["expiry_date"] = int(time.time()) + 365 * 24 * 3600  # 365天有效期
    
    # 完整域名
    full_domain = f"{subdomain}.{_domain_config['base_domain']}"
    _platform_domain = full_domain
    
    # 保存配置
    if _save_config("domain", _domain_config):
        logger.info("已生成子域名: %s", full_domain)
        return full_domain
    else:
        return None

def _is_valid_domain(domain: str) -> bool:
    """验证域名格式是否有效"""
    import re
    pattern = r"^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$"
    return bool(re.match(pattern, domain))

def _is_valid_subdomain(subdomain: str) -> bool:
    """验证子域名格式是否有效"""
    import re
    pattern = r"^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$"
    return bool(re.match(pattern, subdomain))

def _generate_random_string(length: int) -> str:
    """生成随机字符串"""
    chars = string.ascii_lowercase + string.digits
    return ''.join(random.choice(chars) for _ in range(length))

def setup_nas_connection(
    nas_type: str,
    address: str,
    port: int,
    username: str,
    password: str,
    sync_enabled: bool = False,
    sync_interval: int = 3600
) -> bool:
    """
    设置NAS连接
    
    Args:
        nas_type: NAS类型 ("synology", "qnap", "western_digital", "custom")
        address: NAS地址
        port: NAS端口
        username: 用户名
        password: 密码
        sync_enabled: 是否启用同步
        sync_interval: 同步间隔(秒)
        
    Returns:
        bool: 设置是否成功
    """
    global _nas_config
    
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法设置NAS连接")
        return False
    
    # 验证NAS类型
    valid_types = ["synology", "qnap", "western_digital", "custom"]
    if nas_type not in valid_types:
        logger.error("不支持的NAS类型: %s", nas_type)
        return False
    
    # 验证地址
    if not address:
        logger.error("NAS地址不能为空")
        return False
    
    # 验证端口
    if port <= 0 or port > 65535:
        logger.error("无效的端口号: %d", port)
        return False
    
    # 计算密码哈希
    password_hash = hashlib.sha256(password.encode()).hexdigest()
    
    # 更新配置
    _nas_config["enabled"] = True
    _nas_config["type"] = nas_type
    _nas_config["address"] = address
    _nas_config["port"] = port
    _nas_config["username"] = username
    _nas_config["password_hash"] = password_hash
    _nas_config["sync_enabled"] = sync_enabled
    _nas_config["sync_interval"] = sync_interval
    _nas_config["last_sync"] = 0
    
    # 保存配置
    if _save_config("nas", _nas_config):
        logger.info("已设置NAS连接: %s://%s:%d, 用户: %s, 同步: %s", 
                   nas_type, address, port, username, "启用" if sync_enabled else "禁用")
        
        # 尝试测试连接
        if _test_nas_connection():
            logger.info("NAS连接测试成功")
            return True
        else:
            logger.warning("NAS连接设置已保存，但连接测试失败")
            return False
    else:
        return False

def _test_nas_connection() -> bool:
    """测试NAS连接是否成功"""
    # 在实际实现中，这里会根据不同的NAS类型使用不同的协议进行连接测试
    # 这里只是模拟测试过程
    logger.debug("测试NAS连接: %s://%s:%d", 
               _nas_config["type"], _nas_config["address"], _nas_config["port"])
    
    # 模拟连接成功
    return True

def sync_with_nas() -> bool:
    """
    与NAS同步数据
    
    Returns:
        bool: 同步是否成功
    """
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法同步NAS")
        return False
    
    if not _nas_config["enabled"] or not _nas_config["sync_enabled"]:
        logger.error("NAS同步未启用")
        return False
    
    # 在实际实现中，这里会根据配置执行同步操作
    logger.info("开始与NAS同步数据")
    
    # 更新最后同步时间
    _nas_config["last_sync"] = int(time.time())
    _save_config("nas", _nas_config)
    
    logger.info("NAS数据同步完成")
    return True

def setup_backup(
    enabled: bool,
    auto_backup: bool,
    backup_interval: int,
    max_backups: int,
    include_user_data: bool,
    include_apps: bool,
    backup_location: str
) -> bool:
    """
    设置备份配置
    
    Args:
        enabled: 是否启用备份
        auto_backup: 是否自动备份
        backup_interval: 备份间隔(秒)
        max_backups: 最大备份数量
        include_user_data: 是否包含用户数据
        include_apps: 是否包含应用
        backup_location: 备份位置 ("local", "nas", "cloud")
        
    Returns:
        bool: 设置是否成功
    """
    global _backup_config
    
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法设置备份")
        return False
    
    # 验证备份位置
    valid_locations = ["local", "nas", "cloud"]
    if backup_location not in valid_locations:
        logger.error("不支持的备份位置: %s", backup_location)
        return False
    
    # 如果选择NAS备份，检查NAS是否已配置
    if backup_location == "nas" and (not _nas_config["enabled"]):
        logger.error("选择NAS备份，但NAS未配置")
        return False
    
    # 更新配置
    _backup_config["enabled"] = enabled
    _backup_config["auto_backup"] = auto_backup
    _backup_config["backup_interval"] = backup_interval
    _backup_config["max_backups"] = max_backups
    _backup_config["include_user_data"] = include_user_data
    _backup_config["include_apps"] = include_apps
    _backup_config["backup_location"] = backup_location
    
    # 保存配置
    if _save_config("backup", _backup_config):
        logger.info("备份配置已更新: 启用: %s, 自动备份: %s, 位置: %s", 
                   "是" if enabled else "否",
                   "是" if auto_backup else "否",
                   backup_location)
        return True
    else:
        return False

def create_backup(backup_name: Optional[str] = None) -> Optional[str]:
    """
    创建备份
    
    Args:
        backup_name: 备份名称，默认使用时间戳
        
    Returns:
        Optional[str]: 成功时返回备份路径，失败时返回None
    """
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法创建备份")
        return None
    
    if not _backup_config["enabled"]:
        logger.error("备份功能未启用")
        return None
    
    # 生成备份名称
    timestamp = int(time.time())
    if not backup_name:
        backup_name = f"backup_{timestamp}"
    
    # 确定备份路径
    if _backup_config["backup_location"] == "local":
        backup_dir = os.path.join(_deployment_config_dir, "backups")
        os.makedirs(backup_dir, exist_ok=True)
        backup_path = os.path.join(backup_dir, f"{backup_name}.zip")
    elif _backup_config["backup_location"] == "nas":
        # 在实际实现中，这里会构建NAS上的路径
        backup_path = f"nas://{_nas_config['address']}/backups/{backup_name}.zip"
    else:  # cloud
        # 在实际实现中，这里会生成云存储路径
        backup_path = f"cloud://backups/{_device_id}/{backup_name}.zip"
    
    # 在实际实现中，这里会执行备份创建操作
    logger.info("正在创建备份: %s", backup_name)
    
    # 模拟备份过程
    time.sleep(1)
    
    # 更新最后备份时间
    _backup_config["last_backup"] = timestamp
    _save_config("backup", _backup_config)
    
    logger.info("备份创建成功: %s", backup_path)
    return backup_path

def restore_from_backup(backup_path: str) -> bool:
    """
    从备份恢复
    
    Args:
        backup_path: 备份文件路径
        
    Returns:
        bool: 恢复是否成功
    """
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法恢复备份")
        return False
    
    logger.info("开始从备份恢复: %s", backup_path)
    
    # 在实际实现中，这里会执行备份恢复操作
    # 模拟恢复过程
    time.sleep(2)
    
    logger.info("备份恢复完成")
    return True

def check_for_updates() -> Dict[str, Any]:
    """
    检查更新
    
    Returns:
        Dict[str, Any]: 更新信息
    """
    global _update_config
    
    if not _initialized:
        logger.error("私有化部署模块未初始化，无法检查更新")
        return {"status": "error", "message": "模块未初始化"}
    
    logger.info("正在检查更新...")
    
    # 在实际实现中，这里会连接更新服务器检查更新
    # 这里只是模拟更新检查过程
    current_version = _update_config["current_version"]
    
    # 模拟有新版本
    has_update = random.choice([True, False])
    
    if has_update:
        new_version = "0.2.0"
        _update_config["available_version"] = new_version
    else:
        new_version = current_version
        _update_config["available_version"] = ""
    
    # 更新最后检查时间
    _update_config["last_check"] = int(time.time())
    _save_config("update", _update_config)
    
    update_info = {
        "status": "success",
        "current_version": current_version,
        "latest_version": new_version,
        "has_update": has_update,
        "release_notes": "Bug修复和性能改进" if has_update else "",
        "download_url": f"https://update.mobileplatform.app/v{new_version}" if has_update else "",
        "update_size": "15MB" if has_update else ""
    }
    
    logger.info("更新检查完成: %s", "有可用更新" if has_update else "已是最新版本")
    return update_info

def get_platform_url() -> str:
    """
    获取平台访问URL
    
    Returns:
        str: 平台访问URL
    """
    if not _initialized:
        logger.warning("私有化部署模块未初始化，返回默认URL")
        return "http://localhost:8080"
    
    if _domain_config["enabled"]:
        if _domain_config["custom_domain"]:
            domain = _domain_config["custom_domain"]
        else:
            domain = f"{_domain_config['subdomain']}.{_domain_config['base_domain']}"
        
        protocol = "https" if _domain_config["ssl_enabled"] else "http"
        return f"{protocol}://{domain}"
    else:
        # 本地访问URL
        return "http://localhost:8080"

def get_deployment_info() -> Dict[str, Any]:
    """获取部署状态信息"""
    info = {
        "initialized": _initialized,
        "device_id": _device_id,
        "domain": {
            "enabled": _domain_config.get("enabled", False),
            "has_custom_domain": bool(_domain_config.get("custom_domain", "")),
            "has_subdomain": bool(_domain_config.get("subdomain", "")),
            "ssl_enabled": _domain_config.get("ssl_enabled", False),
        },
        "nas": {
            "enabled": _nas_config.get("enabled", False),
            "type": _nas_config.get("type", "none"),
            "sync_enabled": _nas_config.get("sync_enabled", False),
            "last_sync": _nas_config.get("last_sync", 0),
        },
        "backup": {
            "enabled": _backup_config.get("enabled", False),
            "auto_backup": _backup_config.get("auto_backup", False),
            "last_backup": _backup_config.get("last_backup", 0),
            "location": _backup_config.get("backup_location", "local"),
        },
        "update": {
            "auto_check": _update_config.get("auto_check", True),
            "auto_update": _update_config.get("auto_update", False),
            "current_version": _update_config.get("current_version", "0.1.0"),
            "has_update": bool(_update_config.get("available_version", "")),
            "last_check": _update_config.get("last_check", 0),
        },
        "platform_url": get_platform_url()
    }
    
    return info 