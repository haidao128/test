"""
MPK文件格式定义和解析

MPK文件结构:
1. 文件头
   - 魔数: "MPK1"
   - 版本号: 1.0
   - 文件大小
   - 校验和
2. 元数据区
   - 应用信息
   - 权限列表
   - 依赖项
3. 代码区
   - 代码类型
   - 代码内容
4. 资源区
   - 资源类型
   - 资源内容
5. 签名区
   - 签名算法
   - 签名数据
"""

import struct
import json
import hashlib
import zlib
import base64
from typing import Dict, List, Any, Optional
from datetime import datetime
import os
import zipfile
import tempfile
import shutil
import logging

# 魔数
MAGIC_NUMBER = b"MPK1"

# 版本号
VERSION = b"1.0"

# 文件头大小
HEADER_SIZE = 32

# 日志记录器
logger = logging.getLogger("mobile_platform_creator.core.mpk")

# 清单文件必须包含的字段
REQUIRED_MANIFEST_FIELDS = {
    "id", "name", "version", "platform", "min_platform_version"
}

class MPKError(Exception):
    """MPK处理过程中的异常"""
    pass

# 元数据结构
class Metadata:
    def __init__(self):
        self.app_name: str = ""
        self.package_name: str = ""
        self.version: str = ""
        self.version_name: str = ""
        self.author: str = ""
        self.create_date: str = ""
        self.permissions: List[str] = []
        self.dependencies: List[str] = []
        self.min_platform_version: str = ""
        self.icon: str = ""
        self.splash: str = ""
        self.platform: str = "all"  # 新增平台属性，支持"android", "ios", "desktop", "all"
        
    def to_dict(self) -> Dict[str, Any]:
        return {
            "app_name": self.app_name,
            "package_name": self.package_name,
            "version": self.version,
            "version_name": self.version_name,
            "author": self.author,
            "create_date": self.create_date,
            "permissions": self.permissions,
            "dependencies": self.dependencies,
            "min_platform_version": self.min_platform_version,
            "icon": self.icon,
            "splash": self.splash,
            "platform": self.platform
        }
        
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Metadata':
        metadata = cls()
        metadata.app_name = data.get("app_name", "")
        metadata.package_name = data.get("package_name", "")
        metadata.version = data.get("version", "")
        metadata.version_name = data.get("version_name", "")
        metadata.author = data.get("author", "")
        metadata.create_date = data.get("create_date", "")
        metadata.permissions = data.get("permissions", [])
        metadata.dependencies = data.get("dependencies", [])
        metadata.min_platform_version = data.get("min_platform_version", "")
        metadata.icon = data.get("icon", "")
        metadata.splash = data.get("splash", "")
        metadata.platform = data.get("platform", "all")
        return metadata

# 代码区结构
class CodeSection:
    def __init__(self):
        self.code_type: str = ""  # "bytecode", "script", "javascript", "python", "wasm"
        self.code_size: int = 0
        self.code_content: bytes = b""
        self.entry_point: str = ""
        
    def to_dict(self) -> Dict[str, Any]:
        return {
            "code_type": self.code_type,
            "code_size": self.code_size,
            "code_content": base64.b64encode(self.code_content).decode(),
            "entry_point": self.entry_point
        }
        
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'CodeSection':
        code = cls()
        code.code_type = data.get("code_type", "")
        code.code_size = data.get("code_size", 0)
        code.code_content = base64.b64decode(data.get("code_content", ""))
        code.entry_point = data.get("entry_point", "")
        return code

# 资源区结构
class ResourceSection:
    def __init__(self):
        self.resource_type: str = ""
        self.resource_size: int = 0
        self.resource_content: bytes = b""
        self.resource_name: str = ""  # 新增资源名称属性
        
    def to_dict(self) -> Dict[str, Any]:
        return {
            "resource_type": self.resource_type,
            "resource_size": self.resource_size,
            "resource_content": base64.b64encode(self.resource_content).decode(),
            "resource_name": self.resource_name
        }
        
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'ResourceSection':
        resource = cls()
        resource.resource_type = data.get("resource_type", "")
        resource.resource_size = data.get("resource_size", 0)
        resource.resource_content = base64.b64decode(data.get("resource_content", ""))
        resource.resource_name = data.get("resource_name", "")
        return resource

