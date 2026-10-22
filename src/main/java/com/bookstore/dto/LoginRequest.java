package com.bookstore.dto;

/**
 * Request payload for POST /api/auth/login.
 */
public class LoginRequest {

    private String username;
    private String email;
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String emailOrUsername, String password) {
        this.email = emailOrUsername;
        this.username = emailOrUsername;
        this.password = password;
    }

    public String getEmail() {
        return email != null && !email.isBlank() ? email : username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username != null && !username.isBlank() ? username : email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
