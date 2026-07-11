CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    username VARCHAR(64) NOT NULL COMMENT 'Login name',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt password hash',
    nickname VARCHAR(64) NULL COMMENT 'Display name',
    email VARCHAR(128) NULL COMMENT 'Email address',
    role TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0: user, 1: admin',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0: disabled, 1: enabled',
    last_login_at DATETIME(3) NULL COMMENT 'Last successful login time',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Last update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_email (email),
    KEY idx_sys_user_role_status (role, status),
    KEY idx_sys_user_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='System users';

CREATE TABLE IF NOT EXISTS user_refresh_token (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'User id',
    token_id CHAR(36) NOT NULL COMMENT 'Refresh token unique identifier',
    token_hash CHAR(64) NOT NULL COMMENT 'SHA-256 digest of refresh token',
    device_id VARCHAR(128) NULL COMMENT 'Client device identifier',
    user_agent VARCHAR(512) NULL COMMENT 'Client user agent',
    client_ip VARCHAR(45) NULL COMMENT 'Client IP address',
    expires_at DATETIME(3) NOT NULL COMMENT 'Expiration time',
    last_used_at DATETIME(3) NULL COMMENT 'Last refresh time',
    revoked_at DATETIME(3) NULL COMMENT 'Revocation time',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_refresh_token_token_id (token_id),
    UNIQUE KEY uk_user_refresh_token_hash (token_hash),
    KEY idx_user_refresh_token_user_id (user_id),
    KEY idx_user_refresh_token_expires_at (expires_at),
    CONSTRAINT fk_user_refresh_token_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Refresh token sessions';

CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Owner user id',
    title VARCHAR(200) NOT NULL DEFAULT 'New conversation' COMMENT 'Conversation title',
    model VARCHAR(100) NULL COMMENT 'AI model identifier',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0: archived, 1: active',
    last_message_at DATETIME(3) NULL COMMENT 'Last message time',
    deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Last update time',
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user_updated_at (user_id, updated_at),
    KEY idx_ai_conversation_user_deleted (user_id, deleted),
    CONSTRAINT fk_ai_conversation_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI conversations';

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    conversation_id BIGINT UNSIGNED NOT NULL COMMENT 'Conversation id',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'User id',
    sequence_no INT UNSIGNED NOT NULL COMMENT 'Message sequence in conversation',
    role VARCHAR(20) NOT NULL COMMENT 'user, assistant, system or tool',
    content MEDIUMTEXT NOT NULL COMMENT 'Message content',
    model VARCHAR(100) NULL COMMENT 'AI model identifier',
    input_tokens INT UNSIGNED NULL COMMENT 'Input token count',
    output_tokens INT UNSIGNED NULL COMMENT 'Output token count',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_chat_message_sequence (conversation_id, sequence_no),
    KEY idx_ai_chat_message_conversation_created_at (conversation_id, created_at),
    KEY idx_ai_chat_message_user_created_at (user_id, created_at),
    CONSTRAINT fk_ai_chat_message_conversation_id FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id),
    CONSTRAINT fk_ai_chat_message_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI chat messages';
