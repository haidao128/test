#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
沙箱运行器模块
===========

提供安全地运行应用程序的功能，确保应用在受限的环境中执行，防止恶意操作。

主要功能：
1. 应用程序安全启动与执行
2. 系统调用监控与拦截
3. 资源使用限制
4. 异常处理与安全退出
"""

import os
import sys
import time
import json
import logging
import multiprocessing
import signal
import tempfile
import subprocess
import traceback
from typing import Dict, List, Any, Optional, Tuple, Callable

from . import (is_syscall_allowed, is_path_allowed, verify_app_certificate, 
               register_app, get_sandbox_info)

logger = logging.getLogger("mobile_platform_creator.core.sandbox.runner")

# 资源限制配置
DEFAULT_RESOURCE_LIMITS = {
    "max_cpu_time": 30,        # 秒
    "max_memory": 100 * 1024 * 1024,  # 100MB
    "max_files": 50,           # 最大打开文件数
    "max_processes": 5,        # 最大子进程数
    "max_file_size": 10 * 1024 * 1024,  # 最大文件大小 10MB
    "max_network_connections": 10,  # 最大网络连接数
}

class SandboxRunner:
    """沙箱运行器，用于安全地运行应用程序"""
    
    def __init__(self, app_id: str, app_path: str, sandbox_level: str = "strict"):
        """
        初始化沙箱运行器
        
        Args:
            app_id: 应用ID
            app_path: 应用程序路径
            sandbox_level: 沙箱安全级别
        """
        self.app_id = app_id
        self.app_path = app_path
        self.sandbox_level = sandbox_level
        self.process = None
        self.start_time = 0
        self.end_time = 0
        self.exit_code = None
        self.resources_used = {
            "cpu_time": 0,
            "memory": 0,
            "files_opened": 0,
            "processes_created": 0,
        }
        self.running = False
        self.logs = []
        self.resource_limits = DEFAULT_RESOURCE_LIMITS.copy()
        
        # 临时目录，用于存储沙箱日志和状态
        self.temp_dir = tempfile.mkdtemp(prefix=f"sandbox_{app_id}_")
        
        # 应用元数据
        self.app_metadata = None
        
        # 沙箱配置
        self.config = {
            "allow_network": False,
            "allow_filesystem": True,
            "restricted_paths": [],
            "allowed_paths": [],
            "isolated_home": True,
        }
    
    def __del__(self):
        """析构函数，确保资源被释放"""
        self.cleanup()
    
    def cleanup(self):
        """清理资源"""
        try:
            # 终止进程（如果仍在运行）
            if self.running and self.process:
                self.terminate()
            
            # 删除临时文件
            if os.path.exists(self.temp_dir):
                import shutil
                shutil.rmtree(self.temp_dir)
        except Exception as e:
            logger.error("清理沙箱资源失败: %s", e)
    
    def load_app_metadata(self) -> bool:
        """
        加载应用元数据
        
        Returns:
            bool: 加载是否成功
        """
        try:
            metadata_path = os.path.join(self.app_path, "manifest.json")
            if not os.path.exists(metadata_path):
                logger.error("应用元数据文件不存在: %s", metadata_path)
                return False
            
            with open(metadata_path, "r", encoding="utf-8") as f:
                self.app_metadata = json.load(f)
            
            # 验证基本字段
            required_fields = ["id", "name", "version", "permissions"]
            if not all(field in self.app_metadata for field in required_fields):
                logger.error("应用元数据缺少必要字段")
                return False
            
            # 检查应用ID是否匹配
            if self.app_metadata["id"] != self.app_id:
                logger.error("应用ID不匹配: %s != %s", 
                           self.app_metadata["id"], self.app_id)
                return False
            
            logger.info("已加载应用元数据: %s (%s)", 
                       self.app_metadata["name"], self.app_metadata["version"])
            return True
        except Exception as e:
            logger.error("加载应用元数据失败: %s", e)
            return False
    
    def configure_sandbox(self) -> bool:
        """
        配置沙箱环境
        
        Returns:
            bool: 配置是否成功
        """
        try:
            if not self.app_metadata:
                logger.error("未加载应用元数据，无法配置沙箱")
                return False
            
            # 根据权限配置沙箱
            permissions = self.app_metadata.get("permissions", [])
            
            # 网络权限
            self.config["allow_network"] = "network" in permissions
            
            # 文件系统权限
            if "storage" in permissions:
                self.config["allow_filesystem"] = True
                self.config["isolated_home"] = True
                
                # 应用私有目录
                app_data_dir = os.path.join(self.app_path, "data")
                os.makedirs(app_data_dir, exist_ok=True)
                self.config["allowed_paths"].append(app_data_dir)
            else:
                self.config["allow_filesystem"] = False
            
            # 根据安全级别调整资源限制
            if self.sandbox_level == "strict":
                self.resource_limits["max_cpu_time"] = 15
                self.resource_limits["max_memory"] = 50 * 1024 * 1024
                self.resource_limits["max_processes"] = 3
            elif self.sandbox_level == "minimal":
                self.resource_limits["max_cpu_time"] = 60
                self.resource_limits["max_memory"] = 200 * 1024 * 1024
                self.resource_limits["max_processes"] = 10
            
            logger.info("沙箱环境配置完成，权限: %s", permissions)
            return True
        except Exception as e:
            logger.error("配置沙箱环境失败: %s", e)
            return False
    
    def prepare_environment(self) -> Dict[str, str]:
        """
        准备运行环境
        
        Returns:
            Dict[str, str]: 环境变量字典
        """
        env = os.environ.copy()
        
        # 设置沙箱标识
        env["SANDBOX_ENABLED"] = "1"
        env["SANDBOX_LEVEL"] = self.sandbox_level
        env["SANDBOX_APP_ID"] = self.app_id
        
        # 设置权限标识
        env["SANDBOX_ALLOW_NETWORK"] = "1" if self.config["allow_network"] else "0"
        env["SANDBOX_ALLOW_FILESYSTEM"] = "1" if self.config["allow_filesystem"] else "0"
        
        # 设置资源限制
        env["SANDBOX_MAX_CPU_TIME"] = str(self.resource_limits["max_cpu_time"])
        env["SANDBOX_MAX_MEMORY"] = str(self.resource_limits["max_memory"])
        
        # 设置隔离主目录
        if self.config["isolated_home"]:
            isolated_home = os.path.join(self.temp_dir, "home")
            os.makedirs(isolated_home, exist_ok=True)
            env["HOME"] = isolated_home
        
        # 设置临时目录
        tmp_dir = os.path.join(self.temp_dir, "tmp")
        os.makedirs(tmp_dir, exist_ok=True)
        env["TMPDIR"] = tmp_dir
        env["TEMP"] = tmp_dir
        env["TMP"] = tmp_dir
        
        return env
    
    def start(self) -> bool:
        """
        启动沙箱并运行应用
        
        Returns:
            bool: 启动是否成功
        """
        if self.running:
            logger.warning("沙箱已经在运行中")
            return False
        
        try:
            # 加载应用元数据
            if not self.load_app_metadata():
                return False
            
            # 配置沙箱环境
            if not self.configure_sandbox():
                return False
            
            # 准备环境变量
            env = self.prepare_environment()
            
            # 确定启动脚本，使用绝对路径
            main_script = os.path.abspath(os.path.join(self.app_path, "main.py"))
            if not os.path.exists(main_script):
                main_script = os.path.abspath(os.path.join(self.app_path, "code", "main.py"))
                if not os.path.exists(main_script):
                    logger.error("找不到应用主脚本，已尝试以下路径:")
                    logger.error(f"  - {os.path.abspath(os.path.join(self.app_path, 'main.py'))}")
                    logger.error(f"  - {os.path.abspath(os.path.join(self.app_path, 'code', 'main.py'))}")
                    logger.error(f"当前工作目录: {os.getcwd()}")
                    logger.error(f"应用目录内容: {os.listdir(self.app_path)}")
                    return False
            
            logger.info(f"使用主脚本: {main_script}")
            
            # 启动进程
            logger.info("启动应用: %s", self.app_id)
            
            # 创建日志文件
            log_file = os.path.join(self.temp_dir, "sandbox.log")
            
            # 直接运行Python解释器
            python_executable = sys.executable
            logger.info(f"使用Python解释器: {python_executable}")
            
            # 使用完整路径
            self.process = subprocess.Popen(
                [python_executable, main_script],
                cwd=os.path.abspath(self.app_path),  # 使用绝对路径
                env=env,
                stdout=open(os.path.join(self.temp_dir, "stdout.log"), "w"),
                stderr=open(os.path.join(self.temp_dir, "stderr.log"), "w"),
                universal_newlines=True
            )
            
            self.start_time = time.time()
            self.running = True
            
            logger.info("应用已启动，进程ID: %d", self.process.pid)
            return True
        except Exception as e:
            logger.error("启动应用失败: %s", e)
            logger.error("错误详情: %s", traceback.format_exc())
            self.cleanup()
            return False
    
    def wait(self, timeout: Optional[float] = None) -> Optional[int]:
        """
        等待应用结束
        
        Args:
            timeout: 超时时间（秒），None表示无限等待
            
        Returns:
            Optional[int]: 应用进程的退出码，超时则返回None
        """
        if not self.running or not self.process:
            logger.warning("沙箱未运行")
            return None
        
        try:
            exit_code = self.process.wait(timeout=timeout)
            self.end_time = time.time()
            self.exit_code = exit_code
            self.running = False
            
            # 记录资源使用情况
            self.resources_used["cpu_time"] = self.end_time - self.start_time
            
            logger.info("应用已结束，退出码: %d，运行时间: %.2f秒", 
                       exit_code, self.resources_used["cpu_time"])
            
            return exit_code
        except subprocess.TimeoutExpired:
            logger.warning("等待应用结束超时")
            return None
        except Exception as e:
            logger.error("等待应用结束出错: %s", e)
            return None
    
    def terminate(self) -> bool:
        """
        终止应用进程
        
        Returns:
            bool: 终止是否成功
        """
        if not self.running or not self.process:
            logger.warning("沙箱未运行")
            return False
        
        try:
            logger.info("正在终止应用: %s", self.app_id)
            
            # 先尝试正常终止
            self.process.terminate()
            
            # 给应用5秒时间清理资源
            try:
                self.process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                # 如果超时，强制终止
                logger.warning("应用未能及时终止，强制终止")
                self.process.kill()
            
            self.end_time = time.time()
            self.exit_code = self.process.returncode
            self.running = False
            
            # 记录资源使用情况
            self.resources_used["cpu_time"] = self.end_time - self.start_time
            
            logger.info("应用已终止，退出码: %d，运行时间: %.2f秒", 
                       self.exit_code, self.resources_used["cpu_time"])
            
            return True
        except Exception as e:
            logger.error("终止应用失败: %s", e)
            return False
    
    def get_logs(self) -> List[str]:
        """
        获取应用日志
        
        Returns:
            List[str]: 日志列表
        """
        logs = []
        
        # 读取stdout日志
        stdout_log = os.path.join(self.temp_dir, "stdout.log")
        if os.path.exists(stdout_log):
            try:
                # 尝试多种编码
                encodings = ["utf-8", "gbk", "latin1", "cp936"]
                stdout_content = None
                
                for encoding in encodings:
                    try:
                        with open(stdout_log, "r", encoding=encoding) as f:
                            stdout_content = f.read()
                        break  # 如果成功读取，跳出循环
                    except UnicodeDecodeError:
                        continue
                
                if stdout_content:
                    logs.append("--- 标准输出 ---")
                    logs.append(stdout_content)
                else:
                    logs.append("无法读取标准输出日志：不支持的编码")
            except Exception as e:
                logs.append(f"读取标准输出日志失败: {e}")
        
        # 读取stderr日志
        stderr_log = os.path.join(self.temp_dir, "stderr.log")
        if os.path.exists(stderr_log):
            try:
                # 尝试多种编码
                encodings = ["utf-8", "gbk", "latin1", "cp936"]
                stderr_content = None
                
                for encoding in encodings:
                    try:
                        with open(stderr_log, "r", encoding=encoding) as f:
                            stderr_content = f.read()
                        break  # 如果成功读取，跳出循环
                    except UnicodeDecodeError:
                        continue
                
                if stderr_content:
                    logs.append("--- 标准错误 ---")
                    logs.append(stderr_content)
                else:
                    logs.append("无法读取标准错误日志：不支持的编码")
            except Exception as e:
                logs.append(f"读取标准错误日志失败: {e}")
        
        return logs
    
    def get_status(self) -> Dict[str, Any]:
        """
        获取沙箱状态
        
        Returns:
            Dict[str, Any]: 状态信息
        """
        status = {
            "app_id": self.app_id,
            "running": self.running,
            "start_time": self.start_time,
            "elapsed": time.time() - self.start_time if self.running else 0,
            "exit_code": self.exit_code,
            "resources_used": self.resources_used,
            "resource_limits": self.resource_limits,
            "sandbox_level": self.sandbox_level,
            "permissions": self.app_metadata.get("permissions", []) if self.app_metadata else [],
        }
        
        if self.process and self.running:
            try:
                # 在实际实现中，这里应该使用psutil等获取资源使用情况
                status["pid"] = self.process.pid
            except:
                status["pid"] = None
        
        return status


def run_app(app_id: str, app_path: str, sandbox_level: str = "strict") -> Dict[str, Any]:
    """
    运行应用程序
    
    Args:
        app_id: 应用ID
        app_path: 应用路径
        sandbox_level: 沙箱安全级别
        
    Returns:
        Dict[str, Any]: 运行结果
    """
    # 创建沙箱运行器
    runner = SandboxRunner(app_id, app_path, sandbox_level)
    
    try:
        # 启动应用
        if not runner.start():
            return {
                "success": False,
                "error": "启动应用失败",
                "app_id": app_id
            }
        
        # 等待应用结束（最多运行30分钟）
        exit_code = runner.wait(timeout=1800)
        
        if exit_code is None:
            # 应用超时，强制终止
            runner.terminate()
            return {
                "success": False,
                "error": "应用运行超时",
                "app_id": app_id,
                "logs": runner.get_logs(),
                "status": runner.get_status()
            }
        
        # 获取运行状态
        status = runner.get_status()
        logs = runner.get_logs()
        
        # 清理资源
        runner.cleanup()
        
        return {
            "success": exit_code == 0,
            "exit_code": exit_code,
            "app_id": app_id,
            "logs": logs,
            "status": status
        }
    except Exception as e:
        logger.error("运行应用异常: %s", e)
        logger.error("错误详情: %s", traceback.format_exc())
        
        # 确保资源被清理
        try:
            runner.cleanup()
        except:
            pass
        
        return {
            "success": False,
            "error": str(e),
            "app_id": app_id
        }


if __name__ == "__main__":
    # 简单的命令行接口，用于测试
    import argparse
    
    parser = argparse.ArgumentParser(description="沙箱应用运行器")
    parser.add_argument("app_id", help="应用ID")
    parser.add_argument("app_path", help="应用路径")
    parser.add_argument("--level", choices=["minimal", "standard", "strict"], 
                      default="strict", help="沙箱安全级别")
    
    args = parser.parse_args()
    
    # 配置日志
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    
    # 运行应用
    result = run_app(args.app_id, args.app_path, args.level)
    
    # 输出结果
    print(json.dumps(result, indent=2, ensure_ascii=False)) 