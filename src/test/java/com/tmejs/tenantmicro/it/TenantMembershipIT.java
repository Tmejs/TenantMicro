package com.tmejs.tenantmicro.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@Import(TenantMembershipIT.TransactionProbe.class)
class TenantMembershipIT extends IntegrationEnvironment {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void membershipIsCheckedForEveryRequestIncludingAfterRevoke() throws Exception {
        String tenant = "tenant-" + UUID.randomUUID().toString().substring(0, 8);
        String admin = adminToken();
        String alice = userToken();
        String bob = userToken("bob");
        String aliceSubject = json.readTree(get("/api/whoami", alice).body()).get("sub").asText();
        assertThat(request("POST", "/api/admin/tenants", admin, "{\"id\":\"" + tenant + "\",\"name\":\"Example\"}").statusCode()).isEqualTo(201);
        String path = "/api/tenants/" + tenant + "/context";
        assertThat(get(path, alice).statusCode()).isEqualTo(403);
        assertThat(request("PUT", "/api/admin/tenants/" + tenant + "/members/" + aliceSubject, admin, null).statusCode()).isEqualTo(204);
        var allowed = get(path, alice);
        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(json.readTree(allowed.body()).get("tenantId").asText()).isEqualTo(tenant);
        var handoff = get("/api/tenants/" + tenant + "/transaction-probe", alice);
        assertThat(handoff.statusCode()).isEqualTo(200);
        assertThat(json.readTree(handoff.body()).get("transactionActive").asBoolean()).isFalse();
        assertThat(json.readTree(handoff.body()).get("entityManagerBound").asBoolean()).isFalse();
        assertThat(get(path, bob).statusCode()).isEqualTo(403);
        assertThat(request("DELETE", "/api/admin/tenants/" + tenant + "/members/" + aliceSubject, admin, null).statusCode()).isEqualTo(204);
        assertThat(get(path, alice).statusCode()).isEqualTo(403);
        assertThat(get("/api/tenants/missing-" + tenant + "/context", alice).statusCode()).isEqualTo(404);
    }

    @RestController
    static class TransactionProbe {
        private final EntityManagerFactory entityManagerFactory;

        TransactionProbe(EntityManagerFactory entityManagerFactory) {
            this.entityManagerFactory = entityManagerFactory;
        }

        @GetMapping("/api/tenants/{tenantId}/transaction-probe")
        Map<String, Boolean> inspect() {
            return Map.of(
                    "transactionActive", TransactionSynchronizationManager.isActualTransactionActive(),
                    "entityManagerBound", TransactionSynchronizationManager.hasResource(entityManagerFactory));
        }
    }
}
