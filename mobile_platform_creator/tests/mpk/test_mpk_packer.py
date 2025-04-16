import unittest
import os
import tempfile
import shutil
import json
from mobile_platform_creator.core.mpk import MPKPacker, MPKFile

class TestMPKPacker(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.output_mpk_path = os.path.join(self.temp_dir, "test_packed.mpk")
        self.code_file_path = os.path.join(self.temp_dir, "main.py")
        with open(self.code_file_path, "w") as f:
            f.write("def main(): print('Hello from packer')")
        self.resource_file_path = os.path.join(self.temp_dir, "data.txt")
        with open(self.resource_file_path, "w") as f:
            f.write("Resource data")
        self.resource_dir_path = os.path.join(self.temp_dir, "assets")
        os.makedirs(self.resource_dir_path, exist_ok=True)
        self.nested_resource_path = os.path.join(self.resource_dir_path, "image.png")
        with open(self.nested_resource_path, "w") as f:
            f.write("PNG data")
        self.config_file_path = os.path.join(self.temp_dir, "config.json")
        self.sig_data_path = os.path.join(self.temp_dir, "sig.dat")
        with open(self.sig_data_path, "wb") as f:
            f.write(b"signature_bytes")
        self.cert_path = os.path.join(self.temp_dir, "cert.pem")
        with open(self.cert_path, "w") as f:
            f.write("certificate_string")

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def _create_config(self):
        config = {
            "metadata": {
                "app_name": "PackerApp",
                "package_name": "com.test.packer",
                "version": "1.1",
                "author": "PackerTester"
            },
            "code": {
                "path": "main.py",
                "type": "script",
                "entry_point": "main"
            },
            "resources": [
                {"path": "data.txt", "type": "text"},
                {"path": "assets", "type": "assets_dir"}
            ],
            "signature": {
                "algorithm": "RSA-SHA256",
                "data_path": "sig.dat",
                "certificate_path": "cert.pem"
            }
        }
        with open(self.config_file_path, "w", encoding="utf-8") as f:
            json.dump(config, f, indent=2)
        return config

    def test_01_set_metadata(self):
        """测试设置元数据"""
        packer = MPKPacker()
        metadata = {"app_name": "MetaApp", "version": "0.1"}
        packer.set_metadata(metadata)
        self.assertEqual(packer.mpk.metadata.app_name, "MetaApp")
        self.assertEqual(packer.mpk.metadata.version, "0.1")

    def test_02_add_code(self):
        """测试添加代码"""
        packer = MPKPacker()
        packer.add_code(self.code_file_path, code_type="python_script", entry_point="start")
        self.assertEqual(packer.mpk.code_section.code_type, "python_script")
        self.assertEqual(packer.mpk.code_section.entry_point, "start")
        with open(self.code_file_path, "rb") as f:
            self.assertEqual(packer.mpk.code_section.code_content, f.read())

    def test_03_add_resource(self):
        """测试添加单个资源文件"""
        packer = MPKPacker()
        packer.add_resource(self.resource_file_path, "config")
        self.assertEqual(len(packer.mpk.resource_sections), 1)
        resource = packer.mpk.resource_sections[0]
        self.assertEqual(resource.resource_type, "config")
        with open(self.resource_file_path, "rb") as f:
            self.assertEqual(resource.resource_content, f.read())

    def test_04_add_directory(self):
        """测试添加目录资源"""
        packer = MPKPacker()
        packer.add_directory(self.resource_dir_path, "asset_bundle")
        self.assertEqual(len(packer.mpk.resource_sections), 1)
        resource = packer.mpk.resource_sections[0]
        self.assertEqual(resource.resource_type, "asset_bundle")
        # 检查内容是否为zip压缩数据（简单检查）
        self.assertTrue(resource.resource_content.startswith(b'PK\x03\x04'))

    def test_05_set_signature(self):
        """测试设置签名"""
        packer = MPKPacker()
        sig_data = b'randomsigbytes'
        cert_str = "MyCertString"
        packer.set_signature("ECDSA", sig_data, cert_str)
        self.assertEqual(packer.mpk.signature_section.algorithm, "ECDSA")
        self.assertEqual(packer.mpk.signature_section.signature_data, sig_data)
        self.assertEqual(packer.mpk.signature_section.certificate, cert_str)

    def test_06_pack(self):
        """测试打包生成MPK文件"""
        packer = MPKPacker()
        packer.set_metadata({"app_name": "PackTest", "version": "1.0"})
        packer.add_code(self.code_file_path)
        packer.add_resource(self.resource_file_path, "data")
        packer.pack(self.output_mpk_path)

        self.assertTrue(os.path.exists(self.output_mpk_path))

        # 尝试加载打包后的文件进行基本验证
        try:
            loaded_mpk = MPKFile.load(self.output_mpk_path)
            self.assertEqual(loaded_mpk.metadata.app_name, "PackTest")
            self.assertEqual(len(loaded_mpk.resource_sections), 1)
        except ValueError as e:
            self.fail(f"打包后的MPK文件加载失败: {e}")

    def test_07_from_config(self):
        """测试从配置文件创建并打包"""
        config = self._create_config()
        packer = MPKPacker.from_config(self.config_file_path)

        # 验证从配置中读取的信息
        self.assertEqual(packer.mpk.metadata.app_name, config["metadata"]["app_name"])
        self.assertEqual(packer.mpk.code_section.entry_point, config["code"]["entry_point"])
        self.assertEqual(len(packer.mpk.resource_sections), 2) # data.txt + assets dir
        self.assertEqual(packer.mpk.signature_section.algorithm, config["signature"]["algorithm"])

        # 打包
        packer.pack(self.output_mpk_path)
        self.assertTrue(os.path.exists(self.output_mpk_path))

        # 验证打包后的文件
        loaded_mpk = MPKFile.load(self.output_mpk_path)
        self.assertEqual(loaded_mpk.metadata.package_name, config["metadata"]["package_name"])
        # 查找目录资源
        dir_resource = next((r for r in loaded_mpk.resource_sections if r.resource_type == "assets_dir"), None)
        self.assertIsNotNone(dir_resource)
        self.assertTrue(dir_resource.resource_content.startswith(b'PK\x03\x04'))

    def test_08_verify_and_extract(self):
        """测试打包后文件的验证和资源提取功能"""
        packer = MPKPacker()
        packer.set_metadata({"app_name": "VerifyExtract", "version": "1.0"})
        packer.add_code(self.code_file_path)
        packer.add_resource(self.resource_file_path, "readme")
        packer.pack(self.output_mpk_path)

        # 使用 Packer 实例的方法
        self.assertTrue(packer.verify()) # 依赖 MPKFile.verify (目前占位符)

        extract_dir = os.path.join(self.temp_dir, "packer_extract")
        packer.extract_resources(extract_dir)
        self.assertTrue(os.path.exists(extract_dir))
        extracted_files = os.listdir(extract_dir)
        self.assertEqual(len(extracted_files), 1)
        self.assertIn("resource_readme_0.bin", extracted_files)
        with open(os.path.join(extract_dir, "resource_readme_0.bin"), "r") as f:
            self.assertEqual(f.read(), "Resource data")

    def test_09_get_resource(self):
        """测试从Packer获取资源"""
        packer = MPKPacker()
        packer.add_resource(self.resource_file_path, "my_data")
        resource_section = packer.get_resource("my_data")
        self.assertIsNotNone(resource_section)
        self.assertEqual(resource_section.resource_type, "my_data")
        with open(self.resource_file_path, "rb") as f:
            self.assertEqual(resource_section.resource_content, f.read())

        self.assertIsNone(packer.get_resource("not_exist"))


if __name__ == '__main__':
    unittest.main() 