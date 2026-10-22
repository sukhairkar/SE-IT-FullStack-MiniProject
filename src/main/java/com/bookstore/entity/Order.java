package com.bookstore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing the ORDER table.
 * Escaped as `ORDER` to avoid SQL reserved keyword conflicts.
 */
@Entity
@Table(name = "`ORDER`")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Order_id")
    private Long orderId;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "Order_Amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "`Date`", nullable = false)
    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_store_id")
    private Bookstore pickupStore;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 30)
    private OrderType orderType = OrderType.IN_STORE_POS;

    public Order() {
    }

    public Order(Integer quantity, BigDecimal orderAmount, LocalDateTime date, Customer customer) {
        this.quantity = quantity;
        this.orderAmount = orderAmount;
        this.date = date;
        this.customer = customer;
        this.orderType = OrderType.IN_STORE_POS;
    }

    public Order(Integer quantity, BigDecimal orderAmount, LocalDateTime date, Customer customer, Bookstore pickupStore, OrderType orderType) {
        this.quantity = quantity;
        this.orderAmount = orderAmount;
        this.date = date;
        this.customer = customer;
        this.pickupStore = pickupStore;
        this.orderType = orderType != null ? orderType : OrderType.IN_STORE_POS;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Bookstore getPickupStore() {
        return pickupStore;
    }

    public void setPickupStore(Bookstore pickupStore) {
        this.pickupStore = pickupStore;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", quantity=" + quantity +
                ", orderAmount=" + orderAmount +
                ", date=" + date +
                '}';
    }
}
