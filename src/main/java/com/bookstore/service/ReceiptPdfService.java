package com.bookstore.service;

import com.bookstore.dao.OrderDao;
import com.bookstore.entity.HandledBy;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderBook;
import com.bookstore.entity.Payment;
import com.bookstore.util.HibernateUtil;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service to generate digital PDF invoices and receipts using OpenPDF.
 */
public class ReceiptPdfService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptPdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    private final OrderDao orderDao = new OrderDao();

    /**
     * Generates a PDF receipt for a given Order ID.
     *
     * @param orderId the ID of the order to generate a receipt for
     * @return byte array containing the PDF binary stream
     */
    public byte[] generateReceiptPdf(Long orderId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        List<OrderBook> orderBooks = orderDao.findOrderBooks(orderId);

        // Fetch associated payment and cashier
        Payment payment = null;
        HandledBy handledBy = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            payment = session.createQuery(
                    "FROM Payment p WHERE p.customer.customerId = :cId AND p.paymentAmount = :amt ORDER BY p.paymentId DESC",
                    Payment.class)
                    .setParameter("cId", order.getCustomer().getCustomerId())
                    .setParameter("amt", order.getOrderAmount())
                    .setMaxResults(1)
                    .uniqueResult();

            if (payment != null) {
                handledBy = session.createQuery(
                        "FROM HandledBy hb JOIN FETCH hb.employee WHERE hb.payment.paymentId = :pId",
                        HandledBy.class)
                        .setParameter("pId", payment.getPaymentId())
                        .uniqueResult();
            }
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(24, 43, 73));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(41, 70, 115));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(16, 124, 65));
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            // 1. Header
            Paragraph brand = new Paragraph("BOOKSTORE MANAGEMENT SYSTEM", titleFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);

            Paragraph storeInfo = new Paragraph("Official Digital Sales Invoice & Purchase Receipt\n" +
                    "100 Book Haven Boulevard, Reading City | Tel: (555) 019-2831\n" +
                    "Web: www.bookstore-pos.local | Tax ID: US-9283741-B", subtitleFont);
            storeInfo.setAlignment(Element.ALIGN_CENTER);
            storeInfo.setSpacingAfter(15);
            document.add(storeInfo);

            LineSeparator line = new LineSeparator(1f, 100f, new Color(200, 210, 225), Element.ALIGN_CENTER, -2);
            document.add(line);

            // 2. Metadata Table (Order & Customer Info)
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingBefore(15);
            metaTable.setSpacingAfter(15);
            metaTable.setWidths(new float[]{1f, 1f});

            // Left: Order Info
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph("ORDER INFORMATION", sectionFont));
            leftCell.addElement(new Paragraph("Order Number: #" + order.getOrderId(), boldFont));
            leftCell.addElement(new Paragraph("Date & Time: " + (order.getDate() != null ? order.getDate().format(DATE_FORMATTER) : "N/A"), regularFont));
            if (payment != null) {
                leftCell.addElement(new Paragraph("Payment ID: #" + payment.getPaymentId() + " (" + payment.getMethod() + ")", regularFont));
                leftCell.addElement(new Paragraph("Status: " + payment.getStatus(), boldFont));
            }
            if (handledBy != null && handledBy.getEmployee() != null) {
                leftCell.addElement(new Paragraph("Cashier: " + handledBy.getEmployee().getFirstName() + " " +
                        handledBy.getEmployee().getLastName() + " (" + handledBy.getEmployee().getRole() + ")", regularFont));
            }
            metaTable.addCell(leftCell);

            // Right: Customer Info
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.addElement(new Paragraph("CUSTOMER DETAILS", sectionFont));
            if (order.getCustomer() != null) {
                rightCell.addElement(new Paragraph("Name: " + order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(), boldFont));
                rightCell.addElement(new Paragraph("Email: " + order.getCustomer().getEmailId(), regularFont));
                String address = (order.getCustomer().getStreet() != null ? order.getCustomer().getStreet() + ", " : "") +
                        (order.getCustomer().getCity() != null ? order.getCustomer().getCity() + " " : "") +
                        (order.getCustomer().getPincode() != null ? order.getCustomer().getPincode() : "");
                if (!address.isBlank()) {
                    rightCell.addElement(new Paragraph("Address: " + address, regularFont));
                }
            } else {
                rightCell.addElement(new Paragraph("Customer: Guest Shopper", regularFont));
            }
            metaTable.addCell(rightCell);

            document.add(metaTable);

            // 3. Purchased Items Table
            Paragraph itemsHeading = new Paragraph("PURCHASED ITEMS", sectionFont);
            itemsHeading.setSpacingAfter(8);
            document.add(itemsHeading);

            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{4.5f, 1.2f, 1.8f, 2.0f});

            // Table Headers
            String[] headers = {"Book Title & Description", "Qty", "Unit Price", "Subtotal"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
                cell.setBackgroundColor(new Color(41, 70, 115));
                cell.setPadding(6);
                cell.setHorizontalAlignment(h.equals("Book Title & Description") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
                itemsTable.addCell(cell);
            }

            // Table Rows
            BigDecimal computedSum = BigDecimal.ZERO;
            int rowIndex = 0;
            if (orderBooks != null && !orderBooks.isEmpty()) {
                for (OrderBook ob : orderBooks) {
                    var book = ob.getBook();
                    String title = (book != null) ? book.getTitle() : "Book #" + ob.getId().getBookId();
                    BigDecimal price = (book != null) ? book.getPrice() : BigDecimal.ZERO;

                    // Compute approximate line quantity: if order has total quantity matching items count
                    int itemQty = 1;
                    BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(itemQty));
                    computedSum = computedSum.add(lineTotal);

                    Color bg = (rowIndex % 2 == 0) ? Color.WHITE : new Color(245, 247, 250);

                    // Title
                    PdfPCell titleCell = new PdfPCell(new Phrase(title, regularFont));
                    titleCell.setBackgroundColor(bg);
                    titleCell.setPadding(6);
                    itemsTable.addCell(titleCell);

                    // Quantity
                    PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(itemQty), regularFont));
                    qtyCell.setBackgroundColor(bg);
                    qtyCell.setPadding(6);
                    qtyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    itemsTable.addCell(qtyCell);

                    // Unit Price
                    PdfPCell priceCell = new PdfPCell(new Phrase("$" + price.setScale(2), regularFont));
                    priceCell.setBackgroundColor(bg);
                    priceCell.setPadding(6);
                    priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    itemsTable.addCell(priceCell);

                    // Subtotal
                    PdfPCell totalCell = new PdfPCell(new Phrase("$" + lineTotal.setScale(2), boldFont));
                    totalCell.setBackgroundColor(bg);
                    totalCell.setPadding(6);
                    totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    itemsTable.addCell(totalCell);

                    rowIndex++;
                }
            } else {
                // If order_book rows were not populated, fallback to order summary
                PdfPCell emptyCell = new PdfPCell(new Phrase("Standard Bookstore Checkout Package (" + order.getQuantity() + " items)", regularFont));
                emptyCell.setPadding(6);
                itemsTable.addCell(emptyCell);

                PdfPCell qCell = new PdfPCell(new Phrase(String.valueOf(order.getQuantity()), regularFont));
                qCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                itemsTable.addCell(qCell);

                PdfPCell pCell = new PdfPCell(new Phrase("$" + order.getOrderAmount().setScale(2), regularFont));
                pCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                itemsTable.addCell(pCell);

                PdfPCell tCell = new PdfPCell(new Phrase("$" + order.getOrderAmount().setScale(2), boldFont));
                tCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                itemsTable.addCell(tCell);
            }

            document.add(itemsTable);

            // 4. Totals Summary Box
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(45);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.setSpacingBefore(12);
            totalTable.setWidths(new float[]{1.5f, 1.5f});

            PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL AMOUNT PAID:", totalFont));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setPadding(6);
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(labelCell);

            PdfPCell amountCell = new PdfPCell(new Phrase("$" + order.getOrderAmount().setScale(2), totalFont));
            amountCell.setBorder(Rectangle.NO_BORDER);
            amountCell.setPadding(6);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(amountCell);

            document.add(totalTable);

            // 5. Payment Badge and Footer
            Paragraph paidNotice = new Paragraph("\n✓ PAID IN FULL VIA " +
                    (payment != null ? payment.getMethod() : "ELECTRONIC PAYMENT"),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(16, 124, 65)));
            paidNotice.setAlignment(Element.ALIGN_RIGHT);
            document.add(paidNotice);

            document.add(new Paragraph(" "));
            document.add(line);

            Paragraph footer = new Paragraph(
                    "Thank you for your business! Items in original condition may be returned or exchanged within 14 days of purchase with this receipt.\n" +
                            "For customer support or electronic inquiries, please reach out to support@bookstore-pos.local", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(15);
            document.add(footer);

            document.close();
            logger.info("Generated PDF receipt for Order #{} ({} bytes)", orderId, out.size());
            return out.toByteArray();

        } catch (DocumentException e) {
            logger.error("Error creating OpenPDF document for order " + orderId + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF receipt", e);
        } catch (Exception e) {
            logger.error("Unexpected error generating receipt for order " + orderId + ": " + e.getMessage(), e);
            throw new RuntimeException("PDF generation error", e);
        }
    }

    /**
     * Generates a temporary PDF receipt file on disk for a given Order ID.
     * Useful for attaching to outgoing emails.
     *
     * @param orderId the ID of the order
     * @return temporary File containing the generated PDF
     * @throws IOException if writing the temporary file fails
     */
    public File generateReceiptPdfFile(Long orderId) throws IOException {
        byte[] pdfBytes = generateReceiptPdf(orderId);
        File tempFile = File.createTempFile("receipt-order-" + orderId + "-", ".pdf");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(pdfBytes);
        }
        return tempFile;
    }
}
