import unittest
import os
import tempfile
import shutil
import logging
import json
from unittest.mock import patch, MagicMock, call
from mobile_platform_creator.core.mpk import MPKFile, MPKPacker
from mobile_platform_creator.core.mpk.runtime import Runtime
from datetime import datetime

# Disable runtime logging to avoid cluttering test output
logging.disable(logging.CRITICAL)

class TestRuntime(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # Create a main temporary directory for all test artifacts
        cls.main_temp_dir = tempfile.mkdtemp()
        # Define paths within the main temp dir
        cls.mpk_source_dir = os.path.join(cls.main_temp_dir, "mpk_sources")
        os.makedirs(cls.mpk_source_dir, exist_ok=True)

        cls.mpk_valid_path = os.path.join(cls.mpk_source_dir, "runtime_valid.mpk")
        cls.mpk_valid_v2_path = os.path.join(cls.mpk_source_dir, "runtime_valid_v2.mpk")
        cls.mpk_invalid_meta_path = os.path.join(cls.mpk_source_dir, "runtime_invalid_meta.mpk")
        cls.mpk_invalid_file_path = os.path.join(cls.mpk_source_dir, "runtime_invalid_file.mpk")
        cls.mpk_nodeps_path = os.path.join(cls.mpk_source_dir, "runtime_nodeps.mpk") # For load test
        cls.mpk_badperms_path = os.path.join(cls.mpk_source_dir, "runtime_badperms.mpk") # For load test

        # --- Create various MPK files for testing ---
        # Valid MPK v1.0
        packer_valid = MPKPacker()
        packer_valid.set_metadata({
            "app_name": "RuntimeApp", "package_name": "com.runtime.test", "version": "1.0",
            "dependencies": ["os"], "permissions": ["file.read"]
        })
        code_path_valid = os.path.join(cls.mpk_source_dir, "code_runtime_v1.py")
        with open(code_path_valid, "w") as f: f.write("def main(): print('Runtime v1')")
        packer_valid.add_code(code_path_valid, entry_point="main")
        res_path_valid = os.path.join(cls.mpk_source_dir, "res_runtime_v1.txt")
        with open(res_path_valid, "w") as f: f.write("Runtime v1 resource")
        packer_valid.add_resource(res_path_valid, "config")
        packer_valid.pack(cls.mpk_valid_path)
        cls.valid_app_id_v1 = "com.runtime.test_1.0"

        # Valid MPK v2.0 (same package name)
        packer_valid_v2 = MPKPacker()
        packer_valid_v2.set_metadata({
            "app_name": "RuntimeApp", "package_name": "com.runtime.test", "version": "2.0",
             "dependencies": ["json"], "permissions": ["network"]
        })
        code_path_valid_v2 = os.path.join(cls.mpk_source_dir, "code_runtime_v2.py")
        with open(code_path_valid_v2, "w") as f: f.write("def main(): print('Runtime v2')")
        packer_valid_v2.add_code(code_path_valid_v2, entry_point="main")
        packer_valid_v2.pack(cls.mpk_valid_v2_path)
        cls.valid_app_id_v2 = "com.runtime.test_2.0"

        # MPK with invalid metadata (missing version)
        packer_invalid_meta = MPKPacker()
        packer_invalid_meta.set_metadata({"app_name": "InvalidMetaApp", "package_name": "com.invalid.meta"})
        code_path_invalid_meta = os.path.join(cls.mpk_source_dir, "code_invalid_meta.py")
        with open(code_path_invalid_meta, "w") as f: f.write("def main(): pass")
        packer_invalid_meta.add_code(code_path_invalid_meta)
        packer_invalid_meta.pack(cls.mpk_invalid_meta_path)

        # MPK file that will be corrupted after creation
        packer_corrupt = MPKPacker()
        packer_corrupt.set_metadata({"app_name": "CorruptApp", "package_name": "com.corrupt", "version":"1.0"})
        code_path_corrupt = os.path.join(cls.mpk_source_dir, "code_corrupt.py")
        with open(code_path_corrupt, "w") as f: f.write("def main(): pass")
        packer_corrupt.add_code(code_path_corrupt)
        packer_corrupt.pack(cls.mpk_invalid_file_path)
        # Corrupt the file after packing
        with open(cls.mpk_invalid_file_path, "r+b") as f:
            f.seek(5) # Corrupt header
            f.write(b"BAD!")

        # MPK with missing dependency (for load test)
        packer_nodeps = MPKPacker()
        packer_nodeps.set_metadata({
            "app_name": "NoDepsLoad", "package_name": "com.nodeps.load", "version": "1.0",
            "dependencies": ["non_existent_module_runtime"]
        })
        code_path_nodeps = os.path.join(cls.mpk_source_dir, "code_nodeps_load.py")
        with open(code_path_nodeps, "w") as f: f.write("def main(): pass")
        packer_nodeps.add_code(code_path_nodeps)
        packer_nodeps.pack(cls.mpk_nodeps_path)
        cls.nodeps_app_id = "com.nodeps.load_1.0"

        # MPK with bad permission (for load test)
        packer_badperms = MPKPacker()
        packer_badperms.set_metadata({
            "app_name": "BadPermsLoad", "package_name": "com.badperms.load", "version": "1.0",
            "permissions": ["kernel.panic"]
        })
        code_path_badperms = os.path.join(cls.mpk_source_dir, "code_badperms_load.py")
        with open(code_path_badperms, "w") as f: f.write("def main(): pass")
        packer_badperms.add_code(code_path_badperms)
        packer_badperms.pack(cls.mpk_badperms_path)
        cls.badperms_app_id = "com.badperms.load_1.0"


    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.main_temp_dir)
        logging.disable(logging.NOTSET) # Restore logging

    def setUp(self):
        """Set up a clean runtime instance and install directory for each test."""
        # Use patch to ensure a clean singleton instance and install state
        self.patcher_instance = patch('mobile_platform_creator.core.mpk.runtime.Runtime._instance', None)
        self.patcher_instance.start()

        # Define install dir specific to this test run
        self.test_install_dir = os.path.join(self.main_temp_dir, f"install_{self._testMethodName}")
        self.test_registry_path = os.path.join(self.test_install_dir, "registry.json")

        # Patch the Runtime's install_dir and registry_path BEFORE creating the instance
        self.patcher_install_dir = patch('mobile_platform_creator.core.mpk.runtime.Runtime.install_dir', self.test_install_dir)
        self.patcher_registry_path = patch('mobile_platform_creator.core.mpk.runtime.Runtime.registry_path', self.test_registry_path)
        self.patcher_install_dir.start()
        self.patcher_registry_path.start()

        self.runtime = Runtime.get_instance()
        # Ensure the test-specific install dir exists after instance creation
        self.runtime._ensure_install_dir()
        # Load registry specific to this test
        self.runtime.registry = self.runtime._load_registry()

    def tearDown(self):
        """Clean up runtime instance and patched paths."""
        self.runtime.cleanup()
        self.patcher_registry_path.stop()
        self.patcher_install_dir.stop()
        self.patcher_instance.stop()
        # Clean the specific install dir used by the test
        if os.path.exists(self.test_install_dir):
            shutil.rmtree(self.test_install_dir)

    def test_01_get_instance(self):
        """Test getting the runtime singleton."""
        instance1 = Runtime.get_instance()
        instance2 = Runtime.get_instance()
        self.assertIs(instance1, instance2)
        self.assertIs(self.runtime, instance1)

    # --- Installation Tests ---
    def test_10_install_app_success(self):
        """Test successful app installation."""
        install_handler = MagicMock()
        self.runtime.register_event_handler("install", install_handler)

        app_id = self.runtime.install_app(self.mpk_valid_path)
        self.assertEqual(app_id, self.valid_app_id_v1)

        # Check registry
        self.assertIn(app_id, self.runtime.registry)
        app_info = self.runtime.registry[app_id]
        expected_mpk_path = os.path.join(self.runtime.install_dir, f"{app_id}.mpk")
        self.assertEqual(app_info['path'], expected_mpk_path)
        self.assertEqual(app_info['app_name'], "RuntimeApp")
        self.assertTrue(os.path.exists(expected_mpk_path))

        # Check event trigger
        install_handler.assert_called_once_with(app_id, expected_mpk_path)

    def test_11_install_app_file_not_found(self):
        """Test installing a non-existent MPK file."""
        non_existent_path = os.path.join(self.mpk_source_dir, "not_a_real_file.mpk")
        with self.assertRaises(FileNotFoundError):
            self.runtime.install_app(non_existent_path)
        self.assertEqual(len(self.runtime.registry), 0) # Registry should be empty

    def test_12_install_app_already_exists(self):
        """Test installing an app that is already installed."""
        self.runtime.install_app(self.mpk_valid_path) # Install first time
        with self.assertRaises(FileExistsError):
            self.runtime.install_app(self.mpk_valid_path) # Try installing again
        self.assertEqual(len(self.runtime.registry), 1) # Should still only have one entry

    def test_13_install_app_invalid_metadata(self):
        """Test installing an MPK with invalid/incomplete metadata."""
        with self.assertRaisesRegex(ValueError, "MPK 文件元数据不完整"):
            self.runtime.install_app(self.mpk_invalid_meta_path)
        self.assertEqual(len(self.runtime.registry), 0)

    def test_14_install_app_invalid_file(self):
        """Test installing a corrupted MPK file (fails MPKFile.load)."""
        with self.assertRaisesRegex(ValueError, "校验和不匹配"): # Or other load error
            self.runtime.install_app(self.mpk_invalid_file_path)
        self.assertEqual(len(self.runtime.registry), 0)
        # Check if corrupted file was copied and then cleaned up
        corrupted_target_path = os.path.join(self.runtime.install_dir, f"com.corrupt_1.0.mpk")
        self.assertFalse(os.path.exists(corrupted_target_path))

    # --- Uninstallation Tests ---
    def test_20_uninstall_app_success(self):
        """Test successful app uninstallation."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        installed_path = self.runtime.registry[app_id]['path']
        full_installed_path = os.path.join(self.runtime.install_dir, os.path.basename(installed_path))
        self.assertTrue(os.path.exists(full_installed_path))

        uninstall_handler = MagicMock()
        self.runtime.register_event_handler("uninstall", uninstall_handler)

        result = self.runtime.uninstall_app(app_id)
        self.assertTrue(result)
        self.assertNotIn(app_id, self.runtime.registry)
        self.assertFalse(os.path.exists(full_installed_path))
        uninstall_handler.assert_called_once_with(app_id)

    def test_21_uninstall_app_not_installed(self):
        """Test uninstalling an app that is not installed."""
        result = self.runtime.uninstall_app("com.notinstalled.app_1.0")
        self.assertFalse(result)

    def test_22_uninstall_app_file_missing(self):
        """Test uninstalling when registry entry exists but file is missing."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        installed_path = self.runtime.registry[app_id]['path']
        full_installed_path = os.path.join(self.runtime.install_dir, os.path.basename(installed_path))
        os.remove(full_installed_path) # Manually remove the file

        # Uninstall should still succeed by removing the registry entry
        result = self.runtime.uninstall_app(app_id)
        self.assertTrue(result)
        self.assertNotIn(app_id, self.runtime.registry)

    # --- Listing Tests ---
    def test_30_list_installed_apps_empty(self):
        """Test listing installed apps when none are installed."""
        apps = self.runtime.list_installed_apps()
        ids = self.runtime.get_installed_app_ids()
        self.assertEqual(apps, [])
        self.assertEqual(ids, [])

    def test_31_list_installed_apps_multiple(self):
        """Test listing installed apps with multiple apps."""
        app_id1 = self.runtime.install_app(self.mpk_valid_path)
        app_id2 = self.runtime.install_app(self.mpk_valid_v2_path)

        apps = self.runtime.list_installed_apps()
        ids = self.runtime.get_installed_app_ids()

        self.assertEqual(len(apps), 2)
        self.assertEqual(len(ids), 2)
        self.assertIn(app_id1, ids)
        self.assertIn(app_id2, ids)

        app1_info = next((a for a in apps if a['package_name'] == 'com.runtime.test' and a['version'] == '1.0'), None)
        app2_info = next((a for a in apps if a['package_name'] == 'com.runtime.test' and a['version'] == '2.0'), None)
        self.assertIsNotNone(app1_info)
        self.assertIsNotNone(app2_info)
        self.assertEqual(app1_info['app_name'], "RuntimeApp")
        self.assertEqual(app2_info['app_name'], "RuntimeApp")

    # --- Loading Installed App Tests ---
    def test_40_load_app_success(self):
        """Test successfully loading an installed app."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        start_handler = MagicMock()
        self.runtime.register_event_handler("start", start_handler)

        result = self.runtime.load_app(app_id)
        self.assertTrue(result)
        self.assertIn(app_id, self.runtime.loaders)
        loader = self.runtime.loaders[app_id]
        self.assertIsNotNone(loader.module) # Check code was loaded
        self.assertTrue(os.path.isdir(loader.temp_dir)) # Check resources were extracted (to temp)
        start_handler.assert_called_once_with(app_id)

    def test_41_load_app_already_loaded(self):
        """Test loading an app that is already loaded."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        self.runtime.load_app(app_id) # Load first time
        self.assertIn(app_id, self.runtime.loaders)
        loader_instance = self.runtime.loaders[app_id]

        # Try loading again
        result = self.runtime.load_app(app_id)
        self.assertTrue(result) # Should indicate success (or no-op)
        self.assertIs(self.runtime.loaders[app_id], loader_instance) # Should be the same loader instance

    def test_42_load_app_not_installed(self):
        """Test loading an app ID that is not installed."""
        result = self.runtime.load_app("com.notinstalled.app_1.0")
        self.assertFalse(result)
        self.assertEqual(len(self.runtime.loaders), 0)

    def test_43_load_app_installed_file_missing(self):
        """Test loading an installed app whose MPK file is missing."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        installed_path = self.runtime.registry[app_id]['path']
        full_installed_path = os.path.join(self.runtime.install_dir, os.path.basename(installed_path))
        os.remove(full_installed_path) # Remove the MPK file

        result = self.runtime.load_app(app_id)
        self.assertFalse(result)
        self.assertNotIn(app_id, self.runtime.loaders)

    def test_44_load_app_fail_dependency(self):
        """Test loading an installed app that fails dependency check."""
        # Install the app with missing deps
        app_id = self.runtime.install_app(self.mpk_nodeps_path)
        self.assertEqual(app_id, self.nodeps_app_id)

        with self.assertRaisesRegex(ValueError, "应用依赖检查失败"):
            # load_app raises the error directly if checks fail
            self.runtime.load_app(app_id)

        # Ensure it wasn't partially loaded
        self.assertNotIn(app_id, self.runtime.loaders)


    def test_45_load_app_fail_permission(self):
        """Test loading an installed app that fails permission check."""
        # Install the app with bad perms
        app_id = self.runtime.install_app(self.mpk_badperms_path)
        self.assertEqual(app_id, self.badperms_app_id)

        with self.assertRaisesRegex(ValueError, "应用权限检查失败"):
            self.runtime.load_app(app_id)

        self.assertNotIn(app_id, self.runtime.loaders)

    # --- Getting Installed Info Test ---
    def test_50_get_installed_app_info_success(self):
        """Test getting info for an installed app."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        info = self.runtime.get_installed_app_info(app_id)

        self.assertIsNotNone(info)
        self.assertEqual(info['app_id'], app_id)
        self.assertTrue(os.path.exists(info['mpk_path']))
        self.assertEqual(info['metadata']['app_name'], "RuntimeApp")
        self.assertEqual(info['metadata']['version'], "1.0")
        self.assertTrue('install_date' in info)
        self.assertEqual(info['code_info']['入口点'], "main")
        self.assertEqual(len(info['resources_info']), 1)

    def test_51_get_installed_app_info_not_installed(self):
        """Test getting info for a non-installed app ID."""
        info = self.runtime.get_installed_app_info("com.notinstalled.app_1.0")
        self.assertIsNone(info)

    def test_52_get_installed_app_info_file_missing(self):
         """Test getting info when registry exists but file is missing."""
         app_id = self.runtime.install_app(self.mpk_valid_path)
         installed_path = self.runtime.registry[app_id]['path']
         full_installed_path = os.path.join(self.runtime.install_dir, os.path.basename(installed_path))
         os.remove(full_installed_path) # Remove the file

         # Getting info should fail gracefully
         info = self.runtime.get_installed_app_info(app_id)
         self.assertIsNone(info) # MPKViewer.from_file will raise error

    # --- Test unload_app (runtime unload) ---
    def test_60_unload_app_runtime(self):
        """Test unloading a loaded app from runtime."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        self.runtime.load_app(app_id)
        self.assertIn(app_id, self.runtime.loaders)
        loader = self.runtime.loaders[app_id]
        temp_dir = loader.temp_dir # Get temp dir path before cleanup
        self.assertTrue(os.path.isdir(temp_dir))

        stop_handler = MagicMock()
        self.runtime.register_event_handler("stop", stop_handler)

        self.runtime.unload_app(app_id)

        self.assertNotIn(app_id, self.runtime.loaders)
        self.assertFalse(os.path.exists(temp_dir)) # Check temp dir is cleaned
        # Check the installed file still exists
        installed_path = self.runtime.registry[app_id]['path']
        full_installed_path = os.path.join(self.runtime.install_dir, os.path.basename(installed_path))
        self.assertTrue(os.path.exists(full_installed_path))
        stop_handler.assert_called_once_with(app_id)

    def test_61_unload_app_runtime_not_loaded(self):
         """Test unloading an app from runtime that isn't loaded."""
         app_id = self.runtime.install_app(self.mpk_valid_path) # Only installed
         self.assertNotIn(app_id, self.runtime.loaders)
         # Should do nothing gracefully
         self.runtime.unload_app(app_id)
         self.assertNotIn(app_id, self.runtime.loaders)


    # Note: Tests for run_app, get_app_info, get_app_resource, extract_app_resource
    #       remain largely the same as they operate on loaded apps (self.loaders).
    #       We might add a test ensuring they fail for an app_id that is only installed but not loaded.

    def test_70_run_app_installed_not_loaded(self):
        """Test running an app that is installed but not loaded."""
        app_id = self.runtime.install_app(self.mpk_valid_path)
        self.assertNotIn(app_id, self.runtime.loaders)
        result = self.runtime.run_app(app_id)
        self.assertFalse(result) # Should fail because it's not loaded


if __name__ == '__main__':
    unittest.main() 