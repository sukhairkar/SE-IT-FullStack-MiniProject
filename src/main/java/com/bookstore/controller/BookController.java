package com.bookstore.controller;

import com.bookstore.dao.BookDao;
import com.bookstore.dao.PublisherDao;
import com.bookstore.dto.BookSummaryDto;
import com.bookstore.entity.Book;
import com.bookstore.entity.Publisher;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Book catalog operations.
 */
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private final BookDao bookDao = new BookDao();
    private final PublisherDao publisherDao = new PublisherDao();

    /**
     * GET /api/books
     * Supports optional ?search=keyword and ?lowStock=true
     */
    public void getAll(Context ctx) {
        String searchQuery = ctx.queryParam("search");
        String lowStock = ctx.queryParam("lowStock");

        if (searchQuery != null && !searchQuery.isBlank()) {
            List<BookSummaryDto> results = bookDao.search(searchQuery);
            ctx.json(results);
            return;
        }

        if ("true".equalsIgnoreCase(lowStock)) {
            List<BookSummaryDto> all = bookDao.findAllSummaries();
            List<BookSummaryDto> filtered = all.stream()
                    .filter(BookSummaryDto::isLowStock)
                    .toList();
            ctx.json(filtered);
            return;
        }

        ctx.json(bookDao.findAllSummaries());
    }

    /**
     * GET /api/books/{id}
     */
    public void getById(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            bookDao.findById(id).ifPresentOrElse(
                    ctx::json,
                    () -> ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Book not found with ID: " + id))
            );
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid book ID format"));
        }
    }

    /**
     * POST /api/books
     * Body: { "title": "...", "price": 29.99, "stockCount": 10, "genre": "...", "publisherId": 1, "authorId": 1 }
     */
    public void create(Context ctx) {
        try {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String title = (String) body.get("title");
            Number priceNum = (Number) body.get("price");
            Number stockNum = (Number) body.get("stockCount");
            String genre = (String) body.get("genre");
            Number publisherIdNum = (Number) body.get("publisherId");
            Number authorIdNum = (Number) body.get("authorId");

            if (title == null || title.isBlank() || priceNum == null || stockNum == null || publisherIdNum == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "title, price, stockCount, and publisherId are required"));
                return;
            }

            Publisher publisher = publisherDao.findById(publisherIdNum.longValue())
                    .orElseThrow(() -> new IllegalArgumentException("Publisher not found with ID: " + publisherIdNum));

            Book newBook = new Book(
                    title.trim(),
                    new BigDecimal(priceNum.toString()),
                    stockNum.intValue(),
                    genre != null ? genre.trim() : "General",
                    publisher
            );

            bookDao.save(newBook);

            if (authorIdNum != null) {
                bookDao.linkAuthor(newBook.getBookId(), authorIdNum.longValue());
            }

            ctx.status(HttpStatus.CREATED).json(Map.of(
                    "success", true,
                    "message", "Book created successfully",
                    "bookId", newBook.getBookId()
            ));

        } catch (Exception e) {
            logger.error("Failed to create book: " + e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/books/{id}/restock
     * Body: { "additionalStock": 10 } or { "newStock": 25 }
     */
    public void restock(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Book book = bookDao.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));

            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            int newStock;
            if (body.containsKey("additionalStock")) {
                int add = ((Number) body.get("additionalStock")).intValue();
                newStock = book.getStockCount() + add;
            } else if (body.containsKey("newStock")) {
                newStock = ((Number) body.get("newStock")).intValue();
            } else {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Must provide 'additionalStock' or 'newStock'"));
                return;
            }

            bookDao.updateStock(id, newStock);
            logger.info("Restocked book #{} ('{}') from {} to {} units.", id, book.getTitle(), book.getStockCount(), newStock);

            ctx.json(Map.of(
                    "success", true,
                    "message", "Stock updated successfully",
                    "bookId", id,
                    "previousStock", book.getStockCount(),
                    "currentStock", newStock
            ));

        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid book ID format"));
        } catch (Exception e) {
            logger.error("Failed to restock book: " + e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
        }
    }
}
