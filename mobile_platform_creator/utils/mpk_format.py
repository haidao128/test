"""
MPK 文件格式定义

此模块定义了 MPK 文件的结构和格式，包括文件头、元数据、代码、资源和签名等。
"""

import os
import json
import struct
import hashlib
import enum
from typing import Dict, List, Any, Optional, Tuple, Set, Union, BinaryIO, Callable
from pathlib import Path

class ChecksumAlgorithm(enum.Enum):
    """校验和算法"""
    MD5 = 0
    SHA1 = 1
    SHA256 = 2
    SHA512 = 3

class CodeType(enum.Enum):
    """代码类型"""
    PYTHON = 0
    JAVASCRIPT = 1
    HTML = 2
    CSS = 3
    BINARY = 4

class ResourceType(enum.Enum):
    """资源类型"""
    IMAGE = 0
    AUDIO = 1
    VIDEO = 2
    FONT = 3
    OTHER = 4

class SignatureAlgorithm(enum.Enum):
    """签名算法"""
    RSA = 0
    DSA = 1
    ECDSA = 2

class MPKHeader:
    """MPK 文件头"""
    
    HEADER_SIZE = 32
    MAGIC = b"MPK\x00"
    
    def __init__(self):
        """初始化 MPK 文件头"""
        self.magic = self.MAGIC
        self.version = 1
        self.file_size = 0
        self.checksum = b""
        self.checksum_algorithm = ChecksumAlgorithm.SHA256
    
    def pack(self) -> bytes:
        """
        打包文件头
        
        Returns:
            bytes: 打包后的文件头
        """
        return struct.pack(
            "<4sII16sI",
            self.magic,
            self.version,
            self.file_size,
            self.checksum,
            self.checksum_algorithm.value
        )
    
    @classmethod
    def unpack(cls, data: bytes) -> 'MPKHeader':
        """
        解包文件头
        
        Args:
            data: 打包后的文件头
        
        Returns:
            MPKHeader: 解包后的文件头
        """
        header = cls()
        magic, version, file_size, checksum, checksum_algorithm = struct.unpack(
            "<4sII16sI",
            data
        )
        
        if magic != cls.MAGIC:
            raise ValueError("无效的 MPK 文件")
        
        header.version = version
        header.file_size = file_size
        header.checksum = checksum
        header.checksum_algorithm = ChecksumAlgorithm(checksum_algorithm)
        
        return header

