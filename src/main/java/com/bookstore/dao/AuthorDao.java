package com.bookstore.dao;

import com.bookstore.entity.Author;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Author entity operations.
 */
public class AuthorDao {

    private static final Logger logger = LoggerFactory.getLogger(AuthorDao.class);

    public Author save(Author author) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(author);
            tx.commit();
            return author;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to save author: " + e.getMessage(), e);
            throw new RuntimeException("Error saving author", e);
        }
    }

    public Optional<Author> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Author.class, id));
        }
    }

    public Optional<Author> findByEmail(String email) {
        if (email == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Author a WHERE LOWER(a.emailId) = LOWER(:email)", Author.class)
                    .setParameter("email", email.trim())
                    .uniqueResultOptional();
        }
    }

    public List<Author> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Author a ORDER BY a.authorId ASC", Author.class)
                    .getResultList();
        }
    }
}
