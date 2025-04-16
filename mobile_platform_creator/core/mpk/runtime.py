"""
MPK运行时环境

提供应用运行所需的基础设施
"""

import os
import sys
import json
import logging
import tempfile
import threading
import traceback
import shutil
import subprocess
from typing import Dict, Any, Optional, Callable, List, Union
from . import MPKFile, MPKLoader, MPKViewer, is_compatible_with_android
from ..sandbox import Sandbox
from datetime import datetime

class Runtime:
    # 单例实例
    _instance = None

    def __init__(self):
        if Runtime._instance is not None:
            raise Exception("This class is a singleton!")
        else:
            Runtime._instance = self

        self.sandbox = Sandbox()
        self.loaders: Dict[str, MPKLoader] = {}
        self.logger = self._setup_logger()

        # 定义安装目录和注册表路径 (使用相对路径，基于 workspace root)
        self.install_dir = "mpk_installed"
        self.registry_path = os.path.join(self.install_dir, "registry.json")
        self._ensure_install_dir()
        self.registry = self._load_registry()
        
        # Android相关目录
        self.android_install_dir = os.path.join(self.install_dir, "android")
        self.android_registry_path = os.path.join(self.android_install_dir, "registry.json")
        self._ensure_android_dirs()
        self.android_registry = self._load_android_registry()

        self.event_handlers: Dict[str, list] = {
            "start": [],
            "stop": [],
            "error": [],
            "resource": [],
            "install": [],
            "uninstall": []
        }
        
    def _is_android(self) -> bool:
        """检查是否在Android环境下运行"""
        return os.path.exists("/system/bin/adb") or \
               os.path.exists("/system/bin/am") or \
               "ANDROID_ROOT" in os.environ or \
               "ANDROID_DATA" in os.environ or \
               hasattr(sys, 'getandroidapilevel')
               
    def _ensure_android_dirs(self) -> None:
        """确保Android相关目录存在"""
        os.makedirs(self.android_install_dir, exist_ok=True)
        
    def _load_android_registry(self) -> Dict[str, Dict]:
        """加载Android安装注册表"""
        try:
            if os.path.exists(self.android_registry_path):
                with open(self.android_registry_path, "r", encoding="utf-8") as f:
                    return json.load(f)
            else:
                return {}
        except (json.JSONDecodeError, IOError) as e:
            self.logger.error(f"加载Android注册表失败: {e}. 将使用空注册表.")
            return {}
            
    def _save_android_registry(self) -> None:
        """保存Android安装注册表"""
        try:
            with open(self.android_registry_path, "w", encoding="utf-8") as f:
                json.dump(self.android_registry, f, ensure_ascii=False, indent=2)
        except IOError as e:
            self.logger.error(f"保存Android注册表失败: {e}")
        
    def _setup_logger(self) -> logging.Logger:
        """设置日志记录器"""
        logger = logging.getLogger("mpk_runtime")
        logger.setLevel(logging.INFO)
        
        # 控制台处理器
        console_handler = logging.StreamHandler()
        console_handler.setLevel(logging.INFO)
        console_formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        )
        console_handler.setFormatter(console_formatter)
        logger.addHandler(console_handler)
        
        # 文件处理器
        log_dir = os.path.join(tempfile.gettempdir(), "mpk_logs")
        os.makedirs(log_dir, exist_ok=True)
        file_handler = logging.FileHandler(
            os.path.join(log_dir, "runtime.log"),
            encoding="utf-8"
        )
        file_handler.setLevel(logging.DEBUG)
        file_formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        )
        file_handler.setFormatter(file_formatter)
        logger.addHandler(file_handler)
        
        return logger
        
    def register_event_handler(self, event: str, handler: Callable) -> None:
        """注册事件处理器"""
        if event in self.event_handlers:
            self.event_handlers[event].append(handler)
            
    def unregister_event_handler(self, event: str, handler: Callable) -> None:
        """注销事件处理器"""
        if event in self.event_handlers and handler in self.event_handlers[event]:
            self.event_handlers[event].remove(handler)
            
    def _trigger_event(self, event: str, *args, **kwargs) -> None:
        """触发事件"""
        if event in self.event_handlers:
            for handler in self.event_handlers[event]:
                try:
                    handler(*args, **kwargs)
                except Exception as e:
                    self.logger.error(f"事件处理器执行失败: {e}")
                    
    def _ensure_install_dir(self):
        """确保安装目录存在"""
        os.makedirs(self.install_dir, exist_ok=True)

    def _load_registry(self) -> Dict[str, Dict]:
        """加载安装注册表"""
        try:
            if os.path.exists(self.registry_path):
                with open(self.registry_path, "r", encoding="utf-8") as f:
                    return json.load(f)
            else:
                return {}
        except (json.JSONDecodeError, IOError) as e:
            self.logger.error(f"加载注册表失败: {e}. 将使用空注册表.")
            return {}

    def _save_registry(self) -> None:
        """保存安装注册表"""
        try:
            with open(self.registry_path, "w", encoding="utf-8") as f:
                json.dump(self.registry, f, ensure_ascii=False, indent=2)
        except IOError as e:
            self.logger.error(f"保存注册表失败: {e}")

    def install_app(self, mpk_source_path: str) -> str:
        """从指定路径安装MPK应用"""
        self.logger.info(f"开始安装应用: {mpk_source_path}")
        if not os.path.exists(mpk_source_path):
             self.logger.error(f"安装失败: 文件未找到 {mpk_source_path}")
             raise FileNotFoundError(f"MPK file not found: {mpk_source_path}")

        try:
            # 1. 加载并验证 MPK 文件
            mpk_file = MPKFile.load(mpk_source_path)
            # 基本验证 (魔数, 版本, 校验和) 已在 load 中完成
            if not mpk_file.verify(): # 签名验证 (占位符)
                self.logger.warning(f"签名验证跳过或失败 (占位符): {mpk_source_path}")
                # 根据策略决定是否继续，这里我们暂时继续

            # 2. 生成 App ID
            metadata = mpk_file.metadata
            if not metadata or not metadata.package_name or not metadata.version:
                raise ValueError("MPK 文件元数据不完整 (缺少 package_name 或 version)")
            app_id = f"{metadata.package_name}_{metadata.version}"
            self.logger.debug(f"生成 App ID: {app_id}")

            # 3. 检查是否已安装
            if app_id in self.registry:
                self.logger.error(f"安装失败: 应用 '{app_id}' 已安装.")
                raise FileExistsError(f"App '{app_id}' is already installed.")

            # 4. 定义目标路径并复制文件
            target_mpk_name = f"{app_id}.mpk"
            target_mpk_path = os.path.join(self.install_dir, target_mpk_name)
            self.logger.debug(f"复制文件到: {target_mpk_path}")
            shutil.copy2(mpk_source_path, target_mpk_path) # copy2 保留元数据

            # 5. 更新注册表
            self.registry[app_id] = {
                "path": target_mpk_path, # 存储相对或绝对路径皆可，这里存相对安装目录的路径
                "install_date": datetime.now().isoformat(),
                "app_name": metadata.app_name,
                "version": metadata.version,
                "package_name": metadata.package_name
            }
            self._save_registry()
            
            # 6. 如果是Android兼容的应用，安装到Android目录
            if is_compatible_with_android(mpk_file):
                self.logger.debug(f"应用 '{app_id}' 与Android兼容，安装到Android目录")
                self._install_to_android(mpk_file, app_id)

            self.logger.info(f"应用 '{app_id}' 安装成功.")
            self._trigger_event("install", app_id, target_mpk_path)
            return app_id

        except Exception as e:
            self.logger.error(f"应用安装过程中发生错误: {e}")
            # 尝试清理可能已复制的文件
            if 'target_mpk_path' in locals() and os.path.exists(target_mpk_path):
                try:
                    os.remove(target_mpk_path)
                except OSError as remove_err:
                     self.logger.error(f"清理安装失败的文件时出错: {remove_err}")
            self._trigger_event("error", f"Installation failed: {e}")
            raise # 重新抛出异常
            
    def _install_to_android(self, mpk_file: MPKFile, app_id: str) -> None:
        """将MPK应用安装到Android设备
        
        Args:
            mpk_file: MPK文件对象
            app_id: 应用ID
        """
        try:
            # 1. 创建应用目录
            app_dir = os.path.join(self.android_install_dir, app_id)
            os.makedirs(app_dir, exist_ok=True)
            
            # 2. 将MPK文件提取到目录
            mpk_file.extract_to_dir(app_dir)
            
            # 3. 更新Android注册表
            self.android_registry[app_id] = {
                "path": app_dir,
                "install_date": datetime.now().isoformat(),
                "app_name": mpk_file.metadata.app_name,
                "version": mpk_file.metadata.version,
                "package_name": mpk_file.metadata.package_name
            }
            self._save_android_registry()
            
            # 4. 如果在Android设备上，安装到系统
            if self._is_android():
                self._install_to_android_system(mpk_file, app_id)
            
            self.logger.info(f"应用 '{app_id}' 已成功安装到Android目录")
        except Exception as e:
            self.logger.error(f"将应用安装到Android目录时发生错误: {e}")
            # 清理可能已创建的目录
            if 'app_dir' in locals() and os.path.exists(app_dir):
                try:
                    shutil.rmtree(app_dir)
                except OSError as remove_err:
                    self.logger.error(f"清理安装失败的目录时出错: {remove_err}")
            raise
            
    def _install_to_android_system(self, mpk_file: MPKFile, app_id: str) -> None:
        """将MPK应用安装到Android系统
        
        Args:
            mpk_file: MPK文件对象
            app_id: 应用ID
        """
        try:
            # 1. 创建ZIP文件
            zip_path = os.path.join(self.android_install_dir, f"{app_id}.zip")
            mpk_file.to_zip_file(zip_path)
            
            # 2. 调用Android系统安装API
            # 这是一个简化的示例，实际实现可能需要使用Android API或ADB命令
            # 在真实的Android环境中，可以调用PackageManager安装应用
            if os.path.exists("/system/bin/pm"):
                process = subprocess.Popen(
                    ["/system/bin/pm", "install", "-r", zip_path],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    universal_newlines=True
                )
                stdout, stderr = process.communicate()
                if process.returncode != 0:
                    self.logger.error(f"安装到Android系统失败: {stderr}")
                    raise RuntimeError(f"安装到Android系统失败: {stderr}")
                self.logger.info(f"应用 '{app_id}' 已成功安装到Android系统")
            else:
                self.logger.warning("未找到Android包管理器，跳过安装到系统")
            
            # 3. 清理ZIP文件
            if os.path.exists(zip_path):
                os.remove(zip_path)
        except Exception as e:
            self.logger.error(f"将应用安装到Android系统时发生错误: {e}")
            # 清理可能已创建的ZIP文件
            if 'zip_path' in locals() and os.path.exists(zip_path):
                try:
                    os.remove(zip_path)
                except OSError as remove_err:
                    self.logger.error(f"清理ZIP文件时出错: {remove_err}")
            raise

    def uninstall_app(self, app_id: str) -> bool:
        """卸载指定 App ID 的应用"""
        self.logger.info(f"开始卸载应用: {app_id}")
        if app_id not in self.registry:
            self.logger.error(f"卸载失败: 应用 '{app_id}' 未安装.")
            return False

        app_info = self.registry[app_id]
        mpk_path = app_info.get("path")
        full_mpk_path = os.path.join(self.install_dir, os.path.basename(mpk_path)) # 确保路径正确

        try:
            # 1. 删除 MPK 文件
            if os.path.exists(full_mpk_path):
                self.logger.debug(f"删除文件: {full_mpk_path}")
                os.remove(full_mpk_path)
            else:
                self.logger.warning(f"注册表中记录的文件不存在: {full_mpk_path}")

            # 2. 从注册表移除
            del self.registry[app_id]
            self._save_registry()
            
            # 3. 如果在Android注册表中，也卸载Android版本
            if app_id in self.android_registry:
                self._uninstall_from_android(app_id)

            self.logger.info(f"应用 '{app_id}' 卸载成功.")
            self._trigger_event("uninstall", app_id)
            return True

        except Exception as e:
            self.logger.error(f"应用卸载过程中发生错误: {e}")
            self._trigger_event("error", f"Uninstallation failed: {e}")
            # 考虑是否需要回滚注册表？对于卸载来说，通常不需要
            return False
            
    def _uninstall_from_android(self, app_id: str) -> bool:
        """从Android卸载应用
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 卸载是否成功
        """
        if app_id not in self.android_registry:
            self.logger.warning(f"应用 '{app_id}' 未在Android中安装")
            return False
            
        try:
            # 1. 获取应用信息
            app_info = self.android_registry[app_id]
            app_dir = app_info.get("path")
            package_name = app_info.get("package_name")
            
            # 2. 如果在Android系统上，从系统卸载
            if self._is_android() and package_name:
                self._uninstall_from_android_system(package_name)
                
            # 3. 删除应用目录
            if os.path.exists(app_dir):
                shutil.rmtree(app_dir)
                
            # 4. 从Android注册表移除
            del self.android_registry[app_id]
            self._save_android_registry()
            
            self.logger.info(f"应用 '{app_id}' 已从Android卸载")
            return True
        except Exception as e:
            self.logger.error(f"从Android卸载应用时发生错误: {e}")
            return False
            
    def _uninstall_from_android_system(self, package_name: str) -> bool:
        """从Android系统卸载应用
        
        Args:
            package_name: 应用包名
            
        Returns:
            bool: 卸载是否成功
        """
        try:
            # 调用Android系统卸载API
            if os.path.exists("/system/bin/pm"):
                process = subprocess.Popen(
                    ["/system/bin/pm", "uninstall", package_name],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    universal_newlines=True
                )
                stdout, stderr = process.communicate()
                if process.returncode != 0:
                    self.logger.error(f"从Android系统卸载失败: {stderr}")
                    return False
                self.logger.info(f"应用 '{package_name}' 已从Android系统卸载")
                return True
            else:
                self.logger.warning("未找到Android包管理器，跳过从系统卸载")
                return False
        except Exception as e:
            self.logger.error(f"从Android系统卸载应用时发生错误: {e}")
            return False

    def list_installed_apps(self) -> List[Dict[str, Any]]:
        """列出所有已安装的应用信息"""
        return list(self.registry.values()) # 返回包含应用信息的字典列表
        
    def list_android_installed_apps(self) -> List[Dict[str, Any]]:
        """列出所有已安装到Android的应用信息"""
        return list(self.android_registry.values())

    def get_installed_app_ids(self) -> List[str]:
         """获取所有已安装应用的ID列表"""
         return list(self.registry.keys())
         
    def get_android_installed_app_ids(self) -> List[str]:
         """获取所有已安装到Android的应用的ID列表"""
         return list(self.android_registry.keys())

    def load_app(self, app_id: str) -> bool:
        """加载已安装的应用到运行时环境，准备运行"""
        self.logger.info(f"开始加载已安装应用: {app_id}")
        if app_id in self.loaders:
            self.logger.warning(f"应用 '{app_id}' 已经加载.")
            return True # 或 False，取决于是否允许重复加载

        if app_id not in self.registry:
            self.logger.error(f"加载失败: 应用 '{app_id}' 未安装.")
            self._trigger_event("error", f"App '{app_id}' not installed.")
            return False

        app_info = self.registry[app_id]
        mpk_path = app_info.get("path")
        full_mpk_path = os.path.join(self.install_dir, os.path.basename(mpk_path))

        if not os.path.exists(full_mpk_path):
             self.logger.error(f"加载失败: 找不到已安装的 MPK 文件 {full_mpk_path}")
             self._trigger_event("error", f"Installed MPK file not found for '{app_id}'.")
             return False

        try:
            # 加载 MPK 文件对象
            mpk_file = MPKFile.load(full_mpk_path)

            # 创建加载器实例
            loader = MPKLoader(mpk_file)

            # --- 执行加载前的检查 --- (与之前 Loader.run 中的检查类似)
            # 1. 文件基本验证 (已在MPKFile.load完成)
            if not loader.verify(): # 签名验证
                 self.logger.warning(f"应用 '{app_id}' 签名验证失败或跳过 (占位符). 继续加载.")
                 # raise ValueError("MPK文件签名验证失败") # 或根据策略决定

            # 2. 检查依赖
            if not loader.check_dependencies():
                raise ValueError("应用依赖检查失败")

            # 3. 检查权限
            if not loader.check_permissions():
                 raise ValueError("应用权限检查失败")
            # --- 检查完成 --- 
            
            # 4. 设置环境 (提取资源等)
            # 注意: loader.setup_environment() 会创建临时目录
            # 对于已安装应用，我们可能希望资源是持久的，但这会增加复杂性
            # 暂时维持现状：每次加载都在临时目录中解压资源
            loader.setup_environment()
            
            # 5. 加载代码到内存
            if not loader.load_code():
                 raise ValueError("应用代码加载失败")

            # 保存加载器，准备运行
            self.loaders[app_id] = loader
            self.logger.info(f"已安装应用 '{app_id}' 加载成功，准备运行.")
            self._trigger_event("start", app_id) # 使用 start 事件表示加载完成
            return True

        except Exception as e:
            self.logger.error(f"加载已安装应用 '{app_id}' 时出错: {e}")
            self._trigger_event("error", f"Failed to load app '{app_id}': {e}")
            # 清理可能创建的临时资源
            if 'loader' in locals() and hasattr(loader, 'cleanup'):
                loader.cleanup()
            return False

    def unload_app(self, app_id: str) -> None:
        """从运行时卸载一个已加载的应用 (停止运行并清理资源)"""
        if app_id in self.loaders:
            loader = self.loaders[app_id]
            loader.cleanup() # 清理 Loader 创建的临时资源
            del self.loaders[app_id]
            self.logger.info(f"已加载的应用 '{app_id}' 已从运行时卸载.")
            self._trigger_event("stop", app_id)
        else:
             self.logger.warning(f"尝试卸载未加载的应用: {app_id}")

    def run_app(self, app_id: str) -> bool:
        """运行应用"""
        if app_id not in self.loaders:
            self.logger.error(f"应用不存在: {app_id}")
            return False
            
        try:
            loader = self.loaders[app_id]
            return loader.run()
            
        except Exception as e:
            self.logger.error(f"应用运行失败: {e}")
            self._trigger_event("error", str(e))
            return False
            
    def get_app_info(self, app_id: str) -> Optional[Dict[str, Any]]:
        """获取应用信息"""
        if app_id not in self.loaders:
            return None
            
        loader = self.loaders[app_id]
        viewer = loader.viewer
        
        return {
            "metadata": viewer.get_metadata(),
            "code_info": viewer.get_code_info(),
            "resources_info": viewer.get_resources_info(),
            "signature_info": viewer.get_signature_info()
        }
        
    def get_app_resource(self, app_id: str, resource_type: str) -> Optional[bytes]:
        """获取应用资源"""
        if app_id not in self.loaders:
            return None
            
        loader = self.loaders[app_id]
        return loader.get_resource(resource_type)
        
    def extract_app_resource(self, app_id: str, output_dir: str) -> bool:
        """提取应用资源"""
        if app_id not in self.loaders:
            return False
            
        try:
            loader = self.loaders[app_id]
            loader.extract_resources(output_dir)
            self._trigger_event("resource", app_id, output_dir)
            return True
            
        except Exception as e:
            self.logger.error(f"资源提取失败: {e}")
            return False
            
    def cleanup(self) -> None:
        """清理所有资源"""
        for app_id in list(self.loaders.keys()):
            self.unload_app(app_id)
            
    def __enter__(self):
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.cleanup()
        
    def get_installed_app_info(self, app_id: str) -> Optional[Dict[str, Any]]:
        """获取已安装但未加载的应用的基本信息 (从注册表和MPK文件)"""
        if app_id not in self.registry:
            self.logger.warning(f"请求获取未安装应用的信息: {app_id}")
            return None
        
        app_reg_info = self.registry[app_id]
        mpk_path = app_reg_info.get("path")
        full_mpk_path = os.path.join(self.install_dir, os.path.basename(mpk_path))
        
        try:
            # 只加载文件获取信息，不执行验证和加载
            viewer = MPKViewer.from_file(full_mpk_path)
            return {
                "app_id": app_id,
                "install_date": app_reg_info.get("install_date"),
                "mpk_path": full_mpk_path,
                "metadata": viewer.get_metadata(),
                "code_info": viewer.get_code_info(),
                "resources_info": viewer.get_resources_info(),
                "signature_info": viewer.get_signature_info(),
                "file_info": viewer.get_file_info()
            }
        except Exception as e:
            self.logger.error(f"获取已安装应用 '{app_id}' 信息时出错: {e}")
            return None # 或者返回部分注册表信息
        
    @classmethod
    def get_instance(cls) -> 'Runtime':
        """获取运行时实例"""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance 

    def run_on_android(self, app_id: str) -> bool:
        """在Android设备上运行应用
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 是否成功运行
        """
        # 检查应用是否已安装到Android
        if app_id not in self.android_registry:
            self.logger.error(f"启动失败: 应用 '{app_id}' 未安装到Android.")
            return False
            
        try:
            # 获取应用信息
            app_info = self.android_registry[app_id]
            package_name = app_info.get("package_name")
            
            # 检查是否在Android设备上
            if not self._is_android():
                self.logger.error("非Android环境，无法启动Android应用")
                return False
                
            # 使用Android API启动应用
            if os.path.exists("/system/bin/am"):
                # 尝试启动应用的主活动
                process = subprocess.Popen(
                    ["/system/bin/am", "start", "-n", f"{package_name}/.MainActivity"],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    universal_newlines=True
                )
                stdout, stderr = process.communicate()
                if process.returncode != 0:
                    self.logger.error(f"启动Android应用失败: {stderr}")
                    return False
                
                self.logger.info(f"应用 '{app_id}' 已在Android上启动")
                return True
            else:
                self.logger.error("未找到Android活动管理器，无法启动应用")
                return False
        except Exception as e:
            self.logger.error(f"启动Android应用时发生错误: {e}")
            return False 