package com.bookstore.dao;

import com.bookstore.entity.Bookstore;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Bookstore entity operations.
 */
public class BookstoreDao {

    private static final Logger logger = LoggerFactory.getLogger(BookstoreDao.class);

    public Bookstore save(Bookstore store) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(store);
            tx.commit();
            return store;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            logger.error("Failed to persist Bookstore: " + e.getMessage(), e);
            throw new RuntimeException("Error saving Bookstore", e);
        }
    }

    public Bookstore update(Bookstore store) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Bookstore merged = session.merge(store);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            logger.error("Failed to update Bookstore: " + e.getMessage(), e);
            throw new RuntimeException("Error updating Bookstore", e);
        }
    }

    public Optional<Bookstore> findById(Long id) {
        if (id == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Bookstore.class, id));
        }
    }

    public Optional<Bookstore> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Bookstore store = session.createQuery(
                    "FROM Bookstore b WHERE LOWER(b.name) = LOWER(:name)", Bookstore.class)
                    .setParameter("name", name.trim())
                    .uniqueResult();
            return Optional.ofNullable(store);
        }
    }

    public List<Bookstore> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Bookstore b ORDER BY b.storeId ASC", Bookstore.class)
                    .getResultList();
        }
    }

    public List<Bookstore> findActiveStores() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Bookstore b WHERE b.active = true ORDER BY b.storeId ASC", Bookstore.class)
                    .getResultList();
        }
    }

    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(b.storeId) FROM Bookstore b", Long.class)
                    .uniqueResult();
            return count != null ? count : 0L;
        }
    }
}
