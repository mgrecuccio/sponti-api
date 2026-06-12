ALTER TABLE user_preferences
    ADD COLUMN IF NOT EXISTS push_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS suggestion_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE user_preferences preferences
SET push_notifications_enabled = notification_preferences.push_enabled,
    suggestion_notifications_enabled = notification_preferences.suggestion_notifications_enabled
FROM notification_preferences
WHERE notification_preferences.user_id = preferences.user_id;

DROP TABLE IF EXISTS notification_preferences;
