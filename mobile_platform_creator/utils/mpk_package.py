"""
MPK应用包格式处理模块
====================

定义并处理MPK (.mpk) 应用包格式，用于应用的分发与安装。

MPK包格式定义：
- 基于ZIP的压缩包
- 包含manifest.json文件，定义应用元数据
- 包含code/目录，存放应用代码（WASM模块、脚本等）
- 包含assets/目录，存放应用资源（图像、声音等）
- 包含signature.sig文件，用于数字签名
- 可选包含config/目录，用于应用配置

MPK文件的典型结构：
```
example_app.mpk
├── manifest.json         # 应用清单
├── code/                 # 代码目录
│   ├── main.wasm         # 主程序模块
│   └── lib/              # 库目录
├── assets/               # 资源目录
│   ├── images/           # 图像资源
│   ├── sounds/           # 音频资源
│   └── ...
├── config/               # 配置目录
│   └── default.json      # 默认配置
└── signature.sig         # 数字签名
```
"""

import os
import json
import zipfile
import tempfile
import hashlib
import shutil
import logging
import base64
from typing import Dict, List, Any, Optional, Set, BinaryIO, Union, Tuple

logger = logging.getLogger("mobile_platform_creator.utils.mpk_package")

# 清单文件必须包含的字段
REQUIRED_MANIFEST_FIELDS = {
    "id", "name", "version", "platform", "min_platform_version"
}

class MPKError(Exception):
    """MPK处理过程中的异常"""
    pass

def is_valid_mpk(file_path: str) -> bool:
    """
    检查文件是否是有效的MPK包
    
    Args:
        file_path: MPK文件路径
        
    Returns:
        bool: 是否是有效的MPK文件
    """
    if not os.path.exists(file_path):
        return False
    
    try:
        with zipfile.ZipFile(file_path, 'r') as mpk_file:
            # 检查必要文件是否存在
            file_list = mpk_file.namelist()
            
            # 检查清单文件
            if "manifest.json" not in file_list:
                logger.warning("MPK包 %s 缺少manifest.json文件", file_path)
                return False
            
            # 检查签名文件
            if "signature.sig" not in file_list:
                logger.warning("MPK包 %s 缺少signature.sig文件", file_path)
                return False
            
            # 检查代码目录
            if not any(f.startswith("code/") for f in file_list):
                logger.warning("MPK包 %s 缺少code/目录", file_path)
                return False
            
            # 读取并验证清单文件
            try:
                manifest_data = mpk_file.read("manifest.json").decode('utf-8')
                manifest = json.loads(manifest_data)
                
                # 检查必要字段
                for field in REQUIRED_MANIFEST_FIELDS:
                    if field not in manifest:
                        logger.warning("MPK包 %s 的清单缺少必要字段: %s", file_path, field)
                        return False
                
                return True
            except json.JSONDecodeError:
                logger.warning("MPK包 %s 的manifest.json格式无效", file_path)
                return False
            except Exception as e:
                logger.warning("验证MPK包 %s 时出错: %s", file_path, e)
                return False
    except zipfile.BadZipFile:
        logger.warning("文件 %s 不是有效的ZIP文件", file_path)
        return False
    except Exception as e:
        logger.warning("检查MPK包 %s 时出错: %s", file_path, e)
        return False

