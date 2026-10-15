USE smart_hotel;

-- Speeds up the periodic fallback scan for expired unpaid orders.
-- Safe to rerun after an interrupted deployment.
SET @timeout_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'booking_order'
      AND index_name = 'idx_booking_timeout'
);
SET @timeout_index_ddl = IF(
    @timeout_index_exists = 0,
    'ALTER TABLE booking_order ADD INDEX idx_booking_timeout (order_status, payment_status, expire_time)',
    'SELECT 1'
);
PREPARE timeout_index_stmt FROM @timeout_index_ddl;
EXECUTE timeout_index_stmt;
DEALLOCATE PREPARE timeout_index_stmt;
