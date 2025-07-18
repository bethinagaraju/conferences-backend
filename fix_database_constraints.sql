
-- FIXED SQL CONSTRAINTS FOR ALL 3 SERVICES - JAVA APPLICATION INTEGRATION
-- This version is designed to work with JPA/Hibernate entities in your Spring Boot application
-- Includes Optics, Nursing, and Renewable services

-- ====================================
-- 0. VERIFICATION OF EXISTING CONSTRAINTS
-- ====================================

-- Check existing constraints before adding new ones
SELECT 
    'EXISTING CONSTRAINTS' as check_type,
    conname as constraint_name,
    conrelid::regclass as table_name
FROM pg_constraint 
WHERE conname LIKE '%session_id%' 
ORDER BY conname;

-- Check existing indexes
SELECT 
    'EXISTING INDEXES' as check_type,
    indexname,
    tablename
FROM pg_indexes 
WHERE indexname LIKE '%session_id%'
ORDER BY indexname;

-- Check existing triggers
SELECT 
    'EXISTING TRIGGERS' as check_type,
    tgname as trigger_name,
    tgrelid::regclass as table_name,
    CASE 
        WHEN tgenabled = 'O' THEN 'ENABLED'
        WHEN tgenabled = 'D' THEN 'DISABLED'
        ELSE 'UNKNOWN'
    END as trigger_status
FROM pg_trigger 
WHERE tgname LIKE '%sync%discount%payment%'
   OR tgname LIKE 'trigger_sync_%'
   AND NOT tgisinternal
   AND tgname NOT LIKE 'RI_ConstraintTrigger%'
ORDER BY tgname;

-- ====================================
-- 1. DATABASE CONSTRAINTS (BASIC)
-- ====================================

