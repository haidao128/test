"""
JavaScript API 模块
=================

提供给JavaScript应用使用的API接口，包括文件系统、网络、存储等。

API类别：
1. 文件系统API - 文件和目录操作
2. 网络API - HTTP请求、WebSocket等
3. 数据存储API - 键值存储、数据库等
4. 系统API - 平台信息、应用管理等
5. UI API - 用户界面组件和交互
"""

import os
import json
import base64
import logging
import tempfile
import shutil
import urllib.request
import urllib.error
import threading
import time
from typing import Dict, List, Any, Optional, Callable, Union, Set, Tuple

logger = logging.getLogger("mobile_platform_creator.core.js_runtime.js_api")

class JSAPIBase:
    """JavaScript API基类"""
    
    def __init__(self, runtime=None, sandbox_dir=None):
        """
        初始化API
        
        Args:
            runtime: JavaScript运行时实例
            sandbox_dir: 沙箱目录
        """
        self.runtime = runtime
        self.sandbox_dir = sandbox_dir
        self.namespace = "api"
    
    def get_namespace(self) -> str:
        """获取API命名空间"""
        return self.namespace
    
    def get_methods(self) -> Dict[str, Callable]:
        """获取API方法字典"""
        return {}
    
    def register(self, js_engine) -> None:
        """
        将API注册到JavaScript引擎
        
        Args:
            js_engine: JavaScript引擎实例
        """
        methods = self.get_methods()
        for name, method in methods.items():
            full_name = f"{self.namespace}.{name}"
            js_engine.register_function(full_name, method)


