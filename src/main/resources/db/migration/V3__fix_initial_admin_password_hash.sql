-- Fix initial local Super Admin password hash.
-- Login: admin@fap.local / password
-- Change this password immediately after first login.

UPDATE users
SET password_hash = '$2a$10$01NUzQzkNOLNGFoVGj7zH.gT..UEyh6ULvxJ3sgPUOXQr6oNCBlfG',
    updated_at = CURRENT_TIMESTAMP
WHERE lower(email) = 'admin@fap.local';
