-- Insert a test bot if none exists
INSERT INTO bots (name, telegram_bot_username, telegram_bot_token, instagram_username, instagram_password, 
                 instagram_user_id, instagram_access_token, mediastack_api_key, pexels_api_key, 
                 gemini_api_key, fetch_time, post_time)
SELECT 'NeuralNews', 'neuralnewssbot', '${TELEGRAM_BOT_TOKEN}', '${INSTAGRAM_USERNAME}', 
       '${INSTAGRAM_PASSWORD}', '${INSTAGRAM_USER_ID}', '${INSTAGRAM_ACCESS_TOKEN}', 
       '${MEDIASTACK_API_KEY}', '${PEXELS_API_KEY}', '${GEMINI_API_KEY}', 
       '09:00', '10:00'
WHERE NOT EXISTS (SELECT 1 FROM bots LIMIT 1);

-- Insert bot configurations
INSERT INTO bot_configurations (bot_id, config_type, config_value, created_at)
SELECT id, 'TOPIC', 'technology', CURRENT_TIMESTAMP FROM bots WHERE name = 'NeuralNews'
UNION ALL
SELECT id, 'FETCH_AMOUNT', '10', CURRENT_TIMESTAMP FROM bots WHERE name = 'NeuralNews'
UNION ALL
SELECT id, 'MAX_RETRIES', '3', CURRENT_TIMESTAMP FROM bots WHERE name = 'NeuralNews'
UNION ALL
SELECT id, 'SCHEDULE_MINUTES', '30', CURRENT_TIMESTAMP FROM bots WHERE name = 'NeuralNews';
