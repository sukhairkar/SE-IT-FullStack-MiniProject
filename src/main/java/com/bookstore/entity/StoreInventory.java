package com.bookstore.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing store-specific inventory and shelf locations in the STORE_INVENTORY table.
 * Associates a Bookstore branch with a Book title via composite primary key (store_id, book_id).
 */
@Entity
@Table(name = "STORE_INVENTORY")
public class StoreInventory {

    @EmbeddedId
    private StoreInventoryId id;

    @MapsId("storeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Bookstore bookstore;

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "stock_count", nullable = false)
    private Integer stockCount = 0;

    @Column(name = "shelf_location", length = 100)
    private String shelfLocation;

    public StoreInventory() {
    }

    public StoreInventory(Bookstore bookstore, Book book, Integer stockCount, String shelfLocation) {
        this.bookstore = bookstore;
        this.book = book;
        this.stockCount = stockCount != null ? stockCount : 0;
        this.shelfLocation = shelfLocation;
        this.id = new StoreInventoryId(
                bookstore != null ? bookstore.getStoreId() : null,
                book != null ? book.getBookId() : null
        );
    }

    public StoreInventory(StoreInventoryId id, Bookstore bookstore, Book book, Integer stockCount, String shelfLocation) {
        this.id = id;
        this.bookstore = bookstore;
        this.book = book;
        this.stockCount = stockCount != null ? stockCount : 0;
        this.shelfLocation = shelfLocation;
    }

    public StoreInventoryId getId() {
        return id;
    }

    public void setId(StoreInventoryId id) {
        this.id = id;
    }

    public Bookstore getBookstore() {
        return bookstore;
    }

    public void setBookstore(Bookstore bookstore) {
        this.bookstore = bookstore;
        if (bookstore != null && this.id != null) {
            this.id.setStoreId(bookstore.getStoreId());
        }
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
        if (book != null && this.id != null) {
            this.id.setBookId(book.getBookId());
        }
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreInventory that = (StoreInventory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StoreInventory{" +
                "id=" + id +
                ", stockCount=" + stockCount +
                ", shelfLocation='" + shelfLocation + '\'' +
                '}';
    }
}
