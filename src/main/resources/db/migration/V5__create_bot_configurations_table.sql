-- Create bot_configurations table
CREATE TABLE bot_configurations (
    id SERIAL PRIMARY KEY,
    bot_id BIGINT REFERENCES bots(id) ON DELETE CASCADE,
    config_type VARCHAR(50) NOT NULL,
    config_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_config_type CHECK (config_type IN ('FETCH_TIME', 'MAX_RETRIES', 'POST_TIME', 'TOPIC', 'API_KEY', 'API_SECRET', 'FETCH_AMOUNT'))
);
