package com.tmejs.tenantmicro.admin;

import com.tmejs.tenantmicro.security.VerifiedTenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantCatalog {
    private final TenantRepository tenants;
    private final MembershipRepository memberships;

    TenantCatalog(TenantRepository tenants, MembershipRepository memberships) {
        this.tenants = tenants;
        this.memberships = memberships;
    }

    @Transactional
    public Tenant createTenant(String id, String name) {
        VerifiedTenantContext.requirePlatform();
        if (tenants.existsById(id)) throw new TenantAlreadyExistsException();
        return tenants.save(new Tenant(id, name));
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        VerifiedTenantContext.requirePlatform();
        return tenants.findAll();
    }

    @Transactional(readOnly = true)
    public boolean tenantExists(String id) {
        VerifiedTenantContext.requirePlatform();
        return tenants.existsById(id);
    }

    @Transactional
    public void grant(String tenantId, String subject) {
        VerifiedTenantContext.requirePlatform();
        requireExisting(tenantId);
        MembershipId id = new MembershipId(tenantId, subject);
        if (!memberships.existsById(id)) memberships.save(new Membership(id));
    }

    @Transactional
    public void revoke(String tenantId, String subject) {
        VerifiedTenantContext.requirePlatform();
        requireExisting(tenantId);
        memberships.deleteById(new MembershipId(tenantId, subject));
    }

    @Transactional(readOnly = true)
    public boolean isMember(String tenantId, String subject) {
        VerifiedTenantContext.requirePlatform();
        return memberships.existsById(new MembershipId(tenantId, subject));
    }

    private void requireExisting(String tenantId) {
        if (!tenants.existsById(tenantId)) throw new TenantNotFoundException();
    }
}
