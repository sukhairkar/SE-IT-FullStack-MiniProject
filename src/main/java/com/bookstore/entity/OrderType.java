package com.bookstore.entity;

/**
 * Identifies whether an order is an in-store Point of Sale transaction or
 * an online click-and-collect pickup reservation (BOPIS).
 */
public enum OrderType {
    IN_STORE_POS,
    PICKUP_RESERVATION
}
