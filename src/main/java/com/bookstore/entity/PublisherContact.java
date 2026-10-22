package com.bookstore.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing the PUBLISHER_CONTACT table.
 * Uses composite primary key (Publisher_id, Contact_no).
 */
@Entity
@Table(name = "PUBLISHER_CONTACT")
public class PublisherContact {

    @EmbeddedId
    private PublisherContactId id;

    @MapsId("publisherId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Publisher_id", nullable = false)
    private Publisher publisher;

    public PublisherContact() {
    }

    public PublisherContact(Publisher publisher, String contactNo) {
        this.publisher = publisher;
        this.id = new PublisherContactId(
                publisher != null ? publisher.getPublisherId() : null,
                contactNo
        );
    }

    public PublisherContact(PublisherContactId id, Publisher publisher) {
        this.id = id;
        this.publisher = publisher;
    }

    public PublisherContactId getId() {
        return id;
    }

    public void setId(PublisherContactId id) {
        this.id = id;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
        if (publisher != null && id != null) {
            id.setPublisherId(publisher.getPublisherId());
        }
    }

    public String getContactNo() {
        return id != null ? id.getContactNo() : null;
    }

    public void setContactNo(String contactNo) {
        if (id == null) {
            id = new PublisherContactId();
        }
        id.setContactNo(contactNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublisherContact that = (PublisherContact) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PublisherContact{" +
                "id=" + id +
                '}';
    }
}
