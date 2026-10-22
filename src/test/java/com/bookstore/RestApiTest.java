package com.bookstore;

import com.bookstore.entity.Book;
import com.bookstore.dao.BookDao;
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
 * Verification test for Phase 3: Javalin REST API & OpenPDF Invoice Generator.
 */
public class RestApiTest {

    private static final Logger logger = LoggerFactory.getLogger(RestApiTest.class);
    private static final int TEST_PORT = 7071;
    private static App app;
    private static HttpClient client;
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        objectMapper = new ObjectMapper();

        new com.bookstore.service.DatabaseSeeder().seedUsersIfEmpty();
        app = new App(TEST_PORT);
        app.start();
        logger.info("Test server started on port {}", TEST_PORT);
    }

    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.stop();
        }
        HibernateUtil.shutdown();
    }

    @Test
    @DisplayName("Verify GET /api/health endpoint")
    public void testHealthEndpoint() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());

        JsonNode root = objectMapper.readTree(res.body());
        assertEquals("UP", root.get("status").asText());
        logger.info("Verified /api/health response: {}", res.body());
    }

    @Test
    @DisplayName("Verify GET /api/books returns book catalog")
    public void testGetBooks() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/books"))
                .GET()
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());

        JsonNode array = objectMapper.readTree(res.body());
        assertTrue(array.isArray());
        assertTrue(array.size() > 0, "Catalog should have seeded books");
        logger.info("Verified /api/books: returned {} books", array.size());
    }

    @Test
    @DisplayName("Verify POST /api/checkout and GET /api/orders/{id}/receipt PDF streaming")
    public void testCheckoutAndReceiptPdf() throws Exception {
        // 1. Get an existing book ID
        BookDao bookDao = new BookDao();
        Book book = bookDao.findAll().get(0);
        Long bookId = book.getBookId();

        // 2. Perform checkout
        String jsonPayload = String.format("""
            {
                "customerEmail": "rest.client@example.com",
                "customerFirstName": "Sarah",
                "customerLastName": "Connor",
                "paymentMethod": "CREDIT_CARD",
                "items": [
                    { "bookId": %d, "quantity": 1 }
                ]
            }
            """, bookId);

        HttpRequest checkoutReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/checkout"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> checkoutRes = client.send(checkoutReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, checkoutRes.statusCode(), "Checkout must return 201 Created");

        JsonNode checkoutJson = objectMapper.readTree(checkoutRes.body());
        assertTrue(checkoutJson.get("success").asBoolean());
        long orderId = checkoutJson.get("orderId").asLong();
        logger.info("Checkout succeeded via REST API! Generated Order ID: {}", orderId);

        // 3. Request digital PDF receipt
        HttpRequest receiptReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/orders/" + orderId + "/receipt"))
                .GET()
                .build();

        HttpResponse<byte[]> receiptRes = client.send(receiptReq, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, receiptRes.statusCode());
        assertTrue(receiptRes.headers().firstValue("Content-Type").orElse("").contains("application/pdf"));

        byte[] pdfBytes = receiptRes.body();
        assertTrue(pdfBytes.length > 500, "PDF should have substantial content size");

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", header, "Receipt stream must start with valid PDF magic bytes %PDF-");
        logger.info("Successfully verified OpenPDF receipt stream for Order #{} ({} bytes)", orderId, pdfBytes.length);
    }

    @Test
    @DisplayName("Verify PUT /api/books/{id}/restock updates stock")
    public void testRestockEndpoint() throws Exception {
        BookDao bookDao = new BookDao();
        Book book = bookDao.findAll().get(0);
        Long bookId = book.getBookId();
        int initialStock = book.getStockCount();

        String restockPayload = """
            {
                "additionalStock": 10
            }
            """;

        // Log in as owner to obtain auth token for protected restock endpoint
        String loginPayload = "{\"username\": \"owner\", \"password\": \"owner123\"}";
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginPayload))
                .build();
        HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginRes.statusCode());
        String token = objectMapper.readTree(loginRes.body()).get("token").asText();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + "/api/books/" + bookId + "/restock"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(restockPayload))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());

        JsonNode json = objectMapper.readTree(res.body());
        assertTrue(json.get("success").asBoolean());
        assertEquals(initialStock + 10, json.get("currentStock").asInt());
        logger.info("Restock endpoint verified! Book #{} stock updated: {} -> {}",
                bookId, initialStock, json.get("currentStock").asInt());
    }
}
