package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for the ORDER_BOOK entity.
 */
@Embeddable
public class OrderBookId implements Serializable {

    @Column(name = "Order_id")
    private Long orderId;

    @Column(name = "Book_id")
    private Long bookId;

    public OrderBookId() {
    }

    public OrderBookId(Long orderId, Long bookId) {
        this.orderId = orderId;
        this.bookId = bookId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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
        OrderBookId that = (OrderBookId) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(bookId, that.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, bookId);
    }

    @Override
    public String toString() {
        return "OrderBookId{" +
                "orderId=" + orderId +
                ", bookId=" + bookId +
                '}';
    }
}
