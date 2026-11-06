-- Requires the original winit schema (and 002_tournament_team_limit.sql for the app).
-- Existing matches keep group_id = NULL and are preserved.
BEGIN;
CREATE TABLE IF NOT EXISTS winit.tournament_groups (
    group_id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES winit.tournaments(tournament_id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    group_order INTEGER NOT NULL CHECK (group_order > 0),
    UNIQUE (tournament_id, group_order),
    UNIQUE (group_id, tournament_id)
);
CREATE TABLE IF NOT EXISTS winit.tournament_group_teams (
    group_id UUID NOT NULL,
    tournament_id UUID NOT NULL,
    team_id UUID NOT NULL,
    group_seed INTEGER NOT NULL CHECK (group_seed BETWEEN 1 AND 4),
    PRIMARY KEY (tournament_id, team_id),
    UNIQUE (group_id, group_seed),
    FOREIGN KEY (group_id, tournament_id)
        REFERENCES winit.tournament_groups(group_id, tournament_id) ON DELETE CASCADE,
    FOREIGN KEY (tournament_id, team_id)
        REFERENCES winit.tournament_participants(tournament_id, team_id) ON DELETE CASCADE
);
ALTER TABLE winit.matches ADD COLUMN IF NOT EXISTS group_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'matches_group_tournament_fkey'
                   AND conrelid = 'winit.matches'::regclass) THEN
        ALTER TABLE winit.matches ADD CONSTRAINT matches_group_tournament_fkey
            FOREIGN KEY (group_id, tournament_id)
            REFERENCES winit.tournament_groups(group_id, tournament_id) ON DELETE CASCADE;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS matches_group_id_idx ON winit.matches(group_id);
COMMIT;
