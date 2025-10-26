package com.company.billing.persistence;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class DefaultCryptoService implements CryptoService {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final CryptoProperties properties;
  private final SecureRandom secureRandom = new SecureRandom();
  private SecretKey secretKey;

  public DefaultCryptoService(CryptoProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void init() {
    byte[] decoded = Base64.getDecoder().decode(properties.getSecret());
    Assert.isTrue(decoded.length == 16 || decoded.length == 24 || decoded.length == 32,
        "billing.crypto.secret must decode to a valid AES key (16/24/32 bytes)");
    this.secretKey = new SecretKeySpec(decoded, ALGORITHM);
  }

  @Override
  public byte[] encrypt(byte[] plainData) {
    if (plainData == null || plainData.length == 0) {
      return plainData;
    }
    byte[] iv = new byte[IV_LENGTH];
    secureRandom.nextBytes(iv);
    byte[] cipherText = doCipher(Cipher.ENCRYPT_MODE, iv, plainData);

    ByteBuffer buffer = ByteBuffer.allocate(1 + iv.length + cipherText.length);
    buffer.put((byte) iv.length);
    buffer.put(iv);
    buffer.put(cipherText);
    return buffer.array();
  }

  @Override
  public byte[] decrypt(byte[] encryptedData) {
    if (encryptedData == null || encryptedData.length == 0) {
      return encryptedData;
    }
    ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
    if (buffer.remaining() < 1) {
      throw new IllegalArgumentException("Encrypted payload is malformed");
    }
    int ivLength = Byte.toUnsignedInt(buffer.get());
    if (ivLength <= 0 || ivLength > 32 || buffer.remaining() <= ivLength) {
      throw new IllegalArgumentException("Encrypted payload is malformed: invalid IV metadata");
    }
    byte[] iv = new byte[ivLength];
    buffer.get(iv);
    byte[] cipherBytes = new byte[buffer.remaining()];
    buffer.get(cipherBytes);
    return doCipher(Cipher.DECRYPT_MODE, iv, cipherBytes);
  }

  private byte[] doCipher(int mode, byte[] iv, byte[] input) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(mode, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return cipher.doFinal(input);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Unable to process crypto operation", ex);
    }
  }
}
