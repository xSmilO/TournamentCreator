# Current lifecycle workflow (migration 005)

Apply `db/migrations/005_tournament_lifecycle.sql` manually after 001–004. It
preserves results and backfills state from actual competition activity, not dates.
No migration has been run automatically.

1. Create the event; open **Manage tournament → Event control → Open registration**.
2. Register teams and draw groups/a bracket. Drawing closes registration.
3. Choose **Start tournament**, then enter scores under Matches or Bracket.
4. For groups, finish group fixtures and generate playoffs as before.
5. Choose **Finish tournament** to validate all results and save final standings.
6. Browse standings/history in Event control; archive completed/cancelled events.

Routine lifecycle actions need no written note. Cancellation, reopening, draw
reset, completed-score corrections and awarded outcomes require a reason.
Reopening clears placements until the event is finished again. Reset draw removes
all fixtures/groups/schedules, retaining registered teams and seeds; it is allowed
only before starting and before any played or awarded result (byes are allowed).

**Matches → Outcome / reset** supports forfeits and match withdrawals with an
awarded winner and no fabricated map scores. Group awards earn 3 points, zero map
wins. A withdrawal here applies to one match, not the entire event. Manual matches
can be cancelled without a winner; group/bracket fixtures need a winner. Reset a
result before replacing an awarded outcome or discarding played maps. Later played
matches block changes to earlier winners; group results lock after playoffs.

Final standings require a generated single-elimination final, including playoffs.
Semifinal losers share place 3, quarterfinal losers place 5, and so on. Group-stage
eliminations share the first place below the number of playoff qualifiers.
Event control shows the latest 100 audited actions, with reasons and before/after
details. The database retains all audit rows. Routine entry needs no player stats.

**The lifecycle rules above supersede the date-based registration rules in the
original workflow notes below.** Dates now describe the schedule; explicit state
controls registration and scoring. No date automatically completes an event.

# Teams, matches, results, and brackets

Open **Tournaments → Manage tournament** on a tournament card.

## Register teams

1. Open the **Teams** tab and choose **Add teams**.
2. Search for a team and select it. Use Ctrl/Shift to select several teams.
3. Choose **Register selected teams**.

Already registered teams are excluded from the picker. New participants receive consecutive seeds after the existing highest seed. Register everyone before drawing groups or generating a bracket: registration closes once either exists.

Every tournament has a required **team limit** (2–256 teams, default 16). Choose it during creation or change it under **Teams → Team limit → Save limit**. The limit cannot be reduced below the registered count. Cards and the management dialog display the registered count against capacity, and registration stops when the limit is reached. Multi-team registrations are rejected in full if they would exceed capacity.

The team limit is a maximum, not a requirement to fill every slot: generating a bracket with fewer registered teams still works, using byes where necessary.

## Generate a single elimination bracket

1. Open the **Bracket** tab.
2. Choose **Bo1**, **Bo3**, or **Bo5**. This format applies to the bracket's matches.
3. Choose **Generate bracket**.

Random draw is selected by default and shuffles teams before generating the bracket, including allocation of byes. Uncheck Random draw to order teams by seed. In seeded mode, The bracket uses the next power of two and gives byes to the highest seeds where necessary. Bye winners are advanced immediately. The bracket and its links are saved in PostgreSQL and restored when you reopen the tournament.

Generate is available for tournaments with at least two participants and no existing matches. Existing matches are preserved; generation does not replace them. Single elimination supports up to 256 participants. To use groups before playoffs instead, follow the group-stage workflow below.

## Groups → Playoffs

1. Register 4–256 teams in multiples of four, such as 8, 12, 16, or 256.
2. Open **Groups**, choose the group match format (Bo1/Bo3/Bo5), and click **Draw groups**.
3. The app creates groups of four using a snake-seeded draw. Every team appears in one group and plays each opponent once: three rounds, six matches per group.
4. Expand a group to see standings and its matches. Click a match and enter map scores using the normal result editor. Standings update after saving, and the editor returns to the tab you were using.
5. Once all six matches in every group are complete, choose the playoff match format and click **Generate playoffs**. The top two teams from each group enter a single elimination bracket. Winners get higher seeds, and each group's winner and runner-up are placed in opposite halves so they can only meet again in the final.
6. Manage playoff scores in **Bracket** or **Matches** as usual. Group results lock once playoffs are generated, preventing later changes from invalidating the qualified teams.

For example, 256 teams produce 64 groups, 384 group matches, and 128 playoff qualifiers. Non-power-of-two qualifier counts receive bracket byes. A single four-team group is supported; its top two meet in the final.

Standings use **3 points per match win**, **0 per loss**, and completed matches only. Ties are broken by:

1. Head-to-head points from matches between all teams tied on overall points (a mini-league for multi-team ties).
2. Overall map difference.
3. Overall maps won.
4. Original tournament seed, with team ID as a final stable fallback if seeds are equal/missing.

