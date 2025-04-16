"""
MPK 文件打包器

此模块实现了 MPK 文件打包器，用于将应用程序打包成 MPK 文件。
"""

import os
import json
import shutil
import subprocess
from typing import Dict, List, Any, Optional, Tuple, Set, Union, BinaryIO, Callable
from pathlib import Path
from .mpk_format import (
    MPKFile, MPKHeader, MPKMetadata, MPKCode, MPKResource, MPKSignature,
    ChecksumAlgorithm, CodeType, ResourceType, SignatureAlgorithm
)

class MPKPackager:
    """MPK 文件打包器"""
    
    def __init__(self, app_dir: str, output_path: str):
        """
        初始化 MPK 文件打包器
        
        Args:
            app_dir: 应用程序目录
            output_path: 输出路径
        """
        self.app_dir = app_dir
        self.output_path = output_path
        self.mpk_file = MPKFile()
    
    def package(self) -> bool:
        """
        打包 MPK 文件
        
        Returns:
            bool: 打包是否成功
        """
        try:
            # 加载元数据
            if not self._load_metadata():
                return False
            
            # 加载代码
            if not self._load_code():
                return False
            
            # 加载资源
            if not self._load_resources():
                return False
            
            # 加载签名
            if not self._load_signature():
                return False
            
            # 保存 MPK 文件
            if not self._save_mpk_file():
                return False
            
            print("打包成功")
            return True
            
        except Exception as e:
            print(f"打包失败：{str(e)}")
            return False
    
    def _load_metadata(self) -> bool:
        """
        加载元数据
        
        Returns:
            bool: 加载是否成功
        """
        try:
            # 加载元数据
            metadata_path = os.path.join(self.app_dir, "metadata", "metadata.json")
            with open(metadata_path, 'r', encoding='utf-8') as f:
                metadata_dict = json.load(f)
            
            # 设置元数据
            self.mpk_file.metadata = MPKMetadata.from_dict(metadata_dict)
            
            return True
            
        except Exception as e:
            print(f"加载元数据失败：{str(e)}")
            return False
    
    def _load_code(self) -> bool:
        """
        加载代码
        
        Returns:
            bool: 加载是否成功
        """
        try:
            # 加载代码
            code_path = os.path.join(self.app_dir, "code", "code.bin")
            with open(code_path, 'rb') as f:
                code_data = f.read()
            
            # 加载代码类型
            code_type_path = os.path.join(self.app_dir, "code", "code_type.txt")
            with open(code_type_path, 'r', encoding='utf-8') as f:
                code_type = CodeType[f.read().strip()]
            
            # 设置代码
            self.mpk_file.code = MPKCode(code_data, code_type)
            
            return True
            
        except Exception as e:
            print(f"加载代码失败：{str(e)}")
            return False
    
    def _load_resources(self) -> bool:
        """
        加载资源
        
        Returns:
            bool: 加载是否成功
        """
        try:
            # 加载资源
            resources_dir = os.path.join(self.app_dir, "resources")
            for root, dirs, files in os.walk(resources_dir):
                for file in files:
                    if file.endswith(".type"):
                        continue
                    
                    # 获取资源路径
                    resource_path = os.path.relpath(os.path.join(root, file), resources_dir)
                    
                    # 加载资源
                    with open(os.path.join(root, file), 'rb') as f:
                        resource_data = f.read()
                    
                    # 加载资源类型
                    resource_type_path = os.path.join(root, f"{file}.type")
                    with open(resource_type_path, 'r', encoding='utf-8') as f:
                        resource_type = ResourceType[f.read().strip()]
                    
                    # 设置资源
                    self.mpk_file.resources[resource_path] = MPKResource(resource_data, resource_type)
            
            return True
            
        except Exception as e:
            print(f"加载资源失败：{str(e)}")
            return False
    
    def _load_signature(self) -> bool:
        """
        加载签名
        
        Returns:
            bool: 加载是否成功
        """
        try:
            # 加载签名
            signature_path = os.path.join(self.app_dir, "signature", "signature.bin")
            with open(signature_path, 'rb') as f:
                signature_data = f.read()
            
            # 加载签名算法
            signature_algorithm_path = os.path.join(self.app_dir, "signature", "signature_algorithm.txt")
            with open(signature_algorithm_path, 'r', encoding='utf-8') as f:
                signature_algorithm = SignatureAlgorithm[f.read().strip()]
            
            # 加载证书
            certificate_path = os.path.join(self.app_dir, "signature", "certificate.bin")
            with open(certificate_path, 'rb') as f:
                certificate_data = f.read()
            
            # 设置签名
            self.mpk_file.signature = MPKSignature(signature_data, signature_algorithm, certificate_data)
            
            return True
            
        except Exception as e:
            print(f"加载签名失败：{str(e)}")
            return False
    
    def _save_mpk_file(self) -> bool:
        """
        保存 MPK 文件
        
        Returns:
            bool: 保存是否成功
        """
        try:
            # 保存 MPK 文件
            with open(self.output_path, 'wb') as f:
                self.mpk_file.write(f)
            
            return True
            
        except Exception as e:
            print(f"保存 MPK 文件失败：{str(e)}")
            return False

# 示例用法
if __name__ == "__main__":
    # 创建 MPK 文件打包器
    packager = MPKPackager("example_app", "example.mpk")
    
    # 打包 MPK 文件
    is_packaged = packager.package()
    print("打包结果:", is_packaged) 