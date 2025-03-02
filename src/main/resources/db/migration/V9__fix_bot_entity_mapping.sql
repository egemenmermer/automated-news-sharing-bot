-- Add image_prompt column to instagram_posts table if it doesn't exist
ALTER TABLE instagram_posts ADD COLUMN IF NOT EXISTS image_prompt TEXT;

-- Ensure error_message column exists in instagram_posts table
ALTER TABLE instagram_posts ADD COLUMN IF NOT EXISTS error_message TEXT;

-- Make sure the bot entity columns have the correct types
ALTER TABLE bots ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE bots ALTER COLUMN is_active SET DEFAULT true;

ALTER TABLE bots ALTER COLUMN is_fetch_turkish SET NOT NULL;
ALTER TABLE bots ALTER COLUMN is_fetch_turkish SET DEFAULT false;

-- Update any null values to defaults
UPDATE bots SET is_active = true WHERE is_active IS NULL;
UPDATE bots SET is_fetch_turkish = false WHERE is_fetch_turkish IS NULL;