class MPKMetadata:
    """MPK 元数据"""
    
    def __init__(self):
        """初始化 MPK 元数据"""
        self.app_id = ""
        self.app_name = ""
        self.version = ""
        self.version_name = ""
        self.author = ""
        self.description = ""
        self.category = ""
        self.tags = []
        self.permissions = []
        self.dependencies = []
        self.min_platform_version = ""
        self.entry_point = ""
        self.icon = ""
        self.splash_screen = ""
    
    def to_dict(self) -> Dict[str, Any]:
        """
        转换为字典
        
        Returns:
            Dict[str, Any]: 字典
        """
        return {
            "app_id": self.app_id,
            "app_name": self.app_name,
            "version": self.version,
            "version_name": self.version_name,
            "author": self.author,
            "description": self.description,
            "category": self.category,
            "tags": self.tags,
            "permissions": self.permissions,
            "dependencies": self.dependencies,
            "min_platform_version": self.min_platform_version,
            "entry_point": self.entry_point,
            "icon": self.icon,
            "splash_screen": self.splash_screen
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'MPKMetadata':
        """
        从字典创建元数据
        
        Args:
            data: 字典
        
        Returns:
            MPKMetadata: 元数据
        """
        metadata = cls()
        metadata.app_id = data.get("app_id", "")
        metadata.app_name = data.get("app_name", "")
        metadata.version = data.get("version", "")
        metadata.version_name = data.get("version_name", "")
        metadata.author = data.get("author", "")
        metadata.description = data.get("description", "")
        metadata.category = data.get("category", "")
        metadata.tags = data.get("tags", [])
        metadata.permissions = data.get("permissions", [])
        metadata.dependencies = data.get("dependencies", [])
        metadata.min_platform_version = data.get("min_platform_version", "")
        metadata.entry_point = data.get("entry_point", "")
        metadata.icon = data.get("icon", "")
        metadata.splash_screen = data.get("splash_screen", "")
        
        return metadata

class MPKCode:
    """MPK 代码"""
    
    def __init__(self):
        """初始化 MPK 代码"""
        self.code_type = CodeType.PYTHON
        self.code_data = b""
    
    def set_code(self, code_data: bytes):
        """
        设置代码
        
        Args:
            code_data: 代码数据
        """
        self.code_data = code_data
    
    def get_code(self) -> bytes:
        """
        获取代码
        
        Returns:
            bytes: 代码数据
        """
        return self.code_data

class MPKResource:
    """MPK 资源"""
    
    def __init__(self, path: str):
        """
        初始化 MPK 资源
        
        Args:
            path: 资源路径
        """
        self.path = path
        self.resource_type = ResourceType.OTHER
        self.resource_data = b""
    
    def set_resource(self, resource_data: bytes):
        """
        设置资源
        
        Args:
            resource_data: 资源数据
        """
        self.resource_data = resource_data
    
    def get_resource(self) -> bytes:
        """
        获取资源
        
        Returns:
            bytes: 资源数据
        """
        return self.resource_data

class MPKSignature:
    """MPK 签名"""
    
    def __init__(self):
        """初始化 MPK 签名"""
        self.signature_algorithm = SignatureAlgorithm.RSA
        self.signature_data = b""
        self.certificate_data = b""
    
    def set_signature(self, signature_data: bytes):
        """
        设置签名
        
        Args:
            signature_data: 签名数据
        """
        self.signature_data = signature_data
    
    def set_certificate(self, certificate_data: bytes):
        """
        设置证书
        
        Args:
            certificate_data: 证书数据
        """
        self.certificate_data = certificate_data
    
    def get_signature(self) -> bytes:
        """
        获取签名
        
        Returns:
            bytes: 签名数据
        """
        return self.signature_data
    
    def get_certificate(self) -> bytes:
        """
        获取证书
        
        Returns:
            bytes: 证书数据
        """
        return self.certificate_data

class MPKFile:
    """MPK 文件"""
    
    def __init__(self):
        """初始化 MPK 文件"""
        self.header = MPKHeader()
        self.metadata = MPKMetadata()
        self.code = MPKCode()
        self.resources = {}
        self.signature = MPKSignature()
    
    def add_resource(self, resource: MPKResource):
        """
        添加资源
        
        Args:
            resource: 资源
        """
        self.resources[resource.path] = resource
    
    def get_resource(self, path: str) -> Optional[MPKResource]:
        """
        获取资源
        
        Args:
            path: 资源路径
        
        Returns:
            Optional[MPKResource]: 资源
        """
        return self.resources.get(path)
    
    def calculate_checksum(self, data: bytes) -> bytes:
        """
        计算校验和
        
        Args:
            data: 数据
        
        Returns:
            bytes: 校验和
        """
        if self.header.checksum_algorithm == ChecksumAlgorithm.MD5:
            return hashlib.md5(data).digest()
        elif self.header.checksum_algorithm == ChecksumAlgorithm.SHA1:
            return hashlib.sha1(data).digest()
        elif self.header.checksum_algorithm == ChecksumAlgorithm.SHA256:
            return hashlib.sha256(data).digest()
        elif self.header.checksum_algorithm == ChecksumAlgorithm.SHA512:
            return hashlib.sha512(data).digest()
        else:
            raise ValueError("不支持的校验和算法")
    
    def verify_signature(self) -> bool:
        """
        验证签名
        
        Returns:
            bool: 签名是否有效
        """
        # TODO: 实现签名验证
        return True 