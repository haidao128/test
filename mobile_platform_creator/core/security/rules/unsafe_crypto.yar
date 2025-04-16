rule UnsafeCrypto {
    meta:
        description = "检测不安全加密实现"
        author = "Mobile Platform Creator Security Team"
        date = "2024-03-20"
        severity = 5

    strings:
        // 弱哈希算法
        $weak_hash1 = "md5(" nocase
        $weak_hash2 = "sha1(" nocase
        $weak_hash3 = "sha256(" nocase
        $weak_hash4 = "sha512(" nocase
        $weak_hash5 = "hash(" nocase
        $weak_hash6 = "hash_file(" nocase
        $weak_hash7 = "hash_hmac(" nocase
        $weak_hash8 = "hash_hmac_file(" nocase
        $weak_hash9 = "hash_init(" nocase
        $weak_hash10 = "hash_update(" nocase
        $weak_hash11 = "hash_final(" nocase
        $weak_hash12 = "hash_copy(" nocase
        $weak_hash13 = "hash_equals(" nocase
        $weak_hash14 = "hash_pbkdf2(" nocase
        $weak_hash15 = "hash_pbkdf2(" nocase
        $weak_hash16 = "hash_pbkdf2(" nocase
        $weak_hash17 = "hash_pbkdf2(" nocase
        $weak_hash18 = "hash_pbkdf2(" nocase
        $weak_hash19 = "hash_pbkdf2(" nocase
        $weak_hash20 = "hash_pbkdf2(" nocase

        // 弱加密算法
        $weak_crypto1 = "des" nocase
        $weak_crypto2 = "3des" nocase
        $weak_crypto3 = "rc4" nocase
        $weak_crypto4 = "blowfish" nocase
        $weak_crypto5 = "aes-128-ecb" nocase
        $weak_crypto6 = "aes-192-ecb" nocase
        $weak_crypto7 = "aes-256-ecb" nocase
        $weak_crypto8 = "aes-128-cbc" nocase
        $weak_crypto9 = "aes-192-cbc" nocase
        $weak_crypto10 = "aes-256-cbc" nocase
        $weak_crypto11 = "aes-128-cfb" nocase
        $weak_crypto12 = "aes-192-cfb" nocase
        $weak_crypto13 = "aes-256-cfb" nocase
        $weak_crypto14 = "aes-128-ofb" nocase
        $weak_crypto15 = "aes-192-ofb" nocase
        $weak_crypto16 = "aes-256-ofb" nocase
        $weak_crypto17 = "aes-128-ctr" nocase
        $weak_crypto18 = "aes-192-ctr" nocase
        $weak_crypto19 = "aes-256-ctr" nocase
        $weak_crypto20 = "aes-128-gcm" nocase

        // 硬编码密钥
        $hardcoded_key1 = "key = " nocase
        $hardcoded_key2 = "secret = " nocase
        $hardcoded_key3 = "password = " nocase
        $hardcoded_key4 = "api_key = " nocase
        $hardcoded_key5 = "token = " nocase
        $hardcoded_key6 = "salt = " nocase
        $hardcoded_key7 = "iv = " nocase
        $hardcoded_key8 = "nonce = " nocase
        $hardcoded_key9 = "private_key = " nocase
        $hardcoded_key10 = "public_key = " nocase
        $hardcoded_key11 = "certificate = " nocase
        $hardcoded_key12 = "keystore = " nocase
        $hardcoded_key13 = "truststore = " nocase
        $hardcoded_key14 = "jks = " nocase
        $hardcoded_key15 = "p12 = " nocase
        $hardcoded_key16 = "pfx = " nocase
        $hardcoded_key17 = "pem = " nocase
        $hardcoded_key18 = "der = " nocase
        $hardcoded_key19 = "cer = " nocase
        $hardcoded_key20 = "crt = " nocase

        // 不安全的随机数生成
        $weak_random1 = "rand(" nocase
        $weak_random2 = "random(" nocase
        $weak_random3 = "mt_rand(" nocase
        $weak_random4 = "srand(" nocase
        $weak_random5 = "mt_srand(" nocase
        $weak_random6 = "Math.random()" nocase
        $weak_random7 = "Math.floor(Math.random()" nocase
        $weak_random8 = "Math.ceil(Math.random()" nocase
        $weak_random9 = "Math.round(Math.random()" nocase
        $weak_random10 = "Math.random() * " nocase
        $weak_random11 = "Math.random() + " nocase
        $weak_random12 = "Math.random() - " nocase
        $weak_random13 = "Math.random() / " nocase
        $weak_random14 = "Math.random() % " nocase
        $weak_random15 = "Math.random() ** " nocase
        $weak_random16 = "Math.random() ^ " nocase
        $weak_random17 = "Math.random() & " nocase
        $weak_random18 = "Math.random() | " nocase
        $weak_random19 = "Math.random() << " nocase
        $weak_random20 = "Math.random() >> " nocase

        // 不安全的密码存储
        $unsafe_password1 = "password_hash(" nocase
        $unsafe_password2 = "password_verify(" nocase
        $unsafe_password3 = "password_needs_rehash(" nocase
        $unsafe_password4 = "password_get_info(" nocase
        $unsafe_password5 = "password_hash(" nocase
        $unsafe_password6 = "password_verify(" nocase
        $unsafe_password7 = "password_needs_rehash(" nocase
        $unsafe_password8 = "password_get_info(" nocase
        $unsafe_password9 = "password_hash(" nocase
        $unsafe_password10 = "password_verify(" nocase
        $unsafe_password11 = "password_needs_rehash(" nocase
        $unsafe_password12 = "password_get_info(" nocase
        $unsafe_password13 = "password_hash(" nocase
        $unsafe_password14 = "password_verify(" nocase
        $unsafe_password15 = "password_needs_rehash(" nocase
        $unsafe_password16 = "password_get_info(" nocase
        $unsafe_password17 = "password_hash(" nocase
        $unsafe_password18 = "password_verify(" nocase
        $unsafe_password19 = "password_needs_rehash(" nocase
        $unsafe_password20 = "password_get_info(" nocase

        // 不安全的密钥交换
        $unsafe_key_exchange1 = "diffie-hellman" nocase
        $unsafe_key_exchange2 = "dh" nocase
        $unsafe_key_exchange3 = "rsa" nocase
        $unsafe_key_exchange4 = "dsa" nocase
        $unsafe_key_exchange5 = "ecdsa" nocase
        $unsafe_key_exchange6 = "ecdh" nocase
        $unsafe_key_exchange7 = "ecdh-es" nocase
        $unsafe_key_exchange8 = "ecdh-ss" nocase
        $unsafe_key_exchange9 = "ecdh-aes" nocase
        $unsafe_key_exchange10 = "ecdh-aes-128" nocase
        $unsafe_key_exchange11 = "ecdh-aes-192" nocase
        $unsafe_key_exchange12 = "ecdh-aes-256" nocase
        $unsafe_key_exchange13 = "ecdh-aes-128-gcm" nocase
        $unsafe_key_exchange14 = "ecdh-aes-192-gcm" nocase
        $unsafe_key_exchange15 = "ecdh-aes-256-gcm" nocase
        $unsafe_key_exchange16 = "ecdh-aes-128-ccm" nocase
        $unsafe_key_exchange17 = "ecdh-aes-192-ccm" nocase
        $unsafe_key_exchange18 = "ecdh-aes-256-ccm" nocase
        $unsafe_key_exchange19 = "ecdh-aes-128-ocb" nocase
        $unsafe_key_exchange20 = "ecdh-aes-192-ocb" nocase

        // 不安全的SSL/TLS配置
        $unsafe_ssl1 = "ssl_verify_peer" nocase
        $unsafe_ssl2 = "ssl_verify_host" nocase
        $unsafe_ssl3 = "ssl_verify_depth" nocase
        $unsafe_ssl4 = "ssl_verify_peer_name" nocase
        $unsafe_ssl5 = "ssl_verify_peer_cert" nocase
        $unsafe_ssl6 = "ssl_verify_peer_cert_chain" nocase
        $unsafe_ssl7 = "ssl_verify_peer_cert_revoked" nocase
        $unsafe_ssl8 = "ssl_verify_peer_cert_expired" nocase
        $unsafe_ssl9 = "ssl_verify_peer_cert_not_before" nocase
        $unsafe_ssl10 = "ssl_verify_peer_cert_not_after" nocase
        $unsafe_ssl11 = "ssl_verify_peer_cert_issuer" nocase
        $unsafe_ssl12 = "ssl_verify_peer_cert_subject" nocase
        $unsafe_ssl13 = "ssl_verify_peer_cert_serial" nocase
        $unsafe_ssl14 = "ssl_verify_peer_cert_fingerprint" nocase
        $unsafe_ssl15 = "ssl_verify_peer_cert_thumbprint" nocase
        $unsafe_ssl16 = "ssl_verify_peer_cert_thumbprint_sha1" nocase
        $unsafe_ssl17 = "ssl_verify_peer_cert_thumbprint_sha256" nocase
        $unsafe_ssl18 = "ssl_verify_peer_cert_thumbprint_sha512" nocase
        $unsafe_ssl19 = "ssl_verify_peer_cert_thumbprint_md5" nocase
        $unsafe_ssl20 = "ssl_verify_peer_cert_thumbprint_sha384" nocase

        // 不安全的密码学库使用
        $unsafe_crypto_lib1 = "crypto.createCipher(" nocase
        $unsafe_crypto_lib2 = "crypto.createDecipher(" nocase
        $unsafe_crypto_lib3 = "crypto.createHash(" nocase
        $unsafe_crypto_lib4 = "crypto.createHmac(" nocase
        $unsafe_crypto_lib5 = "crypto.createSign(" nocase
        $unsafe_crypto_lib6 = "crypto.createVerify(" nocase
        $unsafe_crypto_lib7 = "crypto.pbkdf2(" nocase
        $unsafe_crypto_lib8 = "crypto.pbkdf2Sync(" nocase
        $unsafe_crypto_lib9 = "crypto.randomBytes(" nocase
        $unsafe_crypto_lib10 = "crypto.randomFill(" nocase
        $unsafe_crypto_lib11 = "crypto.scrypt(" nocase
        $unsafe_crypto_lib12 = "crypto.scryptSync(" nocase
        $unsafe_crypto_lib13 = "crypto.timingSafeEqual(" nocase
        $unsafe_crypto_lib14 = "crypto.getCiphers(" nocase
        $unsafe_crypto_lib15 = "crypto.getHashes(" nocase
        $unsafe_crypto_lib16 = "crypto.getCurves(" nocase
        $unsafe_crypto_lib17 = "crypto.getFips(" nocase
        $unsafe_crypto_lib18 = "crypto.setFips(" nocase
        $unsafe_crypto_lib19 = "crypto.getRandomValues(" nocase
        $unsafe_crypto_lib20 = "crypto.subtle" nocase

        // 不安全的密码学参数
        $unsafe_crypto_params1 = "md5" nocase
        $unsafe_crypto_params2 = "sha1" nocase
        $unsafe_crypto_params3 = "sha256" nocase
        $unsafe_crypto_params4 = "sha512" nocase
        $unsafe_crypto_params5 = "des" nocase
        $unsafe_crypto_params6 = "3des" nocase
        $unsafe_crypto_params7 = "rc4" nocase
        $unsafe_crypto_params8 = "blowfish" nocase
        $unsafe_crypto_params9 = "aes-128-ecb" nocase
        $unsafe_crypto_params10 = "aes-192-ecb" nocase
        $unsafe_crypto_params11 = "aes-256-ecb" nocase
        $unsafe_crypto_params12 = "aes-128-cbc" nocase
        $unsafe_crypto_params13 = "aes-192-cbc" nocase
        $unsafe_crypto_params14 = "aes-256-cbc" nocase
        $unsafe_crypto_params15 = "aes-128-cfb" nocase
        $unsafe_crypto_params16 = "aes-192-cfb" nocase
        $unsafe_crypto_params17 = "aes-256-cfb" nocase
        $unsafe_crypto_params18 = "aes-128-ofb" nocase
        $unsafe_crypto_params19 = "aes-192-ofb" nocase
        $unsafe_crypto_params20 = "aes-256-ofb" nocase

    condition:
        // 检测不安全加密实现
        (
            // 1. 使用弱哈希算法
            any of ($weak_hash*) or
            
            // 2. 使用弱加密算法
            any of ($weak_crypto*) or
            
            // 3. 硬编码密钥
            any of ($hardcoded_key*) or
            
            // 4. 不安全的随机数生成
            any of ($weak_random*) or
            
            // 5. 不安全的密码存储
            any of ($unsafe_password*) or
            
            // 6. 不安全的密钥交换
            any of ($unsafe_key_exchange*) or
            
            // 7. 不安全的SSL/TLS配置
            any of ($unsafe_ssl*) or
            
            // 8. 不安全的密码学库使用
            any of ($unsafe_crypto_lib*) or
            
            // 9. 不安全的密码学参数
            any of ($unsafe_crypto_params*)
        )
} 