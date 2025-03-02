-- Remove user_id column from process_status table
ALTER TABLE process_status DROP CONSTRAINT IF EXISTS fk72w8gnmrenbuj6xo2n95i3sg2;
ALTER TABLE process_status DROP COLUMN IF EXISTS user_id;

-- Remove posted_tweet_id column from post_logs table
ALTER TABLE post_logs DROP CONSTRAINT IF EXISTS fknut8pgo340xew490e90gjx7g0;
ALTER TABLE post_logs DROP COLUMN IF EXISTS posted_tweet_id;

-- Drop tweets table and its references
ALTER TABLE tweets DROP CONSTRAINT IF EXISTS fk9kcg4gua27dgj1qh6javm7jur;
ALTER TABLE tweets DROP CONSTRAINT IF EXISTS fkt9kobrtsykvyxh25oif7hysly;
DROP TABLE IF EXISTS tweets CASCADE;

-- Drop users table and its references
DROP TABLE IF EXISTS users CASCADE;

-- Rename content column to summary in summarized_news table
ALTER TABLE summarized_news RENAME COLUMN content TO summary;
