-- Create bots table
CREATE TABLE IF NOT EXISTS bots (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    telegram_bot_username VARCHAR(255),
    telegram_bot_token VARCHAR(255),
    instagram_username VARCHAR(255),
    instagram_password VARCHAR(255),
    instagram_user_id VARCHAR(255),
    instagram_access_token TEXT,
    mediastack_api_key VARCHAR(255),
    pexels_api_key VARCHAR(255),
    gemini_api_key VARCHAR(255),
    fetch_time VARCHAR(50),
    post_time VARCHAR(50),
    last_run TIMESTAMP
);

-- Create news table
CREATE TABLE IF NOT EXISTS news (
    id SERIAL PRIMARY KEY,
    bot_id INTEGER NOT NULL REFERENCES bots(id),
    title VARCHAR(255) NOT NULL,
    content TEXT,
    url VARCHAR(255),
    image_url TEXT,
    source VARCHAR(255),
    category VARCHAR(100),
    published_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING'
);

-- Create instagram_posts table
CREATE TABLE IF NOT EXISTS instagram_posts (
    id SERIAL PRIMARY KEY,
    bot_id INTEGER NOT NULL REFERENCES bots(id),
    news_id INTEGER REFERENCES news(id),
    title VARCHAR(255),
    caption TEXT,
    image_url TEXT,
    image_prompt TEXT,
    instagram_post_id VARCHAR(255),
    post_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    posted_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT
);

-- Create fetch_logs table
CREATE TABLE IF NOT EXISTS fetch_logs (
    id SERIAL PRIMARY KEY,
    bot_id INTEGER NOT NULL REFERENCES bots(id),
    fetch_time TIMESTAMP DEFAULT NOW(),
    status VARCHAR(50),
    articles_found INTEGER DEFAULT 0,
    articles_saved INTEGER DEFAULT 0,
    error_message TEXT
);

-- Create bot_configs table
CREATE TABLE IF NOT EXISTS bot_configs (
    id SERIAL PRIMARY KEY,
    bot_id INTEGER NOT NULL REFERENCES bots(id),
    config_type VARCHAR(50) NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    UNIQUE (bot_id, config_type, config_key)
);

-- Create bot_logs table
CREATE TABLE bot_logs (
    id SERIAL PRIMARY KEY,
    bot_id INT REFERENCES bots(id) ON DELETE CASCADE,
    log_type VARCHAR(50) NOT NULL,
    log_message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_log_type CHECK (log_type IN ('INFO', 'WARNING', 'ERROR'))
);

-- Create post_logs table
CREATE TABLE post_logs (
    id SERIAL PRIMARY KEY,
    bot_id INT REFERENCES bots(id) ON DELETE CASCADE,
    post_type VARCHAR(50) DEFAULT 'INSTAGRAM',
    platform VARCHAR(50) DEFAULT 'INSTAGRAM',
    post_id INT,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    log_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_post_log_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

-- Create indexes
CREATE INDEX idx_news_status ON news(status);
CREATE INDEX idx_instagram_posts_status ON instagram_posts(post_status);
CREATE INDEX idx_instagram_posts_bot_id ON instagram_posts(bot_id);

-- This is handled by Hibernate now (we saw the tables being created in the logs)