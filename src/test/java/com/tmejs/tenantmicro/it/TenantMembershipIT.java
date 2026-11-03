package com.tmejs.tenantmicro.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

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
        assertThat(get(path, bob).statusCode()).isEqualTo(403);
        assertThat(request("DELETE", "/api/admin/tenants/" + tenant + "/members/" + aliceSubject, admin, null).statusCode()).isEqualTo(204);
        assertThat(get(path, alice).statusCode()).isEqualTo(403);
        assertThat(get("/api/tenants/missing-" + tenant + "/context", alice).statusCode()).isEqualTo(404);
    }
}
