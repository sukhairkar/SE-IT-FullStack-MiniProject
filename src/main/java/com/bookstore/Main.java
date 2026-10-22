package com.bookstore;

import com.bookstore.controller.*;
import com.bookstore.dao.AppUserDao;
import com.bookstore.entity.AppRole;
import com.bookstore.service.DatabaseSeeder;
import com.bookstore.service.UserService;
import com.bookstore.util.HibernateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.security.RouteRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Main application launcher and route wiring for the Bookstore Management System.
 * Configures Javalin 6 with robust email+password authentication, RBAC session guards,
 * OpenPDF digital invoice streaming, and asynchronous receipt email dispatch.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static final int DEFAULT_PORT = 7070;

    private final Javalin app;
    private final int port;
    private final UserService userService;

    public Main() {
        this(DEFAULT_PORT);
    }

    public Main(int port) {
        this.port = port;
        this.userService = new UserService();

        // 1. Configure Jackson ObjectMapper with JavaTime support for ISO timestamps
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.app = Javalin.create(config -> {
            // Serve static files from classpath /public
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });

            // Set customized Jackson JSON mapper
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

        configureSecurityGuards();
        configureRoutes();
    }

    /**
     * Configures Javalin route security guards in app.beforeMatched(...).
     * Enforces that protected /api/admin/* and /api/inventory/* routes return 401/403,
     * and evaluates route-level RouteRole constraints.
     */
    private void configureSecurityGuards() {
        // Global CORS headers
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });
        app.options("/*", ctx -> ctx.status(HttpStatus.OK));

        // Security Guard filter
        app.beforeMatched(ctx -> {
            String path = ctx.path();

            // 1. Path-based guards for /api/admin/*, /api/inventory/*, and /api/owner/*
            boolean isAdminRoute = path.startsWith("/api/admin/") || path.equals("/api/admin");
            boolean isInventoryRoute = path.startsWith("/api/inventory/") || path.equals("/api/inventory");
            boolean isOwnerRoute = path.startsWith("/api/owner/") || path.equals("/api/owner");

            if (isAdminRoute || isInventoryRoute || isOwnerRoute) {
                var user = AuthController.resolveUser(ctx);
                if (user == null) {
                    logger.warn("Unauthorized attempt to access restricted path: {}", path);
                    ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of(
                            "success", false,
                            "error", "UNAUTHORIZED",
                            "message", "Authentication required to access " + path
                    ));
                    ctx.skipRemainingHandlers();
                    return;
                }

                if (isAdminRoute && user.getRole() != AppRole.ADMIN) {
                    logger.warn("Forbidden access attempt by '{}' (role: {}) to admin path: {}", user.getUsername(), user.getRole(), path);
                    ctx.status(HttpStatus.FORBIDDEN).json(Map.of(
                            "success", false,
                            "error", "FORBIDDEN",
                            "message", "Access denied. Admin privileges required."
                    ));
                    ctx.skipRemainingHandlers();
                    return;
                }

                if ((isInventoryRoute || isOwnerRoute) && user.getRole() != AppRole.BOOKSTORE_OWNER && user.getRole() != AppRole.ADMIN) {
                    logger.warn("Forbidden access attempt by '{}' (role: {}) to owner/inventory path: {}", user.getUsername(), user.getRole(), path);
                    ctx.status(HttpStatus.FORBIDDEN).json(Map.of(
                            "success", false,
                            "error", "FORBIDDEN",
                            "message", "Access denied. Owner operations require Bookstore Owner or Admin role."
                    ));
                    ctx.skipRemainingHandlers();
                    return;
                }
            }

            // 2. RouteRole-based constraints
            Set<RouteRole> routeRoles = ctx.routeRoles();
            if (routeRoles.isEmpty() || routeRoles.contains(AppRole.ANYONE)) {
                return; // Public endpoint
            }

            var user = AuthController.resolveUser(ctx);
            if (user == null) {
                logger.warn("Unauthorized access attempt to role-protected route: {}", path);
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of(
                        "success", false,
                        "error", "UNAUTHORIZED",
                        "message", "Authentication required. Please log in at /api/auth/login"
                ));
                ctx.skipRemainingHandlers();
                return;
            }

            AppRole userRole = user.getRole();
            boolean isAuthorized = (userRole == AppRole.ADMIN) || routeRoles.contains(userRole);
            if (!isAuthorized) {
                logger.warn("Forbidden access attempt by '{}' (role: {}) to route '{}' requiring {}",
                        user.getUsername(), userRole, path, routeRoles);
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of(
                        "success", false,
                        "error", "FORBIDDEN",
                        "message", "Access denied. Required role: " + routeRoles + ", but your role is " + userRole
                ));
                ctx.skipRemainingHandlers();
            }
        });
    }

    /**
     * Configures REST API route wiring.
     */
    private void configureRoutes() {
        // Controllers
        AuthController authController = new AuthController(userService);
        BookController bookController = new BookController();
        CheckoutController checkoutController = new CheckoutController();
        OrderController orderController = new OrderController();
        CustomerController customerController = new CustomerController();
        EmployeeController employeeController = new EmployeeController();
        OwnerController ownerController = new OwnerController();
        HoldController holdController = new HoldController();

        // 1. Health Check
        app.get("/api/health", ctx -> ctx.json(Map.of(
                "status", "UP",
                "service", "Bookstore Management System",
                "timestamp", Instant.now().toString()
        )), AppRole.ANYONE);

        // 2. Authentication Endpoints (Public)
        app.post("/api/auth/register", authController::register, AppRole.ANYONE);
        app.post("/api/auth/login", authController::login, AppRole.ANYONE);
        app.post("/api/auth/logout", authController::logout, AppRole.ANYONE);
        app.get("/api/auth/me", authController::me, AppRole.ANYONE);

        // Phase 10: Owner Catalog & Lifecycle Management Endpoints
        app.post("/api/owner/books", ownerController::createBook, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.get("/api/owner/books", ownerController::getBooks, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.get("/api/owner/books/{id}", ownerController::getBookById, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.put("/api/owner/books/{id}", ownerController::updateBook, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.patch("/api/owner/books/{id}/stock", ownerController::adjustStock, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.delete("/api/owner/books/{id}", ownerController::archiveBook, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);

        app.get("/api/owner/publishers", ownerController::getPublishers, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.post("/api/owner/publishers", ownerController::createPublisher, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);

        app.get("/api/owner/authors", ownerController::getAuthors, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.post("/api/owner/authors", ownerController::createAuthor, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);

        // Phase 11: Owner Overview Metrics & Order History Audit
        app.get("/api/owner/metrics", ownerController::getMetrics, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.get("/api/owner/orders", ownerController::getOrders, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.post("/api/owner/orders/{id}/resend-receipt", ownerController::resendReceipt, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);

        // 3. Protected /api/inventory/* Routes (Requires BOOKSTORE_OWNER or ADMIN)
        app.get("/api/inventory/books", bookController::getAll, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.post("/api/inventory/books", bookController::create, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.put("/api/inventory/restock/{id}", bookController::restock, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);

        // 4. Protected /api/admin/* Routes (Requires ADMIN)
        app.get("/api/admin/employees", employeeController::getAll, AppRole.ADMIN);
        app.get("/api/admin/users", ctx -> ctx.json(new AppUserDao().findAll()), AppRole.ADMIN);
        app.get("/api/admin/system", ctx -> ctx.json(Map.of(
                "status", "ACTIVE",
                "service", "Bookstore Management System",
                "javaVersion", System.getProperty("java.version"),
                "totalUsers", new AppUserDao().count()
        )), AppRole.ADMIN);

        // 5. Book Catalog Routes
        app.get("/api/books", bookController::getAll, AppRole.ANYONE);
        app.get("/api/books/{id}", bookController::getById, AppRole.ANYONE);
        app.post("/api/books", bookController::create, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.put("/api/books/{id}/restock", bookController::restock, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);

        // 6. Checkout Route (Atomic Multi-Table Transaction with Async Email Dispatch)
        app.post("/api/checkout", checkoutController::checkout, AppRole.ANYONE);

        // 6b. In-Store Shelf Hold & Pickup Reservation Endpoints
        app.post("/api/holds", holdController::createHold, AppRole.ANYONE);
        app.get("/api/holds", holdController::getHolds, AppRole.ANYONE);
        app.post("/api/holds/{id}/fulfill", holdController::fulfillHold, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.delete("/api/holds/{id}", holdController::cancelHold, AppRole.ANYONE);

        // 7. Order & OpenPDF Receipt Endpoints
        app.get("/api/orders", orderController::getAll, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN, AppRole.CUSTOMER);
        app.get("/api/orders/{id}", orderController::getById, AppRole.ANYONE);
        app.get("/api/orders/{id}/receipt", orderController::getReceiptPdf, AppRole.ANYONE);

        // 8. Customer & Employee Endpoints
        app.get("/api/customers", customerController::getAll, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
        app.post("/api/customers", customerController::create, AppRole.ANYONE);
        app.get("/api/employees", employeeController::getAll, AppRole.ADMIN);

        // 9. Dashboard Sales Summary Analytics
        app.get("/api/dashboard/sales-summary", orderController::getSalesSummary, AppRole.BOOKSTORE_OWNER, AppRole.ADMIN);
    }

    public void start() {
        try {
            logger.info("Initializing relational schema and seeding demo accounts...");
            DatabaseSeeder seeder = new DatabaseSeeder();
            seeder.seedIfEmpty();
            seeder.ensureBooksActiveIfNull();
            seeder.seedUsersIfEmpty();

            // Seed default AppUser email accounts
            userService.seedDefaultUsersIfAbsent();
        } catch (Exception e) {
            logger.warn("Database initialization warning: " + e.getMessage());
        }

        app.start(port);
        logger.info("================================================================================");
        logger.info(" BOOKSTORE MANAGEMENT SYSTEM EMBEDDED SERVER STARTED (AUTH & RBAC ACTIVE)");
        logger.info(" Web Application: http://localhost:{}", port);
        logger.info(" REST API Base:   http://localhost:{}/api/books", port);
        logger.info(" Health Check:    http://localhost:{}/api/health", port);
        logger.info("================================================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping Javalin server and releasing database resources...");
            app.stop();
            HibernateUtil.shutdown();
        }));
    }

    public void stop() {
        app.stop();
    }

    public Javalin getJavalin() {
        return app;
    }

    public static void main(String[] args) {
        new Main(DEFAULT_PORT).start();
    }
}
