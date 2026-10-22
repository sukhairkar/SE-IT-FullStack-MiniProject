package com.bookstore.dao;

import com.bookstore.entity.AppUser;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for AppUser entity.
 */
public class AppUserDao {

    private static final Logger logger = LoggerFactory.getLogger(AppUserDao.class);

    public AppUser save(AppUser user) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(user);
            tx.commit();
            return user;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            logger.error("Failed to persist AppUser: " + e.getMessage(), e);
            throw new RuntimeException("Error saving AppUser", e);
        }
    }

    public Optional<AppUser> findByEmail(String email) {
        if (email == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            AppUser user = session.createQuery(
                    "FROM AppUser u LEFT JOIN FETCH u.assignedStore WHERE LOWER(u.email) = LOWER(:email)", AppUser.class)
                    .setParameter("email", email.trim())
                    .uniqueResult();
            return Optional.ofNullable(user);
        }
    }

    public Optional<AppUser> findById(Long id) {
        if (id == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            AppUser user = session.createQuery(
                    "FROM AppUser u LEFT JOIN FETCH u.assignedStore WHERE u.id = :id", AppUser.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return Optional.ofNullable(user);
        }
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(u.id) FROM AppUser u", Long.class)
                    .uniqueResult();
            return count != null ? count : 0L;
        }
    }

    public AppUser update(AppUser user) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            AppUser merged = session.merge(user);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            logger.error("Failed to update AppUser: " + e.getMessage(), e);
            throw new RuntimeException("Error updating AppUser", e);
        }
    }

    public Optional<AppUser> findByAssignedStoreId(Long storeId) {
        if (storeId == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            AppUser user = session.createQuery(
                    "FROM AppUser u LEFT JOIN FETCH u.assignedStore WHERE u.assignedStore.storeId = :storeId", AppUser.class)
                    .setParameter("storeId", storeId)
                    .uniqueResult();
            return Optional.ofNullable(user);
        }
    }

    public List<AppUser> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM AppUser u LEFT JOIN FETCH u.assignedStore ORDER BY u.id ASC", AppUser.class)
                    .getResultList();
        }
    }
}
