-- ============================================================
-- SmartHotel 智慧酒店预订与运营管理系统
-- 文件：01_user_auth.sql
-- 说明：用户、角色、员工、客户、会员基础数据表
-- MySQL：8.x
-- ============================================================

USE smart_hotel;

SET NAMES utf8mb4;


-- ============================================================
-- 1. 系统角色表 sys_role
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_role
(
    role_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code    VARCHAR(30)  NOT NULL COMMENT '角色编码',
    role_name    VARCHAR(50)  NOT NULL COMMENT '角色名称',
    description  VARCHAR(255) NULL COMMENT '角色描述',

    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',

    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (role_id),

    UNIQUE KEY uk_role_code (role_code)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '系统角色表';


-- ============================================================
-- 2. 系统用户表 sys_user
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user
(
    user_id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',

    username         VARCHAR(50)  NOT NULL COMMENT '登录账号',

    password         VARCHAR(255) NOT NULL COMMENT '加密后的登录密码',

    role_id          BIGINT       NOT NULL COMMENT '角色ID',

    nickname         VARCHAR(50)  NULL COMMENT '显示昵称',

    phone            VARCHAR(20)  NULL COMMENT '登录手机号',

    avatar_url       VARCHAR(255) NULL COMMENT '头像地址',

    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
    COMMENT '账号状态：ACTIVE正常，DISABLED停用，LOCKED锁定',

    last_login_time  DATETIME     NULL COMMENT '最后登录时间',

    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (user_id),

    UNIQUE KEY uk_user_username (username),

    UNIQUE KEY uk_user_phone (phone),

    KEY idx_user_role_id (role_id),

    CONSTRAINT fk_user_role
    FOREIGN KEY (role_id)
    REFERENCES sys_role (role_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '系统用户表';


-- ============================================================
-- 3. 员工信息表 employee
-- ============================================================

CREATE TABLE IF NOT EXISTS employee
(
    employee_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '员工ID',

    user_id        BIGINT       NOT NULL COMMENT '关联系统用户ID',

    employee_no    VARCHAR(30)  NOT NULL COMMENT '员工工号',

    employee_name  VARCHAR(50)  NOT NULL COMMENT '员工姓名',

    phone          VARCHAR(20)  NULL COMMENT '联系电话',

    email          VARCHAR(100) NULL COMMENT '电子邮箱',

    position       VARCHAR(50)  NULL COMMENT '职位',

    hire_date      DATE         NULL COMMENT '入职日期',

    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
    COMMENT '员工状态：ACTIVE在职，INACTIVE停用/离职',

    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (employee_id),

    UNIQUE KEY uk_employee_user_id (user_id),

    UNIQUE KEY uk_employee_no (employee_no),

    CONSTRAINT fk_employee_user
    FOREIGN KEY (user_id)
    REFERENCES sys_user (user_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '员工信息表';


-- ============================================================
-- 4. 会员等级表 member_level
-- ============================================================

CREATE TABLE IF NOT EXISTS member_level
(
    level_id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '会员等级ID',

    level_code        VARCHAR(30)   NOT NULL COMMENT '会员等级编码',

    level_name        VARCHAR(50)   NOT NULL COMMENT '会员等级名称',

    min_consumption   DECIMAL(12,2) NOT NULL DEFAULT 0.00
    COMMENT '最低累计消费金额',

    discount_rate     DECIMAL(5,4)  NOT NULL DEFAULT 1.0000
    COMMENT '折扣系数，例如0.9500表示95折',

    description       VARCHAR(255)  NULL COMMENT '会员等级说明',

    sort_no           INT           NOT NULL DEFAULT 0 COMMENT '排序号',

    status            TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',

    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (level_id),

    UNIQUE KEY uk_member_level_code (level_code)

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '会员等级表';


-- ============================================================
-- 5. 客户信息表 customer
-- ============================================================

CREATE TABLE IF NOT EXISTS customer
(
    customer_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '客户ID',

    user_id          BIGINT       NULL COMMENT '系统用户ID，前台线下建档客户可以为空',

    customer_name    VARCHAR(50)  NOT NULL COMMENT '客户姓名',

    phone            VARCHAR(20)  NOT NULL COMMENT '联系电话',

    id_type          VARCHAR(20)  NULL DEFAULT 'ID_CARD'
    COMMENT '证件类型',

    id_number        VARCHAR(50)  NULL COMMENT '证件号码',

    gender           VARCHAR(10)  NULL COMMENT '性别',

    birthday         DATE         NULL COMMENT '出生日期',

    level_id         BIGINT       NULL COMMENT '当前会员等级ID',

    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
    COMMENT '客户状态：ACTIVE正常，DISABLED停用',

    register_source  VARCHAR(20)  NOT NULL DEFAULT 'ONLINE'
    COMMENT '客户来源：ONLINE线上注册，FRONT_DESK前台建档',

    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (customer_id),

    UNIQUE KEY uk_customer_user_id (user_id),

    UNIQUE KEY uk_customer_phone (phone),

    UNIQUE KEY uk_customer_id_number (id_number),

    KEY idx_customer_level_id (level_id),

    CONSTRAINT fk_customer_user
    FOREIGN KEY (user_id)
    REFERENCES sys_user (user_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

    CONSTRAINT fk_customer_member_level
    FOREIGN KEY (level_id)
    REFERENCES member_level (level_id)
    ON UPDATE CASCADE
    ON DELETE SET NULL

    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '客户信息表';


-- ============================================================
-- 6. 初始化系统角色
-- ============================================================

INSERT INTO sys_role
(
    role_code,
    role_name,
    description,
    status
)
VALUES
    (
        'ADMIN',
        '管理员',
        '系统管理员，负责酒店后台配置、用户权限和运营管理',
        1
    ),
    (
        'STAFF',
        '前台员工',
        '酒店前台工作人员，负责预订处理、入住、换房、续住和退房',
        1
    ),
    (
        'CUSTOMER',
        '客户',
        '酒店普通客户，可进行房型查询、在线预订和订单管理',
        1
    )
    ON DUPLICATE KEY UPDATE
                         role_name = VALUES(role_name),
                         description = VALUES(description),
                         status = VALUES(status);


-- ============================================================
-- 7. 初始化普通会员等级
-- ============================================================

INSERT INTO member_level
(
    level_code,
    level_name,
    min_consumption,
    discount_rate,
    description,
    sort_no,
    status
)
VALUES
    (
        'NORMAL',
        '普通会员',
        0.00,
        1.0000,
        'SmartHotel普通注册会员',
        1,
        1
    )
    ON DUPLICATE KEY UPDATE
                         level_name = VALUES(level_name),
                         min_consumption = VALUES(min_consumption),
                         discount_rate = VALUES(discount_rate),
                         description = VALUES(description),
                         sort_no = VALUES(sort_no),
                         status = VALUES(status);


-- ============================================================
-- 8. 验证结果
-- ============================================================

SHOW TABLES;

SELECT
    role_id,
    role_code,
    role_name,
    status
FROM sys_role;

SELECT
    level_id,
    level_code,
    level_name,
    min_consumption,
    discount_rate
FROM member_level;