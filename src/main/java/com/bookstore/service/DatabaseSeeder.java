package com.bookstore.service;

import com.bookstore.dao.AuthorDao;
import com.bookstore.dao.BookDao;
import com.bookstore.dao.CustomerDao;
import com.bookstore.dao.EmployeeDao;
import com.bookstore.dao.PublisherDao;
import com.bookstore.entity.*;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utility to seed initial test and demo data into the bookstore database.
 */
public class DatabaseSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final PublisherDao publisherDao = new PublisherDao();
    private final AuthorDao authorDao = new AuthorDao();
    private final BookDao bookDao = new BookDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final EmployeeDao employeeDao = new EmployeeDao();

    /**
     * Seeds initial records if the database is currently empty.
     */
    public synchronized void seedIfEmpty() {
        List<Book> existingBooks = bookDao.findAll();
        if (!existingBooks.isEmpty()) {
            logger.info("Database already contains {} books. Skipping seed.", existingBooks.size());
            ensureBooksActiveIfNull();
            return;
        }

        logger.info("Database is empty. Starting comprehensive data seeding...");

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // 1. Publishers
            Publisher p1 = new Publisher("Penguin Random House", "1745 Broadway", "New York", "10019");
            Publisher p2 = new Publisher("HarperCollins Publishers", "195 Broadway", "New York", "10007");
            Publisher p3 = new Publisher("O'Reilly Media", "1005 Gravenstein Hwy N", "Sebastopol", "95472");
            session.persist(p1);
            session.persist(p2);
            session.persist(p3);

            // Publisher Contacts
            session.persist(new PublisherContact(p1, "+1-212-782-9000"));
            session.persist(new PublisherContact(p2, "+1-212-207-7000"));
            session.persist(new PublisherContact(p3, "+1-707-827-7000"));

            // 2. Authors
            Author a1 = new Author("Robert", "Martin", "American software engineer and author, known as Uncle Bob.", "unclebob@cleancode.org");
            Author a2 = new Author("Joshua", "Bloch", "Former Chief Java Architect at Google and author of Effective Java.", "joshua.bloch@effectivejava.org");
            Author a3 = new Author("George", "Orwell", "English novelist, essayist, journalist and critic.", "george.orwell@classiclit.org");
            Author a4 = new Author("J.K.", "Rowling", "British author, philanthropist, and screenwriter best known for Harry Potter.", "jkrowling@hogwarts.org");
            session.persist(a1);
            session.persist(a2);
            session.persist(a3);
            session.persist(a4);

            // 3. Books (Include items with stock <= 5 for UI alert demonstration)
            Book b1 = new Book("Clean Code: A Handbook of Agile Software Craftsmanship", new BigDecimal("42.50"), 15, "Software Engineering", p3);
            Book b2 = new Book("Effective Java (3rd Edition)", new BigDecimal("49.99"), 8, "Computer Science", p3);
            Book b3 = new Book("1984 (Nineteen Eighty-Four)", new BigDecimal("14.99"), 4, "Dystopian Fiction", p1); // Low stock!
            Book b4 = new Book("Animal Farm", new BigDecimal("11.50"), 20, "Political Satire", p1);
            Book b5 = new Book("Harry Potter and the Sorcerer's Stone", new BigDecimal("24.95"), 3, "Fantasy", p2); // Low stock!
            Book b6 = new Book("The Pragmatic Programmer", new BigDecimal("39.95"), 12, "Software Engineering", p3);

            session.persist(b1);
            session.persist(b2);
            session.persist(b3);
            session.persist(b4);
            session.persist(b5);
            session.persist(b6);

            // 4. Writes (Author-Book Relationships)
            session.persist(new Writes(a1, b1));
            session.persist(new Writes(a2, b2));
            session.persist(new Writes(a3, b3));
            session.persist(new Writes(a3, b4));
            session.persist(new Writes(a4, b5));
            session.persist(new Writes(a1, b6));

            // 5. Employees
            Employee e1 = new Employee("Alice", "Smith", "Cashier", new BigDecimal("38000.00"));
            Employee e2 = new Employee("Bob", "Johnson", "Store Manager", new BigDecimal("55000.00"));
            session.persist(e1);
            session.persist(e2);

            session.persist(new EmployeePhone(e1, "+1-555-0101"));
            session.persist(new EmployeePhone(e2, "+1-555-0102"));

            // 6. Customers
            Customer c1 = new Customer("john.doe@example.com", "John", "Doe", "42 Elm St", "Springfield", "62701");
            Customer c2 = new Customer("jane.roe@example.com", "Jane", "Roe", "742 Evergreen Terrace", "Springfield", "62704");
            session.persist(c1);
            session.persist(c2);

            session.persist(new CustomerPhone(c1, "+1-555-0201"));
            session.persist(new CustomerPhone(c2, "+1-555-0202"));

            // 7. Seed Default RBAC Users
            seedUsers(session, c1, e2);

            tx.commit();
            logger.info("Sample bookstore data and RBAC users seeded successfully!");

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to seed database: " + e.getMessage(), e);
            throw new RuntimeException("Seeding failed", e);
        }
    }

    /**
     * Seeds RBAC users if no users currently exist in MySQL.
     */
    public synchronized void seedUsersIfEmpty() {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("SELECT COUNT(u) FROM User u", Long.class).uniqueResult();
            if (count != null && count > 0) {
                logger.info("RBAC Users already exist in database ({} users). Skipping user seed.", count);
                return;
            }

            logger.info("No RBAC users found. Seeding default accounts (Owner, Admin, Customer)...");
            tx = session.beginTransaction();

            Customer customer = session.createQuery("FROM Customer c WHERE c.emailId = :email", Customer.class)
                    .setParameter("email", "john.doe@example.com")
                    .uniqueResult();

            Employee employee = session.createQuery("FROM Employee e WHERE e.role = :role", Employee.class)
                    .setParameter("role", "Store Manager")
                    .setMaxResults(1)
                    .uniqueResult();

            seedUsers(session, customer, employee);

            tx.commit();
            logger.info("RBAC default accounts seeded successfully with BCrypt hashes.");
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to seed RBAC users: " + e.getMessage(), e);
        }
    }

    private void seedUsers(Session session, Customer customer, Employee employee) {
        String ownerHash = org.mindrot.jbcrypt.BCrypt.hashpw("owner123", org.mindrot.jbcrypt.BCrypt.gensalt(10));
        String adminHash = org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt(10));
        String custHash = org.mindrot.jbcrypt.BCrypt.hashpw("cust123", org.mindrot.jbcrypt.BCrypt.gensalt(10));

        // Owner
        User owner = new User("owner", ownerHash, AppRole.BOOKSTORE_OWNER);
        session.persist(owner);

        // Admin
        User admin = new User("admin", adminHash, AppRole.ADMIN, null, employee);
        session.persist(admin);

        // Customer
        User cust = new User("john@example.com", custHash, AppRole.CUSTOMER, customer, null);
        session.persist(cust);

        logger.info("Created users: 'owner' (BOOKSTORE_OWNER), 'admin' (ADMIN), 'john@example.com' (CUSTOMER)");
    }

    /**
     * Ensures any book record where active column is null gets set to true.
     */
    public synchronized void ensureBooksActiveIfNull() {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("UPDATE Book b SET b.active = true WHERE b.active IS NULL")
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.warn("Could not ensure books active: {}", e.getMessage());
        }
    }

    /**
     * Seeds initial Bookstore branches and assigns default demo owner if no stores exist.
     */
    public synchronized void seedDefaultStoresIfEmpty() {
        com.bookstore.dao.BookstoreDao bookstoreDao = new com.bookstore.dao.BookstoreDao();
        com.bookstore.dao.AppUserDao appUserDao = new com.bookstore.dao.AppUserDao();
        com.bookstore.dao.StoreInventoryDao storeInventoryDao = new com.bookstore.dao.StoreInventoryDao();

        if (bookstoreDao.count() > 0) {
            logger.info("Bookstore branches already exist. Skipping store seeding.");
            return;
        }

        logger.info("Seeding initial Bookstore branches and inventory scoping...");
        Bookstore bandra = new Bookstore(
                "BookHaven Bandra Branch",
                "Linking Road, Bandra West",
                "Mumbai",
                "400050",
                "+91 98200 12345",
                "bandra@bookstore.com",
                19.0596,
                72.8295
        );
        bookstoreDao.save(bandra);

        Bookstore fort = new Bookstore(
                "BookHaven Fort Flagship",
                "Flora Fountain, Fort",
                "Mumbai",
                "400001",
                "+91 98200 67890",
                "fort@bookstore.com",
                18.9322,
                72.8315
        );
        bookstoreDao.save(fort);

        // Bind default owner@bookstore.com to Bandra branch
        appUserDao.findByEmail("owner@bookstore.com").ifPresent(owner -> {
            owner.setAssignedStore(bandra);
            appUserDao.update(owner);
            logger.info("Assigned default account 'owner@bookstore.com' to '{}'", bandra.getName());
        });

        // Seed initial store inventory for existing books
        List<Book> books = bookDao.findAll();
        int count = 0;
        for (Book b : books) {
            String shelf1 = "Aisle " + ((count % 3) + 1) + ", Shelf " + (char) ('A' + (count % 4));
            String shelf2 = "Aisle " + ((count % 4) + 1) + ", Shelf " + (char) ('A' + ((count + 1) % 4));
            storeInventoryDao.save(new StoreInventory(bandra, b, Math.max(5, b.getStockCount()), shelf1));
            storeInventoryDao.save(new StoreInventory(fort, b, Math.max(3, b.getStockCount() - 2), shelf2));
            count++;
        }
        logger.info("Seeded initial StoreInventory for {} titles across 2 bookstore branches.", books.size());
    }
}
