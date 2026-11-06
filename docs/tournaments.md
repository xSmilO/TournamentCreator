# Teams, matches, results, and brackets

Open **Tournaments → Manage tournament** on a tournament card.

## Register teams

1. Open the **Teams** tab and choose **Add teams**.
2. Search for a team and select it. Use Ctrl/Shift to select several teams.
3. Choose **Register selected teams**.

Already registered teams are excluded from the picker. New participants receive consecutive seeds after the existing highest seed. Register everyone before generating a bracket: registration closes once the bracket exists.

Every tournament has a required **team limit** (2–256 teams, default 16). Choose it during creation or change it under **Teams → Team limit → Save limit**. The limit cannot be reduced below the registered count. Cards and the management dialog display the registered count against capacity, and registration stops when the limit is reached. Multi-team registrations are rejected in full if they would exceed capacity.

The team limit is a maximum, not a requirement to fill every slot: generating a bracket with fewer registered teams still works, using byes where necessary.

## Generate a single elimination bracket

1. Open the **Bracket** tab.
2. Choose **Bo1**, **Bo3**, or **Bo5**. This format applies to the bracket's matches.
3. Choose **Generate bracket**.

Teams are ordered by seed. The bracket uses the next power of two and gives byes to the highest seeds where necessary. Bye winners are advanced immediately. The bracket and its links are saved in PostgreSQL and restored when you reopen the tournament.

Generate is available for tournaments with at least two participants and no existing matches. Existing matches are preserved; generation does not replace them. The first implementation supports single elimination, with up to 256 participants.

## Enter map scores

Click a playable bracket match, or select it in **Matches** and choose **Enter / edit scores**.

- Enter map names and each team's round scores, such as `13 : 10`.
- Use **Live** for an unfinished map and **Completed** for a finished map.
- Completed maps require different, nonnegative round scores.
- Leave unplayed maps **Upcoming** with `0 : 0`; their map names are optional.
- Finish maps in order. A Bo1 needs one map win, a Bo3 needs two, and a Bo5 needs three. Further maps cannot be played once the series is won.

**Save results** saves the maps, calculates the series status/winner, and advances the winner in one transaction. A completed final displays the champion. Editing a completed result updates the next slot only while the next match is still unplayed. If it has started, correct the later match first. An editor opened before another result update must be reopened to avoid overwriting newer results.

Map round scores are entered manually. The application calculates map/series winners; it does not ingest a game server's live scoreboard or enforce a specific game's regulation/overtime round limits.

## Create individual matches

For a tournament without a generated bracket, open **Matches → Add match**. Choose two registered opponents, a Bo1/Bo3/Bo5 format, and optionally a date/time, stage, and round. Dates and times must either both be provided or both be empty. Enter times in the local 24-hour format, such as `18:30`.

## Database storage

| Table | Data |
| --- | --- |
| `winit.tournament_participants` | Tournament/team registration and seed number |
| `winit.tournaments.team_limit` | Maximum registered teams |
| `winit.matches` | Opponents, format, schedule, round, status, and series winner |
| `winit.matches.next_match_id` / `next_match_slot` | The destination for the winner in the next bracket round |
| `winit.games` | Map names/order, round scores, map statuses, and map winners |

The series score is calculated from completed map wins. Match scoring and bracket links use the existing tables. Team limits require applying `db/migrations/002_tournament_team_limit.sql`: it adds the required `team_limit` column, backfills existing tournaments with 16 or their registered count if higher, and preserves all current registrations. The application checks registration capacity while holding a tournament row lock, so concurrent registrations cannot claim the same last slot. Bracket byes are completed matches with one participant and a winner, and are excluded from the dashboard's played-match count.

## Editing the views

Layouts are in `TournamentManager.fxml`, `TournamentTeamsModal.fxml`, `MatchCreatorModal.fxml`, and `MatchScoresModal.fxml`. Repeated map rows and bracket cards/rounds/connectors are FXML templates in `components/`. Styling is in `css/creator.css`; data and actions are in `TournamentManagerController`, `TournamentDAO`, and `MatchDAO`.
