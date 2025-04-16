rule PathTraversal {
    meta:
        description = "检测路径遍历漏洞"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // Common path traversal sequences
        $pt1 = "../"
        $pt2 = "..\\" // Double backslash for escaping in some contexts

        // URL encoded traversal sequences
        $pt_encoded1 = "%2e%2e%2f" // ../
        $pt_encoded2 = "%2e%2e/" // ../
        $pt_encoded3 = "..%2f" // ../
        $pt_encoded4 = "%2e%2e%5c" // ..\
        $pt_encoded5 = "%2e%2e\\" // ..\
        $pt_encoded6 = "..%5c" // ..\

        // Mixed encoding
        $pt_mixed1 = "%2e./"
        $pt_mixed2 = ".%2e/"
        $pt_mixed3 = "%2e.\\"
        $pt_mixed4 = ".%2e\\"

        // Less common but possible encodings
        $pt_double_encoded1 = "%252e%252e%252f" // ../ (double URL encoded)
        $pt_double_encoded2 = "%252e%252e%255c" // ..\ (double URL encoded)
        $pt_unicode1 = "%c0%af" // Unicode '/' character
        $pt_unicode2 = "%c1%9c" // Unicode '\' character

        // Absolute paths (could indicate traversal attempts outside intended dir)
        $abs_path1 = "/etc/passwd"
        $abs_path2 = "/etc/shadow"
        $abs_path3 = "/proc/self/environ"
        $abs_path4 = "C:\\Windows\\System32\\drivers\\etc\\hosts" // Windows hosts file
        $abs_path5 = "C:/Windows/System32/drivers/etc/hosts"
        $abs_path6 = "\\\\localhost\\" // UNC path
        $abs_path7 = "file:///" // File URI scheme

        // Patterns combining user input with file operations (Example for PHP)
        // These are more contextual and might require more advanced analysis
        // $combo1 = /file_get_contents\s*\(\s*\$_(?:GET|POST|REQUEST|COOKIE)\[.*\]/ nocase
        // $combo2 = /include\s*\(\s*\$_(?:GET|POST|REQUEST|COOKIE)\[.*\]\s*.*(?:\.\.|%2e%2e)/ nocase

    condition:
        // Trigger if any common traversal sequence (raw or encoded) is found
        // Or if sensitive absolute paths are referenced
        any of ($pt*) or
        any of ($pt_encoded*) or
        any of ($pt_mixed*) or
        any of ($pt_double_encoded*) or
        any of ($pt_unicode*) or
        any of ($abs_path*)

} 