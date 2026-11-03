package com.tmejs.tenantmicro.admin;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MembershipId implements Serializable {
    @Column(name = "tenant_id", nullable = false, length = 63)
    private String tenantId;

    @Column(nullable = false, length = 255)
    private String subject;

    protected MembershipId() { }

    public MembershipId(String tenantId, String subject) {
        this.tenantId = tenantId;
        this.subject = subject;
    }

    public String getTenantId() { return tenantId; }
    public String getSubject() { return subject; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MembershipId that)) return false;
        return Objects.equals(tenantId, that.tenantId) && Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() { return Objects.hash(tenantId, subject); }
}
