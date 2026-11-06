-- Adds a required capacity to tournaments. Existing registrations are preserved.
BEGIN;
ALTER TABLE winit.tournaments ADD COLUMN IF NOT EXISTS team_limit INTEGER;
UPDATE winit.tournaments t
SET team_limit = GREATEST(16, (SELECT COUNT(*) FROM winit.tournament_participants p
                            WHERE p.tournament_id = t.tournament_id))
WHERE team_limit IS NULL;
ALTER TABLE winit.tournaments ALTER COLUMN team_limit SET DEFAULT 16;
ALTER TABLE winit.tournaments ALTER COLUMN team_limit SET NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'tournaments_team_limit_check'
                     AND conrelid = 'winit.tournaments'::regclass) THEN
        ALTER TABLE winit.tournaments ADD CONSTRAINT tournaments_team_limit_check CHECK (team_limit >= 2);
    END IF;
END $$;
COMMIT;
