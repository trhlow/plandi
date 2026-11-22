package com.plandi.account.internal;

import com.plandi.account.AccountView;
import com.plandi.shared.Area;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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

    String passwordHash() {
        return passwordHash;
    }

    Role role() {
        return role;
    }

    public AccountView view() {
        return new AccountView(id, email, displayName, area, role.name());
    }
}
