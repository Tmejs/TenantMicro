package com.tmejs.tenantmicro.security;

public final class VerifiedTenantContext {
    public static final String PLATFORM = "__platform__";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private VerifiedTenantContext() { }

    public static Scope platformScope() { return open(PLATFORM); }
    public static Scope tenantScope(String tenantId) {
        if (tenantId == null || PLATFORM.equals(tenantId)) throw new IllegalArgumentException("Invalid tenant scope");
        return open(tenantId);
    }

    private static Scope open(String identifier) {
        if (CURRENT.get() != null) throw new IllegalStateException("Tenant scope is already active");
        CURRENT.set(identifier);
        return new Scope();
    }

    public static String requireTenantId() {
        String value = CURRENT.get();
        if (value == null || PLATFORM.equals(value)) throw new IllegalStateException("Verified tenant scope required");
        return value;
    }

    public static void requirePlatform() {
        if (!PLATFORM.equals(CURRENT.get())) throw new IllegalStateException("Platform scope required");
    }

    public static String currentHibernateIdentifier() {
        String value = CURRENT.get();
        if (value == null) throw new IllegalStateException("Tenant scope is absent");
        return value;
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;

        private Scope() { }

        @Override
        public void close() {
            if (!closed) {
                CURRENT.remove();
                closed = true;
            }
        }
    }
}
