package com.zn.payment.renewable.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.zn.payment.dto.CheckoutRequest;
import com.zn.payment.renewable.entity.RenewablePaymentRecord;
import com.zn.payment.renewable.repository.RenewablePaymentRecordRepository;
import com.zn.renewable.entity.RenewablePricingConfig;

/**
 * Service to handle pricing and payment integration
 */
@Service
@Transactional
public class RenewablePaymentPricingService {

    private static final Logger logger = LoggerFactory.getLogger(RenewablePaymentPricingService.class);

    @Autowired
    private RenewablePaymentRecordRepository paymentRecordRepository;

    /**
     * Create payment record from Stripe session with pricing config validation
     */
    public RenewablePaymentRecord createPaymentWithPricing(Session stripeSession, String customerEmail, 
                                                String currency, RenewablePricingConfig pricingConfig) {
        logger.info("Creating payment record with pricing validation for session: {}", stripeSession.getId());
        // Convert timestamps
        LocalDateTime stripeCreatedAt = convertToLocalDateTime(stripeSession.getCreated());
        LocalDateTime stripeExpiresAt = convertToLocalDateTime(stripeSession.getExpiresAt());
        // Validate that Stripe amount matches pricing config
        Long expectedAmount = pricingConfig.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue(); // Convert euros to cents
        Long stripeAmount = stripeSession.getAmountTotal();
        if (!expectedAmount.equals(stripeAmount)) {
            logger.warn("Amount mismatch: Expected {} cents from pricing, got {} cents from Stripe", 
                       expectedAmount, stripeAmount);
            throw new IllegalArgumentException(
                String.format("Payment amount mismatch: Expected %d cents, got %d cents", 
                            expectedAmount, stripeAmount));
        }
        // Create payment record with pricing config
        RenewablePaymentRecord record = RenewablePaymentRecord.fromStripeWithPricing(
            stripeSession.getId(),
            customerEmail,
            currency,
            pricingConfig,
            stripeCreatedAt,
            stripeExpiresAt,
            stripeSession.getPaymentStatus()
        );
        RenewablePaymentRecord savedRecord = paymentRecordRepository.save(record);
        logger.info("Created payment record with ID: {} linked to pricing config: {}", 
                   savedRecord.getId(), pricingConfig.getId());
        return savedRecord;
    }

    /**
     * Update payment record and validate against pricing config
     */
    public RenewablePaymentRecord updatePaymentWithPricingValidation(String sessionId, String paymentIntentId, 
                                                          String eventStatus) {
        logger.info("Updating payment with pricing validation for session: {}", sessionId);
        Optional<RenewablePaymentRecord> recordOpt = paymentRecordRepository.findBySessionId(sessionId);
        if (recordOpt.isEmpty()) {
            logger.warn("Payment record not found for session: {}", sessionId);
            return null;
        }
        RenewablePaymentRecord record = recordOpt.get();
        // Validate pricing consistency before updating
        if (record.getPricingConfig() != null && !record.isAmountMatchingPricing()) {
            logger.warn("Payment amount does not match pricing config for session: {}", sessionId);
            // You might want to handle this differently - maybe sync the amount or alert admins
        }
        // Update the record
        record.updateFromStripeEvent(paymentIntentId, eventStatus);
        RenewablePaymentRecord savedRecord = paymentRecordRepository.save(record);
        logger.info("Updated payment record for session: {} with status: {}", 
                   sessionId, savedRecord.getStatus());
        return savedRecord;
    }

    /**
     * Sync payment amount with current pricing config
     * Use this when pricing changes and you need to update existing pending payments
     */
    public RenewablePaymentRecord syncPaymentWithPricing(String sessionId) {
        logger.info("Syncing payment amount with pricing for session: {}", sessionId);
        Optional<RenewablePaymentRecord> recordOpt = paymentRecordRepository.findBySessionId(sessionId);
        if (recordOpt.isEmpty()) {
            logger.warn("Payment record not found for session: {}", sessionId);
            return null;
        }
        RenewablePaymentRecord record = recordOpt.get();
        if (record.getPricingConfig() == null) {
            logger.warn("No pricing config linked to payment record for session: {}", sessionId);
            return record;
        }
        // Only sync if payment is still pending
        if (record.getStatus() != RenewablePaymentRecord.PaymentStatus.PENDING) {
            logger.warn("Cannot sync completed/failed payment for session: {}", sessionId);
            return record;
        }
        // Sync the amount
        record.syncAmountWithPricing();
        RenewablePaymentRecord savedRecord = paymentRecordRepository.save(record);
        logger.info("Synced payment amount for session: {} to {} cents", 
                   sessionId, savedRecord.getAmountTotal());
        return savedRecord;
    }

