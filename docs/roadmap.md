# Tournament Creator roadmap

Reviewed against the repository on 2026-11-08.

## Product goal

Create tournaments, register teams, enter scores, and retain event/results history.
Keep routine data entry minimal. Detailed player statistics, historical lineups,
rankings, vetoes, and public publishing are optional future ideas, not requirements
for a complete app. Existing later milestones below are a deferred idea backlog.

Milestone 2 implementation is complete; apply migration 005 manually before using
it. Automated checks use JDBC doubles and offscreen JavaFX; live PostgreSQL has
not been validated or modified.

## Current completeness

The app has a working core competition flow, but is not yet a complete organizer.
A percentage would be misleading without an agreed release scope.

| Area | Implemented | Main gap |
| --- | --- | --- |
| Players | Creation, profile editing, country, search, filters, nullable stats display | Historical stats, player detail view |
| Teams | Creation/editing, free-agent assignment, roster/role editing, release/transfer | Roster history |
| Tournaments | Creation/metadata editing, capacity 2–256, registration/removal, seed editing | Explicit lifecycle, final standings |
| Single elimination | Seeded/random draw, byes, persisted bracket, winner advancement, fixture scheduling | Controlled corrections/reset |
| Groups | Four teams/group, round robin, standings, top two to playoffs | Configurable rules and group sizes |
| Results | Bo1/Bo3/Bo5, map catalogue, map scores, series winner | Forfeits, cancellations, vetoes, player statistics |
| Dashboard | Counts, upcoming/live fixtures, stored team rankings | Full match/results directory and navigable detail pages |
| Persistence | Transactions, tournament locks, stale score/role checks | Complete baseline schema, real database integration tests |
| Delivery | Maven wrapper, configuration guide, JavaFX runtime build settings | Verified installer, upgrade/backup/restore workflow |

### Findings that affect priorities

- Tournament cards derive status from dates (`TournamentSectionController.state`).
  A tournament can appear completed while its matches remain unfinished.
- Milestone 1 now supplies profile/team/tournament editing, player release and
  transfers, seed edits, and upcoming-match scheduling.
- A player's current `team_id` and role do not preserve their historical lineup.
  Future match statistics must not attribute old results using today's roster.
- The dashboard orders existing `world_ranking` values; it does not calculate
  rankings. Player rating/headshot fields are stored values, not aggregates from
  played maps. Missing numeric statistics now remain NULL and display as “—”;
  genuine stored zero values remain zero.
- Generated and manual upcoming matches now have a schedule editor. Times still
  use local date/time; explicit timezone handling remains future work.
- Group size, qualifiers, points, and tie-break order are fixed. The group seed
  constraint in migration 003 also limits each group to four teams.
- The repository contains incremental migrations 001–004, but not a complete
  original schema. A new developer cannot create the whole database from these alone.
- Existing tests do not execute SQL against PostgreSQL or prove concurrency behavior
  with multiple real connections.

## Milestone 1 — Complete everyday organizer actions

Status: **completed on 2026-11-08** using the current schema. No new SQL migration required.

- [x] Edit player nickname, personal details, country, and birth date.
- [x] Edit team name and manually managed ranking.
- [x] Release a player to free agency; clear their current team role atomically.
- [x] Transfer a player and choose the destination role in one transaction.
- [x] Edit tournament name, dates, tier, location, prize pool, and description/format
  where supported. Define which changes are allowed after competition starts.
- [x] Edit seeds before drawing groups/brackets; reject duplicate seeds.
- [x] Edit scheduled dates/times for generated and manual matches.
- [x] Show missing statistics as “—”, not zero.
- [x] Add stale-edit protection and clear success/error feedback for these actions.

Acceptance: an organizer can correct setup mistakes and prepare a full event
without opening a SQL client. Changes made in another window cannot be silently
overwritten. Existing completed results remain intact.

