-- FIXED SQL CONSTRAINTS FOR ALL 3 SERVICES - JAVA APPLICATION INTEGRATION
-- This version is designed to work with JPA/Hibernate entities in your Spring Boot application
-- Includes Optics, Nursing, and Renewable services

-- ====================================
-- 1. DATABASE CONSTRAINTS (BASIC)
-- ====================================

-- Ensure session_id consistency with unique constraints for all services
ALTER TABLE optics_payment_records 
ADD CONSTRAINT uk_optics_payment_session_id UNIQUE (session_id);

ALTER TABLE optics_discounts 
ADD CONSTRAINT uk_optics_discounts_session_id UNIQUE (session_id);

ALTER TABLE nursing_payment_records 
ADD CONSTRAINT uk_nursing_payment_session_id UNIQUE (session_id);

ALTER TABLE nursing_discounts 
ADD CONSTRAINT uk_nursing_discounts_session_id UNIQUE (session_id);

ALTER TABLE renewable_payment_records 
ADD CONSTRAINT uk_renewable_payment_session_id UNIQUE (session_id);

ALTER TABLE renewable_discounts 
ADD CONSTRAINT uk_renewable_discounts_session_id UNIQUE (session_id);

-- ====================================
-- 2. OPTICS SERVICE FUNCTIONS
-- ====================================

-- Simple function to sync optics records by session_id (called from Java)
CREATE OR REPLACE FUNCTION sync_optics_by_session_id(p_session_id VARCHAR(500))
RETURNS TEXT AS $$
DECLARE
    payment_rec RECORD;
    discount_rec RECORD;
    result_msg TEXT := 'No action taken';
BEGIN
    -- Get payment record
    SELECT payment_intent_id, status, payment_status, updated_at, amount_total, currency, customer_email
    INTO payment_rec
    FROM optics_payment_records 
    WHERE session_id = p_session_id;
    
    -- Get discount record
    SELECT payment_intent_id, status, payment_status, updated_at, amount_total, currency, customer_email
    INTO discount_rec
    FROM optics_discounts 
    WHERE session_id = p_session_id;
    
    -- If both records exist, sync them
    IF payment_rec IS NOT NULL AND discount_rec IS NOT NULL THEN
        -- Update discount record with payment record data (payment record is source of truth)
        UPDATE optics_discounts 
        SET 
            payment_intent_id = payment_rec.payment_intent_id,
            status = payment_rec.status,
            payment_status = payment_rec.payment_status,
            updated_at = NOW(),
            amount_total = payment_rec.amount_total,
            currency = payment_rec.currency,
            customer_email = payment_rec.customer_email
        WHERE session_id = p_session_id;
        
        result_msg := 'Synced optics discount from payment record for session: ' || p_session_id;
    ELSIF payment_rec IS NOT NULL THEN
        result_msg := 'Only optics payment record exists for session: ' || p_session_id;
    ELSIF discount_rec IS NOT NULL THEN
        result_msg := 'Only optics discount record exists for session: ' || p_session_id;
    ELSE
        result_msg := 'No optics records found for session: ' || p_session_id;
    END IF;
    
    RETURN result_msg;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 3. NURSING SERVICE FUNCTIONS
-- ====================================

-- Simple function to sync nursing records by session_id (called from Java)
CREATE OR REPLACE FUNCTION sync_nursing_by_session_id(p_session_id VARCHAR(500))
RETURNS TEXT AS $$
DECLARE
    payment_rec RECORD;
    discount_rec RECORD;
    result_msg TEXT := 'No action taken';
