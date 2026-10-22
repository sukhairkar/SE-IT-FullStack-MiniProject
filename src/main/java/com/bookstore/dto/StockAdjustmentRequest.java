package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload for quick stock adjustments with audit reason.
 * Endpoint: PATCH /api/owner/books/{id}/stock
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockAdjustmentRequest {

    private Integer adjustment; // Delta to add (e.g. +20) or subtract (e.g. -1)
    private Integer newStock;   // Alternative absolute stock setting
    private Integer additionalStock; // Backwards compatible alias for positive restock
    private String reason;      // Audit reason, e.g. "+20 Restock" or "-1 Damaged"

    public StockAdjustmentRequest() {
    }

    public StockAdjustmentRequest(Integer adjustment, String reason) {
        this.adjustment = adjustment;
        this.reason = reason;
    }

    public Integer getAdjustment() {
        if (adjustment != null) {
            return adjustment;
        }
        if (additionalStock != null) {
            return additionalStock;
        }
        return null;
    }

    public void setAdjustment(Integer adjustment) {
        this.adjustment = adjustment;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public Integer getAdditionalStock() {
        return additionalStock;
    }

    public void setAdditionalStock(Integer additionalStock) {
        this.additionalStock = additionalStock;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
