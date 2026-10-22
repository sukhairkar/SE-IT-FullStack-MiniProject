package com.bookstore.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing the CUSTOMER_PHONE table.
 * Uses composite primary key (Customer_id, Phone_no).
 */
@Entity
@Table(name = "CUSTOMER_PHONE")
public class CustomerPhone {

    @EmbeddedId
    private CustomerPhoneId id;

    @MapsId("customerId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Customer_id", nullable = false)
    private Customer customer;

    public CustomerPhone() {
    }

    public CustomerPhone(Customer customer, String phoneNo) {
        this.customer = customer;
        this.id = new CustomerPhoneId(
                customer != null ? customer.getCustomerId() : null,
                phoneNo
        );
    }

    public CustomerPhone(CustomerPhoneId id, Customer customer) {
        this.id = id;
        this.customer = customer;
    }

    public CustomerPhoneId getId() {
        return id;
    }

    public void setId(CustomerPhoneId id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null && id != null) {
            id.setCustomerId(customer.getCustomerId());
        }
    }

    public String getPhoneNo() {
        return id != null ? id.getPhoneNo() : null;
    }

    public void setPhoneNo(String phoneNo) {
        if (id == null) {
            id = new CustomerPhoneId();
        }
        id.setPhoneNo(phoneNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerPhone that = (CustomerPhone) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CustomerPhone{" +
                "id=" + id +
                '}';
    }
}
