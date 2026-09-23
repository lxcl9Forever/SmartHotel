USE smart_hotel;

SET NAMES utf8mb4;


-- ============================================================
-- 1. 每日房量表
-- 一个房型每天一条库存记录
-- ============================================================

CREATE TABLE IF NOT EXISTS room_inventory
(
    inventory_id    BIGINT NOT NULL AUTO_INCREMENT COMMENT '房量记录ID',

    room_type_id    BIGINT NOT NULL COMMENT '房型ID',

    inventory_date  DATE NOT NULL COMMENT '房量日期',

    total_qty       INT NOT NULL COMMENT '当天总房量',

    locked_qty      INT NOT NULL DEFAULT 0 COMMENT '待支付订单锁定数量',

    sold_qty        INT NOT NULL DEFAULT 0 COMMENT '已确认售出数量',

    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',

    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (inventory_id),

    UNIQUE KEY uk_inventory_type_date
(room_type_id, inventory_date),

    KEY idx_inventory_date (inventory_date),

    CONSTRAINT fk_inventory_room_type
    FOREIGN KEY (room_type_id)
    REFERENCES room_type (room_type_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '房型每日房量表';


-- ============================================================
-- 2. 每日房价表
-- 一个房型每天一个实际销售价格
-- ============================================================

CREATE TABLE IF NOT EXISTS room_price
(
    price_id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '房价记录ID',

    room_type_id    BIGINT NOT NULL COMMENT '房型ID',

    price_date      DATE NOT NULL COMMENT '价格日期',

    sale_price      DECIMAL(10,2) NOT NULL COMMENT '当天实际销售价格',

    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (price_id),

    UNIQUE KEY uk_price_type_date
(room_type_id, price_date),

    KEY idx_price_date (price_date),

    CONSTRAINT fk_price_room_type
    FOREIGN KEY (room_type_id)
    REFERENCES room_type (room_type_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '房型每日房价表';


-- ============================================================
-- 3. 给15个房型配置开发阶段基础价格
-- 后续管理员后台可以修改
-- ============================================================

UPDATE room_type
SET base_price =
        CASE type_code
            WHEN 'RT01' THEN 688.00
            WHEN 'RT02' THEN 688.00
            WHEN 'RT03' THEN 888.00
            WHEN 'RT04' THEN 888.00
            WHEN 'RT05' THEN 988.00
            WHEN 'RT06' THEN 1188.00
            WHEN 'RT07' THEN 1088.00
            WHEN 'RT08' THEN 1088.00
            WHEN 'RT09' THEN 1288.00
            WHEN 'RT10' THEN 1288.00
            WHEN 'RT11' THEN 1188.00
            WHEN 'RT12' THEN 1288.00
            WHEN 'RT13' THEN 1188.00
            WHEN 'RT14' THEN 1588.00
            WHEN 'RT15' THEN 2188.00
            END;


-- ============================================================
-- 4. 初始化未来90天每日房量
-- 每个房型每天一条
-- 15 × 90 = 1350条
-- ============================================================

INSERT INTO room_inventory
(
    room_type_id,
    inventory_date,
    total_qty,
    locked_qty,
    sold_qty,
    version
)

WITH RECURSIVE date_list AS
                   (
                       SELECT CURDATE() AS inventory_date

                       UNION ALL

                       SELECT DATE_ADD(inventory_date, INTERVAL 1 DAY)
                       FROM date_list
                       WHERE inventory_date <
                             DATE_ADD(CURDATE(), INTERVAL 89 DAY)
                   )

SELECT
    rt.room_type_id,
    dl.inventory_date,
    rt.total_rooms,
    0,
    0,
    0
FROM room_type rt
         CROSS JOIN date_list dl

    ON DUPLICATE KEY UPDATE
    total_qty = VALUES(total_qty);


-- ============================================================
-- 5. 初始化未来90天每日房价
-- 目前全部使用房型基础价
-- 后续支持不同日期不同价格
-- ============================================================

INSERT INTO room_price
(
    room_type_id,
    price_date,
    sale_price
)

WITH RECURSIVE date_list AS
                   (
                       SELECT CURDATE() AS price_date

                       UNION ALL

                       SELECT DATE_ADD(price_date, INTERVAL 1 DAY)
                       FROM date_list
                       WHERE price_date <
                             DATE_ADD(CURDATE(), INTERVAL 89 DAY)
                   )

SELECT
    rt.room_type_id,
    dl.price_date,
    rt.base_price
FROM room_type rt
         CROSS JOIN date_list dl

    ON DUPLICATE KEY UPDATE
    sale_price = VALUES(sale_price);


-- ============================================================
-- 6. 验证
-- ============================================================

SELECT COUNT(*) AS inventory_count
FROM room_inventory;

SELECT COUNT(*) AS price_count
FROM room_price;


SELECT
    rt.type_code,
    rt.type_name,
    ri.inventory_date,
    ri.total_qty,
    ri.locked_qty,
    ri.sold_qty,
    (
        ri.total_qty
            - ri.locked_qty
            - ri.sold_qty
        ) AS available_qty
FROM room_inventory ri
         JOIN room_type rt
              ON ri.room_type_id = rt.room_type_id
ORDER BY
    ri.inventory_date,
    rt.sort_no
    LIMIT 30;


SELECT
    rt.type_code,
    rt.type_name,
    rp.price_date,
    rp.sale_price
FROM room_price rp
         JOIN room_type rt
              ON rp.room_type_id = rt.room_type_id
ORDER BY
    rp.price_date,
    rt.sort_no
    LIMIT 30;