package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.List;

public class MenuController {
    private Stage primaryStage;
    @FXML private Label lastScoreLabel;
    @FXML private ToggleGroup difficultyGroup;
    @FXML private RadioButton easyBtn, normalBtn, hardBtn, insaneBtn;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void initialize() {
        refreshScoresDisplay();
    }

    @FXML
    private void onPlay() {
        loadGameSceneWithDifficulty(getSelectedDifficulty());
    }

    @FXML
    private void onMultiplayer() {
        try {
            // Save the current difficulty selection before navigating to multiplayer menu
            GameController.Difficulty selectedDifficulty = getSelectedDifficulty();
            GameController.setSavedDifficulty(selectedDifficulty);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("multiplayer.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = (Stage) lastScoreLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
            root.requestFocus();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onSinglePlayer() {
        loadGameSceneWithDifficulty(getSelectedDifficulty());
    }

    @FXML
    private void onScoreboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("scoreboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
            root.requestFocus();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onQuit() { System.exit(0); }

    public void handleKey(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER -> onPlay();
            case ESCAPE -> System.exit(0);
            default -> {}
        }
    }

    private GameController.Difficulty getSelectedDifficulty() {
        if (normalBtn.isSelected()) return GameController.Difficulty.NORMAL;
        if (hardBtn.isSelected()) return GameController.Difficulty.HARD;
        if (insaneBtn.isSelected()) return GameController.Difficulty.INSANE;
        return GameController.Difficulty.EASY; // default
    }

    private void loadGameSceneWithDifficulty(GameController.Difficulty difficulty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("game.fxml"));
            Parent root = loader.load();
            GameController controller = loader.getController();

            controller.setPrimaryStage(primaryStage);
            controller.setDifficulty(difficulty);
            
            // Re-setup game after difficulty change to ensure entities are created correctly
            controller.setupGameAfterDifficultyChange();

            // Calculate scene size dynamically based on map dimensions
            double[] dimensions = controller.getSceneDimensions();
            Scene scene = new Scene(root, dimensions[0], dimensions[1]);
            scene.setOnKeyPressed(e -> controller.handleKey(e));

            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
            root.requestFocus();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setLastScore(int score) {
        if (lastScoreLabel != null) lastScoreLabel.setText("Last Score: " + score);
    }

    public void refreshScoresDisplay() {
        int last = ScoreManager.getLastScore();
        if (lastScoreLabel != null) lastScoreLabel.setText("Last Score: " + last);
    }
}
