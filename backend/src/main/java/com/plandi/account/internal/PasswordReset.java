package com.plandi.account.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "password_reset")
class PasswordReset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private long accountId;

    // Chỉ lưu hash (bcrypt) của mã; mã gốc chỉ hiện một lần cho admin.
    @Column(name = "code_hash")
    private String codeHash;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordReset() {}

    PasswordReset(long accountId, String codeHash, Instant expiresAt) {
        this.accountId = accountId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    String codeHash() {
        return codeHash;
    }

    void expire(Instant now) {
        this.expiresAt = now;
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }
}