class FileSystemAPI(JSAPIBase):
    """文件系统API"""
    
    def __init__(self, runtime=None, sandbox_dir=None):
        super().__init__(runtime, sandbox_dir)
        self.namespace = "fs"
    
    def get_methods(self) -> Dict[str, Callable]:
        """获取API方法字典"""
        return {
            "readFile": self.read_file,
            "writeFile": self.write_file,
            "exists": self.exists,
            "isDirectory": self.is_directory,
            "isFile": self.is_file,
            "mkdir": self.mkdir,
            "readdir": self.readdir,
            "stat": self.stat,
            "remove": self.remove,
            "rename": self.rename,
            "copyFile": self.copy_file
        }
    
    def _resolve_path(self, path: str) -> str:
        """
        解析路径，确保在沙箱目录内
        
        Args:
            path: 原始路径
            
        Returns:
            str: 解析后的绝对路径
        """
        if not self.sandbox_dir:
            raise ValueError("沙箱目录未设置")
        
        # 处理相对路径
        if not os.path.isabs(path):
            path = os.path.join(self.sandbox_dir, path)
        
        # 规范化路径
        path = os.path.normpath(path)
        
        # 确保路径在沙箱内
        if not path.startswith(self.sandbox_dir):
            raise ValueError(f"路径超出沙箱范围: {path}")
        
        return path
    
    def read_file(self, path: str, encoding: Optional[str] = "utf-8") -> Union[str, bytes]:
        """
        读取文件内容
        
        Args:
            path: 文件路径
            encoding: 编码方式，为None时返回字节数据
            
        Returns:
            Union[str, bytes]: 文件内容
        """
        try:
            real_path = self._resolve_path(path)
            
            if not os.path.exists(real_path):
                raise FileNotFoundError(f"文件不存在: {path}")
            
            if not os.path.isfile(real_path):
                raise ValueError(f"路径不是文件: {path}")
            
            mode = "r" if encoding else "rb"
            with open(real_path, mode, encoding=encoding) as f:
                return f.read()
        except Exception as e:
            logger.error(f"读取文件失败: {path} - {e}")
            raise
    
    def write_file(self, path: str, data: Union[str, bytes], encoding: Optional[str] = "utf-8") -> bool:
        """
        写入文件内容
        
        Args:
            path: 文件路径
            data: 文件内容
            encoding: 编码方式，为None时data应为字节数据
            
        Returns:
            bool: 是否成功
        """
        try:
            real_path = self._resolve_path(path)
            
            # 确保目录存在
            os.makedirs(os.path.dirname(real_path), exist_ok=True)
            
            mode = "w" if encoding else "wb"
            with open(real_path, mode, encoding=encoding) as f:
                f.write(data)
            
            return True
        except Exception as e:
            logger.error(f"写入文件失败: {path} - {e}")
            raise
    
    def exists(self, path: str) -> bool:
        """
        检查文件或目录是否存在
        
        Args:
            path: 路径
            
        Returns:
            bool: 是否存在
        """
        try:
            real_path = self._resolve_path(path)
            return os.path.exists(real_path)
        except Exception as e:
            logger.error(f"检查文件存在失败: {path} - {e}")
            raise
    
    def is_directory(self, path: str) -> bool:
        """
        检查路径是否为目录
        
        Args:
            path: 路径
            
        Returns:
            bool: 是否为目录
        """
        try:
            real_path = self._resolve_path(path)
            return os.path.isdir(real_path)
        except Exception as e:
            logger.error(f"检查是否为目录失败: {path} - {e}")
            raise
    
    def is_file(self, path: str) -> bool:
        """
        检查路径是否为文件
        
        Args:
            path: 路径
            
        Returns:
            bool: 是否为文件
        """
        try:
            real_path = self._resolve_path(path)
            return os.path.isfile(real_path)
        except Exception as e:
            logger.error(f"检查是否为文件失败: {path} - {e}")
            raise
    
    def mkdir(self, path: str, recursive: bool = True) -> bool:
        """
        创建目录
        
        Args:
            path: 目录路径
            recursive: 是否递归创建
            
        Returns:
            bool: 是否成功
        """
        try:
            real_path = self._resolve_path(path)
            os.makedirs(real_path, exist_ok=True)
            return True
        except Exception as e:
            logger.error(f"创建目录失败: {path} - {e}")
            raise
    
    def readdir(self, path: str) -> List[str]:
        """
        读取目录内容
        
        Args:
            path: 目录路径
            
        Returns:
            List[str]: 文件和目录名列表
        """
        try:
            real_path = self._resolve_path(path)
            
            if not os.path.exists(real_path):
                raise FileNotFoundError(f"目录不存在: {path}")
            
            if not os.path.isdir(real_path):
                raise ValueError(f"路径不是目录: {path}")
            
            return os.listdir(real_path)
        except Exception as e:
            logger.error(f"读取目录失败: {path} - {e}")
            raise
    
    def stat(self, path: str) -> Dict[str, Any]:
        """
        获取文件或目录信息
        
        Args:
            path: 路径
            
        Returns:
            Dict[str, Any]: 文件或目录信息
        """
        try:
            real_path = self._resolve_path(path)
            
            if not os.path.exists(real_path):
                raise FileNotFoundError(f"路径不存在: {path}")
            
            stat_result = os.stat(real_path)
            
            return {
                "size": stat_result.st_size,
                "isFile": os.path.isfile(real_path),
                "isDirectory": os.path.isdir(real_path),
                "created": stat_result.st_ctime,
                "modified": stat_result.st_mtime,
                "accessed": stat_result.st_atime
            }
        except Exception as e:
            logger.error(f"获取文件信息失败: {path} - {e}")
            raise
    
    def remove(self, path: str, recursive: bool = False) -> bool:
        """
        删除文件或目录
        
        Args:
            path: 路径
            recursive: 是否递归删除目录
            
        Returns:
            bool: 是否成功
        """
        try:
            real_path = self._resolve_path(path)
            
            if not os.path.exists(real_path):
                raise FileNotFoundError(f"路径不存在: {path}")
            
            if os.path.isfile(real_path):
                os.remove(real_path)
            elif os.path.isdir(real_path):
                if recursive:
                    shutil.rmtree(real_path)
                else:
                    os.rmdir(real_path)
            
            return True
        except Exception as e:
            logger.error(f"删除路径失败: {path} - {e}")
            raise
    
    def rename(self, old_path: str, new_path: str) -> bool:
        """
        重命名文件或目录
        
        Args:
            old_path: 原路径
            new_path: 新路径
            
        Returns:
            bool: 是否成功
        """
        try:
            real_old_path = self._resolve_path(old_path)
            real_new_path = self._resolve_path(new_path)
            
            if not os.path.exists(real_old_path):
                raise FileNotFoundError(f"原路径不存在: {old_path}")
            
            os.rename(real_old_path, real_new_path)
            return True
        except Exception as e:
            logger.error(f"重命名路径失败: {old_path} -> {new_path} - {e}")
            raise
    
    def copy_file(self, source: str, destination: str) -> bool:
        """
        复制文件
        
        Args:
            source: 源文件路径
            destination: 目标文件路径
            
        Returns:
            bool: 是否成功
        """
        try:
            real_source = self._resolve_path(source)
            real_destination = self._resolve_path(destination)
            
            if not os.path.exists(real_source):
                raise FileNotFoundError(f"源文件不存在: {source}")
            
            if not os.path.isfile(real_source):
                raise ValueError(f"源路径不是文件: {source}")
            
            # 确保目标目录存在
            os.makedirs(os.path.dirname(real_destination), exist_ok=True)
            
            shutil.copy2(real_source, real_destination)
            return True
        except Exception as e:
            logger.error(f"复制文件失败: {source} -> {destination} - {e}")
            raise


