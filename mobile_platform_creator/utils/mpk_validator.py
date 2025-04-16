"""
MPK 文件验证器

此模块实现了 MPK 文件验证器，用于验证 MPK 文件的完整性和签名。
"""

import os
import json
import hashlib
import subprocess
from typing import Dict, List, Any, Optional, Tuple, Set, Union, BinaryIO, Callable
from pathlib import Path
from .mpk_format import (
    MPKFile, MPKHeader, MPKMetadata, MPKCode, MPKResource, MPKSignature,
    ChecksumAlgorithm, CodeType, ResourceType, SignatureAlgorithm
)

class MPKValidator:
    """MPK 文件验证器"""
    
    def __init__(self, mpk_path: str):
        """
        初始化 MPK 文件验证器
        
        Args:
            mpk_path: MPK 文件路径
        """
        self.mpk_path = mpk_path
        self.mpk_file = MPKFile()
    
    def validate(self) -> bool:
        """
        验证 MPK 文件
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 加载 MPK 文件
            if not self._load_mpk_file():
                return False
            
            # 验证文件头
            if not self._validate_header():
                return False
            
            # 验证元数据
            if not self._validate_metadata():
                return False
            
            # 验证代码
            if not self._validate_code():
                return False
            
            # 验证资源
            if not self._validate_resources():
                return False
            
            # 验证签名
            if not self._validate_signature():
                return False
            
            print("验证成功")
            return True
            
        except Exception as e:
            print(f"验证失败：{str(e)}")
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
    
    def _validate_header(self) -> bool:
        """
        验证文件头
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 验证魔数
            if self.mpk_file.header.magic != b'MPK\x00':
                print("魔数错误")
                return False
            
            # 验证版本
            if self.mpk_file.header.version != 1:
                print("版本错误")
                return False
            
            # 验证校验和算法
            if self.mpk_file.header.checksum_algorithm not in ChecksumAlgorithm:
                print("校验和算法错误")
                return False
            
            # 验证校验和
            if not self._verify_checksum():
                print("校验和错误")
                return False
            
            return True
            
        except Exception as e:
            print(f"验证文件头失败：{str(e)}")
            return False
    
    def _verify_checksum(self) -> bool:
        """
        验证校验和
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 计算校验和
            checksum = self._calculate_checksum()
            
            # 验证校验和
            return checksum == self.mpk_file.header.checksum
            
        except Exception as e:
            print(f"验证校验和失败：{str(e)}")
            return False
    
    def _calculate_checksum(self) -> bytes:
        """
        计算校验和
        
        Returns:
            bytes: 校验和
        """
        try:
            # 根据校验和算法计算校验和
            if self.mpk_file.header.checksum_algorithm == ChecksumAlgorithm.MD5:
                return hashlib.md5(self.mpk_file.header.data).digest()
            elif self.mpk_file.header.checksum_algorithm == ChecksumAlgorithm.SHA1:
                return hashlib.sha1(self.mpk_file.header.data).digest()
            elif self.mpk_file.header.checksum_algorithm == ChecksumAlgorithm.SHA256:
                return hashlib.sha256(self.mpk_file.header.data).digest()
            else:
                raise ValueError(f"不支持的校验和算法：{self.mpk_file.header.checksum_algorithm}")
            
        except Exception as e:
            print(f"计算校验和失败：{str(e)}")
            return b''
    
    def _validate_metadata(self) -> bool:
        """
        验证元数据
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 验证应用 ID
            if not self.mpk_file.metadata.app_id:
                print("应用 ID 为空")
                return False
            
            # 验证应用名称
            if not self.mpk_file.metadata.app_name:
                print("应用名称为空")
                return False
            
            # 验证应用版本
            if not self.mpk_file.metadata.app_version:
                print("应用版本为空")
                return False
            
            # 验证应用描述
            if not self.mpk_file.metadata.app_description:
                print("应用描述为空")
                return False
            
            # 验证应用图标
            if not self.mpk_file.metadata.app_icon:
                print("应用图标为空")
                return False
            
            # 验证应用作者
            if not self.mpk_file.metadata.app_author:
                print("应用作者为空")
                return False
            
            # 验证应用许可证
            if not self.mpk_file.metadata.app_license:
                print("应用许可证为空")
                return False
            
            return True
            
        except Exception as e:
            print(f"验证元数据失败：{str(e)}")
            return False
    
    def _validate_code(self) -> bool:
        """
        验证代码
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 验证代码数据
            if not self.mpk_file.code.data:
                print("代码数据为空")
                return False
            
            # 验证代码类型
            if self.mpk_file.code.code_type not in CodeType:
                print("代码类型错误")
                return False
            
            return True
            
        except Exception as e:
            print(f"验证代码失败：{str(e)}")
            return False
    
    def _validate_resources(self) -> bool:
        """
        验证资源
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 验证资源
            for resource_path, resource in self.mpk_file.resources.items():
                # 验证资源数据
                if not resource.data:
                    print(f"资源数据为空：{resource_path}")
                    return False
                
                # 验证资源类型
                if resource.resource_type not in ResourceType:
                    print(f"资源类型错误：{resource_path}")
                    return False
            
            return True
            
        except Exception as e:
            print(f"验证资源失败：{str(e)}")
            return False
    
    def _validate_signature(self) -> bool:
        """
        验证签名
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 验证签名数据
            if not self.mpk_file.signature.signature_data:
                print("签名数据为空")
                return False
            
            # 验证签名算法
            if self.mpk_file.signature.signature_algorithm not in SignatureAlgorithm:
                print("签名算法错误")
                return False
            
            # 验证证书
            if not self.mpk_file.signature.certificate_data:
                print("证书为空")
                return False
            
            # 验证签名
            if not self._verify_signature():
                print("签名验证失败")
                return False
            
            return True
            
        except Exception as e:
            print(f"验证签名失败：{str(e)}")
            return False
    
    def _verify_signature(self) -> bool:
        """
        验证签名
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 根据签名算法验证签名
            if self.mpk_file.signature.signature_algorithm == SignatureAlgorithm.RSA:
                return self._verify_rsa_signature()
            elif self.mpk_file.signature.signature_algorithm == SignatureAlgorithm.ECDSA:
                return self._verify_ecdsa_signature()
            else:
                raise ValueError(f"不支持的签名算法：{self.mpk_file.signature.signature_algorithm}")
            
        except Exception as e:
            print(f"验证签名失败：{str(e)}")
            return False
    
    def _verify_rsa_signature(self) -> bool:
        """
        验证 RSA 签名
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 使用 OpenSSL 验证 RSA 签名
            # 注意：这里需要安装 OpenSSL
            cmd = [
                "openssl", "rsautl", "-verify", "-pubin", "-inkey", "public_key.pem",
                "-in", "signature.bin", "-raw"
            ]
            result = subprocess.run(cmd, capture_output=True, text=True)
            return result.returncode == 0
            
        except Exception as e:
            print(f"验证 RSA 签名失败：{str(e)}")
            return False
    
    def _verify_ecdsa_signature(self) -> bool:
        """
        验证 ECDSA 签名
        
        Returns:
            bool: 验证是否成功
        """
        try:
            # 使用 OpenSSL 验证 ECDSA 签名
            # 注意：这里需要安装 OpenSSL
            cmd = [
                "openssl", "dgst", "-verify", "public_key.pem", "-signature", "signature.bin",
                "data.bin"
            ]
            result = subprocess.run(cmd, capture_output=True, text=True)
            return result.returncode == 0
            
        except Exception as e:
            print(f"验证 ECDSA 签名失败：{str(e)}")
            return False

# 示例用法
if __name__ == "__main__":
    # 创建 MPK 文件验证器
    validator = MPKValidator("example.mpk")
    
    # 验证 MPK 文件
    is_valid = validator.validate()
    print("验证结果:", is_valid) 