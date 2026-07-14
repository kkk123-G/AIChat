CREATE TABLE IF NOT EXISTS user_recharge_record (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Recharge recipient user id',
    operator_id BIGINT UNSIGNED NOT NULL COMMENT 'Administrator user id',
    amount DECIMAL(12, 2) NOT NULL COMMENT 'Recharge amount',
    balance_before DECIMAL(12, 2) NOT NULL COMMENT 'Balance before recharge',
    balance_after DECIMAL(12, 2) NOT NULL COMMENT 'Balance after recharge',
    remark VARCHAR(200) NULL COMMENT 'Administrator remark',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    PRIMARY KEY (id),
    KEY idx_user_recharge_record_user_created_at (user_id, created_at),
    KEY idx_user_recharge_record_operator_created_at (operator_id, created_at),
    CONSTRAINT fk_user_recharge_record_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_recharge_record_operator_id FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User balance recharge records';
