rule SuspiciousCode {
    meta:
        description = "检测可疑代码模式"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 3

    strings:
        $s1 = "eval(" nocase
        $s2 = "exec(" nocase
        $s3 = "system(" nocase
        $s4 = "os.system" nocase
        $s5 = "subprocess.call" nocase
        $s6 = "subprocess.Popen" nocase
        $s7 = "os.popen" nocase
        $s8 = "os.spawn" nocase
        $s9 = "os.exec" nocase
        $s10 = "shell=True" nocase
        $s11 = "base64.decode" nocase
        $s12 = "urllib.request.urlopen" nocase
        $s13 = "requests.get" nocase
        $s14 = "requests.post" nocase
        $s15 = "socket.connect" nocase
        $s16 = "os.remove" nocase
        $s17 = "os.unlink" nocase
        $s18 = "shutil.rmtree" nocase
        $s19 = "os.makedirs" nocase
        $s20 = "os.chmod" nocase

    condition:
        any of ($s*)
} 