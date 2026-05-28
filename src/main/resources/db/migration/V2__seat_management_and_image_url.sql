-- =============================================================================
-- Migration V2: Seat Management, Image URL, and Optimistic Lock Version
-- Project : eventslk / event-registration-api
-- Author  : kaveengayanga@gmail.com
-- Date    : 2026-05-28
-- =============================================================================
-- Run this script ONCE against the target PostgreSQL database BEFORE deploying
-- the v2 application build.  Hibernate ddl-auto must NOT be "create" or
-- "create-drop" in production; use "validate" or "none".
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. event_entity — replace BLOB image with imageUrl, add seat columns + version
-- -----------------------------------------------------------------------------

-- Drop the legacy binary image column (large LOB, no longer stored in DB)
ALTER TABLE event_entity
    DROP COLUMN IF EXISTS image_data;

-- URL of the image stored in S3; set by the client after direct presigned upload
ALTER TABLE event_entity
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024);

-- Seat tracking columns
ALTER TABLE event_entity
    ADD COLUMN IF NOT EXISTS total_seats     INTEGER NOT NULL DEFAULT 0;

ALTER TABLE event_entity
    ADD COLUMN IF NOT EXISTS available_seats INTEGER NOT NULL DEFAULT 0;

-- Optimistic lock version; managed by JPA @Version — never update manually
ALTER TABLE event_entity
    ADD COLUMN IF NOT EXISTS version         INTEGER NOT NULL DEFAULT 0;

-- -----------------------------------------------------------------------------
-- 2. booking_order_entity — add seat_count to support cancellation seat restore
-- -----------------------------------------------------------------------------

ALTER TABLE booking_order_entity
    ADD COLUMN IF NOT EXISTS seat_count INTEGER NOT NULL DEFAULT 1;

-- Backfill existing rows (legacy bookings assumed 1 seat each)
UPDATE booking_order_entity SET seat_count = 1 WHERE seat_count = 0;

-- -----------------------------------------------------------------------------
-- 3. Performance indexes
-- -----------------------------------------------------------------------------

-- Speeds up the seat-availability check during high-concurrency booking bursts
CREATE INDEX IF NOT EXISTS idx_event_available_seats
    ON event_entity (available_seats);

-- Speeds up cancellation lookups by user + status
CREATE INDEX IF NOT EXISTS idx_booking_user_status
    ON booking_order_entity (user_id, order_status);

-- =============================================================================
-- Rollback (manual, if needed):
--
--   ALTER TABLE event_entity
--       ADD COLUMN image_data BYTEA,
--       DROP COLUMN IF EXISTS image_url,
--       DROP COLUMN IF EXISTS total_seats,
--       DROP COLUMN IF EXISTS available_seats,
--       DROP COLUMN IF EXISTS version;
--
--   ALTER TABLE booking_order_entity DROP COLUMN IF EXISTS seat_count;
--
--   DROP INDEX IF EXISTS idx_event_available_seats;
--   DROP INDEX IF EXISTS idx_booking_user_status;
-- =============================================================================
