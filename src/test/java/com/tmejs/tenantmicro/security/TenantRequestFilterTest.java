package com.tmejs.tenantmicro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tmejs.tenantmicro.admin.TenantCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantRequestFilterTest {
    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exceptionClearsTenantScopeOnSameThread() throws Exception {
        assertThatThrownBy(VerifiedTenantContext::requireTenantId).isInstanceOf(IllegalStateException.class);
        TenantCatalog catalog = mock(TenantCatalog.class);
        when(catalog.tenantExists("acme")).thenAnswer(invocation -> {
            assertThat(VerifiedTenantContext.currentHibernateIdentifier()).isEqualTo(VerifiedTenantContext.PLATFORM);
            return true;
        });
        when(catalog.isMember("acme", "alice-subject")).thenAnswer(invocation -> {
            assertThat(VerifiedTenantContext.currentHibernateIdentifier()).isEqualTo(VerifiedTenantContext.PLATFORM);
            return true;
        });
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("alice-subject").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tenants/acme/context");
        request.setRequestURI("/api/tenants/acme/context");
        TenantRequestFilter filter = new TenantRequestFilter(catalog);

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(VerifiedTenantContext.requireTenantId()).isEqualTo("acme");
            throw new IllegalStateException("downstream failure");
        })).isInstanceOf(IllegalStateException.class).hasMessage("downstream failure");
        assertThatThrownBy(VerifiedTenantContext::requireTenantId).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(VerifiedTenantContext::currentHibernateIdentifier).isInstanceOf(IllegalStateException.class);
    }
}
