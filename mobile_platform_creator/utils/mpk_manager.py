"""
MPK 管理器模块
===========

管理 MPK 包的生命周期，包括创建、验证、安装、更新和卸载。

主要功能：
1. MPK 包生命周期管理
2. 包依赖管理
3. 版本控制
4. 安装和卸载
"""

import os
import json
import logging
import shutil
from typing import Dict, List, Any, Optional, Tuple

from .mpk_package import MPKPackage, is_valid_mpk
from .app_store import AppStoreClient
from .app_updater import AppUpdater

logger = logging.getLogger("mobile_platform_creator.utils.mpk_manager")

class MPKManager:
    """MPK 包管理器"""
    
    def __init__(self, repo_dir: Optional[str] = None):
        """
        初始化 MPK 包管理器
        
        Args:
            repo_dir: 包仓库目录，默认为 None (使用全局配置)
        """
        self.repo_dir = repo_dir or os.path.expanduser("~/.mobile_platform/packages")
        os.makedirs(self.repo_dir, exist_ok=True)
        
        # 初始化相关组件
        self.app_store = AppStoreClient(repo_dir=self.repo_dir)
        self.app_updater = AppUpdater(repo_dir=self.repo_dir)
    
    def create(self, source_dir: str, manifest: Optional[Dict[str, Any]] = None) -> MPKPackage:
        """
        从源目录创建 MPK 包
        
        Args:
            source_dir: 源代码目录
            manifest: 可选的清单数据
            
        Returns:
            MPKPackage: 创建的 MPK 包对象
        """
        logger.info(f"从目录创建 MPK 包: {source_dir}")
        
        # 验证源目录
        if not os.path.isdir(source_dir):
            raise ValueError(f"源目录不存在: {source_dir}")
        
        # 创建 MPK 包
        package = MPKPackage()
        
        # 如果提供了清单，使用它
        if manifest:
            package.set_manifest(manifest)
        
        # 添加源目录中的文件
        package.add_directory(source_dir)
        
        return package
    
    def install(self, mpk_path: str, target_dir: Optional[str] = None) -> bool:
        """
        安装 MPK 包
        
        Args:
            mpk_path: MPK 文件路径
            target_dir: 可选的安装目录
            
        Returns:
            bool: 安装是否成功
        """
        logger.info(f"安装 MPK 包: {mpk_path}")
        
        # 验证 MPK 文件
        if not is_valid_mpk(mpk_path):
            logger.error(f"无效的 MPK 文件: {mpk_path}")
            return False
        
        try:
            # 加载 MPK 包
            package = MPKPackage(mpk_path)
            
            # 获取安装目录
            if not target_dir:
                target_dir = os.path.join(self.repo_dir, package.manifest["id"])
            
            # 安装包
            os.makedirs(target_dir, exist_ok=True)
            package.extract_all(target_dir)
            
            logger.info(f"MPK 包安装成功: {target_dir}")
            return True
            
        except Exception as e:
            logger.error(f"安装 MPK 包失败: {e}")
            return False
    
    def uninstall(self, app_id: str) -> bool:
        """
        卸载 MPK 包
        
        Args:
            app_id: 应用 ID
            
        Returns:
            bool: 卸载是否成功
        """
        logger.info(f"卸载应用: {app_id}")
        
        try:
            # 获取应用目录
            app_dir = os.path.join(self.repo_dir, app_id)
            
            # 检查应用是否存在
            if not os.path.exists(app_dir):
                logger.warning(f"应用不存在: {app_id}")
                return False
            
            # 删除应用目录
            shutil.rmtree(app_dir)
            
            logger.info(f"应用卸载成功: {app_id}")
            return True
            
        except Exception as e:
            logger.error(f"卸载应用失败: {e}")
            return False
    
    def update(self, app_id: str, new_version: Optional[str] = None) -> bool:
        """
        更新 MPK 包
        
        Args:
            app_id: 应用 ID
            new_version: 可选的目标版本
            
        Returns:
            bool: 更新是否成功
        """
        logger.info(f"更新应用: {app_id}")
        
        try:
            # 检查更新
            update_info = self.app_store.check_update(app_id)
            
            if not update_info["available"]:
                logger.info(f"应用已是最新版本: {app_id}")
                return True
            
            # 如果指定了版本，验证版本
            if new_version and new_version != update_info["latest_version"]:
                logger.warning(f"指定的版本 {new_version} 与最新版本 {update_info['latest_version']} 不匹配")
                return False
            
            # 下载并安装更新
            success = self.app_updater.update(app_id)
            
            if success:
                logger.info(f"应用更新成功: {app_id}")
            else:
                logger.error(f"应用更新失败: {app_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"更新应用失败: {e}")
            return False 