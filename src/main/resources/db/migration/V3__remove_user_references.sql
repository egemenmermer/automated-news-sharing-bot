-- Create process_status table
CREATE TABLE process_status (
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

-- Remove user_id column from process_status table
ALTER TABLE process_status DROP COLUMN IF EXISTS user_id;

-- Create summarized_news table
CREATE TABLE summarized_news (
    id SERIAL PRIMARY KEY,
    bot_id BIGINT REFERENCES bots(id) ON DELETE CASCADE,
    news_id BIGINT REFERENCES news(id) ON DELETE CASCADE,
    summary TEXT,
    summarized_at TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    summarized_count INT DEFAULT 0,
    CONSTRAINT chk_summarized_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);
