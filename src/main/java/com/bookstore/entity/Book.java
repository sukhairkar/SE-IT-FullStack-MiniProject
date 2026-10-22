package com.bookstore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entity representing the BOOK table.
 */
@Entity
@Table(name = "BOOK")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Book_id")
    private Long bookId;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "Stock_Count", nullable = false)
    private Integer stockCount;

    @Column(name = "Genre", length = 100)
    private String genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Publisher_id", nullable = false)
    private Publisher publisher;

    @Column(name = "Active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    public Book() {
    }

    public Book(String title, BigDecimal price, Integer stockCount, String genre, Publisher publisher) {
        this.title = title;
        this.price = price;
        this.stockCount = stockCount;
        this.genre = genre;
        this.publisher = publisher;
        this.active = true;
    }

    public Book(String title, BigDecimal price, Integer stockCount, String genre, Publisher publisher, Boolean active) {
        this.title = title;
        this.price = price;
        this.stockCount = stockCount;
        this.genre = genre;
        this.publisher = publisher;
        this.active = active != null ? active : true;
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

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public boolean isActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active != null ? active : true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(bookId, book.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId);
    }

    @Override
    public String toString() {
        return "Book{" +
                "bookId=" + bookId +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", stockCount=" + stockCount +
                ", genre='" + genre + '\'' +
                '}';
    }
}
