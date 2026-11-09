package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.utils.BackgroundTask;
import com.hltv.tournamentcreator.utils.FxmlViews;
import com.hltv.tournamentcreator.utils.Icons;
import com.hltv.tournamentcreator.utils.ViewManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

public abstract class DatabaseSectionController<T> {
    @FXML protected TableView<T> table;
    @FXML private Label status, emptyCards;
    @FXML private Button refresh;
    @FXML private TextField search;
    @FXML private ComboBox<String> filter;
    @FXML private FlowPane cards;
    private final ObservableList<T> rows = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(rows);
    private boolean loaded;

    protected abstract String sectionName();
    protected abstract List<T> fetchRows() throws Exception;
    protected abstract void configureColumns();
    protected abstract void openCreator();
    protected Node card(T item) { throw new UnsupportedOperationException("No card template for " + sectionName()); }
    protected List<String> filterOptions() { return List.of("All entries"); }
    protected boolean matchesFilter(T item, String filter) { return true; }
    @FXML public final void createEntry() { openCreator(); }

    @FXML public void initialize() {
        if (ViewManager.getInstance() != null) search.textProperty().bindBidirectional(ViewManager.getInstance().searchTextProperty());
        filter.getItems().setAll(filterOptions());
        filter.getSelectionModel().selectFirst();
        if (table != null) {
            configureColumns();
            SortedList<T> sorted = new SortedList<>(filtered);
            sorted.comparatorProperty().bind(table.comparatorProperty());
            table.setItems(sorted);
        }
        search.textProperty().addListener((obs, old, value) -> applyFilter());
        filter.valueProperty().addListener((obs, old, value) -> applyFilter());
        loadData();
    }

    private void applyFilter() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(item -> item.toString().toLowerCase(Locale.ROOT).contains(query)
                && matchesFilter(item, filter.getValue()));
        if (cards != null) {
            cards.getChildren().setAll(filtered.stream().map(item -> {
                Node node = card(item);
                if (node instanceof Region region) region.prefWidthProperty().bind(cards.widthProperty().subtract(54).divide(2));
                return node;
            }).toList());
            emptyCards.setVisible(loaded && filtered.isEmpty());
            emptyCards.setManaged(emptyCards.isVisible());
        }
        if (loaded) status.setText(filtered.size() + " of " + rows.size() + " " + sectionName().toLowerCase());
    }

    public void dispose() {
        if (ViewManager.getInstance() != null) search.textProperty().unbindBidirectional(ViewManager.getInstance().searchTextProperty());
    }

    @SuppressWarnings("unchecked")
    private <V> TableColumn<T, V> findColumn(String title) {
        return (TableColumn<T, V>) table.getColumns().stream().filter(col -> title.equals(col.getText()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing FXML column: " + title));
    }

    protected <V> void column(String title, Function<T, V> value) {
        this.<V>findColumn(title).setCellValueFactory(row -> new SimpleObjectProperty<>(value.apply(row.getValue())));
    }

    protected void textColumn(String title, Function<T, String> value) {
        column(title, value);
        this.<String>findColumn(title).setComparator(Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    protected void identityColumn(String title, Function<T, String> name, Function<T, String> subtitle, String icon) {
        TableColumn<T, T> column = findColumn(title);
        column.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue()));
        column.setComparator(Comparator.comparing(name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        column.setCellFactory(col -> new TableCell<>() {
            private final FxmlViews.Fragment cell = FxmlViews.load("components/IdentityCell.fxml");
            private final StackPane mark = cell.get("mark");
            { mark.getChildren().setAll(Icons.of(icon)); }
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                mark.getStyleClass().removeIf(style -> style.startsWith("mark-"));
                mark.getStyleClass().add("mark-" + Math.floorMod(name.apply(item).hashCode(), 4));
                cell.<Label>get("name").setText(name.apply(item));
                cell.<Label>get("subtitle").setText(subtitle.apply(item));
                setGraphic(cell.root());
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected void actionColumn(String caption, java.util.function.Consumer<T> action) {
        TableColumn<T, T> column = (TableColumn<T, T>) table.getColumns().stream()
                .filter(col -> caption.equals(col.getUserData())).findFirst().orElseThrow();
        column.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue()));
        column.setCellFactory(view -> new TableCell<>() {
            private final Button button = (Button) FxmlViews.load("components/ActionCell.fxml").root();
            {
                button.setText(caption);
                button.setOnAction(event -> { if (getItem() != null) action.accept(getItem()); });
            }
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : button);
            }
        });
    }

    @FXML protected void loadData() {
        refresh.setDisable(true);
        status.setText("Loading " + sectionName().toLowerCase() + "…");
        BackgroundTask.run(this::fetchRows, values -> {
            rows.setAll(values);
            loaded = true;
            applyFilter();
            refresh.setDisable(false);
        }, error -> {
            status.setText("Could not load " + sectionName().toLowerCase() + ". Check the database connection and try Refresh.");
            refresh.setDisable(false);
        });
    }
}
