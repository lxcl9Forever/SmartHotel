USE smart_hotel;

SET NAMES utf8mb4;


-- ============================================================
-- 1. 预订订单表
--
-- 当前设计：
-- 一个订单只预订一种房型
-- 但可以一次预订多间
--
-- 例如：
-- 豪华大床房 × 2间
-- 2026-10-01入住
-- 2026-10-04退房
-- ============================================================

CREATE TABLE IF NOT EXISTS booking_order
(
    order_id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',

    order_no          VARCHAR(32)   NOT NULL COMMENT '业务订单号',

    customer_id       BIGINT        NOT NULL COMMENT '下单客户ID',

    room_type_id      BIGINT        NOT NULL COMMENT '预订房型ID',

    room_count        INT           NOT NULL COMMENT '预订房间数量',

    guest_count       INT           NOT NULL COMMENT '入住人数',

    check_in_date     DATE          NOT NULL COMMENT '入住日期',

    check_out_date    DATE          NOT NULL COMMENT '退房日期',

    nights            INT           NOT NULL COMMENT '入住晚数',

    total_amount      DECIMAL(12,2) NOT NULL COMMENT '订单总金额',

    contact_name      VARCHAR(50)   NOT NULL COMMENT '联系人姓名',

    contact_phone     VARCHAR(20)   NOT NULL COMMENT '联系人手机号',

    order_status      VARCHAR(30)   NOT NULL DEFAULT 'WAIT_PAY'
    COMMENT '订单状态：WAIT_PAY待支付，CONFIRMED已确认，NO_SHOW未入住，CHECKED_IN已入住，CHECKED_OUT已退房，COMPLETED已完成，CANCEL_REQUESTED申请取消，CANCELLED已取消，CLOSED超时关闭，REFUNDED已退款',

    payment_status    VARCHAR(30)   NOT NULL DEFAULT 'UNPAID'
    COMMENT '支付状态：UNPAID未支付，PAID已支付，REFUNDING退款中，REFUNDED已退款，PARTIAL_REFUND部分退款',

    expire_time       DATETIME      NOT NULL COMMENT '待支付订单过期时间',

    remark            VARCHAR(255)  NULL COMMENT '订单备注',

    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (order_id),

    UNIQUE KEY uk_booking_order_no (order_no),

    KEY idx_booking_customer_id (customer_id),

    KEY idx_booking_room_type_id (room_type_id),

    KEY idx_booking_order_status (order_status),

    KEY idx_booking_check_in_date (check_in_date),

    CONSTRAINT fk_booking_customer
    FOREIGN KEY (customer_id)
    REFERENCES customer (customer_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

    CONSTRAINT fk_booking_room_type
    FOREIGN KEY (room_type_id)
    REFERENCES room_type (room_type_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '酒店预订订单表';


-- ============================================================
-- 2. 入住客人表
--
-- customer = 下单的人
-- booking_guest = 实际入住的人
--
-- 两者可能不是同一个人
-- ============================================================

CREATE TABLE IF NOT EXISTS booking_guest
(
    guest_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '入住客人ID',

    order_id        BIGINT       NOT NULL COMMENT '订单ID',

    guest_name      VARCHAR(50)  NOT NULL COMMENT '入住人姓名',

    phone           VARCHAR(20)  NULL COMMENT '联系电话',

    id_type         VARCHAR(20)  NULL DEFAULT 'ID_CARD' COMMENT '证件类型',

    id_number       VARCHAR(50)  NULL COMMENT '证件号码',

    is_primary      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主要入住人：1是，0否',

    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (guest_id),

    KEY idx_booking_guest_order_id (order_id),

    CONSTRAINT fk_booking_guest_order
    FOREIGN KEY (order_id)
    REFERENCES booking_order (order_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '订单入住客人表';


-- ============================================================
-- 3. 验证
-- ============================================================

SHOW TABLES;

DESC booking_order;

DESC booking_guest;
