package com.bookstore;

import com.bookstore.service.DatabaseSeeder;
import com.bookstore.util.HibernateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 5: Role-Based Access Control (RBAC) & Authentication.
 */
public class RbacAuthTest {

    private static final Logger logger = LoggerFactory.getLogger(RbacAuthTest.class);
    private static final int TEST_PORT = 7072;
    private static App app;
    private static HttpClient client;
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        objectMapper = new ObjectMapper();

        // Initialize database and seed users
        DatabaseSeeder seeder = new DatabaseSeeder();
        seeder.seedIfEmpty();
        seeder.seedUsersIfEmpty();

        app = new App(TEST_PORT);
        app.start();
        logger.info("RBAC Test server started on port {}", TEST_PORT);
    }

    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.stop();
        }
        HibernateUtil.shutdown();
    }

    @Test
    @DisplayName("Verify unauthenticated access to /api/books/{id}/restock returns 401 Unauthorized")
    public void testUnauthenticatedAccessReturns401() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/books/1/restock"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"additionalStock\": 5}"))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, res.statusCode(), "Unauthenticated restock request must return 401 Unauthorized");
        logger.info("Verified 401 Unauthorized for unauthenticated restock attempt.");
    }

    @Test
    @DisplayName("Verify CUSTOMER login succeeds but restock attempt returns 403 Forbidden")
    public void testCustomerCannotRestockReturns403() throws Exception {
        // 1. Login as Customer
        String token = loginAndGetToken("john@example.com", "cust123");
        assertNotNull(token, "Customer token must not be null");

        // 2. Attempt to restock as Customer
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/books/1/restock"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString("{\"additionalStock\": 5}"))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode(), "Customer attempting to restock must return 403 Forbidden");
        logger.info("Verified 403 Forbidden for Customer restock attempt: {}", res.body());
    }

    @Test
    @DisplayName("Verify BOOKSTORE_OWNER login succeeds and restock returns 200 OK")
    public void testOwnerCanRestockReturns200() throws Exception {
        // 1. Login as Bookstore Owner
        String token = loginAndGetToken("owner", "owner123");
        assertNotNull(token, "Owner token must not be null");

        // 2. Perform restock as Owner
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/books/1/restock"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString("{\"additionalStock\": 5}"))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Owner restock request must return 200 OK");

        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.get("success").asBoolean());
        logger.info("Verified 200 OK for Owner restock: {}", res.body());
    }

    @Test
    @DisplayName("Verify OWNER cannot access /api/employees (403) but ADMIN can (200)")
    public void testRoleDelegationEmployeesAccess() throws Exception {
        // 1. Owner attempts to access employees
        String ownerToken = loginAndGetToken("owner", "owner123");
        HttpRequest ownerReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/employees"))
                .header("Authorization", "Bearer " + ownerToken)
                .GET()
                .build();

        HttpResponse<String> ownerRes = client.send(ownerReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, ownerRes.statusCode(), "Owner must be forbidden from accessing /api/employees");

        // 2. Admin accesses employees
        String adminToken = loginAndGetToken("admin", "admin123");
        HttpRequest adminReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/employees"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();

        HttpResponse<String> adminRes = client.send(adminReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, adminRes.statusCode(), "Admin must have access to /api/employees");
        logger.info("Verified role separation: Owner 403, Admin 200 for /api/employees");
    }

    @Test
    @DisplayName("Verify /api/auth/me and /api/auth/logout flow")
    public void testAuthMeAndLogout() throws Exception {
        String token = loginAndGetToken("admin", "admin123");

        // Verify /api/auth/me
        HttpRequest meReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/me"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> meRes = client.send(meReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, meRes.statusCode());
        JsonNode meJson = objectMapper.readTree(meRes.body());
        assertTrue(meJson.get("authenticated").asBoolean());
        assertEquals("ADMIN", meJson.get("user").get("role").asText());

        // Logout
        HttpRequest logoutReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/logout"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> logoutRes = client.send(logoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, logoutRes.statusCode());

        // Subsequent /api/auth/me must return 401
        HttpResponse<String> postLogoutRes = client.send(meReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, postLogoutRes.statusCode());
        logger.info("Verified session invalidation after logout.");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Login should return 200 for valid credentials");

        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.get("success").asBoolean());
        return json.get("token").asText();
    }
}
