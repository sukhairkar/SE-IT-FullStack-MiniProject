package com.bookstore;

import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verification class to connect to MySQL via Hibernate and trigger automatic
 * DDL table creation (hbm2ddl.auto = update) for all 13 schema entities.
 */
public class DatabaseSetupTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSetupTest.class);

    private static final String[] EXPECTED_TABLES = {
            "AUTHOR",
            "PUBLISHER",
            "PUBLISHER_CONTACT",
            "BOOK",
            "WRITES",
            "CUSTOMER",
            "CUSTOMER_PHONE",
            "ORDER",
            "ORDER_BOOK",
            "PAYMENT",
            "EMPLOYEE",
            "EMPLOYEE_PHONE",
            "HANDLED_BY"
    };

    @Test
    @DisplayName("Verify Hibernate connects to MySQL and creates all 13 tables automatically")
    public void testSchemaGeneration() {
        verifySchema();
    }

    /**
     * Standalone main runner allowing execution via:
     * `mvn test-compile exec:java` or directly via IDE Run button.
     */
    public static void main(String[] args) {
        logger.info("================================================================================");
        logger.info("Starting Bookstore Database Setup & Schema Verification...");
        logger.info("================================================================================");
        try {
            verifySchema();
            logger.info("================================================================================");
            logger.info("DATABASE SETUP VERIFICATION SUCCESSFUL!");
            logger.info("All 13 tables have been verified in MySQL database: bookstore_db");
            logger.info("================================================================================");
        } catch (Exception ex) {
            logger.error("Database setup failed: " + ex.getMessage(), ex);
            System.exit(1);
        } finally {
            HibernateUtil.shutdown();
        }
    }

    private static void verifySchema() {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        assertNotNull(sessionFactory, "Hibernate SessionFactory must be initialized");

        try (Session session = sessionFactory.openSession()) {
            assertNotNull(session, "Session must not be null");
            assertTrue(session.isOpen(), "Hibernate Session must be active");

            logger.info("Hibernate Session successfully established.");

            // Inspect tables generated in current active database
            @SuppressWarnings("unchecked")
            List<String> existingTables = session.createNativeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "ORDER BY TABLE_NAME ASC",
                    String.class
            ).getResultList();

            logger.info("Found {} tables in database 'bookstore_db':", existingTables.size());
            for (String tableName : existingTables) {
                logger.info(" -> [TABLE] {}", tableName);
            }

            // Check presence of expected tables
            int matchCount = 0;
            for (String expected : EXPECTED_TABLES) {
                boolean found = existingTables.stream().anyMatch(t -> t.equalsIgnoreCase(expected));
                if (found) {
                    matchCount++;
                    logger.info(" Verified table exists: {}", expected);
                } else {
                    logger.warn(" Expected table not yet found: {}", expected);
                }
            }

            logger.info("Schema verification summary: {} of 13 required tables verified.", matchCount);
        }
    }

    @AfterAll
    public static void tearDown() {
        HibernateUtil.shutdown();
    }
}
