-- Create bots table
CREATE TABLE bots (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    api_key VARCHAR(255) NOT NULL,
    api_secret VARCHAR(255) NOT NULL,
    instagram_username VARCHAR(255),
    instagram_password VARCHAR(255),
    instagram_access_token TEXT,
    pexels_api_key VARCHAR(255),
    mediastack_api_key VARCHAR(255),
    fetch_time TIME NOT NULL DEFAULT '00:00:00',
    post_time TIME NOT NULL DEFAULT '00:30:00',
    last_run TIMESTAMP
);

-- Create news table
CREATE TABLE news (
    id SERIAL PRIMARY KEY,
    bot_id INT REFERENCES bots(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    content TEXT,
    description TEXT,
    image_url TEXT,
    generated_image_path TEXT,
    published_at TIMESTAMP NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    posted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create instagram_posts table
CREATE TABLE instagram_posts (
    id SERIAL PRIMARY KEY,
    bot_id INT REFERENCES bots(id) ON DELETE CASCADE,
    news_id INT REFERENCES news(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    caption TEXT,
    post_status VARCHAR(50) NOT NULL CHECK (post_status IN ('PENDING', 'IMAGE_GENERATED', 'POSTED', 'FAILED')),
    instagram_post_id TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    posted_at TIMESTAMP
);

-- Create bot_logs table
CREATE TABLE bot_logs (
    id SERIAL PRIMARY KEY,
    bot_id INT REFERENCES bots(id) ON DELETE CASCADE,
    log_type VARCHAR(50) NOT NULL CHECK (log_type IN ('INFO', 'WARNING', 'ERROR')),
    log_message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create fetch_logs table
CREATE TABLE fetch_logs (
    id SERIAL PRIMARY KEY,
    bot_id INT REFERENCES bots(id) ON DELETE CASCADE,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    fetched_count INT NOT NULL,
    log_message TEXT
);

-- Create post_logs table
CREATE TABLE post_logs (
    id SERIAL PRIMARY KEY,
    bot_id INT REFERENCES bots(id) ON DELETE CASCADE,
    post_type VARCHAR(50) DEFAULT 'INSTAGRAM',
    platform VARCHAR(50) DEFAULT 'INSTAGRAM',
    post_id INT,
    status VARCHAR(50) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED')),
    log_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_news_processed_posted ON news(processed, posted);
CREATE INDEX idx_instagram_posts_status ON instagram_posts(post_status);
CREATE INDEX idx_instagram_posts_bot_id ON instagram_posts(bot_id); 