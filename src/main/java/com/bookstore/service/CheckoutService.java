package com.bookstore.service;

import com.bookstore.dao.BookDao;
import com.bookstore.dao.CustomerDao;
import com.bookstore.dao.EmployeeDao;
import com.bookstore.dto.CartItemDto;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.dto.CheckoutResponse;
import com.bookstore.entity.*;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing atomic, transactional checkout operations.
 * Guarantees that stock decrement, order creation, order-book associations,
 * payment recording, and cashier handling are committed atomically or rolled back completely.
 */
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    private final BookDao bookDao;
    private final CustomerDao customerDao;
    private final EmployeeDao employeeDao;
    private final ReceiptPdfService receiptPdfService;
    private final EmailReceiptService emailReceiptService;

    public CheckoutService() {
        this.bookDao = new BookDao();
        this.customerDao = new CustomerDao();
        this.employeeDao = new EmployeeDao();
        this.receiptPdfService = new ReceiptPdfService();
        this.emailReceiptService = new EmailReceiptService();
    }

    public CheckoutService(BookDao bookDao, CustomerDao customerDao, EmployeeDao employeeDao) {
        this(bookDao, customerDao, employeeDao, new ReceiptPdfService(), new EmailReceiptService());
    }

    public CheckoutService(BookDao bookDao, CustomerDao customerDao, EmployeeDao employeeDao,
                           ReceiptPdfService receiptPdfService, EmailReceiptService emailReceiptService) {
        this.bookDao = bookDao;
        this.customerDao = customerDao;
        this.employeeDao = employeeDao;
        this.receiptPdfService = receiptPdfService;
        this.emailReceiptService = emailReceiptService;
    }

    /**
     * Executes an atomic checkout transaction.
     *
     * @param request the checkout payload
     * @return CheckoutResponse with details of the created order and payment
     * @throws InsufficientStockException if requested stock exceeds availability
     */
    public CheckoutResponse processCheckout(CheckoutRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Checkout request must contain at least one item.");
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            logger.info("Initiating atomic checkout transaction...");

            // 1. Resolve or Create Customer
            Customer customer = resolveCustomer(session, request);

            // 2. Resolve Cashier Employee (Optional)
            Employee employee = null;
            if (request.getEmployeeId() != null) {
                employee = employeeDao.findById(session, request.getEmployeeId()).orElse(null);
            }

            // 3. Validate Stock and Calculate Totals with Pessimistic Locking
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalQuantity = 0;
            List<CheckoutResponse.PurchasedItem> purchasedItems = new ArrayList<>();
            List<Book> booksToUpdate = new ArrayList<>();
            List<Book> distinctOrderedBooks = new ArrayList<>();

            for (CartItemDto item : request.getItems()) {
                if (item.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity must be greater than zero for book ID: " + item.getBookId());
                }

                // Lock the book row for atomic update
                Book book = bookDao.findByIdWithLock(session, item.getBookId())
                        .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + item.getBookId()));

                // Atomic stock validation
                if (book.getStockCount() < item.getQuantity()) {
                    logger.warn("Atomic checkout aborted: Insufficient stock for book '{}' (ID: {}). Requested: {}, Available: {}",
                            book.getTitle(), book.getBookId(), item.getQuantity(), book.getStockCount());
                    throw new InsufficientStockException(book.getBookId(), book.getTitle(), item.getQuantity(), book.getStockCount());
                }

                // Decrement stock
                book.setStockCount(book.getStockCount() - item.getQuantity());
                booksToUpdate.add(book);
                distinctOrderedBooks.add(book);

                // Accumulate line totals
                BigDecimal unitPrice = book.getPrice();
                BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(lineSubtotal);
                totalQuantity += item.getQuantity();

                purchasedItems.add(new CheckoutResponse.PurchasedItem(
                        book.getBookId(),
                        book.getTitle(),
                        item.getQuantity(),
                        unitPrice,
                        lineSubtotal
                ));
            }

            // Persist stock decrements
            for (Book b : booksToUpdate) {
                session.merge(b);
            }

            // 4. Create and Persist ORDER
            LocalDateTime now = LocalDateTime.now();
            Order order = new Order(totalQuantity, totalAmount, now, customer);
            session.persist(order);
            logger.info("Persisted Order #{} for customer '{}'", order.getOrderId(), customer.getEmailId());

            // 5. Create and Persist ORDER_BOOK associations
            for (Book book : distinctOrderedBooks) {
                OrderBook orderBook = new OrderBook(order, book);
                session.persist(orderBook);
            }

            // 6. Create and Persist PAYMENT
            String paymentMethod = (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                    ? request.getPaymentMethod().toUpperCase()
                    : "CASH";
            Payment payment = new Payment(paymentMethod, "COMPLETED", totalAmount, now, customer);
            session.persist(payment);
            logger.info("Persisted Payment #{} (Amount: {}, Method: {})", payment.getPaymentId(), totalAmount, paymentMethod);

            // 7. Record HANDLED_BY if cashier employee was assigned
            if (employee != null) {
                HandledBy handledBy = new HandledBy(employee, payment);
                session.persist(handledBy);
                logger.info("Recorded HandledBy: Employee #{} handled Payment #{}", employee.getEmployeeId(), payment.getPaymentId());
            }

            // Commit atomic transaction
            tx.commit();
            logger.info("Atomic checkout transaction committed successfully. Order #{} created.", order.getOrderId());

            // Post-commit: Asynchronously dispatch digital PDF receipt via email if enabled
            boolean shouldSendEmail = (request.getSendEmailReceipt() == null || Boolean.TRUE.equals(request.getSendEmailReceipt()));
            boolean emailDispatched = false;

            if (shouldSendEmail && customer.getEmailId() != null && !customer.getEmailId().isBlank() && customer.getEmailId().contains("@")) {
                try {
                    File pdfFile = receiptPdfService.generateReceiptPdfFile(order.getOrderId());
                    emailReceiptService.sendReceiptWithAttachment(customer.getEmailId(), String.valueOf(order.getOrderId()), pdfFile);
                    emailDispatched = true;
                    logger.info("Triggered asynchronous email receipt dispatch for Order #{} to '{}'", order.getOrderId(), customer.getEmailId());
                } catch (Exception emailEx) {
                    logger.warn("Non-blocking error generating/dispatching email receipt for Order #{}: {}", order.getOrderId(), emailEx.getMessage());
                }
            }

            String customerName = customer.getFirstName() + " " + customer.getLastName();
            CheckoutResponse checkoutResponse = CheckoutResponse.success(
                    order.getOrderId(),
                    payment.getPaymentId(),
                    order.getDate(),
                    customer.getCustomerId(),
                    customerName,
                    customer.getEmailId(),
                    totalQuantity,
                    totalAmount,
                    payment.getMethod(),
                    payment.getStatus(),
                    employee != null ? employee.getEmployeeId() : null,
                    purchasedItems
            );
            checkoutResponse.setEmailReceiptDispatched(emailDispatched);

            if (request.getCashTendered() != null && "CASH".equalsIgnoreCase(payment.getMethod())) {
                BigDecimal tendered = request.getCashTendered();
                BigDecimal change = tendered.subtract(totalAmount);
                checkoutResponse.setCashTendered(tendered);
                checkoutResponse.setChangeDue(change.compareTo(BigDecimal.ZERO) >= 0 ? change : BigDecimal.ZERO);
            }

            if (request.getHoldId() != null) {
                try {
                    new StoreHoldService().fulfillHold(request.getHoldId());
                    logger.info("Auto-fulfilled in-store hold #{} after successful checkout", request.getHoldId());
                } catch (Exception hEx) {
                    logger.warn("Could not auto-fulfill hold #{}: {}", request.getHoldId(), hEx.getMessage());
                }
            }

            return checkoutResponse;

        } catch (InsufficientStockException ise) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                    logger.info("Transaction rolled back successfully due to insufficient stock.");
                } catch (Exception rbe) {
                    logger.error("Error during transaction rollback: " + rbe.getMessage(), rbe);
                }
            }
            throw ise;
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                    logger.error("Transaction rolled back due to error: " + ex.getMessage(), ex);
                } catch (Exception rbe) {
                    logger.error("Error during transaction rollback: " + rbe.getMessage(), rbe);
                }
            }
            throw new RuntimeException("Atomic checkout failed: " + ex.getMessage(), ex);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private Customer resolveCustomer(Session session, CheckoutRequest request) {
        if (request.getCustomerId() != null) {
            return customerDao.findById(session, request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.getCustomerId()));
        }

        // Search by email if email provided
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()) {
            var existing = customerDao.findByEmail(session, request.getCustomerEmail());
            if (existing.isPresent()) {
                return existing.get();
            }

            // Create new customer on the fly
            Customer newCustomer = new Customer(
                    request.getCustomerEmail().trim(),
                    request.getCustomerFirstName() != null ? request.getCustomerFirstName().trim() : "Valued",
                    request.getCustomerLastName() != null ? request.getCustomerLastName().trim() : "Customer",
                    request.getCustomerStreet(),
                    request.getCustomerCity(),
                    request.getCustomerPincode()
            );
            session.persist(newCustomer);
            if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank()) {
                CustomerPhone phone = new CustomerPhone(newCustomer, request.getCustomerPhone().trim());
                session.persist(phone);
            }
            return newCustomer;
        }

        // Fallback default guest customer
        var guestOpt = customerDao.findByEmail(session, "guest@bookstore.local");
        if (guestOpt.isPresent()) {
            return guestOpt.get();
        }
        Customer guest = new Customer("guest@bookstore.local", "Guest", "Shopper", "Main St", "City", "000000");
        session.persist(guest);
        return guest;
    }
}
