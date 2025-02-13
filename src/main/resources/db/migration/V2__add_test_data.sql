-- Insert test bot
INSERT INTO bots (
    name, 
    api_key, 
    api_secret, 
    instagram_username, 
    instagram_password,
    mediastack_api_key,
    pexels_api_key,
    fetch_time,
    post_time
) VALUES (
    'TestBot',
    'test_api_key',
    'test_secret',
    'test_instagram',
    'test_password',
    'test_mediastack_key',
    'test_pexels_key',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 minutes'
);


