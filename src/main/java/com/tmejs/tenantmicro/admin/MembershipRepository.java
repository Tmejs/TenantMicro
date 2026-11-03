package com.tmejs.tenantmicro.admin;

import org.springframework.data.jpa.repository.JpaRepository;

interface MembershipRepository extends JpaRepository<Membership, MembershipId> { }
