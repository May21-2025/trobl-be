package com.may21.trobl._global.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class AesEncryptionUtil {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

  @Value("${security.encryption.aes-key}")
  private String secretKey;

  @Value("${security.encryption.aes-iv}")
  private String iv;

  public static String generateSecretKey(int keyLength) throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(keyLength * 8);
    SecretKey key = keyGen.generateKey();
    return Base64.encodeBase64String(key.getEncoded());
  }

  // 안전한 랜덤 IV 생성
  public static String generateIv() {
    SecureRandom random = new SecureRandom();
    byte[] iv = new byte[16];
    random.nextBytes(iv);
    return Base64.encodeBase64String(iv);
  }

  /** AES256으로 암호화하는 메서드 */
  public String encrypt(String text) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      SecretKeySpec keySpec = getKeySpec();
      IvParameterSpec ivSpec = getIvSpec();
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

      byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
      return Base64.encodeBase64String(encrypted);
    } catch (Exception e) {
      throw new RuntimeException("암호화 과정에서 오류가 발생했습니다.", e);
    }
  }

  /** AES256으로 복호화하는 메서드 */
  public String decrypt(String encryptedText) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      SecretKeySpec keySpec = getKeySpec();
      IvParameterSpec ivSpec = getIvSpec();
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

      byte[] decodedBytes = Base64.decodeBase64(encryptedText);
      byte[] decrypted = cipher.doFinal(decodedBytes);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("복호화 과정에서 오류가 발생했습니다.", e);
    }
  }

  private SecretKeySpec getKeySpec() {
    byte[] decodedKey = Base64.decodeBase64(secretKey);
    return new SecretKeySpec(decodedKey, ALGORITHM);
  }

  private IvParameterSpec getIvSpec() {
    byte[] decodedIv = Base64.decodeBase64(iv);
    return new IvParameterSpec(decodedIv);
  }
}