BEGIN
    -- Get payment record
    SELECT payment_intent_id, status, payment_status, updated_at, amount_total, currency, customer_email
    INTO payment_rec
    FROM nursing_payment_records 
    WHERE session_id = p_session_id;
    
    -- Get discount record
    SELECT payment_intent_id, status, payment_status, updated_at, amount_total, currency, customer_email
    INTO discount_rec
    FROM nursing_discounts 
    WHERE session_id = p_session_id;
    
    -- If both records exist, sync them
    IF payment_rec IS NOT NULL AND discount_rec IS NOT NULL THEN
        -- Update discount record with payment record data (payment record is source of truth)
        UPDATE nursing_discounts 
        SET 
            payment_intent_id = payment_rec.payment_intent_id,
            status = payment_rec.status,
            payment_status = payment_rec.payment_status,
            updated_at = NOW(),
            amount_total = payment_rec.amount_total,
            currency = payment_rec.currency,
            customer_email = payment_rec.customer_email
        WHERE session_id = p_session_id;
        
        result_msg := 'Synced nursing discount from payment record for session: ' || p_session_id;
    ELSIF payment_rec IS NOT NULL THEN
        result_msg := 'Only nursing payment record exists for session: ' || p_session_id;
    ELSIF discount_rec IS NOT NULL THEN
        result_msg := 'Only nursing discount record exists for session: ' || p_session_id;
    ELSE
        result_msg := 'No nursing records found for session: ' || p_session_id;
    END IF;
    
    RETURN result_msg;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 4. RENEWABLE SERVICE FUNCTIONS
-- ====================================

-- Simple function to sync renewable records by session_id (called from Java)
CREATE OR REPLACE FUNCTION sync_renewable_by_session_id(p_session_id VARCHAR(500))
RETURNS TEXT AS $$
DECLARE
    payment_rec RECORD;
    discount_rec RECORD;
    result_msg TEXT := 'No action taken';
BEGIN
    -- Get payment record
    SELECT payment_intent_id, status, payment_status, updated_at, amount_total, currency, customer_email
    INTO payment_rec
    FROM renewable_payment_records 
    WHERE session_id = p_session_id;
    
    -- Get discount record
    SELECT payment_intent_id, status, payment_status, updated_at, amount_total, currency, customer_email
    INTO discount_rec
    FROM renewable_discounts 
    WHERE session_id = p_session_id;
    
    -- If both records exist, sync them
    IF payment_rec IS NOT NULL AND discount_rec IS NOT NULL THEN
        -- Update discount record with payment record data (payment record is source of truth)
        UPDATE renewable_discounts 
        SET 
            payment_intent_id = payment_rec.payment_intent_id,
            status = payment_rec.status,
            payment_status = payment_rec.payment_status,
            updated_at = NOW(),
            amount_total = payment_rec.amount_total,
            currency = payment_rec.currency,
            customer_email = payment_rec.customer_email
        WHERE session_id = p_session_id;
        
        result_msg := 'Synced renewable discount from payment record for session: ' || p_session_id;
    ELSIF payment_rec IS NOT NULL THEN
        result_msg := 'Only renewable payment record exists for session: ' || p_session_id;
    ELSIF discount_rec IS NOT NULL THEN
        result_msg := 'Only renewable discount record exists for session: ' || p_session_id;
    ELSE
        result_msg := 'No renewable records found for session: ' || p_session_id;
    END IF;
    
    RETURN result_msg;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 5. MASTER SYNC FUNCTION
-- ====================================

-- Function to sync all services for a given session_id
CREATE OR REPLACE FUNCTION sync_all_services_by_session_id(p_session_id VARCHAR(500))
RETURNS TABLE(service_name TEXT, result TEXT) AS $$
BEGIN
    RETURN QUERY SELECT 'Optics'::TEXT, sync_optics_by_session_id(p_session_id);
    RETURN QUERY SELECT 'Nursing'::TEXT, sync_nursing_by_session_id(p_session_id);
    RETURN QUERY SELECT 'Renewable'::TEXT, sync_renewable_by_session_id(p_session_id);
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 6. VERIFICATION VIEWS
-- ====================================

