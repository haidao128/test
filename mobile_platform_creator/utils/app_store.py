"""
应用商店管理模块
=============

管理移动应用平台的应用商店功能，支持应用的上传、搜索、下载和更新。

主要功能：
1. 应用仓库管理
2. 应用搜索和查询
3. 应用下载和安装
4. 应用更新检测和升级
"""

import os
import json
import time
import logging
import requests
import shutil
import tempfile
from typing import Dict, List, Any, Optional, Tuple, Set, Union
from urllib.parse import urljoin

from .mpk_package import MPKPackage, is_valid_mpk

logger = logging.getLogger("mobile_platform_creator.utils.app_store")

class AppStoreError(Exception):
    """应用商店相关异常"""
    pass

class AppStoreClient:
    """应用商店客户端"""
    
    def __init__(self, api_url: Optional[str] = None, repo_dir: Optional[str] = None):
        """
        初始化应用商店客户端
        
        Args:
            api_url: API地址，默认为None (使用官方地址)
            repo_dir: 本地应用仓库目录，默认为None (自动选择)
        """
        # 设置API地址
        if api_url:
            self.api_url = api_url
        else:
            self.api_url = "https://appstore.mobileplatform.io/api/v1/"
        
        # 设置本地仓库目录
        if repo_dir:
            self.repo_dir = repo_dir
        else:
            self.repo_dir = os.path.expanduser("~/MobilePlatform/apps")
        
        # 确保目录存在
        os.makedirs(self.repo_dir, exist_ok=True)
        
        # 本地应用索引
        self.index_path = os.path.join(self.repo_dir, "app_index.json")
        self.local_index = self._load_local_index()
        
        # 本地应用仓库
        self.local_repo = LocalAppRepository(self.repo_dir)
    
    def _load_local_index(self) -> Dict[str, Any]:
        """
        加载本地应用索引
        
        Returns:
            Dict[str, Any]: 索引数据
        """
        if os.path.exists(self.index_path):
            try:
                with open(self.index_path, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                logger.error("加载本地应用索引失败: %s", e)
        
        # 默认索引
        return {
            "apps": {},
            "last_update": 0
        }
    
    def _save_local_index(self) -> bool:
        """
        保存本地应用索引
        
        Returns:
            bool: 保存是否成功
        """
        try:
            with open(self.index_path, 'w', encoding='utf-8') as f:
                json.dump(self.local_index, f, ensure_ascii=False, indent=2)
            return True
        except Exception as e:
            logger.error("保存本地应用索引失败: %s", e)
            return False
    
    def search_apps(self, query: str, category: Optional[str] = None, 
                  limit: int = 20, offset: int = 0) -> Dict[str, Any]:
        """
        搜索应用
        
        Args:
            query: 搜索关键词
            category: 应用类别，可选
            limit: 返回结果数量限制
            offset: 结果偏移量
            
        Returns:
            Dict[str, Any]: 搜索结果
        """
        try:
            # 构建请求参数
            params = {
                "q": query,
                "limit": limit,
                "offset": offset
            }
            
            if category:
                params["category"] = category
            
            # 发送请求
            # 注意：这里应该实际调用API，这里只是模拟
            # response = requests.get(urljoin(self.api_url, "search"), params=params)
            # response.raise_for_status()
            # return response.json()
            
            # 模拟搜索结果
            return {
                "total": 1,
                "results": [
                    {
                        "id": "com.example.demoapp",
                        "name": "示例应用",
                        "version": "1.0.0",
                        "description": "这是一个示例应用",
                        "category": "工具",
                        "author": "示例开发者",
                        "rating": 4.5,
                        "download_count": 1000,
                        "icon_url": "https://example.com/icon.png",
                        "updated_at": int(time.time()) - 86400  # 1天前
                    }
                ]
            }
        except Exception as e:
            logger.error("搜索应用失败: %s", e)
            raise AppStoreError(f"搜索应用失败: {e}")
    
    def get_app_details(self, app_id: str) -> Dict[str, Any]:
        """
        获取应用详情
        
        Args:
            app_id: 应用ID
            
        Returns:
            Dict[str, Any]: 应用详情
        """
        try:
            # 发送请求
            # 注意：这里应该实际调用API，这里只是模拟
            # response = requests.get(urljoin(self.api_url, f"apps/{app_id}"))
            # response.raise_for_status()
            # return response.json()
            
            # 模拟应用详情
            return {
                "id": app_id,
                "name": "示例应用",
                "version": "1.0.0",
                "description": "这是一个示例应用的详细描述。它展示了移动应用平台的功能。",
                "category": "工具",
                "author": "示例开发者",
                "rating": 4.5,
                "download_count": 1000,
                "icon_url": "https://example.com/icon.png",
                "screenshots": [
                    "https://example.com/screenshot1.png",
                    "https://example.com/screenshot2.png"
                ],
                "size": 1024 * 1024,  # 1MB
                "permissions": ["camera", "storage"],
                "min_platform_version": "1.0.0",
                "platform": "desktop",
                "updated_at": int(time.time()) - 86400,  # 1天前
                "download_url": "https://example.com/app.mpk"
            }
        except Exception as e:
            logger.error("获取应用详情失败: %s", e)
            raise AppStoreError(f"获取应用详情失败: {e}")
    
    def download_app(self, app_id: str, target_dir: Optional[str] = None) -> str:
        """
        下载应用
        
        Args:
            app_id: 应用ID
            target_dir: 目标目录，默认为None（使用本地仓库目录）
            
        Returns:
            str: 下载文件路径
        """
        try:
            # 获取应用详情
            app_details = self.get_app_details(app_id)
            
            # 检查是否已有最新版本
            if app_id in self.local_index["apps"]:
                local_version = self.local_index["apps"][app_id]["version"]
                if local_version == app_details["version"]:
                    # 已有最新版本，直接返回本地路径
                    local_path = self.local_index["apps"][app_id]["local_path"]
                    if os.path.exists(local_path):
                        logger.info("已有最新版本，跳过下载: %s", app_id)
                        return local_path
            
            # 设置目标目录
            if not target_dir:
                target_dir = self.repo_dir
            
            # 确保目标目录存在
            os.makedirs(target_dir, exist_ok=True)
            
            # 构建目标文件路径
            filename = f"{app_id}_{app_details['version']}.mpk"
            target_path = os.path.join(target_dir, filename)
            
            # 下载文件
            # 注意：这里应该实际下载文件，这里只是模拟
            # response = requests.get(app_details["download_url"], stream=True)
            # response.raise_for_status()
            # with open(target_path, 'wb') as f:
            #     for chunk in response.iter_content(chunk_size=8192):
            #         f.write(chunk)
            
            # 模拟下载
            logger.info("模拟下载应用: %s", app_id)
            time.sleep(1)  # 模拟下载延迟
            
            # 创建模拟的MPK包
            temp_dir = tempfile.mkdtemp(prefix="mpk_")
            try:
                # 创建目录结构
                os.makedirs(os.path.join(temp_dir, "code"), exist_ok=True)
                os.makedirs(os.path.join(temp_dir, "assets"), exist_ok=True)
                os.makedirs(os.path.join(temp_dir, "config"), exist_ok=True)
                
                # 创建清单文件
                manifest = {
                    "id": app_id,
                    "name": app_details["name"],
                    "version": app_details["version"],
                    "description": app_details["description"],
                    "author": app_details["author"],
                    "min_platform_version": app_details["min_platform_version"],
                    "platform": app_details["platform"],
                    "permissions": app_details["permissions"]
                }
                
                manifest_path = os.path.join(temp_dir, "manifest.json")
                with open(manifest_path, 'w', encoding='utf-8') as f:
                    json.dump(manifest, f, ensure_ascii=False, indent=2)
                
                # 创建签名文件
                with open(os.path.join(temp_dir, "signature.sig"), 'w') as f:
                    f.write("SIMULATION_SIGNATURE")
                
                # 创建示例文件
                with open(os.path.join(temp_dir, "code", "main.wasm"), 'w') as f:
                    f.write("示例WASM模块")
                
                with open(os.path.join(temp_dir, "assets", "icon.png"), 'w') as f:
                    f.write("示例图标")
                
                # 创建ZIP文件
                import zipfile
                with zipfile.ZipFile(target_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
                    for root, _, files in os.walk(temp_dir):
                        for file in files:
                            file_path = os.path.join(root, file)
                            rel_path = os.path.relpath(file_path, temp_dir)
                            zipf.write(file_path, rel_path)
            finally:
                # 清理临时目录
                shutil.rmtree(temp_dir)
            
            # 更新本地索引
            self.local_index["apps"][app_id] = {
                "name": app_details["name"],
                "version": app_details["version"],
                "local_path": target_path,
                "download_time": int(time.time()),
                "installed": False
            }
            
            self._save_local_index()
            
            logger.info("已下载应用: %s -> %s", app_id, target_path)
            return target_path
        except Exception as e:
            logger.error("下载应用失败: %s", e)
            raise AppStoreError(f"下载应用失败: {e}")
    
    def install_app(self, app_id: str, target_dir: Optional[str] = None) -> Dict[str, Any]:
        """
        安装应用
        
        Args:
            app_id: 应用ID
            target_dir: 安装目标目录，默认为None（使用默认目录）
            
        Returns:
            Dict[str, Any]: 安装结果
        """
        try:
            # 下载应用（如果尚未下载）
            if app_id not in self.local_index["apps"]:
                mpk_path = self.download_app(app_id)
            else:
                mpk_path = self.local_index["apps"][app_id]["local_path"]
                if not os.path.exists(mpk_path):
                    mpk_path = self.download_app(app_id)
            
            # 检查MPK文件是否有效
            if not is_valid_mpk(mpk_path):
                raise AppStoreError(f"无效的MPK文件: {mpk_path}")
            
            # 解析MPK包
            package = MPKPackage(mpk_path)
            manifest = package.get_manifest()
            
            # 设置安装目录
            if not target_dir:
                target_dir = os.path.expanduser("~/MobilePlatform/installed")
            
            # 确保安装目录存在
            app_install_dir = os.path.join(target_dir, app_id)
            os.makedirs(app_install_dir, exist_ok=True)
            
            # 安装应用
            # 方法1：解压整个MPK包
            # 方法2：只提取需要的文件（这里使用方法1）
            
            # 清理旧文件
            for item in os.listdir(app_install_dir):
                item_path = os.path.join(app_install_dir, item)
                if os.path.isdir(item_path):
                    shutil.rmtree(item_path)
                else:
                    os.remove(item_path)
            
            # 提取目录
            for dir_name in ["code", "assets", "config"]:
                if any(f.startswith(f"{dir_name}/") for f in package.list_files()):
                    source_dir = os.path.join(package.temp_dir, dir_name)
                    target_dir = os.path.join(app_install_dir, dir_name)
                    if os.path.exists(source_dir):
                        shutil.copytree(source_dir, target_dir)
            
            # 复制清单文件
            shutil.copy2(
                os.path.join(package.temp_dir, "manifest.json"),
                os.path.join(app_install_dir, "manifest.json")
            )
            
            # 更新本地索引
            self.local_index["apps"][app_id]["installed"] = True
            self.local_index["apps"][app_id]["install_dir"] = app_install_dir
            self.local_index["apps"][app_id]["install_time"] = int(time.time())
            self._save_local_index()
            
            logger.info("已安装应用: %s -> %s", app_id, app_install_dir)
            
            return {
                "app_id": app_id,
                "version": manifest["version"],
                "install_dir": app_install_dir,
                "result": "success"
            }
        except Exception as e:
            logger.error("安装应用失败: %s", e)
            raise AppStoreError(f"安装应用失败: {e}")
    
    def uninstall_app(self, app_id: str) -> bool:
        """
        卸载应用
        
        Args:
            app_id: 应用ID
            
        Returns:
            bool: 卸载是否成功
        """
        try:
            # 检查应用是否已安装
            if app_id not in self.local_index["apps"] or not self.local_index["apps"][app_id].get("installed", False):
                logger.warning("应用未安装: %s", app_id)
                return False
            
            # 获取安装目录
            install_dir = self.local_index["apps"][app_id]["install_dir"]
            
            # 删除安装目录
            if os.path.exists(install_dir):
                shutil.rmtree(install_dir)
            
            # 更新本地索引
            self.local_index["apps"][app_id]["installed"] = False
            self.local_index["apps"][app_id].pop("install_dir", None)
            self.local_index["apps"][app_id]["uninstall_time"] = int(time.time())
            self._save_local_index()
            
            logger.info("已卸载应用: %s", app_id)
            return True
        except Exception as e:
            logger.error("卸载应用失败: %s", e)
            return False
    
    def check_updates(self) -> Dict[str, Dict[str, Any]]:
        """
        检查应用更新
        
        Returns:
            Dict[str, Dict[str, Any]]: 有更新的应用字典，以应用ID为键
        """
        updates = {}
        
        try:
            # 遍历已安装的应用
            for app_id, app_info in self.local_index["apps"].items():
                if app_info.get("installed", False):
                    # 获取远程应用详情
                    remote_info = self.get_app_details(app_id)
                    
                    # 比较版本
                    local_version = app_info["version"]
                    remote_version = remote_info["version"]
                    
                    if remote_version != local_version:
                        updates[app_id] = {
                            "app_id": app_id,
                            "name": remote_info["name"],
                            "current_version": local_version,
                            "latest_version": remote_version,
                            "update_time": remote_info["updated_at"],
                            "size": remote_info["size"]
                        }
            
            logger.info("发现 %d 个应用有更新", len(updates))
            return updates
        except Exception as e:
            logger.error("检查应用更新失败: %s", e)
            return {}
    
    def update_app(self, app_id: str) -> Dict[str, Any]:
        """
        更新应用
        
        Args:
            app_id: 应用ID
            
        Returns:
            Dict[str, Any]: 更新结果
        """
        try:
            # 获取应用详情
            app_details = self.get_app_details(app_id)
            
            # 检查是否需要更新
            if app_id in self.local_index["apps"]:
                local_version = self.local_index["apps"][app_id]["version"]
                if local_version == app_details["version"]:
                    logger.info("应用已是最新版本: %s", app_id)
                    return {
                        "app_id": app_id,
                        "version": local_version,
                        "result": "no_update",
                        "message": "应用已是最新版本"
                    }
            
            # 下载最新版本
            mpk_path = self.download_app(app_id)
            
            # 安装应用
            install_result = self.install_app(app_id)
            
            return {
                "app_id": app_id,
                "old_version": local_version if app_id in self.local_index["apps"] else None,
                "new_version": app_details["version"],
                "result": "success",
                "install_dir": install_result["install_dir"]
            }
        except Exception as e:
            logger.error("更新应用失败: %s", e)
            raise AppStoreError(f"更新应用失败: {e}")
    
    def get_featured_apps(self, limit: int = 10) -> List[Dict[str, Any]]:
        """
        获取推荐应用
        
        Args:
            limit: 返回结果数量限制
            
        Returns:
            List[Dict[str, Any]]: 推荐应用列表
        """
        try:
            # 发送请求
            # 注意：这里应该实际调用API，这里只是模拟
            # response = requests.get(urljoin(self.api_url, "featured"), params={"limit": limit})
            # response.raise_for_status()
            # return response.json()
            
            # 模拟推荐应用
            return [
                {
                    "id": "com.example.featured1",
                    "name": "推荐应用1",
                    "version": "1.0.0",
                    "description": "这是一个推荐应用",
                    "category": "工具",
                    "rating": 4.8,
                    "icon_url": "https://example.com/icon1.png"
                },
                {
                    "id": "com.example.featured2",
                    "name": "推荐应用2",
                    "version": "2.1.0",
                    "description": "这是另一个推荐应用",
                    "category": "娱乐",
                    "rating": 4.5,
                    "icon_url": "https://example.com/icon2.png"
                }
            ]
        except Exception as e:
            logger.error("获取推荐应用失败: %s", e)
            return []
    
    def get_categories(self) -> List[Dict[str, str]]:
        """
        获取应用分类
        
        Returns:
            List[Dict[str, str]]: 分类列表
        """
        try:
            # 发送请求
            # 注意：这里应该实际调用API，这里只是模拟
            # response = requests.get(urljoin(self.api_url, "categories"))
            # response.raise_for_status()
            # return response.json()
            
            # 模拟分类列表
            return [
                {"id": "tools", "name": "工具"},
                {"id": "games", "name": "游戏"},
                {"id": "education", "name": "教育"},
                {"id": "entertainment", "name": "娱乐"},
                {"id": "business", "name": "商务"},
                {"id": "productivity", "name": "效率"},
                {"id": "lifestyle", "name": "生活方式"},
                {"id": "social", "name": "社交"}
            ]
        except Exception as e:
            logger.error("获取应用分类失败: %s", e)
            return []


class LocalAppRepository:
    """本地应用仓库"""
    
    def __init__(self, repo_dir: str):
        """
        初始化本地应用仓库
        
        Args:
            repo_dir: 仓库目录
        """
        self.repo_dir = repo_dir
        os.makedirs(repo_dir, exist_ok=True)
    
    def get_app_path(self, app_id: str, version: Optional[str] = None) -> Optional[str]:
        """
        获取应用文件路径
        
        Args:
            app_id: 应用ID
            version: 应用版本，默认为None（获取最新版本）
            
        Returns:
            Optional[str]: 应用文件路径，如果不存在则返回None
        """
        # 查找应用文件
        app_files = []
        for filename in os.listdir(self.repo_dir):
            if filename.startswith(f"{app_id}_") and filename.endswith(".mpk"):
                file_path = os.path.join(self.repo_dir, filename)
                app_version = filename[len(app_id)+1:-4]  # 提取版本号
                app_files.append((file_path, app_version))
        
        if not app_files:
            return None
        
        if version:
            # 查找指定版本
            for file_path, app_version in app_files:
                if app_version == version:
                    return file_path
            return None
        else:
            # 返回最新版本
            # 这里简单地按字符串排序，实际上应该使用版本比较库
            app_files.sort(key=lambda x: x[1], reverse=True)
            return app_files[0][0]
    
    def add_app(self, mpk_path: str) -> Dict[str, Any]:
        """
        添加应用到仓库
        
        Args:
            mpk_path: MPK文件路径
            
        Returns:
            Dict[str, Any]: 应用信息
        """
        # 检查MPK文件是否有效
        if not is_valid_mpk(mpk_path):
            raise AppStoreError(f"无效的MPK文件: {mpk_path}")
        
        # 解析MPK包
        package = MPKPackage(mpk_path)
        manifest = package.get_manifest()
        
        app_id = manifest["id"]
        version = manifest["version"]
        
        # 构建目标文件路径
        target_filename = f"{app_id}_{version}.mpk"
        target_path = os.path.join(self.repo_dir, target_filename)
        
        # 复制文件
        if mpk_path != target_path:
            shutil.copy2(mpk_path, target_path)
        
        logger.info("已添加应用到仓库: %s v%s", app_id, version)
        
        return {
            "app_id": app_id,
            "name": manifest["name"],
            "version": version,
            "path": target_path
        }
    
    def remove_app(self, app_id: str, version: Optional[str] = None) -> bool:
        """
        从仓库移除应用
        
        Args:
            app_id: 应用ID
            version: 应用版本，默认为None（移除所有版本）
            
        Returns:
            bool: 移除是否成功
        """
        # 移除应用文件
        if version:
            # 移除指定版本
            target_path = self.get_app_path(app_id, version)
            if target_path and os.path.exists(target_path):
                os.remove(target_path)
                logger.info("已从仓库移除应用: %s v%s", app_id, version)
                return True
            return False
        else:
            # 移除所有版本
            removed = False
            for filename in os.listdir(self.repo_dir):
                if filename.startswith(f"{app_id}_") and filename.endswith(".mpk"):
                    file_path = os.path.join(self.repo_dir, filename)
                    os.remove(file_path)
                    removed = True
            
            if removed:
                logger.info("已从仓库移除应用的所有版本: %s", app_id)
            
            return removed
    
    def list_apps(self) -> Dict[str, List[Dict[str, Any]]]:
        """
        列出仓库中的所有应用
        
        Returns:
            Dict[str, List[Dict[str, Any]]]: 应用列表，以应用ID为键
        """
        apps = {}
        
        for filename in os.listdir(self.repo_dir):
            if filename.endswith(".mpk"):
                file_path = os.path.join(self.repo_dir, filename)
                
                # 尝试解析应用ID和版本
                parts = filename[:-4].split("_", 1)
                if len(parts) == 2:
                    app_id, version = parts
                    
                    # 尝试解析MPK文件
                    try:
                        if is_valid_mpk(file_path):
                            package = MPKPackage(file_path)
                            manifest = package.get_manifest()
                            
                            app_info = {
                                "version": version,
                                "name": manifest.get("name", "未知"),
                                "path": file_path,
                                "size": os.path.getsize(file_path),
                                "modified_time": os.path.getmtime(file_path)
                            }
                            
                            if app_id not in apps:
                                apps[app_id] = []
                            
                            apps[app_id].append(app_info)
                    except Exception as e:
                        logger.warning("解析MPK文件失败: %s - %s", file_path, e)
        
        # 按版本排序
        for app_id in apps:
            apps[app_id].sort(key=lambda x: x["version"], reverse=True)
        
        return apps 