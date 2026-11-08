# Organizer UI smoke check

Run `OrganizerUiSmoke.java` after `mvnw test`. It uses fake DAOs for all reads and
writes, creates an offscreen JavaFX scene, and verifies all seven editor modes,
prefilled values, validation, cancellation, saves, and return callbacks. It writes
preview PNGs into ignored `target/`. It needs an environment capable of starting
the JavaFX toolkit but does not open an application window or contact PostgreSQL.

PowerShell (use the same JDK as Maven):

```powershell
.\mvnw.cmd test -B -q
[xml]$report = Get-Content 'target/surefire-reports/TEST-com.hltv.tournamentcreator.utils.CreatorValidationTest.xml'
$cp = ($report.testsuite.properties.property | Where-Object name -eq 'java.class.path').value
$mp = ($report.testsuite.properties.property | Where-Object name -eq 'jdk.module.path').value
& "$env:JAVA_HOME\bin\java.exe" --class-path "$cp;$mp" src/test/manual/OrganizerUiSmoke.java
```

The ordinary Maven suite includes `OrganizerPersistenceTest`: stale edits, transfer
and release semantics, tournament edit restrictions, duplicate/locked seeds,
schedule guards/rollback, and nullable numeric mapping. JDBC doubles validate
application behavior; real PostgreSQL integration testing remains roadmap work.
