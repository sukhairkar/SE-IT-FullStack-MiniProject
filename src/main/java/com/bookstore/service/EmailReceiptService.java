package com.bookstore.service;

import com.bookstore.dao.OrderDao;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderBook;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for asynchronously dispatching digital PDF invoices and purchase receipts
 * via Jakarta Mail using multi-part MIME messages with OpenPDF attachments.
 */
public class EmailReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(EmailReceiptService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

    private static final ExecutorService EMAIL_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "BookHaven-EmailDispatcher");
        thread.setDaemon(true);
        return thread;
    });

    private final OrderDao orderDao;

    // SMTP Configuration
    private String smtpHost;
    private int smtpPort;
    private boolean smtpAuth;
    private boolean smtpStartTls;
    private String smtpUsername;
    private String smtpPassword;
    private String smtpFrom;
    private boolean simulationMode;

    public EmailReceiptService() {
        this(new OrderDao());
    }

    public EmailReceiptService(OrderDao orderDao) {
        this.orderDao = orderDao;
        loadConfiguration();
    }

    /**
     * Loads SMTP properties from mail.properties and allows environment variable overrides.
     */
    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            logger.warn("Could not load mail.properties from classpath: {}", e.getMessage());
        }

        // 1. Host & Port
        this.smtpHost = getEnvOrProp("SMTP_HOST", props.getProperty("mail.smtp.host", "smtp.gmail.com"));
        String portStr = getEnvOrProp("SMTP_PORT", props.getProperty("mail.smtp.port", "587"));
        try {
            this.smtpPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            this.smtpPort = 587;
        }

        // 2. Auth & STARTTLS
        this.smtpAuth = Boolean.parseBoolean(getEnvOrProp("SMTP_AUTH", props.getProperty("mail.smtp.auth", "true")));
        this.smtpStartTls = Boolean.parseBoolean(getEnvOrProp("SMTP_STARTTLS", props.getProperty("mail.smtp.starttls.enable", "true")));

        // 3. Credentials & Sender
        this.smtpUsername = getEnvOrProp("SMTP_USERNAME", props.getProperty("mail.smtp.username", ""));
        this.smtpPassword = getEnvOrProp("SMTP_PASSWORD", props.getProperty("mail.smtp.password", ""));
        this.smtpFrom = getEnvOrProp("SMTP_FROM", props.getProperty("mail.smtp.from", "BookHaven Bookstore <no-reply@bookhaven.local>"));

        // 4. Simulation Mode
        String simProp = getEnvOrProp("EMAIL_SIMULATION_MODE", props.getProperty("mail.simulation.mode", "auto"));
        if ("auto".equalsIgnoreCase(simProp)) {
            // Auto-enable simulation mode if username or password is blank
            this.simulationMode = (this.smtpUsername == null || this.smtpUsername.isBlank()
                    || this.smtpPassword == null || this.smtpPassword.isBlank());
        } else {
            this.simulationMode = Boolean.parseBoolean(simProp);
        }

        logger.info("EmailReceiptService initialized. SMTP Host: {}:{}, Auth: {}, Simulation Mode: {}",
                smtpHost, smtpPort, smtpAuth, simulationMode);
    }

    private String getEnvOrProp(String envKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        String sysProp = System.getProperty(envKey.toLowerCase().replace('_', '.'));
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        return defaultValue;
    }

    /**
     * Asynchronously dispatches a digital PDF invoice receipt to a recipient email.
     *
     * @param toEmail recipient email address
     * @param orderId ID of the associated order
     * @param pdfFile the generated OpenPDF receipt file
     * @return CompletableFuture completing with true if sent successfully (or simulated), false on error
     */
    public CompletableFuture<Boolean> sendReceiptWithAttachment(String toEmail, String orderId, File pdfFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
                    logger.warn("Skipping email receipt dispatch: Invalid recipient email '{}' for Order #{}", toEmail, orderId);
                    return false;
                }

                if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0) {
                    logger.warn("Skipping email receipt dispatch: PDF file for Order #{} is missing or empty.", orderId);
                    return false;
                }

                logger.info("Preparing digital receipt dispatch for Order #{} to '{}' (Attachment: {} [{} bytes])...",
                        orderId, toEmail, pdfFile.getName(), pdfFile.length());

                // Build HTML Body content
                String htmlBody = buildHtmlReceiptBody(orderId, toEmail);

                // Check simulation mode
                if (simulationMode) {
                    logger.info("📧 [MOCK EMAIL DISPATCH - SIMULATION MODE]");
                    logger.info("   -> Recipient: {}", toEmail);
                    logger.info("   -> Subject: Your BookHaven Purchase Receipt - Order #{}", orderId);
                    logger.info("   -> Attached PDF: {} ({} bytes)", pdfFile.getName(), pdfFile.length());
                    logger.info("   -> Dispatch status: SIMULATED_SUCCESS (Set mail.simulation.mode=false to send live)");
                    return true;
                }

                // Live Jakarta Mail Dispatch
                Properties mailProps = new Properties();
                mailProps.put("mail.smtp.host", smtpHost);
                mailProps.put("mail.smtp.port", String.valueOf(smtpPort));
                mailProps.put("mail.smtp.auth", String.valueOf(smtpAuth));
                mailProps.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTls));
                mailProps.put("mail.smtp.starttls.required", "true");
                mailProps.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
                mailProps.put("mail.smtp.ssl.trust", smtpHost);

                Session session;
                if (smtpAuth && smtpUsername != null && !smtpUsername.isBlank()) {
                    session = Session.getInstance(mailProps, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(smtpUsername, smtpPassword);
                        }
                    });
                } else {
                    session = Session.getInstance(mailProps);
                }

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(smtpFrom));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("Your BookHaven Purchase Receipt - Order #" + orderId, "UTF-8");

                // Multi-part MIME body (HTML text + PDF attachment)
                MimeMultipart multipart = new MimeMultipart("mixed");

                // 1. HTML Body part
                MimeBodyPart bodyPart = new MimeBodyPart();
                bodyPart.setContent(htmlBody, "text/html; charset=UTF-8");
                multipart.addBodyPart(bodyPart);

                // 2. OpenPDF Attachment part
                MimeBodyPart attachmentPart = new MimeBodyPart();
                FileDataSource source = new FileDataSource(pdfFile);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName("BookHaven_Receipt_Order_" + orderId + ".pdf");
                multipart.addBodyPart(attachmentPart);

                message.setContent(multipart);

                // Send via SMTP
                Transport.send(message);
                logger.info("📧 Successfully sent email receipt for Order #{} to {}", orderId, toEmail);
                return true;

            } catch (Exception e) {
                logger.error("Failed to dispatch email receipt for Order #" + orderId + " to " + toEmail + ": " + e.getMessage(), e);
                return false;
            }
        }, EMAIL_EXECUTOR);
    }

    /**
     * Builds a responsive, beautifully styled HTML email receipt summary.
     */
    private String buildHtmlReceiptBody(String orderId, String toEmail) {
        String customerName = "Valued Customer";
        String orderDateStr = DATE_FORMATTER.format(java.time.LocalDateTime.now());
        String totalAmountStr = "₹0.00";
        int totalItems = 1;
        StringBuilder itemsHtml = new StringBuilder();

        try {
            Long id = Long.parseLong(orderId);
            var orderOpt = orderDao.findById(id);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                if (order.getCustomer() != null) {
                    customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
                }
                if (order.getDate() != null) {
                    orderDateStr = order.getDate().format(DATE_FORMATTER);
                }
                if (order.getOrderAmount() != null) {
                    totalAmountStr = "₹" + order.getOrderAmount().setScale(2);
                }
                totalItems = order.getQuantity();

                List<OrderBook> books = orderDao.findOrderBooks(id);
                for (OrderBook ob : books) {
                    String title = ob.getBook() != null ? ob.getBook().getTitle() : "Book #" + ob.getId().getBookId();
                    String price = ob.getBook() != null && ob.getBook().getPrice() != null
                            ? "₹" + ob.getBook().getPrice().setScale(2)
                            : "-";
                    itemsHtml.append("<tr>")
                            .append("<td style='padding: 8px 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #1e293b; line-height: 1.4; word-break: break-word; width: 68%;'>")
                            .append(escapeHtml(title))
                            .append("</td>")
                            .append("<td align='right' style='padding: 8px 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px; text-align: right; color: #0f172a; font-weight: 600; white-space: nowrap; width: 32%;'>")
                            .append(price)
                            .append("</td>")
                            .append("</tr>");
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not lookup detailed order summary for email: {}", ex.getMessage());
        }

        if (itemsHtml.isEmpty()) {
            itemsHtml.append("<tr>")
                    .append("<td style='padding: 8px 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #1e293b; word-break: break-word; width: 68%;'>Standard Bookstore Checkout Package</td>")
                    .append("<td align='right' style='padding: 8px 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px; text-align: right; color: #0f172a; font-weight: 600; width: 32%;'>").append(totalAmountStr).append("</td>")
                    .append("</tr>");
        }

        return """
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml" lang="en">
            <head>
              <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Your BookHaven Purchase Receipt - Order #%s</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%;">
              
              <!-- Outer Centering Table -->
              <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f1f5f9; width: 100%%; margin: 0; padding: 24px 8px; table-layout: fixed;">
                <tr>
                  <td align="center" style="padding: 0;">
                    
                    <!-- Main Bounded Card Container (max-width 580px, fixed width on desktop) -->
                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 580px; width: 100%%; background-color: #ffffff; border-radius: 10px; border: 1px solid #cbd5e1; overflow: hidden; table-layout: fixed; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                      
                      <!-- Brand Header -->
                      <tr>
                        <td align="center" style="background-color: #0f172a; padding: 26px 20px; color: #ffffff; text-align: center; border-radius: 9px 9px 0 0;">
                          <div style="font-size: 22px; font-weight: 800; letter-spacing: 1.5px; color: #ffffff; margin: 0; text-transform: uppercase;">BOOKHAVEN POS</div>
                          <div style="font-size: 13px; color: #94a3b8; margin-top: 5px; font-weight: 500;">Official In-Store Sales Receipt &amp; Tax Invoice</div>
                        </td>
                      </tr>

                      <!-- Email Body Content -->
                      <tr>
                        <td style="padding: 24px 20px; word-break: break-word; overflow-wrap: break-word;">
                          
                          <div style="font-size: 16px; font-weight: 700; color: #0f172a; margin-bottom: 8px;">Hello %s,</div>
                          <div style="font-size: 14px; color: #475569; line-height: 1.55; margin-bottom: 20px;">
                            Thank you for shopping at BookHaven! Your order has been processed successfully. An official purchase invoice has been generated and attached to this email as a PDF.
                          </div>

                          <!-- Order Summary Box -->
                          <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="width: 100%%; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; margin-bottom: 20px; table-layout: fixed;">
                            <tr>
                              <td style="padding: 14px 16px;">
                                
                                <!-- Metadata Row 1 -->
                                <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="width: 100%%; margin-bottom: 6px; table-layout: fixed;">
                                  <tr>
                                    <td align="left" style="font-size: 13px; color: #64748b; width: 50%%; word-break: break-word; padding: 2px 0;">
                                      Order ID: <strong style="color: #0f172a;">#%s</strong>
                                    </td>
                                    <td align="right" style="font-size: 13px; color: #64748b; width: 50%%; text-align: right; word-break: break-word; padding: 2px 0;">
                                      Date: <strong style="color: #0f172a;">%s</strong>
                                    </td>
                                  </tr>
                                  <tr>
                                    <td align="left" style="font-size: 13px; color: #64748b; width: 50%%; word-break: break-word; padding: 2px 0;">
                                      Customer: <strong style="color: #0f172a;">%s</strong>
                                    </td>
                                    <td align="right" style="font-size: 13px; color: #64748b; width: 50%%; text-align: right; word-break: break-word; padding: 2px 0;">
                                      Total Items: <strong style="color: #0f172a;">%d unit(s)</strong>
                                    </td>
                                  </tr>
                                </table>

                                <!-- Purchased Items Table -->
                                <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="width: 100%%; border-collapse: collapse; margin-top: 10px; table-layout: fixed;">
                                  <thead>
                                    <tr style="background-color: #e2e8f0;">
                                      <th align="left" style="padding: 7px 10px; font-size: 11px; text-transform: uppercase; color: #475569; letter-spacing: 0.5px; width: 68%%;">Book Title</th>
                                      <th align="right" style="padding: 7px 10px; font-size: 11px; text-transform: uppercase; color: #475569; letter-spacing: 0.5px; text-align: right; width: 32%%;">Amount</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    %s
                                    <tr>
                                      <td align="right" style="padding: 10px 10px 0; text-align: right; font-size: 13px; font-weight: 700; color: #1e293b; width: 68%%;">Total Paid:</td>
                                      <td align="right" style="padding: 10px 10px 0; text-align: right; font-size: 15px; font-weight: 800; color: #15803d; white-space: nowrap; width: 32%%;">%s</td>
                                    </tr>
                                  </tbody>
                                </table>

                              </td>
                            </tr>
                          </table>

                          <!-- PDF Attachment Box -->
                          <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="width: 100%%; background-color: #eff6ff; border: 1px solid #bfdbfe; border-left: 4px solid #3b82f6; border-radius: 6px; margin-bottom: 18px; table-layout: fixed;">
                            <tr>
                              <td style="padding: 12px 14px; font-size: 13px; color: #1e40af; line-height: 1.5; word-break: break-word; overflow-wrap: break-word;">
                                📎 <strong>PDF Receipt Attached:</strong> An official invoice (<span style="font-family: monospace; font-size: 12px; background: #dbeafe; padding: 2px 4px; border-radius: 3px; word-break: break-all;">BookHaven_Receipt_Order_%s.pdf</span>) is attached to this email. You can download or print it for your accounting records.
                              </td>
                            </tr>
                          </table>

                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td align="center" style="background-color: #f8fafc; text-align: center; padding: 16px 20px; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; line-height: 1.5; border-radius: 0 0 9px 9px;">
                          <strong>BookHaven Bookstore</strong> &bull; In-Store Retail Billing Desk<br />
                          Questions? Contact us at <a href="mailto:sukantkhairkar@gmail.com" style="color: #64748b; text-decoration: underline;">sukantkhairkar@gmail.com</a>
                        </td>
                      </tr>

                    </table>
                    
                  </td>
                </tr>
              </table>

            </body>
            </html>
            """.formatted(
                escapeHtml(orderId),
                escapeHtml(customerName),
                escapeHtml(orderId),
                escapeHtml(orderDateStr),
                escapeHtml(customerName),
                totalItems,
                itemsHtml.toString(),
                escapeHtml(totalAmountStr),
                escapeHtml(orderId)
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Getters and setters for runtime configuration
    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getSmtpFrom() {
        return smtpFrom;
    }

    public void setSmtpFrom(String smtpFrom) {
        this.smtpFrom = smtpFrom;
    }

    public boolean isSimulationMode() {
        return simulationMode;
    }

    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }
}
