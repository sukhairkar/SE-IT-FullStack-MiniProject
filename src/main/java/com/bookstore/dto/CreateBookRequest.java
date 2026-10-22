package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload for creating a new book with assigned publisher and authors.
 * Endpoint: POST /api/owner/books
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateBookRequest {

    private String title;
    private BigDecimal price;
    private Integer stockCount;
    private String genre;
    private Long publisherId;
    private List<Long> authorIds = new ArrayList<>();
    private Long authorId; // Single author fallback

    public CreateBookRequest() {
    }

    public CreateBookRequest(String title, BigDecimal price, Integer stockCount, String genre, Long publisherId, List<Long> authorIds) {
        this.title = title;
        this.price = price;
        this.stockCount = stockCount;
        this.genre = genre;
        this.publisherId = publisherId;
        if (authorIds != null) {
            this.authorIds = authorIds;
        }
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

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
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

    public List<Long> getAuthorIds() {
        if ((authorIds == null || authorIds.isEmpty()) && authorId != null) {
            List<Long> single = new ArrayList<>();
            single.add(authorId);
            return single;
        }
        return authorIds != null ? authorIds : new ArrayList<>();
    }

    public void setAuthorIds(List<Long> authorIds) {
        this.authorIds = authorIds != null ? authorIds : new ArrayList<>();
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }
}
