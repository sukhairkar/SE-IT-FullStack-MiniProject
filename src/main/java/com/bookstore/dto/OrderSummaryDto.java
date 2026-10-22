package com.bookstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing an order summary in order history listings.
 */
public class OrderSummaryDto {

    private Long orderId;
    private int quantity;
    private BigDecimal orderAmount;
    private LocalDateTime date;

    private Long customerId;
    private String customerName;
    private String customerEmail;

    private List<String> bookTitles = new ArrayList<>();

    public OrderSummaryDto() {
    }

    public OrderSummaryDto(Long orderId, int quantity, BigDecimal orderAmount, LocalDateTime date,
                           Long customerId, String customerName, String customerEmail, List<String> bookTitles) {
        this.orderId = orderId;
        this.quantity = quantity;
        this.orderAmount = orderAmount;
        this.date = date;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        if (bookTitles != null) {
            this.bookTitles = bookTitles;
        }
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public List<String> getBookTitles() {
        return bookTitles;
    }

    public void setBookTitles(List<String> bookTitles) {
        this.bookTitles = bookTitles;
    }
}
