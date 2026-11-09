-- Run manually after migrations 001-004. Back up the database first.
-- Existing results are preserved; dates never imply that an event is completed.
BEGIN;
ALTER TABLE winit.tournaments ADD COLUMN IF NOT EXISTS lifecycle_state VARCHAR(20);
UPDATE winit.tournaments t SET lifecycle_state = CASE
    WHEN EXISTS (SELECT 1 FROM winit.matches m WHERE m.tournament_id=t.tournament_id
                 AND (m.status='Live' OR (m.status='Completed' AND m.team1_id IS NOT NULL AND m.team2_id IS NOT NULL))) THEN 'In progress'
    WHEN EXISTS (SELECT 1 FROM winit.matches m WHERE m.tournament_id=t.tournament_id) THEN 'Draft'
    WHEN EXISTS (SELECT 1 FROM winit.tournament_participants p WHERE p.tournament_id=t.tournament_id) THEN 'Registration'
    ELSE 'Draft' END
WHERE lifecycle_state IS NULL;
ALTER TABLE winit.tournaments ALTER COLUMN lifecycle_state SET DEFAULT 'Draft';
ALTER TABLE winit.tournaments ALTER COLUMN lifecycle_state SET NOT NULL;
ALTER TABLE winit.tournaments ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE winit.matches ADD COLUMN IF NOT EXISTS outcome_type VARCHAR(20);
UPDATE winit.matches SET outcome_type=CASE
    WHEN status='Completed' AND winner_id IS NOT NULL AND (team1_id IS NULL OR team2_id IS NULL) THEN 'Bye'
    ELSE 'Played' END WHERE outcome_type IS NULL;
ALTER TABLE winit.matches ALTER COLUMN outcome_type SET DEFAULT 'Played';
ALTER TABLE winit.matches ALTER COLUMN outcome_type SET NOT NULL;
ALTER TABLE winit.matches ADD COLUMN IF NOT EXISTS outcome_reason VARCHAR(500);
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='winit.tournaments'::regclass AND conname='tournaments_lifecycle_check') THEN
  ALTER TABLE winit.tournaments ADD CONSTRAINT tournaments_lifecycle_check
    CHECK (lifecycle_state IN ('Draft','Registration','In progress','Completed','Cancelled'));
 END IF;
 IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='winit.matches'::regclass AND conname='matches_outcome_check') THEN
  ALTER TABLE winit.matches ADD CONSTRAINT matches_outcome_check CHECK
    (outcome_type IN ('Played','Bye','Forfeit','Withdrawal','Cancelled'));
 END IF;
END $$;
CREATE TABLE IF NOT EXISTS winit.tournament_audit (
 audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 tournament_id UUID NOT NULL REFERENCES winit.tournaments(tournament_id) ON DELETE CASCADE,
 -- Intentionally no match FK: audit records survive a pre-start draw reset.
 match_id UUID,
 action VARCHAR(50) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 before_details TEXT,
 after_details TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 database_actor TEXT NOT NULL DEFAULT CURRENT_USER
);
CREATE INDEX IF NOT EXISTS tournament_audit_event_idx ON winit.tournament_audit(tournament_id,created_at);
COMMIT;

-- Validation (read only): inspect states and outcomes after migration.
SELECT lifecycle_state, COUNT(*) FROM winit.tournaments GROUP BY lifecycle_state;
SELECT outcome_type, COUNT(*) FROM winit.matches GROUP BY outcome_type;
-- Recovery: restore your backup if rolling back the release. Do not drop these
-- columns after using the new app: doing so loses lifecycle and outcome history.
