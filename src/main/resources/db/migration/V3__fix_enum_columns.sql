-- Convert log_type to use numeric values
ALTER TABLE bot_logs 
    ALTER COLUMN log_type TYPE smallint USING 
    CASE 
        WHEN log_type = 'INFO' THEN 0
        WHEN log_type = 'WARNING' THEN 1
        WHEN log_type = 'ERROR' THEN 2
        ELSE 0
    END;

-- Add check constraint for log_type
ALTER TABLE bot_logs 
    ADD CONSTRAINT chk_log_type CHECK (log_type IN (0, 1, 2));

-- Convert status columns to use enum values
ALTER TABLE fetch_logs 
    ALTER COLUMN status TYPE VARCHAR(50),
    ADD CONSTRAINT chk_fetch_status CHECK (status IN ('SUCCESS', 'FAILED'));

ALTER TABLE post_logs 
    ALTER COLUMN status TYPE VARCHAR(50),
    ADD CONSTRAINT chk_post_status CHECK (status IN ('SUCCESS', 'FAILED'));

ALTER TABLE instagram_posts 
    ALTER COLUMN post_status TYPE VARCHAR(50),
    ADD CONSTRAINT chk_post_status_instagram CHECK (post_status IN ('PENDING', 'IMAGE_GENERATED', 'POSTED', 'FAILED')); 