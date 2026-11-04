package com.tmejs.tenantmicro.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class BoardIT extends IntegrationEnvironment {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void boardHasThreeColumnsAndOnlyCurrentTenantIssues() throws Exception {
        String tenant = slug();
        String other = slug();
        String admin = adminToken();
        String alice = userToken();
        String subject = json.readTree(get("/api/whoami", alice).body()).get("sub").asText();
        for (String id : new String[] { tenant, other }) {
            assertThat(request("POST", "/api/admin/tenants", admin,
                    "{\"id\":\"" + id + "\",\"name\":\"Example\"}").statusCode()).isEqualTo(201);
            assertThat(request("PUT", "/api/admin/tenants/" + id + "/members/" + subject, admin, null).statusCode()).isEqualTo(204);
        }
        String project = project(tenant, alice);
        String foreignProject = project(other, alice);
        String issuePath = "/api/tenants/" + tenant + "/projects/" + project + "/issues";
        String todo = issue(issuePath, alice, "Todo task");
        String progress = issue(issuePath, alice, "Active task");
        String done = issue(issuePath, alice, "Finished task");
        String foreign = issue("/api/tenants/" + other + "/projects/" + foreignProject + "/issues", alice, "Foreign task");
        assertThat(request("PATCH", issuePath + "/" + progress + "/status", alice,
                "{\"status\":\"IN_PROGRESS\"}").statusCode()).isEqualTo(200);
        assertThat(request("PATCH", issuePath + "/" + done + "/status", alice,
                "{\"status\":\"DONE\"}").statusCode()).isEqualTo(200);

        var board = json.readTree(get("/api/tenants/" + tenant + "/projects/" + project + "/board", alice).body());
        assertThat(board.size()).isEqualTo(3);
        assertThat(board.get("TODO").size()).isEqualTo(1);
        assertThat(board.get("IN_PROGRESS").size()).isEqualTo(1);
        assertThat(board.get("DONE").size()).isEqualTo(1);
        assertThat(board.get("TODO").get(0).get("id").asText()).isEqualTo(todo);
        assertThat(board.get("IN_PROGRESS").get(0).get("id").asText()).isEqualTo(progress);
        assertThat(board.get("DONE").get(0).get("id").asText()).isEqualTo(done);
        assertThat(board.toString()).doesNotContain(foreign);
    }

    private String project(String tenant, String token) throws Exception {
        var response = request("POST", "/api/tenants/" + tenant + "/projects", token, "{\"name\":\"Project\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asText();
    }

    private String issue(String path, String token, String title) throws Exception {
        var response = request("POST", path, token, "{\"title\":\"" + title + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asText();
    }

    private String slug() { return "tenant-" + UUID.randomUUID().toString().substring(0, 8); }
}
