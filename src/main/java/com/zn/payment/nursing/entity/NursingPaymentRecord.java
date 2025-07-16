package com.zn.payment.nursing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zn.nursing.entity.NursingPricingConfig;
import com.zn.nursing.entity.NursingRegistrationForm;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nursing_payment_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NursingPaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 500)
    private String sessionId;

    @Column(length = 500)
    private String paymentIntentId;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountTotal; // Amount in euros (e.g., 45.00)

    @Column(length = 3)
    @Builder.Default
    private String currency = "eur";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // Stripe-specific timestamps (from Stripe response)
    @Column(nullable = false)
    private LocalDateTime stripeCreatedAt;

    private LocalDateTime stripeExpiresAt;

    // System timestamps (for our internal tracking)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Additional Stripe fields
    @Column(length = 50)
    private String paymentStatus; // Stripe's payment_status

    // Relationship to PricingConfig
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_config_id")
    private NursingPricingConfig pricingConfig;
    
    // One-to-One relationship with RegistrationForm (bidirectional)
    @OneToOne(mappedBy = "paymentRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonBackReference
    private NursingRegistrationForm registrationForm;


    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        // Set default status if not provided
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        
        // Set default paymentStatus if not provided
        if (paymentStatus == null) {
            paymentStatus = "unpaid"; // Default Stripe payment status for new records
        }
        
        // Validate amount matches pricing config if available
        validateAmountWithPricingConfig();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Validate amount matches pricing config if available
        validateAmountWithPricingConfig();
    }
    
    /**
     * Validates that the payment amount matches the pricing configuration
     */
    private void validateAmountWithPricingConfig() {
        if (pricingConfig != null && pricingConfig.getTotalPrice() != null) {
            BigDecimal expectedAmount = pricingConfig.getTotalPrice();
            
            if (amountTotal != null && expectedAmount.compareTo(amountTotal) != 0) {
                throw new IllegalStateException(
                    String.format("Payment amount (%.2f euros) does not match pricing config total (%.2f euros)", 
                                amountTotal.doubleValue(), expectedAmount.doubleValue()));
            }
        }
    }

    // Enum for payment status
    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED,
        EXPIRED
    }

    // Factory method to create PaymentRecord from Stripe response data
    public static NursingPaymentRecord fromStripeResponse(String sessionId, String customerEmail, 
                                                 BigDecimal amountTotalEuros, String currency,
                                                 LocalDateTime stripeCreatedAt, 
                                                 LocalDateTime stripeExpiresAt,
                                                 String paymentStatus) {
        return NursingPaymentRecord.builder()
                .sessionId(sessionId)
                .customerEmail(customerEmail)
                .amountTotal(amountTotalEuros)
                .currency(currency)
                .stripeCreatedAt(stripeCreatedAt)
                .stripeExpiresAt(stripeExpiresAt)
                .paymentStatus(paymentStatus != null ? paymentStatus : "unpaid") // Default to "unpaid" if null
                .status(PaymentStatus.PENDING)
                .build();
    }

    // Factory method to create PaymentRecord with PricingConfig
    public static NursingPaymentRecord fromPricingConfig(String sessionId, String customerEmail, 
                                                String currency, NursingPricingConfig pricingConfig,
                                                LocalDateTime stripeCreatedAt, 
                                                LocalDateTime stripeExpiresAt,
                                                String paymentStatus) {
        // Use euro amount directly from pricing config
        BigDecimal amountInEuros = pricingConfig.getTotalPrice();
        
        return NursingPaymentRecord.builder()
                .sessionId(sessionId)
                .customerEmail(customerEmail)
                .amountTotal(amountInEuros)
                .currency(currency)
                .stripeCreatedAt(stripeCreatedAt)
                .stripeExpiresAt(stripeExpiresAt)
                .paymentStatus(paymentStatus != null ? paymentStatus : "unpaid") // Default to "unpaid" if null
                .pricingConfig(pricingConfig)
                .status(PaymentStatus.PENDING)
                .build();
    }

    // Factory method for creating PaymentRecord with both Stripe session and PricingConfig
    public static NursingPaymentRecord fromStripeWithPricing(String sessionId, String customerEmail,
                                                     String currency, NursingPricingConfig pricingConfig,
                                                     LocalDateTime stripeCreatedAt,
                                                     LocalDateTime stripeExpiresAt,
                                                     String paymentStatus) {
    	NursingPaymentRecord record = fromPricingConfig(sessionId, customerEmail, currency, pricingConfig,
                                               stripeCreatedAt, stripeExpiresAt, paymentStatus);
        return record;
    }

    // Method to update from Stripe webhook data
    public void updateFromStripeEvent(String paymentIntentId, String eventStatus) {
        this.paymentIntentId = paymentIntentId;
        
        // Map Stripe event status to our enum and set paymentStatus accordingly
        switch (eventStatus.toLowerCase()) {
            case "complete":
            case "paid":
                this.status = PaymentStatus.COMPLETED;
                this.paymentStatus = "paid";
                break;
            case "expired":
                this.status = PaymentStatus.EXPIRED;
                this.paymentStatus = "expired";
                break;
            case "canceled":
            case "cancelled":
                this.status = PaymentStatus.CANCELLED;
                this.paymentStatus = "canceled";
                break;
            case "failed":
                this.status = PaymentStatus.FAILED;
                this.paymentStatus = "failed";
                break;
            case "pending":
            case "unpaid":
            default:
                this.status = PaymentStatus.PENDING;
                this.paymentStatus = "unpaid";
        }
    }
    
    // Method to update from Stripe session data (for checkout.session.completed events)
    public void updateFromStripeSession(String paymentIntentId, String sessionStatus, String paymentStatus) {
        this.paymentIntentId = paymentIntentId;
        this.paymentStatus = paymentStatus != null ? paymentStatus : "unpaid";
        
        // Map Stripe session status to our enum
        if ("complete".equals(sessionStatus) && "paid".equals(paymentStatus)) {
            this.status = PaymentStatus.COMPLETED;
        } else if ("expired".equals(sessionStatus)) {
            this.status = PaymentStatus.EXPIRED;
            this.paymentStatus = "expired";
        } else if ("canceled".equals(sessionStatus)) {
            this.status = PaymentStatus.CANCELLED;
            this.paymentStatus = "canceled";
        } else {
            this.status = PaymentStatus.PENDING;
            if (this.paymentStatus == null || this.paymentStatus.isEmpty()) {
                this.paymentStatus = "unpaid";
            }
        }
    }

    // Convenience methods
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isExpired() {
        return status == PaymentStatus.EXPIRED || 
               (stripeExpiresAt != null && stripeExpiresAt.isBefore(LocalDateTime.now()));
    }
    
    public boolean isPaid() {
        return "paid".equals(paymentStatus);
    }
    
    public boolean isUnpaid() {
        return "unpaid".equals(paymentStatus);
    }
    
    public boolean isPaymentFailed() {
        return "failed".equals(paymentStatus);
    }

    // Format amount for display (already in euros)
    public BigDecimal getAmountInEuros() {
        return amountTotal != null ? amountTotal : BigDecimal.ZERO;
    }
    
    /**
     * Get the expected amount from pricing config in euros
     */
    public BigDecimal getExpectedAmountFromPricing() {
        if (pricingConfig == null || pricingConfig.getTotalPrice() == null) {
            return null;
        }
        return pricingConfig.getTotalPrice();
    }
    
    /**
     * Check if payment amount matches the pricing configuration
     */
    public boolean isAmountMatchingPricing() {
        BigDecimal expectedAmount = getExpectedAmountFromPricing();
        return expectedAmount != null && expectedAmount.compareTo(amountTotal) == 0;
    }
    
    /**
     * Get the pricing config total price in euros
     */
    public double getPricingConfigTotalInDollars() {
        if (pricingConfig == null || pricingConfig.getTotalPrice() == null) {
            return 0.0;
        }
        return pricingConfig.getTotalPrice().doubleValue();
    }
    
    /**
     * Update the payment amount to match the pricing configuration
     * Use this when pricing changes and you need to sync the payment
     */
    public void syncAmountWithPricing() {
        if (pricingConfig != null && pricingConfig.getTotalPrice() != null) {
            this.amountTotal = getExpectedAmountFromPricing();
        }
    }
}
