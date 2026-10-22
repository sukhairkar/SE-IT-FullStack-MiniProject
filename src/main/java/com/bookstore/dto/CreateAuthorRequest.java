package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload for creating a new author.
 * Endpoint: POST /api/owner/authors
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateAuthorRequest {

    private String firstName;
    private String lastName;
    private String biography;
    private String emailId;

    public CreateAuthorRequest() {
    }

    public CreateAuthorRequest(String firstName, String lastName, String biography, String emailId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.biography = biography;
        this.emailId = emailId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }
}
