-- Consolidated migration to fix all issues

-- First, apply the original V3 migration (remove_user_references)
CREATE TABLE IF NOT EXISTS process_status (
    id SERIAL PRIMARY KEY,
    bot_id BIGINT REFERENCES bots(id) ON DELETE CASCADE,
    process_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_attempt TIMESTAMP,
    retries INT DEFAULT 0,
    result TEXT,
    error_message TEXT,
    task_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Remove user_id column from process_status table if it exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns 
               WHERE table_name = 'process_status' AND column_name = 'user_id') THEN
        ALTER TABLE process_status DROP COLUMN user_id;
    END IF;
END $$;

-- Create summarized_news table if it doesn't exist
CREATE TABLE IF NOT EXISTS summarized_news (
    id SERIAL PRIMARY KEY,
    bot_id BIGINT REFERENCES bots(id) ON DELETE CASCADE,
    news_id BIGINT REFERENCES news(id) ON DELETE CASCADE,
    summary TEXT,
    summarized_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    summarized_count INT DEFAULT 0,
    CONSTRAINT chk_summarized_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

-- Now fix the instagram_posts table
DO $$
BEGIN
    -- Check if instagram_posts table exists
    IF EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'instagram_posts') THEN
        -- Add error_message column if it doesn't exist
        IF NOT EXISTS (SELECT FROM information_schema.columns 
                      WHERE table_name = 'instagram_posts' AND column_name = 'error_message') THEN
            ALTER TABLE instagram_posts ADD COLUMN error_message TEXT;
        ELSE
            -- If it exists, make sure it's TEXT type
            ALTER TABLE instagram_posts ALTER COLUMN error_message TYPE TEXT;
        END IF;
        
        -- Add image_prompt column if it doesn't exist
        IF NOT EXISTS (SELECT FROM information_schema.columns 
                      WHERE table_name = 'instagram_posts' AND column_name = 'image_prompt') THEN
            ALTER TABLE instagram_posts ADD COLUMN image_prompt TEXT;
        ELSE
            -- If it exists, make sure it's TEXT type
            ALTER TABLE instagram_posts ALTER COLUMN image_prompt TYPE TEXT;
        END IF;
        
        -- Make sure caption is TEXT type
        ALTER TABLE instagram_posts ALTER COLUMN caption TYPE TEXT;
        
        -- Make sure image_url is TEXT type
        ALTER TABLE instagram_posts ALTER COLUMN image_url TYPE TEXT;
        
        -- Make sure instagram_post_id is TEXT type
        ALTER TABLE instagram_posts ALTER COLUMN instagram_post_id TYPE TEXT;
    ELSE
        -- Create the instagram_posts table if it doesn't exist
        CREATE TABLE instagram_posts (
            id SERIAL PRIMARY KEY,
            bot_id BIGINT REFERENCES bots(id) ON DELETE CASCADE,
            news_id BIGINT REFERENCES news(id) ON DELETE CASCADE,
            image_url TEXT,
            caption TEXT,
            image_prompt TEXT,
            error_message TEXT,
            post_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
            instagram_post_id TEXT,
            retry_count INT DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            posted_at TIMESTAMP,
            CONSTRAINT chk_post_status CHECK (post_status IN ('PENDING', 'IMAGE_GENERATED', 'POSTED', 'FAILED'))
        );
        
        -- Create indexes
        CREATE INDEX idx_instagram_posts_status ON instagram_posts(post_status);
        CREATE INDEX idx_instagram_posts_bot_id ON instagram_posts(bot_id);
    END IF;
END $$; 