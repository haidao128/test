"""
MPK 文件生成器

此模块实现了 MPK 文件生成器，用于生成 MPK 文件。
"""

import os
import json
import struct
import hashlib
import zlib
from typing import Dict, List, Any, Optional, Tuple, Set, Union, BinaryIO, Callable
from pathlib import Path
from .mpk_format import (
    MPKFile, MPKHeader, MPKMetadata, MPKCode, MPKResource, MPKSignature,
    ChecksumAlgorithm, CodeType, ResourceType, SignatureAlgorithm
)

class MPKGenerator:
    """MPK 文件生成器"""
    
    def __init__(self):
        """初始化 MPK 文件生成器"""
        self.mpk_file = MPKFile()
    
    def set_metadata(self, metadata: Dict[str, Any]):
        """
        设置元数据
        
        Args:
            metadata: 元数据字典
        """
        self.mpk_file.metadata = MPKMetadata.from_dict(metadata)
    
    def set_code(self, code_type: CodeType, code_data: bytes):
        """
        设置代码
        
        Args:
            code_type: 代码类型
            code_data: 代码数据
        """
        self.mpk_file.code.code_type = code_type
        self.mpk_file.code.set_code(code_data)
    
    def add_resource(self, path: str, resource_type: ResourceType, resource_data: bytes):
        """
        添加资源
        
        Args:
            path: 资源路径
            resource_type: 资源类型
            resource_data: 资源数据
        """
        resource = MPKResource(path)
        resource.resource_type = resource_type
        resource.set_resource(resource_data)
        self.mpk_file.add_resource(resource)
    
    def set_signature(self, signature_algorithm: SignatureAlgorithm, signature_data: bytes, certificate_data: bytes):
        """
        设置签名
        
        Args:
            signature_algorithm: 签名算法
            signature_data: 签名数据
            certificate_data: 证书数据
        """
        self.mpk_file.signature.signature_algorithm = signature_algorithm
        self.mpk_file.signature.set_signature(signature_data)
        self.mpk_file.signature.set_certificate(certificate_data)
    
    def generate(self, output_path: str):
        """
        生成 MPK 文件
        
        Args:
            output_path: 输出路径
        """
        # 计算文件大小
        file_size = 0
        file_size += MPKHeader.HEADER_SIZE
        file_size += len(json.dumps(self.mpk_file.metadata.to_dict()).encode('utf-8'))
        file_size += len(self.mpk_file.code.code_data)
        for resource in self.mpk_file.resources.values():
            file_size += len(resource.resource_data)
        file_size += len(self.mpk_file.signature.signature_data)
        file_size += len(self.mpk_file.signature.certificate_data)
        
        # 设置文件头
        self.mpk_file.header.file_size = file_size
        
        # 计算校验和
        data = b""
        data += json.dumps(self.mpk_file.metadata.to_dict()).encode('utf-8')
        data += self.mpk_file.code.code_data
        for resource in self.mpk_file.resources.values():
            data += resource.resource_data
        data += self.mpk_file.signature.signature_data
        data += self.mpk_file.signature.certificate_data
        
        self.mpk_file.header.checksum = self.mpk_file.calculate_checksum(data)
        
        # 写入文件
        with open(output_path, 'wb') as f:
            # 写入文件头
            f.write(self.mpk_file.header.pack())
            
            # 写入元数据
            f.write(json.dumps(self.mpk_file.metadata.to_dict()).encode('utf-8'))
            
            # 写入代码
            f.write(self.mpk_file.code.code_data)
            
            # 写入资源
            for resource in self.mpk_file.resources.values():
                f.write(resource.resource_data)
            
            # 写入签名
            f.write(self.mpk_file.signature.signature_data)
            f.write(self.mpk_file.signature.certificate_data)

# 示例用法
if __name__ == "__main__":
    # 创建 MPK 文件生成器
    generator = MPKGenerator()
    
    # 设置元数据
    metadata = {
        "app_id": "com.example.app",
        "app_name": "Example App",
        "version": "1.0.0",
        "version_name": "1.0.0",
        "author": "Example Author",
        "description": "Example App Description",
        "category": "Example Category",
        "tags": ["example", "app"],
        "permissions": ["permission1", "permission2"],
        "dependencies": ["dependency1", "dependency2"],
        "min_platform_version": "1.0.0",
        "entry_point": "main.py",
        "icon": "icon.png",
        "splash_screen": "splash.png"
    }
    generator.set_metadata(metadata)
    
    # 设置代码
    code_type = CodeType.PYTHON
    code_data = b"print('Hello, World!')"
    generator.set_code(code_type, code_data)
    
    # 添加资源
    resource_path = "icon.png"
    resource_type = ResourceType.IMAGE
    resource_data = b"PNG\x00\x00\x00\x00"
    generator.add_resource(resource_path, resource_type, resource_data)
    
    # 设置签名
    signature_algorithm = SignatureAlgorithm.RSA
    signature_data = b"signature"
    certificate_data = b"certificate"
    generator.set_signature(signature_algorithm, signature_data, certificate_data)
    
    # 生成 MPK 文件
    generator.generate("example.mpk") 