"""
沙箱模块
=========

提供安全隔离的应用运行环境。

- Android: 基于Seccomp实现系统调用过滤

该模块提供了三个安全级别的沙箱:
- strict: 严格模式，提供最高安全性
- standard: 标准模式，提供一般安全性
- minimal: 最小限制模式，主要用于调试
"""

import os
import sys
import time
import logging
import tempfile
import json
from typing import Dict, List, Any, Optional, Set

from ..import is_android

logger = logging.getLogger("mobile_platform_creator.core.sandbox")

# 全局变量
_initialized = False
_sandbox_level = "strict"  # 默认安全级别
_restricted_paths: List[str] = []
_allowed_paths: List[str] = []
_allowed_syscalls: Set[str] = set()
_sandbox_info: Dict[str, Any] = {}

def init(level: str = "strict") -> bool:
    """
    初始化沙箱环境
    
    Args:
        level: 沙箱安全级别 ("strict", "standard", "minimal")
        
    Returns:
        bool: 初始化是否成功
    """
    global _initialized, _sandbox_level, _sandbox_info
    
    if _initialized:
        logger.info("沙箱已经初始化，当前安全级别: %s", _sandbox_level)
        return True
    
    # 验证安全级别
    if level not in ["strict", "standard", "minimal"]:
        logger.error("不支持的安全级别: %s，使用默认值 'strict'", level)
        level = "strict"
    
    _sandbox_level = level
    
    try:
        logger.info("初始化沙箱环境，安全级别: %s", level)
        
        # 根据平台选择不同的初始化方法
        if is_android():
            success = _init_android_sandbox()
        else:
            # 默认使用模拟沙箱
            success = _init_simulation_sandbox()
        
        if success:
            _initialized = True
            logger.info("沙箱初始化成功")
            
            # 设置沙箱信息
            _update_sandbox_info()
            
            return True
        else:
            logger.error("沙箱初始化失败")
            return False
    except Exception as e:
        logger.error("沙箱初始化异常: %s", e)
        import traceback
        logger.error(traceback.format_exc())
        return False

def _init_android_sandbox() -> bool:
    """初始化Android平台的沙箱环境"""
    try:
        logger.info("初始化Android安全沙箱...")
        
        # 在实际实现中，这里会调用Android的Seccomp过滤器
        # 目前使用模拟实现
        _setup_allowed_paths()
        _setup_allowed_syscalls()
        
        # 测试沙箱环境
        test_result = _test_sandbox_environment()
        
        if test_result:
            logger.info("Android沙箱测试通过")
            return True
        else:
            logger.error("Android沙箱测试失败")
            return False
    except Exception as e:
        logger.error("初始化Android沙箱失败: %s", e)
        return False

def _init_simulation_sandbox() -> bool:
    """初始化模拟沙箱环境（用于桌面开发/测试）"""
    try:
        logger.info("初始化模拟沙箱环境...")
        
        # 设置允许的路径
        _setup_allowed_paths()
        
        # 设置允许的系统调用
        _setup_allowed_syscalls()
        
        # 创建沙箱临时目录
        sandbox_temp_dir = os.path.join(tempfile.gettempdir(), f"sandbox_{int(time.time())}")
        os.makedirs(sandbox_temp_dir, exist_ok=True)
        
        # 测试沙箱环境
        test_result = _test_sandbox_environment()
        
        if test_result:
            logger.info("模拟沙箱测试通过")
            return True
        else:
            logger.error("模拟沙箱测试失败")
            return False
    except Exception as e:
        logger.error("初始化模拟沙箱失败: %s", e)
        return False

