package com.bookstore.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing the EMPLOYEE_PHONE table.
 * Uses composite primary key (Employee_id, Phone_no).
 */
@Entity
@Table(name = "EMPLOYEE_PHONE")
public class EmployeePhone {

    @EmbeddedId
    private EmployeePhoneId id;

    @MapsId("employeeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Employee_id", nullable = false)
    private Employee employee;

    public EmployeePhone() {
    }

    public EmployeePhone(Employee employee, String phoneNo) {
        this.employee = employee;
        this.id = new EmployeePhoneId(
                employee != null ? employee.getEmployeeId() : null,
                phoneNo
        );
    }

    public EmployeePhone(EmployeePhoneId id, Employee employee) {
        this.id = id;
        this.employee = employee;
    }

    public EmployeePhoneId getId() {
        return id;
    }

    public void setId(EmployeePhoneId id) {
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

    public String getPhoneNo() {
        return id != null ? id.getPhoneNo() : null;
    }

    public void setPhoneNo(String phoneNo) {
        if (id == null) {
            id = new EmployeePhoneId();
        }
        id.setPhoneNo(phoneNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeePhone that = (EmployeePhone) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EmployeePhone{" +
                "id=" + id +
                '}';
    }
}
