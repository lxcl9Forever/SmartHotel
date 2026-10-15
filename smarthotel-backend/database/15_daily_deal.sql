USE smart_hotel;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS daily_deal (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_type_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    quota INT NOT NULL,
    discount_rate DECIMAL(5,4) NOT NULL COMMENT '应付比例，例如 0.8000 为八折',
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_daily_deal_public (status, end_time, check_in_date),
    CONSTRAINT fk_daily_deal_room_type FOREIGN KEY (room_type_id)
        REFERENCES room_type (room_type_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='booking_order' AND column_name='daily_deal_id')=0,
    'ALTER TABLE booking_order ADD COLUMN daily_deal_id BIGINT NULL AFTER promotion_id', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='booking_order' AND index_name='idx_booking_daily_deal')=0,
    'CREATE INDEX idx_booking_daily_deal ON booking_order (daily_deal_id, order_status)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
