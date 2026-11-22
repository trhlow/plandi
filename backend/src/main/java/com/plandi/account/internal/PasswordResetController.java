package com.plandi.account.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PasswordResetController {

    private final PasswordResetService resets;

    PasswordResetController(PasswordResetService resets) {
        this.resets = resets;
    }

    record IssueRequest(String email) {}

    record ResetRequest(String email, String code, String newPassword) {}

    // Chỉ ADMIN: SecurityConfig chặn /api/v1/admin/** theo vai trò.
    @PostMapping("/api/v1/admin/password-resets")
    ResponseEntity<PasswordResetService.IssuedCode> issue(@RequestBody IssueRequest body) {
        if (body.email() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(resets.issue(body.email()));
    }

    @PostMapping("/api/v1/auth/reset-password")
    ResponseEntity<Void> reset(@RequestBody ResetRequest body, HttpServletRequest request) {
        resets.reset(body.email(), body.code(), body.newPassword(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
