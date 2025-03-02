-- Add error_message column to instagram_posts table
ALTER TABLE instagram_posts ADD COLUMN IF NOT EXISTS error_message TEXT;
