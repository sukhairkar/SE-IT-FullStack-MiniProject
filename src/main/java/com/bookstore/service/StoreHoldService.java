package com.bookstore.service;

import com.bookstore.dao.BookDao;
import com.bookstore.dto.HoldRequest;
import com.bookstore.dto.HoldResponse;
import com.bookstore.entity.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing in-store physical shelf reservations and customer pickup holds.
 * Customers hold books physically behind the counter for 48 hours without online payment.
 */
public class StoreHoldService {

    private static final Logger logger = LoggerFactory.getLogger(StoreHoldService.class);

    private final BookDao bookDao;
    private final EmailReceiptService emailReceiptService;

    // Concurrent store hold tracking (shared across service calls)
    private static final Map<Long, HoldResponse> holds = new ConcurrentHashMap<>();
    private static final AtomicLong holdIdCounter = new AtomicLong(1000);

    public StoreHoldService() {
        this.bookDao = new BookDao();
        this.emailReceiptService = new EmailReceiptService();
    }

    public StoreHoldService(BookDao bookDao, EmailReceiptService emailReceiptService) {
        this.bookDao = bookDao;
        this.emailReceiptService = emailReceiptService;
    }

    /**
     * Creates an in-store reservation hold for a customer.
     */
    public synchronized HoldResponse createHold(HoldRequest req) {
        if (req == null || req.getBookId() == null) {
            throw new IllegalArgumentException("Book ID is required for in-store pickup hold.");
        }
        if (req.getCustomerName() == null || req.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required for reservation.");
        }
        if ((req.getCustomerPhone() == null || req.getCustomerPhone().isBlank()) &&
            (req.getCustomerEmail() == null || req.getCustomerEmail().isBlank())) {
            throw new IllegalArgumentException("Either mobile number or email address is required to contact you about pickup.");
        }

        Book book = bookDao.findById(req.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + req.getBookId()));

        if (book.getStockCount() <= 0) {
            throw new IllegalArgumentException("Cannot place hold: '" + book.getTitle() + "' is currently out of stock on shelves.");
        }

        Long holdId = holdIdCounter.incrementAndGet();
        String holdCode = "HLD-" + holdId;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(48);

        String shelfLocation = deriveShelfLocation(book.getGenre());

        HoldResponse res = new HoldResponse(
                holdId,
                holdCode,
                book.getBookId(),
                book.getTitle(),
                shelfLocation,
                book.getPrice(),
                req.getCustomerName().trim(),
                req.getCustomerEmail() != null ? req.getCustomerEmail().trim() : null,
                req.getCustomerPhone() != null ? req.getCustomerPhone().trim() : null,
                req.getPickupWindow() != null ? req.getPickupWindow() : "TODAY_EVENING",
                "HELD",
                now,
                expiresAt
        );

        holds.put(holdId, res);
        logger.info("Created in-store shelf reservation #{} ({}) for customer '{}' on book '{}'",
                holdId, holdCode, res.getCustomerName(), book.getTitle());

        // Attempt non-blocking confirmation email if email is provided
        if (res.getCustomerEmail() != null && res.getCustomerEmail().contains("@")) {
            sendHoldEmailConfirmation(res);
        }

        return res;
    }

    /**
     * Retrieves all active shelf holds awaiting customer counter pickup.
     */
    public List<HoldResponse> getActiveHolds() {
        return holds.values().stream()
                .filter(h -> "HELD".equalsIgnoreCase(h.getStatus()) || "READY_AT_COUNTER".equalsIgnoreCase(h.getStatus()))
                .sorted(Comparator.comparing(HoldResponse::getCreatedAt).reversed())
                .toList();
    }

    /**
     * Retrieves all shelf holds (active, fulfilled, cancelled) for auditing or history.
     */
    public List<HoldResponse> getAllHolds() {
        return holds.values().stream()
                .sorted(Comparator.comparing(HoldResponse::getCreatedAt).reversed())
                .toList();
    }

    public Optional<HoldResponse> getHoldById(Long holdId) {
        return Optional.ofNullable(holds.get(holdId));
    }

    /**
     * Retrieves all holds (active or fulfilled) for a specific customer email.
     */
    public List<HoldResponse> getHoldsByCustomer(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        String cleanEmail = email.trim().toLowerCase();
        return holds.values().stream()
                .filter(h -> h.getCustomerEmail() != null && h.getCustomerEmail().equalsIgnoreCase(cleanEmail))
                .sorted(Comparator.comparing(HoldResponse::getCreatedAt).reversed())
                .toList();
    }

    /**
     * Marks a reservation as fulfilled when customer pays at counter register.
     */
    public synchronized HoldResponse fulfillHold(Long holdId) {
        HoldResponse hold = holds.get(holdId);
        if (hold == null) {
            throw new IllegalArgumentException("Hold not found with ID: " + holdId);
        }
        hold.setStatus("FULFILLED");
        logger.info("Marked in-store hold #{} ({}) as FULFILLED at cashier counter.", holdId, hold.getHoldCode());
        return hold;
    }

    /**
     * Cancels an in-store hold and releases copy back to general shelf display.
     */
    public synchronized HoldResponse cancelHold(Long holdId) {
        HoldResponse hold = holds.get(holdId);
        if (hold == null) {
            throw new IllegalArgumentException("Hold not found with ID: " + holdId);
        }
        hold.setStatus("CANCELLED");
        logger.info("Cancelled in-store hold #{} ({}) - book returned to shelf.", holdId, hold.getHoldCode());
        return hold;
    }

    private void sendHoldEmailConfirmation(HoldResponse res) {
        try {
            String subject = "In-Store Hold Confirmed: " + res.getHoldCode() + " at BookHaven";
            String body = "Dear " + res.getCustomerName() + ",\n\n" +
                    "Your in-store shelf reservation has been confirmed!\n\n" +
                    "• Hold Slip Code: " + res.getHoldCode() + "\n" +
                    "• Reserved Title: " + res.getBookTitle() + "\n" +
                    "• Physical Location: " + res.getShelfLocation() + "\n" +
                    "• In-Store Price: ₹" + res.getPrice() + "\n" +
                    "• Hold Expires: " + res.getExpiresAt() + "\n\n" +
                    "Please visit our store counter and present your Hold Code to collect your copy and pay via Cash, Card, or UPI.\n\n" +
                    "BookHaven Physical Bookstore\n" +
                    "100 Book Haven Boulevard, Reading City";

            // If email service is configured
            logger.info("Prepared hold notification for customer: {}", res.getCustomerEmail());
        } catch (Exception e) {
            logger.warn("Non-blocking error dispatching hold email: {}", e.getMessage());
        }
    }

    private String deriveShelfLocation(String genre) {
        if (genre == null || genre.isBlank()) return "Aisle 1, Main Bay";
        String g = genre.toLowerCase();
        if (g.contains("software") || g.contains("engineering")) return "Shelf A-1 (Tech & Coding Bay)";
        if (g.contains("computer") || g.contains("system")) return "Shelf A-2 (CS & Architecture)";
        if (g.contains("fiction") || g.contains("novel")) return "Shelf B-1 (Literature & Fiction)";
        if (g.contains("science") || g.contains("physics")) return "Shelf C-1 (Natural Sciences)";
        if (g.contains("history") || g.contains("biography")) return "Shelf D-1 (History & Memoirs)";
        if (g.contains("business") || g.contains("finance")) return "Shelf E-2 (Business & Economy)";
        return "Shelf B-3 (" + genre + " Section)";
    }
}
