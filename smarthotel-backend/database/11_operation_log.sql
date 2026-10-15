USE smart_hotel;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(100) NULL,
    operation VARCHAR(100) NOT NULL,
    module VARCHAR(60) NOT NULL,
    request_url VARCHAR(255) NOT NULL,
    ip VARCHAR(45) NULL,
    success TINYINT(1) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_log_user_time (user_id, create_time),
    KEY idx_operation_log_module_time (module, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
