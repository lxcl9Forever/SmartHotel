USE smart_hotel;

SET NAMES utf8mb4;

-- Existing databases already have refund_record. Keep this migration usable
-- when a new database was created from the repository's 01-05 scripts.
CREATE TABLE IF NOT EXISTS refund_record (
    refund_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    refund_amount DECIMAL(12, 2) NOT NULL,
    refund_reason VARCHAR(255) NULL,
    refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (refund_id),
    KEY idx_refund_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One money movement per approved refund (MOCK or ALIPAY). The original payment
-- record remains unchanged so the payment and refund are independently auditable.
CREATE TABLE IF NOT EXISTS refund_payment_record (
    refund_payment_id BIGINT NOT NULL AUTO_INCREMENT,
    refund_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    refund_no VARCHAR(32) NOT NULL,
    refund_amount DECIMAL(12, 2) NOT NULL,
    refund_method VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    refund_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    inventory_sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    refunded_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (refund_payment_id),
    UNIQUE KEY uk_refund_payment_refund (refund_id),
    UNIQUE KEY uk_refund_payment_no (refund_no),
    KEY idx_refund_payment_order (order_id),
    KEY idx_refund_payment_sync (inventory_sync_status),
    CONSTRAINT fk_refund_payment_refund FOREIGN KEY (refund_id)
        REFERENCES refund_record (refund_id),
    CONSTRAINT fk_refund_payment_order FOREIGN KEY (order_id)
        REFERENCES booking_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
