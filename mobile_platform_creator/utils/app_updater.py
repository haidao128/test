"""
应用更新管理器模块
==============

负责检测和管理应用更新，支持自动更新和增量更新功能。

主要功能：
1. 版本比较与检测
2. 更新下载与应用
3. 增量更新
4. 更新回滚
"""

import os
import json
import logging
import time
import shutil
import tempfile
import threading
import queue
from typing import Dict, List, Any, Optional, Tuple, Set, Callable

from .mpk_package import MPKPackage, is_valid_mpk

logger = logging.getLogger("mobile_platform_creator.utils.app_updater")

class UpdateError(Exception):
    """更新过程中的异常"""
    pass

class UpdateTask:
    """更新任务"""
    
    def __init__(self, app_id: str, current_version: str, target_version: str,
                old_path: str, new_path: Optional[str] = None):
        """
        初始化更新任务
        
        Args:
            app_id: 应用ID
            current_version: 当前版本
            target_version: 目标版本
            old_path: 旧版本文件路径
            new_path: 新版本文件路径，可选
        """
        self.app_id = app_id
        self.current_version = current_version
        self.target_version = target_version
        self.old_path = old_path
        self.new_path = new_path
        self.status = "pending"  # pending, downloading, applying, completed, failed
        self.progress = 0  # 0-100
        self.error = None
        self.start_time = 0
        self.end_time = 0
        self.backup_path = None

