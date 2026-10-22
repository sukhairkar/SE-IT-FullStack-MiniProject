package com.bookstore.controller;

import com.bookstore.dto.HoldRequest;
import com.bookstore.dto.HoldResponse;
import com.bookstore.service.StoreHoldService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller handling in-store shelf reservation and pickup holds.
 */
public class HoldController {

    private static final Logger logger = LoggerFactory.getLogger(HoldController.class);
    private final StoreHoldService storeHoldService;

    public HoldController() {
        this.storeHoldService = new StoreHoldService();
    }

    public HoldController(StoreHoldService storeHoldService) {
        this.storeHoldService = storeHoldService;
    }

    /**
     * POST /api/holds
     * Place an in-store shelf hold reservation.
     */
    public void createHold(Context ctx) {
        try {
            HoldRequest req = ctx.bodyAsClass(HoldRequest.class);
            HoldResponse res = storeHoldService.createHold(req);
            ctx.status(HttpStatus.CREATED).json(res);
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            logger.error("Failed to create in-store hold: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", "Failed to place in-store hold: " + ex.getMessage()));
        }
    }

    /**
     * GET /api/holds
     * Lists active in-store holds for cashier desk or for customer by email.
     */
    public void getHolds(Context ctx) {
        try {
            String email = ctx.queryParam("email");
            if (email != null && !email.isBlank()) {
                List<HoldResponse> customerHolds = storeHoldService.getHoldsByCustomer(email);
                ctx.json(customerHolds);
                return;
            }

            boolean all = "true".equalsIgnoreCase(ctx.queryParam("all"));
            if (all) {
                ctx.json(storeHoldService.getAllHolds());
                return;
            }

            // Return all active counter holds for staff/cashiers
            List<HoldResponse> activeHolds = storeHoldService.getActiveHolds();
            ctx.json(activeHolds);
        } catch (Exception ex) {
            logger.error("Failed to retrieve in-store holds: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", "Failed to retrieve holds: " + ex.getMessage()));
        }
    }

    /**
     * POST /api/holds/{id}/fulfill
     * Cashier counter marks hold fulfilled after checkout.
     */
    public void fulfillHold(Context ctx) {
        try {
            Long holdId = Long.parseLong(ctx.pathParam("id"));
            HoldResponse res = storeHoldService.fulfillHold(holdId);
            ctx.json(res);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid hold ID format"));
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            logger.error("Failed to fulfill hold: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * DELETE /api/holds/{id}
     * Release hold back to general shelf display.
     */
    public void cancelHold(Context ctx) {
        try {
            Long holdId = Long.parseLong(ctx.pathParam("id"));
            HoldResponse res = storeHoldService.cancelHold(holdId);
            ctx.json(res);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid hold ID format"));
        } catch (IllegalArgumentException iae) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            logger.error("Failed to cancel hold: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }
}
