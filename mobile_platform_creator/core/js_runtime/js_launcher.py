"""
JavaScript 应用启动器
=================

负责加载和运行MPK包中的JavaScript应用，管理应用生命周期。

功能：
1. 加载MPK包并解析
2. 初始化JavaScript运行时
3. 设置API和权限
4. 执行应用入口点
5. 管理应用生命周期
"""

import os
import sys
import json
import logging
import tempfile
import shutil
import threading
import time
import traceback
from typing import Dict, List, Any, Optional, Callable, Union, Set, Tuple

# 导入相关模块
from . import JSRuntime, get_runtime, dispose_runtime
from .js_engine import create_engine
from .js_api import JSAPIs

# 导入MPK包处理模块
from ...utils.mpk_package import MPKPackage, MPKError

logger = logging.getLogger("mobile_platform_creator.core.js_runtime.js_launcher")

class JSAppLauncher:
    """JavaScript应用启动器"""
    
    def __init__(self):
        """初始化启动器"""
        # 已加载的应用
        self.loaded_apps = {}
        
        # 应用状态
        self.app_states = {}
        
        # 应用工作目录
        self.work_dirs = {}
        
        # JavaScript运行时实例
        self.runtimes = {}
        
        # 启动锁
        self.launch_lock = threading.RLock()
    
    def load_app(self, mpk_path: str, app_id: Optional[str] = None) -> str:
        """
        加载JavaScript应用
        
        Args:
            mpk_path: MPK文件路径
            app_id: 应用ID，默认为None（从MPK包获取）
            
        Returns:
            str: 应用ID
            
        Raises:
            MPKError: 如果MPK包加载失败
            ValueError: 如果应用已加载或参数无效
        """
        with self.launch_lock:
            try:
                # 加载MPK包
                mpk = MPKPackage(mpk_path)
                
                # 获取清单数据
                manifest = mpk.get_manifest()
                
                # 获取应用ID
                if not app_id:
                    app_id = manifest.get("id")
                    if not app_id:
                        raise ValueError("MPK包清单未指定应用ID")
                
                # 检查应用是否已加载
                if app_id in self.loaded_apps:
                    raise ValueError(f"应用已加载: {app_id}")
                
                # 检查代码类型
                code_type = manifest.get("code_type", "")
                if code_type != "javascript":
                    raise ValueError(f"不支持的代码类型: {code_type}")
                
                # 创建工作目录
                work_dir = tempfile.mkdtemp(prefix=f"js_app_{app_id}_")
                
                # 提取MPK内容到工作目录
                try:
                    # 提取代码目录
                    code_dir = os.path.join(work_dir, "code")
                    os.makedirs(code_dir, exist_ok=True)
                    mpk.extract_directory("code", code_dir)
                    
                    # 提取资源目录
                    assets_dir = os.path.join(work_dir, "assets")
                    os.makedirs(assets_dir, exist_ok=True)
                    if "assets" in mpk.list_files():
                        mpk.extract_directory("assets", assets_dir)
                    
                    # 提取配置目录
                    config_dir = os.path.join(work_dir, "config")
                    os.makedirs(config_dir, exist_ok=True)
                    if "config" in mpk.list_files():
                        mpk.extract_directory("config", config_dir)
                    
                    # 保存清单文件
                    with open(os.path.join(work_dir, "manifest.json"), "w", encoding="utf-8") as f:
                        json.dump(manifest, f, ensure_ascii=False, indent=2)
                except Exception as e:
                    # 清理工作目录
                    shutil.rmtree(work_dir, ignore_errors=True)
                    raise MPKError(f"提取MPK内容失败: {e}")
                
                # 更新状态
                self.loaded_apps[app_id] = {
                    "manifest": manifest,
                    "mpk_path": mpk_path
                }
                self.app_states[app_id] = "loaded"
                self.work_dirs[app_id] = work_dir
                
                logger.info(f"应用加载成功: {app_id}")
                return app_id
            except Exception as e:
                logger.error(f"加载应用失败: {mpk_path} - {e}")
                logger.error(traceback.format_exc())
                raise
    
    def start_app(self, app_id: str, engine_type: str = "auto") -> bool:
        """
        启动JavaScript应用
        
        Args:
            app_id: 应用ID
            engine_type: JavaScript引擎类型
            
        Returns:
            bool: 是否成功启动
            
        Raises:
            ValueError: 如果应用未加载或已启动
        """
        with self.launch_lock:
            # 检查应用是否已加载
            if app_id not in self.loaded_apps:
                raise ValueError(f"应用未加载: {app_id}")
            
            # 检查应用状态
            if self.app_states.get(app_id) == "running":
                logger.warning(f"应用已在运行: {app_id}")
                return True
            
            try:
                # 获取应用信息
                app_info = self.loaded_apps[app_id]
                work_dir = self.work_dirs[app_id]
                manifest = app_info["manifest"]
                
                # 获取入口点
                entry_point = manifest.get("entry_point", "")
                if not entry_point:
                    raise ValueError(f"应用未指定入口点: {app_id}")
                
                # 创建JavaScript运行时
                runtime = get_runtime(app_id, work_dir, engine_type)
                
                # 创建JavaScript引擎
                js_engine = create_engine(engine_type)
                
                # 设置全局变量
                js_engine.set_global("APP_ID", app_id)
                js_engine.set_global("APP_PATH", work_dir)
                
                # 创建沙箱环境
                sandbox_dir = os.path.join(work_dir, "sandbox")
                os.makedirs(sandbox_dir, exist_ok=True)
                
                # 注册API
                apis = JSAPIs(runtime, sandbox_dir)
                apis.register_all(js_engine)
                
                # 导入通用工具库
                self._import_utils(js_engine, work_dir)
                
                # 执行入口点
                main_script_path = os.path.join(work_dir, "code", entry_point)
                if not os.path.exists(main_script_path):
                    raise ValueError(f"入口点文件不存在: {main_script_path}")
                
                with open(main_script_path, "r", encoding="utf-8") as f:
                    script = f.read()
                
                # 执行脚本
                js_engine.eval(script, entry_point)
                
                # 保存运行时实例
                self.runtimes[app_id] = js_engine
                
                # 更新状态
                self.app_states[app_id] = "running"
                
                logger.info(f"应用启动成功: {app_id}")
                return True
            except Exception as e:
                logger.error(f"启动应用失败: {app_id} - {e}")
                logger.error(traceback.format_exc())
                
                # 恢复状态
                self.app_states[app_id] = "loaded"
                
                # 清理资源
                if app_id in self.runtimes:
                    del self.runtimes[app_id]
                
                return False
    
    def _import_utils(self, js_engine, work_dir: str) -> None:
        """
        导入通用工具库
        
        Args:
            js_engine: JavaScript引擎
            work_dir: 工作目录
        """
        # 检查工具库是否存在
        utils_dir = os.path.join(work_dir, "code", "utils")
        if os.path.exists(utils_dir) and os.path.isdir(utils_dir):
            # 加载工具库脚本
            for filename in os.listdir(utils_dir):
                if filename.endswith(".js"):
                    file_path = os.path.join(utils_dir, filename)
                    try:
                        with open(file_path, "r", encoding="utf-8") as f:
                            script = f.read()
                        js_engine.eval(script, f"utils/{filename}")
                    except Exception as e:
                        logger.error(f"加载工具库失败: {filename} - {e}")
    
    def stop_app(self, app_id: str) -> bool:
        """
        停止JavaScript应用
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 是否成功停止
            
        Raises:
            ValueError: 如果应用未加载
        """
        with self.launch_lock:
            # 检查应用是否已加载
            if app_id not in self.loaded_apps:
                raise ValueError(f"应用未加载: {app_id}")
            
            # 检查应用状态
            if self.app_states.get(app_id) != "running":
                logger.warning(f"应用未在运行: {app_id}")
                return True
            
            try:
                # 释放运行时资源
                if app_id in self.runtimes:
                    dispose_runtime(app_id)
                    del self.runtimes[app_id]
                
                # 更新状态
                self.app_states[app_id] = "loaded"
                
                logger.info(f"应用停止成功: {app_id}")
                return True
            except Exception as e:
                logger.error(f"停止应用失败: {app_id} - {e}")
                return False
    
    def unload_app(self, app_id: str) -> bool:
        """
        卸载JavaScript应用
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 是否成功卸载
            
        Raises:
            ValueError: 如果应用未加载
        """
        with self.launch_lock:
            # 检查应用是否已加载
            if app_id not in self.loaded_apps:
                raise ValueError(f"应用未加载: {app_id}")
            
            # 如果应用在运行，先停止
            if self.app_states.get(app_id) == "running":
                self.stop_app(app_id)
            
            try:
                # 清理工作目录
                if app_id in self.work_dirs:
                    work_dir = self.work_dirs[app_id]
                    shutil.rmtree(work_dir, ignore_errors=True)
                    del self.work_dirs[app_id]
                
                # 移除应用数据
                if app_id in self.loaded_apps:
                    del self.loaded_apps[app_id]
                
                # 移除应用状态
                if app_id in self.app_states:
                    del self.app_states[app_id]
                
                logger.info(f"应用卸载成功: {app_id}")
                return True
            except Exception as e:
                logger.error(f"卸载应用失败: {app_id} - {e}")
                return False
    
    def is_app_running(self, app_id: str) -> bool:
        """
        检查应用是否在运行
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 是否在运行
        """
        return self.app_states.get(app_id) == "running"
    
    def is_app_loaded(self, app_id: str) -> bool:
        """
        检查应用是否已加载
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 是否已加载
        """
        return app_id in self.loaded_apps
    
    def get_app_info(self, app_id: str) -> Optional[Dict[str, Any]]:
        """
        获取应用信息
        
        Args:
            app_id: 应用ID
            
        Returns:
            Optional[Dict[str, Any]]: 应用信息
        """
        if app_id not in self.loaded_apps:
            return None
        
        app_info = self.loaded_apps[app_id].copy()
        app_info["state"] = self.app_states.get(app_id, "unknown")
        app_info["work_dir"] = self.work_dirs.get(app_id)
        
        return app_info
    
    def get_loaded_apps(self) -> List[str]:
        """
        获取已加载的应用列表
        
        Returns:
            List[str]: 应用ID列表
        """
        return list(self.loaded_apps.keys())
    
    def get_running_apps(self) -> List[str]:
        """
        获取正在运行的应用列表
        
        Returns:
            List[str]: 应用ID列表
        """
        return [app_id for app_id, state in self.app_states.items() if state == "running"]
    
    def call_app_function(self, app_id: str, function_name: str, *args) -> Any:
        """
        调用应用中的函数
        
        Args:
            app_id: 应用ID
            function_name: 函数名
            *args: 参数
            
        Returns:
            Any: 函数返回值
            
        Raises:
            ValueError: 如果应用未加载或未运行
        """
        # 检查应用是否已加载
        if app_id not in self.loaded_apps:
            raise ValueError(f"应用未加载: {app_id}")
        
        # 检查应用状态
        if self.app_states.get(app_id) != "running":
            raise ValueError(f"应用未在运行: {app_id}")
        
        # 检查运行时是否存在
        if app_id not in self.runtimes:
            raise ValueError(f"应用运行时不存在: {app_id}")
        
        try:
            # 调用函数
            runtime = self.runtimes[app_id]
            return runtime.call_function(function_name, *args)
        except Exception as e:
            logger.error(f"调用应用函数失败: {app_id}.{function_name} - {e}")
            raise
    
    def cleanup(self):
        """清理所有资源"""
        with self.launch_lock:
            # 停止所有运行中的应用
            for app_id in list(self.app_states.keys()):
                if self.app_states.get(app_id) == "running":
                    try:
                        self.stop_app(app_id)
                    except:
                        pass
            
            # 清理所有工作目录
            for app_id, work_dir in list(self.work_dirs.items()):
                try:
                    shutil.rmtree(work_dir, ignore_errors=True)
                except:
                    pass
            
            # 清空数据
            self.loaded_apps.clear()
            self.app_states.clear()
            self.work_dirs.clear()
            self.runtimes.clear()


