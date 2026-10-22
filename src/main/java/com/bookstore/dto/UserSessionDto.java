package com.bookstore.dto;

import com.bookstore.entity.AppRole;

import java.io.Serializable;

/**
 * DTO representing an authenticated user session.
 */
public class UserSessionDto implements Serializable {

    private Long userId;
    private String username;
    private String email;
    private AppRole role;
    private Long customerId;
    private Long employeeId;
    private String displayName;
    private String token;
    private Long assignedStoreId;
    private String assignedStoreName;

    public UserSessionDto() {
    }

    public UserSessionDto(Long userId, String username, AppRole role, Long customerId, Long employeeId, String displayName, String token) {
        this.userId = userId;
        this.username = username;
        this.email = username;
        this.role = role;
        this.customerId = customerId;
        this.employeeId = employeeId;
        this.displayName = displayName;
        this.token = token;
    }

    public UserSessionDto(Long userId, String username, AppRole role, Long customerId, Long employeeId, String displayName, String token, Long assignedStoreId, String assignedStoreName) {
        this.userId = userId;
        this.username = username;
        this.email = username;
        this.role = role;
        this.customerId = customerId;
        this.employeeId = employeeId;
        this.displayName = displayName;
        this.token = token;
        this.assignedStoreId = assignedStoreId;
        this.assignedStoreName = assignedStoreName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        if (this.email == null) this.email = username;
    }

    public String getEmail() {
        return email != null ? email : username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AppRole getRole() {
        return role;
    }

    public void setRole(AppRole role) {
        this.role = role;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getAssignedStoreId() {
        return assignedStoreId;
    }

    public void setAssignedStoreId(Long assignedStoreId) {
        this.assignedStoreId = assignedStoreId;
    }

    public String getAssignedStoreName() {
        return assignedStoreName;
    }

    public void setAssignedStoreName(String assignedStoreName) {
        this.assignedStoreName = assignedStoreName;
    }
}
