
package com.studyflow.ui;

import com.studyflow.exception.StorageException;
import com.studyflow.model.Difficulty;
import com.studyflow.model.Subject;
import com.studyflow.model.Task;
import com.studyflow.model.TaskType;
import com.studyflow.service.OverallSummary;
import com.studyflow.service.StudyFlowService;
import com.studyflow.service.SubjectReport;
import com.studyflow.storage.FileDataStore;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

public class StudyFlowDesktop extends Application {

    private final StudyFlowService service = new StudyFlowService();
    private final FileDataStore store = createDataStore();

    private final BorderPane root = new BorderPane();
    private final VBox content = new VBox(18);
    private final Label pageTitle = new Label("Dashboard");
    private final Label notice = new Label();
    private final TableView<Task> taskTable = createTaskTable();
    private final String green = "#286653";
    private final String ink = "#18332D";
    private final String muted = "#6B7D77";
    private final String background = "#F4F7F5";

    
    private static FileDataStore createDataStore() {
    Path directory;
    Path dDrive = Paths.get("D:\\");
    String appData = System.getenv("APPDATA");

    // Prefer D: drive; fall back to AppData if D: is unavailable.
    if (Files.isDirectory(dDrive)) {
        directory = Paths.get("D:\\StudyFlowData");
    } else if (appData != null && !appData.isBlank()) {
        directory = Paths.get(appData, "StudyFlow");
    } else {
        directory = Paths.get(System.getProperty("user.home"), ".studyflow");
    }

    Path target = directory.resolve("studyflow-data.txt");
    Path oldAppData = appData != null && !appData.isBlank()
            ? Paths.get(appData, "StudyFlow", "studyflow-data.txt")
            : null;
    Path legacy = Paths.get("studyflow-data.txt").toAbsolutePath();

    try {
        Files.createDirectories(directory);

        // Preserve existing data when initializing the new location.
        if (!Files.exists(target)) {
            if (oldAppData != null && Files.exists(oldAppData)) {
                Files.copy(oldAppData, target);
            } else if (Files.exists(legacy)) {
                Files.copy(legacy, target);
            }
        }
    } catch (IOException ex) {
        throw new IllegalStateException(
                "Could not initialize StudyFlow data folder: " + directory,
                ex
        );
    }

    return new FileDataStore(target.toString());
}


    @Override
    public void start(Stage stage) {
        loadExistingData();

        root.setLeft(createSidebar());
        root.setCenter(createMainArea());

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().clear();
        root.setStyle("-fx-background-color: " + background + ";");

        stage.setTitle("StudyFlow | Student Productivity");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();

        showDashboard();
    }

    private void loadExistingData() {
        if (!store.exists()) {
            return;
        }

        try {
            service.importData(store.load());
        } catch (StorageException | RuntimeException ex) {
            showMessage("Could not load saved data: " + ex.getMessage(), true);
        }
    }

    private BorderPane createMainArea() {
        VBox header = new VBox(6);
        header.setPadding(new Insets(28, 34, 12, 34));

        pageTitle.setStyle(
                "-fx-font-size: 27px; -fx-font-weight: bold; -fx-text-fill: " + ink + ";"
        );

        Label subtitle = new Label("Plan smarter. Study consistently. Make progress.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + muted + ";");

        notice.setStyle("-fx-text-fill: " + green + "; -fx-font-size: 12px;");
        header.getChildren().addAll(pageTitle, subtitle, notice);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: " + background + "; -fx-background-color: transparent;"
        );
        content.setPadding(new Insets(16, 34, 34, 34));

        BorderPane main = new BorderPane();
        main.setTop(header);
        main.setCenter(scroll);
        return main;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(28, 16, 20, 16));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #173F35;");

        Label brand = new Label("StudyFlow");
        brand.setStyle(
                "-fx-font-size: 25px; -fx-font-weight: bold; -fx-text-fill: white;"
        );

        Label tagline = new Label("YOUR STUDY SPACE");
        tagline.setStyle("-fx-font-size: 10px; -fx-text-fill: #B9D6C9;");

        Region gap = new Region();
        VBox.setVgrow(gap, Priority.NEVER);

        Button dashboard = navButton("Dashboard", this::showDashboard);
        Button tasks = navButton("My Tasks", this::showTasks);
        Button subjects = navButton("Subjects", this::showSubjects);
        Button sessions = navButton("Study Sessions", this::showSessions);
        Button save = navButton("Save My Data", this::saveData);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("Small steps add up.");
        footer.setStyle("-fx-text-fill: #B9D6C9; -fx-font-size: 11px;");

