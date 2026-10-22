package com.bookstore.controller;

import com.bookstore.dto.LoginRequest;
import com.bookstore.dto.RegisterRequest;
import com.bookstore.dto.UserSessionDto;
import com.bookstore.entity.AppRole;
import com.bookstore.entity.AppUser;
import com.bookstore.service.UserService;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller managing email + password authentication, registration, session management,
 * and Javalin RouteRole resolution.
 */
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public static final String SESSION_KEY = "currentUser";
    public static final String COOKIE_KEY = "AUTH_TOKEN";

    // In-memory token store allowing Bearer token authorization in API clients (cURL / Postman / Fetch)
    public static final Map<String, UserSessionDto> TOKEN_STORE = new ConcurrentHashMap<>();

    private final UserService userService;

    public AuthController() {
        this(new UserService());
    }

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/auth/register
     * Validates email format, checks duplication, hashes password with BCrypt,
     * stores user with default role CUSTOMER, and establishes session.
     */
    public void register(Context ctx) {
        try {
            RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
            if (req.getEmail() == null || req.getPassword() == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                        "success", false,
                        "error", "Email and password are required"
                ));
                return;
            }

            // Public Sign-Up Guard: Self-registration strictly defaults to CUSTOMER.
            // Reject/ignore any client-supplied role parameter to prevent privilege escalation.
            AppUser user = userService.register(req.getEmail(), req.getPassword());

            // Establish Session and Token
            String token = UUID.randomUUID().toString();
            UserSessionDto sessionDto = new UserSessionDto(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    null,
                    null,
                    user.getEmail(),
                    token
            );
            sessionDto.setEmail(user.getEmail());

            // Set session attribute as required
            ctx.sessionAttribute(SESSION_KEY, user);
            ctx.sessionAttribute("currentUserSession", sessionDto);

            TOKEN_STORE.put(token, sessionDto);
            ctx.cookie(new Cookie(COOKIE_KEY, token, "/", 86400, false));

            logger.info("New customer registered: '{}'", user.getEmail());

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Registration successful");
            resp.put("email", user.getEmail());
            resp.put("role", user.getRole().name());
            resp.put("token", token);
            resp.put("id", user.getId());

            ctx.status(HttpStatus.CREATED).json(resp);

        } catch (IllegalArgumentException iae) {
            logger.warn("Registration rejected: {}", iae.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Registration error: " + ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Registration error: " + ex.getMessage()
            ));
        }
    }

    /**
     * POST /api/auth/login
     * Verifies email and BCrypt hash, and sets ctx.sessionAttribute("currentUser", user).
     */
    public void login(Context ctx) {
        try {
            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
            String emailOrUsername = req.getEmail();
            String password = req.getPassword();

            if (emailOrUsername == null || emailOrUsername.isBlank() || password == null || password.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                        "success", false,
                        "error", "Email and password are required"
                ));
                return;
            }

            AppUser user = userService.authenticate(emailOrUsername, password);

            // Establish Session and Token
            String token = UUID.randomUUID().toString();
            UserSessionDto sessionDto = new UserSessionDto(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    null,
                    null,
                    resolveDisplayName(user),
                    token
            );
            sessionDto.setEmail(user.getEmail());
            if (user.getAssignedStore() != null) {
                sessionDto.setAssignedStoreId(user.getAssignedStore().getStoreId());
                sessionDto.setAssignedStoreName(user.getAssignedStore().getName());
            }

            // Set session attribute
            ctx.sessionAttribute(SESSION_KEY, user);
            ctx.sessionAttribute("currentUserSession", sessionDto);

            TOKEN_STORE.put(token, sessionDto);
            ctx.cookie(new Cookie(COOKIE_KEY, token, "/", 86400, false));

            logger.info("User '{}' logged in with role: {}", user.getEmail(), user.getRole());

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Login successful");
            resp.put("email", user.getEmail());
            resp.put("username", user.getEmail());
            resp.put("role", user.getRole().name());
            resp.put("token", token);
            resp.put("user", sessionDto);

            ctx.json(resp);

        } catch (IllegalArgumentException iae) {
            logger.warn("Authentication failed: {}", iae.getMessage());
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Login process error: " + ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Login error: " + ex.getMessage()
            ));
        }
    }

    /**
     * POST /api/auth/logout
     * Invalidates session and revokes tokens.
     */
    public void logout(Context ctx) {
        UserSessionDto user = resolveUser(ctx);
        if (user != null && user.getToken() != null) {
            TOKEN_STORE.remove(user.getToken());
        }

        try {
            var session = ctx.req().getSession(false);
            if (session != null) {
                session.invalidate();
            }
        } catch (Exception ignored) {
        }

        ctx.sessionAttribute(SESSION_KEY, null);
        ctx.sessionAttribute("currentUserSession", null);
        ctx.removeCookie(COOKIE_KEY, "/");

        logger.info("User logged out successfully.");

        ctx.json(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }

    /**
     * GET /api/auth/me
     * Returns { "authenticated": true, "email": "...", "role": "...", "user": ... } or 401 { "authenticated": false }.
     */
    public void me(Context ctx) {
        UserSessionDto user = resolveUser(ctx);
        if (user != null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("authenticated", true);
            resp.put("email", user.getEmail());
            resp.put("role", user.getRole().name());
            resp.put("username", user.getUsername());
            resp.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
            resp.put("assignedStoreId", user.getAssignedStoreId());
            resp.put("assignedStoreName", user.getAssignedStoreName());
            resp.put("user", user);
            resp.put("id", user.getUserId() != null ? user.getUserId() : 0L);

            ctx.json(resp);
        } else {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of(
                    "authenticated", false
            ));
        }
    }

    /**
     * Resolves authenticated user session from:
     * 1. Session Attribute ("currentUser")
     * 2. Header "Authorization: Bearer <token>"
     * 3. Cookie "AUTH_TOKEN"
     */
    public static UserSessionDto resolveUser(Context ctx) {
        // 1. Check HTTP Session
        Object sessionUser = ctx.sessionAttribute(SESSION_KEY);
        if (sessionUser instanceof AppUser appUser) {
            UserSessionDto dto = new UserSessionDto(
                    appUser.getId(),
                    appUser.getEmail(),
                    appUser.getRole(),
                    null,
                    null,
                    appUser.getEmail(),
                    null
            );
            if (appUser.getAssignedStore() != null) {
                dto.setAssignedStoreId(appUser.getAssignedStore().getStoreId());
                dto.setAssignedStoreName(appUser.getAssignedStore().getName());
            }
            return dto;
        }
        if (sessionUser instanceof UserSessionDto dto) {
            return dto;
        }

        Object customSession = ctx.sessionAttribute("currentUserSession");
        if (customSession instanceof UserSessionDto dto) {
            return dto;
        }

        // 2. Check Authorization Header (Bearer token)
        String authHeader = ctx.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            UserSessionDto tokenUser = TOKEN_STORE.get(token);
            if (tokenUser != null) {
                return tokenUser;
            }
        }

        // 3. Check Cookie
        String cookieToken = ctx.cookie(COOKIE_KEY);
        if (cookieToken != null) {
            return TOKEN_STORE.get(cookieToken);
        }

        return null;
    }

    private String resolveDisplayName(AppUser user) {
        if (user.getRole() == AppRole.BOOKSTORE_OWNER) return "Store Owner";
        if (user.getRole() == AppRole.ADMIN) return "System Admin";
        return user.getEmail();
    }
}
