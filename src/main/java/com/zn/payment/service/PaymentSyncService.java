package com.zn.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zn.payment.nursing.repository.NursingPaymentRecordRepository;
import com.zn.payment.optics.repository.OpticsPaymentRecordRepository;
import com.zn.payment.renewable.repository.RenewablePaymentRecordRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Centralized Payment Synchronization Service
 * 
 * This service handles database-level synchronization between payment records and discount records
 * across all three services (Optics, Nursing, Renewable).
 * 
 * The service calls database stored procedures that ensure consistency between
 * payment_records and discounts tables based on session_id.
 * 
 * Key Features:
 * - Syncs individual services by session_id
 * - Syncs all services for a given session_id
 * - Provides detailed logging of sync operations
 * - Handles errors gracefully with fallback mechanisms
 */
@Slf4j
@Service
public class PaymentSyncService {
    
    @Autowired
    private OpticsPaymentRecordRepository opticsPaymentRecordRepository;
    
    @Autowired
    private NursingPaymentRecordRepository nursingPaymentRecordRepository;
    
    @Autowired
    private RenewablePaymentRecordRepository renewablePaymentRecordRepository;
    
    /**
     * Synchronize optics payment and discount records for a given session_id
     * 
     * @param sessionId The Stripe session ID
     * @return Sync result message
     */
    public String syncOpticsPaymentBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("⚠️ Cannot sync optics: session ID is null or empty");
            return "Session ID is null or empty";
        }
        
        try {
            String result = opticsPaymentRecordRepository.syncOpticsBySessionId(sessionId);
            log.info("✅ Optics sync result for session {}: {}", sessionId, result);
            return result;
        } catch (Exception e) {
            log.error("❌ Failed to sync optics for session {}: {}", sessionId, e.getMessage());
            return "Sync failed: " + e.getMessage();
        }
    }
    
    /**
     * Synchronize nursing payment and discount records for a given session_id
     * 
     * @param sessionId The Stripe session ID
     * @return Sync result message
     */
    public String syncNursingPaymentBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("⚠️ Cannot sync nursing: session ID is null or empty");
            return "Session ID is null or empty";
        }
        
        try {
            String result = nursingPaymentRecordRepository.syncNursingBySessionId(sessionId);
            log.info("✅ Nursing sync result for session {}: {}", sessionId, result);
            return result;
        } catch (Exception e) {
            log.error("❌ Failed to sync nursing for session {}: {}", sessionId, e.getMessage());
            return "Sync failed: " + e.getMessage();
        }
    }
    
    /**
     * Synchronize renewable payment and discount records for a given session_id
     * 
     * @param sessionId The Stripe session ID
     * @return Sync result message
     */
    public String syncRenewablePaymentBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("⚠️ Cannot sync renewable: session ID is null or empty");
            return "Session ID is null or empty";
        }
        
        try {
            String result = renewablePaymentRecordRepository.syncRenewableBySessionId(sessionId);
            log.info("✅ Renewable sync result for session {}: {}", sessionId, result);
            return result;
        } catch (Exception e) {
            log.error("❌ Failed to sync renewable for session {}: {}", sessionId, e.getMessage());
            return "Sync failed: " + e.getMessage();
        }
    }
    
    /**
     * Synchronize all services (Optics, Nursing, Renewable) for a given session_id
     * 
     * This method calls the sync function for each service and returns a consolidated result.
     * 
     * @param sessionId The Stripe session ID
     * @return Consolidated sync results for all services
     */
    public SyncResult syncAllServicesBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("⚠️ Cannot sync all services: session ID is null or empty");
            return new SyncResult(sessionId, "Session ID is null or empty", "Session ID is null or empty", "Session ID is null or empty");
        }
        
        log.info("🔄 Starting sync for all services with session: {}", sessionId);
        
        String opticsResult = syncOpticsPaymentBySessionId(sessionId);
        String nursingResult = syncNursingPaymentBySessionId(sessionId);
        String renewableResult = syncRenewablePaymentBySessionId(sessionId);
        
        SyncResult result = new SyncResult(sessionId, opticsResult, nursingResult, renewableResult);
        
        log.info("✅ Completed sync for all services with session {}: Optics={}, Nursing={}, Renewable={}", 
                sessionId, opticsResult, nursingResult, renewableResult);
        
        return result;
    }
    
    /**
     * Check if a sync result indicates success
     * 
     * @param syncResult The result string from a sync operation
     * @return true if the sync was successful, false otherwise
     */
    public boolean isSyncSuccessful(String syncResult) {
        if (syncResult == null) {
            return false;
        }
        
        String result = syncResult.toLowerCase();
        return result.contains("synced") || result.contains("success");
    }
    
    /**
     * Check if a sync result indicates that only payment record exists (no discount record)
     * 
     * @param syncResult The result string from a sync operation
     * @return true if only payment record exists, false otherwise
     */
    public boolean isPaymentOnlyResult(String syncResult) {
        if (syncResult == null) {
            return false;
        }
        
        return syncResult.contains("Only") && syncResult.contains("payment record exists");
    }
    
    /**
     * Check if a sync result indicates that only discount record exists (no payment record)
     * 
     * @param syncResult The result string from a sync operation
     * @return true if only discount record exists, false otherwise
     */
    public boolean isDiscountOnlyResult(String syncResult) {
        if (syncResult == null) {
            return false;
        }
        
        return syncResult.contains("Only") && syncResult.contains("discount record exists");
    }
    
    /**
     * Data class to hold sync results for all services
     */
    public static class SyncResult {
        private final String sessionId;
        private final String opticsResult;
        private final String nursingResult;
        private final String renewableResult;
        
        public SyncResult(String sessionId, String opticsResult, String nursingResult, String renewableResult) {
            this.sessionId = sessionId;
            this.opticsResult = opticsResult;
            this.nursingResult = nursingResult;
            this.renewableResult = renewableResult;
        }
        
        public String getSessionId() { return sessionId; }
        public String getOpticsResult() { return opticsResult; }
        public String getNursingResult() { return nursingResult; }
        public String getRenewableResult() { return renewableResult; }
        
        @Override
        public String toString() {
            return String.format("SyncResult{sessionId='%s', optics='%s', nursing='%s', renewable='%s'}", 
                    sessionId, opticsResult, nursingResult, renewableResult);
        }
    }
}
