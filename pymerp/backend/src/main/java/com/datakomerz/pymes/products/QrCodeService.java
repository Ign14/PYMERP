package com.datakomerz.pymes.products;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {
  private static final int DEFAULT_SIZE = 256;

  public GeneratedQr generate(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Value required to generate QR code");
    }
    try {
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix matrix = writer.encode(value, BarcodeFormat.QR_CODE, DEFAULT_SIZE, DEFAULT_SIZE);
      try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);
        return new GeneratedQr(output.toByteArray(), "png", "image/png");
      }
    } catch (WriterException | IOException e) {
      throw new IllegalStateException("Failed to generate QR code", e);
    }
  }

  public record GeneratedQr(byte[] content, String extension, String contentType) {}
}
