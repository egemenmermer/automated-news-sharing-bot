-- Drop the existing check constraint first
ALTER TABLE bot_logs 
DROP CONSTRAINT IF EXISTS chk_log_type;

-- Convert log_type to smallint
ALTER TABLE bot_logs 
ALTER COLUMN log_type TYPE smallint USING 
    CASE 
        WHEN log_type = 'INFO' THEN 0
        WHEN log_type = 'WARNING' THEN 1
        WHEN log_type = 'ERROR' THEN 2
        ELSE 0
    END;

-- Add new check constraint for numeric values
ALTER TABLE bot_logs 
ADD CONSTRAINT chk_log_type CHECK (log_type IN (0, 1, 2)); 