-- Safely add session_id unique constraints for all services (only if they don't exist)
DO $$
BEGIN
    -- Optics constraints
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_optics_payment_session_id') THEN
        ALTER TABLE optics_payment_records ADD CONSTRAINT uk_optics_payment_session_id UNIQUE (session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_optics_discounts_session_id') THEN
        ALTER TABLE optics_discounts ADD CONSTRAINT uk_optics_discounts_session_id UNIQUE (session_id);
    END IF;
    
    -- Nursing constraints
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_nursing_payment_session_id') THEN
        ALTER TABLE nursing_payment_records ADD CONSTRAINT uk_nursing_payment_session_id UNIQUE (session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_nursing_discounts_session_id') THEN
        ALTER TABLE nursing_discounts ADD CONSTRAINT uk_nursing_discounts_session_id UNIQUE (session_id);
    END IF;
    
    -- Renewable constraints
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_renewable_payment_session_id') THEN
        ALTER TABLE renewable_payment_records ADD CONSTRAINT uk_renewable_payment_session_id UNIQUE (session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_renewable_discounts_session_id') THEN
        ALTER TABLE renewable_discounts ADD CONSTRAINT uk_renewable_discounts_session_id UNIQUE (session_id);
    END IF;
END $$;

-- ====================================
-- 2. OPTICS SERVICE FUNCTIONS
-- ====================================

-- Auto-sync function that updates discount table when payment record changes
CREATE OR REPLACE FUNCTION sync_optics_discount_on_payment_update()
RETURNS TRIGGER AS $$
DECLARE
    result_msg TEXT := 'No action taken';
BEGIN
    -- Only proceed if we have a session_id
    IF NEW.session_id IS NULL THEN
        RETURN NEW;
    END IF;
    
    -- Check if discount record exists for this session
    IF EXISTS (SELECT 1 FROM optics_discounts WHERE session_id = NEW.session_id) THEN
        -- Update the matching discount record with payment record data
        UPDATE optics_discounts 
        SET 
            payment_intent_id = NEW.payment_intent_id,
            status = NEW.status,
            payment_status = NEW.payment_status,
            updated_at = NOW(),
            amount_total = NEW.amount_total,
            currency = NEW.currency,
            customer_email = NEW.customer_email
        WHERE session_id = NEW.session_id;
        
        RAISE NOTICE 'Auto-synced optics discount for session: %', NEW.session_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger that fires AFTER INSERT/UPDATE on payment records
DROP TRIGGER IF EXISTS trigger_sync_optics_discount_on_payment_update ON optics_payment_records;
CREATE TRIGGER trigger_sync_optics_discount_on_payment_update
    AFTER INSERT OR UPDATE ON optics_payment_records
    FOR EACH ROW
    EXECUTE FUNCTION sync_optics_discount_on_payment_update();

-- Manual sync function for Java calls
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

-- Auto-sync function that updates discount table when payment record changes
CREATE OR REPLACE FUNCTION sync_nursing_discount_on_payment_update()
RETURNS TRIGGER AS $$
DECLARE
    result_msg TEXT := 'No action taken';
BEGIN
    -- Only proceed if we have a session_id
    IF NEW.session_id IS NULL THEN
        RETURN NEW;
    END IF;
    
    -- Check if discount record exists for this session
    IF EXISTS (SELECT 1 FROM nursing_discounts WHERE session_id = NEW.session_id) THEN
        -- Update the matching discount record with payment record data
        UPDATE nursing_discounts 
        SET 
            payment_intent_id = NEW.payment_intent_id,
            status = NEW.status,
            payment_status = NEW.payment_status,
            updated_at = NOW(),
            amount_total = NEW.amount_total,
            currency = NEW.currency,
            customer_email = NEW.customer_email
        WHERE session_id = NEW.session_id;
        
        RAISE NOTICE 'Auto-synced nursing discount for session: %', NEW.session_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger that fires AFTER INSERT/UPDATE on payment records
DROP TRIGGER IF EXISTS trigger_sync_nursing_discount_on_payment_update ON nursing_payment_records;
CREATE TRIGGER trigger_sync_nursing_discount_on_payment_update
    AFTER INSERT OR UPDATE ON nursing_payment_records
    FOR EACH ROW
    EXECUTE FUNCTION sync_nursing_discount_on_payment_update();

-- Manual sync function for Java calls
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

-- Auto-sync function that updates discount table when payment record changes
CREATE OR REPLACE FUNCTION sync_renewable_discount_on_payment_update()
RETURNS TRIGGER AS $$
DECLARE
    result_msg TEXT := 'No action taken';
BEGIN
    -- Only proceed if we have a session_id
    IF NEW.session_id IS NULL THEN
        RETURN NEW;
    END IF;
    
    -- Check if discount record exists for this session
    IF EXISTS (SELECT 1 FROM renewable_discounts WHERE session_id = NEW.session_id) THEN
        -- Update the matching discount record with payment record data
        UPDATE renewable_discounts 
        SET 
            payment_intent_id = NEW.payment_intent_id,
            status = NEW.status,
            payment_status = NEW.payment_status,
            updated_at = NOW(),
            amount_total = NEW.amount_total,
            currency = NEW.currency,
            customer_email = NEW.customer_email
        WHERE session_id = NEW.session_id;
        
        RAISE NOTICE 'Auto-synced renewable discount for session: %', NEW.session_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger that fires AFTER INSERT/UPDATE on payment records
DROP TRIGGER IF EXISTS trigger_sync_renewable_discount_on_payment_update ON renewable_payment_records;
CREATE TRIGGER trigger_sync_renewable_discount_on_payment_update
    AFTER INSERT OR UPDATE ON renewable_payment_records
    FOR EACH ROW
    EXECUTE FUNCTION sync_renewable_discount_on_payment_update();

-- Manual sync function for Java calls
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

-- Session ID indexes for all services (safe creation)
DO $$
BEGIN
    -- Create indexes only if they don't exist
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_optics_payment_records_session_id') THEN
        CREATE INDEX idx_optics_payment_records_session_id ON optics_payment_records(session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_optics_discounts_session_id') THEN
        CREATE INDEX idx_optics_discounts_session_id ON optics_discounts(session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_nursing_payment_records_session_id') THEN
        CREATE INDEX idx_nursing_payment_records_session_id ON nursing_payment_records(session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_nursing_discounts_session_id') THEN
        CREATE INDEX idx_nursing_discounts_session_id ON nursing_discounts(session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_renewable_payment_records_session_id') THEN
        CREATE INDEX idx_renewable_payment_records_session_id ON renewable_payment_records(session_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_renewable_discounts_session_id') THEN
        CREATE INDEX idx_renewable_discounts_session_id ON renewable_discounts(session_id);
    END IF;
END $$;

-- ====================================
-- 8. SPECIAL CONFIGURATION FOR JPA COMPATIBILITY
-- ====================================

-- Enable trigger compatibility with JPA/Hibernate
-- These settings ensure that triggers fire even when using JPA entity operations
-- NOTE: We only enable our custom sync triggers, not system constraint triggers

-- Make sure session_replication_role is set correctly
SET session_replication_role = 'origin';

-- Enable only our custom sync triggers (not system triggers)
-- System triggers like RI_ConstraintTrigger_* cannot be manually enabled/disabled
DO $$
BEGIN
    -- Enable specific sync triggers only
    IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_sync_optics_discount_on_payment_update') THEN
        ALTER TABLE optics_payment_records ENABLE TRIGGER trigger_sync_optics_discount_on_payment_update;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_sync_nursing_discount_on_payment_update') THEN
        ALTER TABLE nursing_payment_records ENABLE TRIGGER trigger_sync_nursing_discount_on_payment_update;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_sync_renewable_discount_on_payment_update') THEN
        ALTER TABLE renewable_payment_records ENABLE TRIGGER trigger_sync_renewable_discount_on_payment_update;
    END IF;
END $$;

-- Verify triggers are enabled
SELECT 
    'ACTIVE SYNC TRIGGERS' as check_type,
    n.nspname as schema_name,
    c.relname as table_name,
    t.tgname as trigger_name,
    CASE 
        WHEN t.tgenabled = 'O' THEN 'ENABLED'
        WHEN t.tgenabled = 'D' THEN 'DISABLED'
        ELSE 'UNKNOWN'
    END as trigger_status
FROM pg_trigger t
JOIN pg_class c ON t.tgrelid = c.oid
JOIN pg_namespace n ON c.relnamespace = n.oid
WHERE c.relname IN ('optics_payment_records', 'nursing_payment_records', 'renewable_payment_records')
AND NOT t.tgisinternal
AND t.tgname NOT LIKE 'RI_ConstraintTrigger%'
AND t.tgname LIKE 'trigger_sync_%'
ORDER BY c.relname, t.tgname;

-- ====================================
-- 9. TESTING TRIGGERS WITH JPA
-- ====================================

-- Function to test if triggers work with JPA operations
CREATE OR REPLACE FUNCTION test_trigger_with_jpa(p_session_id VARCHAR(500))
RETURNS TEXT AS $$
DECLARE
    discount_before RECORD;
    payment_after RECORD;
    discount_after RECORD;
    result_msg TEXT;
BEGIN
    -- Get discount record before update
    SELECT payment_intent_id, status, payment_status, amount_total, currency, customer_email
    INTO discount_before
    FROM optics_discounts 
    WHERE session_id = p_session_id;
    
    -- Simulate what JPA does - direct SQL UPDATE
    UPDATE optics_payment_records 
    SET 
        payment_intent_id = 'pi_jpa_test_' || extract(epoch from now()),
        status = 'COMPLETED',
        payment_status = 'SUCCEEDED',
        amount_total = 99900,
        currency = 'EUR',
        customer_email = 'jpa@test.com',
        updated_at = NOW()
    WHERE session_id = p_session_id;
    
    -- Get records after update
    SELECT payment_intent_id, status, payment_status, amount_total, currency, customer_email
    INTO payment_after
    FROM optics_payment_records 
    WHERE session_id = p_session_id;
    
    SELECT payment_intent_id, status, payment_status, amount_total, currency, customer_email
    INTO discount_after
    FROM optics_discounts 
    WHERE session_id = p_session_id;
    
    -- Check if trigger worked
    IF discount_after.payment_intent_id = payment_after.payment_intent_id AND
       discount_after.status = payment_after.status AND
       discount_after.payment_status = payment_after.payment_status THEN
        result_msg := 'SUCCESS: Trigger automatically synced discount record. Payment Intent: ' || payment_after.payment_intent_id;
    ELSE
        result_msg := 'FAILED: Trigger did not sync. Payment Intent: ' || payment_after.payment_intent_id || ', Discount Intent: ' || discount_after.payment_intent_id;
    END IF;
    
    RETURN result_msg;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 13. FINAL SUCCESS MESSAGE
-- ====================================

-- Display success message
SELECT 
    '🎉 SUCCESS: Database constraints setup completed!' as status,
    'Automatic sync triggers are now active' as message,
    'Payment record updates will automatically sync to discount records' as functionality;

-- Show what was created
SELECT 
    'CONSTRAINT SUMMARY' as summary_type,
    COUNT(CASE WHEN conname LIKE '%session_id%' THEN 1 END) as session_id_constraints,
    COUNT(CASE WHEN tgname LIKE 'trigger_sync_%' THEN 1 END) as sync_triggers
FROM pg_constraint 
CROSS JOIN pg_trigger 
WHERE NOT tgisinternal;

RAISE NOTICE 'Database constraints setup completed successfully!';
RAISE NOTICE 'Triggers will automatically sync discount records when payment records are updated.';
RAISE NOTICE 'Test the functionality using the validate_constraints.sql script.';
