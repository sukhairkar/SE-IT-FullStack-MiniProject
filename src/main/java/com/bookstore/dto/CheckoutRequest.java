package com.bookstore.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for processing an atomic checkout.
 */
public class CheckoutRequest {

    private Long customerId;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    private String customerStreet;
    private String customerCity;
    private String customerPincode;
    private String customerPhone;

    private String paymentMethod; // e.g., "CASH", "CREDIT_CARD", "UPI"
    private Long employeeId;       // Cashier employee ID handling transaction (optional)
    private Boolean sendEmailReceipt; // Optional toggle to dispatch digital PDF invoice via email

    private List<CartItemDto> items = new ArrayList<>();

    public CheckoutRequest() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }

    public String getCustomerStreet() {
        return customerStreet;
    }

    public void setCustomerStreet(String customerStreet) {
        this.customerStreet = customerStreet;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity = customerCity;
    }

    public String getCustomerPincode() {
        return customerPincode;
    }

    public void setCustomerPincode(String customerPincode) {
        this.customerPincode = customerPincode;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Boolean getSendEmailReceipt() {
        return sendEmailReceipt;
    }

    public void setSendEmailReceipt(Boolean sendEmailReceipt) {
        this.sendEmailReceipt = sendEmailReceipt;
    }

    private java.math.BigDecimal cashTendered; // For counter cash drawer tender & change calculation
    private Long holdId; // Optional link to in-store pickup reservation

    public java.math.BigDecimal getCashTendered() {
        return cashTendered;
    }

    public void setCashTendered(java.math.BigDecimal cashTendered) {
        this.cashTendered = cashTendered;
    }

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }

    public void addItem(Long bookId, int quantity) {
        this.items.add(new CartItemDto(bookId, quantity));
    }

    @Override
    public String toString() {
        return "CheckoutRequest{" +
                "customerId=" + customerId +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", employeeId=" + employeeId +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}
