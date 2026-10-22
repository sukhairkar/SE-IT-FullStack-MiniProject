package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for the HANDLED_BY entity.
 */
@Embeddable
public class HandledById implements Serializable {

    @Column(name = "Employee_id")
    private Long employeeId;

    @Column(name = "Payment_id")
    private Long paymentId;

    public HandledById() {
    }

    public HandledById(Long employeeId, Long paymentId) {
        this.employeeId = employeeId;
        this.paymentId = paymentId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandledById that = (HandledById) o;
        return Objects.equals(employeeId, that.employeeId) &&
                Objects.equals(paymentId, that.paymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, paymentId);
    }

    @Override
    public String toString() {
        return "HandledById{" +
                "employeeId=" + employeeId +
                ", paymentId=" + paymentId +
                '}';
    }
}
