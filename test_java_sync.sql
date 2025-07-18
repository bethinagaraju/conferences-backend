-- TEST SCRIPT FOR JAVA SYNC FUNCTIONALITY
-- Run this script to test the Java sync functions

-- Step 1: Create test data
-- Insert test payment record
INSERT INTO optics_payment_records (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_test_java_sync_123', 'pi_test_java_sync_123', 'COMPLETED', 'SUCCEEDED', 10000, 'EUR', 'test@example.com', NOW(), NOW())
ON CONFLICT (session_id) DO UPDATE SET
    payment_intent_id = EXCLUDED.payment_intent_id,
    status = EXCLUDED.status,
    payment_status = EXCLUDED.payment_status,
    updated_at = NOW();

-- Insert test discount record (with different data to test sync)
INSERT INTO optics_discounts (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_test_java_sync_123', 'pi_old_intent_123', 'PENDING', 'INCOMPLETE', 5000, 'USD', 'old@example.com', NOW(), NOW())
ON CONFLICT (session_id) DO UPDATE SET
    payment_intent_id = EXCLUDED.payment_intent_id,
    status = EXCLUDED.status,
    payment_status = EXCLUDED.payment_status,
    updated_at = NOW();

-- Step 2: Check data before sync
SELECT 'BEFORE SYNC - Payment Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records 
WHERE session_id = 'cs_test_java_sync_123'

UNION ALL

SELECT 'BEFORE SYNC - Discount Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts 
WHERE session_id = 'cs_test_java_sync_123';

-- Step 3: Test the sync function
SELECT sync_optics_by_session_id('cs_test_java_sync_123') as sync_result;

-- Step 4: Check data after sync
SELECT 'AFTER SYNC - Payment Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records 
WHERE session_id = 'cs_test_java_sync_123'

UNION ALL

SELECT 'AFTER SYNC - Discount Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts 
WHERE session_id = 'cs_test_java_sync_123';

-- Step 5: Test the master sync function
SELECT * FROM sync_all_services_by_session_id('cs_test_java_sync_123');

-- Step 6: Check sync status view
SELECT * FROM all_services_sync_check 
WHERE session_id = 'cs_test_java_sync_123';

-- Step 7: Clean up test data
DELETE FROM optics_payment_records WHERE session_id = 'cs_test_java_sync_123';
DELETE FROM optics_discounts WHERE session_id = 'cs_test_java_sync_123';

-- Expected Results:
-- 1. Before sync: Payment and discount records should have different data
-- 2. After sync: Discount record should be updated to match payment record
-- 3. Sync function should return success message
-- 4. Sync status view should show 'IN_SYNC'
