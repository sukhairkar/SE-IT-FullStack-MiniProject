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
 * Verification test suite for Phase 10: Comprehensive Book & Publisher/Author Management Backend & APIs.
 * Tests:
 * 1. Creating authors and publishers with contact numbers.
 * 2. Creating books linked to publisher and multiple authors (populating WRITES).
 * 3. Updating book metadata and modifying author associations.
 * 4. Atomic stock adjustments (+15 restock, -5 adjustment, negative rejection).
 * 5. Safe soft deletion / archiving (active = false) preserving relational order history.
 * 6. Security guards (401 unauthenticated, 403 customer, 200/201 owner & admin).
 */
public class Phase10OwnerCatalogTest {

    private static final Logger logger = LoggerFactory.getLogger(Phase10OwnerCatalogTest.class);
    private static final int TEST_PORT = 7075;

    private static App app;
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        objectMapper = new ObjectMapper();

        // Ensure database tables exist, demo data is seeded, and default accounts exist
        DatabaseSeeder seeder = new DatabaseSeeder();
        seeder.seedIfEmpty();
        seeder.ensureBooksActiveIfNull();
        seeder.seedUsersIfEmpty();

        UserService userService = new UserService();
        userService.seedDefaultUsersIfAbsent();

        app = new App(TEST_PORT);
        app.start();
        logger.info("Phase 10 Owner Catalog Test server running on port {}", TEST_PORT);
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

    private void loginAsOwner(HttpClient client) throws Exception {
        String payload = "{\"username\":\"owner@bookstore.com\",\"password\":\"Owner@123\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "Owner login must succeed");
    }

    private void loginAsCustomer(HttpClient client) throws Exception {
        String payload = "{\"username\":\"reader@gmail.com\",\"password\":\"Reader@123\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), "Customer login must succeed");
    }

    @Test
    @DisplayName("1. Verify creating and listing authors and publishers (POST & GET /api/owner/publishers, /api/owner/authors)")
    public void testCreateAuthorAndPublisher() throws Exception {
        HttpClient client = createSessionClient();
        loginAsOwner(client);

        long timestamp = System.currentTimeMillis();

        // 1. Create Author
        String authorPayload = String.format("{\"firstName\":\"Martin\",\"lastName\":\"Fowler_%d\",\"biography\":\"Renowned author of Refactoring and Patterns of Enterprise Application Architecture.\",\"emailId\":\"martin_%d@thoughtworks.com\"}", timestamp, timestamp);
        HttpRequest authorReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/authors"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authorPayload))
                .build();

        HttpResponse<String> authorResp = client.send(authorReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, authorResp.statusCode(), "Author creation must return 201 Created");
        JsonNode authorNode = objectMapper.readTree(authorResp.body());
        assertTrue(authorNode.has("authorId"));
        long authorId = authorNode.get("authorId").asLong();
        assertTrue(authorId > 0);
        assertEquals("Martin", authorNode.get("firstName").asText());

        // 2. Create Publisher with Contact Number
        String pubPayload = String.format("{\"name\":\"Manning Publications_%d\",\"street\":\"20 Baldwin Road\",\"city\":\"Shelter Island\",\"pincode\":\"11964\",\"contactNo\":\"+1-800-434-3151\"}", timestamp);
        HttpRequest pubReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/publishers"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(pubPayload))
                .build();

        HttpResponse<String> pubResp = client.send(pubReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, pubResp.statusCode(), "Publisher creation must return 201 Created");
        JsonNode pubNode = objectMapper.readTree(pubResp.body());
        long publisherId = pubNode.get("publisherId").asLong();
        assertTrue(publisherId > 0);
        assertTrue(pubNode.get("contactNumbers").toString().contains("800-434-3151"));

        // 3. Verify GET /api/owner/publishers and /api/owner/authors contain the new records
        HttpRequest getPubsReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/publishers"))
                .GET().build();
        HttpResponse<String> getPubsResp = client.send(getPubsReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getPubsResp.statusCode());
        assertTrue(getPubsResp.body().contains("Manning Publications_" + timestamp));

        HttpRequest getAuthorsReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/authors"))
                .GET().build();
        HttpResponse<String> getAuthorsResp = client.send(getAuthorsReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getAuthorsResp.statusCode());
        assertTrue(getAuthorsResp.body().contains("Fowler_" + timestamp));
    }

