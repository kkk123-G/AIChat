UPDATE ai_conversation
SET title = '新对话'
WHERE title = 'New conversation';

ALTER TABLE ai_conversation
    MODIFY COLUMN title VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT 'Conversation title';
