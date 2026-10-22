package com.bookstore.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing the ORDER_BOOK table.
 * Associates Orders with Books via composite primary key (Order_id, Book_id).
 */
@Entity
@Table(name = "ORDER_BOOK")
public class OrderBook {

    @EmbeddedId
    private OrderBookId id;

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Order_id", nullable = false)
    private Order order;

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Book_id", nullable = false)
    private Book book;

    public OrderBook() {
    }

    public OrderBook(Order order, Book book) {
        this.order = order;
        this.book = book;
        this.id = new OrderBookId(
                order != null ? order.getOrderId() : null,
                book != null ? book.getBookId() : null
        );
    }

    public OrderBook(OrderBookId id, Order order, Book book) {
        this.id = id;
        this.order = order;
        this.book = book;
    }

    public OrderBookId getId() {
        return id;
    }

    public void setId(OrderBookId id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
        if (order != null && id != null) {
            id.setOrderId(order.getOrderId());
        }
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
        if (book != null && id != null) {
            id.setBookId(book.getBookId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderBook orderBook = (OrderBook) o;
        return Objects.equals(id, orderBook.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrderBook{" +
                "id=" + id +
                '}';
    }
}
