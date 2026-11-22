package com.plandi.account.internal;

import com.plandi.account.AccountExport;
import com.plandi.account.AccountView;
import com.plandi.shared.Area;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    private Area area;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    // DB đặt now() khi tạo; app chỉ ghi khi đổi hoặc đặt lại mật khẩu.
    @Column(name = "password_changed_at", insertable = false)
    private Instant passwordChangedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Account() {}

    public Account(String email, String passwordHash, String displayName, Area area) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.area = area;
    }

    public long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public void changePassword(String newPasswordHash, Instant changedAt) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = changedAt;
    }

    public void updateProfile(String displayName, Area area) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (area != null) {
            this.area = area;
        }
    }

    Role role() {
        return role;
    }

    public AccountExport export() {
        return new AccountExport(email, displayName, area, role.name(), createdAt);
    }

    public AccountView view() {
        return new AccountView(id, email, displayName, area, role.name());
    }
}
