-- Insert a test bot if none exists
INSERT INTO bots (name, telegram_bot_username, telegram_bot_token, instagram_username, instagram_password, 
                 instagram_user_id, instagram_access_token, mediastack_api_key, pexels_api_key, 
                 gemini_api_key, fetch_time, post_time)
SELECT 'NeuralNews', 'neuralnewssbot', '7712107324:AAH7F0gMj1cYFB-ptpVPxxVUlpakWx55k4o', 
       'neuralnews_', 'D58df18e0d@', '72149775517', 
       'IGAAXRyZC4x1NBBZAE0yZADRpZAy1aaldQNjBJSURrTm9FT2VBZAGJNNF9LdFF4eDZAMUlVXbEFsYjhWdllPemtxMUFaRGJxaUotYk1rbHAzM3g5OHBJTTNmVkQ5ZAklYclF1THIyVHVmTjViYVRtSjJJSkdtS0ZAuZAGxBRXlibHVod1VsVQZDZD', 
       'a29c8129cb28dcb27fad2282c60eb332', 'wxjkJwQc0hcMtMqEX6DYXZOtpAQPxB6t3fFem03nUyzF2TEakGniw1GL', 
       'AIzaSyDXBRDllz01K06ZloNomUq-WOmVSa0aBYw', '09:00', '12:00'
WHERE NOT EXISTS (SELECT 1 FROM bots LIMIT 1);
