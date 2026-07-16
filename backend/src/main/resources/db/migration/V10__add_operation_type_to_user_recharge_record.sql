ALTER TABLE user_recharge_record
    ADD COLUMN operation_type VARCHAR(16) NOT NULL DEFAULT 'RECHARGE' COMMENT 'Balance operation type' AFTER recharge_no;
