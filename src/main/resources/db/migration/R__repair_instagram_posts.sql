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