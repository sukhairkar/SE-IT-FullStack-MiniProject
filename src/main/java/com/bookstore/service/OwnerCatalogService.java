package com.bookstore.service;

import com.bookstore.dao.AuthorDao;
import com.bookstore.dao.BookDao;
import com.bookstore.dao.OrderDao;
import com.bookstore.dao.PublisherDao;
import com.bookstore.dto.*;
import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderBook;
import com.bookstore.entity.Payment;
import com.bookstore.entity.Publisher;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service managing catalog lifecycle operations for bookstore owners and administrators:
 * - Adding and updating books with multi-author associations
 * - Atomic stock adjustments with audit reasons
 * - Safe soft deletion (archiving) preserving historical relational integrity
 * - Publisher and author creation and management
 * - Owner analytics metrics and order history audit
 */
public class OwnerCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(OwnerCatalogService.class);

    private final BookDao bookDao;
    private final PublisherDao publisherDao;
    private final AuthorDao authorDao;
    private final OrderDao orderDao;
    private final ReceiptPdfService receiptPdfService;
    private final EmailReceiptService emailReceiptService;

    public OwnerCatalogService() {
        this(new BookDao(), new PublisherDao(), new AuthorDao(), new OrderDao(), new ReceiptPdfService(), new EmailReceiptService());
    }

    public OwnerCatalogService(BookDao bookDao, PublisherDao publisherDao, AuthorDao authorDao) {
        this(bookDao, publisherDao, authorDao, new OrderDao(), new ReceiptPdfService(), new EmailReceiptService());
    }

    public OwnerCatalogService(BookDao bookDao, PublisherDao publisherDao, AuthorDao authorDao,
                               OrderDao orderDao, ReceiptPdfService receiptPdfService, EmailReceiptService emailReceiptService) {
        this.bookDao = bookDao;
        this.publisherDao = publisherDao;
        this.authorDao = authorDao;
        this.orderDao = orderDao;
        this.receiptPdfService = receiptPdfService;
        this.emailReceiptService = emailReceiptService;
    }

    /**
     * Creates a new book linked with a publisher and one or more authors.
     */
    public BookSummaryDto createBook(CreateBookRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Book request cannot be null.");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("Book title is required.");
        }
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Book price must be greater than zero.");
        }
        if (req.getStockCount() == null || req.getStockCount() < 0) {
            throw new IllegalArgumentException("Initial stock count cannot be negative.");
        }
        if (req.getPublisherId() == null) {
            throw new IllegalArgumentException("Publisher ID is required.");
        }

        Publisher publisher = publisherDao.findById(req.getPublisherId())
                .orElseThrow(() -> new IllegalArgumentException("Publisher not found with ID: " + req.getPublisherId()));

        List<Long> authorIds = req.getAuthorIds();
        if (authorIds == null || authorIds.isEmpty()) {
            throw new IllegalArgumentException("At least one author must be assigned to the book.");
        }

        // Validate that all author IDs exist
        for (Long aId : authorIds) {
            if (authorDao.findById(aId).isEmpty()) {
                throw new IllegalArgumentException("Author not found with ID: " + aId);
            }
        }

        Book book = new Book(
                req.getTitle().trim(),
                req.getPrice(),
                req.getStockCount(),
                req.getGenre() != null ? req.getGenre().trim() : "General",
                publisher,
                true // Active by default
        );

        Book savedBook = bookDao.saveWithAuthors(book, authorIds);
        logger.info("Created new book #{} ('{}') with publisher #{} and {} author(s)",
                savedBook.getBookId(), savedBook.getTitle(), publisher.getPublisherId(), authorIds.size());

        return getBookById(savedBook.getBookId());
    }

    /**
     * Updates an existing book and its author/publisher associations.
     */
    public BookSummaryDto updateBook(Long bookId, UpdateBookRequest req) {
        if (bookId == null) {
            throw new IllegalArgumentException("Book ID cannot be null.");
        }
        if (req == null) {
            throw new IllegalArgumentException("Update payload cannot be null.");
        }

        // Verify book exists
        Book existing = bookDao.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));

        if (req.getPrice() != null && req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Book price must be greater than zero.");
        }
        if (req.getStockCount() != null && req.getStockCount() < 0) {
            throw new IllegalArgumentException("Stock count cannot be negative.");
        }

        if (req.getPublisherId() != null) {
            if (publisherDao.findById(req.getPublisherId()).isEmpty()) {
                throw new IllegalArgumentException("Publisher not found with ID: " + req.getPublisherId());
            }
        }

        List<Long> authorIds = req.getAuthorIds();
        if (authorIds != null && !authorIds.isEmpty()) {
            for (Long aId : authorIds) {
                if (authorDao.findById(aId).isEmpty()) {
                    throw new IllegalArgumentException("Author not found with ID: " + aId);
                }
            }
        }

        bookDao.updateWithAuthors(
                bookId,
                req.getTitle(),
                req.getPrice(),
                req.getStockCount(),
                req.getGenre(),
                req.getPublisherId(),
                authorIds,
                req.getActive()
        );

        logger.info("Updated book #{} ('{}')", bookId, req.getTitle() != null ? req.getTitle() : existing.getTitle());
        return getBookById(bookId);
    }

    /**
     * Performs a quick atomic adjustment on book inventory with an audit reason.
     */
    public Map<String, Object> adjustStock(Long bookId, StockAdjustmentRequest req) {
        if (bookId == null) {
            throw new IllegalArgumentException("Book ID is required.");
        }
        if (req == null) {
            throw new IllegalArgumentException("Stock adjustment request cannot be null.");
        }

        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));

        int previousStock = book.getStockCount() != null ? book.getStockCount() : 0;
        int delta;

        if (req.getNewStock() != null) {
            if (req.getNewStock() < 0) {
                throw new IllegalArgumentException("Stock count cannot be negative.");
            }
            delta = req.getNewStock() - previousStock;
        } else if (req.getAdjustment() != null) {
            delta = req.getAdjustment();
        } else {
            throw new IllegalArgumentException("Either 'adjustment' (delta) or 'newStock' (absolute) must be provided.");
        }

        if (previousStock + delta < 0) {
            throw new IllegalArgumentException("Adjustment would cause negative stock. Current: " + previousStock + ", adjustment: " + delta);
        }

        int currentStock = bookDao.adjustStock(bookId, delta);
        String reason = req.getReason() != null && !req.getReason().isBlank() ? req.getReason().trim() : (delta >= 0 ? "Stock Restock" : "Inventory Adjustment");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("bookId", bookId);
        result.put("title", book.getTitle());
        result.put("previousStock", previousStock);
        result.put("adjustment", delta);
        result.put("currentStock", currentStock);
        result.put("reason", reason);
        result.put("message", "Stock adjusted successfully: " + previousStock + " -> " + currentStock + " (" + (delta >= 0 ? "+" : "") + delta + ")");

        logger.info("Stock adjusted for book #{}: {} -> {} | Reason: {}", bookId, previousStock, currentStock, reason);
        return result;
    }

    /**
     * Soft deletes (archives) a book so historical order records remain valid
     * while hiding it from the active POS catalog.
     */
    public Map<String, Object> archiveBook(Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Book ID is required.");
        }

        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));

        boolean success = bookDao.archive(bookId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("bookId", bookId);
        result.put("title", book.getTitle());
        result.put("active", false);
        result.put("message", "Book #" + bookId + " ('" + book.getTitle() + "') was successfully archived and hidden from POS catalog.");

        logger.info("Archived book #{} ('{}')", bookId, book.getTitle());
        return result;
    }

    /**
     * Retrieves book catalog for management dashboard, supporting search and inclusion of archived titles.
     */
    public List<BookSummaryDto> getBooks(boolean includeArchived, String search) {
        if (search != null && !search.isBlank()) {
            return bookDao.search(search, includeArchived);
        }
        return bookDao.findAllSummaries(includeArchived);
    }

    /**
     * Retrieves a single book summary by ID.
     */
    public BookSummaryDto getBookById(Long bookId) {
        List<BookSummaryDto> all = bookDao.findAllSummaries(true);
        return all.stream()
                .filter(b -> b.getBookId().equals(bookId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));
    }

    /**
     * Retrieves all publishers enriched with contact numbers.
     */
    public List<PublisherSummaryDto> getPublishers() {
        List<Publisher> publishers = publisherDao.findAll();
        List<PublisherSummaryDto> dtoList = new ArrayList<>();

        for (Publisher p : publishers) {
            List<String> contacts = publisherDao.getContacts(p.getPublisherId());
            dtoList.add(new PublisherSummaryDto(
                    p.getPublisherId(),
                    p.getName(),
                    p.getStreet(),
                    p.getCity(),
                    p.getPincode(),
                    contacts
            ));
        }
        return dtoList;
    }

    /**
     * Creates a new publisher with optional contact number.
     */
    public PublisherSummaryDto createPublisher(CreatePublisherRequest req) {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Publisher name is required.");
        }

        Publisher publisher = new Publisher(
                req.getName().trim(),
                req.getStreet() != null ? req.getStreet().trim() : null,
                req.getCity() != null ? req.getCity().trim() : null,
                req.getPincode() != null ? req.getPincode().trim() : null
        );

        Publisher saved = publisherDao.save(publisher);

        List<String> contacts = new ArrayList<>();
        if (req.getContactNo() != null && !req.getContactNo().isBlank()) {
            publisherDao.addContact(saved.getPublisherId(), req.getContactNo().trim());
            contacts.add(req.getContactNo().trim());
        }

        logger.info("Created publisher #{} ('{}')", saved.getPublisherId(), saved.getName());
        return new PublisherSummaryDto(
                saved.getPublisherId(),
                saved.getName(),
                saved.getStreet(),
                saved.getCity(),
                saved.getPincode(),
                contacts
        );
    }

    /**
     * Retrieves all authors.
     */
    public List<AuthorSummaryDto> getAuthors() {
        List<Author> authors = authorDao.findAll();
        List<AuthorSummaryDto> dtoList = new ArrayList<>();
        for (Author a : authors) {
            dtoList.add(new AuthorSummaryDto(
                    a.getAuthorId(),
                    a.getFirstName(),
                    a.getLastName(),
                    a.getBiography(),
                    a.getEmailId()
            ));
        }
        return dtoList;
    }

    /**
     * Creates a new author.
     */
    public AuthorSummaryDto createAuthor(CreateAuthorRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Author request cannot be null.");
        }
        if (req.getFirstName() == null || req.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Author first name is required.");
        }
        if (req.getLastName() == null || req.getLastName().isBlank()) {
            throw new IllegalArgumentException("Author last name is required.");
        }

        String email = req.getEmailId() != null && !req.getEmailId().isBlank() ? req.getEmailId().trim().toLowerCase() : null;
        if (email != null && authorDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An author with email '" + email + "' already exists.");
        }

        Author author = new Author(
                req.getFirstName().trim(),
                req.getLastName().trim(),
                req.getBiography() != null ? req.getBiography().trim() : null,
                email
        );

        Author saved = authorDao.save(author);
        logger.info("Created author #{} ('{} {}')", saved.getAuthorId(), saved.getFirstName(), saved.getLastName());

        return new AuthorSummaryDto(
                saved.getAuthorId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getBiography(),
                saved.getEmailId()
        );
    }

    /**
     * Computes the 4 essential owner overview metrics:
     * 1. Total Catalog Size
     * 2. Low Stock Count (< 5)
     * 3. Today's Sales Volume (₹)
     * 4. Total Orders
     */
    public Map<String, Object> getMetrics() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long totalCatalogSize = session.createQuery("SELECT COUNT(b) FROM Book b", Long.class).uniqueResult();
            Long activeCatalogSize = session.createQuery("SELECT COUNT(b) FROM Book b WHERE (b.active = true OR b.active IS NULL)", Long.class).uniqueResult();
            Long lowStockCount = session.createQuery("SELECT COUNT(b) FROM Book b WHERE b.stockCount < 5 AND (b.active = true OR b.active IS NULL)", Long.class).uniqueResult();

            Object[] orderTotals = session.createQuery("SELECT COUNT(o), COALESCE(SUM(o.orderAmount), 0) FROM Order o", Object[].class).uniqueResult();
            long totalOrders = (orderTotals != null && orderTotals[0] != null) ? ((Number) orderTotals[0]).longValue() : 0L;
            BigDecimal totalSalesVolume = (orderTotals != null && orderTotals[1] != null) ? (BigDecimal) orderTotals[1] : BigDecimal.ZERO;

            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
            Object[] todayTotals = session.createQuery(
                    "SELECT COUNT(o), COALESCE(SUM(o.orderAmount), 0) FROM Order o WHERE o.date >= :start AND o.date < :end",
                    Object[].class)
                    .setParameter("start", startOfDay)
                    .setParameter("end", endOfDay)
                    .uniqueResult();
            long todayOrders = (todayTotals != null && todayTotals[0] != null) ? ((Number) todayTotals[0]).longValue() : 0L;
            BigDecimal todaySalesVolume = (todayTotals != null && todayTotals[1] != null) ? (BigDecimal) todayTotals[1] : BigDecimal.ZERO;

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("totalCatalogSize", totalCatalogSize != null ? totalCatalogSize : 0L);
            metrics.put("activeCatalogSize", activeCatalogSize != null ? activeCatalogSize : 0L);
            metrics.put("lowStockCount", lowStockCount != null ? lowStockCount : 0L);
            metrics.put("todaySalesVolume", todaySalesVolume);
            metrics.put("todayOrders", todayOrders);
            metrics.put("totalOrders", totalOrders);
            metrics.put("totalSalesVolume", totalSalesVolume);
            return metrics;
        }
    }

    /**
     * Retrieves past transactions enriched with customer email, payment method, date, total amount, and item titles.
     */
    public List<Map<String, Object>> getOwnerOrders(int limit) {
        int safeLimit = limit > 0 ? limit : 100;
        List<Order> orders = orderDao.findRecentOrders(safeLimit);
        List<Map<String, Object>> result = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            for (Order o : orders) {
                List<OrderBook> books = orderDao.findOrderBooks(o.getOrderId());
                List<String> titles = books.stream()
                        .map(ob -> ob.getBook() != null ? ob.getBook().getTitle() : "Book #" + ob.getId().getBookId())
                        .toList();

                Payment payment = null;
                if (o.getCustomer() != null) {
                    payment = session.createQuery(
                            "FROM Payment p WHERE p.customer.customerId = :cId AND p.paymentAmount = :amt ORDER BY p.paymentId DESC",
                            Payment.class)
                            .setParameter("cId", o.getCustomer().getCustomerId())
                            .setParameter("amt", o.getOrderAmount())
                            .setMaxResults(1)
                            .uniqueResult();
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("orderId", o.getOrderId());
                map.put("date", o.getDate());
                map.put("quantity", o.getQuantity());
                map.put("orderAmount", o.getOrderAmount());
                map.put("customerEmail", o.getCustomer() != null ? o.getCustomer().getEmailId() : "N/A");
                map.put("customerName", o.getCustomer() != null ? o.getCustomer().getFirstName() + " " + o.getCustomer().getLastName() : "Guest");
                map.put("paymentMethod", payment != null && payment.getMethod() != null ? payment.getMethod() : "CASH");
                map.put("bookTitles", titles);
                result.add(map);
            }
        }
        return result;
    }

    /**
     * Generates and dispatches digital OpenPDF receipt to customer email for a past transaction.
     */
    public Map<String, Object> resendReceipt(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null.");
        }
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (order.getCustomer() == null || order.getCustomer().getEmailId() == null || order.getCustomer().getEmailId().isBlank()) {
            throw new IllegalArgumentException("Customer on Order #" + orderId + " does not have a valid email address.");
        }

        String recipientEmail = order.getCustomer().getEmailId().trim();
        try {
            File pdfFile = receiptPdfService.generateReceiptPdfFile(orderId);
            emailReceiptService.sendReceiptWithAttachment(recipientEmail, String.valueOf(orderId), pdfFile);
            logger.info("Resent receipt for Order #{} to '{}'", orderId, recipientEmail);

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("orderId", orderId);
            res.put("customerEmail", recipientEmail);
            res.put("message", "Digital PDF receipt dispatched to " + recipientEmail);
            return res;
        } catch (Exception e) {
            logger.error("Failed to resend receipt for Order #{}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate or dispatch receipt: " + e.getMessage(), e);
        }
    }
}
