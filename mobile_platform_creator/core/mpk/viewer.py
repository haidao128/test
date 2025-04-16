"""
MPK查看器

用于查看和验证MPK文件的内容
"""

import os
import json
import base64
from typing import Dict, Any, List
from datetime import datetime
from . import MPKFile

class MPKViewer:
    def __init__(self, mpk_file: MPKFile):
        self.mpk = mpk_file
        
    def get_metadata(self) -> Dict[str, Any]:
        """获取元数据信息"""
        return {
            "应用名称": self.mpk.metadata.app_name,
            "包名": self.mpk.metadata.package_name,
            "版本": self.mpk.metadata.version,
            "版本名称": self.mpk.metadata.version_name,
            "作者": self.mpk.metadata.author,
            "创建日期": self.mpk.metadata.create_date,
            "权限列表": self.mpk.metadata.permissions,
            "依赖项": self.mpk.metadata.dependencies,
            "最低平台版本": self.mpk.metadata.min_platform_version,
            "图标": self.mpk.metadata.icon,
            "启动图": self.mpk.metadata.splash
        }
        
    def get_code_info(self) -> Dict[str, Any]:
        """获取代码信息"""
        return {
            "代码类型": self.mpk.code_section.code_type,
            "代码大小": f"{self.mpk.code_section.code_size} 字节",
            "入口点": self.mpk.code_section.entry_point
        }
        
    def get_resources_info(self) -> List[Dict[str, Any]]:
        """获取资源信息"""
        resources = []
        for resource in self.mpk.resource_sections:
            resources.append({
                "资源类型": resource.resource_type,
                "资源大小": f"{resource.resource_size} 字节"
            })
        return resources
        
    def get_signature_info(self) -> Dict[str, Any]:
        """获取签名信息"""
        return {
            "签名算法": self.mpk.signature_section.algorithm,
            "签名数据": base64.b64encode(self.mpk.signature_section.signature_data).decode(),
            "证书": self.mpk.signature_section.certificate
        }
        
    def get_file_info(self) -> Dict[str, Any]:
        """获取文件信息"""
        return {
            "魔数": self.mpk.magic_number.decode(),
            "版本": self.mpk.version.decode(),
            "文件大小": f"{self.mpk.file_size} 字节",
            "校验和": self.mpk.checksum.hex()
        }
        
    def verify(self) -> Dict[str, Any]:
        """验证文件"""
        return {
            "魔数验证": self.mpk.magic_number == b"MPK1",
            "版本验证": self.mpk.version == b"1.0",
            "校验和验证": self.mpk.checksum == self.mpk.calculate_checksum(),
            "签名验证": self.mpk.verify()
        }
        
    def generate_report(self, output_dir: str) -> str:
        """生成报告"""
        # 创建输出目录
        os.makedirs(output_dir, exist_ok=True)
        
        # 生成报告内容
        report = {
            "文件信息": self.get_file_info(),
            "元数据": self.get_metadata(),
            "代码信息": self.get_code_info(),
            "资源信息": self.get_resources_info(),
            "签名信息": self.get_signature_info(),
            "验证结果": self.verify()
        }
        
        # 保存报告
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_path = os.path.join(output_dir, f"mpk_report_{timestamp}.json")
        
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
            
        return report_path
        
    def extract_resources(self, output_dir: str) -> None:
        """提取资源文件"""
        self.mpk.extract_resources(output_dir)
        
    def get_resource(self, resource_type: str) -> bytes:
        """获取指定类型的资源内容"""
        resource = self.mpk.get_resource(resource_type)
        if resource:
            return resource.resource_content
        return b""
        
    @classmethod
    def from_file(cls, file_path: str) -> 'MPKViewer':
        """从文件创建查看器"""
        return cls(MPKFile.load(file_path))
        
    def print_info(self) -> None:
        """打印文件信息"""
        print("\n=== MPK文件信息 ===")
        for key, value in self.get_file_info().items():
            print(f"{key}: {value}")
            
        print("\n=== 元数据信息 ===")
        for key, value in self.get_metadata().items():
            print(f"{key}: {value}")
            
        print("\n=== 代码信息 ===")
        for key, value in self.get_code_info().items():
            print(f"{key}: {value}")
            
        print("\n=== 资源信息 ===")
        for resource in self.get_resources_info():
            print(f"类型: {resource['资源类型']}, 大小: {resource['资源大小']}")
            
        print("\n=== 签名信息 ===")
        for key, value in self.get_signature_info().items():
            print(f"{key}: {value}")
            
        print("\n=== 验证结果 ===")
        for key, value in self.verify().items():
            print(f"{key}: {'通过' if value else '失败'}") 