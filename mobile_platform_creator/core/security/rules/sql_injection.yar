rule SQLInjection {
    meta:
        description = "检测SQL注入攻击"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        $s1 = "UNION SELECT" nocase
        $s2 = "UNION ALL SELECT" nocase
        $s3 = "OR '1'='1" nocase
        $s4 = "OR 1=1" nocase
        $s5 = "OR 'x'='x" nocase
        $s6 = "OR 'a'='a" nocase
        $s7 = "OR '1'='1'--" nocase
        $s8 = "OR '1'='1'#" nocase
        $s9 = "OR '1'='1'/*" nocase
        $s10 = "OR '1'='1'%23" nocase
        $s11 = "OR '1'='1'%2d" nocase
        $s12 = "OR '1'='1'%2b" nocase
        $s13 = "OR '1'='1'%20" nocase
        $s14 = "OR '1'='1'%0a" nocase
        $s15 = "OR '1'='1'%0d" nocase
        $s16 = "OR '1'='1'%09" nocase
        $s17 = "OR '1'='1'%0b" nocase
        $s18 = "OR '1'='1'%0c" nocase
        $s19 = "OR '1'='1'%0e" nocase
        $s20 = "OR '1'='1'%0f" nocase
        $s21 = "OR '1'='1'%10" nocase
        $s22 = "OR '1'='1'%11" nocase
        $s23 = "OR '1'='1'%12" nocase
        $s24 = "OR '1'='1'%13" nocase
        $s25 = "OR '1'='1'%14" nocase
        $s26 = "OR '1'='1'%15" nocase
        $s27 = "OR '1'='1'%16" nocase
        $s28 = "OR '1'='1'%17" nocase
        $s29 = "OR '1'='1'%18" nocase
        $s30 = "OR '1'='1'%19" nocase

    condition:
        any of ($s*)
} 