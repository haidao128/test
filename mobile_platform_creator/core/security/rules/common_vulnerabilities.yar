rule CommonVulnerabilities {
    meta:
        description = "检测常见安全漏洞，包括SQL注入、XSS、命令注入等"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // SQL注入相关
        $sql_injection1 = "SELECT * FROM" nocase
        $sql_injection2 = "INSERT INTO" nocase
        $sql_injection3 = "UPDATE" nocase
        $sql_injection4 = "DELETE FROM" nocase
        $sql_injection5 = "DROP TABLE" nocase
        $sql_injection6 = "UNION SELECT" nocase
        $sql_injection7 = "UNION ALL SELECT" nocase
        $sql_injection8 = "OR 1=1" nocase
        $sql_injection9 = "OR '1'='1'" nocase
        $sql_injection10 = "OR \"1\"=\"1\"" nocase
        $sql_injection11 = "OR 1=1--" nocase
        $sql_injection12 = "OR '1'='1'--" nocase
        $sql_injection13 = "OR \"1\"=\"1\"--" nocase
        $sql_injection14 = "OR 1=1#" nocase
        $sql_injection15 = "OR '1'='1'#" nocase
        $sql_injection16 = "OR \"1\"=\"1\"#" nocase
        $sql_injection17 = "OR 1=1/*" nocase
        $sql_injection18 = "OR '1'='1'/*" nocase
        $sql_injection19 = "OR \"1\"=\"1\"/*" nocase
        $sql_injection20 = "OR 1=1;" nocase

        // XSS相关
        $xss1 = "<script>" nocase
        $xss2 = "</script>" nocase
        $xss3 = "javascript:" nocase
        $xss4 = "onerror=" nocase
        $xss5 = "onload=" nocase
        $xss6 = "onclick=" nocase
        $xss7 = "onmouseover=" nocase
        $xss8 = "onmouseout=" nocase
        $xss9 = "onmouseenter=" nocase
        $xss10 = "onmouseleave=" nocase
        $xss11 = "onmousedown=" nocase
        $xss12 = "onmouseup=" nocase
        $xss13 = "onmousemove=" nocase
        $xss14 = "onkeydown=" nocase
        $xss15 = "onkeyup=" nocase
        $xss16 = "onkeypress=" nocase
        $xss17 = "onfocus=" nocase
        $xss18 = "onblur=" nocase
        $xss19 = "onchange=" nocase
        $xss20 = "onsubmit=" nocase

        // 命令注入相关
        $cmd_injection1 = "system(" nocase
        $cmd_injection2 = "exec(" nocase
        $cmd_injection3 = "shell_exec(" nocase
        $cmd_injection4 = "passthru(" nocase
        $cmd_injection5 = "popen(" nocase
        $cmd_injection6 = "proc_open(" nocase
        $cmd_injection7 = "pcntl_exec(" nocase
        $cmd_injection8 = "eval(" nocase
        $cmd_injection9 = "assert(" nocase
        $cmd_injection10 = "preg_replace(" nocase
        $cmd_injection11 = "create_function(" nocase
        $cmd_injection12 = "include(" nocase
        $cmd_injection13 = "include_once(" nocase
        $cmd_injection14 = "require(" nocase
        $cmd_injection15 = "require_once(" nocase
        $cmd_injection16 = "`" nocase
        $cmd_injection17 = "subprocess.call(" nocase
        $cmd_injection18 = "subprocess.Popen(" nocase
        $cmd_injection19 = "os.system(" nocase
        $cmd_injection20 = "os.popen(" nocase

        // 文件包含漏洞相关
        $file_include1 = "include(" nocase
        $file_include2 = "include_once(" nocase
        $file_include3 = "require(" nocase
        $file_include4 = "require_once(" nocase
        $file_include5 = "fopen(" nocase
        $file_include6 = "file_get_contents(" nocase
        $file_include7 = "file_put_contents(" nocase
        $file_include8 = "readfile(" nocase
        $file_include9 = "highlight_file(" nocase
        $file_include10 = "show_source(" nocase
        $file_include11 = "get_file_contents(" nocase
        $file_include12 = "file(" nocase
        $file_include13 = "file_exists(" nocase
        $file_include14 = "is_file(" nocase
        $file_include15 = "is_readable(" nocase
        $file_include16 = "is_writable(" nocase
        $file_include17 = "is_executable(" nocase
        $file_include18 = "fileperms(" nocase
        $file_include19 = "fileowner(" nocase
        $file_include20 = "filegroup(" nocase

        // 路径遍历漏洞相关
        $path_traversal1 = "../" nocase
        $path_traversal2 = "..\\" nocase
        $path_traversal3 = "..%2f" nocase
        $path_traversal4 = "..%5c" nocase
        $path_traversal5 = "%2e%2e%2f" nocase
        $path_traversal6 = "%2e%2e%5c" nocase
        $path_traversal7 = "..%252f" nocase
        $path_traversal8 = "..%255c" nocase
        $path_traversal9 = "..%c0%af" nocase
        $path_traversal10 = "..%c1%9c" nocase
        $path_traversal11 = "..%c0%9v" nocase
        $path_traversal12 = "..%c0%qf" nocase
        $path_traversal13 = "..%c1%8s" nocase
        $path_traversal14 = "..%c1%1c" nocase
        $path_traversal15 = "..%c1%pc" nocase
        $path_traversal16 = "..%c0%80%af" nocase
        $path_traversal17 = "..%c0%80%5c" nocase
        $path_traversal18 = "..%c1%9c" nocase
        $path_traversal19 = "..%c1%9c%c1%9c" nocase
        $path_traversal20 = "..%c0%af%c0%af" nocase

        // 不安全的反序列化相关
        $unsafe_deserialization1 = "unserialize(" nocase
        $unsafe_deserialization2 = "yaml.load(" nocase
        $unsafe_deserialization3 = "pickle.loads(" nocase
        $unsafe_deserialization4 = "pickle.load(" nocase
        $unsafe_deserialization5 = "ObjectInputStream" nocase
        $unsafe_deserialization6 = "readObject(" nocase
        $unsafe_deserialization7 = "fromJson(" nocase
        $unsafe_deserialization8 = "JSON.parse(" nocase
        $unsafe_deserialization9 = "JSON.parseObject(" nocase
        $unsafe_deserialization10 = "JSON.parseArray(" nocase
        $unsafe_deserialization11 = "JSON.parseObject(" nocase
        $unsafe_deserialization12 = "JSON.parseArray(" nocase
        $unsafe_deserialization13 = "JSON.parseObject(" nocase
        $unsafe_deserialization14 = "JSON.parseArray(" nocase
        $unsafe_deserialization15 = "JSON.parseObject(" nocase
        $unsafe_deserialization16 = "JSON.parseArray(" nocase
        $unsafe_deserialization17 = "JSON.parseObject(" nocase
        $unsafe_deserialization18 = "JSON.parseArray(" nocase
        $unsafe_deserialization19 = "JSON.parseObject(" nocase
        $unsafe_deserialization20 = "JSON.parseArray(" nocase

        // 不安全的加密实现相关
        $unsafe_crypto1 = "md5(" nocase
        $unsafe_crypto2 = "sha1(" nocase
        $unsafe_crypto3 = "sha256(" nocase
        $unsafe_crypto4 = "sha512(" nocase
        $unsafe_crypto5 = "des" nocase
        $unsafe_crypto6 = "3des" nocase
        $unsafe_crypto7 = "rc4" nocase
        $unsafe_crypto8 = "blowfish" nocase
        $unsafe_crypto9 = "aes-128-ecb" nocase
        $unsafe_crypto10 = "aes-192-ecb" nocase
        $unsafe_crypto11 = "aes-256-ecb" nocase
        $unsafe_crypto12 = "aes-128-cbc" nocase
        $unsafe_crypto13 = "aes-192-cbc" nocase
        $unsafe_crypto14 = "aes-256-cbc" nocase
        $unsafe_crypto15 = "aes-128-cfb" nocase
        $unsafe_crypto16 = "aes-192-cfb" nocase
        $unsafe_crypto17 = "aes-256-cfb" nocase
        $unsafe_crypto18 = "aes-128-ofb" nocase
        $unsafe_crypto19 = "aes-192-ofb" nocase
        $unsafe_crypto20 = "aes-256-ofb" nocase

        // 不安全的随机数生成相关
        $unsafe_random1 = "rand(" nocase
        $unsafe_random2 = "random(" nocase
        $unsafe_random3 = "mt_rand(" nocase
        $unsafe_random4 = "srand(" nocase
        $unsafe_random5 = "mt_srand(" nocase
        $unsafe_random6 = "Math.random()" nocase
        $unsafe_random7 = "Math.floor(Math.random()" nocase
        $unsafe_random8 = "Math.ceil(Math.random()" nocase
        $unsafe_random9 = "Math.round(Math.random()" nocase
        $unsafe_random10 = "Math.random() * " nocase
        $unsafe_random11 = "Math.random() + " nocase
        $unsafe_random12 = "Math.random() - " nocase
        $unsafe_random13 = "Math.random() / " nocase
        $unsafe_random14 = "Math.random() % " nocase
        $unsafe_random15 = "Math.random() ** " nocase
        $unsafe_random16 = "Math.random() ^ " nocase
        $unsafe_random17 = "Math.random() & " nocase
        $unsafe_random18 = "Math.random() | " nocase
        $unsafe_random19 = "Math.random() << " nocase
        $unsafe_random20 = "Math.random() >> " nocase

        // 硬编码凭证相关
        $hardcoded_credentials1 = "password = " nocase
        $hardcoded_credentials2 = "password=" nocase
        $hardcoded_credentials3 = "passwd = " nocase
        $hardcoded_credentials4 = "passwd=" nocase
        $hardcoded_credentials5 = "pwd = " nocase
        $hardcoded_credentials6 = "pwd=" nocase
        $hardcoded_credentials7 = "username = " nocase
        $hardcoded_credentials8 = "username=" nocase
        $hardcoded_credentials9 = "user = " nocase
        $hardcoded_credentials10 = "user=" nocase
        $hardcoded_credentials11 = "api_key = " nocase
        $hardcoded_credentials12 = "api_key=" nocase
        $hardcoded_credentials13 = "secret = " nocase
        $hardcoded_credentials14 = "secret=" nocase
        $hardcoded_credentials15 = "token = " nocase
        $hardcoded_credentials16 = "token=" nocase
        $hardcoded_credentials17 = "key = " nocase
        $hardcoded_credentials18 = "key=" nocase
        $hardcoded_credentials19 = "auth = " nocase
        $hardcoded_credentials20 = "auth=" nocase

        // 不安全的HTTP头相关
        $unsafe_http_headers1 = "Access-Control-Allow-Origin: *" nocase
        $unsafe_http_headers2 = "Access-Control-Allow-Origin: null" nocase
        $unsafe_http_headers3 = "Access-Control-Allow-Credentials: true" nocase
        $unsafe_http_headers4 = "Access-Control-Allow-Methods: *" nocase
        $unsafe_http_headers5 = "Access-Control-Allow-Headers: *" nocase
        $unsafe_http_headers6 = "Access-Control-Expose-Headers: *" nocase
        $unsafe_http_headers7 = "Access-Control-Max-Age: 0" nocase
        $unsafe_http_headers8 = "X-Frame-Options: ALLOWALL" nocase
        $unsafe_http_headers9 = "X-Frame-Options: ALLOW-FROM" nocase
        $unsafe_http_headers10 = "X-Frame-Options: SAMEORIGIN" nocase
        $unsafe_http_headers11 = "X-Frame-Options: DENY" nocase
        $unsafe_http_headers12 = "X-XSS-Protection: 0" nocase
        $unsafe_http_headers13 = "X-Content-Type-Options: nosniff" nocase
        $unsafe_http_headers14 = "Strict-Transport-Security: max-age=0" nocase
        $unsafe_http_headers15 = "Content-Security-Policy: none" nocase
        $unsafe_http_headers16 = "Content-Security-Policy: unsafe-inline" nocase
        $unsafe_http_headers17 = "Content-Security-Policy: unsafe-eval" nocase
        $unsafe_http_headers18 = "Content-Security-Policy: data:" nocase
        $unsafe_http_headers19 = "Content-Security-Policy: blob:" nocase
        $unsafe_http_headers20 = "Content-Security-Policy: *" nocase

        // 不安全的Cookie设置相关
        $unsafe_cookie1 = "Set-Cookie: " nocase
        $unsafe_cookie2 = "document.cookie" nocase
        $unsafe_cookie3 = "cookie = " nocase
        $unsafe_cookie4 = "cookie=" nocase
        $unsafe_cookie5 = "session = " nocase
        $unsafe_cookie6 = "session=" nocase
        $unsafe_cookie7 = "sessionid = " nocase
        $unsafe_cookie8 = "sessionid=" nocase
        $unsafe_cookie9 = "jsessionid = " nocase
        $unsafe_cookie10 = "jsessionid=" nocase
        $unsafe_cookie11 = "phpsessid = " nocase
        $unsafe_cookie12 = "phpsessid=" nocase
        $unsafe_cookie13 = "secure = false" nocase
        $unsafe_cookie14 = "secure=false" nocase
        $unsafe_cookie15 = "httponly = false" nocase
        $unsafe_cookie16 = "httponly=false" nocase
        $unsafe_cookie17 = "samesite = none" nocase
        $unsafe_cookie18 = "samesite=none" nocase
        $unsafe_cookie19 = "domain = " nocase
        $unsafe_cookie20 = "domain=" nocase

        // 不安全的文件上传相关
        $unsafe_file_upload1 = "move_uploaded_file(" nocase
        $unsafe_file_upload2 = "copy(" nocase
        $unsafe_file_upload3 = "rename(" nocase
        $unsafe_file_upload4 = "unlink(" nocase
        $unsafe_file_upload5 = "delete(" nocase
        $unsafe_file_upload6 = "rm(" nocase
        $unsafe_file_upload7 = "rmdir(" nocase
        $unsafe_file_upload8 = "mkdir(" nocase
        $unsafe_file_upload9 = "chmod(" nocase
        $unsafe_file_upload10 = "chown(" nocase
        $unsafe_file_upload11 = "touch(" nocase
        $unsafe_file_upload12 = "symlink(" nocase
        $unsafe_file_upload13 = "link(" nocase
        $unsafe_file_upload14 = "readlink(" nocase
        $unsafe_file_upload15 = "realpath(" nocase
        $unsafe_file_upload16 = "basename(" nocase
        $unsafe_file_upload17 = "dirname(" nocase
        $unsafe_file_upload18 = "pathinfo(" nocase
        $unsafe_file_upload19 = "fileperms(" nocase
        $unsafe_file_upload20 = "fileowner(" nocase

        // 不安全的会话管理相关
        $unsafe_session1 = "session_start(" nocase
        $unsafe_session2 = "session_id(" nocase
        $unsafe_session3 = "session_name(" nocase
        $unsafe_session4 = "session_regenerate_id(" nocase
        $unsafe_session5 = "session_destroy(" nocase
        $unsafe_session6 = "session_unset(" nocase
        $unsafe_session7 = "session_write_close(" nocase
        $unsafe_session8 = "session_cache_limiter(" nocase
        $unsafe_session9 = "session_cache_expire(" nocase
        $unsafe_session10 = "session_set_cookie_params(" nocase
        $unsafe_session11 = "session_get_cookie_params(" nocase
        $unsafe_session12 = "session_save_path(" nocase
        $unsafe_session13 = "session_save_handler(" nocase
        $unsafe_session14 = "session_module_name(" nocase
        $unsafe_session15 = "session_name(" nocase
        $unsafe_session16 = "session_id(" nocase
        $unsafe_session17 = "session_regenerate_id(" nocase
        $unsafe_session18 = "session_destroy(" nocase
        $unsafe_session19 = "session_unset(" nocase
        $unsafe_session20 = "session_write_close(" nocase

        // 不安全的错误处理相关
        $unsafe_error_handling1 = "error_reporting(0)" nocase
        $unsafe_error_handling2 = "display_errors = On" nocase
        $unsafe_error_handling3 = "display_errors=On" nocase
        $unsafe_error_handling4 = "log_errors = Off" nocase
        $unsafe_error_handling5 = "log_errors=Off" nocase
        $unsafe_error_handling6 = "error_log = " nocase
        $unsafe_error_handling7 = "error_log=" nocase
        $unsafe_error_handling8 = "error_handler = " nocase
        $unsafe_error_handling9 = "error_handler=" nocase
        $unsafe_error_handling10 = "set_error_handler(" nocase
        $unsafe_error_handling11 = "restore_error_handler(" nocase
        $unsafe_error_handling12 = "set_exception_handler(" nocase
        $unsafe_error_handling13 = "restore_exception_handler(" nocase
        $unsafe_error_handling14 = "trigger_error(" nocase
        $unsafe_error_handling15 = "user_error(" nocase
        $unsafe_error_handling16 = "error_log(" nocase
        $unsafe_error_handling17 = "debug_backtrace(" nocase
        $unsafe_error_handling18 = "debug_print_backtrace(" nocase
        $unsafe_error_handling19 = "debug_backtrace(" nocase
        $unsafe_error_handling20 = "debug_print_backtrace(" nocase

        // 不安全的日志记录相关
        $unsafe_logging1 = "error_log(" nocase
        $unsafe_logging2 = "syslog(" nocase
        $unsafe_logging3 = "openlog(" nocase
        $unsafe_logging4 = "closelog(" nocase
        $unsafe_logging5 = "setlogmask(" nocase
        $unsafe_logging6 = "syslog(" nocase
        $unsafe_logging7 = "openlog(" nocase
        $unsafe_logging8 = "closelog(" nocase
        $unsafe_logging9 = "setlogmask(" nocase
        $unsafe_logging10 = "syslog(" nocase
        $unsafe_logging11 = "openlog(" nocase
        $unsafe_logging12 = "closelog(" nocase
        $unsafe_logging13 = "setlogmask(" nocase
        $unsafe_logging14 = "syslog(" nocase
        $unsafe_logging15 = "openlog(" nocase
        $unsafe_logging16 = "closelog(" nocase
        $unsafe_logging17 = "setlogmask(" nocase
        $unsafe_logging18 = "syslog(" nocase
        $unsafe_logging19 = "openlog(" nocase
        $unsafe_logging20 = "closelog(" nocase

        // 不安全的配置相关
        $unsafe_config1 = "allow_url_fopen = On" nocase
        $unsafe_config2 = "allow_url_fopen=On" nocase
        $unsafe_config3 = "allow_url_include = On" nocase
        $unsafe_config4 = "allow_url_include=On" nocase
        $unsafe_config5 = "register_globals = On" nocase
        $unsafe_config6 = "register_globals=On" nocase
        $unsafe_config7 = "magic_quotes_gpc = On" nocase
        $unsafe_config8 = "magic_quotes_gpc=On" nocase
        $unsafe_config9 = "magic_quotes_runtime = On" nocase
        $unsafe_config10 = "magic_quotes_runtime=On" nocase
        $unsafe_config11 = "magic_quotes_sybase = On" nocase
        $unsafe_config12 = "magic_quotes_sybase=On" nocase
        $unsafe_config13 = "safe_mode = Off" nocase
        $unsafe_config14 = "safe_mode=Off" nocase
        $unsafe_config15 = "open_basedir = " nocase
        $unsafe_config16 = "open_basedir=" nocase
        $unsafe_config17 = "disable_functions = " nocase
        $unsafe_config18 = "disable_functions=" nocase
        $unsafe_config19 = "disable_classes = " nocase
        $unsafe_config20 = "disable_classes=" nocase

    condition:
        // 检测常见安全漏洞
        (
            // 1. SQL注入
            any of ($sql_injection*) or
            
            // 2. XSS
            any of ($xss*) or
            
            // 3. 命令注入
            any of ($cmd_injection*) or
            
            // 4. 文件包含漏洞
            any of ($file_include*) or
            
            // 5. 路径遍历漏洞
            any of ($path_traversal*) or
            
            // 6. 不安全的反序列化
            any of ($unsafe_deserialization*) or
            
            // 7. 不安全的加密实现
            any of ($unsafe_crypto*) or
            
            // 8. 不安全的随机数生成
            any of ($unsafe_random*) or
            
            // 9. 硬编码凭证
            any of ($hardcoded_credentials*) or
            
            // 10. 不安全的HTTP头
            any of ($unsafe_http_headers*) or
            
            // 11. 不安全的Cookie设置
            any of ($unsafe_cookie*) or
            
            // 12. 不安全的文件上传
            any of ($unsafe_file_upload*) or
            
            // 13. 不安全的会话管理
            any of ($unsafe_session*) or
            
            // 14. 不安全的错误处理
            any of ($unsafe_error_handling*) or
            
            // 15. 不安全的日志记录
            any of ($unsafe_logging*) or
            
            // 16. 不安全的配置
            any of ($unsafe_config*)
        )
} 