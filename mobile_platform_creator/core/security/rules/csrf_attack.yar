rule CSRFAttack {
    meta:
        description = "检测跨站请求伪造(CSRF)攻击"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // HTML表单相关
        $form1 = "<form" nocase
        $form2 = "method=" nocase
        $form3 = "action=" nocase
        $form4 = "enctype=" nocase
        $form5 = "multipart/form-data" nocase
        $form6 = "application/x-www-form-urlencoded" nocase

        // 缺少CSRF令牌的表单提交
        $no_token1 = "<form[^>]*>[^<]*<input[^>]*type=[\"']hidden[\"'][^>]*>[^<]*</form>" nocase
        $no_token2 = "<form[^>]*>[^<]*<input[^>]*type=[\"']submit[\"'][^>]*>[^<]*</form>" nocase

        // 自动提交表单的JavaScript代码
        $auto_submit1 = "document.forms[0].submit()" nocase
        $auto_submit2 = ".submit()" nocase
        $auto_submit3 = "form.submit()" nocase
        $auto_submit4 = "getElementById('form').submit()" nocase
        $auto_submit5 = "querySelector('form').submit()" nocase

        // AJAX请求
        $ajax1 = "XMLHttpRequest" nocase
        $ajax2 = "fetch(" nocase
        $ajax3 = "$.ajax" nocase
        $ajax4 = "$.get" nocase
        $ajax5 = "$.post" nocase
        $ajax6 = "axios." nocase
        $ajax7 = "superagent" nocase

        // 可疑的请求头操作
        $headers1 = "X-CSRF-Token" nocase
        $headers2 = "X-XSRF-Token" nocase
        $headers3 = "csrftoken" nocase
        $headers4 = "xsrftoken" nocase
        $headers5 = "authenticity_token" nocase

        // 可疑的Cookie操作
        $cookie1 = "document.cookie" nocase
        $cookie2 = "cookie=" nocase
        $cookie3 = "setCookie" nocase
        $cookie4 = "getCookie" nocase

        // 可疑的状态管理操作
        $state1 = "localStorage" nocase
        $state2 = "sessionStorage" nocase
        $state3 = "getItem" nocase
        $state4 = "setItem" nocase

        // 可疑的URL操作
        $url1 = "window.location" nocase
        $url2 = "location.href" nocase
        $url3 = "location.replace" nocase
        $url4 = "location.assign" nocase
        $url5 = "history.pushState" nocase
        $url6 = "history.replaceState" nocase

        // 可疑的DOM操作
        $dom1 = "createElement" nocase
        $dom2 = "appendChild" nocase
        $dom3 = "insertBefore" nocase
        $dom4 = "replaceChild" nocase
        $dom5 = "innerHTML" nocase
        $dom6 = "outerHTML" nocase
        $dom7 = "insertAdjacentHTML" nocase

    condition:
        // 检测可能的CSRF攻击场景
        (
            // 1. 表单提交但缺少CSRF保护
            (any of ($form*) and any of ($no_token*)) or
            
            // 2. 自动提交表单
            (any of ($form*) and any of ($auto_submit*)) or
            
            // 3. 可疑的AJAX请求
            (any of ($ajax*) and not any of ($headers*)) or
            
            // 4. Cookie操作与状态管理
            (any of ($cookie*) and any of ($state*)) or
            
            // 5. URL重定向与DOM操作
            (any of ($url*) and any of ($dom*))
        )
} 