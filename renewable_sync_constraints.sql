-- SQL Constraints and Triggers for Renewable Payment and Discount Synchronization
-- This ensures that when a session_id exists in both tables, they stay synchronized

-- ====================================
-- 1. FOREIGN KEY CONSTRAINTS
-- ====================================

-- Add foreign key constraint to ensure session_id consistency
-- (Optional: only if you want to enforce that discount records must have corresponding payment records)
-- ALTER TABLE renewable_discounts 
-- ADD CONSTRAINT fk_renewable_discounts_session 
-- FOREIGN KEY (session_id) REFERENCES renewable_payment_records(session_id) 
-- ON UPDATE CASCADE ON DELETE CASCADE;

-- ====================================
-- 2. TRIGGER FUNCTIONS FOR SYNCHRONIZATION
-- ====================================

-- Function to sync from renewable_payment_records to renewable_discounts
CREATE OR REPLACE FUNCTION sync_renewable_payment_to_discount()
RETURNS TRIGGER AS $$
BEGIN
    -- Update corresponding discount record if it exists
    UPDATE renewable_discounts 
    SET 
        payment_intent_id = NEW.payment_intent_id,
        status = NEW.status,
        payment_status = NEW.payment_status,
        updated_at = NEW.updated_at,
        amount_total = NEW.amount_total,
        currency = NEW.currency,
        customer_email = NEW.customer_email
    WHERE session_id = NEW.session_id;
    
    -- Log the sync operation
    IF FOUND THEN
        RAISE NOTICE 'Synced renewable payment record changes to discount table for session_id: %', NEW.session_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to sync from renewable_discounts to renewable_payment_records
CREATE OR REPLACE FUNCTION sync_renewable_discount_to_payment()
RETURNS TRIGGER AS $$
BEGIN
    -- Update corresponding payment record if it exists
    UPDATE renewable_payment_records 
    SET 
        payment_intent_id = NEW.payment_intent_id,
        status = NEW.status,
        payment_status = NEW.payment_status,
        updated_at = NEW.updated_at,
        amount_total = NEW.amount_total,
        currency = NEW.currency,
        customer_email = NEW.customer_email
    WHERE session_id = NEW.session_id;
    
    -- Log the sync operation
    IF FOUND THEN
        RAISE NOTICE 'Synced renewable discount record changes to payment table for session_id: %', NEW.session_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 3. CREATE TRIGGERS
-- ====================================

-- Trigger on renewable_payment_records to sync to renewable_discounts
DROP TRIGGER IF EXISTS trigger_sync_renewable_payment_to_discount ON renewable_payment_records;
CREATE TRIGGER trigger_sync_renewable_payment_to_discount
    AFTER UPDATE ON renewable_payment_records
    FOR EACH ROW
    WHEN (OLD.payment_intent_id IS DISTINCT FROM NEW.payment_intent_id OR
          OLD.status IS DISTINCT FROM NEW.status OR
          OLD.payment_status IS DISTINCT FROM NEW.payment_status OR
          OLD.updated_at IS DISTINCT FROM NEW.updated_at)
    EXECUTE FUNCTION sync_renewable_payment_to_discount();

-- Trigger on renewable_discounts to sync to renewable_payment_records
DROP TRIGGER IF EXISTS trigger_sync_renewable_discount_to_payment ON renewable_discounts;
CREATE TRIGGER trigger_sync_renewable_discount_to_payment
    AFTER UPDATE ON renewable_discounts
    FOR EACH ROW
    WHEN (OLD.payment_intent_id IS DISTINCT FROM NEW.payment_intent_id OR
          OLD.status IS DISTINCT FROM NEW.status OR
          OLD.payment_status IS DISTINCT FROM NEW.payment_status OR
          OLD.updated_at IS DISTINCT FROM NEW.updated_at)
    EXECUTE FUNCTION sync_renewable_discount_to_payment();

-- ====================================
-- 4. STORED PROCEDURE FOR MANUAL SYNC
-- ====================================

-- Stored procedure to manually sync records by session_id
CREATE OR REPLACE FUNCTION sync_renewable_records_by_session(p_session_id VARCHAR(500))
RETURNS TABLE(
    sync_status TEXT,
    payment_record_found BOOLEAN,
    discount_record_found BOOLEAN,
    records_synced BOOLEAN
) AS $$
DECLARE
    payment_rec RECORD;
    discount_rec RECORD;
    payment_exists BOOLEAN := FALSE;
    discount_exists BOOLEAN := FALSE;
    sync_performed BOOLEAN := FALSE;
