INSERT INTO user_profiles (
    telegram_id,
    telegram_id_hash,
    username,
    role_name,
    bot_status,
    password_hash,
    is_email_active,
    created_at,
    updated_at
)
SELECT
    'admin-seed-' || md5(random()::text || clock_timestamp()::text),
    md5(random()::text || clock_timestamp()::text),
    'admin',
    'ADMIN',
    'ACTIVE',
    '$2b$12$kRB8jLTTY54a2iGYo7YnKe91Mf2G1hHB6lH1ZHVD6tz/CtAzE/3E6',
    FALSE,
    now(),
    now()
    WHERE NOT EXISTS (
    SELECT 1 FROM user_profiles WHERE username = 'admin'
);