import unittest
import os
import tempfile
import shutil
import json
from mobile_platform_creator.core.mpk import MPKViewer, MPKFile, MPKPacker

class TestMPKViewer(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        """在所有测试开始前，创建一个临时的MPK文件"""
        cls.temp_dir = tempfile.mkdtemp()
        cls.mpk_path = os.path.join(cls.temp_dir, "viewer_test.mpk")
        cls.code_path = os.path.join(cls.temp_dir, "code.py")
        with open(cls.code_path, "w") as f:
            f.write("def main():\n    '''Main entry point docstring'''\n    print('Viewer test')")
        cls.res_path = os.path.join(cls.temp_dir, "res.txt")
        with open(cls.res_path, "w") as f:
            f.write("Viewer resource")

        packer = MPKPacker()
        packer.set_metadata({
            "app_name": "ViewerApp", "package_name": "com.viewer.test",
            "version": "2.0", "version_name": "2.0-beta", "author": "ViewerTester",
            "create_date": "2024-02-01", "permissions": ["network"],
            "dependencies": ["json"], "min_platform_version": "1.1",
            "icon": "app_icon.ico", "splash": "splash_screen.jpg"
        })
        packer.add_code(cls.code_path, entry_point="main")
        packer.add_resource(cls.res_path, "text_data")
        packer.set_signature("SHA1", b"viewersig", "viewercert")
        packer.pack(cls.mpk_path)

        cls.mpk_file = MPKFile.load(cls.mpk_path)
        cls.viewer = MPKViewer(cls.mpk_file)

    @classmethod
    def tearDownClass(cls):
        """在所有测试结束后，清理临时文件和目录"""
        shutil.rmtree(cls.temp_dir)

    def test_01_get_metadata(self):
        """测试获取元数据"""
        metadata = self.viewer.get_metadata()
        self.assertEqual(metadata["应用名称"], "ViewerApp")
        self.assertEqual(metadata["包名"], "com.viewer.test")
        self.assertEqual(metadata["版本"], "2.0")
        self.assertEqual(metadata["作者"], "ViewerTester")
        self.assertIn("network", metadata["权限列表"])
        self.assertIn("json", metadata["依赖项"])

    def test_02_get_code_info(self):
        """测试获取代码信息"""
        code_info = self.viewer.get_code_info()
        self.assertEqual(code_info["代码类型"], "script") # 默认是 script
        self.assertEqual(code_info["入口点"], "main")
        self.assertTrue(code_info["代码大小"].endswith("字节"))

    def test_03_get_resources_info(self):
        """测试获取资源信息"""
        resources_info = self.viewer.get_resources_info()
        self.assertEqual(len(resources_info), 1)
        self.assertEqual(resources_info[0]["资源类型"], "text_data")
        self.assertTrue(resources_info[0]["资源大小"].endswith("字节"))

    def test_04_get_signature_info(self):
        """测试获取签名信息"""
        signature_info = self.viewer.get_signature_info()
        self.assertEqual(signature_info["签名算法"], "SHA1")
        self.assertIn("viewersig", signature_info["签名数据"]) # Base64 encoded
        self.assertEqual(signature_info["证书"], "viewercert")

    def test_05_get_file_info(self):
        """测试获取文件信息"""
        file_info = self.viewer.get_file_info()
        self.assertEqual(file_info["魔数"], "MPK1")
        self.assertEqual(file_info["版本"], "1.0 ")
        self.assertTrue(file_info["文件大小"].endswith("字节"))
        self.assertTrue(len(file_info["校验和"]) > 0)

    def test_06_verify(self):
        """测试文件验证"""
        verify_result = self.viewer.verify()
        self.assertTrue(verify_result["魔数验证"])
        self.assertTrue(verify_result["版本验证"])
        self.assertTrue(verify_result["校验和验证"])
        self.assertTrue(verify_result["签名验证"]) # 依赖 MPKFile.verify

    def test_07_generate_report(self):
        """测试生成报告"""
        report_dir = os.path.join(self.temp_dir, "reports")
        report_path = self.viewer.generate_report(report_dir)
        self.assertTrue(os.path.exists(report_path))
        self.assertTrue(report_path.endswith(".json"))

        with open(report_path, "r", encoding="utf-8") as f:
            report_data = json.load(f)

        self.assertIn("文件信息", report_data)
        self.assertIn("元数据", report_data)
        self.assertIn("代码信息", report_data)
        self.assertIn("资源信息", report_data)
        self.assertIn("签名信息", report_data)
        self.assertIn("验证结果", report_data)
        self.assertEqual(report_data["元数据"]["应用名称"], "ViewerApp")
        self.assertTrue(report_data["验证结果"]["校验和验证"])

    def test_08_extract_resources(self):
        """测试提取资源"""
        extract_dir = os.path.join(self.temp_dir, "viewer_extract")
        self.viewer.extract_resources(extract_dir)
        self.assertTrue(os.path.exists(extract_dir))
        extracted_files = os.listdir(extract_dir)
        self.assertEqual(len(extracted_files), 1)
        self.assertIn("resource_text_data_0.bin", extracted_files)
        with open(os.path.join(extract_dir, "resource_text_data_0.bin"), "r") as f:
            self.assertEqual(f.read(), "Viewer resource")

    def test_09_get_resource(self):
        """测试获取特定资源内容"""
        content = self.viewer.get_resource("text_data")
        self.assertIsNotNone(content)
        self.assertEqual(content, b"Viewer resource")

        no_content = self.viewer.get_resource("no_such_resource")
        self.assertEqual(no_content, b"")

    def test_10_from_file(self):
        """测试从文件创建查看器"""
        viewer_from_file = MPKViewer.from_file(self.mpk_path)
        self.assertIsInstance(viewer_from_file, MPKViewer)
        metadata = viewer_from_file.get_metadata()
        self.assertEqual(metadata["应用名称"], "ViewerApp")

    # test_print_info 可以在本地手动运行检查输出，自动化测试较难验证标准输出
    # def test_11_print_info(self):
    #     """测试打印信息（手动检查输出）"""
    #     print("\n--- Testing print_info --- (Manual Check Needed)")
    #     self.viewer.print_info()
    #     print("--- End print_info Test ---")

if __name__ == '__main__':
    unittest.main() 