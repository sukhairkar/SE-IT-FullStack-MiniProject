package com.bookstore.controller;

import com.bookstore.dao.EmployeeDao;
import io.javalin.http.Context;

/**
 * REST controller for employee operations.
 */
public class EmployeeController {

    private final EmployeeDao employeeDao = new EmployeeDao();

    /**
     * GET /api/employees
     */
    public void getAll(Context ctx) {
        ctx.json(employeeDao.findAll());
    }
}
