rule CommandInjection {
    meta:
        description = "检测命令注入攻击"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // PHP command execution functions
        $php_exec1 = "system(" nocase
        $php_exec2 = "exec(" nocase
        $php_exec3 = "passthru(" nocase
        $php_exec4 = "shell_exec(" nocase
        $php_exec5 = "popen(" nocase
        $php_exec6 = "proc_open(" nocase
        $php_exec7 = "pcntl_exec(" nocase
        $php_backtick = "`"  // Backtick operator in PHP

        // Python command execution functions
        $py_exec1 = "os.system(" nocase
        $py_exec2 = "subprocess.call(" nocase
        $py_exec3 = "subprocess.check_call(" nocase
        $py_exec4 = "subprocess.check_output(" nocase
        $py_exec5 = "subprocess.run(" nocase
        $py_exec6 = "subprocess.Popen(" nocase
        $py_exec7 = "commands.getoutput(" nocase // Python 2
        $py_exec8 = "commands.getstatusoutput(" nocase // Python 2

        // Perl command execution functions
        $pl_exec1 = "system(" nocase
        $pl_exec2 = "exec(" nocase
        $pl_exec3 = "open(" nocase // Can be used for command execution with pipes
        $pl_backtick = "`" // Backtick operator in Perl

        // Ruby command execution functions
        $rb_exec1 = "system(" nocase
        $rb_exec2 = "exec(" nocase
        $rb_exec3 = "%x(" nocase // %x{} literal
        $rb_backtick = "`" // Backtick operator in Ruby

        // Node.js command execution functions
        $js_exec1 = "child_process.exec(" nocase
        $js_exec2 = "child_process.execSync(" nocase
        $js_exec3 = "child_process.execFile(" nocase
        $js_exec4 = "child_process.execFileSync(" nocase
        $js_exec5 = "child_process.spawn(" nocase
        $js_exec6 = "child_process.spawnSync(" nocase

        // Shell command separators and pipes
        $sep1 = ";"
        $sep2 = "|"
        $sep3 = "&"
        $sep4 = "&&"
        $sep5 = "||"
        $sep6 = "\n" // Newline character as separator
        $sep7 = "\r" // Carriage return as separator

        // Common potentially dangerous commands
        $cmd1 = "wget " nocase
        $cmd2 = "curl " nocase
        $cmd3 = "nc " nocase
        $cmd4 = "netcat " nocase
        $cmd5 = "rm " nocase
        $cmd6 = "chmod " nocase
        $cmd7 = "chown " nocase
        $cmd8 = "cat " nocase
        $cmd9 = "ls " nocase
        $cmd10 = "dir " nocase
        $cmd11 = "ping " nocase
        $cmd12 = "telnet " nocase
        $cmd13 = "ssh " nocase
        $cmd14 = "perl " nocase
        $cmd15 = "python " nocase
        $cmd16 = "ruby " nocase
        $cmd17 = "php " nocase
        $cmd18 = "sh " nocase
        $cmd19 = "bash " nocase
        $cmd20 = "powershell" nocase
        $cmd21 = "cmd.exe" nocase

        // Patterns combining user input with execution (Example for PHP, adapt for others)
        $combo1 = /\$(?:_GET|_POST|_REQUEST|_COOKIE)\s*\[[^]]*\]\s*[;&|`]/ nocase
        $combo2 = /(?:system|exec|passthru|shell_exec|popen|proc_open)\s*\(\s*\$(?:_GET|_POST|_REQUEST|_COOKIE)/ nocase

    condition:
        // Trigger if any execution function, dangerous command, or suspicious combination is found
        1 of ($php_exec*) or
        1 of ($py_exec*) or
        1 of ($pl_exec*) or
        1 of ($rb_exec*) or
        1 of ($js_exec*) or
        1 of ($cmd*) or
        1 of ($combo*) or
        // Trigger if a command separator is found near an execution function or user input (potential injection)
        // This requires more complex logic, potentially checking proximity or specific function arguments.
        // A simpler approach is included above, focusing on known functions and commands.
        // For a more robust rule, consider specific patterns like: `system($_GET['cmd'])`
        // $php_backtick or $pl_backtick or $rb_backtick // Uncomment if needed, but backticks can be common
        // 1 of ($sep*) // Uncomment if needed, but separators alone are too common
        false // Placeholder, final condition aggregates above checks
} 