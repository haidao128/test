"""
MPK加载器

用于加载和运行MPK文件
"""

import os
import sys
import json
import tempfile
import importlib
import importlib.util
from typing import Dict, Any, Optional
from . import MPKFile, MPKViewer
from ..sandbox import Sandbox

class MPKLoader:
    def __init__(self, mpk_file: MPKFile):
        self.mpk = mpk_file
        self.viewer = MPKViewer(mpk_file)
        self.sandbox = Sandbox()
        self.temp_dir = None
        self.module = None
        
    def verify(self) -> bool:
        """验证MPK文件"""
        verify_result = self.viewer.verify()
        return all(verify_result.values())
        
    def check_dependencies(self) -> bool:
        """检查依赖项"""
        metadata = self.viewer.get_metadata()
        dependencies = metadata["依赖项"]
        
        for dep in dependencies:
            try:
                importlib.import_module(dep)
            except ImportError:
                print(f"缺少依赖: {dep}")
                return False
                
        return True
        
    def check_permissions(self) -> bool:
        """检查权限"""
        metadata = self.viewer.get_metadata()
        permissions = metadata["权限列表"]
        
        # 检查权限是否在允许范围内
        allowed_permissions = {
            "file.read",
            "file.write",
            "network",
            "process",
            "system"
        }
        
        for perm in permissions:
            if perm not in allowed_permissions:
                print(f"不允许的权限: {perm}")
                return False
                
        return True
        
    def extract_resources(self) -> str:
        """提取资源文件"""
        if not self.temp_dir:
            self.temp_dir = tempfile.mkdtemp()
            self.viewer.extract_resources(self.temp_dir)
        return self.temp_dir
        
    def load_code(self) -> bool:
        """加载代码"""
        try:
            # 创建临时模块
            spec = importlib.util.spec_from_loader(
                "mpk_module",
                loader=None
            )
            self.module = importlib.util.module_from_spec(spec)
            
            # 执行代码
            exec(self.mpk.code_section.code_content, self.module.__dict__)
            
            return True
        except Exception as e:
            print(f"加载代码失败: {e}")
            return False
            
    def setup_environment(self) -> None:
        """设置运行环境"""
        # 设置资源路径
        resource_dir = self.extract_resources()
        sys.path.append(resource_dir)
        
        # 设置环境变量
        os.environ["MPK_RESOURCE_DIR"] = resource_dir
        os.environ["MPK_APP_NAME"] = self.mpk.metadata.app_name
        os.environ["MPK_VERSION"] = self.mpk.metadata.version
        
    def run(self) -> bool:
        """运行MPK文件"""
        try:
            # 验证文件
            if not self.verify():
                print("MPK文件验证失败")
                return False
                
            # 检查依赖
            if not self.check_dependencies():
                print("依赖检查失败")
                return False
                
            # 检查权限
            if not self.check_permissions():
                print("权限检查失败")
                return False
                
            # 设置环境
            self.setup_environment()
            
            # 加载代码
            if not self.load_code():
                print("代码加载失败")
                return False
                
            # 运行入口函数
            entry_point = self.mpk.code_section.entry_point
            if hasattr(self.module, entry_point):
                getattr(self.module, entry_point)()
                return True
            else:
                print(f"找不到入口函数: {entry_point}")
                return False
                
        except Exception as e:
            print(f"运行失败: {e}")
            return False
            
    def cleanup(self) -> None:
        """清理资源"""
        if self.temp_dir and os.path.exists(self.temp_dir):
            import shutil
            shutil.rmtree(self.temp_dir)
            
    def __enter__(self):
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.cleanup()
        
    @classmethod
    def from_file(cls, file_path: str) -> 'MPKLoader':
        """从文件创建加载器"""
        return cls(MPKFile.load(file_path))
        
    @classmethod
    def from_bytes(cls, data: bytes) -> 'MPKLoader':
        """从字节数据创建加载器"""
        return cls(MPKFile.from_bytes(data)) 