"""
MPK打包工具

用于将应用打包成MPK文件
"""

import os
import json
import shutil
from typing import List, Dict, Any
from . import MPKFile, Metadata, CodeSection, ResourceSection, SignatureSection

class MPKPacker:
    def __init__(self):
        self.mpk = MPKFile()
        
    def set_metadata(self, metadata: Dict[str, Any]) -> None:
        """设置元数据"""
        self.mpk.metadata = Metadata.from_dict(metadata)
        
    def add_code(self, code_path: str, code_type: str = "script", entry_point: str = "main") -> None:
        """添加代码"""
        with open(code_path, "rb") as f:
            code_content = f.read()
            
        self.mpk.code_section = CodeSection()
        self.mpk.code_section.code_type = code_type
        self.mpk.code_section.code_size = len(code_content)
        self.mpk.code_section.code_content = code_content
        self.mpk.code_section.entry_point = entry_point
        
    def add_resource(self, resource_path: str, resource_type: str) -> None:
        """添加资源"""
        with open(resource_path, "rb") as f:
            resource_content = f.read()
            
        resource = ResourceSection()
        resource.resource_type = resource_type
        resource.resource_size = len(resource_content)
        resource.resource_content = resource_content
        
        self.mpk.resource_sections.append(resource)
        
    def add_directory(self, dir_path: str, resource_type: str) -> None:
        """添加目录作为资源"""
        # 创建临时目录
        temp_dir = os.path.join(os.path.dirname(dir_path), f"temp_{resource_type}")
        os.makedirs(temp_dir, exist_ok=True)
        
        # 复制文件
        for root, _, files in os.walk(dir_path):
            for file in files:
                src = os.path.join(root, file)
                dst = os.path.join(temp_dir, file)
                shutil.copy2(src, dst)
                
        # 打包目录
        shutil.make_archive(temp_dir, "zip", temp_dir)
        
        # 添加资源
        with open(f"{temp_dir}.zip", "rb") as f:
            resource_content = f.read()
            
        resource = ResourceSection()
        resource.resource_type = resource_type
        resource.resource_size = len(resource_content)
        resource.resource_content = resource_content
        
        self.mpk.resource_sections.append(resource)
        
        # 清理临时文件
        shutil.rmtree(temp_dir)
        os.remove(f"{temp_dir}.zip")
        
    def set_signature(self, algorithm: str, signature_data: bytes, certificate: str) -> None:
        """设置签名"""
        self.mpk.signature_section = SignatureSection()
        self.mpk.signature_section.algorithm = algorithm
        self.mpk.signature_section.signature_data = signature_data
        self.mpk.signature_section.certificate = certificate
        
    def pack(self, output_path: str) -> None:
        """打包MPK文件"""
        self.mpk.save(output_path)
        
    def verify(self) -> bool:
        """验证MPK文件"""
        return self.mpk.verify()
        
    def extract_resources(self, output_dir: str) -> None:
        """提取资源文件"""
        self.mpk.extract_resources(output_dir)
        
    def get_resource(self, resource_type: str) -> ResourceSection:
        """获取指定类型的资源"""
        return self.mpk.get_resource(resource_type)
        
    @classmethod
    def from_config(cls, config_path: str) -> 'MPKPacker':
        """从配置文件创建打包器"""
        with open(config_path, "r", encoding="utf-8") as f:
            config = json.load(f)
            
        packer = cls()
        
        # 设置元数据
        if "metadata" in config:
            packer.set_metadata(config["metadata"])
            
        # 添加代码
        if "code" in config:
            code_config = config["code"]
            packer.add_code(
                code_config["path"],
                code_config.get("type", "script"),
                code_config.get("entry_point", "main")
            )
            
        # 添加资源
        if "resources" in config:
            for resource in config["resources"]:
                if os.path.isdir(resource["path"]):
                    packer.add_directory(resource["path"], resource["type"])
                else:
                    packer.add_resource(resource["path"], resource["type"])
                    
        # 设置签名
        if "signature" in config:
            signature_config = config["signature"]
            with open(signature_config["data_path"], "rb") as f:
                signature_data = f.read()
            with open(signature_config["certificate_path"], "r", encoding="utf-8") as f:
                certificate = f.read()
            packer.set_signature(
                signature_config["algorithm"],
                signature_data,
                certificate
            )
            
        return packer 