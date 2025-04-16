rule SensitiveData {
    meta:
        description = "检测敏感数据泄露"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 4

    strings:
        $s1 = "password" nocase
        $s2 = "api_key" nocase
        $s3 = "secret" nocase
        $s4 = "token" nocase
        $s5 = "credential" nocase
        $s6 = "private_key" nocase
        $s7 = "public_key" nocase
        $s8 = "ssh_key" nocase
        $s9 = "aws_key" nocase
        $s10 = "access_key" nocase
        $s11 = "secret_key" nocase
        $s12 = "database_url" nocase
        $s13 = "connection_string" nocase
        $s14 = "auth_token" nocase
        $s15 = "jwt" nocase
        $s16 = "oauth" nocase
        $s17 = "api_secret" nocase
        $s18 = "client_secret" nocase
        $s19 = "encryption_key" nocase
        $s20 = "decryption_key" nocase

    condition:
        any of ($s*)
} 