package com.zn.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// import com.zn.payment.entity.PaymentRecord; // Removed, PaymentRecord does not exist as a base class
import com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    
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
    // Removed fromEntity(PaymentRecord record) as there is no common PaymentRecord base class. Use the vertical-specific methods below.

    /**
     * Convert OpticsPaymentRecord entity to DTO
     */
    public static PaymentResponseDTO fromEntity(com.zn.payment.optics.entity.OpticsPaymentRecord record) {
        PaymentResponseDTOBuilder builder = PaymentResponseDTO.builder()
            .id(record.getId())
            .sessionId(record.getSessionId())
            .paymentIntentId(record.getPaymentIntentId())
            .customerEmail(record.getCustomerEmail())
            .amountTotalEuros(record.getAmountTotal())
            .amountTotalCents(record.getAmountTotal() != null ?
                record.getAmountTotal().multiply(BigDecimal.valueOf(100)).longValue() : null)
            .currency(record.getCurrency())
            .status(PaymentStatus.valueOf(record.getStatus().name()))
            .paymentStatus(record.getPaymentStatus())
            .stripeCreatedAt(record.getStripeCreatedAt())
            .stripeExpiresAt(record.getStripeExpiresAt())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt());
        if (record.getPricingConfig() != null) {
            builder.pricingConfigId(record.getPricingConfig().getId())
                   .pricingConfigTotalPrice(record.getPricingConfig().getTotalPrice());
        }
        return builder.build();
    }

    /**
     * Convert RenewablePaymentRecord entity to DTO
     */
    public static PaymentResponseDTO fromEntity(com.zn.payment.renewable.entity.RenewablePaymentRecord record) {
        PaymentResponseDTOBuilder builder = PaymentResponseDTO.builder()
            .id(record.getId())
            .sessionId(record.getSessionId())
            .paymentIntentId(record.getPaymentIntentId())
            .customerEmail(record.getCustomerEmail())
            .amountTotalEuros(record.getAmountTotal())
            .amountTotalCents(record.getAmountTotal() != null ?
                record.getAmountTotal().multiply(BigDecimal.valueOf(100)).longValue() : null)
            .currency(record.getCurrency())
            .status(PaymentStatus.valueOf(record.getStatus().name()))
            .paymentStatus(record.getPaymentStatus())
            .stripeCreatedAt(record.getStripeCreatedAt())
            .stripeExpiresAt(record.getStripeExpiresAt())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt());
        if (record.getPricingConfig() != null) {
            builder.pricingConfigId(record.getPricingConfig().getId())
                   .pricingConfigTotalPrice(record.getPricingConfig().getTotalPrice());
        }
        return builder.build();
    }

    /**
     * Convert NursingPaymentRecord entity to DTO
     */
    public static PaymentResponseDTO fromEntity(com.zn.payment.nursing.entity.NursingPaymentRecord record) {
        PaymentResponseDTOBuilder builder = PaymentResponseDTO.builder()
            .id(record.getId())
            .sessionId(record.getSessionId())
            .paymentIntentId(record.getPaymentIntentId())
            .customerEmail(record.getCustomerEmail())
            .amountTotalEuros(record.getAmountTotal())
            .amountTotalCents(record.getAmountTotal() != null ?
                record.getAmountTotal().multiply(BigDecimal.valueOf(100)).longValue() : null)
            .currency(record.getCurrency())
            .status(PaymentStatus.valueOf(record.getStatus().name()))
            .paymentStatus(record.getPaymentStatus())
            .stripeCreatedAt(record.getStripeCreatedAt())
            .stripeExpiresAt(record.getStripeExpiresAt())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt());
        if (record.getPricingConfig() != null) {
            builder.pricingConfigId(record.getPricingConfig().getId())
                   .pricingConfigTotalPrice(record.getPricingConfig().getTotalPrice());
        }
        return builder.build();
    }

    /**
     * Convert PolymersPaymentRecord entity to DTO
     */
    public static PaymentResponseDTO fromEntity(com.zn.payment.polymers.entity.PolymersPaymentRecord record) {
        PaymentResponseDTOBuilder builder = PaymentResponseDTO.builder()
            .id(record.getId())
            .sessionId(record.getSessionId())
            .paymentIntentId(record.getPaymentIntentId())
            .customerEmail(record.getCustomerEmail())
            .amountTotalEuros(record.getAmountTotal())
            .amountTotalCents(record.getAmountTotal() != null ?
                record.getAmountTotal().multiply(BigDecimal.valueOf(100)).longValue() : null)
            .currency(record.getCurrency())
            .status(PaymentStatus.valueOf(record.getStatus().name()))
            .paymentStatus(record.getPaymentStatus())
            .stripeCreatedAt(record.getStripeCreatedAt())
            .stripeExpiresAt(record.getStripeExpiresAt())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt());
        if (record.getPricingConfig() != null) {
            builder.pricingConfigId(record.getPricingConfig().getId())
                   .pricingConfigTotalPrice(record.getPricingConfig().getTotalPrice());
        }
        return builder.build();
    }
}