# 全局单例实例
_launcher_instance = None

def get_launcher() -> JSAppLauncher:
    """
    获取JavaScript应用启动器实例
    
    Returns:
        JSAppLauncher: 启动器实例
    """
    global _launcher_instance
    if _launcher_instance is None:
        _launcher_instance = JSAppLauncher()
    return _launcher_instance


# 方便的全局函数
def load_app(mpk_path: str, app_id: Optional[str] = None) -> str:
    """
    加载JavaScript应用
    
    Args:
        mpk_path: MPK文件路径
        app_id: 应用ID
        
    Returns:
        str: 应用ID
    """
    return get_launcher().load_app(mpk_path, app_id)

def start_app(app_id: str, engine_type: str = "auto") -> bool:
    """
    启动JavaScript应用
    
    Args:
        app_id: 应用ID
        engine_type: JavaScript引擎类型
        
    Returns:
        bool: 是否成功启动
    """
    return get_launcher().start_app(app_id, engine_type)

def stop_app(app_id: str) -> bool:
    """
    停止JavaScript应用
    
    Args:
        app_id: 应用ID
        
    Returns:
        bool: 是否成功停止
    """
    return get_launcher().stop_app(app_id)

def unload_app(app_id: str) -> bool:
    """
    卸载JavaScript应用
    
    Args:
        app_id: 应用ID
        
    Returns:
        bool: 是否成功卸载
    """
    return get_launcher().unload_app(app_id)

def is_app_running(app_id: str) -> bool:
    """
    检查应用是否在运行
    
    Args:
        app_id: 应用ID
        
    Returns:
        bool: 是否在运行
    """
    return get_launcher().is_app_running(app_id)

def get_app_info(app_id: str) -> Optional[Dict[str, Any]]:
    """
    获取应用信息
    
    Args:
        app_id: 应用ID
        
    Returns:
        Optional[Dict[str, Any]]: 应用信息
    """
    return get_launcher().get_app_info(app_id)

def call_app_function(app_id: str, function_name: str, *args) -> Any:
    """
    调用应用中的函数
    
    Args:
        app_id: 应用ID
        function_name: 函数名
        *args: 参数
        
    Returns:
        Any: 函数返回值
    """
    return get_launcher().call_app_function(app_id, function_name, *args) 