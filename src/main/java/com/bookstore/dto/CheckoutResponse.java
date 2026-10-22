package com.bookstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed response returned after an atomic checkout is performed.
 */
public class CheckoutResponse {

    private boolean success;
    private String message;

    private Long orderId;
    private Long paymentId;
    private LocalDateTime orderDate;

    private Long customerId;
    private String customerName;
    private String customerEmail;

    private int totalQuantity;
    private BigDecimal totalAmount;

    private String paymentMethod;
    private String paymentStatus;
    private Long employeeId;
    private boolean emailReceiptDispatched;

    private List<PurchasedItem> items = new ArrayList<>();

    public static class PurchasedItem {
        private Long bookId;
        private String title;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;

        public PurchasedItem() {
        }

        public PurchasedItem(Long bookId, String title, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
            this.bookId = bookId;
            this.title = title;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }

        public Long getBookId() {
            return bookId;
        }

        public String getTitle() {
            return title;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }

    public CheckoutResponse() {
    }

    public static CheckoutResponse success(Long orderId, Long paymentId, LocalDateTime date,
                                          Long customerId, String customerName, String customerEmail,
                                          int totalQuantity, BigDecimal totalAmount,
                                          String paymentMethod, String paymentStatus,
                                          Long employeeId, List<PurchasedItem> items) {
        CheckoutResponse res = new CheckoutResponse();
        res.success = true;
        res.message = "Order #" + orderId + " placed successfully.";
        res.orderId = orderId;
        res.paymentId = paymentId;
        res.orderDate = date;
        res.customerId = customerId;
        res.customerName = customerName;
        res.customerEmail = customerEmail;
        res.totalQuantity = totalQuantity;
        res.totalAmount = totalAmount;
        res.paymentMethod = paymentMethod;
        res.paymentStatus = paymentStatus;
        res.employeeId = employeeId;
        res.items = items;
        return res;
    }

    public static CheckoutResponse failure(String errorMessage) {
        CheckoutResponse res = new CheckoutResponse();
        res.success = false;
        res.message = errorMessage;
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
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

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public boolean isEmailReceiptDispatched() {
        return emailReceiptDispatched;
    }

    public void setEmailReceiptDispatched(boolean emailReceiptDispatched) {
        this.emailReceiptDispatched = emailReceiptDispatched;
    }

    private BigDecimal cashTendered;
    private BigDecimal changeDue;

    public BigDecimal getCashTendered() {
        return cashTendered;
    }

    public void setCashTendered(BigDecimal cashTendered) {
        this.cashTendered = cashTendered;
    }

    public BigDecimal getChangeDue() {
        return changeDue;
    }

    public void setChangeDue(BigDecimal changeDue) {
        this.changeDue = changeDue;
    }

    public List<PurchasedItem> getItems() {
        return items;
    }

    public void setItems(List<PurchasedItem> items) {
        this.items = items;
    }
}
