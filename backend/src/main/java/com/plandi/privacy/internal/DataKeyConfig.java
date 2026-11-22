package com.plandi.privacy.internal;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Khóa AES-256 cho cột nhạy cảm, lấy từ {@code PLANDI_DATA_KEY} (32 byte, base64; tạo bằng {@code openssl rand -base64 32}).
 * Profile prod thiếu khóa thì app không khởi động, để không bao giờ ghi dữ liệu thật bằng khóa tạm.
 */
@Configuration
class DataKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(DataKeyConfig.class);

    @Bean
    SecretKey plandiDataKey(@Value("${plandi.data-key:${PLANDI_DATA_KEY:}}") String encoded, Environment environment) {
        if (encoded.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("PLANDI_DATA_KEY is required in the prod profile");
            }
            // Máy dev và test: khóa tạm, mất khi khởi động lại. Profile local đặt khóa cố định trong application-local.
            log.warn("PLANDI_DATA_KEY not set, using a throwaway key");
            byte[] throwaway = new byte[32];
            new SecureRandom().nextBytes(throwaway);
            return new SecretKeySpec(throwaway, "AES");
        }
        byte[] key = Base64.getDecoder().decode(encoded.strip());
        if (key.length != 32) {
            throw new IllegalStateException("PLANDI_DATA_KEY must be 32 bytes (base64), got " + key.length);
        }
        return new SecretKeySpec(key, "AES");
    }
}
