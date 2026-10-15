USE smart_hotel;

CREATE TABLE IF NOT EXISTS order_review (
    review_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_order_review_order (order_id),
    KEY idx_order_review_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
