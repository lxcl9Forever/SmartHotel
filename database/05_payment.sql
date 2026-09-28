USE smart_hotel;

SET NAMES utf8mb4;


CREATE TABLE IF NOT EXISTS payment
(
    payment_id       BIGINT         NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',

    payment_no       VARCHAR(32)    NOT NULL COMMENT '平台支付流水号',

    order_id         BIGINT         NOT NULL COMMENT '订单ID',

    payment_method   VARCHAR(20)    NOT NULL COMMENT '支付方式：MOCK、ALIPAY',

    amount           DECIMAL(12,2)  NOT NULL COMMENT '支付金额',

    payment_status   VARCHAR(20)    NOT NULL COMMENT '支付记录状态：SUCCESS、FAILED',

    third_party_no   VARCHAR(64)    NULL COMMENT '第三方支付平台交易号',

    paid_time        DATETIME       NULL COMMENT '实际支付时间',

    create_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (payment_id),

    UNIQUE KEY uk_payment_no (payment_no),

    KEY idx_payment_order_id (order_id),

    CONSTRAINT fk_payment_order
    FOREIGN KEY (order_id)
    REFERENCES booking_order (order_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '支付记录表';