"""
MPK 文件安装器

此模块实现了 MPK 文件安装器，用于安装 MPK 文件到移动平台。
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
from .mpk_validator import MPKValidator

class MPKInstaller:
    """MPK 文件安装器"""
    
    def __init__(self, mpk_path: str, install_dir: str):
        """
        初始化 MPK 文件安装器
        
        Args:
            mpk_path: MPK 文件路径
            install_dir: 安装目录
        """
        self.mpk_path = mpk_path
        self.install_dir = install_dir
        self.mpk_file = MPKFile()
        self.validator = MPKValidator(mpk_path)
    
    def install(self) -> bool:
        """
        安装 MPK 文件
        
        Returns:
            bool: 安装是否成功
        """
        try:
            # 验证 MPK 文件
            if not self.validator.validate():
                print("安装失败：MPK 文件验证失败")
                return False
            
            # 创建安装目录
            os.makedirs(self.install_dir, exist_ok=True)
            
            # 安装元数据
            if not self._install_metadata():
                return False
            
            # 安装代码
            if not self._install_code():
                return False
            
            # 安装资源
            if not self._install_resources():
                return False
            
            # 安装签名
            if not self._install_signature():
                return False
            
            print("安装成功")
            return True
            
        except Exception as e:
            print(f"安装失败：{str(e)}")
            return False
    
    def _install_metadata(self) -> bool:
        """
        安装元数据
        
        Returns:
            bool: 安装是否成功
        """
        try:
            # 创建元数据目录
            metadata_dir = os.path.join(self.install_dir, "metadata")
            os.makedirs(metadata_dir, exist_ok=True)
            
            # 保存元数据
            metadata_path = os.path.join(metadata_dir, "metadata.json")
            with open(metadata_path, 'w', encoding='utf-8') as f:
                json.dump(self.mpk_file.metadata.to_dict(), f, ensure_ascii=False, indent=2)
            
            return True
            
        except Exception as e:
            print(f"安装元数据失败：{str(e)}")
            return False
    
    def _install_code(self) -> bool:
        """
        安装代码
        
        Returns:
            bool: 安装是否成功
        """
        try:
            # 创建代码目录
            code_dir = os.path.join(self.install_dir, "code")
            os.makedirs(code_dir, exist_ok=True)
            
            # 保存代码
            code_path = os.path.join(code_dir, "code.bin")
            with open(code_path, 'wb') as f:
                f.write(self.mpk_file.code.code_data)
            
            # 保存代码类型
            code_type_path = os.path.join(code_dir, "code_type.txt")
            with open(code_type_path, 'w', encoding='utf-8') as f:
                f.write(self.mpk_file.code.code_type.name)
            
            return True
            
        except Exception as e:
            print(f"安装代码失败：{str(e)}")
            return False
    
    def _install_resources(self) -> bool:
        """
        安装资源
        
        Returns:
            bool: 安装是否成功
        """
        try:
            # 创建资源目录
            resources_dir = os.path.join(self.install_dir, "resources")
            os.makedirs(resources_dir, exist_ok=True)
            
            # 保存资源
            for path, resource in self.mpk_file.resources.items():
                # 创建资源目录
                resource_dir = os.path.join(resources_dir, os.path.dirname(path))
                os.makedirs(resource_dir, exist_ok=True)
                
                # 保存资源
                resource_path = os.path.join(resources_dir, path)
                with open(resource_path, 'wb') as f:
                    f.write(resource.resource_data)
                
                # 保存资源类型
                resource_type_path = os.path.join(resources_dir, f"{path}.type")
                with open(resource_type_path, 'w', encoding='utf-8') as f:
                    f.write(resource.resource_type.name)
            
            return True
            
        except Exception as e:
            print(f"安装资源失败：{str(e)}")
            return False
    
    def _install_signature(self) -> bool:
        """
        安装签名
        
        Returns:
            bool: 安装是否成功
        """
        try:
            # 创建签名目录
            signature_dir = os.path.join(self.install_dir, "signature")
            os.makedirs(signature_dir, exist_ok=True)
            
            # 保存签名
            signature_path = os.path.join(signature_dir, "signature.bin")
            with open(signature_path, 'wb') as f:
                f.write(self.mpk_file.signature.signature_data)
            
            # 保存签名算法
            signature_algorithm_path = os.path.join(signature_dir, "signature_algorithm.txt")
            with open(signature_algorithm_path, 'w', encoding='utf-8') as f:
                f.write(self.mpk_file.signature.signature_algorithm.name)
            
            # 保存证书
            certificate_path = os.path.join(signature_dir, "certificate.bin")
            with open(certificate_path, 'wb') as f:
                f.write(self.mpk_file.signature.certificate_data)
            
            return True
            
        except Exception as e:
            print(f"安装签名失败：{str(e)}")
            return False
    
    def uninstall(self) -> bool:
        """
        卸载 MPK 文件
        
        Returns:
            bool: 卸载是否成功
        """
        try:
            # 删除安装目录
            if os.path.exists(self.install_dir):
                shutil.rmtree(self.install_dir)
            
            print("卸载成功")
            return True
            
        except Exception as e:
            print(f"卸载失败：{str(e)}")
            return False

# 示例用法
if __name__ == "__main__":
    # 创建 MPK 文件安装器
    installer = MPKInstaller("example.mpk", "example_app")
    
    # 安装 MPK 文件
    is_installed = installer.install()
    print("安装结果:", is_installed)
    
    # 卸载 MPK 文件
    is_uninstalled = installer.uninstall()
    print("卸载结果:", is_uninstalled) 