Green rows mark the current top two. Before all group matches finish, those positions are provisional. Incomplete, duplicate, or inconsistent group results cannot be used to generate playoffs. Groups and playoff generation are transactional and cannot be repeated over existing competition matches.

## Enter map scores

Apply `db/migrations/004_maps.sql` before using the score editor. It creates
`winit.maps(map_id, name)` and links each played map through `games.map_id`.
The relationship is `matches.match_id → games.match_id`, then
`games.map_id → maps.map_id`, allowing Bo3/Bo5 series to use multiple maps.
Existing map names are imported and linked without changing results. The editor
offers a map dropdown. Additional names can be added to `winit.maps`.

## Changing tournament participants

In **Teams**, select a registered team and choose **Remove selected team** to
unregister it; the team itself remains in the team directory. Adding and removing
teams close on the start date, after the end date, once a match is Live/Completed,
or once groups/a bracket exist. These checks also run inside the write transaction,
so an old open window cannot bypass the restriction. A team referenced by a manual
scheduled match cannot be removed. Remaining teams keep their seeds.

## Score editor

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
| `winit.tournament_groups` | Persistent group IDs, names, and group order |
| `winit.tournament_group_teams` | Group membership linked to registered tournament teams |
| `winit.matches.group_id` | Identifies group-stage fixtures; NULL for manual/playoff matches |
| `winit.matches` | Opponents, format, schedule, round, status, and series winner |
| `winit.matches.next_match_id` / `next_match_slot` | The destination for the winner in the next bracket round |
| `winit.games` | Map names/order, round scores, map statuses, and map winners |

The series score is calculated from completed map wins. Match scoring and bracket links use the existing tables. Team limits require applying `db/migrations/002_tournament_team_limit.sql`: it adds the required `team_limit` column, backfills existing tournaments with 16 or their registered count if higher, and preserves all current registrations. The application checks registration capacity while holding a tournament row lock, so concurrent registrations cannot claim the same last slot. Bracket byes are completed matches with one participant and a winner, and are excluded from the dashboard's played-match count.

Group mechanics additionally require **`db/migrations/003_tournament_groups.sql`**. It adds the group and membership tables plus the nullable match group link. Existing registrations, matches, and map scores are preserved. Standings are calculated from saved results rather than stored as a second copy of the scores. All competition writers lock the tournament row, so qualification uses a consistent set of group results and concurrent generation/saves cannot create partial or duplicate stages. Migration files are provided for you to run; the app does not automatically change the live database schema.

## Editing the views

Layouts are in `TournamentManager.fxml`, `TournamentTeamsModal.fxml`, `MatchCreatorModal.fxml`, and `MatchScoresModal.fxml`. Repeated map rows, group panels/match rows, and bracket cards/rounds/connectors are FXML templates in `components/`. Styling is in `css/creator.css`; data and actions are in `TournamentManagerController`, `TournamentDAO`, `MatchDAO`, and `GroupStageDAO`.

## Organizer editing

Milestone 1 uses the existing schema and migrations 001–004; no additional SQL is required.

- **Players → Edit:** change nickname, personal details, country, or birth date.
  Optional details can be cleared. Unknown rating/headshot statistics display as
  “—”; a real stored zero remains zero.
- **Teams → Edit:** change the name or manually managed world ranking. Clear the
  ranking to mark the team unranked.
- **Teams → View roster:** select a player to change their role, transfer them to
  another team with a new role, or release them to free agency. Release requires
  confirmation in its form and clears the team and role together. Transfers update
  team and role atomically. Historical rosters are not recorded yet.
- **Tournament card → Edit tournament:** update name, tier, dates, prize pool,
  location, and format description. Dates and format lock once the start date is
  reached, the end date has passed, groups/a bracket exist, or a match is Live or
  Completed. Other metadata remains editable. The format description does not
  regenerate fixtures or change existing match formats. There is no separate
  tournament description column in the current schema.
- **Manage tournament → Teams → select team → Edit seed:** enter a positive unused
  seed. To swap seeds, temporarily move one team to an unused number. Seeds lock
  after a draw or started match. Registration and seeding use the same tournament
  lock to prevent concurrent changes from assigning duplicate seeds.
- **Manage tournament → Matches → select match → Schedule:** set the local date
  and 24-hour time for any Upcoming manual/group/bracket fixture, including TBD
  opponents. Clear both fields to remove a schedule. Live and Completed fixtures
  cannot be rescheduled through this editor.

Each form shows a saved confirmation; **Done** returns to the refreshed originating
view. **Cancel** before saving discards changes. If another edit changed the original
values, the save is rejected with a refresh instruction instead of overwriting it.
Scores, completed results, tournament capacity, and unrelated profile fields are
not changed by these edits.

## Delete a tournament

Choose **Delete tournament** on its card, then type its exact name and choose
**Delete permanently**. This removes the event and its registrations, groups,
matches, map scores, placements and audit history using existing cascading foreign
keys. Teams, players and the map catalogue remain. Deletion is available for every
event state, including archived events. Use Archive instead if you want to retain
the results. No new database migration is needed.
