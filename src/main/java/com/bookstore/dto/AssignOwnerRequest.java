package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request payload for assigning or creating a BOOKSTORE_OWNER for a bookstore branch.
 * (POST /api/admin/stores/{id}/assign-owner).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignOwnerRequest {

    private Long userId;
    private String email;
    private String password;
    private String displayName;

    public AssignOwnerRequest() {
    }

    public AssignOwnerRequest(String email) {
        this.email = email;
    }

    public AssignOwnerRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public AssignOwnerRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
