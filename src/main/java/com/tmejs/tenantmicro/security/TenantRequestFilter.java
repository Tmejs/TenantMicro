package com.tmejs.tenantmicro.security;

import com.tmejs.tenantmicro.admin.TenantCatalog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantRequestFilter extends OncePerRequestFilter {
    private static final Pattern TENANT_PATH = Pattern.compile("^/api/tenants/([^/]+)(?:/.*)?$");
    private static final Pattern SLUG = Pattern.compile("[a-z][a-z0-9-]{2,62}");
    private final TenantCatalog catalog;

    public TenantRequestFilter(TenantCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/admin/")) {
            try (var scope = VerifiedTenantContext.platformScope()) {
                chain.doFilter(request, response);
            }
            return;
        }

        Matcher match = TENANT_PATH.matcher(path);
        if (!match.matches()) {
            chain.doFilter(request, response);
            return;
        }
        String tenantId = match.group(1);
        if (!SLUG.matcher(tenantId).matches() || VerifiedTenantContext.PLATFORM.equals(tenantId)) {
            error(response, 400, "invalid_tenant", "Invalid tenant ID");
            return;
        }

        boolean exists;
        boolean member;
        try (var scope = VerifiedTenantContext.platformScope()) {
            exists = catalog.tenantExists(tenantId);
            member = exists && catalog.isMember(tenantId, jwt.getToken().getSubject());
        }
        if (!exists) {
            error(response, 404, "tenant_not_found", "Tenant not found");
            return;
        }
        if (!member) {
            error(response, 403, "membership_required", "Tenant membership required");
            return;
        }
        try (var scope = VerifiedTenantContext.tenantScope(tenantId)) {
            chain.doFilter(request, response);
        }
    }

    private static void error(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
