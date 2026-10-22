package com.bookstore.controller;

import com.bookstore.dto.*;
import com.bookstore.service.OwnerCatalogService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller handling bookstore owner catalog operations:
 * - Adding, updating, restocking, and soft-deleting books
 * - Publisher and author management
 * Guarded by BOOKSTORE_OWNER or ADMIN role.
 */
public class OwnerController {

    private static final Logger logger = LoggerFactory.getLogger(OwnerController.class);
    private final OwnerCatalogService ownerCatalogService;

    public OwnerController() {
        this(new OwnerCatalogService());
    }

    public OwnerController(OwnerCatalogService ownerCatalogService) {
        this.ownerCatalogService = ownerCatalogService;
    }

    /**
     * POST /api/owner/books
     * Creates a new book linked with a publisher and one or more authors.
     */
    public void createBook(Context ctx) {
        try {
            CreateBookRequest req = ctx.bodyAsClass(CreateBookRequest.class);
            BookSummaryDto created = ownerCatalogService.createBook(req);
            ctx.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException iae) {
            logger.warn("Validation error creating book: {}", iae.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Internal error creating book: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Failed to create book: " + ex.getMessage()
            ));
        }
    }

    /**
     * GET /api/owner/books
     * Retrieves books for owner dashboard (supports ?includeArchived=true&search=...).
     */
    public void getBooks(Context ctx) {
        try {
            boolean includeArchived = "true".equalsIgnoreCase(ctx.queryParam("includeArchived"));
            String search = ctx.queryParam("search");
            List<BookSummaryDto> books = ownerCatalogService.getBooks(includeArchived, search);
            ctx.json(books);
        } catch (Exception ex) {
            logger.error("Error retrieving books: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/owner/books/{id}
     */
    public void getBookById(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            BookSummaryDto book = ownerCatalogService.getBookById(id);
            ctx.json(book);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid book ID format"));
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * PUT /api/owner/books/{id}
     * Updates title, genre, price, publisher, and authors.
     */
    public void updateBook(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            UpdateBookRequest req = ctx.bodyAsClass(UpdateBookRequest.class);
            BookSummaryDto updated = ownerCatalogService.updateBook(id, req);
            ctx.json(updated);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid book ID format"));
        } catch (IllegalArgumentException iae) {
            logger.warn("Validation error updating book #{}: {}", ctx.pathParam("id"), iae.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Internal error updating book #{}: {}", ctx.pathParam("id"), ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Failed to update book: " + ex.getMessage()
            ));
        }
    }

    /**
     * PATCH /api/owner/books/{id}/stock
     * Quick adjustment to add/subtract stock count with an audit reason.
     */
    public void adjustStock(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            StockAdjustmentRequest req = ctx.bodyAsClass(StockAdjustmentRequest.class);
            Map<String, Object> result = ownerCatalogService.adjustStock(id, req);
            ctx.json(result);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid book ID format"));
        } catch (IllegalArgumentException iae) {
            logger.warn("Stock adjustment rejected for book #{}: {}", ctx.pathParam("id"), iae.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Internal error adjusting stock for book #{}: {}", ctx.pathParam("id"), ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Failed to adjust stock: " + ex.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/owner/books/{id}
     * Soft deletes (archives) the book (active = false) so historical orders remain valid
     * while immediately hiding it from the customer POS catalog.
     */
    public void archiveBook(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> result = ownerCatalogService.archiveBook(id);
            ctx.json(result);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid book ID format"));
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            logger.error("Internal error archiving book #{}: {}", ctx.pathParam("id"), ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Failed to archive book: " + ex.getMessage()
            ));
        }
    }

    /**
     * GET /api/owner/publishers
     */
    public void getPublishers(Context ctx) {
        try {
            List<PublisherSummaryDto> publishers = ownerCatalogService.getPublishers();
            ctx.json(publishers);
        } catch (Exception ex) {
            logger.error("Error retrieving publishers: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/owner/publishers
     */
    public void createPublisher(Context ctx) {
        try {
            CreatePublisherRequest req = ctx.bodyAsClass(CreatePublisherRequest.class);
            PublisherSummaryDto created = ownerCatalogService.createPublisher(req);
            ctx.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Error creating publisher: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/owner/authors
     */
    public void getAuthors(Context ctx) {
        try {
            List<AuthorSummaryDto> authors = ownerCatalogService.getAuthors();
            ctx.json(authors);
        } catch (Exception ex) {
            logger.error("Error retrieving authors: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/owner/authors
     */
    public void createAuthor(Context ctx) {
        try {
            CreateAuthorRequest req = ctx.bodyAsClass(CreateAuthorRequest.class);
            AuthorSummaryDto created = ownerCatalogService.createAuthor(req);
            ctx.status(HttpStatus.CREATED).json(created);
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Error creating author: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/owner/metrics
     * Returns the 4 overview metrics for bookstore owner dashboard.
     */
    public void getMetrics(Context ctx) {
        try {
            Map<String, Object> metrics = ownerCatalogService.getMetrics();
            ctx.json(metrics);
        } catch (Exception ex) {
            logger.error("Error retrieving owner metrics: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * GET /api/owner/orders
     * Retrieves past transactions enriched with customer email and payment method for audit.
     */
    public void getOrders(Context ctx) {
        try {
            int limit = 100;
            String limitParam = ctx.queryParam("limit");
            if (limitParam != null && !limitParam.isBlank()) {
                try {
                    limit = Integer.parseInt(limitParam.trim());
                } catch (NumberFormatException ignored) {}
            }
            List<Map<String, Object>> orders = ownerCatalogService.getOwnerOrders(limit);
            ctx.json(orders);
        } catch (Exception ex) {
            logger.error("Error retrieving owner orders: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/owner/orders/{id}/resend-receipt
     * Resends the digital OpenPDF receipt email to the customer.
     */
    public void resendReceipt(Context ctx) {
        try {
            Long orderId = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> result = ownerCatalogService.resendReceipt(orderId);
            ctx.json(result);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid order ID format"));
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            logger.error("Error resending receipt for order #{}: {}", ctx.pathParam("id"), ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", "Failed to resend receipt: " + ex.getMessage()));
        }
    }
}