-- Combined view to check sync status across all services
CREATE OR REPLACE VIEW all_services_sync_check AS
SELECT 
    'Optics' AS service_name,
    COALESCE(p.session_id, d.session_id) AS session_id,
    p.payment_intent_id AS payment_intent_id_payment,
    d.payment_intent_id AS payment_intent_id_discount,
    p.status AS status_payment,
    d.status AS status_discount,
    p.payment_status AS payment_status_payment,
    d.payment_status AS payment_status_discount,
    p.updated_at AS updated_at_payment,
    d.updated_at AS updated_at_discount,
    CASE 
        WHEN p.session_id IS NULL THEN 'DISCOUNT_ONLY'
        WHEN d.session_id IS NULL THEN 'PAYMENT_ONLY'
        WHEN p.payment_intent_id IS DISTINCT FROM d.payment_intent_id OR
             p.status IS DISTINCT FROM d.status OR
             p.payment_status IS DISTINCT FROM d.payment_status
        THEN 'OUT_OF_SYNC'
        ELSE 'IN_SYNC'
    END AS sync_status
FROM optics_payment_records p
FULL OUTER JOIN optics_discounts d ON p.session_id = d.session_id

UNION ALL

SELECT 
    'Nursing' AS service_name,
    COALESCE(p.session_id, d.session_id) AS session_id,
    p.payment_intent_id AS payment_intent_id_payment,
    d.payment_intent_id AS payment_intent_id_discount,
    p.status AS status_payment,
    d.status AS status_discount,
    p.payment_status AS payment_status_payment,
    d.payment_status AS payment_status_discount,
    p.updated_at AS updated_at_payment,
    d.updated_at AS updated_at_discount,
    CASE 
        WHEN p.session_id IS NULL THEN 'DISCOUNT_ONLY'
        WHEN d.session_id IS NULL THEN 'PAYMENT_ONLY'
        WHEN p.payment_intent_id IS DISTINCT FROM d.payment_intent_id OR
             p.status IS DISTINCT FROM d.status OR
             p.payment_status IS DISTINCT FROM d.payment_status
        THEN 'OUT_OF_SYNC'
        ELSE 'IN_SYNC'
    END AS sync_status
FROM nursing_payment_records p
FULL OUTER JOIN nursing_discounts d ON p.session_id = d.session_id

UNION ALL

SELECT 
    'Renewable' AS service_name,
    COALESCE(p.session_id, d.session_id) AS session_id,
    p.payment_intent_id AS payment_intent_id_payment,
    d.payment_intent_id AS payment_intent_id_discount,
    p.status AS status_payment,
    d.status AS status_discount,
    p.payment_status AS payment_status_payment,
    d.payment_status AS payment_status_discount,
    p.updated_at AS updated_at_payment,
    d.updated_at AS updated_at_discount,
    CASE 
        WHEN p.session_id IS NULL THEN 'DISCOUNT_ONLY'
        WHEN d.session_id IS NULL THEN 'PAYMENT_ONLY'
        WHEN p.payment_intent_id IS DISTINCT FROM d.payment_intent_id OR
             p.status IS DISTINCT FROM d.status OR
             p.payment_status IS DISTINCT FROM d.payment_status
        THEN 'OUT_OF_SYNC'
        ELSE 'IN_SYNC'
    END AS sync_status
FROM renewable_payment_records p
FULL OUTER JOIN renewable_discounts d ON p.session_id = d.session_id;

-- ====================================
-- 7. INDEXES FOR PERFORMANCE
-- ====================================

-- Session ID indexes for all services
CREATE INDEX IF NOT EXISTS idx_optics_payment_records_session_id 
ON optics_payment_records(session_id);

CREATE INDEX IF NOT EXISTS idx_optics_discounts_session_id 
ON optics_discounts(session_id);

CREATE INDEX IF NOT EXISTS idx_nursing_payment_records_session_id 
ON nursing_payment_records(session_id);

CREATE INDEX IF NOT EXISTS idx_nursing_discounts_session_id 
ON nursing_discounts(session_id);

CREATE INDEX IF NOT EXISTS idx_renewable_payment_records_session_id 
ON renewable_payment_records(session_id);

