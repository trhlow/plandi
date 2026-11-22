package com.plandi.account.internal;

import com.plandi.account.AccountService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Sai 5 lần trong 15 phút (theo email + IP) thì khóa 15 phút tính từ lần sai thứ 5. Đếm trên Redis để vẫn đúng khi app
 * khởi động lại hoặc chạy nhiều instance. Khóa Redis là hash, không chứa email.
 */
@Component
class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    LoginThrottle(StringRedisTemplate redis) {
        this.redis = redis;
    }

    boolean isLocked(String email, String ip) {
        String failures = redis.opsForValue().get(key(email, ip));
        return failures != null && Integer.parseInt(failures) >= MAX_FAILURES;
    }

    void recordFailure(String email, String ip) {
        String key = key(email, ip);
        Long failures = redis.opsForValue().increment(key);
        // Lần sai đầu mở cửa sổ 15 phút; lần thứ 5 đặt lại hạn để khóa đủ 15 phút từ lúc bị khóa.
        if (failures != null && (failures == 1 || failures >= MAX_FAILURES)) {
            redis.expire(key, WINDOW);
        }
    }

    void reset(String email, String ip) {
        redis.delete(key(email, ip));
    }

    private static String key(String email, String ip) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((AccountService.normalizeEmail(email) + "|" + ip).getBytes(StandardCharsets.UTF_8));
            return "login-failures:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
