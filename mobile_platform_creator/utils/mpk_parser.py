"""
MPK 文件解析器

此模块实现了 MPK 文件解析器，用于解析 MPK 文件。
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

class MPKParser:
    """MPK 文件解析器"""
    
    def __init__(self, mpk_path: str):
        """
        初始化 MPK 文件解析器
        
        Args:
            mpk_path: MPK 文件路径
        """
        self.mpk_path = mpk_path
        self.mpk_file = MPKFile()
    
    def parse(self) -> MPKFile:
        """
        解析 MPK 文件
        
        Returns:
            MPKFile: MPK 文件对象
        """
        with open(self.mpk_path, 'rb') as f:
            # 解析文件头
            header_data = f.read(MPKHeader.HEADER_SIZE)
            self.mpk_file.header = MPKHeader.unpack(header_data)
            
            # 验证魔数
            if self.mpk_file.header.magic != MPKHeader.MAGIC:
                raise ValueError("Invalid MPK file: magic number mismatch")
            
            # 验证文件大小
            file_size = os.path.getsize(self.mpk_path)
            if file_size != self.mpk_file.header.file_size:
                raise ValueError("Invalid MPK file: file size mismatch")
            
            # 解析元数据
            metadata_size = struct.unpack('<I', f.read(4))[0]
            metadata_data = f.read(metadata_size)
            metadata_dict = json.loads(metadata_data.decode('utf-8'))
            self.mpk_file.metadata = MPKMetadata.from_dict(metadata_dict)
            
            # 解析代码
            code_type = CodeType(f.read(1)[0])
            code_size = struct.unpack('<I', f.read(4))[0]
            code_data = f.read(code_size)
            self.mpk_file.code.code_type = code_type
            self.mpk_file.code.set_code(code_data)
            
            # 解析资源
            resource_count = struct.unpack('<I', f.read(4))[0]
            for _ in range(resource_count):
                path_size = struct.unpack('<I', f.read(4))[0]
                path = f.read(path_size).decode('utf-8')
                resource_type = ResourceType(f.read(1)[0])
                resource_size = struct.unpack('<I', f.read(4))[0]
                resource_data = f.read(resource_size)
                
                resource = MPKResource(path)
                resource.resource_type = resource_type
                resource.set_resource(resource_data)
                self.mpk_file.add_resource(resource)
            
            # 解析签名
            signature_algorithm = SignatureAlgorithm(f.read(1)[0])
            signature_size = struct.unpack('<I', f.read(4))[0]
            signature_data = f.read(signature_size)
            certificate_size = struct.unpack('<I', f.read(4))[0]
            certificate_data = f.read(certificate_size)
            
            self.mpk_file.signature.signature_algorithm = signature_algorithm
            self.mpk_file.signature.set_signature(signature_data)
            self.mpk_file.signature.set_certificate(certificate_data)
            
            # 验证校验和
            data = b""
            data += metadata_data
            data += bytes([code_type.value])
            data += struct.pack('<I', code_size)
            data += code_data
            data += struct.pack('<I', resource_count)
            for resource in self.mpk_file.resources.values():
                data += struct.pack('<I', len(resource.path))
                data += resource.path.encode('utf-8')
                data += bytes([resource.resource_type.value])
                data += struct.pack('<I', len(resource.resource_data))
                data += resource.resource_data
            data += bytes([signature_algorithm.value])
            data += struct.pack('<I', signature_size)
            data += signature_data
            data += struct.pack('<I', certificate_size)
            data += certificate_data
            
            checksum = self.mpk_file.calculate_checksum(data)
            if checksum != self.mpk_file.header.checksum:
                raise ValueError("Invalid MPK file: checksum mismatch")
            
            return self.mpk_file
    
    def extract(self, output_dir: str):
        """
        解压 MPK 文件到指定目录
        
        Args:
            output_dir: 输出目录
        """
        # 创建输出目录
        os.makedirs(output_dir, exist_ok=True)
        
        # 写入元数据
        metadata_path = os.path.join(output_dir, "manifest.json")
        with open(metadata_path, 'w', encoding='utf-8') as f:
            json.dump(self.mpk_file.metadata.to_dict(), f, ensure_ascii=False, indent=2)
        
        # 写入代码
        code_dir = os.path.join(output_dir, "code")
        os.makedirs(code_dir, exist_ok=True)
        code_path = os.path.join(code_dir, self.mpk_file.metadata.entry_point)
        with open(code_path, 'wb') as f:
            f.write(self.mpk_file.code.code_data)
        
        # 写入资源
        assets_dir = os.path.join(output_dir, "assets")
        os.makedirs(assets_dir, exist_ok=True)
        for path, resource in self.mpk_file.resources.items():
            resource_path = os.path.join(assets_dir, path)
            os.makedirs(os.path.dirname(resource_path), exist_ok=True)
            with open(resource_path, 'wb') as f:
                f.write(resource.resource_data)
        
        # 写入签名
        if self.mpk_file.signature.signature_data:
            signature_path = os.path.join(output_dir, "signature.sig")
            with open(signature_path, 'wb') as f:
                f.write(self.mpk_file.signature.signature_data)
        
        # 写入证书
        if self.mpk_file.signature.certificate_data:
            certificate_path = os.path.join(output_dir, "certificate.pem")
            with open(certificate_path, 'wb') as f:
                f.write(self.mpk_file.signature.certificate_data)

# 示例用法
if __name__ == "__main__":
    # 创建 MPK 文件解析器
    parser = MPKParser("example.mpk")
    
    # 解析 MPK 文件
    mpk_file = parser.parse()
    
    # 打印元数据
    print("App ID:", mpk_file.metadata.app_id)
    print("App Name:", mpk_file.metadata.app_name)
    print("Version:", mpk_file.metadata.version)
    print("Version Name:", mpk_file.metadata.version_name)
    print("Author:", mpk_file.metadata.author)
    print("Description:", mpk_file.metadata.description)
    print("Category:", mpk_file.metadata.category)
    print("Tags:", mpk_file.metadata.tags)
    print("Permissions:", mpk_file.metadata.permissions)
    print("Dependencies:", mpk_file.metadata.dependencies)
    print("Min Platform Version:", mpk_file.metadata.min_platform_version)
    print("Entry Point:", mpk_file.metadata.entry_point)
    print("Icon:", mpk_file.metadata.icon)
    print("Splash Screen:", mpk_file.metadata.splash_screen)
    
    # 打印代码信息
    print("Code Type:", mpk_file.code.code_type)
    print("Code Size:", len(mpk_file.code.code_data))
    
    # 打印资源信息
    print("Resource Count:", len(mpk_file.resources))
    for path, resource in mpk_file.resources.items():
        print("Resource Path:", path)
        print("Resource Type:", resource.resource_type)
        print("Resource Size:", len(resource.resource_data))
    
    # 打印签名信息
    print("Signature Algorithm:", mpk_file.signature.signature_algorithm)
    print("Signature Size:", len(mpk_file.signature.signature_data))
    print("Certificate Size:", len(mpk_file.signature.certificate_data))
    
    # 解压 MPK 文件
    parser.extract("example_app") 