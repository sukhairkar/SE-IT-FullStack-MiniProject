package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key for the WRITES entity.
 */
@Embeddable
public class WritesId implements Serializable {

    @Column(name = "Author_id")
    private Long authorId;

    @Column(name = "Book_id")
    private Long bookId;

    public WritesId() {
    }

    public WritesId(Long authorId, Long bookId) {
        this.authorId = authorId;
        this.bookId = bookId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WritesId writesId = (WritesId) o;
        return Objects.equals(authorId, writesId.authorId) &&
                Objects.equals(bookId, writesId.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorId, bookId);
    }

    @Override
    public String toString() {
        return "WritesId{" +
                "authorId=" + authorId +
                ", bookId=" + bookId +
                '}';
    }
}
