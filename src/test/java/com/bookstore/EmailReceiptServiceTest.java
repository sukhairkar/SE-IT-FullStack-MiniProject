package com.bookstore;

import com.bookstore.dto.CheckoutRequest;
import com.bookstore.dto.CheckoutResponse;
import com.bookstore.entity.Book;
import com.bookstore.dao.BookDao;
import com.bookstore.service.CheckoutService;
import com.bookstore.service.DatabaseSeeder;
import com.bookstore.service.EmailReceiptService;
import com.bookstore.service.ReceiptPdfService;
import com.bookstore.util.HibernateUtil;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Phase 6: Automated Asynchronous Email Receipt Dispatch with Jakarta Mail.
 */
public class EmailReceiptServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(EmailReceiptServiceTest.class);

    private static EmailReceiptService emailReceiptService;
    private static ReceiptPdfService receiptPdfService;
    private static CheckoutService checkoutService;

    @BeforeAll
    public static void setUp() {
        DatabaseSeeder seeder = new DatabaseSeeder();
        seeder.seedIfEmpty();
        seeder.seedUsersIfEmpty();

        emailReceiptService = new EmailReceiptService();
        receiptPdfService = new ReceiptPdfService();
        checkoutService = new CheckoutService();
    }

    @AfterAll
    public static void tearDown() {
        HibernateUtil.shutdown();
    }

    @Test
    @DisplayName("Verify asynchronous dispatch with valid OpenPDF receipt attachment")
    public void testSendReceiptWithAttachment() throws Exception {
        // 1. Create a sample PDF file using ReceiptPdfService for Order #1
        File pdfReceipt = receiptPdfService.generateReceiptPdfFile(1L);
        assertNotNull(pdfReceipt);
        assertTrue(pdfReceipt.exists());
        assertTrue(pdfReceipt.length() > 0, "PDF receipt file must not be empty");
        logger.info("Generated sample PDF receipt for testing: {} ({} bytes)", pdfReceipt.getAbsolutePath(), pdfReceipt.length());

        // 2. Dispatch asynchronously
        CompletableFuture<Boolean> future = emailReceiptService.sendReceiptWithAttachment(
                "customer.test@example.com",
                "1",
                pdfReceipt
        );

        // 3. Await result with timeout
        Boolean success = future.get(5, TimeUnit.SECONDS);
        assertTrue(success, "Email receipt dispatch (simulation or live) should complete with true");
        logger.info("Successfully verified asynchronous email receipt dispatch!");
    }

    @Test
    @DisplayName("Verify graceful handling of invalid recipient email")
    public void testInvalidEmailHandling() throws Exception {
        File pdfReceipt = receiptPdfService.generateReceiptPdfFile(1L);

        CompletableFuture<Boolean> future = emailReceiptService.sendReceiptWithAttachment(
                "invalid-email-address",
                "1",
                pdfReceipt
        );

        Boolean success = future.get(5, TimeUnit.SECONDS);
        assertFalse(success, "Invalid email address should be rejected gracefully and return false");
    }

    @Test
    @DisplayName("Verify graceful handling of missing attachment file")
    public void testMissingFileHandling() throws Exception {
        File nonExistentFile = new File("non_existent_file_" + System.currentTimeMillis() + ".pdf");

        CompletableFuture<Boolean> future = emailReceiptService.sendReceiptWithAttachment(
                "customer.test@example.com",
                "1",
                nonExistentFile
        );

        Boolean success = future.get(5, TimeUnit.SECONDS);
        assertFalse(success, "Missing attachment file should be handled gracefully and return false");
    }

    @Test
    @DisplayName("Verify end-to-end atomic checkout triggers async email dispatch post-commit")
    public void testCheckoutWithEmailDispatch() throws Exception {
        BookDao bookDao = new BookDao();
        Book book = bookDao.findAll().get(0);
        int initialStock = book.getStockCount();

        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerEmail("jane.doe.test@example.com");
        request.setCustomerFirstName("Jane");
        request.setCustomerLastName("Doe");
        request.setPaymentMethod("CREDIT_CARD");
        request.setSendEmailReceipt(true);
        request.addItem(book.getBookId(), 1);

        CheckoutResponse response = checkoutService.processCheckout(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getOrderId());
        assertTrue(response.isEmailReceiptDispatched(), "emailReceiptDispatched flag must be true when email is provided");
        assertEquals("jane.doe.test@example.com", response.getCustomerEmail());

        // Verify stock was decremented properly
        Book updatedBook = bookDao.findById(book.getBookId()).orElseThrow();
        assertEquals(initialStock - 1, updatedBook.getStockCount());
        logger.info("End-to-end checkout with automated email dispatch verified successfully for Order #{}", response.getOrderId());
    }

    @Test
    @DisplayName("Verify checkout respects sendEmailReceipt=false toggle")
    public void testCheckoutWithEmailDispatchDisabled() {
        BookDao bookDao = new BookDao();
        Book book = bookDao.findAll().get(0);

        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerEmail("silent.shopper@example.com");
        request.setPaymentMethod("CASH");
        request.setSendEmailReceipt(false); // Opt-out
        request.addItem(book.getBookId(), 1);

        CheckoutResponse response = checkoutService.processCheckout(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertFalse(response.isEmailReceiptDispatched(), "emailReceiptDispatched should be false when opted out");
    }
}