    @Test
    @DisplayName("2. Verify creating a book linked to a publisher and multiple authors (POST /api/owner/books)")
    public void testCreateBookWithMultipleAuthors() throws Exception {
        HttpClient client = createSessionClient();
        loginAsOwner(client);

        long timestamp = System.currentTimeMillis();

        // Create 2 co-authors
        String author1Json = String.format("{\"firstName\":\"Kent\",\"lastName\":\"Beck_%d\",\"biography\":\"Creator of Extreme Programming\",\"emailId\":\"kent_%d@test.com\"}", timestamp, timestamp);
        HttpResponse<String> a1Resp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/authors")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(author1Json)).build(), HttpResponse.BodyHandlers.ofString());
        long a1Id = objectMapper.readTree(a1Resp.body()).get("authorId").asLong();

        String author2Json = String.format("{\"firstName\":\"Erich\",\"lastName\":\"Gamma_%d\",\"biography\":\"Design Patterns Gang of Four\",\"emailId\":\"erich_%d@test.com\"}", timestamp, timestamp);
        HttpResponse<String> a2Resp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/authors")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(author2Json)).build(), HttpResponse.BodyHandlers.ofString());
        long a2Id = objectMapper.readTree(a2Resp.body()).get("authorId").asLong();

        // Create book linked to both authors and Publisher 1
        String bookPayload = String.format("{\"title\":\"Contributing to Eclipse_%d\",\"price\":45.50,\"stockCount\":30,\"genre\":\"Software Engineering\",\"publisherId\":1,\"authorIds\":[%d,%d]}", timestamp, a1Id, a2Id);
        HttpRequest bookReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bookPayload))
                .build();

        HttpResponse<String> bookResp = client.send(bookReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, bookResp.statusCode(), "Book creation must return 201 Created");
        JsonNode bookNode = objectMapper.readTree(bookResp.body());

        long bookId = bookNode.get("bookId").asLong();
        assertTrue(bookId > 0);
        assertTrue(bookNode.get("active").asBoolean(), "Newly created book must have active = true");
        assertEquals(30, bookNode.get("stockCount").asInt());

        // Verify multiple authors are populated
        JsonNode authorsArr = bookNode.get("authors");
        assertEquals(2, authorsArr.size(), "Both co-authors must be linked in WRITES");
        String authorsText = authorsArr.toString();
        assertTrue(authorsText.contains("Kent Beck_" + timestamp));
        assertTrue(authorsText.contains("Erich Gamma_" + timestamp));
    }

    @Test
    @DisplayName("3. Verify updating title, genre, price, publisher, and authors (PUT /api/owner/books/{id})")
    public void testUpdateBookDetailsAndAuthors() throws Exception {
        HttpClient client = createSessionClient();
        loginAsOwner(client);

        long timestamp = System.currentTimeMillis();

        // 1. Create a book
        String createJson = String.format("{\"title\":\"Initial Title_%d\",\"price\":20.00,\"stockCount\":10,\"genre\":\"Fiction\",\"publisherId\":1,\"authorIds\":[1]}", timestamp);
        HttpResponse<String> createResp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(createJson)).build(), HttpResponse.BodyHandlers.ofString());
        long bookId = objectMapper.readTree(createResp.body()).get("bookId").asLong();

        // 2. Update with new title, price, and author list [2, 3]
        String updateJson = String.format("{\"title\":\"Revised Masterpiece_%d\",\"price\":29.99,\"stockCount\":12,\"genre\":\"Computer Science\",\"publisherId\":2,\"authorIds\":[2,3]}", timestamp);
        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books/" + bookId))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
                .build();

        HttpResponse<String> updateResp = client.send(updateReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, updateResp.statusCode(), "Book update must return 200 OK");
        JsonNode updatedNode = objectMapper.readTree(updateResp.body());

        assertEquals("Revised Masterpiece_" + timestamp, updatedNode.get("title").asText());
        assertEquals(29.99, updatedNode.get("price").asDouble(), 0.001);
        assertEquals(12, updatedNode.get("stockCount").asInt());
        assertEquals("Computer Science", updatedNode.get("genre").asText());
        assertEquals(2, updatedNode.get("publisherId").asLong());
        assertEquals(2, updatedNode.get("authors").size());
    }

    @Test
    @DisplayName("4. Verify restocking and stock adjustment with audit reason (PATCH /api/owner/books/{id}/stock)")
    public void testStockAdjustmentWithAuditReason() throws Exception {
        HttpClient client = createSessionClient();
        loginAsOwner(client);

        long timestamp = System.currentTimeMillis();

        // Create book with stock 20
        String createJson = String.format("{\"title\":\"Stock Audit Test_%d\",\"price\":15.00,\"stockCount\":20,\"genre\":\"General\",\"publisherId\":1,\"authorIds\":[1]}", timestamp);
        HttpResponse<String> createResp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(createJson)).build(), HttpResponse.BodyHandlers.ofString());
        long bookId = objectMapper.readTree(createResp.body()).get("bookId").asLong();

        // 1. Restock by +15
        String restockPayload = "{\"adjustment\":15,\"reason\":\"Warehouse restock shipment #482\"}";
        HttpRequest restockReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books/" + bookId + "/stock"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(restockPayload))
                .build();

        HttpResponse<String> restockResp = client.send(restockReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, restockResp.statusCode());
        JsonNode restockNode = objectMapper.readTree(restockResp.body());
        assertEquals(20, restockNode.get("previousStock").asInt());
        assertEquals(15, restockNode.get("adjustment").asInt());
        assertEquals(35, restockNode.get("currentStock").asInt());
        assertEquals("Warehouse restock shipment #482", restockNode.get("reason").asText());

        // 2. Decrement by -5 (Damaged goods)
        String deductPayload = "{\"adjustment\":-5,\"reason\":\"Water damaged copies discarded\"}";
        HttpResponse<String> deductResp = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books/" + bookId + "/stock"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(deductPayload))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, deductResp.statusCode());
        JsonNode deductNode = objectMapper.readTree(deductResp.body());
        assertEquals(35, deductNode.get("previousStock").asInt());
        assertEquals(-5, deductNode.get("adjustment").asInt());
        assertEquals(30, deductNode.get("currentStock").asInt());

        // 3. Overflow deduction (-1000) -> Must return 400 Bad Request
        String overflowPayload = "{\"adjustment\":-1000,\"reason\":\"Invalid deduction test\"}";
        HttpResponse<String> overflowResp = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books/" + bookId + "/stock"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(overflowPayload))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, overflowResp.statusCode(), "Excessive deduction causing negative stock must be rejected");
    }

    @Test
    @DisplayName("5. Verify soft delete (archive) hides book from POS catalog while preserving historical relations (DELETE /api/owner/books/{id})")
    public void testSoftDeleteArchivalWithoutBreakingForeignKeys() throws Exception {
        HttpClient client = createSessionClient();
        loginAsOwner(client);

        long timestamp = System.currentTimeMillis();

        // 1. Create a book
        String createJson = String.format("{\"title\":\"Historical Order Test_%d\",\"price\":50.00,\"stockCount\":10,\"genre\":\"History\",\"publisherId\":1,\"authorIds\":[1]}", timestamp);
        HttpResponse<String> createResp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(createJson)).build(), HttpResponse.BodyHandlers.ofString());
        long bookId = objectMapper.readTree(createResp.body()).get("bookId").asLong();

        // 2. Simulate historical checkout order referencing this book
        String checkoutJson = String.format("{\"customerId\":1,\"employeeId\":1,\"paymentMethod\":\"CASH\",\"sendEmailReceipt\":false,\"items\":[{\"bookId\":%d,\"quantity\":1}]}", bookId);
        HttpResponse<String> checkoutResp = client.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + TEST_PORT + "/api/checkout")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(checkoutJson)).build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(checkoutResp.statusCode() == 200 || checkoutResp.statusCode() == 201, "Checkout establishing historical ORDER_BOOK must succeed");

        // 3. Soft delete/archive the book via DELETE /api/owner/books/{id}
        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books/" + bookId))
                .DELETE()
                .build();
        HttpResponse<String> deleteResp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, deleteResp.statusCode(), "Archival must return 200 OK without foreign key constraint crash");

        JsonNode deleteNode = objectMapper.readTree(deleteResp.body());
        assertTrue(deleteNode.get("success").asBoolean());
        assertFalse(deleteNode.get("active").asBoolean());

        // 4. Verify book is hidden from public POS catalog (GET /api/books)
        HttpRequest publicCatalogReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/books"))
                .GET().build();
        HttpResponse<String> publicResp = client.send(publicCatalogReq, HttpResponse.BodyHandlers.ofString());
        assertFalse(publicResp.body().contains("Historical Order Test_" + timestamp), "Archived book must NOT be visible in public customer catalog");

        // 5. Verify book IS present in owner view when requested with ?includeArchived=true
        HttpRequest ownerCatalogReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books?includeArchived=true"))
                .GET().build();
        HttpResponse<String> ownerResp = client.send(ownerCatalogReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(ownerResp.body().contains("Historical Order Test_" + timestamp), "Archived book must be visible in owner management view");
    }

    @Test
    @DisplayName("6. Verify security guards on /api/owner/* (401 unauth, 403 customer, 200/201 owner)")
    public void testRoleSecurityGuards() throws Exception {
        HttpClient client = createSessionClient();

        // 1. Unauthenticated request to /api/owner/books -> Must return 401 UNAUTHORIZED
        HttpRequest unauthReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books"))
                .GET().build();
        HttpResponse<String> unauthResp = client.send(unauthReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthResp.statusCode(), "Unauthenticated request to /api/owner/* must return 401");

        // 2. Customer login -> Request to /api/owner/books -> Must return 403 FORBIDDEN
        loginAsCustomer(client);
        HttpRequest custReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books"))
                .GET().build();
        HttpResponse<String> custResp = client.send(custReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, custResp.statusCode(), "Customer accessing /api/owner/* must return 403 Forbidden");

        // 3. Bookstore Owner login -> Request to /api/owner/books -> Must return 200 OK
        HttpClient ownerClient = createSessionClient();
        loginAsOwner(ownerClient);
        HttpRequest ownerReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/owner/books"))
                .GET().build();
        HttpResponse<String> ownerResp = ownerClient.send(ownerReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ownerResp.statusCode(), "Bookstore owner accessing /api/owner/* must be authorized");
    }
}
