CREATE TABLE IF NOT EXISTS user_daily_check_in (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Checked-in user id',
    check_in_date DATE NOT NULL COMMENT 'Check-in date in Asia/Shanghai',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Check-in time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_daily_check_in_user_date (user_id, check_in_date),
    KEY idx_user_daily_check_in_date (check_in_date),
    CONSTRAINT fk_user_daily_check_in_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Daily user check-in records';
