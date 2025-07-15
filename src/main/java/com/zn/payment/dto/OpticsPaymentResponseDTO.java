package com.zn.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zn.payment.optics.entity.OpticsPaymentRecord;
import com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpticsPaymentResponseDTO {
    
    private Long id;
    private String sessionId;
    private String paymentIntentId;
    private String customerEmail;
    private String url; // Stripe checkout session URL
    private BigDecimal amountTotalEuros; // Amount in euros (database format)
    private Long amountTotalCents; // Amount in cents (for Stripe API compatibility)
    private String currency;
    private PaymentStatus status;
    private String paymentStatus;
    private LocalDateTime stripeCreatedAt;
    private LocalDateTime stripeExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Pricing Config information
    private Long pricingConfigId;
    private BigDecimal pricingConfigTotalPrice;
    
    // Customer and session metadata from Stripe
    private String registrationType;
    private String presentationType;
    private BigDecimal presentationPrice;
    private BigDecimal processingFeePercent;
    private String customerName;
    private String customerInstitute;
    private String customerCountry;
    private String productName;
    private String description;
    
    /**
     * Convert PaymentRecord entity to DTO with total price information
     */
    public static OpticsPaymentResponseDTO fromEntity(OpticsPaymentRecord record) {
    	OpticsPaymentResponseDTOBuilder builder = OpticsPaymentResponseDTO.builder()
            .id(record.getId())
            .sessionId(record.getSessionId())
            .paymentIntentId(record.getPaymentIntentId())
            .customerEmail(record.getCustomerEmail())
            .amountTotalEuros(record.getAmountTotal()) // Amount already stored in euros
            .amountTotalCents(record.getAmountTotal() != null ? 
                record.getAmountTotal().multiply(BigDecimal.valueOf(100)).longValue() : null) // Convert euros to cents
            .currency(record.getCurrency())
            .status(record.getStatus())
            .paymentStatus(record.getPaymentStatus())
            .stripeCreatedAt(record.getStripeCreatedAt())
            .stripeExpiresAt(record.getStripeExpiresAt())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt());
            
        // Add pricing config information if available
        if (record.getPricingConfig() != null) {
            builder.pricingConfigId(record.getPricingConfig().getId())
                   .pricingConfigTotalPrice(record.getPricingConfig().getTotalPrice());
        }
        
        return builder.build();
    }
}
