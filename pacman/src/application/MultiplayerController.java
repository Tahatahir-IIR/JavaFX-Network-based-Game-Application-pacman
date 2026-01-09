package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class MultiplayerController {
    @FXML private TextField roomCodeField;
    @FXML private Label statusLabel;

    // Renamed variable to avoid conflict with the method name onCreateRoom()
    @FXML private Button createRoomBtn;

    @FXML
    private void onLocal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("game.fxml"));
            Parent root = loader.load();
            GameController controller = loader.getController();
            
            // Get saved difficulty and apply it
            GameController.Difficulty savedDifficulty = GameController.getSavedDifficulty();
            controller.setDifficulty(savedDifficulty);
            controller.enableLocalTwoPlayer();
            
            // setupGame() is already called in initialize(), but we need to re-setup after setDifficulty
            // to ensure entities are created with the correct difficulty settings
            controller.setupGameAfterDifficultyChange();
            
            Stage stage = (Stage) roomCodeField.getScene().getWindow();
            controller.setPrimaryStage(stage);
            // Calculate scene size dynamically based on map dimensions
            double[] dimensions = controller.getSceneDimensions();
            Scene scene = new Scene(root, dimensions[0], dimensions[1]);
            scene.setOnKeyPressed(e -> controller.handleKey(e));
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            root.requestFocus();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onCreateRoom() {
        try {
            NetworkManager nm = NetworkManager.createServer();
            String code = nm.getRoomCode();
            roomCodeField.setText(code);
            statusLabel.setText("Room created — waiting for player to join.");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("waiting_room.fxml"));
            Parent root = loader.load();
            WaitingRoomController controller = loader.getController();
            Stage stage = (Stage) roomCodeField.getScene().getWindow();
            controller.setPrimaryStage(stage);
            controller.setRoomCode(code);
            controller.initNetworkManager(nm, true);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            root.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to create room");
        }
    }

    

    @FXML
    private void onJoinRoom() {
        String code = roomCodeField.getText().trim();
        if (code.isEmpty()) {
            statusLabel.setText("Enter a room code");
            return;
        }
        try {
            NetworkManager nm = NetworkManager.connectTo(code);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("waiting_room.fxml"));
            Parent root = loader.load();
            WaitingRoomController controller = loader.getController();
            Stage stage = (Stage) roomCodeField.getScene().getWindow();
            controller.setPrimaryStage(stage);
            controller.setRoomCode(code);
            controller.initNetworkManager(nm, false);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            root.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to join room");
        }
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
            Parent root = loader.load();
            MenuController controller = loader.getController();
            Stage stage = (Stage) roomCodeField.getScene().getWindow();
            controller.setPrimaryStage(stage);
            Scene scene = new Scene(root);
            scene.setOnKeyPressed(e -> controller.handleKey(e));
            stage.setScene(scene);
            stage.show();
            root.requestFocus();
        } catch (Exception e) { e.printStackTrace(); }
    }
}