package com.tmejs.tenantmicro.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AdminTenantIT extends IntegrationEnvironment {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void adminCreatesListsGrantsAndRevokes() throws Exception {
        String admin = adminToken();
        String tenant = slug();
        String subject = subject(userToken());
        var created = request("POST", "/api/admin/tenants", admin, "{\"id\":\"" + tenant + "\",\"name\":\"Example\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(get("/api/admin/tenants", admin).body()).contains(tenant);
        assertThat(request("PUT", "/api/admin/tenants/" + tenant + "/members/" + subject, admin, null).statusCode()).isEqualTo(204);
        assertThat(request("DELETE", "/api/admin/tenants/" + tenant + "/members/" + subject, admin, null).statusCode()).isEqualTo(204);
    }

    @Test
    void duplicateUnknownAndInvalidInputsHaveExpectedStatuses() throws Exception {
        String admin = adminToken();
        String tenant = slug();
        String body = "{\"id\":\"" + tenant + "\",\"name\":\"Example\"}";
        assertThat(request("POST", "/api/admin/tenants", admin, body).statusCode()).isEqualTo(201);
        assertThat(request("POST", "/api/admin/tenants", admin, body).statusCode()).isEqualTo(409);
        assertThat(request("POST", "/api/admin/tenants", admin, "{\"id\":\"BAD ID\",\"name\":\" \"}").statusCode()).isEqualTo(400);
        assertThat(request("PUT", "/api/admin/tenants/missing-" + tenant + "/members/someone", admin, null).statusCode()).isEqualTo(404);
    }

    @Test
    void regularUserCannotMutateAnyAdminEndpoint() throws Exception {
        String user = userToken();
        String tenant = slug();
        assertThat(request("POST", "/api/admin/tenants", user, "{\"id\":\"" + tenant + "\",\"name\":\"Example\"}").statusCode()).isEqualTo(403);
        assertThat(request("PUT", "/api/admin/tenants/" + tenant + "/members/someone", user, null).statusCode()).isEqualTo(403);
        assertThat(request("DELETE", "/api/admin/tenants/" + tenant + "/members/someone", user, null).statusCode()).isEqualTo(403);
    }

    private String subject(String token) throws Exception {
        return json.readTree(get("/api/whoami", token).body()).get("sub").asText();
    }

    private String slug() {
        return "tenant-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
