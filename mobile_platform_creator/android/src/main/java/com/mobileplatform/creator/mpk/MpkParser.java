package com.mobileplatform.creator.mpk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * MPK 文件解析器 (已过时 - 适用于旧的二进制格式)
 * 
 * @deprecated MPK 格式已更新为基于标准 ZIP 的格式 (v2.1+)。
 *             请使用 {@link MpkFile#fromFile(File)} 来解析新的 MPK 包。
 *             此类不再维护，并可能在未来版本中移除。
 */
@Deprecated
public class MpkParser {

    private static final String TAG = "MpkParser(Deprecated)";

    /**
     * @deprecated 请使用 {@link MpkFile#fromFile(File)}。
     */
    @Deprecated
    public static MpkFile fromFile(File file) throws IOException, MpkException {
        throw new UnsupportedOperationException("MpkParser 已过时，请使用 MpkFile.fromFile() 解析基于 ZIP 的 MPK 包。");
    }

    /**
     * @deprecated 请使用 {@link MpkFile#fromFile(File)} 或其他流处理方式。
     */
    @Deprecated
    public static MpkFile fromInputStream(InputStream inputStream) throws IOException, MpkException {
        throw new UnsupportedOperationException("MpkParser 已过时，请使用 MpkFile.fromFile() 或直接处理 ZIP 流。");
    }
    
    // 保留旧的内部实现或将其移除，这里我们选择移除以减少混淆
    /*
    // 魔数常量
    private static final String MAGIC = "MPK1";
    // 文件头大小（字节）
    private static final int HEADER_SIZE = 24;
    // ... 旧的字段和方法 ...
    */
    
    /**
     * @deprecated MPK 验证现在应直接通过 {@link MpkFile} 的构造函数或单独的验证逻辑进行。
     */
    @Deprecated
    public static boolean validate(File file) {
         throw new UnsupportedOperationException("MpkParser.validate 已过时。请使用 MpkFile.fromFile() 进行解析和验证。");
    }

    /**
     * @deprecated 获取 MPK 信息现在应通过成功解析的 {@link MpkFile} 对象进行。
     */
    @Deprecated
    public static MpkInfo getInfo(File file) throws IOException, MpkException {
        throw new UnsupportedOperationException("MpkParser.getInfo 已过时。请使用 MpkFile.fromFile() 解析后获取信息。");
    }
    
     /**
     * @deprecated MpkInfo 类也可能需要更新或废弃，取决于其在新格式下的用途。
     */
    @Deprecated
    public static class MpkInfo { 
        // ... MpkInfo 的旧实现 ... 
         // 注意：MpkInfo 的字段可能与新的 manifest.json 结构不完全匹配
    }
} 