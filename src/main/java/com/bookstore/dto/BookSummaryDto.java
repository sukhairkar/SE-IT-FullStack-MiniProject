package com.bookstore.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for displaying book details including publisher and authors.
 */
public class BookSummaryDto {

    private Long bookId;
    private String title;
    private BigDecimal price;
    private int stockCount;
    private String genre;
    private Long publisherId;
    private String publisherName;
    private List<String> authors = new ArrayList<>();
    private List<Long> authorIds = new ArrayList<>();
    private boolean active = true;

    public BookSummaryDto() {
    }

    public BookSummaryDto(Long bookId, String title, BigDecimal price, int stockCount,
                          String genre, Long publisherId, String publisherName, List<String> authors) {
        this.bookId = bookId;
        this.title = title;
        this.price = price;
        this.stockCount = stockCount;
        this.genre = genre;
        this.publisherId = publisherId;
        this.publisherName = publisherName;
        if (authors != null) {
            this.authors = authors;
        }
        this.active = true;
    }

    public BookSummaryDto(Long bookId, String title, BigDecimal price, int stockCount,
                          String genre, Long publisherId, String publisherName,
                          List<String> authors, List<Long> authorIds, boolean active) {
        this.bookId = bookId;
        this.title = title;
        this.price = price;
        this.stockCount = stockCount;
        this.genre = genre;
        this.publisherId = publisherId;
        this.publisherName = publisherName;
        if (authors != null) {
            this.authors = authors;
        }
        if (authorIds != null) {
            this.authorIds = authorIds;
        }
        this.active = active;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockCount() {
        return stockCount;
    }

    public void setStockCount(int stockCount) {
        this.stockCount = stockCount;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors != null ? authors : new ArrayList<>();
    }

    public List<Long> getAuthorIds() {
        return authorIds;
    }

    public void setAuthorIds(List<Long> authorIds) {
        this.authorIds = authorIds != null ? authorIds : new ArrayList<>();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private String shelfLocation;

    public String getShelfLocation() {
        if (shelfLocation != null && !shelfLocation.isBlank()) {
            return shelfLocation;
        }
        if (genre == null || genre.isBlank()) {
            return "Aisle 1, Main Bay";
        }
        String g = genre.toLowerCase();
        if (g.contains("software") || g.contains("engineering")) return "Shelf A-1 (Tech & Coding Bay)";
        if (g.contains("computer") || g.contains("system")) return "Shelf A-2 (CS & Architecture)";
        if (g.contains("fiction") || g.contains("novel")) return "Shelf B-1 (Literature & Fiction)";
        if (g.contains("science") || g.contains("physics")) return "Shelf C-1 (Natural Sciences)";
        if (g.contains("history") || g.contains("biography")) return "Shelf D-1 (History & Memoirs)";
        if (g.contains("business") || g.contains("finance")) return "Shelf E-2 (Business & Economy)";
        return "Shelf B-3 (" + genre + " Section)";
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public boolean isLowStock() {
        return stockCount <= 5;
    }
}
