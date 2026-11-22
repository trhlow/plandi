package com.plandi.account;

import com.plandi.shared.Area;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record RegistrationRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 8) String password,
        @NotBlank @Size(max = 50) String displayName,
        @NotNull Area area,
        @AssertTrue Boolean adultConfirmed) {

    // Trim trước khi validate: bàn phím điện thoại hay tự thêm dấu cách sau email.
    public RegistrationRequest {
        email = email == null ? null : email.strip();
    }

    // bcrypt chỉ dùng 72 byte đầu và Spring Security từ chối mật khẩu dài hơn, nên giới hạn theo byte chứ không theo ký
    // tự.
    @AssertTrue
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @AssertTrue
    public boolean isPasswordDifferentFromEmail() {
        return password == null || email == null || !password.strip().equalsIgnoreCase(email.strip());
    }
}