# 签名区结构
class SignatureSection:
    def __init__(self):
        self.algorithm: str = ""
        self.signature_data: bytes = b""
        self.certificate: str = ""
        
    def to_dict(self) -> Dict[str, Any]:
        return {
            "algorithm": self.algorithm,
            "signature_data": base64.b64encode(self.signature_data).decode(),
            "certificate": self.certificate
        }
        
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'SignatureSection':
        signature = cls()
        signature.algorithm = data.get("algorithm", "")
        signature.signature_data = base64.b64decode(data.get("signature_data", ""))
        signature.certificate = data.get("certificate", "")
        return signature

# MPK文件结构
class MPKFile:
    def __init__(self):
        self.magic_number: bytes = MAGIC_NUMBER
        self.version: bytes = VERSION
        self.file_size: int = 0
        self.checksum: bytes = b""
        self.metadata: Metadata = Metadata()
        self.code_section: CodeSection = CodeSection()
        self.resource_sections: List[ResourceSection] = []
        self.signature_section: SignatureSection = SignatureSection()
        self.temp_dir: Optional[str] = None  # 临时目录，用于解压缩后的文件操作
        
    def calculate_checksum(self) -> bytes:
        """计算文件校验和"""
        data = (
            self.magic_number +
            self.version +
            struct.pack(">I", self.file_size) +
            json.dumps(self.metadata.to_dict()).encode() +
            json.dumps(self.code_section.to_dict()).encode() +
            b"".join(json.dumps(r.to_dict()).encode() for r in self.resource_sections) +
            json.dumps(self.signature_section.to_dict()).encode()
        )
        return hashlib.sha256(data).digest()
        
    def pack(self) -> bytes:
        """打包MPK文件"""
        # 序列化各部分
        metadata_data = json.dumps(self.metadata.to_dict()).encode()
        code_data = json.dumps(self.code_section.to_dict()).encode()
        resource_data = b"".join(json.dumps(r.to_dict()).encode() for r in self.resource_sections)
        signature_data = json.dumps(self.signature_section.to_dict()).encode()
        
        # 计算文件大小
        self.file_size = (
            HEADER_SIZE +
            len(metadata_data) +
            len(code_data) +
            len(resource_data) +
            len(signature_data)
        )
        
        # 计算校验和
        self.checksum = self.calculate_checksum()
        
        # 打包文件头
        header = struct.pack(
            ">4s4sI16s",
            self.magic_number,
            self.version,
            self.file_size,
            self.checksum
        )
        
        # 压缩数据
        metadata_data = zlib.compress(metadata_data)
        code_data = zlib.compress(code_data)
        resource_data = zlib.compress(resource_data)
        signature_data = zlib.compress(signature_data)
        
        # 组合所有数据
        return header + metadata_data + code_data + resource_data + signature_data
        
    @classmethod
    def unpack(cls, data: bytes) -> 'MPKFile':
        """解包MPK文件"""
        mpk = cls()
        
        # 解析文件头
        header = data[:HEADER_SIZE]
        mpk.magic_number, mpk.version, mpk.file_size, mpk.checksum = struct.unpack(
            ">4s4sI16s",
            header
        )
        
        # 验证魔数和版本
        if mpk.magic_number != MAGIC_NUMBER:
            raise ValueError("Invalid magic number")
        if mpk.version != VERSION:
            raise ValueError("Unsupported version")
            
        # 解压数据
        metadata_data = zlib.decompress(data[HEADER_SIZE:HEADER_SIZE+len(data)-HEADER_SIZE])
        code_data = zlib.decompress(data[HEADER_SIZE+len(metadata_data):])
        resource_data = zlib.decompress(data[HEADER_SIZE+len(metadata_data)+len(code_data):])
        signature_data = zlib.decompress(data[HEADER_SIZE+len(metadata_data)+len(code_data)+len(resource_data):])
        
        # 解析各部分
        mpk.metadata = Metadata.from_dict(json.loads(metadata_data))
        mpk.code_section = CodeSection.from_dict(json.loads(code_data))
        mpk.resource_sections = [
            ResourceSection.from_dict(r)
            for r in json.loads(resource_data)
        ]
        mpk.signature_section = SignatureSection.from_dict(json.loads(signature_data))
        
        return mpk
    
    def save(self, file_path: str) -> None:
        """保存MPK文件到磁盘"""
        with open(file_path, "wb") as f:
            f.write(self.pack())
    
    @staticmethod
    def load(file_path: str) -> 'MPKFile':
        """从磁盘加载MPK文件"""
        with open(file_path, "rb") as f:
            data = f.read()
        return MPKFile.unpack(data)
    
    def verify(self) -> bool:
        """验证MPK文件的完整性和签名"""
        # 这里应该实现真实的签名验证
        # 目前仅返回True作为占位符
        return True
    
    def extract_to_dir(self, target_dir: str) -> None:
        """将MPK文件解压到指定目录（用于Android安装）
        
        Args:
            target_dir: 目标目录
        """
        # 确保目录存在
        os.makedirs(target_dir, exist_ok=True)
        
        # 创建和写入清单文件
        manifest_path = os.path.join(target_dir, "manifest.json")
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(self.metadata.to_dict(), f, ensure_ascii=False, indent=2)
        
        # 创建代码目录
        code_dir = os.path.join(target_dir, "code")
        os.makedirs(code_dir, exist_ok=True)
        
        # 写入代码文件
        code_path = os.path.join(code_dir, f"main.{self.code_section.code_type}")
        with open(code_path, "wb") as f:
            f.write(self.code_section.code_content)
        
        # 创建资源目录
        assets_dir = os.path.join(target_dir, "assets")
        os.makedirs(assets_dir, exist_ok=True)
        
        # 写入资源文件
        for i, resource in enumerate(self.resource_sections):
            if resource.resource_name:
                resource_path = os.path.join(assets_dir, resource.resource_name)
            else:
                resource_path = os.path.join(assets_dir, f"resource_{i}.{resource.resource_type}")
            
            with open(resource_path, "wb") as f:
                f.write(resource.resource_content)
        
        # 写入签名文件
        signature_path = os.path.join(target_dir, "signature.sig")
        with open(signature_path, "wb") as f:
            f.write(self.signature_section.signature_data)
    
    def to_zip_file(self, output_path: str) -> None:
        """将MPK文件转换为ZIP文件（用于Android安装）
        
        Args:
            output_path: 输出ZIP文件路径
        """
        # 创建临时目录
        self.temp_dir = tempfile.mkdtemp(prefix="mpk_")
        
        try:
            # 提取到临时目录
            self.extract_to_dir(self.temp_dir)
            
            # 创建ZIP文件
            with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as zip_file:
                for root, _, files in os.walk(self.temp_dir):
                    for file in files:
                        file_path = os.path.join(root, file)
                        rel_path = os.path.relpath(file_path, self.temp_dir)
                        zip_file.write(file_path, rel_path)
        finally:
            # 清理临时目录
            if self.temp_dir and os.path.exists(self.temp_dir):
                shutil.rmtree(self.temp_dir)
                self.temp_dir = None
    
    @staticmethod
    def from_zip_file(zip_file_path: str) -> 'MPKFile':
        """从ZIP文件创建MPK文件（用于从Android导入）
        
        Args:
            zip_file_path: ZIP文件路径
            
        Returns:
            MPKFile: MPK文件对象
        """
        mpk = MPKFile()
        
        # 创建临时目录
        temp_dir = tempfile.mkdtemp(prefix="mpk_")
        
        try:
            # 解压ZIP文件到临时目录
            with zipfile.ZipFile(zip_file_path, "r") as zip_file:
                zip_file.extractall(temp_dir)
            
            # 读取清单文件
            manifest_path = os.path.join(temp_dir, "manifest.json")
            if not os.path.exists(manifest_path):
                raise MPKError("ZIP文件缺少manifest.json")
            
            with open(manifest_path, "r", encoding="utf-8") as f:
                manifest_data = json.load(f)
                mpk.metadata = Metadata.from_dict(manifest_data)
            
            # 读取代码文件
            code_dir = os.path.join(temp_dir, "code")
            if not os.path.exists(code_dir) or not os.listdir(code_dir):
                raise MPKError("ZIP文件缺少代码目录或代码文件")
            
            # 寻找主代码文件
            main_code_file = None
            code_type = None
            for file in os.listdir(code_dir):
                if file.startswith("main."):
                    main_code_file = os.path.join(code_dir, file)
                    code_type = file.split(".")[-1]
                    break
            
            if not main_code_file or not code_type:
                # 如果没有找到main.*文件，使用第一个找到的文件
                first_file = os.listdir(code_dir)[0]
                main_code_file = os.path.join(code_dir, first_file)
                code_type = first_file.split(".")[-1]
            
            # 读取代码内容
            with open(main_code_file, "rb") as f:
                code_content = f.read()
            
            mpk.code_section.code_type = code_type
            mpk.code_section.code_content = code_content
            mpk.code_section.code_size = len(code_content)
            mpk.code_section.entry_point = os.path.basename(main_code_file)
            
            # 读取资源文件
            assets_dir = os.path.join(temp_dir, "assets")
            if os.path.exists(assets_dir):
                for file in os.listdir(assets_dir):
                    file_path = os.path.join(assets_dir, file)
                    if os.path.isfile(file_path):
                        resource = ResourceSection()
                        resource.resource_name = file
                        resource.resource_type = file.split(".")[-1] if "." in file else "bin"
                        
                        with open(file_path, "rb") as f:
                            resource.resource_content = f.read()
                        
                        resource.resource_size = len(resource.resource_content)
                        mpk.resource_sections.append(resource)
            
            # 读取签名文件
            signature_path = os.path.join(temp_dir, "signature.sig")
            if os.path.exists(signature_path):
                with open(signature_path, "rb") as f:
                    mpk.signature_section.signature_data = f.read()
                    mpk.signature_section.algorithm = "SHA256"
            
            return mpk
        finally:
            # 清理临时目录
            if os.path.exists(temp_dir):
                shutil.rmtree(temp_dir)
    
    def get_permissions(self) -> List[str]:
        """获取应用权限列表"""
        return self.metadata.permissions
    
    def get_android_package_name(self) -> str:
        """获取Android包名"""
        return self.metadata.package_name