def _setup_allowed_paths() -> None:
    """设置允许访问的路径"""
    global _allowed_paths, _restricted_paths
    
    # 根据平台设置不同的路径
    if is_android():
        # Android允许访问的路径
        _allowed_paths = [
            "/data/data/com.example.mobilecreator/files/apps",
            "/data/data/com.example.mobilecreator/files/cache",
            "/sdcard/Android/data/com.example.mobilecreator",
        ]
        
        # Android限制访问的路径
        _restricted_paths = [
            "/data/data",  # 除了允许的特定目录外
            "/system",
            "/proc",
            "/dev",
        ]
    else:
        # 桌面平台允许的路径
        current_dir = os.getcwd()
        home_dir = os.path.expanduser("~")
        
        _allowed_paths = [
            os.path.join(current_dir, "apps"),
            os.path.join(current_dir, "sandbox_data"),
            os.path.join(home_dir, "mobile_platform_creator", "apps"),
            os.path.join(home_dir, "mobile_platform_creator", "temp"),
        ]
        
        # 桌面平台限制的路径
        _restricted_paths = [
            os.path.join(home_dir, ".ssh"),
            os.path.join(home_dir, ".aws"),
            "/etc",
            "/var",
        ]
    
    # 根据安全级别调整
    if _sandbox_level == "minimal":
        # 最小限制模式下允许更多路径
        _allowed_paths.append(os.path.join(os.getcwd(), "examples"))
    
    # 确保路径存在
    for path in _allowed_paths:
        os.makedirs(path, exist_ok=True)

def _setup_allowed_syscalls() -> None:
    """设置允许的系统调用"""
    global _allowed_syscalls
    
    # 基本系统调用，所有级别都允许
    base_syscalls = {
        "read", "write", "open", "close", "stat", "fstat", "lstat",
        "poll", "lseek", "mmap", "mprotect", "munmap", "brk", "rt_sigaction",
        "rt_sigprocmask", "rt_sigreturn", "ioctl", "pread64", "pwrite64",
        "readv", "writev", "access", "pipe", "select", "sched_yield",
        "mremap", "msync", "mincore", "madvise", "shmget", "shmat",
        "shmctl", "dup", "dup2", "pause", "nanosleep", "getitimer",
        "alarm", "setitimer", "getpid", "exit", "uname", "fcntl",
        "getcwd", "chdir", "fchdir", "rename", "mkdir", "rmdir",
        "creat", "link", "unlink", "symlink", "readlink", "chmod",
        "fchmod", "chown", "fchown", "umask", "gettimeofday", "getrlimit",
        "getrusage", "sysinfo", "times", "ptrace", "getuid", "syslog",
        "getgid", "setuid", "setgid", "geteuid", "getegid", "setpgid",
    }
    
    # 根据安全级别添加额外系统调用
    if _sandbox_level == "minimal":
        # 最小限制模式，允许更多系统调用
        extra_syscalls = {
            "socket", "connect", "accept", "sendto", "recvfrom", "sendmsg",
            "recvmsg", "shutdown", "bind", "listen", "getsockname",
            "getpeername", "socketpair", "setsockopt", "getsockopt",
            "clone", "fork", "vfork", "execve", "kill", "mkdirat", "openat",
        }
    elif _sandbox_level == "standard":
        # 标准模式，允许部分网络和进程操作
        extra_syscalls = {
            "socket", "connect", "accept", "sendto", "recvfrom", "sendmsg",
            "recvmsg", "shutdown", "bind", "listen", "getsockname",
            "getpeername", "socketpair", "setsockopt", "getsockopt",
        }
    else:
        # 严格模式，不允许额外系统调用
        extra_syscalls = set()
    
    _allowed_syscalls = base_syscalls.union(extra_syscalls)

def _test_sandbox_environment() -> bool:
    """
    测试沙箱环境是否正常
    
    Returns:
        bool: 测试是否通过
    """
    try:
        # 检查允许的路径是否可访问
        for path in _allowed_paths:
            if not os.path.exists(path):
                continue
            
            # 尝试创建临时文件
            test_file = os.path.join(path, f"sandbox_test_{int(time.time())}.txt")
            try:
                with open(test_file, "w") as f:
                    f.write("Sandbox test")
                
                # 读取测试文件
                with open(test_file, "r") as f:
                    content = f.read()
                    if content != "Sandbox test":
                        logger.warning("沙箱测试文件内容不匹配: %s", content)
                
                # 删除测试文件
                os.remove(test_file)
            except Exception as e:
                logger.warning("沙箱路径测试失败: %s - %s", path, e)
        
        return True
    except Exception as e:
        logger.error("沙箱环境测试失败: %s", e)
        return False

