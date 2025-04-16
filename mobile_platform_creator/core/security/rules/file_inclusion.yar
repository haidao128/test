rule FileInclusion {
    meta:
        description = "检测文件包含漏洞"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        $s1 = "include(" nocase
        $s2 = "include_once(" nocase
        $s3 = "require(" nocase
        $s4 = "require_once(" nocase
        $s5 = "include $_GET" nocase
        $s6 = "include $_POST" nocase
        $s7 = "include $_REQUEST" nocase
        $s8 = "include $_COOKIE" nocase
        $s9 = "include $_FILES" nocase
        $s10 = "include $_ENV" nocase
        $s11 = "include $_SERVER" nocase
        $s12 = "include $_SESSION" nocase
        $s13 = "include $GLOBALS" nocase
        $s14 = "include $HTTP_GET_VARS" nocase
        $s15 = "include $HTTP_POST_VARS" nocase
        $s16 = "include $HTTP_COOKIE_VARS" nocase
        $s17 = "include $HTTP_SERVER_VARS" nocase
        $s18 = "include $HTTP_ENV_VARS" nocase
        $s19 = "include $HTTP_SESSION_VARS" nocase
        $s20 = "include $HTTP_RAW_POST_DATA" nocase
        $s21 = "include $HTTP_POST_FILES" nocase
        $s22 = "include $HTTP_COOKIE" nocase
        $s23 = "include $HTTP_SERVER" nocase
        $s24 = "include $HTTP_ENV" nocase
        $s25 = "include $HTTP_SESSION" nocase
        $s26 = "include $HTTP_RAW_POST" nocase
        $s27 = "include $HTTP_POST" nocase
        $s28 = "include $HTTP_GET" nocase
        $s29 = "include $HTTP_REQUEST" nocase
        $s30 = "include $HTTP_FILES" nocase
        $s31 = "include $HTTP_GLOBALS" nocase
        $s32 = "include $HTTP_ALL_VARS" nocase
        $s33 = "include $HTTP_ALL_POST" nocase
        $s34 = "include $HTTP_ALL_GET" nocase
        $s35 = "include $HTTP_ALL_COOKIE" nocase
        $s36 = "include $HTTP_ALL_SERVER" nocase
        $s37 = "include $HTTP_ALL_ENV" nocase
        $s38 = "include $HTTP_ALL_SESSION" nocase
        $s39 = "include $HTTP_ALL_RAW_POST" nocase
        $s40 = "include $HTTP_ALL_FILES" nocase
        $s41 = "include $HTTP_ALL_GLOBALS" nocase
        $s42 = "include $HTTP_ALL_REQUEST" nocase
        $s43 = "include $HTTP_ALL_POST_FILES" nocase
        $s44 = "include $HTTP_ALL_COOKIE_VARS" nocase
        $s45 = "include $HTTP_ALL_SERVER_VARS" nocase
        $s46 = "include $HTTP_ALL_ENV_VARS" nocase
        $s47 = "include $HTTP_ALL_SESSION_VARS" nocase
        $s48 = "include $HTTP_ALL_RAW_POST_DATA" nocase
        $s49 = "include $HTTP_ALL_POST_FILES" nocase
        $s50 = "include $HTTP_ALL_GLOBALS" nocase

    condition:
        any of ($s*)
} 