package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request payload for creating a new Bookstore branch (POST /api/admin/stores).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateStoreRequest {

    private String name;
    private String street;
    private String city;
    private String pincode;
    private String phone;
    private String email;
    private Double latitude;
    private Double longitude;
    private Boolean active;

    public CreateStoreRequest() {
    }

    public CreateStoreRequest(String name, String street, String city, String pincode, String phone, String email, Double latitude, Double longitude) {
        this.name = name;
        this.street = street;
        this.city = city;
        this.pincode = pincode;
        this.phone = phone;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
