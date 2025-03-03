-- Repeatable migration to fix instagram_posts table
-- This will run every time regardless of version numbers

-- Check if instagram_posts table exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'instagram_posts') THEN
        -- Check if caption column is varchar
        IF EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'instagram_posts' 
            AND column_name = 'caption' 
            AND data_type = 'character varying'
        ) THEN
            -- Alter existing columns to TEXT type
            ALTER TABLE instagram_posts ALTER COLUMN caption TYPE TEXT;
        END IF;
        
        -- Check if error_message column is varchar
        IF EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'instagram_posts' 
            AND column_name = 'error_message' 
            AND data_type = 'character varying'
        ) THEN
            -- Alter existing columns to TEXT type
            ALTER TABLE instagram_posts ALTER COLUMN error_message TYPE TEXT;
        END IF;
        
        -- Check if image_prompt column is varchar
        IF EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'instagram_posts' 
            AND column_name = 'image_prompt' 
            AND data_type = 'character varying'
        ) THEN
            -- Alter existing columns to TEXT type
            ALTER TABLE instagram_posts ALTER COLUMN image_prompt TYPE TEXT;
        END IF;
        
        -- Check if image_url column is varchar
        IF EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'instagram_posts' 
            AND column_name = 'image_url' 
            AND data_type = 'character varying'
        ) THEN
            -- Alter existing columns to TEXT type
            ALTER TABLE instagram_posts ALTER COLUMN image_url TYPE TEXT;
        END IF;
        
        -- Check if instagram_post_id column is varchar
        IF EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'instagram_posts' 
            AND column_name = 'instagram_post_id' 
            AND data_type = 'character varying'
        ) THEN
            -- Alter existing columns to TEXT type
            ALTER TABLE instagram_posts ALTER COLUMN instagram_post_id TYPE TEXT;
        END IF;
    END IF;
END $$;

-- Repair script for instagram_posts table

-- Check if the table exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'instagram_posts') THEN
        -- Add title column if it doesn't exist
        IF NOT EXISTS (SELECT FROM information_schema.columns 
                      WHERE table_name = 'instagram_posts' AND column_name = 'title') THEN
            ALTER TABLE instagram_posts ADD COLUMN title VARCHAR(255);
        END IF;
        
        -- Make sure bot_id is not nullable
        ALTER TABLE instagram_posts ALTER COLUMN bot_id SET NOT NULL;
        
        -- Set default value for post_status if it's null
        UPDATE instagram_posts SET post_status = 'PENDING' WHERE post_status IS NULL;
        
        -- Set default timestamp for created_at if it's null
        UPDATE instagram_posts SET created_at = NOW() WHERE created_at IS NULL;
        
        -- Set default retry_count if it's null
        UPDATE instagram_posts SET retry_count = 0 WHERE retry_count IS NULL;
    END IF;
END $$; 