class MPKPackage:
    """MPK应用包类"""
    
    def __init__(self, file_path: Optional[str] = None):
        """
        初始化MPK应用包
        
        Args:
            file_path: MPK文件路径，默认为None（创建新包）
        """
        self.file_path = file_path
        self.manifest: Dict[str, Any] = {}
        self.temp_dir: Optional[str] = None
        
        if file_path and os.path.exists(file_path):
            self._load_package()
        else:
            # 创建基本清单
            self.manifest = {
                "id": "",
                "name": "",
                "version": "1.0.0",
                "platform": "desktop",
                "min_platform_version": "1.0.0",
                "permissions": [],
                "description": "",
                "author": "",
                "icon": "assets/icon.png"
            }
            
            # 创建临时目录
            self.temp_dir = tempfile.mkdtemp(prefix="mpk_")
            
            # 创建基本目录结构
            os.makedirs(os.path.join(self.temp_dir, "code"), exist_ok=True)
            os.makedirs(os.path.join(self.temp_dir, "assets"), exist_ok=True)
            os.makedirs(os.path.join(self.temp_dir, "config"), exist_ok=True)
            
            # 创建清单文件
            self._save_manifest()
    
    def __del__(self):
        """析构函数，清理临时目录"""
        self._cleanup()
    
    def _cleanup(self):
        """清理临时目录"""
        if self.temp_dir and os.path.exists(self.temp_dir):
            try:
                shutil.rmtree(self.temp_dir)
                self.temp_dir = None
            except Exception as e:
                logger.warning("清理临时目录失败: %s", e)
    
    def _load_package(self):
        """从文件加载包"""
        if not is_valid_mpk(self.file_path):
            raise MPKError(f"无效的MPK文件: {self.file_path}")
        
        # 创建临时目录
        self.temp_dir = tempfile.mkdtemp(prefix="mpk_")
        
        # 解压文件
        try:
            with zipfile.ZipFile(self.file_path, 'r') as mpk_file:
                mpk_file.extractall(self.temp_dir)
                
                # 加载清单
                manifest_path = os.path.join(self.temp_dir, "manifest.json")
                with open(manifest_path, 'r', encoding='utf-8') as f:
                    self.manifest = json.load(f)
        except Exception as e:
            # 清理临时目录
            self._cleanup()
            raise MPKError(f"加载MPK文件失败: {e}")
    
    def _save_manifest(self):
        """保存清单到临时目录"""
        if not self.temp_dir:
            raise MPKError("临时目录未创建")
        
        try:
            manifest_path = os.path.join(self.temp_dir, "manifest.json")
            with open(manifest_path, 'w', encoding='utf-8') as f:
                json.dump(self.manifest, f, ensure_ascii=False, indent=2)
        except Exception as e:
            raise MPKError(f"保存清单文件失败: {e}")
    
    def get_manifest(self) -> Dict[str, Any]:
        """
        获取应用清单
        
        Returns:
            Dict[str, Any]: 应用清单数据
        """
        return self.manifest.copy()
    
    def set_manifest(self, manifest: Dict[str, Any]) -> None:
        """
        设置应用清单
        
        Args:
            manifest: 应用清单数据
        """
        # 验证必要字段
        for field in REQUIRED_MANIFEST_FIELDS:
            if field not in manifest:
                raise MPKError(f"清单缺少必要字段: {field}")
        
        self.manifest = manifest.copy()
        
        # 保存清单到临时目录
        if self.temp_dir:
            self._save_manifest()
    
    def update_manifest(self, key: str, value: Any) -> None:
        """
        更新清单中的特定字段
        
        Args:
            key: 字段名
            value: 字段值
        """
        self.manifest[key] = value
        
        # 保存清单到临时目录
        if self.temp_dir:
            self._save_manifest()
    
    def add_file(self, source_path: str, relative_target_path: str) -> None:
        """
        添加文件到包中
        
        Args:
            source_path: 源文件路径
            relative_target_path: 相对于包根目录的目标路径
        """
        if not self.temp_dir:
            raise MPKError("临时目录未创建")
        
        # 检查源文件是否存在
        if not os.path.exists(source_path):
            raise MPKError(f"源文件不存在: {source_path}")
        
        # 生成目标路径
        target_path = os.path.join(self.temp_dir, relative_target_path)
        
        # 确保目标目录存在
        os.makedirs(os.path.dirname(target_path), exist_ok=True)
        
        # 复制文件
        try:
            shutil.copy2(source_path, target_path)
        except Exception as e:
            raise MPKError(f"添加文件失败: {e}")
    
    def add_directory(self, source_dir: str, relative_target_dir: str) -> None:
        """
        添加目录到包中
        
        Args:
            source_dir: 源目录路径
            relative_target_dir: 相对于包根目录的目标目录路径
        """
        if not self.temp_dir:
            raise MPKError("临时目录未创建")
        
        # 检查源目录是否存在
        if not os.path.exists(source_dir) or not os.path.isdir(source_dir):
            raise MPKError(f"源目录不存在: {source_dir}")
        
        # 生成目标目录
        target_dir = os.path.join(self.temp_dir, relative_target_dir)
        
        # 确保目标目录存在
        os.makedirs(target_dir, exist_ok=True)
        
        # 复制目录内容
        try:
            for item in os.listdir(source_dir):
                source_item = os.path.join(source_dir, item)
                target_item = os.path.join(target_dir, item)
                
                if os.path.isdir(source_item):
                    # 递归复制子目录
                    shutil.copytree(source_item, target_item)
                else:
                    # 复制文件
                    shutil.copy2(source_item, target_item)
        except Exception as e:
            raise MPKError(f"添加目录失败: {e}")
    
    def extract_file(self, relative_path: str, target_path: str) -> None:
        """
        从包中提取文件
        
        Args:
            relative_path: 相对于包根目录的文件路径
            target_path: 目标文件路径
        """
        if not self.temp_dir:
            raise MPKError("包未加载")
        
        # 生成源文件路径
        source_path = os.path.join(self.temp_dir, relative_path)
        
        # 检查源文件是否存在
        if not os.path.exists(source_path) or not os.path.isfile(source_path):
            raise MPKError(f"包中不存在文件: {relative_path}")
        
        # 确保目标目录存在
        os.makedirs(os.path.dirname(target_path), exist_ok=True)
        
        # 复制文件
        try:
            shutil.copy2(source_path, target_path)
        except Exception as e:
            raise MPKError(f"提取文件失败: {e}")
    
    def extract_directory(self, relative_path: str, target_dir: str) -> None:
        """
        从包中提取目录
        
        Args:
            relative_path: 相对于包根目录的目录路径
            target_dir: 目标目录路径
        """
        if not self.temp_dir:
            raise MPKError("包未加载")
        
        # 生成源目录路径
        source_dir = os.path.join(self.temp_dir, relative_path)
        
        # 检查源目录是否存在
        if not os.path.exists(source_dir) or not os.path.isdir(source_dir):
            raise MPKError(f"包中不存在目录: {relative_path}")
        
        # 确保目标目录存在
        os.makedirs(target_dir, exist_ok=True)
        
        # 复制目录
        try:
            # 使用distutils的复制函数，它能处理复制目录的情况
            shutil.copytree(source_dir, target_dir, dirs_exist_ok=True)
        except Exception as e:
            raise MPKError(f"提取目录失败: {e}")
    
    def list_files(self) -> List[str]:
        """
        列出包中的所有文件
        
        Returns:
            List[str]: 文件路径列表，相对于包根目录
        """
        if not self.temp_dir:
            raise MPKError("包未加载")
        
        # 获取所有文件
        file_list = []
        for root, _, files in os.walk(self.temp_dir):
            for file in files:
                # 计算相对路径
                abs_path = os.path.join(root, file)
                rel_path = os.path.relpath(abs_path, self.temp_dir)
                file_list.append(rel_path)
        
        return file_list
    
    def sign(self, private_key_path: str, output_path: Optional[str] = None) -> str:
        """
        使用私钥对包进行签名
        
        Args:
            private_key_path: 私钥文件路径
            output_path: 输出文件路径，默认为None（不输出文件）
            
        Returns:
            str: 签名字符串
        """
        # 生成包内容哈希
        package_hash = self._generate_hash()
        
        # 签名
        # 注意：这里应该使用加密库进行实际的签名操作
        # 出于简化，我们只是模拟了签名过程
        signature = f"SIMULATION_SIGNATURE_{package_hash}"
        
        # 保存签名文件
        signature_path = os.path.join(self.temp_dir, "signature.sig")
        with open(signature_path, 'w', encoding='utf-8') as f:
            f.write(signature)
        
        # 如果指定了输出路径，保存包文件
        if output_path:
            self.save(output_path)
        
        return signature
    
    def verify(self, public_key_path: Optional[str] = None) -> bool:
        """
        验证包签名
        
        Args:
            public_key_path: 公钥文件路径，默认为None（使用内置公钥）
            
        Returns:
            bool: 签名是否有效
        """
        if not self.temp_dir:
            raise MPKError("包未加载")
        
        # 获取签名文件路径
        signature_path = os.path.join(self.temp_dir, "signature.sig")
        
        # 检查签名文件是否存在
        if not os.path.exists(signature_path):
            logger.warning("签名文件不存在")
            return False
        
        # 读取签名
        try:
            with open(signature_path, 'r', encoding='utf-8') as f:
                signature = f.read().strip()
        except Exception as e:
            logger.warning("读取签名文件失败: %s", e)
            return False
        
        # 生成包内容哈希
        package_hash = self._generate_hash()
        
        # 验证签名
        # 注意：这里应该使用加密库进行实际的签名验证
        # 出于简化，我们只是模拟了验证过程
        expected_signature = f"SIMULATION_SIGNATURE_{package_hash}"
        
        return signature == expected_signature
    
    def _generate_hash(self) -> str:
        """
        生成包内容的哈希值
        
        Returns:
            str: 哈希值
        """
        if not self.temp_dir:
            raise MPKError("包未加载")
        
        # 获取所有文件
        files = self.list_files()
        
        # 排除签名文件
        if "signature.sig" in files:
            files.remove("signature.sig")
        
        # 排序文件列表，确保哈希值的一致性
        files.sort()
        
        # 计算哈希
        hasher = hashlib.sha256()
        
        for file_path in files:
            abs_path = os.path.join(self.temp_dir, file_path)
            
            # 添加文件路径
            hasher.update(file_path.encode('utf-8'))
            
            # 添加文件内容
            with open(abs_path, 'rb') as f:
                while True:
                    chunk = f.read(65536)  # 64KB chunks
                    if not chunk:
                        break
                    hasher.update(chunk)
        
        return hasher.hexdigest()
    
    def save(self, output_path: str) -> None:
        """
        保存包到文件
        
        Args:
            output_path: 输出文件路径
        """
        if not self.temp_dir:
            raise MPKError("临时目录未创建")
        
        try:
            # 确保输出目录存在
            os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
            
            # 创建ZIP文件
            with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zip_file:
                # 添加文件
                for root, _, files in os.walk(self.temp_dir):
                    for file in files:
                        # 计算相对路径
                        abs_path = os.path.join(root, file)
                        rel_path = os.path.relpath(abs_path, self.temp_dir)
                        
                        # 添加到ZIP
                        zip_file.write(abs_path, rel_path)
            
            # 更新文件路径
            self.file_path = output_path
            
            logger.info("MPK包已保存到: %s", output_path)
        except Exception as e:
            raise MPKError(f"保存MPK包失败: {e}")
    
    @staticmethod
    def create_from_directory(source_dir: str, output_path: str,
                             manifest: Dict[str, Any], private_key_path: Optional[str] = None) -> 'MPKPackage':
        """
        从目录创建MPK包
        
        Args:
            source_dir: 源目录路径
            output_path: 输出文件路径
            manifest: 应用清单数据
            private_key_path: 私钥文件路径，默认为None（不签名）
            
        Returns:
            MPKPackage: 创建的MPK包对象
        """
        # 创建新包
        package = MPKPackage()
        
        # 设置清单
        package.set_manifest(manifest)
        
        # 复制目录结构
        if os.path.exists(os.path.join(source_dir, "code")):
            package.add_directory(os.path.join(source_dir, "code"), "code")
        else:
            os.makedirs(os.path.join(package.temp_dir, "code"), exist_ok=True)
        
        if os.path.exists(os.path.join(source_dir, "assets")):
            package.add_directory(os.path.join(source_dir, "assets"), "assets")
        else:
            os.makedirs(os.path.join(package.temp_dir, "assets"), exist_ok=True)
        
        if os.path.exists(os.path.join(source_dir, "config")):
            package.add_directory(os.path.join(source_dir, "config"), "config")
        else:
            os.makedirs(os.path.join(package.temp_dir, "config"), exist_ok=True)
        
        # 签名包
        if private_key_path:
            package.sign(private_key_path)
        else:
            # 创建空签名文件
            signature_path = os.path.join(package.temp_dir, "signature.sig")
            with open(signature_path, 'w', encoding='utf-8') as f:
                f.write("UNSIGNED")
        
        # 保存包
        package.save(output_path)
        
        return package 