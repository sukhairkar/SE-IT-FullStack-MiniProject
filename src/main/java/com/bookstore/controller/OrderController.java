package com.bookstore.controller;

import com.bookstore.dao.OrderDao;
import com.bookstore.dto.OrderSummaryDto;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderBook;
import com.bookstore.service.ReceiptPdfService;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Order lookups and PDF receipt streaming.
 */
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderDao orderDao = new OrderDao();
    private final ReceiptPdfService receiptPdfService = new ReceiptPdfService();

    /**
     * GET /api/orders
     */
    public void getAll(Context ctx) {
        String limitParam = ctx.queryParam("limit");
        int limit = 100;
        if (limitParam != null) {
            try {
                limit = Math.max(1, Integer.parseInt(limitParam));
            } catch (NumberFormatException ignored) {}
        }

        com.bookstore.dto.UserSessionDto user = AuthController.resolveUser(ctx);
        List<Order> orders;
        if (user != null && user.getRole() == com.bookstore.entity.AppRole.CUSTOMER && user.getCustomerId() != null) {
            orders = orderDao.findOrdersByCustomerId(user.getCustomerId());
        } else {
            orders = orderDao.findRecentOrders(limit);
        }

        List<OrderSummaryDto> dtoList = new ArrayList<>();
        for (Order o : orders) {
            List<OrderBook> books = orderDao.findOrderBooks(o.getOrderId());
            List<String> titles = books.stream()
                    .map(ob -> ob.getBook() != null ? ob.getBook().getTitle() : "Book #" + ob.getId().getBookId())
                    .toList();

            dtoList.add(new OrderSummaryDto(
                    o.getOrderId(),
                    o.getQuantity(),
                    o.getOrderAmount(),
                    o.getDate(),
                    o.getCustomer() != null ? o.getCustomer().getCustomerId() : null,
                    o.getCustomer() != null ? o.getCustomer().getFirstName() + " " + o.getCustomer().getLastName() : "Guest",
                    o.getCustomer() != null ? o.getCustomer().getEmailId() : "N/A",
                    titles
            ));
        }
        ctx.json(dtoList);
    }

    /**
     * GET /api/dashboard/sales-summary
     * Computes daily and overall revenue metrics for Owner and Admin dashboards.
     */
    public void getSalesSummary(Context ctx) {
        List<Order> allOrders = orderDao.findAll();
        java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
        int totalBooksSold = 0;
        int totalOrders = allOrders.size();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.math.BigDecimal todayRevenue = java.math.BigDecimal.ZERO;
        int todayOrders = 0;

        for (Order o : allOrders) {
            if (o.getOrderAmount() != null) {
                totalRevenue = totalRevenue.add(o.getOrderAmount());
                if (o.getDate() != null && o.getDate().toLocalDate().isEqual(today)) {
                    todayRevenue = todayRevenue.add(o.getOrderAmount());
                    todayOrders++;
                }
            }
            totalBooksSold += o.getQuantity();
        }

        java.math.BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(java.math.BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;

        ctx.json(Map.of(
                "totalRevenue", totalRevenue,
                "totalOrders", totalOrders,
                "totalBooksSold", totalBooksSold,
                "averageOrderValue", avgOrderValue,
                "todayRevenue", todayRevenue,
                "todayOrders", todayOrders
        ));
    }

    /**
     * GET /api/orders/{id}
     */
    public void getById(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            orderDao.findById(id).ifPresentOrElse(
                    order -> {
                        List<OrderBook> books = orderDao.findOrderBooks(id);
                        ctx.json(Map.of(
                                "orderId", order.getOrderId(),
                                "quantity", order.getQuantity(),
                                "orderAmount", order.getOrderAmount(),
                                "date", order.getDate(),
                                "customer", order.getCustomer(),
                                "books", books.stream().map(ob -> ob.getBook()).toList()
                        ));
                    },
                    () -> ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Order not found with ID: " + id))
            );
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid order ID format"));
        }
    }

    /**
     * GET /api/orders/{id}/receipt
     * Streams digital PDF receipt directly to client.
     */
    public void getReceiptPdf(Context ctx) {
        try {
            Long orderId = Long.parseLong(ctx.pathParam("id"));
            logger.info("Serving digital PDF invoice for Order #{}", orderId);

            byte[] pdfBytes = receiptPdfService.generateReceiptPdf(orderId);

            ctx.contentType(ContentType.APPLICATION_PDF);
            ctx.header("Content-Disposition", "inline; filename=\"receipt-order-" + orderId + ".pdf\"");
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.result(new ByteArrayInputStream(pdfBytes));

        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Invalid order ID"));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error generating receipt PDF: " + e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", "Failed to generate receipt PDF: " + e.getMessage()));
        }
    }
}
