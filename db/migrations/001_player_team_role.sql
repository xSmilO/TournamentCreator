-- Run once against the same PostgreSQL database configured in .env.
-- Existing players keep their current teams and receive an unassigned role.
BEGIN;
ALTER TABLE winit.players ADD COLUMN IF NOT EXISTS team_role VARCHAR(50);
COMMENT ON COLUMN winit.players.team_role IS 'Role in the current team, e.g. IGL, AWPer, Support. NULL means unassigned.';
COMMIT;
