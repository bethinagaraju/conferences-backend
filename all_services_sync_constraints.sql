-- MASTER SQL FILE FOR ALL 3 SERVICES SYNCHRONIZATION
-- This file contains constraints and triggers for Optics, Nursing, and Renewable services
-- Ensures that when a session_id exists in both payment and discount tables, they stay synchronized

-- ====================================
-- MASTER CONTROL FUNCTIONS
-- ====================================

-- Function to sync all services at once for a specific session
CREATE OR REPLACE FUNCTION sync_all_services_by_session(p_session_id VARCHAR(500))
RETURNS TABLE(
    service_name TEXT,
    sync_status TEXT,
    payment_record_found BOOLEAN,
    discount_record_found BOOLEAN,
    records_synced BOOLEAN
) AS $$
BEGIN
    -- Sync Optics service
    RETURN QUERY 
    SELECT 
        'Optics'::TEXT,
        result.sync_status,
        result.payment_record_found,
        result.discount_record_found,
        result.records_synced
    FROM sync_optics_records_by_session(p_session_id) AS result;
    
    -- Sync Nursing service
    RETURN QUERY 
    SELECT 
        'Nursing'::TEXT,
        result.sync_status,
        result.payment_record_found,
        result.discount_record_found,
        result.records_synced
    FROM sync_nursing_records_by_session(p_session_id) AS result;
    
    -- Sync Renewable service
    RETURN QUERY 
    SELECT 
        'Renewable'::TEXT,
        result.sync_status,
        result.payment_record_found,
        result.discount_record_found,
        result.records_synced
    FROM sync_renewable_records_by_session(p_session_id) AS result;
END;
$$ LANGUAGE plpgsql;

-- Function to batch sync all services
CREATE OR REPLACE FUNCTION batch_sync_all_services()
RETURNS TABLE(
    service_name TEXT,
    session_id VARCHAR(500),
    sync_result TEXT
) AS $$
BEGIN
    -- Batch sync Optics
    RETURN QUERY 
    SELECT 
        'Optics'::TEXT,
        result.session_id,
        result.sync_result
    FROM batch_sync_optics_records() AS result;
    
    -- Batch sync Nursing
    RETURN QUERY 
    SELECT 
        'Nursing'::TEXT,
        result.session_id,
        result.sync_result
    FROM batch_sync_nursing_records() AS result;
    
    -- Batch sync Renewable
    RETURN QUERY 
    SELECT 
        'Renewable'::TEXT,
        result.session_id,
        result.sync_result
    FROM batch_sync_renewable_records() AS result;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- MASTER MONITORING VIEW
-- ====================================

-- Combined view to monitor sync status across all services
CREATE OR REPLACE VIEW all_services_sync_status AS
SELECT 
    'Optics' AS service_name,
    session_id,
    payment_intent_id_payment,
    payment_intent_id_discount,
    status_payment,
    status_discount,
    payment_status_payment,
    payment_status_discount,
    updated_at_payment,
    updated_at_discount,
    sync_status
FROM optics_sync_status

UNION ALL

SELECT 
    'Nursing' AS service_name,
    session_id,
    payment_intent_id_payment,
    payment_intent_id_discount,
    status_payment,
    status_discount,
    payment_status_payment,
    payment_status_discount,
    updated_at_payment,
    updated_at_discount,
    sync_status
FROM nursing_sync_status

UNION ALL

SELECT 
    'Renewable' AS service_name,
    session_id,
    payment_intent_id_payment,
    payment_intent_id_discount,
    status_payment,
    status_discount,
    payment_status_payment,
    payment_status_discount,
    updated_at_payment,
    updated_at_discount,
    sync_status
FROM renewable_sync_status;

-- ====================================
-- MASTER TRIGGER CONTROL
-- ====================================

-- Function to disable all sync triggers across all services
CREATE OR REPLACE FUNCTION disable_all_sync_triggers()
RETURNS VOID AS $$
BEGIN
    -- Disable Optics triggers
    PERFORM disable_optics_sync_triggers();
    
    -- Disable Nursing triggers
    PERFORM disable_nursing_sync_triggers();
    
    -- Disable Renewable triggers
    PERFORM disable_renewable_sync_triggers();
    
    RAISE NOTICE 'All sync triggers disabled across all services';
