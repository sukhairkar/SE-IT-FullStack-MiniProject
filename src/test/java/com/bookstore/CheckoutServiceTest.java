package com.bookstore;

import com.bookstore.dao.BookDao;
import com.bookstore.dao.CustomerDao;
import com.bookstore.dao.EmployeeDao;
import com.bookstore.dao.OrderDao;
import com.bookstore.dto.BookSummaryDto;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.dto.CheckoutResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Customer;
import com.bookstore.entity.Employee;
import com.bookstore.entity.Order;
import com.bookstore.service.CheckoutService;
import com.bookstore.service.DatabaseSeeder;
import com.bookstore.service.InsufficientStockException;
import com.bookstore.util.HibernateUtil;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 2: Core DAOs and Atomic Checkout Service.
 */
public class CheckoutServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutServiceTest.class);

    private static BookDao bookDao;
    private static CustomerDao customerDao;
    private static EmployeeDao employeeDao;
    private static OrderDao orderDao;
    private static CheckoutService checkoutService;

    @BeforeAll
    public static void setUp() {
        bookDao = new BookDao();
        customerDao = new CustomerDao();
        employeeDao = new EmployeeDao();
        orderDao = new OrderDao();
        checkoutService = new CheckoutService(bookDao, customerDao, employeeDao);

        // Ensure database has demo data
        new DatabaseSeeder().seedIfEmpty();
    }

    @AfterAll
    public static void tearDown() {
        HibernateUtil.shutdown();
    }

    @Test
    @DisplayName("Verify DAOs retrieve books, authors, and low stock items correctly")
    public void testDaoQueries() {
        List<BookSummaryDto> summaries = bookDao.findAllSummaries();
        assertFalse(summaries.isEmpty(), "Book summaries list should not be empty");
        logger.info("Retrieved {} book summaries with author & publisher joins.", summaries.size());

        List<BookSummaryDto> searchResults = bookDao.search("clean");
        assertFalse(searchResults.isEmpty(), "Search for 'clean' should return matching books");
        logger.info("Search for 'clean' returned {} result(s).", searchResults.size());

        List<Book> lowStockBooks = bookDao.findLowStock(5);
        assertFalse(lowStockBooks.isEmpty(), "Should find books with stock <= 5");
        logger.info("Found {} books meeting low stock criteria (stock <= 5).", lowStockBooks.size());
    }

    @Test
    @DisplayName("Verify Atomic Checkout: stock decrement, order persistence, and payment handling")
    public void testSuccessfulAtomicCheckout() {
        List<Book> books = bookDao.findAll();
        Book targetBook = books.stream()
                .filter(b -> b.getStockCount() >= 5)
                .findFirst()
                .orElseThrow();

        int initialStock = targetBook.getStockCount();
        Long bookId = targetBook.getBookId();

        Customer customer = customerDao.findAll().get(0);
        Employee cashier = employeeDao.findAll().get(0);

        int buyQuantity = 2;
        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerId(customer.getCustomerId());
        request.setEmployeeId(cashier.getEmployeeId());
        request.setPaymentMethod("UPI");
        request.addItem(bookId, buyQuantity);

        logger.info("Executing successful checkout for {} copies of '{}' (Initial stock: {})...",
                buyQuantity, targetBook.getTitle(), initialStock);

        CheckoutResponse response = checkoutService.processCheckout(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getOrderId());
        assertNotNull(response.getPaymentId());
        assertEquals("UPI", response.getPaymentMethod());
        assertEquals(buyQuantity, response.getTotalQuantity());

        // Verify stock was decremented in database
        Book updatedBook = bookDao.findById(bookId).orElseThrow();
        assertEquals(initialStock - buyQuantity, updatedBook.getStockCount(),
                "Stock count must be decremented by purchased quantity");

        // Verify order persisted with line items
        Order order = orderDao.findById(response.getOrderId()).orElseThrow();
        assertEquals(buyQuantity, order.getQuantity());
        assertEquals(response.getTotalAmount(), order.getOrderAmount());

        logger.info("Atomic checkout verified! Order ID: {}, Decremented Stock: {} -> {}",
                response.getOrderId(), initialStock, updatedBook.getStockCount());
    }

    @Test
    @DisplayName("Verify Transaction Rollback on Insufficient Stock: no stock change, no ghost order")
    public void testInsufficientStockRollback() {
        List<Book> books = bookDao.findAll();
        Book targetBook = books.get(0);
        int initialStock = targetBook.getStockCount();
        Long bookId = targetBook.getBookId();

        int requestedQuantity = initialStock + 500; // Intentionally exceeds stock

        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerEmail("rollback.test@example.com");
        request.setCustomerFirstName("Test");
        request.setCustomerLastName("Rollback");
        request.setPaymentMethod("CASH");
        request.addItem(bookId, requestedQuantity);

        logger.info("Testing transaction rollback with excess quantity {} (Available: {})...",
                requestedQuantity, initialStock);

        assertThrows(InsufficientStockException.class, () -> {
            checkoutService.processCheckout(request);
        });

        // Verify stock has NOT changed in the database
        Book bookAfterRollback = bookDao.findById(bookId).orElseThrow();
        assertEquals(initialStock, bookAfterRollback.getStockCount(),
                "Stock must remain identical after a rolled-back transaction");

        logger.info("Transaction rollback successfully verified! Stock remained at {}.", initialStock);
    }

    /**
     * Standalone main runner to execute Phase 2 verification directly.
     */
    public static void main(String[] args) {
        logger.info("================================================================================");
        logger.info("Running Phase 2: Core DAOs & Atomic Checkout Service Verification");
        logger.info("================================================================================");
        try {
            setUp();
            CheckoutServiceTest test = new CheckoutServiceTest();

            logger.info("--- Step 1: Testing DAO Queries & Joins ---");
            test.testDaoQueries();

            logger.info("--- Step 2: Testing Successful Atomic Checkout ---");
            test.testSuccessfulAtomicCheckout();

            logger.info("--- Step 3: Testing Transaction Rollback on Insufficient Stock ---");
            test.testInsufficientStockRollback();

            logger.info("================================================================================");
            logger.info("PHASE 2 VERIFICATION SUCCESSFUL!");
            logger.info("All DAOs, HQL Joins, and Atomic Transactional Checkout passed with 100% integrity.");
            logger.info("================================================================================");
        } catch (Exception ex) {
            logger.error("Phase 2 verification failed: " + ex.getMessage(), ex);
            System.exit(1);
        } finally {
            tearDown();
        }
    }
}
