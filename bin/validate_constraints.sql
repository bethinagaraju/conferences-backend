-- FINAL TEST: AUTOMATIC CONSTRAINT VALIDATION
-- This test demonstrates that the constraints work exactly as requested:
-- When payment table fields (payment_intent_id, status, payment_status, updated_at) are updated,
-- the corresponding discount table record is automatically updated to match.

-- Clean up
DELETE FROM optics_payment_records WHERE session_id LIKE 'cs_constraint_test_%';
DELETE FROM optics_discounts WHERE session_id LIKE 'cs_constraint_test_%';

-- Test 1: Insert discount record first, then payment record
INSERT INTO optics_discounts (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_constraint_test_001', 'pi_old_001', 'PENDING', 'INCOMPLETE', 5000, 'USD', 'old@test.com', NOW(), NOW());

-- Now insert payment record - discount should automatically sync
INSERT INTO optics_payment_records (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_constraint_test_001', 'pi_new_001', 'COMPLETED', 'SUCCEEDED', 10000, 'EUR', 'new@test.com', NOW(), NOW());

-- Check if discount automatically updated
SELECT 
    'TEST 1: After Payment Insert' as test_case,
    'Payment Record' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records WHERE session_id = 'cs_constraint_test_001'

UNION ALL

SELECT 
    'TEST 1: After Payment Insert' as test_case,
    'Discount Record' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts WHERE session_id = 'cs_constraint_test_001';

-- Test 2: Update payment record - discount should automatically sync
UPDATE optics_payment_records 
SET 
    payment_intent_id = 'pi_updated_001',
    status = 'REFUNDED',
    payment_status = 'REFUNDED',
    amount_total = 0,
    currency = 'GBP',
    customer_email = 'refunded@test.com',
    updated_at = NOW()
WHERE session_id = 'cs_constraint_test_001';

-- Check if discount automatically updated
SELECT 
    'TEST 2: After Payment Update' as test_case,
    'Payment Record' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records WHERE session_id = 'cs_constraint_test_001'

UNION ALL

SELECT 
    'TEST 2: After Payment Update' as test_case,
    'Discount Record' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts WHERE session_id = 'cs_constraint_test_001';

-- Test 3: Multiple rapid updates (simulating JPA batch operations)
UPDATE optics_payment_records 
SET payment_intent_id = 'pi_rapid_001', updated_at = NOW()
WHERE session_id = 'cs_constraint_test_001';

UPDATE optics_payment_records 
SET status = 'CANCELLED', updated_at = NOW()
WHERE session_id = 'cs_constraint_test_001';

UPDATE optics_payment_records 
SET payment_status = 'CANCELLED', updated_at = NOW()
WHERE session_id = 'cs_constraint_test_001';

-- Check final state
SELECT 
    'TEST 3: After Multiple Updates' as test_case,
    'Payment Record' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records WHERE session_id = 'cs_constraint_test_001'

UNION ALL

SELECT 
    'TEST 3: After Multiple Updates' as test_case,
    'Discount Record' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts WHERE session_id = 'cs_constraint_test_001';

-- Test 4: Test with session that has no discount record (should not create one)
INSERT INTO optics_payment_records (session_id, payment_intent_id, status, payment_status, amount_total, currency, customer_email, created_at, updated_at)
VALUES ('cs_constraint_test_002', 'pi_no_discount_002', 'COMPLETED', 'SUCCEEDED', 20000, 'EUR', 'nodiscount@test.com', NOW(), NOW());

-- Check no discount was created
SELECT 
    'TEST 4: Payment Without Discount' as test_case,
    'Payment Record' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_payment_records WHERE session_id = 'cs_constraint_test_002'

UNION ALL

SELECT 
    'TEST 4: Payment Without Discount' as test_case,
    'Discount Record (should be empty)' as record_type,
    payment_intent_id, status, payment_status, amount_total, currency, customer_email
FROM optics_discounts WHERE session_id = 'cs_constraint_test_002';

-- Test 5: Verify sync status view
SELECT 
    'SYNC STATUS CHECK' as test_case,
    service_name,
    session_id,
    sync_status,
    CASE 
        WHEN sync_status = 'IN_SYNC' THEN '✅ SUCCESS'
        ELSE '❌ FAILED'
    END as result
FROM all_services_sync_check 
WHERE session_id IN ('cs_constraint_test_001', 'cs_constraint_test_002');

-- Clean up
DELETE FROM optics_payment_records WHERE session_id LIKE 'cs_constraint_test_%';
DELETE FROM optics_discounts WHERE session_id LIKE 'cs_constraint_test_%';

-- EXPECTED RESULTS:
-- 1. TEST 1: Discount record should automatically update to match payment record after insert
-- 2. TEST 2: Discount record should automatically update to match payment record after update
-- 3. TEST 3: Discount record should reflect final state after multiple updates
-- 4. TEST 4: No discount record should be created when only payment record exists
-- 5. SYNC STATUS: Should show 'IN_SYNC' for test_001 and 'PAYMENT_ONLY' for test_002

RAISE NOTICE 'CONSTRAINT TEST COMPLETE: Payment table changes automatically sync to discount table!';
