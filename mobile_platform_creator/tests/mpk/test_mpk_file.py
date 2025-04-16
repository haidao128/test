import unittest
import os
import tempfile
import shutil
import hashlib
from mobile_platform_creator.core.mpk import MPKFile, Metadata, CodeSection, ResourceSection, SignatureSection

class TestMPKFile(unittest.TestCase):

    def setUp(self):
        """设置测试环境，创建临时目录"""
        self.temp_dir = tempfile.mkdtemp()
        self.test_file_path = os.path.join(self.temp_dir, "test.mpk")
        self.resource_file_path = os.path.join(self.temp_dir, "resource1.txt")
        with open(self.resource_file_path, "w") as f:
            f.write("This is a test resource.")
        self.output_resource_dir = os.path.join(self.temp_dir, "extracted_resources")

    def tearDown(self):
        """清理测试环境，删除临时目录"""
        shutil.rmtree(self.temp_dir)

    def _create_sample_mpk(self) -> MPKFile:
        """创建一个示例 MPKFile 对象"""
        mpk = MPKFile()
        mpk.metadata = Metadata(
            app_name="TestApp",
            package_name="com.test.app",
            version="1.0",
            version_name="1.0.0",
            author="Tester",
            create_date="2024-01-01",
            permissions=["test.perm"],
            dependencies=["test.dep"],
            min_platform_version="1.0",
            icon="icon.png",
            splash="splash.png"
        )
        mpk.code_section = CodeSection(
            code_type="script",
            code_content=b'print("Hello")',
            entry_point="main"
        )
        resource = ResourceSection(
            resource_type="text",
            resource_content=b"This is resource content."
        )
        mpk.resource_sections.append(resource)
        mpk.signature_section = SignatureSection(
            algorithm="SHA256",
            signature_data=b"testsig",
            certificate="testcert"
        )
        return mpk

    def test_01_create_and_pack_unpack(self):
        """测试创建MPK对象和打包/解包数据"""
        mpk = self._create_sample_mpk()
        self.assertEqual(mpk.magic_number, b'MPK1')
        self.assertEqual(mpk.version, b'1.0 ')

        # 测试打包
        packed_data = mpk._pack_data()
        self.assertIsInstance(packed_data, bytes)

        # 测试解包
        new_mpk = MPKFile()
        new_mpk._unpack_data(packed_data)

        self.assertEqual(new_mpk.metadata.app_name, mpk.metadata.app_name)
        self.assertEqual(new_mpk.code_section.code_content, mpk.code_section.code_content)
        self.assertEqual(len(new_mpk.resource_sections), 1)
        self.assertEqual(new_mpk.resource_sections[0].resource_content, mpk.resource_sections[0].resource_content)
        self.assertEqual(new_mpk.signature_section.signature_data, mpk.signature_section.signature_data)

    def test_02_calculate_checksum(self):
        """测试校验和计算"""
        mpk = self._create_sample_mpk()
        checksum = mpk.calculate_checksum()
        self.assertIsInstance(checksum, bytes)
        self.assertEqual(len(checksum), 32) # SHA256

    def test_03_save_and_load(self):
        """测试保存和加载MPK文件"""
        mpk = self._create_sample_mpk()
        mpk.save(self.test_file_path)
        self.assertTrue(os.path.exists(self.test_file_path))

        loaded_mpk = MPKFile.load(self.test_file_path)
        self.assertIsInstance(loaded_mpk, MPKFile)
        self.assertEqual(loaded_mpk.metadata.package_name, mpk.metadata.package_name)
        self.assertEqual(loaded_mpk.code_section.entry_point, mpk.code_section.entry_point)
        self.assertEqual(len(loaded_mpk.resource_sections), 1)
        self.assertEqual(loaded_mpk.signature_section.certificate, mpk.signature_section.certificate)

        # 测试校验和验证
        loaded_checksum = loaded_mpk.checksum
        calculated_checksum = loaded_mpk.calculate_checksum()
        self.assertEqual(loaded_checksum, calculated_checksum)

    def test_04_load_invalid_file(self):
        """测试加载无效的MPK文件"""
        # 测试空文件
        with open(self.test_file_path, "w") as f:
            f.write("")
        with self.assertRaises(ValueError):
            MPKFile.load(self.test_file_path)

        # 测试错误魔数
        mpk = self._create_sample_mpk()
        mpk.magic_number = b'XXXX'
        mpk.save(self.test_file_path)
        with self.assertRaisesRegex(ValueError, "无效的魔数"):
             MPKFile.load(self.test_file_path)

        # 测试错误版本
        mpk = self._create_sample_mpk()
        mpk.version = b'2.0 '
        mpk.save(self.test_file_path)
        with self.assertRaisesRegex(ValueError, "不支持的版本"):
             MPKFile.load(self.test_file_path)

        # 测试校验和不匹配
        mpk = self._create_sample_mpk()
        mpk.save(self.test_file_path)
        # 手动修改文件内容破坏校验和
        with open(self.test_file_path, "r+b") as f:
            f.seek(100) # 移动到某个位置
            f.write(b'corrupt')
        with self.assertRaisesRegex(ValueError, "校验和不匹配"):
            MPKFile.load(self.test_file_path)

    def test_05_verify_signature_placeholder(self):
        """测试签名验证（占位符）"""
        mpk = self._create_sample_mpk()
        # 当前签名验证总是返回 True
        self.assertTrue(mpk.verify())

    def test_06_extract_resources(self):
        """测试资源提取"""
        mpk = self._create_sample_mpk()
        # 添加一个使用文件的资源
        with open(self.resource_file_path, "rb") as f:
            content = f.read()
        file_resource = ResourceSection(resource_type="file", resource_content=content)
        mpk.resource_sections.append(file_resource)
        mpk.save(self.test_file_path)

        loaded_mpk = MPKFile.load(self.test_file_path)
        loaded_mpk.extract_resources(self.output_resource_dir)

        self.assertTrue(os.path.exists(self.output_resource_dir))
        # 应该提取了2个资源: text 和 file
        extracted_files = os.listdir(self.output_resource_dir)
        self.assertEqual(len(extracted_files), 2)
        self.assertIn("resource_text_0.bin", extracted_files)
        self.assertIn("resource_file_1.bin", extracted_files)

        # 检查文件内容
        with open(os.path.join(self.output_resource_dir, "resource_file_1.bin"), "rb") as f:
            extracted_content = f.read()
        self.assertEqual(extracted_content, content)

    def test_07_get_resource(self):
        """测试获取指定类型的资源"""
        mpk = self._create_sample_mpk()
        mpk.save(self.test_file_path)

        loaded_mpk = MPKFile.load(self.test_file_path)
        resource = loaded_mpk.get_resource("text")
        self.assertIsNotNone(resource)
        self.assertEqual(resource.resource_type, "text")
        self.assertEqual(resource.resource_content, b"This is resource content.")

        non_existent_resource = loaded_mpk.get_resource("nonexistent")
        self.assertIsNone(non_existent_resource)

if __name__ == '__main__':
    unittest.main() 