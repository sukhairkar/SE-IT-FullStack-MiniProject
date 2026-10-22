package com.bookstore;

import com.bookstore.service.DatabaseSeeder;
import com.bookstore.service.UserService;
import com.bookstore.util.HibernateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 8: Robust Email + Password Authentication & Security Guards.
 */
public class Phase8AuthTest {

    private static final Logger logger = LoggerFactory.getLogger(Phase8AuthTest.class);
    private static final int TEST_PORT = 7073;

    private static App app;
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        objectMapper = new ObjectMapper();

        // Ensure database tables exist and demo accounts are seeded
        DatabaseSeeder seeder = new DatabaseSeeder();
        seeder.seedIfEmpty();
        seeder.seedUsersIfEmpty();

        UserService userService = new UserService();
        userService.seedDefaultUsersIfAbsent();

        app = new App(TEST_PORT);
        app.start();
        logger.info("Phase 8 Auth Test server running on port {}", TEST_PORT);
    }

    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.stop();
        }
        HibernateUtil.shutdown();
    }

    private HttpClient createSessionClient() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Test
    @DisplayName("Verify seeded demo accounts can authenticate successfully")
    public void testDemoAccountsLogin() throws Exception {
        HttpClient client = createSessionClient();

        // 1. Owner Login
        String ownerPayload = "{\"email\":\"owner@bookstore.com\",\"password\":\"Owner@123\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ownerPayload))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.get("success").asBoolean());
        assertEquals("owner@bookstore.com", json.get("email").asText());
        assertEquals("BOOKSTORE_OWNER", json.get("role").asText());

        // Verify /api/auth/me reflects active session
        HttpRequest meReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/me"))
                .GET()
                .build();
        HttpResponse<String> meRes = client.send(meReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, meRes.statusCode());
        JsonNode meJson = objectMapper.readTree(meRes.body());
        assertTrue(meJson.get("authenticated").asBoolean());
        assertEquals("owner@bookstore.com", meJson.get("email").asText());
        assertEquals("BOOKSTORE_OWNER", meJson.get("role").asText());
    }

    @Test
    @DisplayName("Verify customer registration establishes session with default CUSTOMER role")
    public void testCustomerRegistration() throws Exception {
        HttpClient client = createSessionClient();
        String uniqueEmail = "newcustomer_" + System.currentTimeMillis() + "@gmail.com";
        String regPayload = String.format("{\"email\":\"%s\",\"password\":\"SecurePass@123\"}", uniqueEmail);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(regPayload))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res.statusCode());

        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.get("success").asBoolean());
        assertEquals(uniqueEmail, json.get("email").asText());
        assertEquals("CUSTOMER", json.get("role").asText());

        // Verify session established
        HttpRequest meReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/me"))
                .GET()
                .build();
        HttpResponse<String> meRes = client.send(meReq, HttpResponse.BodyHandlers.ofString());
        JsonNode meJson = objectMapper.readTree(meRes.body());
        assertTrue(meJson.get("authenticated").asBoolean());
        assertEquals(uniqueEmail, meJson.get("email").asText());
        assertEquals("CUSTOMER", meJson.get("role").asText());
    }

    @Test
    @DisplayName("Verify duplicate email registration is rejected with 400 Bad Request")
    public void testDuplicateRegistrationRejected() throws Exception {
        HttpClient client = createSessionClient();
        String payload = "{\"email\":\"reader@gmail.com\",\"password\":\"NewPassword@123\"}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, res.statusCode());
        JsonNode json = objectMapper.readTree(res.body());
        assertFalse(json.get("success").asBoolean());
        assertTrue(json.get("error").asText().contains("already registered"));
    }

    @Test
    @DisplayName("Verify unauthenticated GET /api/auth/me returns authenticated: false")
    public void testUnauthenticatedMe() throws Exception {
        HttpClient client = createSessionClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/me"))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(res.statusCode() == 401 || res.statusCode() == 200, "Unauthenticated /me should return 401 or 200");
        JsonNode json = objectMapper.readTree(res.body());
        assertFalse(json.get("authenticated").asBoolean());
    }

    @Test
    @DisplayName("Verify logout invalidates session")
    public void testLogoutInvalidatesSession() throws Exception {
        HttpClient client = createSessionClient();

        // 1. Login
        String loginPayload = "{\"email\":\"reader@gmail.com\",\"password\":\"Reader@123\"}";
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginPayload))
                .build();
        HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginRes.statusCode());

        // 2. Logout
        HttpRequest logoutReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/logout"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> logoutRes = client.send(logoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, logoutRes.statusCode());

        // 3. Verify me returns authenticated: false
        HttpRequest meReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/me"))
                .GET()
                .build();
        HttpResponse<String> meRes = client.send(meReq, HttpResponse.BodyHandlers.ofString());
        JsonNode meJson = objectMapper.readTree(meRes.body());
        assertFalse(meJson.get("authenticated").asBoolean());
    }

    @Test
    @DisplayName("Verify security guard for /api/admin/* (401 unauth, 403 customer, 200 admin)")
    public void testAdminSecurityGuards() throws Exception {
        // 1. Unauthenticated -> 401
        HttpClient anonClient = createSessionClient();
        HttpRequest anonReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/employees"))
                .GET()
                .build();
        HttpResponse<String> anonRes = anonClient.send(anonReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, anonRes.statusCode());

        // 2. Customer -> 403
        HttpClient custClient = createSessionClient();
        custClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"reader@gmail.com\",\"password\":\"Reader@123\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> custRes = custClient.send(anonReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, custRes.statusCode());

        // 3. Admin -> 200
        HttpClient adminClient = createSessionClient();
        adminClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"admin@bookstore.com\",\"password\":\"Admin@123\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> adminRes = adminClient.send(anonReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, adminRes.statusCode());
    }

    @Test
    @DisplayName("Verify security guard for /api/inventory/* (401 unauth, 403 customer, 200 owner)")
    public void testInventorySecurityGuards() throws Exception {
        // 1. Unauthenticated -> 401
        HttpClient anonClient = createSessionClient();
        HttpRequest anonReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/inventory/books"))
                .GET()
                .build();
        HttpResponse<String> anonRes = anonClient.send(anonReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, anonRes.statusCode());

        // 2. Customer -> 403
        HttpClient custClient = createSessionClient();
        custClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"reader@gmail.com\",\"password\":\"Reader@123\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> custRes = custClient.send(anonReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, custRes.statusCode());

        // 3. Bookstore Owner -> 200
        HttpClient ownerClient = createSessionClient();
        ownerClient.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"owner@bookstore.com\",\"password\":\"Owner@123\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> ownerRes = ownerClient.send(anonReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ownerRes.statusCode());
    }
}
