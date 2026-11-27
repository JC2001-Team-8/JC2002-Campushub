-- Store the notification preferences exposed by the frozen UserService endpoints.
-- V1-V4 remain unchanged; email delivery itself is not enabled in this iteration.
ALTER TABLE users
    ADD COLUMN in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email_enabled BOOLEAN NOT NULL DEFAULT FALSE;
