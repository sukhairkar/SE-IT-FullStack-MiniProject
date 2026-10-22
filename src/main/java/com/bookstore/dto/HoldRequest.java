package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload for requesting an in-store 24-48hr shelf hold/reservation.
 * Customer reserves book on shelf; no online payment required; customer pays at counter upon pickup.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HoldRequest {

    private Long bookId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String pickupWindow; // e.g. "TODAY_4_HOURS", "TODAY_EVENING", "TOMORROW"
    private String notes;

    public HoldRequest() {
    }

    public HoldRequest(Long bookId, String customerName, String customerEmail, String customerPhone, String pickupWindow) {
        this.bookId = bookId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.pickupWindow = pickupWindow;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
