-- Map catalogue shared by all matches. A series has one games row per map.
BEGIN;
CREATE TABLE IF NOT EXISTS winit.maps (
    map_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE CHECK (btrim(name) <> '')
);
INSERT INTO winit.maps (name)
VALUES ('Mirage'), ('Inferno'), ('Nuke'), ('Ancient'), ('Anubis'), ('Dust2'), ('Vertigo'), ('Overpass'), ('Train')
ON CONFLICT (name) DO NOTHING;
-- Preserve names already used in saved results.
INSERT INTO winit.maps (name)
SELECT DISTINCT map_name FROM winit.games WHERE map_name IS NOT NULL AND btrim(map_name) <> ''
ON CONFLICT (name) DO NOTHING;
ALTER TABLE winit.games ADD COLUMN IF NOT EXISTS map_id UUID REFERENCES winit.maps(map_id);
UPDATE winit.games g SET map_id=m.map_id FROM winit.maps m
WHERE g.map_id IS NULL AND g.map_name=m.name;
-- Legacy unnamed/upcoming maps may remain NULL until a map is selected.
CREATE INDEX IF NOT EXISTS games_map_id_idx ON winit.games(map_id);
COMMIT;
