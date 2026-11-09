# Tournament Creator

A JavaFX desktop application for managing esports players, teams, tournaments,
group stages, single-elimination brackets and Bo1/Bo3/Bo5 match results.

## Requirements

- JDK 22 or newer; the Maven compiler uses `release 22`.
- PostgreSQL with the existing `winit` schema.
- Internet access on the first build to download Maven and dependencies.

The Maven Wrapper is included, so a separate Maven installation is unnecessary.
Set `JAVA_HOME` to your JDK installation and run commands from the repository root.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-22'
Copy-Item .env.sample .env
```

Edit `.env` with your database settings:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/tournamentcreator
DB_USER=tournament_app
DB_PASS=replace_with_your_password
```

These values may also be supplied as environment variables, which take precedence
over `.env`. The `.env` file is optional when all three variables are set, and is
ignored by Git. Missing or blank settings produce a database configuration error.
Connections default to a 10-second connection timeout and a 60-second socket read
timeout. PostgreSQL JDBC URL parameters can override those driver defaults.

## Database setup

**The original `winit` schema is not included in this repository.** Obtain an
existing database or its schema export before applying these incremental migrations.
They do not create a complete database from scratch.

Apply the migrations in order to the database configured above:

```powershell
psql -h localhost -U tournament_app -d tournamentcreator -v ON_ERROR_STOP=1 -f db/migrations/001_player_team_role.sql
psql -h localhost -U tournament_app -d tournamentcreator -v ON_ERROR_STOP=1 -f db/migrations/002_tournament_team_limit.sql
psql -h localhost -U tournament_app -d tournamentcreator -v ON_ERROR_STOP=1 -f db/migrations/003_tournament_groups.sql
psql -h localhost -U tournament_app -d tournamentcreator -v ON_ERROR_STOP=1 -f db/migrations/004_maps.sql
```

Use a database owner or migration account if the application account cannot alter
tables. The app does not automatically execute migrations.

Apply `db/migrations/005_tournament_lifecycle.sql` after 004 to enable event states, outcomes, and audit history.

## Run and test

```powershell
.\mvnw.cmd test
.\mvnw.cmd javafx:run
```

On Linux/macOS use `./mvnw` instead. The current tests use pure Java logic and JDBC
test doubles; they do not require or write to a live database. They do not validate
the actual PostgreSQL schema, SQL execution or concurrent database sessions.

## Project layout

- `src/main/java`: controllers, models, JDBC DAOs and competition algorithms.
- `src/main/resources`: FXML views, CSS and assets.
- `src/test/java`: validation, competition logic and persistence tests.
- `db/migrations`: incremental PostgreSQL migrations.
- [Tournament workflows](docs/tournaments.md): registration, groups, brackets and scoring.
- [Product roadmap](docs/roadmap.md): current completeness, priorities, and proposed database work.
- [JavaFX view guide](src/main/resources/com/hltv/tournamentcreator/README.md).
- `Tournament Management App (design)`: separate React/Vite UI prototype with
  sample data; the JavaFX application does not depend on it.
