package com.bookstore;

import com.bookstore.dao.AppUserDao;
import com.bookstore.entity.AppRole;
import com.bookstore.entity.AppUser;
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
 * Verification test suite for Phase 12:
 * 1. Public Sign-Up Guard: Self-registration strictly defaults to CUSTOMER role.
 * 2. Multi-Store Entity Schema: Bookstore creation with geo-coordinates and address.
 * 3. Admin Store Management: Creation of distinct stores and binding of separate BOOKSTORE_OWNER users.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase12MultiStoreTest {

    private static final Logger logger = LoggerFactory.getLogger(Phase12MultiStoreTest.class);
    private static final int TEST_PORT = 7074;
    private static App app;
    private static HttpClient client;
    private static ObjectMapper objectMapper;

    private static String adminToken;
    private static Long store1Id;
    private static Long store2Id;

    @BeforeAll
    public static void setUp() throws Exception {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        objectMapper = new ObjectMapper();

        // Initialize schema and seed accounts
        DatabaseSeeder seeder = new DatabaseSeeder();
        seeder.seedIfEmpty();
        seeder.ensureBooksActiveIfNull();
        seeder.seedUsersIfEmpty();

        app = new App(TEST_PORT);
        app.start();
        logger.info("Phase 12 Multi-Store Test server started on port {}", TEST_PORT);

        // Authenticate as Admin to acquire Bearer token
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"admin@bookstore.com\",\"password\":\"Admin@123\"}"
                ))
                .build();

        HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginRes.statusCode(), "Admin login should succeed");
        JsonNode json = objectMapper.readTree(loginRes.body());
        adminToken = json.get("token").asText();
        assertNotNull(adminToken, "Admin token must not be null");
    }

    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.stop();
        }
        HibernateUtil.shutdown();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Public Sign-Up Guard: Client attempting to register as BOOKSTORE_OWNER defaults strictly to CUSTOMER")
    public void testPublicRegistrationDefaultsToCustomer() throws Exception {
        String testEmail = "malicious_owner_" + System.currentTimeMillis() + "@test.com";

        // Client attempts to pass role: BOOKSTORE_OWNER
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "email", testEmail,
                "password", "Password@123",
                "role", "BOOKSTORE_OWNER"
        ));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res.statusCode(), "Registration should succeed");

        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.get("success").asBoolean());
        // Verify response role is strictly CUSTOMER
        assertEquals("CUSTOMER", json.get("role").asText(),
                "Public self-registration must assign role CUSTOMER even when client requests BOOKSTORE_OWNER");

        // Verify in database
        AppUserDao userDao = new AppUserDao();
        AppUser user = userDao.findByEmail(testEmail).orElse(null);
        assertNotNull(user, "User must exist in database");
        assertEquals(AppRole.CUSTOMER, user.getRole(), "Database role must be strictly CUSTOMER");
        assertNull(user.getAssignedStore(), "Self-registered customer must not have an assigned store");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Admin Store Provisioning: Admin creates 2 distinct stores with addresses and geo-coordinates")
    public void testAdminCreatesTwoDistinctStores() throws Exception {
        // Store 1: Bandra Branch
        String store1Payload = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "BookHaven Bandra Seaface Branch",
                "street", "Carter Road",
                "city", "Mumbai",
                "pincode", "400050",
                "phone", "+91 98200 11111",
                "email", "bandra.seaface@bookhaven.com",
                "latitude", 19.0607,
                "longitude", 72.8258
        ));

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/stores"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(store1Payload))
                .build();

        HttpResponse<String> res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res1.statusCode(), "Admin should successfully create store 1");
        JsonNode json1 = objectMapper.readTree(res1.body());
        assertTrue(json1.get("success").asBoolean());
        store1Id = json1.get("storeId").asLong();
        assertTrue(store1Id > 0, "Store 1 ID must be generated");

        // Store 2: Fort Heritage Branch
        String store2Payload = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "BookHaven Fort Heritage Branch",
                "street", "Kala Ghoda, Fort",
                "city", "Mumbai",
                "pincode", "400001",
                "phone", "+91 98200 22222",
                "email", "fort.heritage@bookhaven.com",
                "latitude", 18.9298,
                "longitude", 72.8331
        ));

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/stores"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(store2Payload))
                .build();

        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res2.statusCode(), "Admin should successfully create store 2");
        JsonNode json2 = objectMapper.readTree(res2.body());
        assertTrue(json2.get("success").asBoolean());
        store2Id = json2.get("storeId").asLong();
        assertTrue(store2Id > 0, "Store 2 ID must be generated");

        assertNotEquals(store1Id, store2Id, "Store 1 and Store 2 must have distinct IDs");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Admin Store Binding: Admin binds separate BOOKSTORE_OWNER users to each branch")
    public void testAdminBindsSeparateOwnersToStores() throws Exception {
        assertNotNull(store1Id, "Store 1 ID must be set");
        assertNotNull(store2Id, "Store 2 ID must be set");

        String owner1Email = "bandra.manager." + System.currentTimeMillis() + "@bookhaven.com";
        String owner2Email = "fort.manager." + System.currentTimeMillis() + "@bookhaven.com";

        // 1. Assign Owner 1 to Store 1
        String assign1Payload = objectMapper.writeValueAsString(java.util.Map.of(
                "email", owner1Email,
                "password", "BandraOwner@123",
                "displayName", "Bandra Seaface Store Manager"
        ));

        HttpRequest assignReq1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/stores/" + store1Id + "/assign-owner"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(assign1Payload))
                .build();

        HttpResponse<String> assignRes1 = client.send(assignReq1, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, assignRes1.statusCode(), "Assigning owner 1 should succeed");
        JsonNode assignJson1 = objectMapper.readTree(assignRes1.body());
        assertTrue(assignJson1.get("success").asBoolean());
        assertEquals("BOOKSTORE_OWNER", assignJson1.get("role").asText());
        assertEquals(owner1Email, assignJson1.get("ownerEmail").asText());

        // 2. Assign Owner 2 to Store 2
        String assign2Payload = objectMapper.writeValueAsString(java.util.Map.of(
                "email", owner2Email,
                "password", "FortOwner@123",
                "displayName", "Fort Heritage Store Manager"
        ));

        HttpRequest assignReq2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/stores/" + store2Id + "/assign-owner"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(assign2Payload))
                .build();

        HttpResponse<String> assignRes2 = client.send(assignReq2, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, assignRes2.statusCode(), "Assigning owner 2 should succeed");
        JsonNode assignJson2 = objectMapper.readTree(assignRes2.body());
        assertTrue(assignJson2.get("success").asBoolean());
        assertEquals("BOOKSTORE_OWNER", assignJson2.get("role").asText());
        assertEquals(owner2Email, assignJson2.get("ownerEmail").asText());

        // 3. Verify via GET /api/admin/stores that both stores are returned with correct owner emails
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/stores"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();

        HttpResponse<String> listRes = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listRes.statusCode());
        JsonNode storesArray = objectMapper.readTree(listRes.body());
        assertTrue(storesArray.isArray() && storesArray.size() >= 2, "Must return at least 2 stores");

        boolean foundStore1 = false;
        boolean foundStore2 = false;

        for (JsonNode storeNode : storesArray) {
            long sId = storeNode.get("storeId").asLong();
            if (sId == store1Id) {
                foundStore1 = true;
                assertEquals(owner1Email, storeNode.get("ownerEmail").asText());
                assertEquals("BookHaven Bandra Seaface Branch", storeNode.get("name").asText());
            } else if (sId == store2Id) {
                foundStore2 = true;
                assertEquals(owner2Email, storeNode.get("ownerEmail").asText());
                assertEquals("BookHaven Fort Heritage Branch", storeNode.get("name").asText());
            }
        }

        assertTrue(foundStore1, "Store 1 must be present in admin listing with correct owner email");
        assertTrue(foundStore2, "Store 2 must be present in admin listing with correct owner email");
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("RBAC Protection: Non-admin users are rejected with 401 or 403 on /api/admin/stores")
    public void testNonAdminCannotAccessStoreAdminApis() throws Exception {
        // Unauthenticated access
        HttpRequest unauthReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/stores"))
                .GET()
                .build();

        HttpResponse<String> unauthRes = client.send(unauthReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthRes.statusCode(), "Unauthenticated request should return 401");

        // Authenticated customer access
        HttpRequest custLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"reader@gmail.com\",\"password\":\"Reader@123\"}"
                ))
                .build();
        HttpResponse<String> custLoginRes = client.send(custLogin, HttpResponse.BodyHandlers.ofString());
        String custToken = objectMapper.readTree(custLoginRes.body()).get("token").asText();

        HttpRequest custReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/admin/stores"))
                .header("Authorization", "Bearer " + custToken)
                .GET()
                .build();

        HttpResponse<String> custRes = client.send(custReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, custRes.statusCode(), "Customer request to /api/admin/stores should return 403 Forbidden");
    }
}
