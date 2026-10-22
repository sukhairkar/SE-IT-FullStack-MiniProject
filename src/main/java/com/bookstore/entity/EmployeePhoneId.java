package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for the EMPLOYEE_PHONE entity.
 */
@Embeddable
public class EmployeePhoneId implements Serializable {

    @Column(name = "Employee_id")
    private Long employeeId;

    @Column(name = "Phone_no", length = 25)
    private String phoneNo;

    public EmployeePhoneId() {
    }

    public EmployeePhoneId(Long employeeId, String phoneNo) {
        this.employeeId = employeeId;
        this.phoneNo = phoneNo;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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
        EmployeePhoneId that = (EmployeePhoneId) o;
        return Objects.equals(employeeId, that.employeeId) &&
                Objects.equals(phoneNo, that.phoneNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, phoneNo);
    }

    @Override
    public String toString() {
        return "EmployeePhoneId{" +
                "employeeId=" + employeeId +
                ", phoneNo='" + phoneNo + '\'' +
                '}';
    }
}
