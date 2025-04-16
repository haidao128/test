rule XXEAttack {
    meta:
        description = "检测XML外部实体注入(XXE)漏洞"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // XML解析器相关
        $xml_parser1 = "DOMParser" nocase
        $xml_parser2 = "XMLHttpRequest" nocase
        $xml_parser3 = "XMLSerializer" nocase
        $xml_parser4 = "XMLDocument" nocase
        $xml_parser5 = "XMLReader" nocase
        $xml_parser6 = "XMLStreamReader" nocase
        $xml_parser7 = "SAXParser" nocase
        $xml_parser8 = "SAXBuilder" nocase
        $xml_parser9 = "SAXReader" nocase
        $xml_parser10 = "SAXParserFactory" nocase
        $xml_parser11 = "DocumentBuilder" nocase
        $xml_parser12 = "DocumentBuilderFactory" nocase
        $xml_parser13 = "XMLInputFactory" nocase
        $xml_parser14 = "XMLOutputFactory" nocase
        $xml_parser15 = "XMLStreamWriter" nocase
        $xml_parser16 = "XMLStreamReader" nocase
        $xml_parser17 = "XMLStreamEvent" nocase
        $xml_parser18 = "XMLStreamConstants" nocase
        $xml_parser19 = "XMLStreamException" nocase
        $xml_parser20 = "XMLStreamWriter" nocase

        // XML解析函数相关
        $xml_parse1 = "parseXML(" nocase
        $xml_parse2 = "parseFromString(" nocase
        $xml_parse3 = "parseFromFile(" nocase
        $xml_parse4 = "parseFromStream(" nocase
        $xml_parse5 = "parseFromURL(" nocase
        $xml_parse6 = "parseFromString(" nocase
        $xml_parse7 = "parseFromFile(" nocase
        $xml_parse8 = "parseFromStream(" nocase
        $xml_parse9 = "parseFromURL(" nocase
        $xml_parse10 = "parseFromString(" nocase
        $xml_parse11 = "parseFromFile(" nocase
        $xml_parse12 = "parseFromStream(" nocase
        $xml_parse13 = "parseFromURL(" nocase
        $xml_parse14 = "parseFromString(" nocase
        $xml_parse15 = "parseFromFile(" nocase
        $xml_parse16 = "parseFromStream(" nocase
        $xml_parse17 = "parseFromURL(" nocase
        $xml_parse18 = "parseFromString(" nocase
        $xml_parse19 = "parseFromFile(" nocase
        $xml_parse20 = "parseFromStream(" nocase

        // XML外部实体相关
        $xxe1 = "<!ENTITY" nocase
        $xxe2 = "SYSTEM" nocase
        $xxe3 = "PUBLIC" nocase
        $xxe4 = "DOCTYPE" nocase
        $xxe5 = "ELEMENT" nocase
        $xxe6 = "ATTLIST" nocase
        $xxe7 = "NOTATION" nocase
        $xxe8 = "ENTITY" nocase
        $xxe9 = "PARAMETER" nocase
        $xxe10 = "CDATA" nocase
        $xxe11 = "PCDATA" nocase
        $xxe12 = "ANY" nocase
        $xxe13 = "EMPTY" nocase
        $xxe14 = "NMTOKEN" nocase
        $xxe15 = "NMTOKENS" nocase
        $xxe16 = "ID" nocase
        $xxe17 = "IDREF" nocase
        $xxe18 = "IDREFS" nocase
        $xxe19 = "ENTITY" nocase
        $xxe20 = "ENTITIES" nocase

        // 外部实体URL相关
        $xxe_url1 = "file://" nocase
        $xxe_url2 = "ftp://" nocase
        $xxe_url3 = "gopher://" nocase
        $xxe_url4 = "http://" nocase
        $xxe_url5 = "https://" nocase
        $xxe_url6 = "netdoc://" nocase
        $xxe_url7 = "jar://" nocase
        $xxe_url8 = "mailto:" nocase
        $xxe_url9 = "data:" nocase
        $xxe_url10 = "ldap://" nocase
        $xxe_url11 = "ldaps://" nocase
        $xxe_url12 = "dav://" nocase
        $xxe_url13 = "davs://" nocase
        $xxe_url14 = "urn:" nocase
        $xxe_url15 = "news:" nocase
        $xxe_url16 = "nntp:" nocase
        $xxe_url17 = "telnet:" nocase
        $xxe_url18 = "tel:" nocase
        $xxe_url19 = "fax:" nocase
        $xxe_url20 = "modem:" nocase

        // 外部实体协议相关
        $xxe_protocol1 = "expect://" nocase
        $xxe_protocol2 = "php://" nocase
        $xxe_protocol3 = "glob://" nocase
        $xxe_protocol4 = "phar://" nocase
        $xxe_protocol5 = "data://" nocase
        $xxe_protocol6 = "file://" nocase
        $xxe_protocol7 = "ftp://" nocase
        $xxe_protocol8 = "gopher://" nocase
        $xxe_protocol9 = "http://" nocase
        $xxe_protocol10 = "https://" nocase
        $xxe_protocol11 = "netdoc://" nocase
        $xxe_protocol12 = "jar://" nocase
        $xxe_protocol13 = "mailto:" nocase
        $xxe_protocol14 = "data:" nocase
        $xxe_protocol15 = "ldap://" nocase
        $xxe_protocol16 = "ldaps://" nocase
        $xxe_protocol17 = "dav://" nocase
        $xxe_protocol18 = "davs://" nocase
        $xxe_protocol19 = "urn:" nocase
        $xxe_protocol20 = "news:" nocase

        // 外部实体攻击模式相关
        $xxe_pattern1 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>" nocase
        $xxe_pattern2 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///etc/shadow\"> ]>" nocase
        $xxe_pattern3 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/environ\"> ]>" nocase
        $xxe_pattern4 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/cmdline\"> ]>" nocase
        $xxe_pattern5 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/0\"> ]>" nocase
        $xxe_pattern6 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/1\"> ]>" nocase
        $xxe_pattern7 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/2\"> ]>" nocase
        $xxe_pattern8 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/3\"> ]>" nocase
        $xxe_pattern9 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/4\"> ]>" nocase
        $xxe_pattern10 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/5\"> ]>" nocase
        $xxe_pattern11 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/6\"> ]>" nocase
        $xxe_pattern12 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/7\"> ]>" nocase
        $xxe_pattern13 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/8\"> ]>" nocase
        $xxe_pattern14 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/9\"> ]>" nocase
        $xxe_pattern15 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/10\"> ]>" nocase
        $xxe_pattern16 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/11\"> ]>" nocase
        $xxe_pattern17 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/12\"> ]>" nocase
        $xxe_pattern18 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/13\"> ]>" nocase
        $xxe_pattern19 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/14\"> ]>" nocase
        $xxe_pattern20 = "<!DOCTYPE test [ <!ENTITY xxe SYSTEM \"file:///proc/self/fd/15\"> ]>" nocase

        // 外部实体攻击目标相关
        $xxe_target1 = "/etc/passwd" nocase
        $xxe_target2 = "/etc/shadow" nocase
        $xxe_target3 = "/etc/hosts" nocase
        $xxe_target4 = "/etc/group" nocase
        $xxe_target5 = "/etc/gshadow" nocase
        $xxe_target6 = "/etc/sudoers" nocase
        $xxe_target7 = "/etc/ssh/sshd_config" nocase
        $xxe_target8 = "/etc/ssh/ssh_config" nocase
        $xxe_target9 = "/etc/ssh/known_hosts" nocase
        $xxe_target10 = "/etc/ssh/authorized_keys" nocase
        $xxe_target11 = "/etc/ssh/id_rsa" nocase
        $xxe_target12 = "/etc/ssh/id_dsa" nocase
        $xxe_target13 = "/etc/ssh/id_ecdsa" nocase
        $xxe_target14 = "/etc/ssh/id_ed25519" nocase
        $xxe_target15 = "/etc/ssh/known_hosts" nocase
        $xxe_target16 = "/etc/ssh/authorized_keys" nocase
        $xxe_target17 = "/etc/ssh/id_rsa" nocase
        $xxe_target18 = "/etc/ssh/id_dsa" nocase
        $xxe_target19 = "/etc/ssh/id_ecdsa" nocase
        $xxe_target20 = "/etc/ssh/id_ed25519" nocase

        // 外部实体攻击参数相关
        $xxe_param1 = "FEATURE_GENERAL_ENTITY_REFERENCES" nocase
        $xxe_param2 = "FEATURE_PARAMETER_ENTITY_REFERENCES" nocase
        $xxe_param3 = "FEATURE_EXTERNAL_GENERAL_ENTITIES" nocase
        $xxe_param4 = "FEATURE_EXTERNAL_PARAMETER_ENTITIES" nocase
        $xxe_param5 = "FEATURE_DISALLOW_DOCTYPE_DECL" nocase
        $xxe_param6 = "FEATURE_LOAD_EXTERNAL_DTD" nocase
        $xxe_param7 = "FEATURE_VALIDATION" nocase
        $xxe_param8 = "FEATURE_NAMESPACES" nocase
        $xxe_param9 = "FEATURE_NAMESPACE_PREFIXES" nocase
        $xxe_param10 = "FEATURE_XINCLUDE" nocase
        $xxe_param11 = "FEATURE_SECURE_PROCESSING" nocase
        $xxe_param12 = "FEATURE_STRINGINTERNING" nocase
        $xxe_param13 = "FEATURE_FAST_INFOSET" nocase
        $xxe_param14 = "FEATURE_FAST_INFOSET_OPTIMIZATION" nocase
        $xxe_param15 = "FEATURE_FAST_INFOSET_OPTIMIZATION_LEVEL" nocase
        $xxe_param16 = "FEATURE_FAST_INFOSET_OPTIMIZATION_LEVEL_MAX" nocase
        $xxe_param17 = "FEATURE_FAST_INFOSET_OPTIMIZATION_LEVEL_MIN" nocase
        $xxe_param18 = "FEATURE_FAST_INFOSET_OPTIMIZATION_LEVEL_DEFAULT" nocase
        $xxe_param19 = "FEATURE_FAST_INFOSET_OPTIMIZATION_LEVEL_CUSTOM" nocase
        $xxe_param20 = "FEATURE_FAST_INFOSET_OPTIMIZATION_LEVEL_NONE" nocase

        // 外部实体攻击防御相关
        $xxe_defense1 = "setFeature(\"http://xml.org/sax/features/external-general-entities\", false)" nocase
        $xxe_defense2 = "setFeature(\"http://xml.org/sax/features/external-parameter-entities\", false)" nocase
        $xxe_defense3 = "setFeature(\"http://apache.org/xml/features/disallow-doctype-decl\", true)" nocase
        $xxe_defense4 = "setFeature(\"http://apache.org/xml/features/nonvalidating/load-external-dtd\", false)" nocase
        $xxe_defense5 = "setExpandEntityReferences(false)" nocase
        $xxe_defense6 = "setFeature(\"http://xml.org/sax/features/validation\", false)" nocase
        $xxe_defense7 = "setFeature(\"http://apache.org/xml/features/nonvalidating/load-external-dtd\", false)" nocase
        $xxe_defense8 = "setFeature(\"http://xml.org/sax/features/external-general-entities\", false)" nocase
        $xxe_defense9 = "setFeature(\"http://xml.org/sax/features/external-parameter-entities\", false)" nocase
        $xxe_defense10 = "setFeature(\"http://apache.org/xml/features/disallow-doctype-decl\", true)" nocase
        $xxe_defense11 = "setFeature(\"http://apache.org/xml/features/nonvalidating/load-external-dtd\", false)" nocase
        $xxe_defense12 = "setExpandEntityReferences(false)" nocase
        $xxe_defense13 = "setFeature(\"http://xml.org/sax/features/validation\", false)" nocase
        $xxe_defense14 = "setFeature(\"http://apache.org/xml/features/nonvalidating/load-external-dtd\", false)" nocase
        $xxe_defense15 = "setFeature(\"http://xml.org/sax/features/external-general-entities\", false)" nocase
        $xxe_defense16 = "setFeature(\"http://xml.org/sax/features/external-parameter-entities\", false)" nocase
        $xxe_defense17 = "setFeature(\"http://apache.org/xml/features/disallow-doctype-decl\", true)" nocase
        $xxe_defense18 = "setFeature(\"http://apache.org/xml/features/nonvalidating/load-external-dtd\", false)" nocase
        $xxe_defense19 = "setExpandEntityReferences(false)" nocase
        $xxe_defense20 = "setFeature(\"http://xml.org/sax/features/validation\", false)" nocase

        // 外部实体攻击检测相关
        $xxe_detection1 = "ENTITY" nocase
        $xxe_detection2 = "SYSTEM" nocase
        $xxe_detection3 = "PUBLIC" nocase
        $xxe_detection4 = "DOCTYPE" nocase
        $xxe_detection5 = "ELEMENT" nocase
        $xxe_detection6 = "ATTLIST" nocase
        $xxe_detection7 = "NOTATION" nocase
        $xxe_detection8 = "PARAMETER" nocase
        $xxe_detection9 = "CDATA" nocase
        $xxe_detection10 = "PCDATA" nocase
        $xxe_detection11 = "ANY" nocase
        $xxe_detection12 = "EMPTY" nocase
        $xxe_detection13 = "NMTOKEN" nocase
        $xxe_detection14 = "NMTOKENS" nocase
        $xxe_detection15 = "ID" nocase
        $xxe_detection16 = "IDREF" nocase
        $xxe_detection17 = "IDREFS" nocase
        $xxe_detection18 = "ENTITIES" nocase
        $xxe_detection19 = "ENTITY" nocase
        $xxe_detection20 = "SYSTEM" nocase

    condition:
        // 检测XXE漏洞
        (
            // 1. 检测XML解析器
            any of ($xml_parser*) and
            
            // 2. 检测XML解析函数
            any of ($xml_parse*) and
            
            // 3. 检测外部实体
            (
                // 3.1 检测外部实体定义
                any of ($xxe*) or
                
                // 3.2 检测外部实体URL
                any of ($xxe_url*) or
                
                // 3.3 检测外部实体协议
                any of ($xxe_protocol*) or
                
                // 3.4 检测外部实体攻击模式
                any of ($xxe_pattern*) or
                
                // 3.5 检测外部实体攻击目标
                any of ($xxe_target*) or
                
                // 3.6 检测外部实体攻击参数
                any of ($xxe_param*) or
                
                // 3.7 检测外部实体攻击防御
                any of ($xxe_defense*) or
                
                // 3.8 检测外部实体攻击检测
                any of ($xxe_detection*)
            )
        )
} 