USE smart_hotel;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS promotion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL COMMENT 'DISCOUNT or FULL_REDUCTION',
    discount_rate DECIMAL(5,4) NULL,
    min_amount DECIMAL(12,2) NULL,
    reduce_amount DECIMAL(12,2) NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_promotion_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='booking_order' AND column_name='original_amount')=0,
    'ALTER TABLE booking_order ADD COLUMN original_amount DECIMAL(12,2) NULL AFTER total_amount', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='booking_order' AND column_name='discount_amount')=0,
    'ALTER TABLE booking_order ADD COLUMN discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER original_amount', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='booking_order' AND column_name='promotion_id')=0,
    'ALTER TABLE booking_order ADD COLUMN promotion_id BIGINT NULL AFTER discount_amount', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