BEGIN
    -- Check if payment record exists
    SELECT * INTO payment_rec 
    FROM renewable_payment_records 
    WHERE session_id = p_session_id;
    
    IF FOUND THEN
        payment_exists := TRUE;
    END IF;
    
    -- Check if discount record exists
    SELECT * INTO discount_rec 
    FROM renewable_discounts 
    WHERE session_id = p_session_id;
    
    IF FOUND THEN
        discount_exists := TRUE;
    END IF;
    
    -- Perform sync if both records exist
    IF payment_exists AND discount_exists THEN
        -- Use the most recently updated record as the source of truth
        IF payment_rec.updated_at >= discount_rec.updated_at THEN
            -- Sync from payment to discount
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
            
            sync_performed := TRUE;
            
            RETURN QUERY SELECT 
                'Synced from renewable payment record to discount record'::TEXT,
                payment_exists,
                discount_exists,
                sync_performed;
        ELSE
            -- Sync from discount to payment
            UPDATE renewable_payment_records 
            SET 
                payment_intent_id = discount_rec.payment_intent_id,
                status = discount_rec.status,
                payment_status = discount_rec.payment_status,
                updated_at = NOW(),
                amount_total = discount_rec.amount_total,
                currency = discount_rec.currency,
                customer_email = discount_rec.customer_email
            WHERE session_id = p_session_id;
            
            sync_performed := TRUE;
            
            RETURN QUERY SELECT 
                'Synced from renewable discount record to payment record'::TEXT,
                payment_exists,
                discount_exists,
                sync_performed;
        END IF;
    ELSE
        -- Return status of what was found
        RETURN QUERY SELECT 
            CASE 
                WHEN NOT payment_exists AND NOT discount_exists THEN 'No renewable records found for session_id'
                WHEN payment_exists AND NOT discount_exists THEN 'Only renewable payment record exists'
                WHEN NOT payment_exists AND discount_exists THEN 'Only renewable discount record exists'
                ELSE 'Unknown status'
            END::TEXT,
            payment_exists,
            discount_exists,
            sync_performed;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 5. BATCH SYNC PROCEDURE
-- ====================================

-- Procedure to sync all records that have matching session_ids
CREATE OR REPLACE FUNCTION batch_sync_renewable_records()
RETURNS TABLE(
    session_id VARCHAR(500),
    sync_result TEXT
) AS $$
DECLARE
    session_rec RECORD;
BEGIN
    -- Find all session_ids that exist in both tables
    FOR session_rec IN 
        SELECT DISTINCT p.session_id
        FROM renewable_payment_records p
        INNER JOIN renewable_discounts d ON p.session_id = d.session_id
    LOOP
        -- Sync each matching session
        PERFORM sync_renewable_records_by_session(session_rec.session_id);
        
        RETURN QUERY SELECT 
            session_rec.session_id,
            'Synchronized'::TEXT;
    END LOOP;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 6. VERIFICATION QUERIES
-- ====================================

-- Query to find records that are out of sync
CREATE OR REPLACE VIEW renewable_sync_status AS
SELECT 
    p.session_id,
    p.payment_intent_id AS payment_intent_id_payment,
    d.payment_intent_id AS payment_intent_id_discount,
    p.status AS status_payment,
    d.status AS status_discount,
    p.payment_status AS payment_status_payment,
    d.payment_status AS payment_status_discount,
    p.updated_at AS updated_at_payment,
    d.updated_at AS updated_at_discount,
    CASE 
        WHEN p.payment_intent_id IS DISTINCT FROM d.payment_intent_id OR
             p.status IS DISTINCT FROM d.status OR
             p.payment_status IS DISTINCT FROM d.payment_status
        THEN 'OUT_OF_SYNC'
        ELSE 'IN_SYNC'
    END AS sync_status
FROM renewable_payment_records p
INNER JOIN renewable_discounts d ON p.session_id = d.session_id;

-- ====================================
-- 7. MONITORING AND CLEANUP
-- ====================================

-- Function to disable triggers temporarily (for bulk operations)
CREATE OR REPLACE FUNCTION disable_renewable_sync_triggers()
RETURNS VOID AS $$
BEGIN
    ALTER TABLE renewable_payment_records DISABLE TRIGGER trigger_sync_renewable_payment_to_discount;
    ALTER TABLE renewable_discounts DISABLE TRIGGER trigger_sync_renewable_discount_to_payment;
    RAISE NOTICE 'Renewable sync triggers disabled';
END;
$$ LANGUAGE plpgsql;

-- Function to enable triggers
CREATE OR REPLACE FUNCTION enable_renewable_sync_triggers()
RETURNS VOID AS $$
BEGIN
    ALTER TABLE renewable_payment_records ENABLE TRIGGER trigger_sync_renewable_payment_to_discount;
    ALTER TABLE renewable_discounts ENABLE TRIGGER trigger_sync_renewable_discount_to_payment;
    RAISE NOTICE 'Renewable sync triggers enabled';
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- 8. INDEX RECOMMENDATIONS
-- ====================================

-- Ensure optimal performance for session_id lookups
CREATE INDEX IF NOT EXISTS idx_renewable_payment_records_session_id 
ON renewable_payment_records(session_id);

CREATE INDEX IF NOT EXISTS idx_renewable_discounts_session_id 
ON renewable_discounts(session_id);

-- Index for updated_at to optimize sync operations
CREATE INDEX IF NOT EXISTS idx_renewable_payment_records_updated_at 
ON renewable_payment_records(updated_at);

CREATE INDEX IF NOT EXISTS idx_renewable_discounts_updated_at 
ON renewable_discounts(updated_at);
