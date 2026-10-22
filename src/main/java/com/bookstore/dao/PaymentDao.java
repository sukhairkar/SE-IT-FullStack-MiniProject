package com.bookstore.dao;

import com.bookstore.entity.HandledBy;
import com.bookstore.entity.Payment;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Data Access Object for Payment and HandledBy entity operations.
 */
public class PaymentDao {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDao.class);

    public Payment save(Payment payment) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(payment);
            tx.commit();
            return payment;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to save payment: " + e.getMessage(), e);
            throw new RuntimeException("Error saving payment", e);
        }
    }

    public Payment save(Session session, Payment payment) {
        session.persist(payment);
        return payment;
    }

    public void saveHandledBy(Session session, HandledBy handledBy) {
        session.persist(handledBy);
    }

    public Optional<Payment> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Payment.class, id));
        }
    }
}
