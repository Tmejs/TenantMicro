package com.tmejs.tenantmicro.admin;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "memberships")
public class Membership {
    @EmbeddedId
    private MembershipId id;

    protected Membership() { }

    public Membership(MembershipId id) { this.id = id; }
    public MembershipId getId() { return id; }
}
