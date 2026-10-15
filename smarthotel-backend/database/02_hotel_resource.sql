USE smart_hotel;

SET NAMES utf8mb4;


-- ============================================================
-- 1. 酒店基本信息表
-- 单体酒店系统，目前只维护一条酒店信息
-- ============================================================

CREATE TABLE IF NOT EXISTS hotel_info
(
    hotel_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '酒店ID',
    hotel_name       VARCHAR(100) NOT NULL COMMENT '酒店名称',
    address          VARCHAR(255) NOT NULL COMMENT '酒店地址',
    phone            VARCHAR(30)  NULL COMMENT '联系电话',
    description      TEXT         NULL COMMENT '酒店介绍',

    check_in_time    TIME         NOT NULL DEFAULT '14:00:00' COMMENT '标准入住时间',
    check_out_time   TIME         NOT NULL DEFAULT '12:00:00' COMMENT '标准退房时间',

    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1营业，0停业',

    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (hotel_id)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '酒店基本信息表';


-- ============================================================
-- 2. 房型表
-- 顾客在线预订的是房型，而不是具体房号
-- ============================================================

CREATE TABLE IF NOT EXISTS room_type
(
    room_type_id     BIGINT         NOT NULL AUTO_INCREMENT COMMENT '房型ID',

    type_code        VARCHAR(20)    NOT NULL COMMENT '房型编码',

    type_name        VARCHAR(100)   NOT NULL COMMENT '房型名称',

    area             DECIMAL(8,2)   NULL COMMENT '房间面积，平方米',

    bed_type         VARCHAR(50)    NULL COMMENT '床型',

    bed_count        INT            NOT NULL DEFAULT 1 COMMENT '床数量',

    max_guests       INT            NOT NULL DEFAULT 2 COMMENT '最大入住人数',

    total_rooms      INT            NOT NULL COMMENT '该房型实体房总数',

    base_price       DECIMAL(10,2)  NULL COMMENT '基础参考价格，实际售价以后由每日房价表决定',

    description      VARCHAR(500)   NULL COMMENT '房型描述',

    status           TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1可售，0停售',

    sort_no          INT            NOT NULL DEFAULT 0 COMMENT '排序号',

    create_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (room_type_id),

    UNIQUE KEY uk_room_type_code (type_code),

    UNIQUE KEY uk_room_type_name (type_name)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '酒店房型表';


-- ============================================================
-- 3. 房型图片表
-- 一个房型可以拥有多张展示图片
-- ============================================================

CREATE TABLE IF NOT EXISTS room_type_image
(
    image_id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '图片ID',

    room_type_id     BIGINT        NOT NULL COMMENT '房型ID',

    image_url        VARCHAR(500)  NOT NULL COMMENT '图片地址',

    is_cover         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否封面：1是，0否',

    sort_no          INT           NOT NULL DEFAULT 0 COMMENT '图片排序号',

    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (image_id),

    KEY idx_room_type_image_type_id (room_type_id),

    CONSTRAINT fk_room_type_image_type
    FOREIGN KEY (room_type_id)
    REFERENCES room_type (room_type_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '房型图片表';


-- ============================================================
-- 4. 实体房间表
-- 顾客在线预订阶段不绑定具体房间
-- 入住时前台才分配实际 room
-- ============================================================

CREATE TABLE IF NOT EXISTS room
(
    room_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '实体房间ID',

    room_no          VARCHAR(20)  NOT NULL COMMENT '房号',

    room_type_id     BIGINT       NOT NULL COMMENT '所属房型ID',

    floor_no         INT          NOT NULL COMMENT '楼层',

    status           VARCHAR(30)  NOT NULL DEFAULT 'VACANT'
    COMMENT '房态：VACANT空闲，OCCUPIED入住，DIRTY待清洁，CLEANING清洁中，MAINTENANCE维修，OUT_OF_SERVICE停用',

    remark           VARCHAR(255) NULL COMMENT '备注',

    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (room_id),

    UNIQUE KEY uk_room_no (room_no),

    KEY idx_room_type_id (room_type_id),

    KEY idx_room_status (status),

    CONSTRAINT fk_room_room_type
    FOREIGN KEY (room_type_id)
    REFERENCES room_type (room_type_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '酒店实体房间表';


-- ============================================================
-- 5. 初始化单体酒店基本信息
-- 目前使用项目演示信息，后续后台可修改
-- ============================================================

INSERT INTO hotel_info
(
    hotel_name,
    address,
    phone,
    description,
    check_in_time,
    check_out_time,
    status
)
SELECT
    '智慧酒店',
    '甘肃·兰州',
    '15909238580',
    '智慧酒店预订与运营管理系统',
    '14:00:00',
    '12:00:00',
    1
    WHERE NOT EXISTS
(
    SELECT 1
    FROM hotel_info
);


-- ============================================================
-- 6. 初始化15种房型
-- 总实体房数：354
-- ============================================================

INSERT INTO room_type
(
    type_code,
    type_name,
    area,
    bed_type,
    bed_count,
    max_guests,
    total_rooms,
    status,
    sort_no
)
VALUES
    ('RT01', '豪华大床房',               42.00, '大床', 1, 2, 70, 1, 1),
    ('RT02', '豪华双床房',               42.00, '双床', 2, 2, 54, 1, 2),
    ('RT03', '行政大床房',               42.00, '大床', 1, 2, 42, 1, 3),
    ('RT04', '行政双床房',               42.00, '双床', 2, 2, 34, 1, 4),
    ('RT05', '高级大床房',               57.00, '大床', 1, 2, 30, 1, 5),
    ('RT06', '行政高级大床房',           57.00, '大床', 1, 2, 20, 1, 6),
    ('RT07', '豪华河景大床房',           42.00, '大床', 1, 2, 22, 1, 7),
    ('RT08', '豪华河景双床房',           42.00, '双床', 2, 2, 18, 1, 8),
    ('RT09', '行政河景大床房',           42.00, '大床', 1, 2, 16, 1, 9),
    ('RT10', '行政河景双床房',           42.00, '双床', 2, 2, 14, 1, 10),
    ('RT11', '海底小纵队豪华大床房',     42.00, '大床', 1, 2, 6,  1, 11),
    ('RT12', 'Hape 新丝路亲子豪华大床房',42.00, '大床', 1, 3, 6,  1, 12),
    ('RT13', '小黄鸭主题豪华大床房',     42.00, '大床', 1, 3, 6,  1, 13),
    ('RT14', '豪华套房',                 65.00, '大床', 1, 2, 10, 1, 14),
    ('RT15', '行政套房',                100.00, '大床', 1, 2, 6,  1, 15)

    ON DUPLICATE KEY UPDATE
                         type_name = VALUES(type_name),
                         area = VALUES(area),
                         bed_type = VALUES(bed_type),
                         bed_count = VALUES(bed_count),
                         max_guests = VALUES(max_guests),
                         total_rooms = VALUES(total_rooms),
                         status = VALUES(status),
                         sort_no = VALUES(sort_no);


-- ============================================================
-- 7. 验证
-- ============================================================

SELECT *
FROM hotel_info;

SELECT
    room_type_id,
    type_code,
    type_name,
    area,
    total_rooms,
    status
FROM room_type
ORDER BY sort_no;

SELECT SUM(total_rooms) AS total_room_count
FROM room_type;
