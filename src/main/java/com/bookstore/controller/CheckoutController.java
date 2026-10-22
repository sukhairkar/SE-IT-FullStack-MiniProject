package com.bookstore.controller;

import com.bookstore.dto.CheckoutRequest;
import com.bookstore.dto.CheckoutResponse;
import com.bookstore.service.CheckoutService;
import com.bookstore.service.InsufficientStockException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * REST controller for checkout transactions.
 */
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);
    private final CheckoutService checkoutService;

    public CheckoutController() {
        this.checkoutService = new CheckoutService();
    }

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * POST /api/checkout
     * Executes atomic multi-table checkout transaction.
     */
    public void checkout(Context ctx) {
        try {
            CheckoutRequest request = ctx.bodyAsClass(CheckoutRequest.class);
            CheckoutResponse response = checkoutService.processCheckout(request);
            ctx.status(HttpStatus.CREATED).json(response);

        } catch (InsufficientStockException ise) {
            logger.warn("Checkout rejected due to stock: " + ise.getMessage());
            ctx.status(HttpStatus.CONFLICT).json(Map.of(
                    "success", false,
                    "error", "INSUFFICIENT_STOCK",
                    "message", ise.getMessage(),
                    "bookId", ise.getBookId(),
                    "requested", ise.getRequested(),
                    "available", ise.getAvailable()
            ));

        } catch (IllegalArgumentException iae) {
            logger.warn("Invalid checkout payload: " + iae.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", "INVALID_REQUEST",
                    "message", iae.getMessage()
            ));

        } catch (Exception ex) {
            logger.error("Checkout transaction error: " + ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "CHECKOUT_FAILED",
                    "message", ex.getMessage()
            ));
        }
    }
}
