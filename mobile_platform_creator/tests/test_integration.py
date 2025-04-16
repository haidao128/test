import unittest
import os
import tempfile
import shutil
import json
import sys
from io import StringIO
from unittest.mock import patch

# 导入需要测试的模块
from mobile_platform_creator.core.mpk.dev_tools import DevTools
from mobile_platform_creator.core.mpk.runtime import Runtime
from mobile_platform_creator.core.mpk import MPKFile # 用于验证

class TestIntegration(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        """为所有集成测试创建一个主临时目录"""
        cls.main_temp_dir = tempfile.mkdtemp(prefix="mpk_integration_")
        print(f"\nIntegration test directory: {cls.main_temp_dir}")

        # 定义集成测试中使用的路径
        cls.project_dir = os.path.join(cls.main_temp_dir, "MyIntegrationApp")
        cls.build_dir = os.path.join(cls.project_dir, "dist") # 构建输出目录
        cls.analysis_dir = os.path.join(cls.main_temp_dir, "analysis_output")

        # 为 Runtime 指定一个位于集成测试目录下的安装位置
        cls.runtime_install_dir = os.path.join(cls.main_temp_dir, "installed_apps")
        cls.runtime_registry_path = os.path.join(cls.runtime_install_dir, "registry.json")

        # --- Patch Runtime 的安装路径和注册表路径 --- 
        # 这确保集成测试不会干扰默认的 mpk_installed 目录，并且是独立的
        cls.patcher_install_dir = patch('mobile_platform_creator.core.mpk.runtime.Runtime.install_dir', cls.runtime_install_dir)
        cls.patcher_registry_path = patch('mobile_platform_creator.core.mpk.runtime.Runtime.registry_path', cls.runtime_registry_path)
        cls.patcher_install_dir.start()
        cls.patcher_registry_path.start()

        # --- 获取 DevTools 和 Runtime 实例 --- 
        # 注意：获取实例必须在 patch 之后，以便它们使用正确的路径
        # 同样需要重置单例，以防其他测试修改了状态
        cls.patcher_runtime_instance = patch('mobile_platform_creator.core.mpk.runtime.Runtime._instance', None)
        cls.patcher_devtools_instance = patch('mobile_platform_creator.core.mpk.dev_tools.DevTools._instance', None)
        cls.patcher_runtime_instance.start()
        cls.patcher_devtools_instance.start()

        cls.dev_tools = DevTools.get_instance()
        cls.runtime = Runtime.get_instance()

        # 禁用日志以保持输出清洁 (可以选择性保留 INFO)
        cls.patcher_dev_log = patch('mobile_platform_creator.core.mpk.dev_tools.logging.disable')
        cls.patcher_run_log = patch('mobile_platform_creator.core.mpk.runtime.logging.disable')
        mock_dev_log_disable = cls.patcher_dev_log.start()
        mock_run_log_disable = cls.patcher_run_log.start()
        # mock_dev_log_disable(logging.INFO) # 如果需要看INFO日志
        # mock_run_log_disable(logging.INFO)
        mock_dev_log_disable(999) # 禁用所有
        mock_run_log_disable(999)

    @classmethod
    def tearDownClass(cls):
        """清理所有集成测试资源"""
        # 停止 patchers
        cls.patcher_dev_log.stop()
        cls.patcher_run_log.stop()
        cls.patcher_devtools_instance.stop()
        cls.patcher_runtime_instance.stop()
        cls.patcher_registry_path.stop()
        cls.patcher_install_dir.stop()

        # 删除主临时目录
        try:
            shutil.rmtree(cls.main_temp_dir)
            print(f"Cleaned up integration test directory: {cls.main_temp_dir}")
        except OSError as e:
            print(f"Error cleaning up temp dir {cls.main_temp_dir}: {e}")

    # setUp 和 tearDown 可以用于每个测试方法前的特定准备和清理（如果需要）
    # def setUp(self):
    #     pass
    # def tearDown(self):
    #     pass

    def test_01_full_lifecycle(self):
        """测试完整的应用生命周期：创建->构建->安装->运行->分析->卸载"""

        # --- 1. 创建项目 --- 
        print("\nStep 1: Creating project...")
        config_data = {
            "metadata": {
                "app_name": "IntegrationTestApp",
                "package_name": "com.integration.test",
                "version": "1.0.0",
                "author": "IntegrationTester",
                "dependencies": ["json"], # 添加一个实际依赖
                "permissions": ["network"]
            },
             # 我们将在下一步手动创建文件，因此这里路径是预期的
            "code": {"path": "src/main.py", "entry_point": "run_integration"},
            "resources": [{"path": "assets/config.json", "type": "config"}]
        }
        result_create = self.dev_tools.create_project(self.project_dir, config_data)
        self.assertTrue(result_create, "Failed to create project")
        self.assertTrue(os.path.isdir(self.project_dir))
        self.assertTrue(os.path.exists(os.path.join(self.project_dir, "mpk_config.json")))
        print("Step 1: Project created successfully.")

        # --- 2. 准备项目文件 --- 
        print("Step 2: Preparing project files...")
        src_dir = os.path.join(self.project_dir, "src")
        assets_dir = os.path.join(self.project_dir, "assets")
        os.makedirs(src_dir, exist_ok=True)
        os.makedirs(assets_dir, exist_ok=True)

        # 写入代码文件
        code_content = """
import json
import os

def run_integration():
    print("Integration test code running!")
    resource_dir = os.getenv("MPK_RESOURCE_DIR", ".") # Loader会设置这个环境变量
    config_path = os.path.join(resource_dir, "resource_config_0.bin") # Loader提取后默认文件名
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config_data = json.load(f)
            print(f"Read config: {config_data.get('message')}")
        return True # 表示成功
    except Exception as e:
        print(f"Error reading resource config: {e}")
        return False # 表示失败
"""
        with open(os.path.join(src_dir, "main.py"), "w", encoding="utf-8") as f:
            f.write(code_content)

        # 写入资源文件
        resource_data = {"message": "Hello from integration resource!"}
        with open(os.path.join(assets_dir, "config.json"), "w", encoding="utf-8") as f:
            json.dump(resource_data, f, indent=2)
        print("Step 2: Project files prepared.")

        # --- 3. 构建项目 --- 
        print("Step 3: Building project...")
        result_build = self.dev_tools.build_project(self.project_dir, self.build_dir)
        self.assertTrue(result_build, "Failed to build project")
        # 确定预期的 MPK 文件名
        app_name = config_data["metadata"]["package_name"]
        app_version = config_data["metadata"]["version"]
        expected_mpk_filename = f"{app_name}_{app_version}.mpk"
        self.built_mpk_path = os.path.join(self.build_dir, expected_mpk_filename)
        self.assertTrue(os.path.exists(self.built_mpk_path), f"Built MPK file not found: {self.built_mpk_path}")
        self.app_id = f"{app_name}_{app_version}"
        print(f"Step 3: Project built successfully: {self.built_mpk_path}")

        # --- 4. (可选) 验证构建结果 --- 
        try:
             mpk_verify = MPKFile.load(self.built_mpk_path)
             self.assertEqual(mpk_verify.metadata.app_name, "IntegrationTestApp")
        except Exception as e:
             self.fail(f"Failed to load and verify the built MPK file: {e}")

        # --- 5. 安装应用 --- 
        print("Step 5: Installing application...")
        result_install = self.dev_tools.install_package(self.built_mpk_path)
        self.assertTrue(result_install, "Failed to install package")
        print(f"Step 5: Application {self.app_id} installed.")

        # --- 6. 验证安装结果 --- 
        print("Step 6: Verifying installation...")
        self.assertTrue(os.path.exists(self.runtime_install_dir), "Install directory not created")
        self.assertTrue(os.path.exists(self.runtime_registry_path), "Registry file not created")
        installed_mpk_target_path = os.path.join(self.runtime_install_dir, f"{self.app_id}.mpk")
        self.assertTrue(os.path.exists(installed_mpk_target_path), "Installed MPK file not found in install dir")
        registry_data = self.runtime._load_registry() # 直接读取文件或通过 runtime 加载
        self.assertIn(self.app_id, registry_data, "App ID not found in registry")
        self.assertEqual(registry_data[self.app_id]["app_name"], "IntegrationTestApp")
        print("Step 6: Installation verified.")

        # --- 7. 加载已安装应用 --- 
        print("Step 7: Loading installed application into runtime...")
        result_load = self.runtime.load_app(self.app_id)
        self.assertTrue(result_load, f"Failed to load installed app {self.app_id}")
        self.assertIn(self.app_id, self.runtime.loaders, "App not found in runtime loaders after load")
        print(f"Step 7: Application {self.app_id} loaded.")

        # --- 8. 运行已加载应用 --- 
        print("Step 8: Running loaded application...")
        # 捕获标准输出以验证代码执行
        captured_output = StringIO()
        original_stdout = sys.stdout
        sys.stdout = captured_output
        try:
            result_run = self.runtime.run_app(self.app_id)
        finally:
            sys.stdout = original_stdout # 确保恢复标准输出
        run_output = captured_output.getvalue()

        self.assertTrue(result_run, f"Failed to run loaded app {self.app_id}\nOutput:\n{run_output}")
        self.assertIn("Integration test code running!", run_output, "Expected output missing from run")
        self.assertIn("Read config: Hello from integration resource!", run_output, "Failed to read resource in run")
        print(f"Step 8: Application run successful. Output:\n{run_output.strip()}")

        # --- 8a. 卸载运行时应用 (非物理卸载) --- 
        print("Step 8a: Unloading application from runtime...")
        self.runtime.unload_app(self.app_id)
        self.assertNotIn(self.app_id, self.runtime.loaders, "App still in runtime loaders after unload")
        # 验证安装的文件仍然存在
        self.assertTrue(os.path.exists(installed_mpk_target_path), "Installed MPK file removed during runtime unload")
        print("Step 8a: Runtime unload successful.")

        # --- 9. 分析应用 --- 
        print("Step 9: Analyzing installed application package...")
        # 注意：分析的是已安装的包，而不是原始构建的包
        result_analyze = self.dev_tools.analyze_project(installed_mpk_target_path, self.analysis_dir)
        self.assertTrue(result_analyze, "Failed to analyze project")
        self.assertTrue(os.path.isdir(self.analysis_dir), "Analysis directory not created")
        # 检查报告和资源提取
        report_files = [f for f in os.listdir(self.analysis_dir) if f.startswith("mpk_report") and f.endswith(".json")]
        self.assertEqual(len(report_files), 1, "Analysis report not found or multiple reports found")
        resource_extract_dir = os.path.join(self.analysis_dir, "resources")
        self.assertTrue(os.path.isdir(resource_extract_dir), "Analysis resource directory not found")
        extracted_files = os.listdir(resource_extract_dir)
        self.assertIn("resource_config_0.bin", extracted_files, "Extracted resource file not found")
        print("Step 9: Analysis successful.")

        # --- 10. 卸载应用 (物理卸载) --- 
        print("Step 10: Uninstalling application package...")
        result_uninstall = self.dev_tools.uninstall_package(self.app_id)
        self.assertTrue(result_uninstall, "Failed to uninstall package")
        print(f"Step 10: Application {self.app_id} uninstalled.")

        # --- 11. 验证卸载结果 --- 
        print("Step 11: Verifying uninstallation...")
        self.assertFalse(os.path.exists(installed_mpk_target_path), "Installed MPK file not deleted after uninstall")
        registry_data_after = self.runtime._load_registry()
        self.assertNotIn(self.app_id, registry_data_after, "App ID still found in registry after uninstall")
        print("Step 11: Uninstallation verified.")

        # --- 12. (可选) 尝试加载已卸载应用 --- 
        print("Step 12: Attempting to load uninstalled app...")
        result_load_after = self.runtime.load_app(self.app_id)
        self.assertFalse(result_load_after, "Loading uninstalled app should fail")
        print("Step 12: Loading uninstalled app failed as expected.")
        print("\nIntegration test completed successfully!")

# 需要添加导入
from io import StringIO
import sys

if __name__ == '__main__':
    unittest.main() 