END;
$$ LANGUAGE plpgsql;

-- Function to enable all sync triggers across all services
CREATE OR REPLACE FUNCTION enable_all_sync_triggers()
RETURNS VOID AS $$
BEGIN
    -- Enable Optics triggers
    PERFORM enable_optics_sync_triggers();
    
    -- Enable Nursing triggers
    PERFORM enable_nursing_sync_triggers();
    
    -- Enable Renewable triggers
    PERFORM enable_renewable_sync_triggers();
    
    RAISE NOTICE 'All sync triggers enabled across all services';
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- CROSS-SERVICE SYNC FUNCTIONS
-- ====================================

-- Function to find sessions that exist across multiple services
CREATE OR REPLACE FUNCTION find_cross_service_sessions()
RETURNS TABLE(
    session_id VARCHAR(500),
    optics_payment BOOLEAN,
    optics_discount BOOLEAN,
    nursing_payment BOOLEAN,
    nursing_discount BOOLEAN,
    renewable_payment BOOLEAN,
    renewable_discount BOOLEAN,
    total_services INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH session_summary AS (
        SELECT 
            COALESCE(op.session_id, od.session_id, np.session_id, nd.session_id, rp.session_id, rd.session_id) AS session_id,
            op.session_id IS NOT NULL AS optics_payment,
            od.session_id IS NOT NULL AS optics_discount,
            np.session_id IS NOT NULL AS nursing_payment,
            nd.session_id IS NOT NULL AS nursing_discount,
            rp.session_id IS NOT NULL AS renewable_payment,
            rd.session_id IS NOT NULL AS renewable_discount
        FROM optics_payment_records op
        FULL OUTER JOIN optics_discounts od ON op.session_id = od.session_id
        FULL OUTER JOIN nursing_payment_records np ON COALESCE(op.session_id, od.session_id) = np.session_id
        FULL OUTER JOIN nursing_discounts nd ON COALESCE(op.session_id, od.session_id, np.session_id) = nd.session_id
        FULL OUTER JOIN renewable_payment_records rp ON COALESCE(op.session_id, od.session_id, np.session_id, nd.session_id) = rp.session_id
        FULL OUTER JOIN renewable_discounts rd ON COALESCE(op.session_id, od.session_id, np.session_id, nd.session_id, rp.session_id) = rd.session_id
    )
    SELECT 
        ss.session_id,
        ss.optics_payment,
        ss.optics_discount,
        ss.nursing_payment,
        ss.nursing_discount,
        ss.renewable_payment,
        ss.renewable_discount,
        (CASE WHEN ss.optics_payment THEN 1 ELSE 0 END +
         CASE WHEN ss.optics_discount THEN 1 ELSE 0 END +
         CASE WHEN ss.nursing_payment THEN 1 ELSE 0 END +
         CASE WHEN ss.nursing_discount THEN 1 ELSE 0 END +
         CASE WHEN ss.renewable_payment THEN 1 ELSE 0 END +
         CASE WHEN ss.renewable_discount THEN 1 ELSE 0 END) AS total_services
    FROM session_summary ss
    WHERE ss.session_id IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- COMPREHENSIVE SYNC REPORT
-- ====================================

-- Function to generate a comprehensive sync report
CREATE OR REPLACE FUNCTION generate_sync_report()
RETURNS TABLE(
    service_name TEXT,
    total_sessions INTEGER,
    in_sync_count INTEGER,
    out_of_sync_count INTEGER,
    sync_percentage NUMERIC(5,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'Optics'::TEXT,
        COUNT(*)::INTEGER AS total_sessions,
        SUM(CASE WHEN sync_status = 'IN_SYNC' THEN 1 ELSE 0 END)::INTEGER AS in_sync_count,
        SUM(CASE WHEN sync_status = 'OUT_OF_SYNC' THEN 1 ELSE 0 END)::INTEGER AS out_of_sync_count,
        ROUND(
            (SUM(CASE WHEN sync_status = 'IN_SYNC' THEN 1 ELSE 0 END)::NUMERIC / COUNT(*)) * 100, 
            2
        ) AS sync_percentage
    FROM optics_sync_status
    
    UNION ALL
    
    SELECT 
        'Nursing'::TEXT,
        COUNT(*)::INTEGER AS total_sessions,
        SUM(CASE WHEN sync_status = 'IN_SYNC' THEN 1 ELSE 0 END)::INTEGER AS in_sync_count,
        SUM(CASE WHEN sync_status = 'OUT_OF_SYNC' THEN 1 ELSE 0 END)::INTEGER AS out_of_sync_count,
        ROUND(
            (SUM(CASE WHEN sync_status = 'IN_SYNC' THEN 1 ELSE 0 END)::NUMERIC / COUNT(*)) * 100, 
            2
        ) AS sync_percentage
    FROM nursing_sync_status
    
    UNION ALL
    
    SELECT 
        'Renewable'::TEXT,
        COUNT(*)::INTEGER AS total_sessions,
        SUM(CASE WHEN sync_status = 'IN_SYNC' THEN 1 ELSE 0 END)::INTEGER AS in_sync_count,
        SUM(CASE WHEN sync_status = 'OUT_OF_SYNC' THEN 1 ELSE 0 END)::INTEGER AS out_of_sync_count,
        ROUND(
            (SUM(CASE WHEN sync_status = 'IN_SYNC' THEN 1 ELSE 0 END)::NUMERIC / COUNT(*)) * 100, 
            2
        ) AS sync_percentage
    FROM renewable_sync_status;
END;
$$ LANGUAGE plpgsql;

-- ====================================
-- USAGE EXAMPLES FOR ALL SERVICES
-- ====================================

/*
-- Example 1: Check sync status across all services
SELECT * FROM all_services_sync_status WHERE sync_status = 'OUT_OF_SYNC';

-- Example 2: Sync a specific session across all services
SELECT * FROM sync_all_services_by_session('cs_test_123456');

-- Example 3: Batch sync all services
SELECT * FROM batch_sync_all_services();

-- Example 4: Generate sync report
SELECT * FROM generate_sync_report();

-- Example 5: Find sessions that exist across multiple services
SELECT * FROM find_cross_service_sessions() WHERE total_services > 2;

-- Example 6: Count out-of-sync records by service
SELECT service_name, sync_status, COUNT(*) 
FROM all_services_sync_status 
GROUP BY service_name, sync_status 
ORDER BY service_name, sync_status;

-- Example 7: Disable all triggers for bulk operations
SELECT disable_all_sync_triggers();

-- Example 8: Enable all triggers after bulk operations
SELECT enable_all_sync_triggers();

-- Example 9: Check specific service sync status
SELECT * FROM optics_sync_status WHERE sync_status = 'OUT_OF_SYNC';
SELECT * FROM nursing_sync_status WHERE sync_status = 'OUT_OF_SYNC';
SELECT * FROM renewable_sync_status WHERE sync_status = 'OUT_OF_SYNC';

-- Example 10: Manual sync for specific service
SELECT * FROM sync_optics_records_by_session('cs_test_123456');
SELECT * FROM sync_nursing_records_by_session('cs_test_123456');
SELECT * FROM sync_renewable_records_by_session('cs_test_123456');
*/

-- ====================================
-- INSTALLATION INSTRUCTIONS
-- ====================================

/*
TO INSTALL ALL SYNCHRONIZATION CONSTRAINTS:

1. Run the individual service files first:
   - optics_sync_constraints.sql
   - nursing_sync_constraints.sql  
   - renewable_sync_constraints.sql

2. Then run this master file: all_services_sync_constraints.sql

3. Verify installation:
   SELECT * FROM generate_sync_report();

4. Test with a known session ID:
   SELECT * FROM sync_all_services_by_session('your_session_id_here');

MAINTENANCE SCHEDULE:
- Daily: SELECT * FROM all_services_sync_status WHERE sync_status = 'OUT_OF_SYNC';
- Weekly: SELECT * FROM batch_sync_all_services();
- Monthly: SELECT * FROM generate_sync_report();
*/
