package com.bookstore.entity;

import io.javalin.security.RouteRole;

/**
 * Role-Based Access Control (RBAC) roles for the Bookstore System.
 */
public enum AppRole implements RouteRole {
    BOOKSTORE_OWNER,
    ADMIN,
    CUSTOMER,
    ANYONE
}
