ALTER TABLE sys_user
    ADD COLUMN balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Account balance' AFTER email,
    ADD COLUMN last_active_at DATETIME(3) NULL COMMENT 'Last account activity time' AFTER last_login_at,
    ADD COLUMN last_used_at DATETIME(3) NULL COMMENT 'Last account usage time' AFTER last_active_at;
