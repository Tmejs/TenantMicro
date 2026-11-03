package com.tmejs.tenantmicro.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JwtSecurityIT extends IntegrationEnvironment {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void validUserTokenIdentifiesSubject() throws Exception {
        String token = userToken();
        var response = get("/api/whoami", token);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).get("sub").asText()).isEqualTo(claims(token).get("sub").asText());
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        assertThat(get("/api/whoami", null).statusCode()).isEqualTo(401);
    }

    @Test
    void wrongAudienceIsUnauthorized() throws Exception {
        assertThat(get("/api/whoami", token("alice", "wrong-audience-test")).statusCode()).isEqualTo(401);
    }

    @Test
    void invalidSignatureIsUnauthorized() throws Exception {
        String token = userToken();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String altered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        assertThat(get("/api/whoami", altered).statusCode()).isEqualTo(401);
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        String token = userToken();
        long expiresAt = claims(token).get("exp").asLong();
        long waitMillis = Math.max(0, (expiresAt + 2) * 1000 - Instant.now().toEpochMilli());
        Thread.sleep(waitMillis);
        assertThat(get("/api/whoami", token).statusCode()).isEqualTo(401);
    }

    @Test
    void regularUserCannotAccessAdminEndpoint() throws Exception {
        assertThat(get("/api/admin/tenants", userToken()).statusCode()).isEqualTo(403);
    }

    private tools.jackson.databind.JsonNode claims(String token) {
        String body = token.split("\\.")[1];
        return json.readTree(new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8));
    }
}
