package com.bookstore.controller;

import com.bookstore.dao.CustomerDao;
import com.bookstore.entity.Customer;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * REST controller for customer operations.
 */
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    private final CustomerDao customerDao = new CustomerDao();

    /**
     * GET /api/customers
     */
    public void getAll(Context ctx) {
        ctx.json(customerDao.findAll());
    }

    /**
     * POST /api/customers
     */
    public void create(Context ctx) {
        try {
            Customer customer = ctx.bodyAsClass(Customer.class);
            if (customer.getEmailId() == null || customer.getEmailId().isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Email is required"));
                return;
            }
            customerDao.save(customer);
            ctx.status(HttpStatus.CREATED).json(customer);
        } catch (Exception e) {
            logger.error("Failed to create customer: " + e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
        }
    }
}
