-- SQL script to fix foreign key constraint issues
-- Run this in your Supabase SQL editor or database management tool

-- 1. Remove orphaned registration form records that reference non-existent payment records
DELETE FROM optics_registration_form 
WHERE payment_record_id NOT IN (SELECT id FROM optics_payment_records);

DELETE FROM nursing_registration_form 
WHERE payment_record_id NOT IN (SELECT id FROM nursing_payment_records);

DELETE FROM renewable_registration_form 
WHERE payment_record_id NOT IN (SELECT id FROM renewable_payment_records);

-- 2. Verify the cleanup
SELECT 'Optics orphaned records' as table_name, COUNT(*) as orphaned_count
FROM optics_registration_form 
WHERE payment_record_id NOT IN (SELECT id FROM optics_payment_records)

UNION ALL

SELECT 'Nursing orphaned records' as table_name, COUNT(*) as orphaned_count
FROM nursing_registration_form 
WHERE payment_record_id NOT IN (SELECT id FROM nursing_payment_records)

UNION ALL

SELECT 'Renewable orphaned records' as table_name, COUNT(*) as orphaned_count
FROM renewable_registration_form 
WHERE payment_record_id NOT IN (SELECT id FROM renewable_payment_records);

-- 3. Check current table structures
SELECT 'optics_payment_records' as table_name, COUNT(*) as record_count FROM optics_payment_records
UNION ALL
SELECT 'nursing_payment_records' as table_name, COUNT(*) as record_count FROM nursing_payment_records  
UNION ALL
SELECT 'renewable_payment_records' as table_name, COUNT(*) as record_count FROM renewable_payment_records;
