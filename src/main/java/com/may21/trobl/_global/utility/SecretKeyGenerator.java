package com.may21.trobl._global.utility;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class SecretKeyGenerator {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64]; // 64 바이트 = 512 비트
        random.nextBytes(bytes);
        String secretKey = Base64.getEncoder().encodeToString(bytes);
        log.info("Generated Secret Key: {}", secretKey);
    }
}
