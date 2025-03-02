-- Insert test bot
INSERT INTO bots (
    name,
    instagram_username,
    instagram_password,
    instagram_access_token,
    instagram_user_id,
    pexels_api_key,
    mediastack_api_key,
    gemini_api_key,
    telegram_bot_username,
    telegram_bot_token,
    fetch_time,
    post_time
) VALUES (
    'NeuralNewsBot',
    'neuralnews_',
    'D58df18e0d@',
    'IGAAXRyZC4x1NBBZAE92TGVxNHBaeU1QMlU2dzUzMExwdTJPTFNyclZAaMzdKNXNHQVcydUlIcGpkcDdIX3k5ZAzNPRnJ4YkxjczRBUVBjWWVBaG9nbG9qLVJaRHhJcVRwREVod3VEeXlVMVJIQTQ0d1plcjVldXZAXbUNKUTVjSUNTSQZDZD',
    '72149775517',
    'wxjkJwQc0hcMtMqEX6DYXZOtpAQPxB6t3fFem03nUyzF2TEakGniw1GL',
    '8c2e88b68bfd74df64a1b19241de9c22',
    'AIzaSyDWWdE3JbZlOKqWvNP9lYQ_IbD1CFrc_XE',
    'neuralnewssbot',
    '7712107324:AAH7F0gMj1cYFB-ptpVPxxVUlpakWx55k4o',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 minutes'
);
