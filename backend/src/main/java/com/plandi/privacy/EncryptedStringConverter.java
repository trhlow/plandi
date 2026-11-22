package com.plandi.privacy;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Mã hóa cột nhạy cảm bằng AES-256-GCM: mỗi giá trị một IV 12 byte ngẫu nhiên, lưu dạng {@code v1:} + base64(iv‖ciphertext).
 * Dùng với {@code @Convert(converter = EncryptedStringConverter.class)}; Hibernate lấy converter qua Spring nên khóa được
 * tiêm vào. Cột mã hóa không tìm kiếm hay SUM được trong SQL (spec/SPEC-privacy.md).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "v1:"; // đổi khóa hoặc thuật toán sau này thì thêm v2
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptedStringConverter(SecretKey key) {
        this.key = key;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] stored = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            return PREFIX + Base64.getEncoder().encodeToString(stored);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("encrypt failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(PREFIX)) {
            throw new IllegalStateException("unknown encryption format");
        }
        try {
            byte[] raw = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES));
            byte[] plaintext = cipher.doFinal(raw, IV_BYTES, raw.length - IV_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // GCM kiểm tra toàn vẹn: dữ liệu bị sửa hoặc sai khóa đều rơi vào đây, không trả về chuỗi rác.
            throw new IllegalStateException("decrypt failed", e);
        }
    }
}
