package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class ScoreboardController {
    @FXML private ListView<String> scoresList;
    @FXML private Button adminButton;
    @FXML private Button deleteButton;
    private Stage primaryStage;

    private enum Mode { SOLO, SOLO_GLOBAL, DUO, DUO_GLOBAL, RANKED, ADMIN }
    private Mode mode = Mode.SOLO;
    private boolean isAdmin = false;

    public void initialize() {
        User currentUser = UserSession.getCurrentUser();
        isAdmin = (currentUser != null && currentUser.isAdmin);
        if (adminButton != null) {
            adminButton.setVisible(isAdmin);
        }
        if (deleteButton != null) {
            deleteButton.setVisible(isAdmin && mode == Mode.ADMIN);
        }
        showSolo();
    }

    private void showSolo() {
        scoresList.getItems().clear();
        List<Integer> top = ScoreManager.getSoloScores(10);
        int rank = 1;
        for (Integer s : top) scoresList.getItems().add(rank++ + ". " + s);
        mode = Mode.SOLO;
        if (deleteButton != null) deleteButton.setVisible(false);
    }
    
    private void showGlobal() {
        scoresList.getItems().clear();
        List<String> top = ScoreManager.getSoloGlobalScores(10);
        int rank = 1;
        for (String entry : top) scoresList.getItems().add(rank++ + ". " + entry);
        mode = Mode.SOLO_GLOBAL;
        if (deleteButton != null) deleteButton.setVisible(false);
    }
    
    private void showDuo() {
        scoresList.getItems().clear();
        List<Integer> top = ScoreManager.getDuoScores(10);
        int rank = 1;
        for (Integer s : top) scoresList.getItems().add(rank++ + ". " + s);
        mode = Mode.DUO;
        if (deleteButton != null) deleteButton.setVisible(false);
    }
    
    private void showDuoGlobal() {
        scoresList.getItems().clear();
        List<String> top = ScoreManager.getDuoGlobalScores(10);
        int rank = 1;
        for (String entry : top) scoresList.getItems().add(rank++ + ". " + entry);
        mode = Mode.DUO_GLOBAL;
        if (deleteButton != null) deleteButton.setVisible(false);
    }
    
    private void showRanked() {
        scoresList.getItems().clear();
        List<Integer> top = ScoreManager.getRankedScores(10);
        int rank = 1;
        for (Integer s : top) scoresList.getItems().add(rank++ + ". " + s);
        mode = Mode.RANKED;
        if (deleteButton != null) deleteButton.setVisible(false);
    }
    
    private void showAdmin() {
        scoresList.getItems().clear();
        if (!isAdmin) {
            scoresList.getItems().add("Admin access required");
            return;
        }
        
        List<Map<String, Object>> allScores = ScoreManager.getAllScoresForAdmin(50);
        for (Map<String, Object> scoreData : allScores) {
            String username = (String) scoreData.get("username");
            int score = (Integer) scoreData.get("score");
            String difficulty = (String) scoreData.get("difficulty");
            String gameMode = (String) scoreData.get("game_mode");
            int scoreId = (Integer) scoreData.get("id");
            
            String entry = String.format("%s - %d (%s, %s) [ID: %d]", username, score, difficulty, gameMode, scoreId);
            scoresList.getItems().add(entry);
        }
        mode = Mode.ADMIN;
        if (deleteButton != null) {
            deleteButton.setVisible(true);
        }
    }

    @FXML
    private void onSolo() { showSolo(); }
    
    @FXML
    private void onGlobal() { showGlobal(); }
    
    @FXML
    private void onDuo() { showDuo(); }
    
    @FXML
    private void onDuoGlobal() { showDuoGlobal(); }
    
    @FXML
    private void onRanked() { showRanked(); }
    
    @FXML
    private void onAdmin() {
        if (isAdmin) {
            showAdmin();
        } else {
            scoresList.getItems().clear();
            scoresList.getItems().add("Admin access required");
        }
    }
    
    @FXML
    private void onDeleteScore() {
        if (!isAdmin) return;
        
        String selected = scoresList.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.contains("[ID: ")) return;
        
        try {
            // Extract score ID from the selected item
            int startIdx = selected.indexOf("[ID: ") + 5;
            int endIdx = selected.indexOf("]", startIdx);
            if (endIdx > startIdx) {
                int scoreId = Integer.parseInt(selected.substring(startIdx, endIdx));
                if (ScoreManager.deleteScore(scoreId)) {
                    // Refresh the current view
                    if (mode == Mode.ADMIN) {
                        showAdmin();
                    } else {
                        switch (mode) {
                            case SOLO: showSolo(); break;
                            case SOLO_GLOBAL: showGlobal(); break;
                            case DUO: showDuo(); break;
                            case DUO_GLOBAL: showDuoGlobal(); break;
                            case RANKED: showRanked(); break;
                            case ADMIN: showAdmin(); break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
            Parent root = loader.load();
            MenuController controller = loader.getController();

            Stage stage = (Stage) scoresList.getScene().getWindow();
            controller.setPrimaryStage(stage);
            controller.refreshScoresDisplay();

            Scene scene = new Scene(root);
            scene.setOnKeyPressed(e -> controller.handleKey(e));
            stage.setScene(scene);
            stage.show();
            root.requestFocus();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
