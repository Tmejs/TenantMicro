package com.tmejs.tenantmicro.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import com.tmejs.tenantmicro.project.ProjectRepository;
import com.tmejs.tenantmicro.security.VerifiedTenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

class ProjectIsolationIT extends IntegrationEnvironment {
    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    ProjectRepository projects;

    @Test
    void projectsAreVisibleOnlyWithinVerifiedTenant() throws Exception {
        String acme = slug();
        String beta = slug();
        String admin = adminToken();
        String alice = userToken();
        String bob = userToken("bob");
        createTenant(admin, acme);
        createTenant(admin, beta);
        grant(admin, acme, subject(alice));
        grant(admin, beta, subject(bob));

        var acmeProject = request("POST", "/api/tenants/" + acme + "/projects", alice, "{\"name\":\"Delivery\"}");
        assertThat(acmeProject.statusCode()).isEqualTo(201);
        String acmeId = json.readTree(acmeProject.body()).get("id").asText();
        var betaProject = request("POST", "/api/tenants/" + beta + "/projects", bob, "{\"name\":\"Private\"}");
        assertThat(betaProject.statusCode()).isEqualTo(201);
        String betaId = json.readTree(betaProject.body()).get("id").asText();

        assertThat(get("/api/tenants/" + acme + "/projects", alice).body()).contains(acmeId).doesNotContain(betaId);
        assertThat(get("/api/tenants/" + beta + "/projects", bob).body()).contains(betaId).doesNotContain(acmeId);
        assertThat(get("/api/tenants/" + acme + "/projects/" + betaId, alice).statusCode()).isEqualTo(404);
        try (var platform = VerifiedTenantContext.platformScope()) {
            assertThat(projects.findAll()).isEmpty();
            assertThat(projects.findById(UUID.fromString(acmeId))).isEmpty();
        }
        assertThatThrownBy(() -> projects.findAll()).hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(get("/api/tenants/" + acme + "/projects", bob).statusCode()).isEqualTo(403);
        assertThat(request("POST", "/api/tenants/" + acme + "/projects", alice, "{\"name\":\" \"}").statusCode()).isEqualTo(400);
    }

    private void createTenant(String admin, String slug) throws Exception {
        assertThat(request("POST", "/api/admin/tenants", admin,
                "{\"id\":\"" + slug + "\",\"name\":\"Example\"}").statusCode()).isEqualTo(201);
    }

    private void grant(String admin, String tenant, String subject) throws Exception {
        assertThat(request("PUT", "/api/admin/tenants/" + tenant + "/members/" + subject, admin, null).statusCode()).isEqualTo(204);
    }

    private String subject(String token) throws Exception {
        return json.readTree(get("/api/whoami", token).body()).get("sub").asText();
    }

    private String slug() {
        return "tenant-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
