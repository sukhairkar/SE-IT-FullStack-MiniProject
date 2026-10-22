package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for the PUBLISHER_CONTACT entity.
 */
@Embeddable
public class PublisherContactId implements Serializable {

    @Column(name = "Publisher_id")
    private Long publisherId;

    @Column(name = "Contact_no", length = 25)
    private String contactNo;

    public PublisherContactId() {
    }

    public PublisherContactId(Long publisherId, String contactNo) {
        this.publisherId = publisherId;
        this.contactNo = contactNo;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublisherContactId that = (PublisherContactId) o;
        return Objects.equals(publisherId, that.publisherId) &&
                Objects.equals(contactNo, that.contactNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publisherId, contactNo);
    }

    @Override
    public String toString() {
        return "PublisherContactId{" +
                "publisherId=" + publisherId +
                ", contactNo='" + contactNo + '\'' +
                '}';
    }
}
