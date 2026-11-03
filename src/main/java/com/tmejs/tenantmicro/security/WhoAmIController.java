package com.tmejs.tenantmicro.security;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class WhoAmIController {
    @GetMapping("/api/whoami")
    Map<String, String> whoAmI(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("sub", jwt.getSubject());
    }
}