        sidebar.getChildren().addAll(
                brand, tagline, new Separator(), gap,
                dashboard, tasks, subjects, sessions, spacer, save, footer
        );

        return sidebar;
    }

    private Button navButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(12, 14, 12, 14));
        button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-background-radius: 8;"
        );

        button.setOnAction(event -> action.run());

        button.setOnMouseEntered(event ->
                button.setStyle(
                        "-fx-background-color: #286653; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-background-radius: 8;"
                )
        );

        button.setOnMouseExited(event ->
                button.setStyle(
                        "-fx-background-color: transparent; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-background-radius: 8;"
                )
        );

        return button;
    }

    private void setPage(String title) {
        pageTitle.setText(title);
        notice.setText("");
        content.getChildren().clear();
    }

    private void showDashboard() {
        setPage("Dashboard");

        OverallSummary summary = service.getSummary();

        HBox cards = new HBox(14);
        cards.getChildren().addAll(
                statCard("Total tasks",
                        String.valueOf(summary.getTotalTasks()), "All your tasks"),
                statCard("Completed",
                        String.valueOf(summary.getCompletedTasks()),
                        String.format("%.0f%% completion", summary.getCompletionRate())),
                statCard("Overdue",
                        String.valueOf(summary.getOverdueTasks()), "Need your attention"),
                statCard("Study hours",
                        String.format("%.1f", summary.getTotalHours()), "Hours recorded")
        );

        for (javafx.scene.Node node : cards.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        Label focus = new Label(
                "Focus subject: " + summary.getFocusSubject().orElse("Not enough data yet")
        );
        focus.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ink + ";");

        Label section = sectionLabel("Upcoming tasks");
        TableView<Task> upcoming = createTaskTable();
        upcoming.setPrefHeight(300);
        upcoming.getItems().setAll(
                service.getTasksByDeadline().stream().limit(6).toList()
        );

        Button addTask = primaryButton("+ Add a task");
        addTask.setOnAction(event -> addTaskDialog());

        Button addSubject = secondaryButton("+ Add a subject");
        addSubject.setOnAction(event -> addSubjectDialog());

        HBox actions = new HBox(10, addTask, addSubject);
        content.getChildren().addAll(cards, focus, section, upcoming, actions);
    }

    private VBox statCard(String heading, String value, String description) {
        VBox card = new VBox(9);
        card.setPadding(new Insets(18));
        card.setMinWidth(130);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-border-color: #E0E9E4; -fx-border-radius: 12;"
        );

        Label title = new Label(heading);
        title.setStyle("-fx-font-size: 12px; -fx-text-fill: " + muted + ";");

        Label number = new Label(value);
        number.setStyle(
                "-fx-font-size: 27px; -fx-font-weight: bold; -fx-text-fill: " + green + ";"
        );

        Label detail = new Label(description);
        detail.setStyle("-fx-font-size: 11px; -fx-text-fill: " + muted + ";");

        card.getChildren().addAll(title, number, detail);
        return card;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + ink + ";"
        );
        return label;
    }

    private TableView<Task> createTaskTable() {
        TableView<Task> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No tasks yet. Add a task to get started."));

        TableColumn<Task, String> title = new TableColumn<>("Task");
        title.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getTitle()
                )
        );

        TableColumn<Task, String> subject = new TableColumn<>("Subject");
        subject.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getSubject().getName()
                )
        );

        TableColumn<Task, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getType().getLabel()
                )
        );

        TableColumn<Task, String> due = new TableColumn<>("Due date");
        due.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getDueDate().toString()
                )
        );

        TableColumn<Task, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getStatus().getLabel()
                )
        );

        TableColumn<Task, String> progress = new TableColumn<>("Progress");
        progress.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f%%", cell.getValue().getProgressPercent())
                )
        );

        table.getColumns().addAll(title, subject, type, due, status, progress);
        return table;
    }

    private void showTasks() {
        setPage("My Tasks");

        HBox actions = new HBox(10);

        Button add = primaryButton("+ Add task");
        add.setOnAction(event -> addTaskDialog());

        Button complete = secondaryButton("Mark selected complete");
        complete.setOnAction(event -> {
            Task selected = taskTable.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showMessage("Select a task first.", true);
                return;
            }

            try {
                service.completeTask(selected.getId());
                refreshTaskViews();
                showTasks();
                showMessage("Task marked as completed.", false);
            } catch (com.studyflow.exception.EntityNotFoundException
                     | RuntimeException ex) {
                showMessage("Could not complete task: " + ex.getMessage(), true);
            }
        });

        Button log = secondaryButton("Log study time");
        log.setOnAction(event -> logSessionDialog());

        actions.getChildren().addAll(add, complete, log);

        taskTable.getItems().setAll(service.getTasksByDeadline());
        taskTable.setPrefHeight(470);

        content.getChildren().addAll(
                new Label("Select a task to update its status or record study time."),
                taskTable, actions
        );
    }

    private void showSubjects() {
        setPage("Subjects");

        Button add = primaryButton("+ Add subject");
        add.setOnAction(event -> addSubjectDialog());
        content.getChildren().add(add);

        if (service.getSubjects().isEmpty()) {
            content.getChildren().add(
                    new Label("No subjects yet. Add your first subject.")
            );
            return;
        }

        for (SubjectReport report : service.getSubjectReports()) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(18));
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 12;" +
                    "-fx-border-color: #E0E9E4; -fx-border-radius: 12;"
            );

            Label name = sectionLabel(report.getSubjectName());

            Label details = new Label(String.format(
                    "Progress: %.0f%%     Studied: %.1f hours     Active tasks: %d     Overdue: %d",
                    report.getProgress(), report.getHoursStudied(),
                    report.getActiveTasks(), report.getOverdueTasks()
            ));

            ProgressBar bar = new ProgressBar(
                    Math.max(0, Math.min(1, report.getProgress() / 100.0))
            );
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setStyle("-fx-accent: " + green + ";");

            Label prep = new Label(
                    "Preparation level: " + report.getPreparation() + "/100"
            );
            prep.setStyle("-fx-text-fill: " + muted + ";");

            card.getChildren().addAll(name, details, bar, prep);
            content.getChildren().add(card);
        }
    }

    private void showSessions() {
        setPage("Study Sessions");

        Button log = primaryButton("+ Log study session");
        log.setOnAction(event -> logSessionDialog());
        content.getChildren().add(log);

        if (service.getSessions().isEmpty()) {
            content.getChildren().add(
                    new Label("No study sessions recorded yet.")
            );
            return;
        }

        TableView<com.studyflow.model.StudySession> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<com.studyflow.model.StudySession, String> task =
                new TableColumn<>("Task");
        task.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getTask().getTitle()
                )
        );

        TableColumn<com.studyflow.model.StudySession, String> date =
                new TableColumn<>("Date");
        date.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getDate().toString()
                )
        );

        TableColumn<com.studyflow.model.StudySession, String> hours =
                new TableColumn<>("Hours");
        hours.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.1f", cell.getValue().getHours())
                )
        );

        table.getColumns().addAll(task, date, hours);
        table.getItems().setAll(service.getSessions());
        table.setPrefHeight(450);
        content.getChildren().add(table);
    }

    private void addSubjectDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add subject");
        dialog.setHeaderText("Create a subject to organise your work.");
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL
        );

        TextField name = new TextField();
        name.setPromptText("e.g. Data Structures");

        Spinner<Integer> difficulty = new Spinner<>(1, 5, 3);
        Spinner<Integer> preparation = new Spinner<>(0, 100, 30);
        difficulty.setEditable(true);
        preparation.setEditable(true);

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Subject name"), name);
        grid.addRow(1, new Label("Difficulty (1–5)"), difficulty);
        grid.addRow(2, new Label("Preparation (0–100)"), preparation);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (name.getText().isBlank()) {
                    throw new IllegalArgumentException("Enter a subject name.");
                }

                service.addSubject(
                        name.getText().trim(),
                        difficulty.getValue(),
                        preparation.getValue()
                );

                showSubjects();
                showMessage("Subject added. Save to keep it for next time.", false);
            } catch (RuntimeException ex) {
                showMessage("Could not add subject: " + ex.getMessage(), true);
            }
        }
    }

    private void addTaskDialog() {
        if (service.getSubjects().isEmpty()) {
            showMessage("Add a subject first, then add a task.", true);
            addSubjectDialog();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add task");
        dialog.setHeaderText("Add an assignment, exam, or project.");
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL
        );

        ComboBox<Subject> subject = new ComboBox<>(
                FXCollections.observableArrayList(service.getSubjects())
        );
        subject.setMaxWidth(Double.MAX_VALUE);

        subject.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Subject item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        subject.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Subject item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        subject.getSelectionModel().selectFirst();

        TextField title = new TextField();
        title.setPromptText("e.g. Binary tree assignment");

        DatePicker due = new DatePicker(LocalDate.now().plusDays(7));
        due.setMaxWidth(Double.MAX_VALUE);

        TextField hours = new TextField("3");

        ComboBox<TaskType> type =
                new ComboBox<>(FXCollections.observableArrayList(TaskType.values()));
        type.setMaxWidth(Double.MAX_VALUE);
        type.getSelectionModel().selectFirst();

        ComboBox<Difficulty> difficulty =
                new ComboBox<>(FXCollections.observableArrayList(Difficulty.values()));
        difficulty.setMaxWidth(Double.MAX_VALUE);
        difficulty.getSelectionModel().select(Difficulty.MEDIUM);

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Subject"), subject);
        grid.addRow(1, new Label("Task title"), title);
        grid.addRow(2, new Label("Type"), type);
        grid.addRow(3, new Label("Due date"), due);
        grid.addRow(4, new Label("Estimated hours"), hours);
        grid.addRow(5, new Label("Difficulty"), difficulty);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (subject.getValue() == null || title.getText().isBlank()) {
                    throw new IllegalArgumentException(
                            "Choose a subject and enter a title."
                    );
                }

                if (due.getValue() == null
                        || due.getValue().isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException(
                            "Choose today or a future due date."
                    );
                }

                double estimatedHours =
                        Double.parseDouble(hours.getText().trim());

                if (estimatedHours <= 0) {
                    throw new IllegalArgumentException(
                            "Estimated hours must be greater than zero."
                    );
                }

                String key = subject.getValue().getId();
                String taskTitle = title.getText().trim();
                LocalDate deadline = due.getValue();
                Difficulty level = difficulty.getValue();

                switch (type.getValue()) {
                    case ASSIGNMENT -> service.addAssignment(
                            key, taskTitle, deadline, estimatedHours, level, 1
                    );
                    case EXAM -> service.addExam(
                            key, taskTitle, deadline, estimatedHours, level, 1, 0
                    );
                    case PROJECT -> service.addProject(
                            key, taskTitle, deadline, estimatedHours, level, 1
                    );
                }

                showTasks();
                showMessage("Task added. Save to keep it for next time.", false);
            } catch (Exception ex) {
                showMessage("Could not add task: " + ex.getMessage(), true);
            }
        }
    }

    private void logSessionDialog() {
        if (service.getTasks().isEmpty()) {
            showMessage("Add a task before logging a study session.", true);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Log study session");
        dialog.setHeaderText("Record time spent studying.");
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL
        );

        ComboBox<Task> task = new ComboBox<>(
                FXCollections.observableArrayList(service.getActiveTasks())
        );
        task.setMaxWidth(Double.MAX_VALUE);

        task.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });

        task.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });

        task.getSelectionModel().selectFirst();

        TextField hours = new TextField("1");

        DatePicker date = new DatePicker(LocalDate.now());
        date.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = formGrid();
        grid.addRow(0, new Label("Task"), task);
        grid.addRow(1, new Label("Hours studied"), hours);
        grid.addRow(2, new Label("Session date"), date);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (task.getValue() == null) {
                    throw new IllegalArgumentException("Choose a task.");
                }

                double amount = Double.parseDouble(hours.getText().trim());

                if (amount <= 0) {
                    throw new IllegalArgumentException(
                            "Hours must be greater than zero."
                    );
                }

                if (date.getValue() == null
                        || date.getValue().isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException(
                            "Choose today or an earlier date."
                    );
                }

                service.logSession(
                        task.getValue().getId(), amount, date.getValue()
                );

                refreshTaskViews();
                showSessions();
                showMessage(
                        "Study session recorded. Save to keep it for next time.",
                        false
                );
            } catch (Exception ex) {
                showMessage("Could not log session: " + ex.getMessage(), true);
            }
        }
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.setPrefWidth(440);

        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(140);

        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labels, fields);
        return grid;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + green + "; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-padding: 10 16 10 16;" +
                "-fx-background-radius: 8;"
        );
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: white; -fx-text-fill: " + green + ";" +
                "-fx-border-color: #B9D6C9; -fx-padding: 10 16 10 16;" +
                "-fx-background-radius: 8; -fx-border-radius: 8;"
        );
        return button;
    }

    private void refreshTaskViews() {
        taskTable.getItems().setAll(service.getTasksByDeadline());
    }

    private void saveData() {
        try {
            store.save(service.exportData());
            showMessage("Saved successfully to " + store.getPath(), false);
        } catch (StorageException ex) {
            showMessage("Save failed: " + ex.getMessage(), true);
        }
    }

    private void showMessage(String message, boolean error) {
        notice.setText(message);
        notice.setStyle(
                "-fx-text-fill: " + (error ? "#B42318" : green) +
                "; -fx-font-size: 12px;"
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}
