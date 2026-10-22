package com.bookstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Details of a confirmed in-store shelf reservation.
 */
public class HoldResponse {

    private Long holdId;
    private String holdCode; // e.g. "HLD-1042"
    private Long bookId;
    private String bookTitle;
    private String shelfLocation;
    private BigDecimal price;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String pickupWindow;
    private String status; // "HELD", "READY_AT_COUNTER", "FULFILLED", "CANCELLED"
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String instructions;

    public HoldResponse() {
    }

    public HoldResponse(Long holdId, String holdCode, Long bookId, String bookTitle,
                        String shelfLocation, BigDecimal price, String customerName,
                        String customerEmail, String customerPhone, String pickupWindow,
                        String status, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.holdId = holdId;
        this.holdCode = holdCode;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.shelfLocation = shelfLocation;
        this.price = price;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.pickupWindow = pickupWindow;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.instructions = "Please present Hold Code " + holdCode + " at the BookHaven billing counter to collect your book and pay.";
    }

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public String getHoldCode() {
        return holdCode;
    }

    public void setHoldCode(String holdCode) {
        this.holdCode = holdCode;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPickupWindow() {
        return pickupWindow;
    }

    public void setPickupWindow(String pickupWindow) {
        this.pickupWindow = pickupWindow;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
