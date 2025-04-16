rule SSRFAttack {
    meta:
        description = "检测服务器端请求伪造(SSRF)漏洞"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // 网络请求函数
        $request_func1 = "fetch(" nocase
        $request_func2 = "axios.get(" nocase
        $request_func3 = "axios.post(" nocase
        $request_func4 = "axios.put(" nocase
        $request_func5 = "axios.delete(" nocase
        $request_func6 = "axios.patch(" nocase
        $request_func7 = "axios.request(" nocase
        $request_func8 = "axios.all(" nocase
        $request_func9 = "axios.spread(" nocase
        $request_func10 = "axios.create(" nocase
        $request_func11 = "axios.CancelToken(" nocase
        $request_func12 = "axios.isCancel(" nocase
        $request_func13 = "axios.getUri(" nocase
        $request_func14 = "axios.defaults" nocase
        $request_func15 = "axios.interceptors" nocase
        $request_func16 = "axios.AxiosError" nocase
        $request_func17 = "axios.AxiosHeaders" nocase
        $request_func18 = "axios.AxiosRequestConfig" nocase
        $request_func19 = "axios.AxiosResponse" nocase
        $request_func20 = "axios.AxiosInstance" nocase

        // HTTP请求库
        $http_lib1 = "requests.get(" nocase
        $http_lib2 = "requests.post(" nocase
        $http_lib3 = "requests.put(" nocase
        $http_lib4 = "requests.delete(" nocase
        $http_lib5 = "requests.patch(" nocase
        $http_lib6 = "requests.head(" nocase
        $http_lib7 = "requests.options(" nocase
        $http_lib8 = "requests.Session(" nocase
        $http_lib9 = "requests.Request(" nocase
        $http_lib10 = "requests.Response(" nocase
        $http_lib11 = "requests.adapters" nocase
        $http_lib12 = "requests.auth" nocase
        $http_lib13 = "requests.cookies" nocase
        $http_lib14 = "requests.exceptions" nocase
        $http_lib15 = "requests.hooks" nocase
        $http_lib16 = "requests.models" nocase
        $http_lib17 = "requests.packages" nocase
        $http_lib18 = "requests.sessions" nocase
        $http_lib19 = "requests.status_codes" nocase
        $http_lib20 = "requests.structures" nocase

        // 文件操作函数
        $file_func1 = "file_get_contents(" nocase
        $file_func2 = "file_put_contents(" nocase
        $file_func3 = "fopen(" nocase
        $file_func4 = "fread(" nocase
        $file_func5 = "fwrite(" nocase
        $file_func6 = "fclose(" nocase
        $file_func7 = "fgets(" nocase
        $file_func8 = "fgetc(" nocase
        $file_func9 = "fscanf(" nocase
        $file_func10 = "fprintf(" nocase
        $file_func11 = "fseek(" nocase
        $file_func12 = "ftell(" nocase
        $file_func13 = "rewind(" nocase
        $file_func14 = "feof(" nocase
        $file_func15 = "fpassthru(" nocase
        $file_func16 = "fgetcsv(" nocase
        $file_func17 = "fputcsv(" nocase
        $file_func18 = "fgetss(" nocase
        $file_func19 = "fgetc(" nocase
        $file_func20 = "fgets(" nocase

        // 网络套接字函数
        $socket_func1 = "socket_create(" nocase
        $socket_func2 = "socket_connect(" nocase
        $socket_func3 = "socket_bind(" nocase
        $socket_func4 = "socket_listen(" nocase
        $socket_func5 = "socket_accept(" nocase
        $socket_func6 = "socket_read(" nocase
        $socket_func7 = "socket_write(" nocase
        $socket_func8 = "socket_close(" nocase
        $socket_func9 = "socket_shutdown(" nocase
        $socket_func10 = "socket_get_option(" nocase
        $socket_func11 = "socket_set_option(" nocase
        $socket_func12 = "socket_getpeername(" nocase
        $socket_func13 = "socket_getsockname(" nocase
        $socket_func14 = "socket_select(" nocase
        $socket_func15 = "socket_send(" nocase
        $socket_func16 = "socket_recv(" nocase
        $socket_func17 = "socket_sendto(" nocase
        $socket_func18 = "socket_recvfrom(" nocase
        $socket_func19 = "socket_sendmsg(" nocase
        $socket_func20 = "socket_recvmsg(" nocase

        // 内部网络地址
        $internal_ip1 = "127.0.0.1" nocase
        $internal_ip2 = "localhost" nocase
        $internal_ip3 = "0.0.0.0" nocase
        $internal_ip4 = "10." nocase
        $internal_ip5 = "172.16." nocase
        $internal_ip6 = "172.17." nocase
        $internal_ip7 = "172.18." nocase
        $internal_ip8 = "172.19." nocase
        $internal_ip9 = "172.20." nocase
        $internal_ip10 = "172.21." nocase
        $internal_ip11 = "172.22." nocase
        $internal_ip12 = "172.23." nocase
        $internal_ip13 = "172.24." nocase
        $internal_ip14 = "172.25." nocase
        $internal_ip15 = "172.26." nocase
        $internal_ip16 = "172.27." nocase
        $internal_ip17 = "172.28." nocase
        $internal_ip18 = "172.29." nocase
        $internal_ip19 = "172.30." nocase
        $internal_ip20 = "172.31." nocase

        // 内部网络服务
        $internal_service1 = "file://" nocase
        $internal_service2 = "ftp://" nocase
        $internal_service3 = "sftp://" nocase
        $internal_service4 = "gopher://" nocase
        $internal_service5 = "dict://" nocase
        $internal_service6 = "ldap://" nocase
        $internal_service7 = "ldaps://" nocase
        $internal_service8 = "tftp://" nocase
        $internal_service9 = "jar:" nocase
        $internal_service10 = "netdoc:" nocase
        $internal_service11 = "mailto:" nocase
        $internal_service12 = "news:" nocase
        $internal_service13 = "nntp:" nocase
        $internal_service14 = "pop:" nocase
        $internal_service15 = "pop3:" nocase
        $internal_service16 = "smtp:" nocase
        $internal_service17 = "telnet:" nocase
        $internal_service18 = "urn:" nocase
        $internal_service19 = "view-source:" nocase
        $internal_service20 = "ws:" nocase

        // 用户输入变量
        $user_input1 = "$_GET" nocase
        $user_input2 = "$_POST" nocase
        $user_input3 = "$_REQUEST" nocase
        $user_input4 = "$_COOKIE" nocase
        $user_input5 = "$_FILES" nocase
        $user_input6 = "$_ENV" nocase
        $user_input7 = "$_SERVER" nocase
        $user_input8 = "$_SESSION" nocase
        $user_input9 = "req.query" nocase
        $user_input10 = "req.body" nocase
        $user_input11 = "req.params" nocase
        $user_input12 = "req.cookies" nocase
        $user_input13 = "req.headers" nocase
        $user_input14 = "req.files" nocase
        $user_input15 = "req.get(" nocase
        $user_input16 = "req.param(" nocase
        $user_input17 = "req.path" nocase
        $user_input18 = "req.url" nocase
        $user_input19 = "req.originalUrl" nocase
        $user_input20 = "req.baseUrl" nocase

        // URL解析函数
        $url_parse1 = "parse_url(" nocase
        $url_parse2 = "urlparse(" nocase
        $url_parse3 = "urljoin(" nocase
        $url_parse4 = "urlsplit(" nocase
        $url_parse5 = "urlunparse(" nocase
        $url_parse6 = "urlunsplit(" nocase
        $url_parse7 = "urlencode(" nocase
        $url_parse8 = "urldecode(" nocase
        $url_parse9 = "rawurlencode(" nocase
        $url_parse10 = "rawurldecode(" nocase
        $url_parse11 = "http_build_query(" nocase
        $url_parse12 = "parse_str(" nocase
        $url_parse13 = "parse_ini_string(" nocase
        $url_parse14 = "parse_ini_file(" nocase
        $url_parse15 = "parse_ini_section(" nocase
        $url_parse16 = "parse_ini_sections(" nocase
        $url_parse17 = "parse_ini_section_names(" nocase
        $url_parse18 = "parse_ini_section_values(" nocase
        $url_parse19 = "parse_ini_section_keys(" nocase
        $url_parse20 = "parse_ini_section_key_values(" nocase

        // 不安全的URL验证
        $unsafe_url_validation1 = "filter_var(" nocase
        $unsafe_url_validation2 = "filter_input(" nocase
        $unsafe_url_validation3 = "filter_input_array(" nocase
        $unsafe_url_validation4 = "filter_list(" nocase
        $unsafe_url_validation5 = "filter_has_var(" nocase
        $unsafe_url_validation6 = "filter_id(" nocase
        $unsafe_url_validation7 = "filter_var_array(" nocase
        $unsafe_url_validation8 = "filter_var_array(" nocase
        $unsafe_url_validation9 = "filter_var_array(" nocase
        $unsafe_url_validation10 = "filter_var_array(" nocase
        $unsafe_url_validation11 = "filter_var_array(" nocase
        $unsafe_url_validation12 = "filter_var_array(" nocase
        $unsafe_url_validation13 = "filter_var_array(" nocase
        $unsafe_url_validation14 = "filter_var_array(" nocase
        $unsafe_url_validation15 = "filter_var_array(" nocase
        $unsafe_url_validation16 = "filter_var_array(" nocase
        $unsafe_url_validation17 = "filter_var_array(" nocase
        $unsafe_url_validation18 = "filter_var_array(" nocase
        $unsafe_url_validation19 = "filter_var_array(" nocase
        $unsafe_url_validation20 = "filter_var_array(" nocase

        // 不安全的URL重定向
        $unsafe_redirect1 = "header(" nocase
        $unsafe_redirect2 = "Location:" nocase
        $unsafe_redirect3 = "window.location" nocase
        $unsafe_redirect4 = "window.location.href" nocase
        $unsafe_redirect5 = "window.location.replace(" nocase
        $unsafe_redirect6 = "window.location.assign(" nocase
        $unsafe_redirect7 = "document.location" nocase
        $unsafe_redirect8 = "document.location.href" nocase
        $unsafe_redirect9 = "document.location.replace(" nocase
        $unsafe_redirect10 = "document.location.assign(" nocase
        $unsafe_redirect11 = "location.href" nocase
        $unsafe_redirect12 = "location.replace(" nocase
        $unsafe_redirect13 = "location.assign(" nocase
        $unsafe_redirect14 = "location.reload(" nocase
        $unsafe_redirect15 = "location.search" nocase
        $unsafe_redirect16 = "location.hash" nocase
        $unsafe_redirect17 = "location.pathname" nocase
        $unsafe_redirect18 = "location.hostname" nocase
        $unsafe_redirect19 = "location.port" nocase
        $unsafe_redirect20 = "location.protocol" nocase

        // 不安全的URL拼接
        $unsafe_url_concat1 = "url + " nocase
        $unsafe_url_concat2 = "url +=" nocase
        $unsafe_url_concat3 = "url.concat(" nocase
        $unsafe_url_concat4 = "url + user_input" nocase
        $unsafe_url_concat5 = "url += user_input" nocase
        $unsafe_url_concat6 = "url.concat(user_input)" nocase
        $unsafe_url_concat7 = "base_url + path" nocase
        $unsafe_url_concat8 = "base_url += path" nocase
        $unsafe_url_concat9 = "base_url.concat(path)" nocase
        $unsafe_url_concat10 = "base_url + user_input" nocase
        $unsafe_url_concat11 = "base_url += user_input" nocase
        $unsafe_url_concat12 = "base_url.concat(user_input)" nocase
        $unsafe_url_concat13 = "api_url + endpoint" nocase
        $unsafe_url_concat14 = "api_url += endpoint" nocase
        $unsafe_url_concat15 = "api_url.concat(endpoint)" nocase
        $unsafe_url_concat16 = "api_url + user_input" nocase
        $unsafe_url_concat17 = "api_url += user_input" nocase
        $unsafe_url_concat18 = "api_url.concat(user_input)" nocase
        $unsafe_url_concat19 = "service_url + path" nocase
        $unsafe_url_concat20 = "service_url += path" nocase

    condition:
        // 检测SSRF漏洞
        (
            // 1. 网络请求函数与用户输入组合
            (
                any of ($request_func*) and
                any of ($user_input*)
            ) or
            
            // 2. HTTP请求库与用户输入组合
            (
                any of ($http_lib*) and
                any of ($user_input*)
            ) or
            
            // 3. 文件操作函数与用户输入组合
            (
                any of ($file_func*) and
                any of ($user_input*)
            ) or
            
            // 4. 网络套接字函数与用户输入组合
            (
                any of ($socket_func*) and
                any of ($user_input*)
            ) or
            
            // 5. 内部网络地址与用户输入组合
            (
                any of ($internal_ip*) and
                any of ($user_input*)
            ) or
            
            // 6. 内部网络服务与用户输入组合
            (
                any of ($internal_service*) and
                any of ($user_input*)
            ) or
            
            // 7. URL解析函数与用户输入组合
            (
                any of ($url_parse*) and
                any of ($user_input*)
            ) or
            
            // 8. 不安全的URL验证与用户输入组合
            (
                any of ($unsafe_url_validation*) and
                any of ($user_input*)
            ) or
            
            // 9. 不安全的URL重定向与用户输入组合
            (
                any of ($unsafe_redirect*) and
                any of ($user_input*)
            ) or
            
            // 10. 不安全的URL拼接与用户输入组合
            (
                any of ($unsafe_url_concat*) and
                any of ($user_input*)
            )
        )
} 