DROP TABLE IF EXISTS user_balance_change_record;

CREATE TABLE user_balance_change_record (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Affected user id',
    change_amount DECIMAL(12, 2) NOT NULL COMMENT 'Signed balance change amount',
    balance_before DECIMAL(12, 2) NOT NULL COMMENT 'Balance before change',
    balance_after DECIMAL(12, 2) NOT NULL COMMENT 'Balance after change',
    change_type VARCHAR(32) NOT NULL COMMENT 'ADMIN_RECHARGE, DAILY_CHECK_IN, AI_CHAT_CONSUMPTION or AI_CHAT_REFUND',
    reference_id BIGINT UNSIGNED NULL COMMENT 'Related business record id',
    remark VARCHAR(200) NULL COMMENT 'Change description',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    PRIMARY KEY (id),
    KEY idx_user_balance_change_user_created_at (user_id, created_at, id),
    KEY idx_user_balance_change_type_reference (change_type, reference_id),
    CONSTRAINT fk_user_balance_change_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User balance change records';
