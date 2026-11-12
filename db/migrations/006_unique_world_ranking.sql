-- Run this query to identify existing conflicts before applying the constraint.
SELECT world_ranking, array_agg(name ORDER BY name) AS teams
FROM winit.teams
WHERE world_ranking IS NOT NULL
GROUP BY world_ranking
HAVING count(*) > 1
ORDER BY world_ranking;

-- Resolve any conflicts above and rerun this migration.
BEGIN;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'winit.teams'::regclass
          AND conname = 'teams_world_ranking_unique'
    ) THEN
        ALTER TABLE winit.teams ADD CONSTRAINT teams_world_ranking_unique UNIQUE (world_ranking);
    END IF;
END $$;
COMMIT;
