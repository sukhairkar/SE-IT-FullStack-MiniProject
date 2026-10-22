package com.bookstore.dao;

import com.bookstore.entity.StoreInventory;
import com.bookstore.entity.StoreInventoryId;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for StoreInventory entity (composite key: store_id, book_id).
 */
public class StoreInventoryDao {

    private static final Logger logger = LoggerFactory.getLogger(StoreInventoryDao.class);

    public StoreInventory save(StoreInventory inventory) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(inventory);
            tx.commit();
            return inventory;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            logger.error("Failed to persist StoreInventory: " + e.getMessage(), e);
            throw new RuntimeException("Error saving StoreInventory", e);
        }
    }

    public StoreInventory update(StoreInventory inventory) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            StoreInventory merged = session.merge(inventory);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            logger.error("Failed to update StoreInventory: " + e.getMessage(), e);
            throw new RuntimeException("Error updating StoreInventory", e);
        }
    }

    public Optional<StoreInventory> findById(StoreInventoryId id) {
        if (id == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(StoreInventory.class, id));
        }
    }

    public Optional<StoreInventory> findByStoreAndBook(Long storeId, Long bookId) {
        if (storeId == null || bookId == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StoreInventory inv = session.createQuery(
                    "FROM StoreInventory si JOIN FETCH si.book JOIN FETCH si.bookstore " +
                    "WHERE si.id.storeId = :sId AND si.id.bookId = :bId", StoreInventory.class)
                    .setParameter("sId", storeId)
                    .setParameter("bId", bookId)
                    .uniqueResult();
            return Optional.ofNullable(inv);
        }
    }

    public List<StoreInventory> findByStoreId(Long storeId) {
        if (storeId == null) return List.of();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM StoreInventory si JOIN FETCH si.book WHERE si.id.storeId = :sId ORDER BY si.book.title ASC", StoreInventory.class)
                    .setParameter("sId", storeId)
                    .getResultList();
        }
    }

    public long countDistinctTitlesByStore(Long storeId) {
        if (storeId == null) return 0L;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(si.id.bookId) FROM StoreInventory si WHERE si.id.storeId = :sId", Long.class)
                    .setParameter("sId", storeId)
                    .uniqueResult();
            return count != null ? count : 0L;
        }
    }

    public long sumStockByStore(Long storeId) {
        if (storeId == null) return 0L;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long sum = session.createQuery(
                    "SELECT COALESCE(SUM(si.stockCount), 0) FROM StoreInventory si WHERE si.id.storeId = :sId", Long.class)
                    .setParameter("sId", storeId)
                    .uniqueResult();
            return sum != null ? sum : 0L;
        }
    }
}
