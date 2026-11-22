package com.plandi.privacy.internal;

import com.plandi.privacy.Purpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "consent_event")
public class ConsentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private long accountId;

    @Enumerated(EnumType.STRING)
    private Purpose purpose;

    private boolean granted;

    @Column(name = "policy_version")
    private String policyVersion;

    protected ConsentEvent() {}

    public ConsentEvent(long accountId, Purpose purpose, boolean granted, String policyVersion) {
        this.accountId = accountId;
        this.purpose = purpose;
        this.granted = granted;
        this.policyVersion = policyVersion;
    }

    public boolean granted() {
        return granted;
    }

    public String policyVersion() {
        return policyVersion;
    }
}
