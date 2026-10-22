package com.bookstore;

import com.bookstore.controller.*;
import com.bookstore.service.DatabaseSeeder;
import com.bookstore.util.HibernateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Main application launcher for the Bookstore Management System.
 * Starts an embedded Javalin web server on port 7070 serving:
 * 1. REST API endpoints under /api/*
 * 2. Static HTML/CSS/JS frontend files from /public
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    public static final int DEFAULT_PORT = 7070;

    private final Javalin javalinApp;
    private final int port;

    public App() {
        this(DEFAULT_PORT);
    }

    public App(int port) {
        this.port = port;

        // Configure Jackson ObjectMapper with JavaTime support for ISO-8601 timestamps
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.javalinApp = Javalin.create(config -> {
            // Serve static files from src/main/resources/public
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });

            // Set customized Jackson JSON mapper
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        });

        configureRoutes();
    }

    private void configureRoutes() {
        // Global CORS and Pre-flight handling
        javalinApp.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });
        javalinApp.options("/*", ctx -> ctx.status(HttpStatus.OK));

        // RBAC & Path Security Guard Filter
        javalinApp.beforeMatched(ctx -> {
            String path = ctx.path();

            // 1. Path-based guards for /api/admin/*, /api/inventory/*, and /api/owner/*
            boolean isAdminRoute = path.startsWith("/api/admin/") || path.equals("/api/admin");
            boolean isInventoryRoute = path.startsWith("/api/inventory/") || path.equals("/api/inventory");
            boolean isOwnerRoute = path.startsWith("/api/owner/") || path.equals("/api/owner");

            if (isAdminRoute || isInventoryRoute || isOwnerRoute) {
                var user = com.bookstore.controller.AuthController.resolveUser(ctx);
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

                if (isAdminRoute && user.getRole() != com.bookstore.entity.AppRole.ADMIN) {
                    logger.warn("Forbidden access attempt to admin path: {}", path);
                    ctx.status(HttpStatus.FORBIDDEN).json(Map.of(
                            "success", false,
                            "error", "FORBIDDEN",
                            "message", "Access denied. Admin privileges required."
                    ));
                    ctx.skipRemainingHandlers();
                    return;
                }

                if ((isInventoryRoute || isOwnerRoute) && user.getRole() != com.bookstore.entity.AppRole.BOOKSTORE_OWNER && user.getRole() != com.bookstore.entity.AppRole.ADMIN) {
                    logger.warn("Forbidden access attempt to owner/inventory path: {}", path);
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
            java.util.Set<io.javalin.security.RouteRole> routeRoles = ctx.routeRoles();
            if (routeRoles.isEmpty() || routeRoles.contains(com.bookstore.entity.AppRole.ANYONE)) {
                return; // Publicly accessible route
            }

            com.bookstore.dto.UserSessionDto user = com.bookstore.controller.AuthController.resolveUser(ctx);
            if (user == null) {
                logger.warn("Unauthorized attempt to access: {}", ctx.path());
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of(
                        "success", false,
                        "error", "UNAUTHORIZED",
                        "message", "Authentication required. Please log in at /api/auth/login"
                ));
                ctx.skipRemainingHandlers();
                return;
            }

            com.bookstore.entity.AppRole userRole = user.getRole();
            // ADMIN has superuser privileges across all routes
            boolean isAuthorized = (userRole == com.bookstore.entity.AppRole.ADMIN) || routeRoles.contains(userRole);
            if (!isAuthorized) {
                logger.warn("Forbidden access attempt by user '{}' (role: {}) to restricted route: {}",
                        user.getUsername(), userRole, ctx.path());
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of(
                        "success", false,
                        "error", "FORBIDDEN",
                        "message", "Access denied. Required role: " + routeRoles + ", but your role is " + userRole
                ));
                ctx.skipRemainingHandlers();
            }
        });

        // Health Check
        javalinApp.get("/api/health", ctx -> ctx.json(Map.of(
                "status", "UP",
                "service", "Bookstore Management System",
                "timestamp", Instant.now().toString()
        )), com.bookstore.entity.AppRole.ANYONE);

        // Controllers
        BookController bookController = new BookController();
        CheckoutController checkoutController = new CheckoutController();
        OrderController orderController = new OrderController();
        CustomerController customerController = new CustomerController();
        EmployeeController employeeController = new EmployeeController();
        AuthController authController = new AuthController();
        OwnerController ownerController = new OwnerController();
        HoldController holdController = new HoldController();
        AdminStoreController adminStoreController = new AdminStoreController();

        // Auth Endpoints (Public)
        javalinApp.post("/api/auth/register", authController::register, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.post("/api/auth/login", authController::login, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.post("/api/auth/logout", authController::logout, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.get("/api/auth/me", authController::me, com.bookstore.entity.AppRole.ANYONE);

        // Phase 10: Owner Catalog & Lifecycle Management Endpoints
        javalinApp.post("/api/owner/books", ownerController::createBook, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.get("/api/owner/books", ownerController::getBooks, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.get("/api/owner/books/{id}", ownerController::getBookById, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.put("/api/owner/books/{id}", ownerController::updateBook, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.patch("/api/owner/books/{id}/stock", ownerController::adjustStock, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.delete("/api/owner/books/{id}", ownerController::archiveBook, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);

        javalinApp.get("/api/owner/publishers", ownerController::getPublishers, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.post("/api/owner/publishers", ownerController::createPublisher, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);

        javalinApp.get("/api/owner/authors", ownerController::getAuthors, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.post("/api/owner/authors", ownerController::createAuthor, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);

        // Phase 11: Owner Overview Metrics & Order History Audit
        javalinApp.get("/api/owner/metrics", ownerController::getMetrics, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.get("/api/owner/orders", ownerController::getOrders, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.post("/api/owner/orders/{id}/resend-receipt", ownerController::resendReceipt, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);

        // Protected /api/inventory/* Routes
        javalinApp.get("/api/inventory/books", bookController::getAll, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.post("/api/inventory/books", bookController::create, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.put("/api/inventory/restock/{id}", bookController::restock, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);

        // Protected /api/admin/* Routes
        javalinApp.get("/api/admin/employees", employeeController::getAll, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.get("/api/admin/users", ctx -> ctx.json(new com.bookstore.dao.AppUserDao().findAll()), com.bookstore.entity.AppRole.ADMIN);
        javalinApp.get("/api/admin/system", ctx -> ctx.json(Map.of(
                "status", "ACTIVE",
                "service", "Bookstore Management System",
                "javaVersion", System.getProperty("java.version"),
                "totalUsers", new com.bookstore.dao.AppUserDao().count()
        )), com.bookstore.entity.AppRole.ADMIN);

        // Phase 12: Admin Multi-Store Branch Provisioning & Scoping
        javalinApp.post("/api/admin/stores", adminStoreController::createStore, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.get("/api/admin/stores", adminStoreController::listStores, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.post("/api/admin/stores/{id}/assign-owner", adminStoreController::assignOwner, com.bookstore.entity.AppRole.ADMIN);

        // Book Endpoints
        javalinApp.get("/api/books", bookController::getAll, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.get("/api/books/{id}", bookController::getById, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.post("/api/books", bookController::create, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.put("/api/books/{id}/restock", bookController::restock, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);

        // Checkout Endpoint (Atomic Transaction)
        javalinApp.post("/api/checkout", checkoutController::checkout, com.bookstore.entity.AppRole.ANYONE);

        // In-Store Shelf Hold & Pickup Reservation Endpoints
        javalinApp.post("/api/holds", holdController::createHold, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.get("/api/holds", holdController::getHolds, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.post("/api/holds/{id}/fulfill", holdController::fulfillHold, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.delete("/api/holds/{id}", holdController::cancelHold, com.bookstore.entity.AppRole.ANYONE);

        // Order & PDF Receipt Endpoints
        javalinApp.get("/api/orders", orderController::getAll, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN, com.bookstore.entity.AppRole.CUSTOMER);
        javalinApp.get("/api/orders/{id}", orderController::getById, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.get("/api/orders/{id}/receipt", orderController::getReceiptPdf, com.bookstore.entity.AppRole.ANYONE);

        // Customer & Employee Endpoints
        javalinApp.get("/api/customers", customerController::getAll, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
        javalinApp.post("/api/customers", customerController::create, com.bookstore.entity.AppRole.ANYONE);
        javalinApp.get("/api/employees", employeeController::getAll, com.bookstore.entity.AppRole.ADMIN);

        // Dashboard Revenue & Daily Sales Summary
        javalinApp.get("/api/dashboard/sales-summary", orderController::getSalesSummary, com.bookstore.entity.AppRole.BOOKSTORE_OWNER, com.bookstore.entity.AppRole.ADMIN);
    }

    public void start() {
        // Ensure database tables exist, demo data is seeded, and default RBAC accounts are created
        try {
            logger.info("Initializing Bookstore database and checking seed data...");
            DatabaseSeeder seeder = new DatabaseSeeder();
            seeder.seedIfEmpty();
            seeder.ensureBooksActiveIfNull();
            seeder.seedUsersIfEmpty();

            // Seed default AppUser accounts
            new com.bookstore.service.UserService().seedDefaultUsersIfAbsent();

            // Seed default Bookstore branches & inventory if absent
            seeder.seedDefaultStoresIfEmpty();
        } catch (Exception e) {
            logger.warn("Database initialization warning: " + e.getMessage());
        }

        javalinApp.start(port);
        logger.info("================================================================================");
        logger.info(" BOOKSTORE MANAGEMENT SYSTEM EMBEDDED SERVER STARTED (RBAC ENABLED)");
        logger.info(" Port: http://localhost:{}", port);
        logger.info(" REST API Base: http://localhost:{}/api/books", port);
        logger.info(" Static Frontend Root: http://localhost:{}/", port);
        logger.info("================================================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping Javalin server and releasing resources...");
            javalinApp.stop();
            HibernateUtil.shutdown();
        }));
    }

    public void stop() {
        javalinApp.stop();
    }

    public Javalin getJavalin() {
        return javalinApp;
    }

    public static void main(String[] args) {
        new App(DEFAULT_PORT).start();
    }
}
