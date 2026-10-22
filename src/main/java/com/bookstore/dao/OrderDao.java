package com.bookstore.dao;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderBook;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Order and OrderBook entity operations.
 */
public class OrderDao {

    private static final Logger logger = LoggerFactory.getLogger(OrderDao.class);

    public Order save(Order order) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(order);
            tx.commit();
            return order;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to save order: " + e.getMessage(), e);
            throw new RuntimeException("Error saving order", e);
        }
    }

    public Order save(Session session, Order order) {
        session.persist(order);
        return order;
    }

    public void saveOrderBook(Session session, OrderBook orderBook) {
        session.persist(orderBook);
    }

    public Optional<Order> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Order order = session.createQuery(
                    "FROM Order o JOIN FETCH o.customer WHERE o.orderId = :id", Order.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return Optional.ofNullable(order);
        }
    }

    public List<Order> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Order o JOIN FETCH o.customer ORDER BY o.date DESC", Order.class)
                    .getResultList();
        }
    }

    public List<OrderBook> findOrderBooks(Long orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM OrderBook ob JOIN FETCH ob.book WHERE ob.order.orderId = :oId", OrderBook.class)
                    .setParameter("oId", orderId)
                    .getResultList();
        }
    }

    public List<Order> findRecentOrders(int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Order o JOIN FETCH o.customer ORDER BY o.date DESC", Order.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    public List<Order> findOrdersByCustomerId(Long customerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Order o JOIN FETCH o.customer WHERE o.customer.customerId = :cId ORDER BY o.date DESC", Order.class)
                    .setParameter("cId", customerId)
                    .getResultList();
        }
    }

    public long countOrdersByStoreId(Long storeId) {
        if (storeId == null) return 0L;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(o.orderId) FROM Order o WHERE o.pickupStore.storeId = :sId", Long.class)
                    .setParameter("sId", storeId)
                    .uniqueResult();
            return count != null ? count : 0L;
        }
    }

    public java.math.BigDecimal sumRevenueByStoreId(Long storeId) {
        if (storeId == null) return java.math.BigDecimal.ZERO;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            java.math.BigDecimal sum = session.createQuery(
                    "SELECT COALESCE(SUM(o.orderAmount), 0) FROM Order o WHERE o.pickupStore.storeId = :sId", java.math.BigDecimal.class)
                    .setParameter("sId", storeId)
                    .uniqueResult();
            return sum != null ? sum : java.math.BigDecimal.ZERO;
        }
    }
}
