package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void onSignIn() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) { messageLabel.setText("Enter username and password"); return; }
        try {
            User user = AuthManager.login(u, p);
            if (user == null) { messageLabel.setText("Invalid credentials"); return; }
            UserSession.setCurrentUser(user);
            openMenu();
        } catch (Exception e) {
            messageLabel.setText("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSignUp() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) { messageLabel.setText("Enter username and password"); return; }
        try {
            User user = AuthManager.register(u, p);
            if (user == null) { messageLabel.setText("Registration failed"); return; }
            UserSession.setCurrentUser(user);
            openMenu();
        } catch (Exception e) {
            messageLabel.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
            Parent root = loader.load();
            MenuController controller = loader.getController();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            controller.setPrimaryStage(stage);
            controller.refreshScoresDisplay();
            Scene scene = new Scene(root);
            scene.setOnKeyPressed(e -> controller.handleKey(e));
            stage.setScene(scene);
            stage.show();
            root.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
