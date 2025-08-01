package com.zn.payment.polymers.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class PolymersPaymentResponseDTO {
    private String sessionId;
    private String url;
    private String paymentStatus;
    private String description;
    // Constructor for error responses
    public PolymersPaymentResponseDTO(String paymentStatus, String description) {
        this.paymentStatus = paymentStatus;
        this.description = description;
    }
    private LocalDateTime stripeCreatedAt;
    private LocalDateTime stripeExpiresAt;
    private Long id;
    private String customerEmail;
    private BigDecimal amountTotalEuros;
    private Long amountTotalCents;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long pricingConfigId;
    private BigDecimal pricingConfigTotalPrice;
    private String customerName;
    private String productName;

    // Getters and setters for all fields
    // ...

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStripeCreatedAt() { return stripeCreatedAt; }
    public void setStripeCreatedAt(LocalDateTime stripeCreatedAt) { this.stripeCreatedAt = stripeCreatedAt; }
    public LocalDateTime getStripeExpiresAt() { return stripeExpiresAt; }
    public void setStripeExpiresAt(LocalDateTime stripeExpiresAt) { this.stripeExpiresAt = stripeExpiresAt; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public BigDecimal getAmountTotalEuros() { return amountTotalEuros; }
    public void setAmountTotalEuros(BigDecimal amountTotalEuros) { this.amountTotalEuros = amountTotalEuros; }
    public Long getAmountTotalCents() { return amountTotalCents; }
    public void setAmountTotalCents(Long amountTotalCents) { this.amountTotalCents = amountTotalCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getPricingConfigId() { return pricingConfigId; }
    public void setPricingConfigId(Long pricingConfigId) { this.pricingConfigId = pricingConfigId; }
    public BigDecimal getPricingConfigTotalPrice() { return pricingConfigTotalPrice; }
    public void setPricingConfigTotalPrice(BigDecimal pricingConfigTotalPrice) { this.pricingConfigTotalPrice = pricingConfigTotalPrice; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
