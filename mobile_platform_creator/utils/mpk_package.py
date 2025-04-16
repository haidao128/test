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

# 尝试导入加密库，如果失败则发出警告
try:
    from cryptography.hazmat.primitives import hashes
    from cryptography.hazmat.primitives.asymmetric import padding, rsa
    from cryptography.hazmat.primitives import serialization
    from cryptography.hazmat.primitives.asymmetric.utils import Prehashed
    CRYPTOGRAPHY_AVAILABLE = True
except ImportError:
    CRYPTOGRAPHY_AVAILABLE = False
    logging.warning("Cryptography library not found. Real signature functionality will be disabled.")

logger = logging.getLogger("mobile_platform_creator.utils.mpk_package")

# 根据新规范 v2.1 更新
MPK_FORMAT_VERSION = "2.1"

# 清单文件必须包含的字段 (规范 v2.1)
REQUIRED_MANIFEST_FIELDS = {
    "format_version", "id", "name", "version", "platform", "min_platform_version",
    "code_type", "entry_point"
}

class MPKError(Exception):
    """MPK处理过程中的异常"""
    pass

def is_valid_mpk(file_path: str, verify_signature: bool = False, public_key_path: Optional[str] = None) -> bool:
    """
    检查文件是否是有效的MPK包（基于ZIP格式）

    Args:
        file_path: MPK文件路径
        verify_signature: 是否验证签名 (需要 cryptography 库)
        public_key_path: 用于验证签名的公钥文件路径

    Returns:
        bool: 是否是有效的MPK文件
    """
    if not os.path.exists(file_path):
        logger.warning(f"MPK 文件不存在: {file_path}")
        return False

    try:
        with zipfile.ZipFile(file_path, 'r') as mpk_file:
            file_list = mpk_file.namelist()

            # 1. 检查 manifest.json 是否存在
            if "manifest.json" not in file_list:
                logger.warning(f"MPK包 {file_path} 缺少 manifest.json 文件")
                return False

            # 2. 检查 code/ 目录是否存在且不为空
            code_files = [f for f in file_list if f.startswith("code/") and not f.endswith('/')]
            if not code_files:
                logger.warning(f"MPK包 {file_path} 缺少 code/ 目录或该目录为空")
                return False

            # 3. 检查 signature.sig 文件是否存在
            if "signature.sig" not in file_list:
                logger.warning(f"MPK包 {file_path} 缺少 signature.sig 文件")
                # 根据策略，缺少签名文件可能不算完全无效，但可能需要警告或特殊处理
                # 这里我们暂时将其视为有效，但会在需要验证时失败
                pass

            # 4. 读取并验证清单文件
            try:
                manifest_data = mpk_file.read("manifest.json").decode('utf-8')
                manifest = json.loads(manifest_data)

                # 检查格式版本
                if manifest.get("format_version") != MPK_FORMAT_VERSION:
                    logger.warning(f"MPK包 {file_path} 的 format_version 不匹配当前支持的版本 ({MPK_FORMAT_VERSION})，找到的是: {manifest.get('format_version')}")
                    # 根据兼容性策略决定是否返回 False
                    # return False

                # 检查必要字段
                missing_fields = REQUIRED_MANIFEST_FIELDS - set(manifest.keys())
                if missing_fields:
                    logger.warning(f"MPK包 {file_path} 的清单缺少必要字段: {', '.join(missing_fields)}")
                    return False

                # 检查入口点是否存在
                entry_point = manifest.get("entry_point", "")
                if not entry_point or entry_point not in file_list:
                    logger.warning(f"MPK包 {file_path} 的清单指定的入口点 '{entry_point}' 不存在")
                    return False

            except json.JSONDecodeError:
                logger.warning(f"MPK包 {file_path} 的 manifest.json 格式无效")
                return False
            except UnicodeDecodeError:
                logger.warning(f"MPK包 {file_path} 的 manifest.json 非 UTF-8 编码")
                return False
            except Exception as e:
                logger.warning(f"验证MPK包 {file_path} 的清单时出错: {e}")
                return False

            # 5. 可选：验证签名
            if verify_signature:
                if not CRYPTOGRAPHY_AVAILABLE:
                    logger.error("无法验证签名，因为 cryptography 库不可用。")
                    return False # 或者根据策略决定是否允许未验证的包
                if not public_key_path or not os.path.exists(public_key_path):
                    logger.error(f"无法验证签名，公钥文件不存在或未提供: {public_key_path}")
                    return False

                try:
                    package = MPKPackage(file_path)
                    if not package.verify(public_key_path):
                        logger.warning(f"MPK包 {file_path} 签名验证失败")
                        return False
                    else:
                        logger.info(f"MPK包 {file_path} 签名验证成功")
                except MPKError as e:
                    logger.warning(f"验证MPK包 {file_path} 签名时出错: {e}")
                    return False
                finally:
                    # 确保临时文件被清理
                    if 'package' in locals() and hasattr(package, '_cleanup'):
                        package._cleanup()

            return True

    except zipfile.BadZipFile:
        logger.warning(f"文件 {file_path} 不是有效的ZIP文件")
        return False
    except Exception as e:
        logger.warning(f"检查MPK包 {file_path} 时发生意外错误: {e}")
        return False

