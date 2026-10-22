package com.bookstore.dto;

/**
 * Summary DTO for Author entity.
 */
public class AuthorSummaryDto {

    private Long authorId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String biography;
    private String emailId;

    public AuthorSummaryDto() {
    }

    public AuthorSummaryDto(Long authorId, String firstName, String lastName, String biography, String emailId) {
        this.authorId = authorId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "").trim();
        this.biography = biography;
        this.emailId = emailId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
