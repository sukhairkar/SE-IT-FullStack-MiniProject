package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for the STORE_INVENTORY entity (store_id, book_id).
 */
@Embeddable
public class StoreInventoryId implements Serializable {

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "book_id")
    private Long bookId;

    public StoreInventoryId() {
    }

    public StoreInventoryId(Long storeId, Long bookId) {
        this.storeId = storeId;
        this.bookId = bookId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreInventoryId that = (StoreInventoryId) o;
        return Objects.equals(storeId, that.storeId) &&
                Objects.equals(bookId, that.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId, bookId);
    }

    @Override
    public String toString() {
        return "StoreInventoryId{" +
                "storeId=" + storeId +
                ", bookId=" + bookId +
                '}';
    }
}