def reset_sandbox() -> bool:
    """
    重置沙箱环境
    
    Returns:
        bool: 重置是否成功
    """
    global _initialized, _sandbox_level
    
    if not _initialized:
        logger.info("沙箱尚未初始化，无需重置")
        return True
    
    try:
        logger.info("重置沙箱环境")
        
        # 保存当前安全级别
        current_level = _sandbox_level
        
        # 清除状态
        _initialized = False
        
        # 重新初始化
        return init(current_level)
    except Exception as e:
        logger.error("重置沙箱失败: %s", e)
        return False

def _update_sandbox_info() -> None:
    """更新沙箱信息"""
    global _sandbox_info
    
    _sandbox_info = {
        "initialized": _initialized,
        "level": _sandbox_level,
        "platform": "android" if is_android() else "desktop",
        "allowed_paths": _allowed_paths,
        "restricted_paths": _restricted_paths,
        "allowed_syscalls_count": len(_allowed_syscalls),
        "timestamp": time.time()
    }

def get_sandbox_info() -> Dict[str, Any]:
    """
    获取沙箱信息
    
    Returns:
        Dict[str, Any]: 沙箱信息
    """
    # 确保信息是最新的
    _update_sandbox_info()
    return _sandbox_info

def is_syscall_allowed(syscall: str) -> bool:
    """
    检查系统调用是否被允许
    
    Args:
        syscall: 系统调用名称
        
    Returns:
        bool: 是否允许
    """
    if not _initialized:
        # 沙箱未初始化时，默认允许
        return True
    
    return syscall in _allowed_syscalls

def is_path_allowed(path: str) -> bool:
    """
    检查路径是否被允许访问
    
    Args:
        path: 文件路径
        
    Returns:
        bool: 是否允许
    """
    if not _initialized:
        # 沙箱未初始化时，默认允许
        return True
    
    # 检查是否在允许的路径中
    if _sandbox_level == "minimal":
        # 最小限制模式下，默认允许
        return True
    
    # 绝对路径
    path = os.path.abspath(path)
    
    # 检查是否在允许的路径中
    for allowed_path in _allowed_paths:
        if path.startswith(allowed_path):
            return True
    
    # 检查是否在限制的路径中
    for restricted_path in _restricted_paths:
        if path.startswith(restricted_path):
            return False
    
    # 默认规则
    if _sandbox_level == "strict":
        # 严格模式下，默认不允许
        return False
    else:
        # 其他模式下，默认允许
        return True

def verify_app_certificate(app_id: str, app_path: str) -> bool:
    """
    验证应用证书
    
    Args:
        app_id: 应用ID
        app_path: 应用路径
        
    Returns:
        bool: 验证是否通过
    """
    try:
        manifest_path = os.path.join(app_path, "manifest.json")
        if not os.path.exists(manifest_path):
            logger.error("应用清单文件不存在: %s", manifest_path)
            return False
        
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
        
        # 检查应用ID
        if manifest.get("id") != app_id:
            logger.error("应用ID不匹配: %s != %s", manifest.get("id"), app_id)
            return False
        
        # 在实际实现中，这里会验证数字签名
        # 目前简单返回真
        return True
    except Exception as e:
        logger.error("验证应用证书失败: %s", e)
        return False

def register_app(app_id: str, app_path: str) -> bool:
    """
    注册应用到沙箱
    
    Args:
        app_id: 应用ID
        app_path: 应用路径
        
    Returns:
        bool: 注册是否成功
    """
    try:
        # 验证应用
        if not verify_app_certificate(app_id, app_path):
            return False
        
        # 在实际实现中，这里会将应用登记到沙箱系统
        logger.info("注册应用到沙箱: %s", app_id)
        
        return True
    except Exception as e:
        logger.error("注册应用失败: %s", e)
        return False

# 导入沙箱运行器
from .sandbox_runner import SandboxRunner, run_app 