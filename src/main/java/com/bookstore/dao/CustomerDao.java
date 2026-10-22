package com.bookstore.dao;

import com.bookstore.entity.Customer;
import com.bookstore.entity.CustomerPhone;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Customer entity operations.
 */
public class CustomerDao {

    private static final Logger logger = LoggerFactory.getLogger(CustomerDao.class);

    public Customer save(Customer customer) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(customer);
            tx.commit();
            return customer;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to save customer: " + e.getMessage(), e);
            throw new RuntimeException("Error saving customer", e);
        }
    }

    public Customer save(Session session, Customer customer) {
        session.persist(customer);
        return customer;
    }

    public Optional<Customer> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Customer.class, id));
        }
    }

    public Optional<Customer> findById(Session session, Long id) {
        return Optional.ofNullable(session.get(Customer.class, id));
    }

    public Optional<Customer> findByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return findByEmail(session, email);
        }
    }

    public Optional<Customer> findByEmail(Session session, String email) {
        if (email == null) return Optional.empty();
        return session.createQuery("FROM Customer c WHERE LOWER(c.emailId) = LOWER(:email)", Customer.class)
                .setParameter("email", email.trim())
                .uniqueResultOptional();
    }

    public List<Customer> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Customer c ORDER BY c.customerId ASC", Customer.class)
                    .getResultList();
        }
    }

    public void addPhone(Long customerId, String phoneNo) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Customer customer = session.get(Customer.class, customerId);
            if (customer != null) {
                CustomerPhone phone = new CustomerPhone(customer, phoneNo);
                session.persist(phone);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to add phone for customer " + customerId + ": " + e.getMessage(), e);
            throw new RuntimeException("Error adding customer phone", e);
        }
    }

    public List<String> getPhones(Long customerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT cp.id.phoneNo FROM CustomerPhone cp WHERE cp.customer.customerId = :cId", String.class)
                    .setParameter("cId", customerId)
                    .getResultList();
        }
    }
}
