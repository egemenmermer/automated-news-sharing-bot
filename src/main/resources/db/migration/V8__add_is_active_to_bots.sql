-- Add is_active column to bots table with default value true
ALTER TABLE bots ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;

-- Add is_fetch_turkish column to bots table with default value false
ALTER TABLE bots ADD COLUMN IF NOT EXISTS is_fetch_turkish BOOLEAN NOT NULL DEFAULT false;