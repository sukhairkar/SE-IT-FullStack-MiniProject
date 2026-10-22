package com.bookstore.dto;

/**
 * DTO representing an item in the checkout cart.
 */
public class CartItemDto {

    private Long bookId;
    private int quantity;

    public CartItemDto() {
    }

    public CartItemDto(Long bookId, int quantity) {
        this.bookId = bookId;
        this.quantity = quantity;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItemDto{" +
                "bookId=" + bookId +
                ", quantity=" + quantity +
                '}';
    }
}
