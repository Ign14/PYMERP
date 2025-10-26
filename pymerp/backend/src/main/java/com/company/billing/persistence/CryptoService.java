package com.company.billing.persistence;

public interface CryptoService {

  byte[] encrypt(byte[] plainData);

  byte[] decrypt(byte[] encryptedData);
}
