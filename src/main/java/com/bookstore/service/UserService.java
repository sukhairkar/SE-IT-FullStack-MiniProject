package com.bookstore.service;

import com.bookstore.dao.AppUserDao;
import com.bookstore.entity.AppRole;
import com.bookstore.entity.AppUser;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service managing user registration, authentication, password hashing, and demo account seeding.
 */
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final AppUserDao appUserDao;

    public UserService() {
        this(new AppUserDao());
    }

    public UserService(AppUserDao appUserDao) {
        this.appUserDao = appUserDao;
    }

    /**
     * Registers a new customer user account.
     *
     * @param email valid customer email
     * @param rawPassword plain-text password
     * @return persisted AppUser with role CUSTOMER
     */
    public AppUser register(String email, String rawPassword) {
        return registerWithRole(email, rawPassword, AppRole.CUSTOMER);
    }

    /**
     * Registers a user account with a designated role.
     */
    public AppUser registerWithRole(String email, String rawPassword, AppRole role) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        if (appUserDao.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with email '" + cleanEmail + "' is already registered.");
        }

        String salt = BCrypt.gensalt(12);
        String passwordHash = BCrypt.hashpw(rawPassword, salt);

        AppUser user = new AppUser(cleanEmail, passwordHash, role != null ? role : AppRole.CUSTOMER);
        AppUser saved = appUserDao.save(user);
        logger.info("Successfully registered user '{}' with role '{}'", saved.getEmail(), saved.getRole());
        return saved;
    }

    /**
     * Authenticates a user by email and password.
     *
     * @param email user email address
     * @param rawPassword plain-text password
     * @return authenticated AppUser
     * @throws IllegalArgumentException on invalid credentials
     */
    public AppUser authenticate(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        String cleanEmail = email.trim().toLowerCase();
        Optional<AppUser> userOpt = appUserDao.findByEmail(cleanEmail);

        if (userOpt.isEmpty()) {
            logger.warn("Authentication failed: User with email '{}' not found.", cleanEmail);
            throw new IllegalArgumentException("Invalid email or password.");
        }

        AppUser user = userOpt.get();
        boolean matches = false;
        try {
            matches = BCrypt.checkpw(rawPassword, user.getPasswordHash());
        } catch (Exception e) {
            logger.error("Error evaluating BCrypt hash for user '{}': {}", cleanEmail, e.getMessage());
        }

        if (!matches) {
            logger.warn("Authentication failed: Incorrect password for user '{}'.", cleanEmail);
            throw new IllegalArgumentException("Invalid email or password.");
        }

        logger.info("Authentication succeeded for user '{}' (Role: {})", user.getEmail(), user.getRole());
        return user;
    }

    public Optional<AppUser> findByEmail(String email) {
        return appUserDao.findByEmail(email);
    }

    public Optional<AppUser> findById(Long id) {
        return appUserDao.findById(id);
    }

    /**
     * Seeds default demo accounts if absent:
     * - owner@bookstore.com / Owner@123 (Role: BOOKSTORE_OWNER)
     * - admin@bookstore.com / Admin@123 (Role: ADMIN)
     * - reader@gmail.com / Reader@123 (Role: CUSTOMER)
     */
    public void seedDefaultUsersIfAbsent() {
        logger.info("Checking default AppUser accounts...");

        seedUserIfAbsent("owner@bookstore.com", "Owner@123", AppRole.BOOKSTORE_OWNER);
        seedUserIfAbsent("admin@bookstore.com", "Admin@123", AppRole.ADMIN);
        seedUserIfAbsent("reader@gmail.com", "Reader@123", AppRole.CUSTOMER);

        // Also ensure backward-compatible demo accounts from Phase 5 are mapped if desired
        seedUserIfAbsent("owner", "owner123", AppRole.BOOKSTORE_OWNER);
        seedUserIfAbsent("admin", "admin123", AppRole.ADMIN);
        seedUserIfAbsent("john@example.com", "cust123", AppRole.CUSTOMER);

        logger.info("AppUser demo accounts verified.");
    }

    private void seedUserIfAbsent(String emailOrUsername, String password, AppRole role) {
        if (!appUserDao.existsByEmail(emailOrUsername)) {
            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            AppUser user = new AppUser(emailOrUsername.toLowerCase().trim(), hash, role);
            appUserDao.save(user);
            logger.info("Seeded default account: '{}' with role '{}'", emailOrUsername, role);
        }
    }
}
