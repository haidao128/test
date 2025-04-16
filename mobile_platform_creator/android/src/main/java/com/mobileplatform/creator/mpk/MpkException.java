package com.mobileplatform.creator.mpk;

/**
 * MPK 文件解析异常
 */
public class MpkException extends Exception {
    
    /**
     * 创建一个新的 MPK 异常
     * @param message 异常消息
     */
    public MpkException(String message) {
        super(message);
    }
    
    /**
     * 创建一个新的 MPK 异常
     * @param message 异常消息
     * @param cause 原因异常
     */
    public MpkException(String message, Throwable cause) {
        super(message, cause);
    }
} 