CREATE INDEX IF NOT EXISTS idx_renewable_discounts_session_id 
ON renewable_discounts(session_id);

-- ====================================
-- 8. USAGE EXAMPLES
-- ====================================

/*
-- Example 1: Check sync status across all services
SELECT * FROM all_services_sync_check WHERE sync_status = 'OUT_OF_SYNC';

-- Example 2: Sync specific session across all services
SELECT * FROM sync_all_services_by_session_id('cs_test_123456');

-- Example 3: Sync specific service
SELECT sync_optics_by_session_id('cs_test_123456');
SELECT sync_nursing_by_session_id('cs_test_123456');
SELECT sync_renewable_by_session_id('cs_test_123456');

-- Example 4: Count records by sync status
SELECT service_name, sync_status, COUNT(*) 
FROM all_services_sync_check 
GROUP BY service_name, sync_status 
ORDER BY service_name, sync_status;
*/

-- ====================================
-- 9. JAVA INTEGRATION CODE
-- ====================================

/*
TO USE FROM JAVA APPLICATION:

1. Add to your repositories:

// OpticsPaymentRecordRepository.java
@Query(value = "SELECT sync_optics_by_session_id(:sessionId)", nativeQuery = true)
String syncOpticsBySessionId(@Param("sessionId") String sessionId);

// NursingPaymentRecordRepository.java
@Query(value = "SELECT sync_nursing_by_session_id(:sessionId)", nativeQuery = true)
String syncNursingBySessionId(@Param("sessionId") String sessionId);

// RenewablePaymentRecordRepository.java
@Query(value = "SELECT sync_renewable_by_session_id(:sessionId)", nativeQuery = true)
String syncRenewableBySessionId(@Param("sessionId") String sessionId);

2. Update your service methods:

// In OpticsStripeService.java
private void autoSyncDiscountOnPaymentUpdate(OpticsPaymentRecord paymentRecord) {
    if (paymentRecord == null || paymentRecord.getSessionId() == null) {
        log.warn("⚠️ Cannot auto-sync discount: payment record or session ID is null");
        return;
    }
    
    try {
        // Call database sync function
        String result = paymentRecordRepository.syncOpticsBySessionId(paymentRecord.getSessionId());
        log.info("✅ Database sync result: {}", result);
    } catch (Exception e) {
        log.error("❌ Database sync failed for session {}: {}", paymentRecord.getSessionId(), e.getMessage());
    }
}

3. Add to webhook processing:

// In DiscountsController.java - enhanced webhook handler
if (opticsDiscountsService.updatePaymentStatusBySessionId(sessionId, "COMPLETED")) {
    // Also sync via database function
    try {
        String result = paymentRecordRepository.syncOpticsBySessionId(sessionId);
        log.info("[DiscountsController] Database sync result: {}", result);
    } catch (Exception e) {
        log.error("[DiscountsController] Database sync failed: {}", e.getMessage());
    }
    updated = true;
}

4. Create a utility service:

@Service
public class SyncService {
    
    @Autowired
    private OpticsPaymentRecordRepository opticsPaymentRecordRepository;
    
    @Autowired
    private NursingPaymentRecordRepository nursingPaymentRecordRepository;
    
    @Autowired
    private RenewablePaymentRecordRepository renewablePaymentRecordRepository;
    
    public void syncAllServicesBySessionId(String sessionId) {
        try {
            String opticsResult = opticsPaymentRecordRepository.syncOpticsBySessionId(sessionId);
            String nursingResult = nursingPaymentRecordRepository.syncNursingBySessionId(sessionId);
            String renewableResult = renewablePaymentRecordRepository.syncRenewableBySessionId(sessionId);
            
            log.info("Sync results for session {}: Optics={}, Nursing={}, Renewable={}", 
                    sessionId, opticsResult, nursingResult, renewableResult);
        } catch (Exception e) {
            log.error("Failed to sync all services for session {}: {}", sessionId, e.getMessage());
        }
    }
}
*/
