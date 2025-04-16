"""
MPK 文件解包器

此模块实现了 MPK 文件解包器，用于将 MPK 文件解包成应用程序。
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

class MPKUnpacker:
    """MPK 文件解包器"""
    
    def __init__(self, mpk_path: str, output_dir: str):
        """
        初始化 MPK 文件解包器
        
        Args:
            mpk_path: MPK 文件路径
            output_dir: 输出目录
        """
        self.mpk_path = mpk_path
        self.output_dir = output_dir
        self.mpk_file = MPKFile()
    
    def unpack(self) -> bool:
        """
        解包 MPK 文件
        
        Returns:
            bool: 解包是否成功
        """
        try:
            # 加载 MPK 文件
            if not self._load_mpk_file():
                return False
            
            # 创建输出目录
            if not self._create_output_dir():
                return False
            
            # 保存元数据
            if not self._save_metadata():
                return False
            
            # 保存代码
            if not self._save_code():
                return False
            
            # 保存资源
            if not self._save_resources():
                return False
            
            # 保存签名
            if not self._save_signature():
                return False
            
            print("解包成功")
            return True
            
        except Exception as e:
            print(f"解包失败：{str(e)}")
            return False
    
    def _load_mpk_file(self) -> bool:
        """
        加载 MPK 文件
        
        Returns:
            bool: 加载是否成功
        """
        try:
            # 加载 MPK 文件
            with open(self.mpk_path, 'rb') as f:
                self.mpk_file.read(f)
            
            return True
            
        except Exception as e:
            print(f"加载 MPK 文件失败：{str(e)}")
            return False
    
    def _create_output_dir(self) -> bool:
        """
        创建输出目录
        
        Returns:
            bool: 创建是否成功
        """
        try:
            # 创建输出目录
            os.makedirs(self.output_dir, exist_ok=True)
            
            # 创建元数据目录
            os.makedirs(os.path.join(self.output_dir, "metadata"), exist_ok=True)
            
            # 创建代码目录
            os.makedirs(os.path.join(self.output_dir, "code"), exist_ok=True)
            
            # 创建资源目录
            os.makedirs(os.path.join(self.output_dir, "resources"), exist_ok=True)
            
            # 创建签名目录
            os.makedirs(os.path.join(self.output_dir, "signature"), exist_ok=True)
            
            return True
            
        except Exception as e:
            print(f"创建输出目录失败：{str(e)}")
            return False
    
    def _save_metadata(self) -> bool:
        """
        保存元数据
        
        Returns:
            bool: 保存是否成功
        """
        try:
            # 保存元数据
            metadata_path = os.path.join(self.output_dir, "metadata", "metadata.json")
            with open(metadata_path, 'w', encoding='utf-8') as f:
                json.dump(self.mpk_file.metadata.to_dict(), f, indent=4)
            
            return True
            
        except Exception as e:
            print(f"保存元数据失败：{str(e)}")
            return False
    
    def _save_code(self) -> bool:
        """
        保存代码
        
        Returns:
            bool: 保存是否成功
        """
        try:
            # 保存代码
            code_path = os.path.join(self.output_dir, "code", "code.bin")
            with open(code_path, 'wb') as f:
                f.write(self.mpk_file.code.data)
            
            # 保存代码类型
            code_type_path = os.path.join(self.output_dir, "code", "code_type.txt")
            with open(code_type_path, 'w', encoding='utf-8') as f:
                f.write(self.mpk_file.code.code_type.name)
            
            return True
            
        except Exception as e:
            print(f"保存代码失败：{str(e)}")
            return False
    
    def _save_resources(self) -> bool:
        """
        保存资源
        
        Returns:
            bool: 保存是否成功
        """
        try:
            # 保存资源
            for resource_path, resource in self.mpk_file.resources.items():
                # 创建资源目录
                resource_dir = os.path.join(self.output_dir, "resources", os.path.dirname(resource_path))
                os.makedirs(resource_dir, exist_ok=True)
                
                # 保存资源
                resource_file_path = os.path.join(self.output_dir, "resources", resource_path)
                with open(resource_file_path, 'wb') as f:
                    f.write(resource.data)
                
                # 保存资源类型
                resource_type_path = os.path.join(self.output_dir, "resources", f"{resource_path}.type")
                with open(resource_type_path, 'w', encoding='utf-8') as f:
                    f.write(resource.resource_type.name)
            
            return True
            
        except Exception as e:
            print(f"保存资源失败：{str(e)}")
            return False
    
    def _save_signature(self) -> bool:
        """
        保存签名
        
        Returns:
            bool: 保存是否成功
        """
        try:
            # 保存签名
            signature_path = os.path.join(self.output_dir, "signature", "signature.bin")
            with open(signature_path, 'wb') as f:
                f.write(self.mpk_file.signature.signature_data)
            
            # 保存签名算法
            signature_algorithm_path = os.path.join(self.output_dir, "signature", "signature_algorithm.txt")
            with open(signature_algorithm_path, 'w', encoding='utf-8') as f:
                f.write(self.mpk_file.signature.signature_algorithm.name)
            
            # 保存证书
            certificate_path = os.path.join(self.output_dir, "signature", "certificate.bin")
            with open(certificate_path, 'wb') as f:
                f.write(self.mpk_file.signature.certificate_data)
            
            return True
            
        except Exception as e:
            print(f"保存签名失败：{str(e)}")
            return False

# 示例用法
if __name__ == "__main__":
    # 创建 MPK 文件解包器
    unpacker = MPKUnpacker("example.mpk", "example_app")
    
    # 解包 MPK 文件
    is_unpacked = unpacker.unpack()
    print("解包结果:", is_unpacked) 