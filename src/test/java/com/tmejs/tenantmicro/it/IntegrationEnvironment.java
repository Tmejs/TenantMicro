package com.tmejs.tenantmicro.it;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationEnvironment {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");
    private static final GenericContainer<?> KEYCLOAK = new GenericContainer<>("quay.io/keycloak/keycloak:26.8.1")
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forClasspathResource("realm.json"), "/opt/keycloak/data/import/realm.json")
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp("/realms/tenantmicro-test/.well-known/openid-configuration").forPort(8080));
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    static {
        POSTGRES.start();
        KEYCLOAK.start();
    }

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", IntegrationEnvironment::issuer);
        registry.add("app.security.clock-skew", () -> "1s");
    }

    static String issuer() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/tenantmicro-test";
    }

    String token(String username, String clientId) throws Exception {
        String password = username.equals("admin") ? "admin-pass" : "alice-pass";
        String body = "grant_type=password&client_id=" + encode(clientId) + "&username=" + encode(username)
                + "&password=" + encode(password);
        HttpRequest request = HttpRequest.newBuilder(URI.create(issuer() + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Keycloak token request failed: " + response.statusCode() + " " + response.body());
        }
        return JSON.readTree(response.body()).get("access_token").asText();
    }

    String userToken() throws Exception {
        return token("alice", "tenantmicro-test");
    }

    String userToken(String username) throws Exception {
        return token(username, "tenantmicro-test");
    }

    String adminToken() throws Exception {
        return token("admin", "tenantmicro-test");
    }

    HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
