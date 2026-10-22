package com.bookstore.dao;

import com.bookstore.entity.Publisher;
import com.bookstore.entity.PublisherContact;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Publisher entity operations.
 */
public class PublisherDao {

    private static final Logger logger = LoggerFactory.getLogger(PublisherDao.class);

    public Publisher save(Publisher publisher) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(publisher);
            tx.commit();
            return publisher;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to save publisher: " + e.getMessage(), e);
            throw new RuntimeException("Error saving publisher", e);
        }
    }

    public Optional<Publisher> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Publisher.class, id));
        }
    }

    public List<Publisher> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Publisher p ORDER BY p.publisherId ASC", Publisher.class)
                    .getResultList();
        }
    }

    public void addContact(Long publisherId, String contactNo) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Publisher publisher = session.get(Publisher.class, publisherId);
            if (publisher != null) {
                PublisherContact contact = new PublisherContact(publisher, contactNo);
                session.persist(contact);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to add publisher contact: " + e.getMessage(), e);
            throw new RuntimeException("Error adding publisher contact", e);
        }
    }

    public List<String> getContacts(Long publisherId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT pc.id.contactNo FROM PublisherContact pc WHERE pc.publisher.publisherId = :pId", String.class)
                    .setParameter("pId", publisherId)
                    .getResultList();
        }
    }
}