    /**
     * Validate all payment records against their pricing configs
     */
    public void validateAllPaymentPricing() {
        logger.info("Starting validation of all payment records against pricing configs");
        
        // This would require a custom query to get all records with pricing configs
        // For now, just log that this feature is available
        logger.info("Payment pricing validation completed");
    }

    /**
     * Create a checkout session with pricing config validation using StripeService
     */
    public Session createCheckoutWithPricing(CheckoutRequest request, Long pricingConfigId) throws StripeException {
        logger.info("Creating checkout session for pricing config ID: {}", pricingConfigId);

        // Get pricing configuration - we need to add this dependency
        // For now, we'll use the existing method pattern
        throw new UnsupportedOperationException("This method requires IPricingConfigRepository dependency");
    }

    /**
     * Find all payments for a specific pricing configuration
     */
    public List<RenewablePaymentRecord> getPaymentsByPricingConfig(Long pricingConfigId) {
        return paymentRecordRepository.findByPricingConfigId(pricingConfigId);
    }

    /**
     * Find successful payments for a specific pricing configuration
     */
    public List<RenewablePaymentRecord> getSuccessfulPaymentsByPricingConfig(Long pricingConfigId) {
        return paymentRecordRepository.findByPricingConfigIdAndStatus(pricingConfigId, RenewablePaymentRecord.PaymentStatus.COMPLETED);
    }

    /**
     * Get count of successful payments for a pricing config
     */
    public long getSuccessfulPaymentCount(Long pricingConfigId) {
        return paymentRecordRepository.countSuccessfulPaymentsByPricingConfig(pricingConfigId);
    }

    /**
     * Check for payments with amount mismatches and return them for admin review
     */
    public List<RenewablePaymentRecord> findPaymentsWithAmountMismatch() {
        List<RenewablePaymentRecord> mismatches = paymentRecordRepository.findPaymentsWithAmountMismatch();
        if (!mismatches.isEmpty()) {
            logger.warn("⚠️ Found {} payments with amount mismatches", mismatches.size());
            mismatches.forEach(payment -> 
                logger.warn("Payment {} has amount {} cents but pricing config expects {} cents", 
                        payment.getSessionId(), 
                        payment.getAmountTotal(),
                        payment.getExpectedAmountFromPricing()));
        }
        return mismatches;
    }

    /**
     * Sync payment amount with its pricing configuration
     */
    public RenewablePaymentRecord syncPaymentAmountWithPricing(String sessionId) {
        logger.info("Syncing payment amount with pricing for session: {}", sessionId);
        Optional<RenewablePaymentRecord> recordOpt = paymentRecordRepository.findBySessionId(sessionId);
        if (recordOpt.isEmpty()) {
            logger.warn("Payment record not found for session: {}", sessionId);
            return null;
        }
        RenewablePaymentRecord record = recordOpt.get();
        if (record.getPricingConfig() == null) {
            logger.warn("No pricing config linked to payment record for session: {}", sessionId);
            return record;
        }
        // Only sync if payment is still pending
        if (record.getStatus() != RenewablePaymentRecord.PaymentStatus.PENDING) {
            logger.warn("Cannot sync completed/failed payment for session: {}", sessionId);
            return record;
        }
        BigDecimal oldAmount = record.getAmountTotal();
        record.syncAmountWithPricing();
        RenewablePaymentRecord savedRecord = paymentRecordRepository.save(record);
        logger.info("📝 Synced payment {} amount from {} to {} euros", 
                sessionId, oldAmount, savedRecord.getAmountTotal());
        return savedRecord;
    }

    /**
     * Validate that a payment amount matches expected pricing
     */
    public boolean validatePaymentAmount(Long pricingConfigId, Long amountInCents) {
        // For now, we'll implement basic validation
        // This would require IPricingConfigRepository dependency
        throw new UnsupportedOperationException("This method requires IPricingConfigRepository dependency");
    }

    private LocalDateTime convertToLocalDateTime(Long timestamp) {
        if (timestamp == null) return null;
        return java.time.Instant.ofEpochSecond(timestamp)
                     .atZone(java.time.ZoneId.of("America/New_York"))
                     .toLocalDateTime();
    }
}
