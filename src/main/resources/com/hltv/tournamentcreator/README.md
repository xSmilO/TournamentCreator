# Editing the JavaFX UI

Open the FXML files in Scene Builder to edit layouts, controls, table columns, spacing, and button text. Colors, fonts, and shared view sizing live in `css/app.css` and `css/creator.css`.

| View | Layout | Java controller |
| --- | --- | --- |
| Application shell | MainScreen-view.fxml | MainController |
| Dashboard | OverviewSection.fxml | OverviewController |
| Directories | TeamsSection.fxml, PlayersSection.fxml, TournamentsSection.fxml | Respective section controllers, with shared filtering in DatabaseSectionController |
| Creators | TeamCreatorModal.fxml, TournamentCreatorModal.fxml, PlayerCreatorModal.fxml | Respective creator controllers; team/tournament share CreatorController |
| Team roster | TeamRosterModal.fxml | TeamRosterDialog |
| Player selection | AvailablePlayersModal.fxml | TeamRosterDialog |
| Role prompt | PlayerRoleModal.fxml | TeamRosterDialog |
| Tournament management | TournamentManager.fxml, TournamentTeamsModal.fxml | TournamentManagerController |
| Matches and map scores | MatchCreatorModal.fxml, MatchScoresModal.fxml | TournamentManagerController |
| Bracket rounds, matches, and connectors | components/Bracket*.fxml | TournamentManagerController |
| Group standings and fixtures | components/GroupPanel.fxml, components/GroupMatchRow.fxml | TournamentManagerController |
| Tournament participants | DetailModal.fxml | DetailDialog |
| Repeated cards and rows | components/*.fxml | Populated by the owning controller |

The three roster views share an existing TeamRosterDialog instance so they can retain the current team and player search. Their controller is supplied by FxmlViews rather than an fx:controller attribute. They can still be opened and edited visually in Scene Builder.

Keep existing fx:id values and #action names when changing layouts: controllers use them to bind data and handle actions. Table cell factories remain in Java to work with JavaFX's virtualized rows; their visible content comes from components/IdentityCell.fxml and components/ActionCell.fxml. Dynamically repeated choices, cards, ranking rows, and match rows also load FXML templates.

Database queries, input validation, search predicates, selection state, and asynchronous saves belong in Java. This UI refactor does not require a database migration.