# 兼容性检查方法
def is_compatible_with_android(mpk_file: MPKFile) -> bool:
    """检查MPK文件是否与Android兼容
    
    Args:
        mpk_file: MPK文件对象
        
    Returns:
        bool: 是否兼容
    """
    # 检查平台是否包含android
    platform = mpk_file.metadata.platform.lower()
    if platform != "all" and platform != "android" and "android" not in platform.split(","):
        return False
    
    # 检查必要的元数据
    if not mpk_file.metadata.package_name:
        return False
    
    # 检查代码类型
    supported_code_types = ["javascript", "wasm", "bytecode"]
    if mpk_file.code_section.code_type.lower() not in supported_code_types:
        return False
    
    return True

# 检查MPK文件是否有效
def is_valid_mpk(file_path: str) -> bool:
    """检查文件是否是有效的MPK文件
    
    Args:
        file_path: 文件路径
        
    Returns:
        bool: 是否是有效的MPK文件
    """
    if not os.path.exists(file_path):
        return False
    
    try:
        with open(file_path, "rb") as f:
            # 读取文件头
            header = f.read(HEADER_SIZE)
            
            # 检查魔数
            magic = header[:4]
            if magic != MAGIC_NUMBER:
                return False
            
            # 检查版本
            version = header[4:8]
            if version != VERSION:
                return False
            
            return True
    except Exception:
        return False 