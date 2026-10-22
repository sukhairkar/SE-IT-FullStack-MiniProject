package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload for updating an existing book.
 * Endpoint: PUT /api/owner/books/{id}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateBookRequest {

    private String title;
    private BigDecimal price;
    private Integer stockCount;
    private String genre;
    private Long publisherId;
    private List<Long> authorIds;
    private Long authorId;
    private Boolean active;

    public UpdateBookRequest() {
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
        return authorIds;
    }

    public void setAuthorIds(List<Long> authorIds) {
        this.authorIds = authorIds;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
