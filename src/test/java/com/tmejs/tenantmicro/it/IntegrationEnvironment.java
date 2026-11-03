package com.tmejs.tenantmicro.it;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    MockMvc mvc;

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
        String password = username + "-pass";
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

    MvcResponse get(String path, String bearerToken) throws Exception {
        return request("GET", path, bearerToken, null);
    }

    MvcResponse request(String method, String path, String bearerToken, String body) throws Exception {
        var request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        var response = mvc.perform(request).andReturn().getResponse();
        return new MvcResponse(response.getStatus(), response.getContentAsString());
    }

    record MvcResponse(int statusCode, String body) { }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
