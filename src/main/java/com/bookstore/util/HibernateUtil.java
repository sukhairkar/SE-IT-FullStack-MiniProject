package com.bookstore.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton utility class to initialize and provide access to the Hibernate SessionFactory.
 */
public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            logger.info("Initializing Hibernate SessionFactory using hibernate.cfg.xml...");
            Configuration configuration = new Configuration().configure();
            SessionFactory factory = configuration.buildSessionFactory();
            logger.info("Hibernate SessionFactory initialized successfully.");
            return factory;
        } catch (Throwable ex) {
            logger.error("Initial SessionFactory creation failed: " + ex.getMessage(), ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Get the singleton SessionFactory instance, rebuilding if closed.
     *
     * @return Hibernate SessionFactory
     */
    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    /**
     * Gracefully close the SessionFactory and release database connections.
     */
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            logger.info("Closing Hibernate SessionFactory...");
            sessionFactory.close();
            logger.info("Hibernate SessionFactory closed successfully.");
        }
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
        }
    }
}
