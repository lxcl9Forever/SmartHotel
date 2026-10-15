USE smart_hotel;

-- Older databases already contain refund_record, so CREATE TABLE IF NOT EXISTS
-- in 06_refund_payment.sql does not add its order lookup index.
SET @refund_order_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'refund_record'
      AND index_name = 'idx_refund_order_id'
);
SET @refund_order_index_sql := IF(
    @refund_order_index_exists = 0,
    'ALTER TABLE refund_record ADD INDEX idx_refund_order_id (order_id, refund_id)',
    'SELECT 1'
);
PREPARE refund_order_index_statement FROM @refund_order_index_sql;
EXECUTE refund_order_index_statement;
DEALLOCATE PREPARE refund_order_index_statement;
