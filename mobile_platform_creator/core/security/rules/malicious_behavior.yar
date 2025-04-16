rule MaliciousBehavior {
    meta:
        description = "检测恶意行为模式"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 4

    strings:
        $s1 = "keylogger" nocase
        $s2 = "screenshot" nocase
        $s3 = "webcam" nocase
        $s4 = "microphone" nocase
        $s5 = "clipboard" nocase
        $s6 = "password" nocase
        $s7 = "credential" nocase
        $s8 = "cookie" nocase
        $s9 = "session" nocase
        $s10 = "token" nocase
        $s11 = "encrypt" nocase
        $s12 = "decrypt" nocase
        $s13 = "ransomware" nocase
        $s14 = "backdoor" nocase
        $s15 = "rootkit" nocase
        $s16 = "botnet" nocase
        $s17 = "spyware" nocase
        $s18 = "adware" nocase
        $s19 = "malware" nocase
        $s20 = "virus" nocase

    condition:
        any of ($s*)
} 