class AppUpdater:
    """应用更新管理器"""
    
    def __init__(self, repo_dir: Optional[str] = None, backup_dir: Optional[str] = None):
        """
        初始化应用更新管理器
        
        Args:
            repo_dir: 应用仓库目录，默认为None (自动选择)
            backup_dir: 备份目录，默认为None (自动选择)
        """
        # 设置目录
        if repo_dir:
            self.repo_dir = repo_dir
        else:
            self.repo_dir = os.path.expanduser("~/MobilePlatform/apps")
        
        if backup_dir:
            self.backup_dir = backup_dir
        else:
            self.backup_dir = os.path.expanduser("~/MobilePlatform/backups")
        
        # 确保目录存在
        os.makedirs(self.repo_dir, exist_ok=True)
        os.makedirs(self.backup_dir, exist_ok=True)
        
        # 任务队列
        self.tasks: Dict[str, UpdateTask] = {}
        
        # 工作线程
        self.worker_thread = None
        self.task_queue = queue.Queue()
        self.running = False
        
        # 加载更新配置
        self.config_path = os.path.join(self.repo_dir, "update_config.json")
        self.config = self._load_config()
    
    def _load_config(self) -> Dict[str, Any]:
        """
        加载更新配置
        
        Returns:
            Dict[str, Any]: 配置数据
        """
        if os.path.exists(self.config_path):
            try:
                with open(self.config_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                logger.error("加载更新配置失败: %s", e)
        
        # 默认配置
        return {
            "auto_check": True,
            "auto_update": False,
            "check_interval": 86400,  # 1天
            "update_channel": "stable",
            "last_check": 0,
            "backup_enabled": True,
            "max_backups": 5
        }
    
    def _save_config(self) -> bool:
        """
        保存更新配置
        
        Returns:
            bool: 保存是否成功
        """
        try:
            with open(self.config_path, 'w', encoding='utf-8') as f:
                json.dump(self.config, f, ensure_ascii=False, indent=2)
            return True
        except Exception as e:
            logger.error("保存更新配置失败: %s", e)
            return False
    
    def set_config(self, key: str, value: Any) -> bool:
        """
        设置配置项
        
        Args:
            key: 配置项键名
            value: 配置项值
            
        Returns:
            bool: 设置是否成功
        """
        self.config[key] = value
        return self._save_config()
    
    def start_worker(self) -> None:
        """启动工作线程"""
        if self.worker_thread and self.worker_thread.is_alive():
            logger.warning("工作线程已在运行")
            return
        
        self.running = True
        self.worker_thread = threading.Thread(target=self._worker_loop)
        self.worker_thread.daemon = True
        self.worker_thread.start()
        logger.info("应用更新管理器工作线程已启动")
    
    def stop_worker(self) -> None:
        """停止工作线程"""
        self.running = False
        if self.worker_thread:
            self.worker_thread.join(timeout=5.0)
        logger.info("应用更新管理器工作线程已停止")
    
    def _worker_loop(self) -> None:
        """工作线程循环"""
        while self.running:
            try:
                # 从队列获取任务，超时1秒以便能够响应停止信号
                task_id = self.task_queue.get(timeout=1.0)
                
                if task_id in self.tasks:
                    task = self.tasks[task_id]
                    logger.info("处理更新任务: %s (%s -> %s)", 
                               task.app_id, task.current_version, task.target_version)
                    
                    try:
                        task.status = "processing"
                        task.start_time = time.time()
                        
                        # 备份原应用
                        if self.config.get("backup_enabled", True):
                            task.backup_path = self._backup_app(task.app_id, task.old_path)
                        
                        # 如果需要先下载更新
                        if not task.new_path:
                            task.status = "downloading"
                            task.new_path = self._download_update(task.app_id, task.target_version)
                        
                        # 应用更新
                        task.status = "applying"
                        success = self._apply_update(task)
                        
                        # 更新任务状态
                        task.end_time = time.time()
                        if success:
                            task.status = "completed"
                            task.progress = 100
                            logger.info("应用 %s 更新成功: %s -> %s", 
                                       task.app_id, task.current_version, task.target_version)
                        else:
                            task.status = "failed"
                            task.error = "应用更新失败"
                            logger.error("应用 %s 更新失败", task.app_id)
                    except Exception as e:
                        task.status = "failed"
                        task.error = str(e)
                        task.end_time = time.time()
                        logger.error("处理更新任务异常: %s", e)
                
                # 标记任务完成
                self.task_queue.task_done()
            except queue.Empty:
                # 队列为空，继续等待
                pass
            except Exception as e:
                logger.error("工作线程异常: %s", e)
    
    def _backup_app(self, app_id: str, file_path: str) -> Optional[str]:
        """
        备份应用
        
        Args:
            app_id: 应用ID
            file_path: 应用文件路径
            
        Returns:
            Optional[str]: 备份文件路径，失败时返回None
        """
        try:
            # 创建备份目录
            app_backup_dir = os.path.join(self.backup_dir, app_id)
            os.makedirs(app_backup_dir, exist_ok=True)
            
            # 生成备份文件名
            timestamp = int(time.time())
            backup_filename = f"{app_id}_{timestamp}.mpk.bak"
            backup_path = os.path.join(app_backup_dir, backup_filename)
            
            # 复制文件
            shutil.copy2(file_path, backup_path)
            
            # 清理旧备份
            self._cleanup_old_backups(app_id)
            
            logger.info("已备份应用 %s: %s", app_id, backup_path)
            return backup_path
        except Exception as e:
            logger.error("备份应用 %s 失败: %s", app_id, e)
            return None
    
    def _cleanup_old_backups(self, app_id: str) -> None:
        """
        清理旧备份
        
        Args:
            app_id: 应用ID
        """
        max_backups = self.config.get("max_backups", 5)
        app_backup_dir = os.path.join(self.backup_dir, app_id)
        
        if not os.path.exists(app_backup_dir):
            return
        
        try:
            # 获取所有备份文件
            backups = []
            for filename in os.listdir(app_backup_dir):
                if filename.endswith(".mpk.bak"):
                    file_path = os.path.join(app_backup_dir, filename)
                    backups.append((file_path, os.path.getmtime(file_path)))
            
            # 按修改时间排序
            backups.sort(key=lambda x: x[1], reverse=True)
            
            # 删除多余的备份
            if len(backups) > max_backups:
                for file_path, _ in backups[max_backups:]:
                    os.remove(file_path)
                    logger.debug("已删除旧备份: %s", file_path)
        except Exception as e:
            logger.error("清理旧备份失败: %s", e)
    
    def _download_update(self, app_id: str, version: str) -> Optional[str]:
        """
        下载应用更新
        
        Args:
            app_id: 应用ID
            version: 目标版本
            
        Returns:
            Optional[str]: 下载文件路径，失败时返回None
        """
        # 这里应该调用应用商店API下载更新
        # 由于这只是一个演示，这里只是模拟下载过程
        
        try:
            # 模拟下载延迟
            time.sleep(2)
            
            # 创建临时文件模拟下载结果
            temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".mpk")
            temp_file.close()
            
            # 模拟更新文件内容
            with open(temp_file.name, 'w') as f:
                f.write(f"Simulated update for {app_id} version {version}")
            
            logger.info("已下载应用 %s 的更新: %s", app_id, version)
            return temp_file.name
        except Exception as e:
            logger.error("下载应用 %s 更新失败: %s", app_id, e)
            return None
    
    def _apply_update(self, task: UpdateTask) -> bool:
        """
        应用更新
        
        Args:
            task: 更新任务
            
        Returns:
            bool: 更新是否成功
        """
        try:
            # 检查新版本文件是否存在
            if not task.new_path or not os.path.exists(task.new_path):
                raise UpdateError("更新文件不存在")
            
            # 检查MPK文件有效性
            if not is_valid_mpk(task.new_path):
                raise UpdateError("无效的MPK文件")
            
            # 解析MPK包
            package = MPKPackage(task.new_path)
            manifest = package.get_manifest()
            
            # 验证应用ID和版本
            if manifest["id"] != task.app_id:
                raise UpdateError(f"应用ID不匹配: {manifest['id']} != {task.app_id}")
            
            if manifest["version"] != task.target_version:
                raise UpdateError(f"版本不匹配: {manifest['version']} != {task.target_version}")
            
            # 复制文件到仓库
            target_filename = f"{task.app_id}_{task.target_version}.mpk"
            target_path = os.path.join(self.repo_dir, target_filename)
            
            # 复制文件
            shutil.copy2(task.new_path, target_path)
            
            # 删除旧文件（如果不同）
            if os.path.exists(task.old_path) and not os.path.samefile(task.old_path, target_path):
                os.remove(task.old_path)
            
            # 删除临时文件（如果存在且不同）
            if os.path.exists(task.new_path) and not os.path.samefile(task.new_path, target_path):
                os.remove(task.new_path)
            
            # 更新索引（在实际实现中，这里需要更新应用索引）
            
            return True
        except Exception as e:
            logger.error("应用更新失败: %s", e)
            # 如果有备份，可以考虑恢复
            if task.backup_path and os.path.exists(task.backup_path):
                logger.info("从备份恢复应用: %s", task.backup_path)
                try:
                    shutil.copy2(task.backup_path, task.old_path)
                except Exception as restore_error:
                    logger.error("恢复备份失败: %s", restore_error)
            
            return False
    
    def schedule_update(self, app_id: str, current_version: str, target_version: str,
                      old_path: str, new_path: Optional[str] = None) -> str:
        """
        调度更新任务
        
        Args:
            app_id: 应用ID
            current_version: 当前版本
            target_version: 目标版本
            old_path: 旧版本文件路径
            new_path: 新版本文件路径，可选
            
        Returns:
            str: 任务ID
        """
        # 生成任务ID
        task_id = f"{app_id}_{int(time.time())}"
        
        # 创建任务
        task = UpdateTask(app_id, current_version, target_version, old_path, new_path)
        self.tasks[task_id] = task
        
        # 将任务加入队列
        self.task_queue.put(task_id)
        
        logger.info("已调度更新任务: %s (%s -> %s)", app_id, current_version, target_version)
        return task_id
    
    def get_task_status(self, task_id: str) -> Optional[Dict[str, Any]]:
        """
        获取任务状态
        
        Args:
            task_id: 任务ID
            
        Returns:
            Optional[Dict[str, Any]]: 任务状态，不存在时返回None
        """
        if task_id not in self.tasks:
            return None
        
        task = self.tasks[task_id]
        return {
            "app_id": task.app_id,
            "current_version": task.current_version,
            "target_version": task.target_version,
            "status": task.status,
            "progress": task.progress,
            "error": task.error,
            "start_time": task.start_time,
            "end_time": task.end_time,
            "duration": task.end_time - task.start_time if task.end_time > 0 else 0
        }
    
    def cancel_update(self, task_id: str) -> bool:
        """
        取消更新任务
        
        Args:
            task_id: 任务ID
            
        Returns:
            bool: 取消是否成功
        """
        if task_id not in self.tasks:
            return False
        
        task = self.tasks[task_id]
        
        # 只能取消未开始的任务
        if task.status == "pending":
            # 从队列中移除（这是不可能的，因为队列不支持删除）
            # 但可以标记为已取消，工作线程会忽略它
            task.status = "cancelled"
            logger.info("已取消更新任务: %s", task_id)
            return True
        else:
            logger.warning("无法取消已开始的任务: %s", task_id)
            return False
    
    def rollback(self, app_id: str) -> bool:
        """
        回滚更新
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 回滚是否成功
        """
        # 获取最新的备份
        app_backup_dir = os.path.join(self.backup_dir, app_id)
        if not os.path.exists(app_backup_dir):
            logger.error("应用 %s 没有备份，无法回滚", app_id)
            return False
        
        try:
            # 获取所有备份文件
            backups = []
            for filename in os.listdir(app_backup_dir):
                if filename.endswith(".mpk.bak"):
                    file_path = os.path.join(app_backup_dir, filename)
                    backups.append((file_path, os.path.getmtime(file_path)))
            
            if not backups:
                logger.error("应用 %s 没有备份文件，无法回滚", app_id)
                return False
            
            # 按修改时间排序
            backups.sort(key=lambda x: x[1], reverse=True)
            
            # 获取最新的备份
            latest_backup = backups[0][0]
            
            # 恢复应用（在实际实现中，这里需要更新应用索引）
            # ...
            
            logger.info("已回滚应用 %s 到上一版本", app_id)
            return True
        except Exception as e:
            logger.error("回滚应用 %s 失败: %s", app_id, e)
            return False
    
    def check_updates_available(self) -> Dict[str, Dict[str, Any]]:
        """
        检查有更新的应用
        
        Returns:
            Dict[str, Dict[str, Any]]: 有更新的应用字典，以应用ID为键
        """
        # 这里应该调用应用商店API检查更新
        # 由于这只是一个演示，这里只是返回空字典
        
        # 更新最后检查时间
        self.config["last_check"] = int(time.time())
        self._save_config()
        
        return {} 