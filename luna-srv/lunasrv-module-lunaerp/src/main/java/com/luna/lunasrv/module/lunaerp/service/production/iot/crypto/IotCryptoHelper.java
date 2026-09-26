package com.luna.lunasrv.module.lunaerp.service.production.iot.crypto;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * IoT 凭证密码 AES 加解密组件
 *
 * 复用项目已有加密秘钥（mybatis-plus.encryptor.password，默认 fallback 保证单测轻量运行），
 * 密码写入即加密，禁止明文回显（ADR-0028）。
 *
 * @author luna
 */
@Component
public class IotCryptoHelper {

    private static final String DEFAULT_KEY = "XDV71a+xqStEA3WH";

    @Value("${mybatis-plus.encryptor.password:XDV71a+xqStEA3WH}")
    private String passwordKey = DEFAULT_KEY;

    private AES aes;

    public IotCryptoHelper() {
        this.aes = SecureUtil.aes(normalizeKey(DEFAULT_KEY));
    }

    public IotCryptoHelper(String key) {
        String finalKey = StringUtils.hasText(key) ? key : DEFAULT_KEY;
        this.passwordKey = finalKey;
        this.aes = SecureUtil.aes(normalizeKey(finalKey));
    }

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(passwordKey)) {
            this.aes = SecureUtil.aes(normalizeKey(passwordKey));
        }
    }

    private byte[] normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return DEFAULT_KEY.getBytes(StandardCharsets.UTF_8);
        }
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 16 || bytes.length == 24 || bytes.length == 32) {
            return bytes;
        }
        byte[] key16 = new byte[16];
        System.arraycopy(bytes, 0, key16, 0, Math.min(bytes.length, 16));
        for (int i = bytes.length; i < 16; i++) {
            key16[i] = (byte) '#';
        }
        return key16;
    }

    /**
     * 加密明文字符串
     */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        return aes.encryptBase64(plainText);
    }

    /**
     * 解密密文字符串
     */
    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return cipherText;
        }
        return aes.decryptStr(cipherText);
    }

}
