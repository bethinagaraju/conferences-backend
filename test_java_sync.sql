-- TEST SCRIPT FOR AUTOMATIC TRIGGER-BASED SYNC FUNCTIONALITY
-- This tests the database triggers that automatically sync discount records when payment records are updated

-- Step 1: Clean up any existing test data
DELETE FROM optics_payment_records WHERE session_id = 'cs_test_java_sync_123';
DELETE FROM optics_discounts WHERE session_id = 'cs_test_java_sync_123';

-- Step 2: Insert test discount record first (with initial data)
INSERT INTO optics_discounts (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_test_java_sync_123', 'pi_old_intent_123', 'PENDING', 'INCOMPLETE', 5000, 'USD', 'old@example.com', NOW(), NOW());

-- Step 3: Check initial discount record
SELECT 'INITIAL DISCOUNT RECORD' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts 
WHERE session_id = 'cs_test_java_sync_123';

-- Step 4: Insert payment record - THIS SHOULD TRIGGER THE AUTOMATIC SYNC
INSERT INTO optics_payment_records (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_test_java_sync_123', 'pi_test_java_sync_123', 'COMPLETED', 'SUCCEEDED', 10000, 'EUR', 'test@example.com', NOW(), NOW());

-- Step 5: Check if discount record was automatically updated by the trigger
SELECT 'AFTER PAYMENT INSERT - Payment Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records 
WHERE session_id = 'cs_test_java_sync_123'

UNION ALL

SELECT 'AFTER PAYMENT INSERT - Discount Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts 
WHERE session_id = 'cs_test_java_sync_123';

-- Step 6: Now test UPDATE trigger - update payment record with new data
UPDATE optics_payment_records 
SET 
    payment_intent_id = 'pi_updated_intent_456',
    status = 'REFUNDED',
    payment_status = 'REFUNDED',
    amount_total = 15000,
    currency = 'GBP',
    customer_email = 'updated@example.com',
    updated_at = NOW()
WHERE session_id = 'cs_test_java_sync_123';

-- Step 7: Check if discount record was automatically updated by the UPDATE trigger
SELECT 'AFTER PAYMENT UPDATE - Payment Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records 
WHERE session_id = 'cs_test_java_sync_123'

UNION ALL

SELECT 'AFTER PAYMENT UPDATE - Discount Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts 
WHERE session_id = 'cs_test_java_sync_123';

-- Step 8: Test the manual sync function as well
SELECT sync_optics_by_session_id('cs_test_java_sync_123') as manual_sync_result;

-- Step 9: Check sync status view
SELECT * FROM all_services_sync_check 
WHERE session_id = 'cs_test_java_sync_123';

-- Step 10: Test with a session that has no discount record
INSERT INTO optics_payment_records (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_test_no_discount_456', 'pi_no_discount_456', 'COMPLETED', 'SUCCEEDED', 20000, 'EUR', 'nodiscount@example.com', NOW(), NOW());

-- Check that no error occurs and no discount is created automatically (trigger only updates existing records)
SELECT 'NO DISCOUNT CASE - Payment Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records 
WHERE session_id = 'cs_test_no_discount_456'

UNION ALL

SELECT 'NO DISCOUNT CASE - Discount Record' as record_type, session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts 
WHERE session_id = 'cs_test_no_discount_456';

-- Step 11: Clean up test data
DELETE FROM optics_payment_records WHERE session_id IN ('cs_test_java_sync_123', 'cs_test_no_discount_456');
DELETE FROM optics_discounts WHERE session_id IN ('cs_test_java_sync_123', 'cs_test_no_discount_456');

-- Expected Results:
-- 1. Initial discount record should have old values (pi_old_intent_123, PENDING, INCOMPLETE, 5000, USD, old@example.com)
-- 2. After payment insert: Discount record should automatically update to match payment record (pi_test_java_sync_123, COMPLETED, SUCCEEDED, 10000, EUR, test@example.com)
-- 3. After payment update: Discount record should automatically update again (pi_updated_intent_456, REFUNDED, REFUNDED, 15000, GBP, updated@example.com)
-- 4. Manual sync should confirm records are in sync
-- 5. No discount case should show only payment record, no discount record created
-- 6. This proves the triggers are working automatically whenever payment records change!
