USE smart_hotel;

CREATE TABLE IF NOT EXISTS customer_saved_guest (
    guest_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '常用入住人ID',
    user_id BIGINT NOT NULL COMMENT '所属顾客账号ID',
    guest_name VARCHAR(50) NOT NULL COMMENT '入住人姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    id_type VARCHAR(20) NOT NULL DEFAULT 'ID_CARD' COMMENT '证件类型',
    id_number VARCHAR(50) NULL COMMENT '证件号码',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (guest_id),
    KEY idx_saved_guest_user (user_id),
    UNIQUE KEY uk_saved_guest_document (user_id, id_type, id_number),
    CONSTRAINT fk_saved_guest_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='顾客常用入住人';
