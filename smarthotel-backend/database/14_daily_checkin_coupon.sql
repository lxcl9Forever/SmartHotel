USE smart_hotel;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS daily_checkin_reward (
    day_number TINYINT NOT NULL PRIMARY KEY,
    amount DECIMAL(12,2) NOT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO daily_checkin_reward (day_number, amount) VALUES
    (1, 50.00), (2, 50.00), (3, 100.00), (4, 50.00),
    (5, 50.00), (6, 50.00), (7, 200.00);

CREATE TABLE IF NOT EXISTS customer_checkin_state (
    customer_id BIGINT NOT NULL PRIMARY KEY,
    last_checkin_date DATE NOT NULL,
    streak_days TINYINT NOT NULL,
    CONSTRAINT fk_checkin_state_customer FOREIGN KEY (customer_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'CHECKIN' COMMENT 'CHECKIN 24h; future promotion coupons 7d',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    order_id BIGINT NULL,
    expire_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coupon_customer_status (customer_id, status, expire_time),
    KEY idx_coupon_order (order_id),
    CONSTRAINT fk_coupon_customer FOREIGN KEY (customer_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_checkin_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    day_number TINYINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    coupon_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_checkin_customer_date (customer_id, checkin_date),
    UNIQUE KEY uk_checkin_coupon (coupon_id),
    CONSTRAINT fk_checkin_record_customer FOREIGN KEY (customer_id)
        REFERENCES customer (customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_checkin_record_coupon FOREIGN KEY (coupon_id)
        REFERENCES customer_coupon (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='booking_order' AND column_name='coupon_id')=0,
    'ALTER TABLE booking_order ADD COLUMN coupon_id BIGINT NULL AFTER promotion_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='booking_order' AND column_name='coupon_discount_amount')=0,
    'ALTER TABLE booking_order ADD COLUMN coupon_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER coupon_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
