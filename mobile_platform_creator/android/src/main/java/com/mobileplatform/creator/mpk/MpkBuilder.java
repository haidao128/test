package com.mobileplatform.creator.mpk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * MPK 文件构建器
 * 用于创建 .mpk 文件，包含元数据、代码和资源
 */
public class MpkBuilder {
    private static final String TAG = "MpkBuilder";
    
    // 魔数常量
    private static final String MAGIC = "MPK1";
    
    // 文件头大小（字节）
    private static final int HEADER_SIZE = 24;
    
    // 清单文件
    private JSONObject manifest;
    
    // 代码数据
    private String codeType;
    private String entryPoint;
    private byte[] codeData;
    
    // 资源数据
    private byte[] resourcesData;
    
    // 签名数据
    private byte[] signatureData;
    private byte[] certificateData;
    
    // 文件列表
    private List<String> fileList;
    
    /**
     * 创建 MPK 构建器
     */
    public MpkBuilder() {
        manifest = new JSONObject();
        fileList = new ArrayList<>();
    }
    
    /**
     * 设置清单文件
     * @param manifest 清单文件 JSON 对象
     * @return 构建器
     */
    public MpkBuilder setManifest(JSONObject manifest) {
        this.manifest = manifest;
        return this;
    }
    
    /**
     * 设置代码数据
     * @param codeType 代码类型
     * @param entryPoint 入口点
     * @param codeData 代码数据
     * @return 构建器
     */
    public MpkBuilder setCode(String codeType, String entryPoint, byte[] codeData) {
        this.codeType = codeType;
        this.entryPoint = entryPoint;
        this.codeData = codeData;
        return this;
    }
    
    /**
     * 设置资源数据
     * @param resourcesData 资源数据
     * @return 构建器
     */
    public MpkBuilder setResources(byte[] resourcesData) {
        this.resourcesData = resourcesData;
        return this;
    }
    
    /**
     * 设置签名数据
     * @param signatureData 签名数据
     * @param certificateData 证书数据
     * @return 构建器
     */
    public MpkBuilder setSignature(byte[] signatureData, byte[] certificateData) {
        this.signatureData = signatureData;
        this.certificateData = certificateData;
        return this;
    }
    
    /**
     * 添加文件
     * @param path 文件路径
     * @return 构建器
     */
    public MpkBuilder addFile(String path) {
        fileList.add(path);
        return this;
    }
    
    /**
     * 构建 MPK 文件
     * @param outputFile 输出文件
     * @throws IOException 如果文件写入失败
     * @throws MpkException 如果构建失败
     */
    public void build(File outputFile) throws IOException, MpkException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            // 写入清单文件
            ZipEntry manifestEntry = new ZipEntry("manifest.json");
            zos.putNextEntry(manifestEntry);
            zos.write(manifest.toString().getBytes());
            zos.closeEntry();
            
            // 写入代码数据
            if (codeData != null) {
                ZipEntry codeEntry = new ZipEntry("code/" + entryPoint);
                zos.putNextEntry(codeEntry);
                zos.write(codeData);
                zos.closeEntry();
            }
            
            // 写入资源数据
            if (resourcesData != null) {
                ZipEntry resourcesEntry = new ZipEntry("assets/resources.zip");
                zos.putNextEntry(resourcesEntry);
                zos.write(resourcesData);
                zos.closeEntry();
            }
            
            // 写入签名数据
            if (signatureData != null) {
                ZipEntry signatureEntry = new ZipEntry("signature.sig");
                zos.putNextEntry(signatureEntry);
                zos.write(signatureData);
                zos.closeEntry();
            }
            
            // 写入证书数据
            if (certificateData != null) {
                ZipEntry certificateEntry = new ZipEntry("certificate.cer");
                zos.putNextEntry(certificateEntry);
                zos.write(certificateData);
                zos.closeEntry();
            }
            
            // 写入其他文件
            for (String path : fileList) {
                ZipEntry entry = new ZipEntry(path);
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(path)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new MpkException("构建 MPK 文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证构建参数
     * @throws MpkException 如果参数无效
     */
    private void validate() throws MpkException {
        // 验证清单文件
        String[] requiredFields = {
            "format_version", "id", "name", "version",
            "platform", "min_platform_version", "code_type", "entry_point"
        };
        
        for (String field : requiredFields) {
            if (!manifest.has(field)) {
                throw new MpkException("清单文件缺少必需字段: " + field);
            }
        }
        
        // 验证代码数据
        if (codeData == null) {
            throw new MpkException("缺少代码数据");
        }
        
        // 验证入口点
        if (entryPoint == null || entryPoint.isEmpty()) {
            throw new MpkException("缺少入口点");
        }
    }
} 