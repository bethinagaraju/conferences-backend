package com.zn.payment.nursing.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zn.payment.nursing.entity.NursingDiscounts;
import com.zn.payment.nursing.entity.NursingPaymentRecord;
import com.zn.payment.nursing.repository.NursingDiscountsRepository;
import com.zn.payment.nursing.repository.NursingPaymentRecordRepository;

/**
 * Service to automatically sync NursingDiscounts with NursingPaymentRecord entities
 * This service ensures discount table is always updated when payment records change
 */
@Service
@Transactional
public class NursingDiscountSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(NursingDiscountSyncService.class);
    
    @Autowired
    private NursingDiscountsRepository discountsRepository;
    
    @Autowired
    private NursingPaymentRecordRepository paymentRecordRepository;
    
    /**
     * Auto-sync discount with payment record whenever payment is updated
     * This is the main method that implements the constraint requirement
     */
    public void autoSyncOnPaymentUpdate(NursingPaymentRecord paymentRecord) {
        if (paymentRecord == null || paymentRecord.getSessionId() == null) {
            logger.warn("⚠️ Cannot auto-sync: payment record or session ID is null");
            return;
        }
        
        logger.info("🔄 Auto-syncing discount for payment record ID: {} with session: {}", 
                   paymentRecord.getId(), paymentRecord.getSessionId());
        
        try {
            // Find existing discount record or create new one
            NursingDiscounts discount = discountsRepository.findBySessionId(paymentRecord.getSessionId());
            boolean isNewDiscount = (discount == null);
            
            if (isNewDiscount) {
                logger.info("📝 Creating new NursingDiscounts record for session: {}", paymentRecord.getSessionId());
                discount = new NursingDiscounts();
                discount.setSessionId(paymentRecord.getSessionId());
            } else {
                logger.info("📝 Updating existing NursingDiscounts ID: {} for session: {}", 
                           discount.getId(), paymentRecord.getSessionId());
            }
            
            // Sync all fields from payment record to discount record
            syncFields(paymentRecord, discount);
            
            // Save the discount record
            NursingDiscounts savedDiscount = discountsRepository.save(discount);
            
            if (isNewDiscount) {
                logger.info("✅ Created new NursingDiscounts ID: {} synced with PaymentRecord ID: {}", 
                           savedDiscount.getId(), paymentRecord.getId());
            } else {
                logger.info("✅ Updated NursingDiscounts ID: {} synced with PaymentRecord ID: {}", 
                           savedDiscount.getId(), paymentRecord.getId());
            }
            
        } catch (Exception e) {
            logger.error("❌ Auto-sync failed for payment record ID {}: {}", 
                        paymentRecord.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Sync fields from payment record to discount record
     * This ensures both tables have the same data
     */
    private void syncFields(NursingPaymentRecord source, NursingDiscounts target) {
        // Core payment fields
        target.setCustomerEmail(source.getCustomerEmail());
        target.setAmountTotal(source.getAmountTotal());
        target.setCurrency(source.getCurrency());
        target.setPaymentIntentId(source.getPaymentIntentId());
        target.setStripeCreatedAt(source.getStripeCreatedAt());
        target.setStripeExpiresAt(source.getStripeExpiresAt());
        target.setPaymentStatus(source.getPaymentStatus());
        
        // Map PaymentRecord status to Discount status
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
        
        logger.debug("🔄 Synced fields: email={}, amount={}, currency={}, status={}", 
                    target.getCustomerEmail(), target.getAmountTotal(), 
                    target.getCurrency(), target.getStatus());
    }
    
    /**
     * Manual sync method for specific session ID
     * Can be called when needed for specific records
     */
    public boolean syncDiscountWithPaymentRecord(String sessionId) {
        logger.info("🔄 Manual sync for session ID: {}", sessionId);
        
        try {
            Optional<NursingPaymentRecord> paymentRecordOpt = paymentRecordRepository.findBySessionId(sessionId);
            if (paymentRecordOpt.isEmpty()) {
                logger.warn("❌ No NursingPaymentRecord found for session ID: {}", sessionId);
                return false;
            }
            
            autoSyncOnPaymentUpdate(paymentRecordOpt.get());
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Manual sync failed for session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }
}