class NetworkAPI(JSAPIBase):
    """网络API"""
    
    def __init__(self, runtime=None, sandbox_dir=None):
        super().__init__(runtime, sandbox_dir)
        self.namespace = "network"
    
    def get_methods(self) -> Dict[str, Callable]:
        """获取API方法字典"""
        return {
            "httpRequest": self.http_request,
            "httpGet": self.http_get,
            "httpPost": self.http_post
        }
    
    def http_request(self, url: str, options: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        发送HTTP请求
        
        Args:
            url: 请求URL
            options: 请求选项
            
        Returns:
            Dict[str, Any]: 响应信息
        """
        if options is None:
            options = {}
        
        method = options.get("method", "GET").upper()
        headers = options.get("headers", {})
        data = options.get("data")
        timeout = options.get("timeout", 30)
        
        if isinstance(data, dict):
            data = json.dumps(data).encode("utf-8")
            if "content-type" not in headers and "Content-Type" not in headers:
                headers["Content-Type"] = "application/json"
        elif isinstance(data, str):
            data = data.encode("utf-8")
        
        try:
            req = urllib.request.Request(
                url,
                data=data,
                headers=headers,
                method=method
            )
            
            with urllib.request.urlopen(req, timeout=timeout) as response:
                response_data = response.read()
                content_type = response.getheader("Content-Type", "")
                
                result = {
                    "status": response.status,
                    "headers": dict(response.getheaders()),
                    "url": response.url
                }
                
                # 处理不同的内容类型
                if "application/json" in content_type:
                    try:
                        result["data"] = json.loads(response_data.decode("utf-8"))
                    except:
                        result["text"] = response_data.decode("utf-8")
                elif "text/" in content_type:
                    result["text"] = response_data.decode("utf-8")
                else:
                    result["data"] = base64.b64encode(response_data).decode("ascii")
                    result["isBase64Encoded"] = True
                
                return result
        except urllib.error.HTTPError as e:
            return {
                "error": True,
                "status": e.code,
                "headers": dict(e.headers),
                "message": str(e)
            }
        except Exception as e:
            logger.error(f"HTTP请求失败: {url} - {e}")
            return {
                "error": True,
                "message": str(e)
            }
    
    def http_get(self, url: str, headers: Dict[str, str] = None) -> Dict[str, Any]:
        """
        发送HTTP GET请求
        
        Args:
            url: 请求URL
            headers: 请求头
            
        Returns:
            Dict[str, Any]: 响应信息
        """
        options = {
            "method": "GET",
            "headers": headers or {}
        }
        return self.http_request(url, options)
    
    def http_post(self, url: str, data: Any, headers: Dict[str, str] = None) -> Dict[str, Any]:
        """
        发送HTTP POST请求
        
        Args:
            url: 请求URL
            data: 请求数据
            headers: 请求头
            
        Returns:
            Dict[str, Any]: 响应信息
        """
        options = {
            "method": "POST",
            "headers": headers or {},
            "data": data
        }
        return self.http_request(url, options)


class StorageAPI(JSAPIBase):
    """存储API"""
    
    def __init__(self, runtime=None, sandbox_dir=None):
        super().__init__(runtime, sandbox_dir)
        self.namespace = "storage"
        self._storage_file = None
        self._storage_data = {}
        self._storage_lock = threading.Lock()
        
        if sandbox_dir:
            self._storage_file = os.path.join(sandbox_dir, "storage.json")
            self._load_storage()
    
    def get_methods(self) -> Dict[str, Callable]:
        """获取API方法字典"""
        return {
            "getItem": self.get_item,
            "setItem": self.set_item,
            "removeItem": self.remove_item,
            "clear": self.clear,
            "keys": self.keys
        }
    
    def _load_storage(self):
        """加载存储数据"""
        if not self._storage_file:
            return
        
        with self._storage_lock:
            if os.path.exists(self._storage_file):
                try:
                    with open(self._storage_file, "r", encoding="utf-8") as f:
                        self._storage_data = json.load(f)
                except Exception as e:
                    logger.error(f"加载存储数据失败: {e}")
                    self._storage_data = {}
    
    def _save_storage(self):
        """保存存储数据"""
        if not self._storage_file:
            return
        
        with self._storage_lock:
            try:
                os.makedirs(os.path.dirname(self._storage_file), exist_ok=True)
                
                with open(self._storage_file, "w", encoding="utf-8") as f:
                    json.dump(self._storage_data, f)
            except Exception as e:
                logger.error(f"保存存储数据失败: {e}")
    
    def get_item(self, key: str, default_value: Any = None) -> Any:
        """
        获取存储项
        
        Args:
            key: 键
            default_value: 默认值
            
        Returns:
            Any: 值
        """
        with self._storage_lock:
            return self._storage_data.get(key, default_value)
    
    def set_item(self, key: str, value: Any) -> bool:
        """
        设置存储项
        
        Args:
            key: 键
            value: 值
            
        Returns:
            bool: 是否成功
        """
        with self._storage_lock:
            self._storage_data[key] = value
            self._save_storage()
        return True
    
    def remove_item(self, key: str) -> bool:
        """
        删除存储项
        
        Args:
            key: 键
            
        Returns:
            bool: 是否成功
        """
        with self._storage_lock:
            if key in self._storage_data:
                del self._storage_data[key]
                self._save_storage()
                return True
        return False
    
    def clear(self) -> bool:
        """
        清空存储
        
        Returns:
            bool: 是否成功
        """
        with self._storage_lock:
            self._storage_data.clear()
            self._save_storage()
        return True
    
    def keys(self) -> List[str]:
        """
        获取所有键
        
        Returns:
            List[str]: 键列表
        """
        with self._storage_lock:
            return list(self._storage_data.keys())


class SystemAPI(JSAPIBase):
    """系统API"""
    
    def __init__(self, runtime=None, sandbox_dir=None):
        super().__init__(runtime, sandbox_dir)
        self.namespace = "system"
    
    def get_methods(self) -> Dict[str, Callable]:
        """获取API方法字典"""
        return {
            "getPlatformInfo": self.get_platform_info,
            "exit": self.exit,
            "getMemoryUsage": self.get_memory_usage,
            "getAppInfo": self.get_app_info
        }
    
    def get_platform_info(self) -> Dict[str, Any]:
        """
        获取平台信息
        
        Returns:
            Dict[str, Any]: 平台信息
        """
        import platform
        
        return {
            "os": platform.system(),
            "version": platform.version(),
            "architecture": platform.architecture()[0],
            "machine": platform.machine(),
            "python": platform.python_version()
        }
    
    def exit(self, code: int = 0) -> None:
        """
        退出应用
        
        Args:
            code: 退出码
        """
        if self.runtime:
            self.runtime.dispose()
        return None
    
    def get_memory_usage(self) -> Dict[str, Any]:
        """
        获取内存使用情况
        
        Returns:
            Dict[str, Any]: 内存使用情况
        """
        import psutil
        
        process = psutil.Process(os.getpid())
        memory_info = process.memory_info()
        
        return {
            "rss": memory_info.rss,
            "vms": memory_info.vms,
            "percent": process.memory_percent()
        }
    
    def get_app_info(self) -> Dict[str, Any]:
        """
        获取应用信息
        
        Returns:
            Dict[str, Any]: 应用信息
        """
        if not self.runtime:
            return {}
        
        return {
            "id": self.runtime.app_id,
            "workDir": self.runtime.work_dir,
            "engineType": self.runtime.engine_type,
            "isRunning": self.runtime.is_running
        }


# API集合类
class JSAPIs:
    """JavaScript API集合"""
    
    def __init__(self, runtime=None, sandbox_dir=None):
        """
        初始化API集合
        
        Args:
            runtime: JavaScript运行时实例
            sandbox_dir: 沙箱目录
        """
        self.runtime = runtime
        self.sandbox_dir = sandbox_dir
        
        # 创建API实例
        self.file_system_api = FileSystemAPI(runtime, sandbox_dir)
        self.network_api = NetworkAPI(runtime, sandbox_dir)
        self.storage_api = StorageAPI(runtime, sandbox_dir)
        self.system_api = SystemAPI(runtime, sandbox_dir)
        
        # 全部API列表
        self.apis = [
            self.file_system_api,
            self.network_api,
            self.storage_api,
            self.system_api
        ]
    
    def register_all(self, js_engine) -> None:
        """
        注册所有API到JavaScript引擎
        
        Args:
            js_engine: JavaScript引擎实例
        """
        for api in self.apis:
            api.register(js_engine)
    
    def register(self, js_engine, namespace: str) -> bool:
        """
        注册指定命名空间的API到JavaScript引擎
        
        Args:
            js_engine: JavaScript引擎实例
            namespace: API命名空间
            
        Returns:
            bool: 是否成功注册
        """
        for api in self.apis:
            if api.get_namespace() == namespace:
                api.register(js_engine)
                return True
        return False 