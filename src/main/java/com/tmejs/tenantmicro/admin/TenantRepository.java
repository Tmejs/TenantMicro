package com.tmejs.tenantmicro.admin;

import org.springframework.data.jpa.repository.JpaRepository;

interface TenantRepository extends JpaRepository<Tenant, String> { }