Delivered: FXML organizer editor, conditional updates for stale-edit protection,
atomic team/role changes, tournament-serialized seed/schedule edits, 22 new
automated regression tests, and a reusable offscreen UI smoke check in
`src/test/manual/OrganizerUiSmoke.java`. Detailed usage and restrictions are in
[tournament workflows](tournaments.md#organizer-editing).

Database impact: basic edits reuse existing columns. Add roster history in
milestone 3 before treating transfers as historically accurate. Do not introduce
hard deletion of teams/players referenced by results.

## Milestone 2 — Tournament lifecycle and final results

Status: **completed on 2026-11-09**. Requires `005_tournament_lifecycle.sql`.

Withdrawals are match-level awarded outcomes; whole-event withdrawal automation
is deferred. Cancellation without a winner applies to manual matches; managed
group/bracket fixtures require a winner or cancellation of the event.

- [x] Persist tournament states: Draft, Registration, In progress, Completed,
  Cancelled; support archiving separately from competition state.
- [x] Add explicit registration open/close and start/finish actions with validation.
- [x] Display schedule-derived labels separately from actual competition state.
- [x] On finish, require all relevant matches resolved and a valid champion.
- [x] Produce final standings and persist placements atomically with completion.
  Decide how shared places work (for example, both semifinal losers share 3rd–4th).
- [x] Support forfeit, withdrawal, and cancelled match outcomes without inventing
  map scores. Distinguish these from automatic byes.
- [x] Provide a controlled correction workflow: explain affected downstream games,
  block incompatible edits, and record the reason for an override.
- [x] Allow redraw/reset only under explicit rules, initially before any game starts.

Acceptance: complete a group-to-playoff event, reopen it, and see the same champion,
placements, and state. An expired date alone never completes a tournament. A
forfeit advances the winner without polluting map statistics.

Database impact: lifecycle columns, explicit match outcome type/reason, audit
records; reuse `tournament_participants.placement` if the existing schema supports
the agreed shared-placement representation.

## Milestone 3 — Historical rosters and match identity

Priority: required before trustworthy player statistics; can overlap milestone 2.

- [ ] Store team membership periods and role changes.
- [ ] Register event lineups, including starters, substitutes, and coaches.
- [ ] Snapshot the actual lineup for each match, supporting stand-ins.
- [ ] Validate lineup eligibility and configurable roster limits when starting a match.
- [ ] Preserve past lineups when a player changes team or role.

Acceptance: transfer a player after an event; the old event and match pages still
show the lineup that actually played. A role such as IGL/AWPer remains separate
from lineup status such as starter/substitute.

Database impact: proposed `team_memberships`, `tournament_roster_players`, and
`match_lineup_players` tables. Use foreign keys and uniqueness rules to prevent a
player being assigned to both opponents in the same match. Backfill only known
current memberships; do not invent historical join dates.

## Milestone 4 — HLTV-style browsing and statistics

Priority: after organizer actions and historical identity are reliable.

- [ ] Dedicated match page: event/stage, opponents, schedule, series and map results,
  lineups, and links to teams/players.
- [ ] Full matches/results directory with event, team, date, and status filters.
- [ ] Team pages with roster, recent results, event finishes, and map records.
- [ ] Player pages with biography, roster history, and filtered statistics.
- [ ] Tournament overview with schedule, groups, bracket, final standings, and prizes.
- [ ] Enter/import per-map player kills, deaths, assists, damage, and rounds played.
- [ ] Calculate documented statistics from those records; display sample sizes.
- [ ] Add logos/photos and consistent missing-image states.
- [ ] Add ranking history only after choosing a documented ranking method. Label
  any custom rating clearly; existing fields do not establish HLTV-equivalent ratings.

Acceptance: every aggregate can be traced to saved map/player records. Missing
records are not treated as zero performance. Date/event filters recalculate
statistics consistently and exclude byes/forfeits where appropriate.

Database impact: `game_player_stats` linked to games and historical lineups,
optional media references, later ranking snapshots. Some metrics need additional
round-level data; do not display them until their inputs exist.

## Milestone 5 — Competition configuration and map vetoes

Priority: after one full tournament workflow is stable.

- [ ] Persist stage settings: format, group size, qualifiers, points, tie-breaks,
  match format, and draw method. Lock settings once affected games begin.
- [ ] Support different series formats by round, such as Bo3 playoffs/Bo5 final.
- [ ] Configure each event's map pool from the existing map catalogue.
- [ ] Add ordered pick/ban actions, acting team, decider, and starting-side choices.
- [ ] Extend formats one at a time: configurable round robin, then double
  elimination or Swiss according to organizer demand.
- [ ] Preview a draw before saving; record the actual draw and configuration.

Acceptance: each supported format has documented advancement/tie-break rules and
tests covering odd counts, byes, corrections, and the maximum supported field.

Database impact: explicit tournament stages, stage rules, tournament map pool,
and match veto actions. Double elimination needs loser destinations in addition
to the existing winner links; Swiss needs round-by-round pairing history.

## Milestone 6 — Release readiness and optional publishing

Start reliability work during milestone 1; complete it before distributing the app.

- [ ] Add a complete baseline schema and an explicit migration/version procedure.
- [ ] Test migrations, transactions, locking, rollback, and stale edits against an
  isolated PostgreSQL database, never the user's working database.
- [ ] Automate a smoke flow: create players/teams/event, register, draw, score,
  qualify, finish, reopen, and verify results.
- [ ] Verify 256-team responsiveness, bracket navigation, filtering, and query
  performance; introduce pagination/lazy loading where measurements justify it.
- [ ] Split the large tournament manager into focused controllers/services as
  workflows grow, keeping layout in FXML and presentation in CSS.
- [ ] Package and verify a Windows installer/runtime; document backup and restore.
- [ ] Export schedules, standings, and results to CSV and printable views.
- [ ] Optional later: read-only public site/API, then organizer accounts and access
  control. Keep PostgreSQL credentials on the server for any public client.
- [ ] Optional later: game-server/demo ingestion with deduplication and provenance.

Acceptance: a clean installation can create its database, run an event, survive a
failed save without partial results, and restore a tested backup.

## Database delivery policy

No new migration is required to read or adopt this roadmap. Existing migrations
001–004 remain the prerequisites for implemented features; their application to
your database has not been verified in this review.

For each feature needing schema changes, deliver a separate reviewed SQL file
for you to execute, with prerequisites, data-preserving backfill, validation
queries, and recovery notes. Do not automatically run it against your database.
The table/column ideas above are proposals, not SQL to execute yet: lifecycle,
history, and scoring rules should be implemented together with their migrations.

## Recommended next work package

Focus next on a simple results-history browser: find past tournaments, open their
brackets and final standings, and see saved scores. Improve the create → register
→ play → finish workflow before adding optional data. No detailed player-stat
entry, lineup history, or veto system is required for the core release.
