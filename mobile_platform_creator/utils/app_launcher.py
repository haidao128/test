"""
应用启动器模块
===========

负责启动和管理应用程序的运行，提供运行时环境和资源管理。

主要功能：
1. 应用程序启动和停止
2. 运行时环境管理
3. 资源监控和限制
4. 错误处理和恢复
"""

import os
import sys
import json
import logging
import threading
import multiprocessing
from typing import Dict, List, Any, Optional, Tuple, Callable

from ..core.sandbox.sandbox_runner import SandboxRunner
from .mpk_package import MPKPackage, is_valid_mpk

logger = logging.getLogger("mobile_platform_creator.utils.app_launcher")

class AppLauncher:
    """应用启动器"""
    
    def __init__(self, sandbox_level: str = "strict"):
        """
        初始化应用启动器
        
        Args:
            sandbox_level: 沙箱安全级别 ("strict", "standard", "minimal")
        """
        self.sandbox_level = sandbox_level
        self.running_apps: Dict[str, Dict[str, Any]] = {}
        self._lock = threading.Lock()
    
    def launch(self, app_id: str, app_path: str, 
               args: Optional[List[str]] = None,
               env: Optional[Dict[str, str]] = None,
               on_exit: Optional[Callable[[str, int], None]] = None) -> bool:
        """
        启动应用程序
        
        Args:
            app_id: 应用ID
            app_path: 应用程序路径
            args: 可选的命令行参数
            env: 可选的环境变量
            on_exit: 可选的退出回调函数
            
        Returns:
            bool: 启动是否成功
        """
        logger.info(f"启动应用: {app_id}")
        
        try:
            # 检查应用是否已经在运行
            with self._lock:
                if app_id in self.running_apps:
                    logger.warning(f"应用已在运行: {app_id}")
                    return False
            
            # 验证应用路径
            if not os.path.exists(app_path):
                logger.error(f"应用路径不存在: {app_path}")
                return False
            
            # 创建沙箱运行器
            runner = SandboxRunner(app_id, app_path, self.sandbox_level)
            
            # 设置环境变量
            if env:
                runner.set_env(env)
            
            # 启动应用
            success = runner.start(args)
            
            if not success:
                logger.error(f"启动应用失败: {app_id}")
                return False
            
            # 记录运行中的应用
            with self._lock:
                self.running_apps[app_id] = {
                    "runner": runner,
                    "start_time": runner.start_time,
                    "pid": runner.process.pid if runner.process else None,
                }
            
            # 启动监控线程
            def monitor():
                try:
                    # 等待应用退出
                    exit_code = runner.wait()
                    
                    # 清理记录
                    with self._lock:
                        if app_id in self.running_apps:
                            del self.running_apps[app_id]
                    
                    # 调用退出回调
                    if on_exit:
                        on_exit(app_id, exit_code)
                        
                except Exception as e:
                    logger.error(f"应用监控失败: {e}")
            
            thread = threading.Thread(target=monitor, daemon=True)
            thread.start()
            
            logger.info(f"应用启动成功: {app_id}")
            return True
            
        except Exception as e:
            logger.error(f"启动应用失败: {e}")
            return False
    
    def stop(self, app_id: str, force: bool = False) -> bool:
        """
        停止应用程序
        
        Args:
            app_id: 应用ID
            force: 是否强制停止
            
        Returns:
            bool: 停止是否成功
        """
        logger.info(f"停止应用: {app_id}")
        
        try:
            # 获取运行中的应用
            with self._lock:
                if app_id not in self.running_apps:
                    logger.warning(f"应用未在运行: {app_id}")
                    return False
                
                app_info = self.running_apps[app_id]
            
            # 停止应用
            runner = app_info["runner"]
            if force:
                success = runner.terminate()
            else:
                success = runner.stop()
            
            if success:
                logger.info(f"应用停止成功: {app_id}")
            else:
                logger.error(f"应用停止失败: {app_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"停止应用失败: {e}")
            return False
    
    def list_running(self) -> List[Dict[str, Any]]:
        """
        列出所有运行中的应用
        
        Returns:
            List[Dict[str, Any]]: 运行中的应用列表
        """
        with self._lock:
            return [{
                "app_id": app_id,
                "start_time": info["start_time"],
                "pid": info["pid"],
                "sandbox_level": info["runner"].sandbox_level,
            } for app_id, info in self.running_apps.items()]
    
    def get_status(self, app_id: str) -> Optional[Dict[str, Any]]:
        """
        获取应用状态
        
        Args:
            app_id: 应用ID
            
        Returns:
            Optional[Dict[str, Any]]: 应用状态信息
        """
        with self._lock:
            if app_id not in self.running_apps:
                return None
            
            info = self.running_apps[app_id]
            runner = info["runner"]
            
            return {
                "app_id": app_id,
                "start_time": info["start_time"],
                "pid": info["pid"],
                "sandbox_level": runner.sandbox_level,
                "resources": runner.resources_used,
                "running": runner.running,
            } 