package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload for creating a new publisher.
 * Endpoint: POST /api/owner/publishers
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePublisherRequest {

    private String name;
    private String street;
    private String city;
    private String pincode;
    private String contactNo;

    public CreatePublisherRequest() {
    }

    public CreatePublisherRequest(String name, String street, String city, String pincode, String contactNo) {
        this.name = name;
        this.street = street;
        this.city = city;
        this.pincode = pincode;
        this.contactNo = contactNo;
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

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }
}