class MPKPackage:
    """MPK应用包类 (基于标准ZIP格式, 规范 v2.1)"""
    
    def __init__(self, file_path: Optional[str] = None):
        """
        初始化MPK应用包
        
        Args:
            file_path: MPK文件路径。如果提供且文件存在，则加载现有包；否则创建新包的内存表示。
        """
        self.file_path = file_path
        self.manifest: Dict[str, Any] = {}
        self.temp_dir: Optional[str] = None # 用于存储解压或待打包内容的临时目录
        self._loaded = False
        
        if file_path and os.path.exists(file_path):
            try:
                self._load_package()
                self._loaded = True
            except MPKError as e:
                self._cleanup() # 加载失败时清理可能创建的临时目录
                raise e # 重新抛出异常
        else:
            # 创建新包的内存表示，初始化基本清单
            self.manifest = {
                "format_version": MPK_FORMAT_VERSION,
                "id": "",
                "name": "",
                "version": "1.0.0",
                "platform": "all",
                "min_platform_version": "1.0.0",
                "code_type": "javascript", # 默认为JS，需要用户修改
                "entry_point": "",          # 需要用户设置
                "permissions": [],
                "description": "",
                "author": {},
                "icon": "",
                "dependencies": [],
                "sandbox": {}
            }
            # 新包不立即创建临时目录，只在添加文件或保存时创建
    
    def __del__(self):
        """析构函数，清理临时目录"""
        self._cleanup()
    
    def _ensure_temp_dir(self):
        """确保临时目录存在"""
        if not self.temp_dir or not os.path.exists(self.temp_dir):
            self.temp_dir = tempfile.mkdtemp(prefix="mpk_")
            # 如果是新包，需要创建基本目录结构
            if not self._loaded:
                os.makedirs(os.path.join(self.temp_dir, "code"), exist_ok=True)
                os.makedirs(os.path.join(self.temp_dir, "assets"), exist_ok=True)
                os.makedirs(os.path.join(self.temp_dir, "config"), exist_ok=True)
                self._save_manifest() # 确保新清单写入临时目录
    
    def _cleanup(self):
        """清理临时目录"""
        if self.temp_dir and os.path.exists(self.temp_dir):
            try:
                shutil.rmtree(self.temp_dir)
                self.temp_dir = None
                logger.debug("临时目录已清理")
            except Exception as e:
                logger.warning(f"清理临时目录失败: {e}")
    
    def _load_package(self):
        """从文件加载包到临时目录"""
        if not self.file_path or not os.path.exists(self.file_path):
            raise MPKError("MPK文件路径无效或文件不存在")

        # 使用 is_valid_mpk 进行初步检查，但不验证签名（签名验证是单独步骤）
        if not is_valid_mpk(self.file_path, verify_signature=False):
             # is_valid_mpk 会记录具体错误
            raise MPKError(f"无效的MPK文件: {self.file_path}")

        self._ensure_temp_dir() # 创建临时目录
        logger.debug(f"将MPK解压到临时目录: {self.temp_dir}")

        try:
            with zipfile.ZipFile(self.file_path, 'r') as mpk_file:
                # 解压所有文件
                mpk_file.extractall(self.temp_dir)

                # 加载清单
                manifest_path = os.path.join(self.temp_dir, "manifest.json")
                with open(manifest_path, 'r', encoding='utf-8') as f:
                    self.manifest = json.load(f)
            self._loaded = True
        except (zipfile.BadZipFile, json.JSONDecodeError, UnicodeDecodeError, IOError) as e:
            self._cleanup()
            raise MPKError(f"加载MPK文件失败: {e}")
        except Exception as e:
            self._cleanup()
            raise MPKError(f"加载MPK文件时发生意外错误: {e}")
    
    def _save_manifest(self):
        """保存当前清单到临时目录中的 manifest.json"""
        self._ensure_temp_dir() # 确保临时目录存在
        manifest_path = os.path.join(self.temp_dir, "manifest.json")
        logger.debug(f"保存清单到: {manifest_path}")
        try:
            # 确保清单中有正确的 format_version
            self.manifest["format_version"] = MPK_FORMAT_VERSION
            with open(manifest_path, 'w', encoding='utf-8') as f:
                json.dump(self.manifest, f, ensure_ascii=False, indent=2)
        except IOError as e:
            raise MPKError(f"保存清单文件失败: {e}")
        except Exception as e:
            raise MPKError(f"保存清单时发生意外错误: {e}")
    
    def get_manifest(self) -> Dict[str, Any]:
        """获取应用清单的副本"""
        return self.manifest.copy()
    
    def set_manifest(self, manifest: Dict[str, Any]) -> None:
        """
        设置应用清单，会覆盖现有清单。
        执行基本验证（必需字段）。
        """
        # 验证必需字段
        missing_fields = REQUIRED_MANIFEST_FIELDS - set(manifest.keys())
        if missing_fields:
            raise MPKError(f"清单缺少必要字段: {', '.join(missing_fields)}")

        # 检查 format_version (可选，但推荐)
        if manifest.get("format_version") != MPK_FORMAT_VERSION:
             logger.warning(f"设置的清单 format_version ('{manifest.get('format_version')}') 与当前支持的版本 ('{MPK_FORMAT_VERSION}') 不符")
             # 可以选择抛出错误或仅警告
             # raise MPKError(f"不支持的 format_version: {manifest.get('format_version')}")

        self.manifest = manifest.copy()
        self._save_manifest() # 更新临时目录中的文件
    
    def update_manifest(self, updates: Dict[str, Any]) -> None:
        """
        更新清单中的一个或多个字段。

        Args:
            updates: 包含要更新的键值对的字典。
        """
        if not isinstance(updates, dict):
            raise MPKError("更新必须是一个字典")

        self.manifest.update(updates)
        # 不需要重新验证必需字段，因为是更新操作
        self._save_manifest() # 更新临时目录中的文件
    
    def add_file(self, source_path: str, relative_target_path: str) -> None:
        """
        添加或替换文件到包的临时表示中。

        Args:
            source_path: 源文件路径。
            relative_target_path: 相对于包根目录的目标路径 (例如 'code/main.js', 'assets/image.png')。
                                  路径分隔符应使用 '/'。
        """
        self._ensure_temp_dir() # 确保临时目录存在

        if not os.path.exists(source_path):
            raise MPKError(f"源文件不存在: {source_path}")
        if not os.path.isfile(source_path):
            raise MPKError(f"源路径不是一个文件: {source_path}")

        # 规范化相对路径，确保使用 '/' 并移除前导 '/
        normalized_relative_path = relative_target_path.replace('\\\\', '/').lstrip('/')
        if not normalized_relative_path or normalized_relative_path == '.':
             raise MPKError("无效的目标相对路径")
        if normalized_relative_path == 'manifest.json':
             logger.warning("不推荐直接使用 add_file 添加 manifest.json，请使用 set_manifest 或 update_manifest。")
             # 或者直接阻止
             # raise MPKError("请使用 set_manifest 或 update_manifest 修改清单文件。")
        if normalized_relative_path == 'signature.sig':
             raise MPKError("不能直接添加 signature.sig 文件，请使用 sign 方法生成。")

        target_path = os.path.join(self.temp_dir, normalized_relative_path)
        target_dir = os.path.dirname(target_path)

        try:
            # 确保目标目录存在
            os.makedirs(target_dir, exist_ok=True)
            # 复制文件（覆盖已存在的）
            shutil.copy2(source_path, target_path)
            logger.debug(f"文件已添加/更新到临时目录: {normalized_relative_path}")
        except Exception as e:
            raise MPKError(f"添加文件 '{normalized_relative_path}' 失败: {e}")
    
    def add_directory(self, source_dir: str, relative_target_dir: str) -> None:
        """
        添加或替换目录及其内容到包的临时表示中。

        Args:
            source_dir: 源目录路径。
            relative_target_dir: 相对于包根目录的目标目录路径 (例如 'code', 'assets/images')。
                                 路径分隔符应使用 '/'。
        """
        self._ensure_temp_dir() # 确保临时目录存在

        if not os.path.exists(source_dir):
            raise MPKError(f"源目录不存在: {source_dir}")
        if not os.path.isdir(source_dir):
            raise MPKError(f"源路径不是一个目录: {source_dir}")

        # 规范化相对路径
        normalized_relative_dir = relative_target_dir.replace('\\\\', '/').strip('/')
        if normalized_relative_dir in ['manifest.json', 'signature.sig']:
            raise MPKError(f"目标目录名称无效: {normalized_relative_dir}")
        # 允许空字符串表示根目录，但不推荐直接操作根目录
        # if not normalized_relative_dir:
        #     logger.warning("直接向根目录添加内容可能导致结构混乱，建议使用明确的子目录如 'code', 'assets' 等。")

        target_dir_path = os.path.join(self.temp_dir, normalized_relative_dir)

        try:
            # shutil.copytree 要求目标目录不存在，或者使用 dirs_exist_ok=True (Python 3.8+)
            # 为了兼容性和明确性，我们先删除可能已存在的目标目录
            if os.path.exists(target_dir_path):
                shutil.rmtree(target_dir_path)

            # 确保父目录存在
            os.makedirs(os.path.dirname(target_dir_path), exist_ok=True)

            # 复制整个目录树
            shutil.copytree(source_dir, target_dir_path)
            logger.debug(f"目录已添加/更新到临时目录: {normalized_relative_dir}")

        except Exception as e:
            raise MPKError(f"添加目录 '{normalized_relative_dir}' 失败: {e}")
    
    def extract_file(self, relative_path: str, target_path: str) -> None:
        """
        从加载的包（临时目录中）提取单个文件。

        Args:
            relative_path: 相对于包根目录的文件路径。
            target_path: 目标文件保存路径。
        """
        if not self._loaded:
            raise MPKError("包未加载，无法提取文件。请先从文件初始化MPKPackage对象。")
        if not self.temp_dir:
             raise MPKError("内部错误：包已加载但临时目录不存在。")

        normalized_relative_path = relative_path.replace('\\\\', '/').lstrip('/')
        source_file_path = os.path.join(self.temp_dir, normalized_relative_path)

        if not os.path.exists(source_file_path) or not os.path.isfile(source_file_path):
            raise MPKError(f"包中不存在文件: {normalized_relative_path}")

        target_dir = os.path.dirname(target_path)

        try:
            if target_dir:
                os.makedirs(target_dir, exist_ok=True)
            shutil.copy2(source_file_path, target_path)
            logger.debug(f"文件已提取到: {target_path}")
        except Exception as e:
            raise MPKError(f"提取文件 '{normalized_relative_path}' 失败: {e}")
    
    def extract_directory(self, relative_path: str, target_dir: str) -> None:
        """
        从加载的包（临时目录中）提取整个目录。

        Args:
            relative_path: 相对于包根目录的目录路径。
            target_dir: 目标目录保存路径。
        """
        if not self._loaded:
            raise MPKError("包未加载，无法提取目录。")
        if not self.temp_dir:
             raise MPKError("内部错误：包已加载但临时目录不存在。")

        normalized_relative_path = relative_path.replace('\\\\', '/').strip('/')
        source_dir_path = os.path.join(self.temp_dir, normalized_relative_path)

        if not os.path.exists(source_dir_path) or not os.path.isdir(source_dir_path):
            raise MPKError(f"包中不存在目录: {normalized_relative_path}")

        try:
             # 确保目标父目录存在
            parent_target_dir = os.path.dirname(target_dir)
            if parent_target_dir:
                os.makedirs(parent_target_dir, exist_ok=True)

            # 如果目标目录已存在，先删除（或使用 dirs_exist_ok=True）
            if os.path.exists(target_dir):
                shutil.rmtree(target_dir)

            shutil.copytree(source_dir_path, target_dir)
            logger.debug(f"目录 '{normalized_relative_path}' 已提取到: {target_dir}")
        except Exception as e:
            raise MPKError(f"提取目录 '{normalized_relative_path}' 失败: {e}")
    
    def list_files(self) -> List[str]:
        """
        列出包临时表示中的所有文件（相对于根目录）。

        Returns:
            List[str]: 文件路径列表，使用 '/' 作为分隔符。
        """
        self._ensure_temp_dir() # 确保临时目录存在，即使是新包也要能列出（可能是空的）
        if not self.temp_dir:
            # 理论上 _ensure_temp_dir 后不会到这里，除非创建失败
             raise MPKError("无法列出文件，临时目录不可用。")

        file_list = []
        try:
            for root, _, files in os.walk(self.temp_dir):
                for file in files:
                    abs_path = os.path.join(root, file)
                    rel_path = os.path.relpath(abs_path, self.temp_dir)
                    # 转换为 POSIX 风格路径
                    file_list.append(rel_path.replace('\\\\', '/'))
        except Exception as e:
            raise MPKError(f"列出文件失败: {e}")

        return sorted(file_list)
    
    def _generate_hash_for_signing(self) -> bytes:
        """
        根据规范 v2.1 生成包内容的待签名哈希摘要。

        Returns:
            bytes: SHA-256 哈希摘要。
        """
        self._ensure_temp_dir() # 确保所有待打包内容都在临时目录
        if not self.temp_dir:
             raise MPKError("无法生成哈希，临时目录不可用。")

        hasher = hashlib.sha256()
        files_to_hash = self.list_files()

        # 排除 signature.sig 文件
        if "signature.sig" in files_to_hash:
            files_to_hash.remove("signature.sig")

        # **重要：必须按文件相对路径排序**
        files_to_hash.sort()

        logger.debug("参与哈希计算的文件:")
        for file_path in files_to_hash:
            logger.debug(f"  - {file_path}")
            abs_path = os.path.join(self.temp_dir, file_path.replace('/', os.sep))

            try:
                # 1. 更新路径 (UTF-8 编码)
                hasher.update(file_path.encode('utf-8'))

                # 2. 更新文件内容
                with open(abs_path, 'rb') as f:
                    while True:
                        chunk = f.read(65536)  # 64KB chunks
                        if not chunk:
                            break
                        hasher.update(chunk)
            except FileNotFoundError:
                 # list_files 来自 os.walk，理论上文件应该存在
                 raise MPKError(f"内部错误：无法找到文件 '{file_path}' 进行哈希计算")
            except IOError as e:
                 raise MPKError(f"读取文件 '{file_path}' 进行哈希计算失败: {e}")
            except Exception as e:
                 raise MPKError(f"为文件 '{file_path}' 计算哈希时发生意外错误: {e}")

        digest = hasher.digest()
        logger.debug(f"生成的待签名摘要 (SHA-256): {digest.hex()}")
        return digest
    
    def sign(self, private_key_path: str, password: Optional[str] = None) -> None:
        """
        使用私钥对包内容进行签名，并将签名写入临时目录中的 signature.sig。
        需要 cryptography 库。

        Args:
            private_key_path: PEM 格式的私钥文件路径。
            password: 私钥文件的密码 (如果已加密)。
        """
        self._ensure_temp_dir() # 确保临时目录和内容已准备好
        if not self.temp_dir:
             raise MPKError("无法签名，临时目录不可用。")

        if not CRYPTOGRAPHY_AVAILABLE:
            logger.warning("无法执行真实签名，因为 cryptography 库未安装。将写入模拟签名。")
            # 写入模拟签名或标记
            signature_path = os.path.join(self.temp_dir, "signature.sig")
            try:
                # 计算一个简单的哈希作为模拟标记的一部分
                simulated_hash = self._generate_hash_for_signing().hex()[:16] # 取前16位
                with open(signature_path, 'w', encoding='utf-8') as f:
                    f.write(f"SIMULATED_SIGNATURE_{simulated_hash}")
                logger.debug("已写入模拟签名到 signature.sig")
            except Exception as e:
                raise MPKError(f"写入模拟签名失败: {e}")
            return # 结束执行

        # --- 以下是真实的签名逻辑 --- 
        if not os.path.exists(private_key_path):
            raise MPKError(f"私钥文件不存在: {private_key_path}")

        try:
            # 1. 加载私钥
            with open(private_key_path, "rb") as key_file:
                private_key = serialization.load_pem_private_key(
                    key_file.read(),
                    password=password.encode('utf-8') if password else None
                )

            # 检查私钥类型，目前主要支持 RSA
            if not isinstance(private_key, rsa.RSAPrivateKey):
                # 可以扩展支持 ECDSA 等
                raise MPKError("目前仅支持 RSA 私钥进行签名")

            # 2. 计算待签名的数据摘要 (SHA-256)
            data_to_sign_digest = self._generate_hash_for_signing()

            # 3. 执行签名 (使用 PSS padding, 这是推荐的 RSA padding 方案)
            signature = private_key.sign(
                data_to_sign_digest, # 直接签名哈希摘要
                padding.PSS(
                    mgf=padding.MGF1(hashes.SHA256()),
                    salt_length=padding.PSS.MAX_LENGTH
                ),
                hashes.SHA256() # 需要指定哈希算法
                # 或者使用 Prehashed，如果你确定摘要算法
                # padding.PKCS1v15(), # 也可以用旧的 PKCS1v15 padding
                # hashes.SHA256()
            )

            # 4. 将签名结果保存到 signature.sig (通常使用 Base64 编码)
            signature_base64 = base64.b64encode(signature).decode('ascii')
            signature_path = os.path.join(self.temp_dir, "signature.sig")

            with open(signature_path, 'w', encoding='ascii') as f:
                f.write(signature_base64)

            logger.info(f"包已使用私钥 {private_key_path} 签名，签名写入 signature.sig")

        except (IOError, ValueError, TypeError) as e:
            # 包括文件读取错误、密码错误、PEM格式错误等
            raise MPKError(f"加载私钥或签名过程中出错: {e}")
        except Exception as e:
            raise MPKError(f"签名过程中发生意外错误: {e}")
    
    def verify(self, public_key_path: str) -> bool:
        """
        使用公钥验证包的签名 (signature.sig)。
        需要 cryptography 库。

        Args:
            public_key_path: PEM 格式的公钥文件路径。

        Returns:
            bool: 签名是否有效。
        """
        if not self._loaded:
            raise MPKError("包未加载，无法验证签名。")
        if not self.temp_dir:
             raise MPKError("内部错误：包已加载但临时目录不存在。")

        signature_path = os.path.join(self.temp_dir, "signature.sig")

        if not os.path.exists(signature_path):
            logger.warning("签名文件 signature.sig 不存在，无法验证。")
            return False

        if not CRYPTOGRAPHY_AVAILABLE:
            logger.error("无法执行真实签名验证，因为 cryptography 库未安装。")
            # 检查是否是模拟签名
            try:
                with open(signature_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                if content.startswith("SIMULATED_SIGNATURE_"):
                     logger.warning("检测到模拟签名，无法进行真实验证。")
                     # 根据策略，可以认为模拟签名无效或跳过
                     return False # 通常认为模拟签名无效
                else:
                     logger.error("Cryptography 库不可用，且 signature.sig 内容不是预期的模拟签名格式。")
                     return False
            except Exception as e:
                logger.error(f"读取或检查模拟签名时出错: {e}")
                return False
            # return False # 结束执行

        # --- 以下是真实的验证逻辑 --- 
        if not os.path.exists(public_key_path):
            raise MPKError(f"公钥文件不存在: {public_key_path}")

        try:
            # 1. 加载公钥
            with open(public_key_path, "rb") as key_file:
                public_key = serialization.load_pem_public_key(
                    key_file.read()
                )

            # 检查公钥类型
            if not isinstance(public_key, rsa.RSAPublicKey):
                 raise MPKError("目前仅支持 RSA 公钥进行验证")

            # 2. 读取签名文件内容 (Base64)
            with open(signature_path, 'r', encoding='ascii') as f:
                signature_base64 = f.read().strip()
            signature = base64.b64decode(signature_base64)

            # 3. 计算预期的数据摘要 (SHA-256)
            expected_digest = self._generate_hash_for_signing()

            # 4. 执行验证 (需要使用与签名时相同的 padding 和哈希算法)
            try:
                public_key.verify(
                    signature,         # 待验证的签名
                    expected_digest,   # 预期的数据摘要
                    padding.PSS(       # 使用 PSS padding
                        mgf=padding.MGF1(hashes.SHA256()),
                        salt_length=padding.PSS.MAX_LENGTH
                    ),
                    hashes.SHA256()    # 指定哈希算法
                    # padding.PKCS1v15(), # 如果签名用的是 PKCS1v15
                    # hashes.SHA256()
                )
                logger.info(f"使用公钥 {public_key_path} 验证签名成功。")
                return True
            except InvalidSignature: # cryptography.exceptions.InvalidSignature
                logger.warning("签名验证失败：签名与内容不匹配。")
                return False
            except Exception as e:
                # 其他可能的验证错误
                logger.error(f"签名验证过程中发生错误: {e}")
                return False

        except (IOError, ValueError, TypeError, base64.binascii.Error) as e:
            raise MPKError(f"加载公钥或读取签名时出错: {e}")
        except Exception as e:
            raise MPKError(f"签名验证过程中发生意外错误: {e}")
    
    def save(self, output_path: str) -> None:
        """
        将包的当前临时表示保存为 MPK (ZIP) 文件。
        会先自动保存 manifest.json。
        如果包已签名，会包含 signature.sig。

        Args:
            output_path: 输出的 MPK 文件路径。
        """
        self._ensure_temp_dir() # 确保临时目录和基本结构存在
        if not self.temp_dir:
             raise MPKError("无法保存，临时目录不可用。")

        # 确保清单已写入临时目录
        self._save_manifest()

        # 验证清单是否满足基本要求（可选，但推荐）
        missing_fields = REQUIRED_MANIFEST_FIELDS - set(self.manifest.keys())
        if missing_fields:
            raise MPKError(f"无法保存：清单缺少必要字段: {', '.join(missing_fields)}")
        if not self.manifest.get("entry_point"): # 确保入口点已设置
            raise MPKError("无法保存：清单缺少入口点 (entry_point) 信息")
        # 可以在这里添加更多保存前的检查，比如入口点文件是否存在等
        entry_point_path_in_temp = os.path.join(self.temp_dir, self.manifest["entry_point"].replace('/', os.sep))
        if not os.path.exists(entry_point_path_in_temp):
            logger.warning(f"清单指定的入口点 '{self.manifest['entry_point']}' 在临时目录中不存在，但仍会继续保存。")
            # 或者直接报错
            # raise MPKError(f"无法保存：清单指定的入口点 '{self.manifest['entry_point']}' 文件未添加到包中")

        logger.info(f"正在将包内容从 {self.temp_dir} 保存到 {output_path}")

        try:
            # 确保输出目录存在
            output_dir = os.path.dirname(os.path.abspath(output_path))
            if output_dir:
                os.makedirs(output_dir, exist_ok=True)

            # 创建ZIP文件
            with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zip_file:
                files_to_add = self.list_files() # 获取临时目录中的所有文件相对路径

                if not files_to_add:
                     logger.warning("尝试保存一个空的 MPK 包。")
                     # raise MPKError("无法保存空包")

                for rel_path in files_to_add:
                    abs_path = os.path.join(self.temp_dir, rel_path.replace('/', os.sep))
                    # 使用相对路径作为 ZIP 文件中的路径
                    zip_file.write(abs_path, rel_path)

            # 更新实例的文件路径
            self.file_path = output_path
            self._loaded = True # 保存后视为已加载状态
            logger.info(f"MPK包已成功保存到: {output_path}")

        except Exception as e:
            raise MPKError(f"保存MPK包到 '{output_path}' 失败: {e}")
    
    @staticmethod
    def create_from_directory(source_dir: str, output_path: str,
                             manifest_override: Optional[Dict[str, Any]] = None,
                             private_key_path: Optional[str] = None,
                             private_key_password: Optional[str] = None) -> 'MPKPackage':
        """
        从一个源目录创建、签名（可选）并保存 MPK 包。
        会自动查找源目录下的 manifest.json，或使用 manifest_override 覆盖。

        Args:
            source_dir: 包含应用文件（如 code/, assets/, manifest.json）的源目录。
            output_path: 输出的 MPK 文件路径。
            manifest_override: 可选，用于覆盖或提供清单数据的字典。如果提供，将忽略 source_dir 中的 manifest.json。
            private_key_path: 可选，用于签名的私钥文件路径。
            private_key_password: 可选，私钥文件的密码。

        Returns:
            MPKPackage: 创建并保存后的 MPK 包对象。
        """
        logger.info(f"从目录 '{source_dir}' 创建 MPK 包到 '{output_path}'")

        if not os.path.exists(source_dir) or not os.path.isdir(source_dir):
            raise MPKError(f"源目录不存在或不是一个目录: {source_dir}")

        # 创建一个新的 MPKPackage 实例（内存表示）
        package = MPKPackage() # 不传 file_path
        package._ensure_temp_dir() # 创建临时目录

        # 处理清单文件
        manifest_data = {}
        if manifest_override:
            logger.debug("使用提供的 manifest_override 数据")
            manifest_data = manifest_override
        else:
            source_manifest_path = os.path.join(source_dir, "manifest.json")
            if os.path.exists(source_manifest_path):
                logger.debug(f"从源目录加载 manifest.json: {source_manifest_path}")
                try:
                    with open(source_manifest_path, 'r', encoding='utf-8') as f:
                        manifest_data = json.load(f)
                except (json.JSONDecodeError, UnicodeDecodeError, IOError) as e:
                    raise MPKError(f"读取源目录清单文件失败: {e}")
            else:
                raise MPKError("源目录中未找到 manifest.json 且未提供 manifest_override")

        # 设置并验证清单
        try:
            package.set_manifest(manifest_data)
        except MPKError as e:
            package._cleanup()
            raise MPKError(f"设置清单失败: {e}")

        # 复制源目录内容到临时目录 (排除 manifest.json，因为它已处理)
        # 复制 code, assets, config 等标准目录（如果存在）
        standard_dirs = ["code", "assets", "config"]
        copied_something = False
        try:
            for item_name in os.listdir(source_dir):
                source_item_path = os.path.join(source_dir, item_name)
                relative_item_path = item_name.replace('\\\\', '/')

                if item_name == "manifest.json":
                    continue # 已处理
                if item_name == "signature.sig":
                     logger.warning("源目录中的 signature.sig 文件将被忽略，签名将在打包时重新生成。")
                     continue

                if os.path.isdir(source_item_path):
                    # 确保目标目录在临时目录中创建
                    target_item_dir = os.path.join(package.temp_dir, relative_item_path)
                    # 显式创建目标目录，因为 add_directory 会先删除
                    os.makedirs(os.path.dirname(target_item_dir), exist_ok=True)
                    package.add_directory(source_item_path, relative_item_path)
                    copied_something = True
                elif os.path.isfile(source_item_path):
                    package.add_file(source_item_path, relative_item_path)
                    copied_something = True
                else:
                    logger.warning(f"跳过源目录中不支持的项目: {item_name}")

            # 检查是否至少复制了 code 目录（或入口点文件）
            if not any(f.startswith('code/') for f in package.list_files()) and not os.path.exists(os.path.join(package.temp_dir, package.manifest.get('entry_point','').replace('/',os.sep))):
                logger.warning("源目录中似乎没有 'code/' 目录或清单指定的入口点文件。")
                # raise MPKError("源目录缺少必要的代码文件或 'code/' 目录")

        except MPKError as e:
            package._cleanup()
            raise MPKError(f"从源目录复制文件失败: {e}")
        except Exception as e:
            package._cleanup()
            raise MPKError(f"处理源目录时发生意外错误: {e}")

        # 签名包 (如果提供了私钥)
        if private_key_path:
            logger.info(f"使用私钥 '{private_key_path}' 对包进行签名")
            try:
                package.sign(private_key_path, private_key_password)
            except MPKError as e:
                package._cleanup()
                raise MPKError(f"签名失败: {e}")
        else:
            # 根据规范，未签名的包也应该有 signature.sig 文件，标记为UNSIGNED
            signature_path = os.path.join(package.temp_dir, "signature.sig")
            try:
                with open(signature_path, 'w', encoding='utf-8') as f:
                    f.write("UNSIGNED")
                logger.info("未提供私钥，包标记为 UNSIGNED")
            except Exception as e:
                 package._cleanup()
                 raise MPKError(f"写入UNSIGNED标记失败: {e}")

        # 保存最终的 MPK 包
        try:
            package.save(output_path)
        except MPKError as e:
            # 注意：save失败时，临时目录可能仍然存在，但 package 对象析构时会尝试清理
            raise MPKError(f"最终保存MPK包失败: {e}")

        # 清理临时目录现在由 package 对象的析构函数处理
        # package._cleanup() # 不在这里清理，因为返回的对象可能还需要访问

        logger.info(f"MPK 包已成功创建并保存到: {output_path}")
        return package 