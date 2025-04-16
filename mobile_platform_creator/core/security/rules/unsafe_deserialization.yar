rule UnsafeDeserialization {
    meta:
        description = "检测不安全反序列化漏洞"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // PHP反序列化函数
        $php_deserial1 = "unserialize(" nocase
        $php_deserial2 = "yaml_parse(" nocase
        $php_deserial3 = "yaml_parse_file(" nocase
        $php_deserial4 = "yaml_parse_url(" nocase
        $php_deserial5 = "json_decode(" nocase
        $php_deserial6 = "json_decode(" nocase
        $php_deserial7 = "wddx_deserialize(" nocase
        $php_deserial8 = "xml_parse(" nocase
        $php_deserial9 = "xml_parse_string(" nocase
        $php_deserial10 = "xml_parse_into_struct(" nocase
        $php_deserial11 = "xml_parse_file(" nocase
        $php_deserial12 = "xml_parse_into_struct(" nocase
        $php_deserial13 = "xml_parse_into_struct(" nocase
        $php_deserial14 = "xml_parse_into_struct(" nocase
        $php_deserial15 = "xml_parse_into_struct(" nocase
        $php_deserial16 = "xml_parse_into_struct(" nocase
        $php_deserial17 = "xml_parse_into_struct(" nocase
        $php_deserial18 = "xml_parse_into_struct(" nocase
        $php_deserial19 = "xml_parse_into_struct(" nocase
        $php_deserial20 = "xml_parse_into_struct(" nocase

        // Python反序列化函数
        $py_deserial1 = "pickle.loads(" nocase
        $py_deserial2 = "pickle.load(" nocase
        $py_deserial3 = "yaml.load(" nocase
        $py_deserial4 = "yaml.safe_load(" nocase
        $py_deserial5 = "yaml.load_all(" nocase
        $py_deserial6 = "yaml.safe_load_all(" nocase
        $py_deserial7 = "json.loads(" nocase
        $py_deserial8 = "json.load(" nocase
        $py_deserial9 = "marshal.loads(" nocase
        $py_deserial10 = "marshal.load(" nocase
        $py_deserial11 = "ast.literal_eval(" nocase
        $py_deserial12 = "eval(" nocase
        $py_deserial13 = "exec(" nocase
        $py_deserial14 = "compile(" nocase
        $py_deserial15 = "codecs.decode(" nocase
        $py_deserial16 = "base64.b64decode(" nocase
        $py_deserial17 = "base64.b32decode(" nocase
        $py_deserial18 = "base64.b16decode(" nocase
        $py_deserial19 = "base64.b85decode(" nocase
        $py_deserial20 = "base64.a85decode(" nocase

        // Java反序列化函数
        $java_deserial1 = "ObjectInputStream" nocase
        $java_deserial2 = "readObject(" nocase
        $java_deserial3 = "readUnshared(" nocase
        $java_deserial4 = "readClassDescriptor(" nocase
        $java_deserial5 = "readStreamHeader(" nocase
        $java_deserial6 = "readStreamHeader(" nocase
        $java_deserial7 = "readStreamHeader(" nocase
        $java_deserial8 = "readStreamHeader(" nocase
        $java_deserial9 = "readStreamHeader(" nocase
        $java_deserial10 = "readStreamHeader(" nocase
        $java_deserial11 = "readStreamHeader(" nocase
        $java_deserial12 = "readStreamHeader(" nocase
        $java_deserial13 = "readStreamHeader(" nocase
        $java_deserial14 = "readStreamHeader(" nocase
        $java_deserial15 = "readStreamHeader(" nocase
        $java_deserial16 = "readStreamHeader(" nocase
        $java_deserial17 = "readStreamHeader(" nocase
        $java_deserial18 = "readStreamHeader(" nocase
        $java_deserial19 = "readStreamHeader(" nocase
        $java_deserial20 = "readStreamHeader(" nocase

        // .NET反序列化函数
        $dotnet_deserial1 = "BinaryFormatter" nocase
        $dotnet_deserial2 = "Deserialize(" nocase
        $dotnet_deserial3 = "JavaScriptSerializer" nocase
        $dotnet_deserial4 = "DeserializeObject(" nocase
        $dotnet_deserial5 = "DataContractSerializer" nocase
        $dotnet_deserial6 = "ReadObject(" nocase
        $dotnet_deserial7 = "XmlSerializer" nocase
        $dotnet_deserial8 = "Deserialize(" nocase
        $dotnet_deserial9 = "JsonConvert.DeserializeObject" nocase
        $dotnet_deserial10 = "JsonConvert.DeserializeAnonymousType" nocase
        $dotnet_deserial11 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial12 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial13 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial14 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial15 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial16 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial17 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial18 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial19 = "JsonConvert.DeserializeXmlNode" nocase
        $dotnet_deserial20 = "JsonConvert.DeserializeXmlNode" nocase

        // Node.js反序列化函数
        $node_deserial1 = "JSON.parse(" nocase
        $node_deserial2 = "eval(" nocase
        $node_deserial3 = "Function(" nocase
        $node_deserial4 = "require(" nocase
        $node_deserial5 = "import(" nocase
        $node_deserial6 = "vm.runInNewContext(" nocase
        $node_deserial7 = "vm.runInThisContext(" nocase
        $node_deserial8 = "vm.runInContext(" nocase
        $node_deserial9 = "vm.createScript(" nocase
        $node_deserial10 = "vm.createContext(" nocase
        $node_deserial11 = "vm.createContext(" nocase
        $node_deserial12 = "vm.createContext(" nocase
        $node_deserial13 = "vm.createContext(" nocase
        $node_deserial14 = "vm.createContext(" nocase
        $node_deserial15 = "vm.createContext(" nocase
        $node_deserial16 = "vm.createContext(" nocase
        $node_deserial17 = "vm.createContext(" nocase
        $node_deserial18 = "vm.createContext(" nocase
        $node_deserial19 = "vm.createContext(" nocase
        $node_deserial20 = "vm.createContext(" nocase

        // Ruby反序列化函数
        $ruby_deserial1 = "Marshal.load(" nocase
        $ruby_deserial2 = "YAML.load(" nocase
        $ruby_deserial3 = "YAML.safe_load(" nocase
        $ruby_deserial4 = "JSON.parse(" nocase
        $ruby_deserial5 = "eval(" nocase
        $ruby_deserial6 = "instance_eval(" nocase
        $ruby_deserial7 = "class_eval(" nocase
        $ruby_deserial8 = "module_eval(" nocase
        $ruby_deserial9 = "send(" nocase
        $ruby_deserial10 = "public_send(" nocase
        $ruby_deserial11 = "method_missing(" nocase
        $ruby_deserial12 = "const_get(" nocase
        $ruby_deserial13 = "const_set(" nocase
        $ruby_deserial14 = "const_missing(" nocase
        $ruby_deserial15 = "method_missing(" nocase
        $ruby_deserial16 = "method_missing(" nocase
        $ruby_deserial17 = "method_missing(" nocase
        $ruby_deserial18 = "method_missing(" nocase
        $ruby_deserial19 = "method_missing(" nocase
        $ruby_deserial20 = "method_missing(" nocase

        // 用户输入与反序列化结合
        $user_input1 = "$_GET" nocase
        $user_input2 = "$_POST" nocase
        $user_input3 = "$_REQUEST" nocase
        $user_input4 = "$_COOKIE" nocase
        $user_input5 = "$_FILES" nocase
        $user_input6 = "$_ENV" nocase
        $user_input7 = "$_SERVER" nocase
        $user_input8 = "$_SESSION" nocase
        $user_input9 = "$GLOBALS" nocase
        $user_input10 = "request.getParameter(" nocase
        $user_input11 = "request.getQueryString(" nocase
        $user_input12 = "request.getParameterValues(" nocase
        $user_input13 = "request.getParameterMap(" nocase
        $user_input14 = "request.getHeader(" nocase
        $user_input15 = "request.getHeaders(" nocase
        $user_input16 = "request.getHeaderNames(" nocase
        $user_input17 = "request.getAttribute(" nocase
        $user_input18 = "request.getAttributeNames(" nocase
        $user_input19 = "request.getCookies(" nocase
        $user_input20 = "request.getInputStream(" nocase

        // 危险的反序列化模式
        $dangerous1 = "ObjectInputStream" nocase
        $dangerous2 = "BinaryFormatter" nocase
        $dangerous3 = "pickle.loads" nocase
        $dangerous4 = "yaml.load" nocase
        $dangerous5 = "eval(" nocase
        $dangerous6 = "exec(" nocase
        $dangerous7 = "Function(" nocase
        $dangerous8 = "require(" nocase
        $dangerous9 = "import(" nocase
        $dangerous10 = "vm.runInNewContext" nocase
        $dangerous11 = "vm.runInThisContext" nocase
        $dangerous12 = "vm.runInContext" nocase
        $dangerous13 = "vm.createScript" nocase
        $dangerous14 = "Marshal.load" nocase
        $dangerous15 = "YAML.load" nocase
        $dangerous16 = "instance_eval" nocase
        $dangerous17 = "class_eval" nocase
        $dangerous18 = "module_eval" nocase
        $dangerous19 = "send(" nocase
        $dangerous20 = "const_get" nocase

    condition:
        // 检测不安全反序列化漏洞
        (
            // 1. 直接使用危险的反序列化函数
            any of ($dangerous*) or
            
            // 2. 用户输入与反序列化函数结合
            (
                (any of ($php_deserial*) and any of ($user_input*)) or
                (any of ($py_deserial*) and any of ($user_input*)) or
                (any of ($java_deserial*) and any of ($user_input*)) or
                (any of ($dotnet_deserial*) and any of ($user_input*)) or
                (any of ($node_deserial*) and any of ($user_input*)) or
                (any of ($ruby_deserial*) and any of ($user_input*))
            )
        )
} 