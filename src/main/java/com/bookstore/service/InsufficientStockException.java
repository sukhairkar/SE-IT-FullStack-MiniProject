package com.bookstore.service;

/**
 * Exception thrown when an atomic checkout fails due to insufficient book stock.
 */
public class InsufficientStockException extends RuntimeException {

    private final Long bookId;
    private final String bookTitle;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long bookId, String bookTitle, int requested, int available) {
        super(String.format("Insufficient stock for book '%s' (ID: %d). Requested: %d, Available: %d",
                bookTitle, bookId, requested, available));
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.requested = requested;
        this.available = available;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
