package com.bookstore.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity representing the WRITES table.
 * Associates Authors with Books through a composite primary key (Author_id, Book_id).
 */
@Entity
@Table(name = "WRITES")
public class Writes {

    @EmbeddedId
    private WritesId id;

    @MapsId("authorId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Author_id", nullable = false)
    private Author author;

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Book_id", nullable = false)
    private Book book;

    public Writes() {
    }

    public Writes(Author author, Book book) {
        this.author = author;
        this.book = book;
        this.id = new WritesId(
                author != null ? author.getAuthorId() : null,
                book != null ? book.getBookId() : null
        );
    }

    public Writes(WritesId id, Author author, Book book) {
        this.id = id;
        this.author = author;
        this.book = book;
    }

    public WritesId getId() {
        return id;
    }

    public void setId(WritesId id) {
        this.id = id;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
        if (author != null && id != null) {
            id.setAuthorId(author.getAuthorId());
        }
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
        if (book != null && id != null) {
            id.setBookId(book.getBookId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Writes writes = (Writes) o;
        return Objects.equals(id, writes.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Writes{" +
                "id=" + id +
                '}';
    }
}
