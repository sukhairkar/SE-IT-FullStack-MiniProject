package com.bookstore.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary DTO for Publisher including contact phone numbers.
 */
public class PublisherSummaryDto {

    private Long publisherId;
    private String name;
    private String street;
    private String city;
    private String pincode;
    private List<String> contactNumbers = new ArrayList<>();

    public PublisherSummaryDto() {
    }

    public PublisherSummaryDto(Long publisherId, String name, String street, String city, String pincode, List<String> contactNumbers) {
        this.publisherId = publisherId;
        this.name = name;
        this.street = street;
        this.city = city;
        this.pincode = pincode;
        if (contactNumbers != null) {
            this.contactNumbers = contactNumbers;
        }
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public List<String> getContactNumbers() {
        return contactNumbers;
    }

    public void setContactNumbers(List<String> contactNumbers) {
        this.contactNumbers = contactNumbers != null ? contactNumbers : new ArrayList<>();
    }
}
