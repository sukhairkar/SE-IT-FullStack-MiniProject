package com.bookstore.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing the HANDLED_BY table.
 * Associates Employees with Payments via composite primary key (Employee_id, Payment_id).
 */
@Entity
@Table(name = "HANDLED_BY")
public class HandledBy {

    @EmbeddedId
    private HandledById id;

    @MapsId("employeeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Employee_id", nullable = false)
    private Employee employee;

    @MapsId("paymentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Payment_id", nullable = false)
    private Payment payment;

    public HandledBy() {
    }

    public HandledBy(Employee employee, Payment payment) {
        this.employee = employee;
        this.payment = payment;
        this.id = new HandledById(
                employee != null ? employee.getEmployeeId() : null,
                payment != null ? payment.getPaymentId() : null
        );
    }

    public HandledBy(HandledById id, Employee employee, Payment payment) {
        this.id = id;
        this.employee = employee;
        this.payment = payment;
    }

    public HandledById getId() {
        return id;
    }

    public void setId(HandledById id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
        if (employee != null && id != null) {
            id.setEmployeeId(employee.getEmployeeId());
        }
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
        if (payment != null && id != null) {
            id.setPaymentId(payment.getPaymentId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandledBy handledBy = (HandledBy) o;
        return Objects.equals(id, handledBy.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "HandledBy{" +
                "id=" + id +
                '}';
    }
}
