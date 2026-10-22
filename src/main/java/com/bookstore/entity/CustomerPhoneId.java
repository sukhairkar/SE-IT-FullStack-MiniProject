package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for the CUSTOMER_PHONE entity.
 */
@Embeddable
public class CustomerPhoneId implements Serializable {

    @Column(name = "Customer_id")
    private Long customerId;

    @Column(name = "Phone_no", length = 25)
    private String phoneNo;

    public CustomerPhoneId() {
    }

    public CustomerPhoneId(Long customerId, String phoneNo) {
        this.customerId = customerId;
        this.phoneNo = phoneNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerPhoneId that = (CustomerPhoneId) o;
        return Objects.equals(customerId, that.customerId) &&
                Objects.equals(phoneNo, that.phoneNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, phoneNo);
    }

    @Override
    public String toString() {
        return "CustomerPhoneId{" +
                "customerId=" + customerId +
                ", phoneNo='" + phoneNo + '\'' +
                '}';
    }
}
