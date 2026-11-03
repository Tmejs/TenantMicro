package com.tmejs.tenantmicro.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public final class RealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realm)) {
            return List.of();
        }
        Object roles = realm.get("roles");
        if (!(roles instanceof Collection<?> names)) {
            return List.of();
        }
        return names.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + name))
                .toList();
    }
}
