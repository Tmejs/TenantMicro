package com.tmejs.tenantmicro.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

class IssueIsolationIT extends IntegrationEnvironment {
    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void foreignProjectsAndIssuesCannotBeUsedAcrossTenants() throws Exception {
        String acme = slug();
        String beta = slug();
        String admin = adminToken();
        String alice = userToken();
        String bob = userToken("bob");
        createTenant(admin, acme);
        createTenant(admin, beta);
        String subject = subject(alice);
        grant(admin, acme, subject);
        grant(admin, beta, subject);
        String acmeProject = createProject(acme, alice, "Acme");
        String secondAcmeProject = createProject(acme, alice, "Other Acme");
        String betaProject = createProject(beta, alice, "Beta");
        String acmePath = "/api/tenants/" + acme + "/projects/" + acmeProject + "/issues";
        String secondAcmePath = "/api/tenants/" + acme + "/projects/" + secondAcmeProject + "/issues";
        String betaPath = "/api/tenants/" + beta + "/projects/" + betaProject + "/issues";
        var acmeCreated = request("POST", acmePath, alice, "{\"title\":\"Acme task\"}");
        assertThat(acmeCreated.statusCode()).isEqualTo(201);
        String acmeIssue = json.readTree(acmeCreated.body()).get("id").asText();
        var created = request("POST", betaPath, alice, "{\"title\":\"Secret\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        String betaIssue = json.readTree(created.body()).get("id").asText();

        assertThat(request("POST", "/api/tenants/" + acme + "/projects/" + betaProject + "/issues", alice,
                "{\"title\":\"Cross tenant\"}").statusCode()).isEqualTo(404);
        assertThat(get("/api/tenants/" + acme + "/projects/" + betaProject + "/issues", alice).statusCode()).isEqualTo(404);
        assertThat(get(acmePath + "/" + betaIssue, alice).statusCode()).isEqualTo(404);
        assertThat(request("PATCH", acmePath + "/" + betaIssue + "/status", alice,
                "{\"status\":\"DONE\"}").statusCode()).isEqualTo(404);
        assertThat(get(secondAcmePath + "/" + acmeIssue, alice).statusCode()).isEqualTo(404);
        assertThat(request("PATCH", secondAcmePath + "/" + acmeIssue + "/status", alice,
                "{\"status\":\"DONE\"}").statusCode()).isEqualTo(404);
        assertThat(json.readTree(get(acmePath + "/" + acmeIssue, alice).body()).get("status").asText()).isEqualTo("TODO");
        assertThat(json.readTree(get(betaPath + "/" + betaIssue, alice).body()).get("status").asText()).isEqualTo("TODO");
        assertThat(get(acmePath, bob).statusCode()).isEqualTo(403);
        assertThat(request("POST", acmePath, alice, "{\"title\":\" \"}").statusCode()).isEqualTo(400);
        assertThat(request("PATCH", betaPath + "/" + betaIssue + "/status", alice,
                "{\"status\":\"INVALID\"}").statusCode()).isEqualTo(400);
        assertThat(get(acmePath, alice).body()).doesNotContain(betaIssue);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO issues(id,tenant_id,project_id,title,status) VALUES (?,?,?,?,?)",
                UUID.randomUUID(), acme, UUID.fromString(betaProject), "Invalid link", "TODO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void createTenant(String admin, String slug) throws Exception {
        assertThat(request("POST", "/api/admin/tenants", admin,
                "{\"id\":\"" + slug + "\",\"name\":\"Example\"}").statusCode()).isEqualTo(201);
    }

    private void grant(String admin, String tenant, String subject) throws Exception {
        assertThat(request("PUT", "/api/admin/tenants/" + tenant + "/members/" + subject, admin, null).statusCode()).isEqualTo(204);
    }

    private String createProject(String tenant, String token, String name) throws Exception {
        var response = request("POST", "/api/tenants/" + tenant + "/projects", token, "{\"name\":\"" + name + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asText();
    }

    private String subject(String token) throws Exception {
        return json.readTree(get("/api/whoami", token).body()).get("sub").asText();
    }

    private String slug() { return "tenant-" + UUID.randomUUID().toString().substring(0, 8); }
}
