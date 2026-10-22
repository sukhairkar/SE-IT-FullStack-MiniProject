package com.bookstore.controller;

import com.bookstore.dto.AssignOwnerRequest;
import com.bookstore.dto.CreateStoreRequest;
import com.bookstore.dto.StoreSummaryDto;
import com.bookstore.entity.AppUser;
import com.bookstore.entity.Bookstore;
import com.bookstore.service.StoreService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing Admin APIs for Multi-Store branch management, geo-location mapping,
 * and Bookstore Owner provisioning.
 * (Guarded strictly to ADMIN role).
 */
public class AdminStoreController {

    private static final Logger logger = LoggerFactory.getLogger(AdminStoreController.class);

    private final StoreService storeService;

    public AdminStoreController() {
        this(new StoreService());
    }

    public AdminStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * POST /api/admin/stores
     * Provisions a new bookstore branch with address, phone, email, and coordinates.
     */
    public void createStore(Context ctx) {
        try {
            CreateStoreRequest req = ctx.bodyAsClass(CreateStoreRequest.class);
            Bookstore store = storeService.createStore(req);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Bookstore branch created successfully.");
            resp.put("store", store);
            resp.put("storeId", store.getStoreId());

            ctx.status(HttpStatus.CREATED).json(resp);
        } catch (IllegalArgumentException iae) {
            logger.warn("Invalid store creation request: {}", iae.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Error creating bookstore branch: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Failed to create bookstore: " + ex.getMessage()
            ));
        }
    }

    /**
     * GET /api/admin/stores
     * Lists all bookstore branches with assigned manager/owner email and performance summary.
     */
    public void listStores(Context ctx) {
        try {
            List<StoreSummaryDto> stores = storeService.listStoresForAdmin();
            ctx.json(stores);
        } catch (Exception ex) {
            logger.error("Error listing stores for admin: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Failed to fetch stores: " + ex.getMessage()
            ));
        }
    }

    /**
     * POST /api/admin/stores/{id}/assign-owner
     * Assigns an existing user or creates a new user with BOOKSTORE_OWNER role bound to this branch.
     */
    public void assignOwner(Context ctx) {
        try {
            Long storeId = Long.parseLong(ctx.pathParam("id"));
            AssignOwnerRequest req = ctx.bodyAsClass(AssignOwnerRequest.class);

            AppUser owner = storeService.assignOwnerToStore(storeId, req);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Bookstore owner bound successfully.");
            resp.put("storeId", storeId);
            resp.put("ownerId", owner.getId());
            resp.put("ownerEmail", owner.getEmail());
            resp.put("role", owner.getRole().name());

            ctx.json(resp);
        } catch (NumberFormatException nfe) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", "Invalid store ID format."
            ));
        } catch (IllegalArgumentException iae) {
            logger.warn("Invalid owner assignment request: {}", iae.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                    "success", false,
                    "error", iae.getMessage()
            ));
        } catch (Exception ex) {
            logger.error("Error assigning store owner: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
                    "success", false,
                    "error", "Failed to assign owner: " + ex.getMessage()
            ));
        }
    }
}
