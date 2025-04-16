rule MaliciousCode {
    meta:
        description = "检测恶意代码"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        $s1 = "eval(" nocase
        $s2 = "exec(" nocase
        $s3 = "system(" nocase
        $s4 = "shell_exec(" nocase
        $s5 = "passthru(" nocase
        $s6 = "preg_replace" nocase
        $s7 = "assert(" nocase
        $s8 = "base64_decode" nocase
        $s9 = "gzinflate" nocase
        $s10 = "str_rot13" nocase
        $s11 = "create_function" nocase
        $s12 = "proc_open" nocase
        $s13 = "proc_get_status" nocase
        $s14 = "proc_nice" nocase
        $s15 = "proc_terminate" nocase
        $s16 = "proc_close" nocase
        $s17 = "pfsockopen" nocase
        $s18 = "fsockopen" nocase
        $s19 = "apache_child_terminate" nocase
        $s20 = "posix_kill" nocase
        $s21 = "posix_mkfifo" nocase
        $s22 = "posix_setpgid" nocase
        $s23 = "posix_setsid" nocase
        $s24 = "posix_setuid" nocase
        $s25 = "ftp_connect" nocase
        $s26 = "ftp_login" nocase
        $s27 = "ftp_get" nocase
        $s28 = "ftp_put" nocase
        $s29 = "ftp_nb_get" nocase
        $s30 = "ftp_nb_put" nocase

    condition:
        any of ($s*)
} 