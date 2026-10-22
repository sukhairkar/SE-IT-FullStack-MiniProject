package com.bookstore.dao;

import com.bookstore.dto.BookSummaryDto;
import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.entity.Publisher;
import com.bookstore.entity.Writes;
import com.bookstore.util.HibernateUtil;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Book entity operations with multi-author management,
 * soft deletion/archiving, and atomic stock adjustment.
 */
public class BookDao {

    private static final Logger logger = LoggerFactory.getLogger(BookDao.class);

    public Book save(Book book) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(book);
            tx.commit();
            return book;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to save book: " + e.getMessage(), e);
            throw new RuntimeException("Error saving book", e);
        }
    }

    public void update(Book book) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(book);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to update book: " + e.getMessage(), e);
            throw new RuntimeException("Error updating book", e);
        }
    }

    /**
     * Soft deletes (archives) a book so historical ORDER_BOOK records remain valid
     * while hiding the book from the active customer POS catalog.
     */
    public boolean archive(Long bookId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Book book = session.get(Book.class, bookId);
            if (book == null) {
                return false;
            }
            book.setActive(false);
            session.merge(book);
            tx.commit();
            logger.info("Successfully archived (soft-deleted) book #{} ('{}')", bookId, book.getTitle());
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to archive book #" + bookId + ": " + e.getMessage(), e);
            throw new RuntimeException("Error archiving book", e);
        }
    }

    public void delete(Long bookId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Book book = session.get(Book.class, bookId);
            if (book != null) {
                // Delete associated writes first
                session.createMutationQuery("DELETE FROM Writes w WHERE w.book.bookId = :bId")
                        .setParameter("bId", bookId)
                        .executeUpdate();
                session.remove(book);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to delete book: " + e.getMessage(), e);
            throw new RuntimeException("Error deleting book", e);
        }
    }

    public Optional<Book> findById(Long bookId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Book book = session.get(Book.class, bookId);
            return Optional.ofNullable(book);
        }
    }

    /**
     * Finds a book within an existing active session.
     */
    public Optional<Book> findById(Session session, Long bookId) {
        Book book = session.get(Book.class, bookId);
        return Optional.ofNullable(book);
    }

    /**
     * Finds a book within an active session with pessimistic write lock for atomic concurrency.
     */
    public Optional<Book> findByIdWithLock(Session session, Long bookId) {
        Book book = session.find(Book.class, bookId, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(book);
    }

    public List<Book> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Book b JOIN FETCH b.publisher ORDER BY b.bookId ASC", Book.class)
                    .getResultList();
        }
    }

    /**
     * Retrieves all active books (for public customer catalog).
     */
    public List<BookSummaryDto> findAllSummaries() {
        return findAllSummaries(false);
    }

    /**
     * Retrieves books enriched with publisher and author names, optionally including archived books.
     */
    public List<BookSummaryDto> findAllSummaries(boolean includeArchived) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = includeArchived
                    ? "FROM Book b JOIN FETCH b.publisher ORDER BY b.bookId ASC"
                    : "FROM Book b JOIN FETCH b.publisher WHERE (b.active = true OR b.active IS NULL) ORDER BY b.bookId ASC";

            List<Book> books = session.createQuery(hql, Book.class).getResultList();

            List<BookSummaryDto> dtoList = new ArrayList<>();
            for (Book b : books) {
                List<String> authors = session.createQuery(
                        "SELECT CONCAT(w.author.firstName, ' ', w.author.lastName) " +
                                "FROM Writes w WHERE w.book.bookId = :bId ORDER BY w.author.authorId ASC", String.class)
                        .setParameter("bId", b.getBookId())
                        .getResultList();

                List<Long> authorIds = session.createQuery(
                        "SELECT w.author.authorId FROM Writes w WHERE w.book.bookId = :bId ORDER BY w.author.authorId ASC", Long.class)
                        .setParameter("bId", b.getBookId())
                        .getResultList();

                BookSummaryDto dto = new BookSummaryDto(
                        b.getBookId(),
                        b.getTitle(),
                        b.getPrice(),
                        b.getStockCount(),
                        b.getGenre(),
                        b.getPublisher() != null ? b.getPublisher().getPublisherId() : null,
                        b.getPublisher() != null ? b.getPublisher().getName() : "Unknown",
                        authors,
                        authorIds,
                        b.isActive()
                );
                dtoList.add(dto);
            }
            return dtoList;
        }
    }

    /**
     * Searches books by title, genre, publisher, or author name.
     */
    public List<BookSummaryDto> search(String keyword) {
        return search(keyword, false);
    }

    /**
     * Searches books with optional archived filter.
     */
    public List<BookSummaryDto> search(String keyword, boolean includeArchived) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllSummaries(includeArchived);
        }
        String cleanKeyword = "%" + keyword.trim().toLowerCase() + "%";
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String activeClause = includeArchived ? "" : "AND (b.active = true OR b.active IS NULL) ";
            String hql = "SELECT DISTINCT b FROM Book b " +
                    "JOIN FETCH b.publisher p " +
                    "WHERE (LOWER(b.title) LIKE :kw " +
                    "OR LOWER(b.genre) LIKE :kw " +
                    "OR LOWER(p.name) LIKE :kw " +
                    "OR b.bookId IN (" +
                    "   SELECT w.book.bookId FROM Writes w " +
                    "   WHERE LOWER(w.author.firstName) LIKE :kw " +
                    "   OR LOWER(w.author.lastName) LIKE :kw" +
                    ")) " + activeClause +
                    "ORDER BY b.bookId ASC";

            List<Book> matchingBooks = session.createQuery(hql, Book.class)
                    .setParameter("kw", cleanKeyword)
                    .getResultList();

            List<BookSummaryDto> result = new ArrayList<>();
            for (Book b : matchingBooks) {
                List<String> authors = session.createQuery(
                        "SELECT CONCAT(w.author.firstName, ' ', w.author.lastName) " +
                                "FROM Writes w WHERE w.book.bookId = :bId ORDER BY w.author.authorId ASC", String.class)
                        .setParameter("bId", b.getBookId())
                        .getResultList();

                List<Long> authorIds = session.createQuery(
                        "SELECT w.author.authorId FROM Writes w WHERE w.book.bookId = :bId ORDER BY w.author.authorId ASC", Long.class)
                        .setParameter("bId", b.getBookId())
                        .getResultList();

                result.add(new BookSummaryDto(
                        b.getBookId(),
                        b.getTitle(),
                        b.getPrice(),
                        b.getStockCount(),
                        b.getGenre(),
                        b.getPublisher() != null ? b.getPublisher().getPublisherId() : null,
                        b.getPublisher() != null ? b.getPublisher().getName() : "Unknown",
                        authors,
                        authorIds,
                        b.isActive()
                ));
            }
            return result;
        }
    }

    /**
     * Returns books where stock count is less than or equal to threshold (e.g. <= 5).
     */
    public List<Book> findLowStock(int threshold) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Book b WHERE b.stockCount <= :th AND (b.active = true OR b.active IS NULL) ORDER BY b.stockCount ASC", Book.class)
                    .setParameter("th", threshold)
                    .getResultList();
        }
    }

    /**
     * Updates the stock count of a specific book.
     */
    public void updateStock(Long bookId, int newStock) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("UPDATE Book b SET b.stockCount = :stock WHERE b.bookId = :id")
                    .setParameter("stock", newStock)
                    .setParameter("id", bookId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to update stock for book " + bookId + ": " + e.getMessage(), e);
            throw new RuntimeException("Error updating stock", e);
        }
    }

    /**
     * Atomically adjusts stock count by delta (e.g. +15 or -2).
     * Returns the updated stock count.
     */
    public int adjustStock(Long bookId, int adjustment) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Book book = session.find(Book.class, bookId, LockModeType.PESSIMISTIC_WRITE);
            if (book == null) {
                throw new IllegalArgumentException("Book not found with ID: " + bookId);
            }
            int current = book.getStockCount() != null ? book.getStockCount() : 0;
            int updated = current + adjustment;
            if (updated < 0) {
                throw new IllegalArgumentException("Adjustment would cause negative stock. Current: " + current + ", adjustment: " + adjustment);
            }
            book.setStockCount(updated);
            session.merge(book);
            tx.commit();
            logger.info("Stock adjusted for book #{}: {} -> {} (delta: {})", bookId, current, updated, adjustment);
            return updated;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rbEx) {
                    logger.warn("Transaction rollback failed: {}", rbEx.getMessage());
                }
            }
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Creates a new book and links one or more authors atomically in a single transaction.
     */
    public Book saveWithAuthors(Book book, List<Long> authorIds) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(book);

            if (authorIds != null && !authorIds.isEmpty()) {
                // Deduplicate author IDs
                LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(authorIds);
                for (Long aId : uniqueIds) {
                    Author author = session.get(Author.class, aId);
                    if (author != null) {
                        Writes writes = new Writes(author, book);
                        session.persist(writes);
                    } else {
                        logger.warn("Author not found with ID {} while creating book #{}", aId, book.getBookId());
                    }
                }
            }
            tx.commit();
            return book;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rbEx) {
                    logger.warn("Transaction rollback failed: {}", rbEx.getMessage());
                }
            }
            logger.error("Failed to save book with authors: " + e.getMessage(), e);
            throw new RuntimeException("Error saving book with authors", e);
        } finally {
            session.close();
        }
    }

    /**
     * Updates an existing book and its author associations atomically in a single transaction.
     */
    public Book updateWithAuthors(Long bookId, String title, BigDecimal price, Integer stockCount,
                                  String genre, Long publisherId, List<Long> authorIds, Boolean active) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Book book = session.get(Book.class, bookId);
            if (book == null) {
                throw new IllegalArgumentException("Book not found with ID: " + bookId);
            }

            if (title != null && !title.isBlank()) {
                book.setTitle(title.trim());
            }
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                book.setPrice(price);
            }
            if (stockCount != null && stockCount >= 0) {
                book.setStockCount(stockCount);
            }
            if (genre != null) {
                book.setGenre(genre.trim());
            }
            if (active != null) {
                book.setActive(active);
            }
            if (publisherId != null) {
                Publisher publisher = session.get(Publisher.class, publisherId);
                if (publisher == null) {
                    throw new IllegalArgumentException("Publisher not found with ID: " + publisherId);
                }
                book.setPublisher(publisher);
            }

            session.merge(book);

            // Update Author Associations if provided
            if (authorIds != null) {
                // Remove existing WRITES for this book
                session.createMutationQuery("DELETE FROM Writes w WHERE w.book.bookId = :bId")
                        .setParameter("bId", bookId)
                        .executeUpdate();

                // Re-insert new WRITES
                LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(authorIds);
                for (Long aId : uniqueIds) {
                    Author author = session.get(Author.class, aId);
                    if (author != null) {
                        Writes writes = new Writes(author, book);
                        session.persist(writes);
                    } else {
                        logger.warn("Author ID {} not found while updating book #{}", aId, bookId);
                    }
                }
            }

            tx.commit();
            return book;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rbEx) {
                    logger.warn("Transaction rollback failed: {}", rbEx.getMessage());
                }
            }
            logger.error("Failed to update book with authors: " + e.getMessage(), e);
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Links an author to a book via the WRITES table.
     */
    public void linkAuthor(Long bookId, Long authorId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Book book = session.get(Book.class, bookId);
            Author author = session.get(Author.class, authorId);
            if (book != null && author != null) {
                Writes writes = new Writes(author, book);
                session.persist(writes);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to link author to book: " + e.getMessage(), e);
            throw new RuntimeException("Error linking author to book", e);
        }
    }
}
