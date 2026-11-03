package com.tmejs.tenantmicro.security;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TenantContextController {
    @GetMapping("/api/tenants/{tenantId}/context")
    Map<String, String> context(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("tenantId", VerifiedTenantContext.requireTenantId(), "sub", jwt.getSubject());
    }
}
