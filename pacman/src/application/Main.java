package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize database and tables
        Database.init();

        // Start by showing the cinematic intro screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("intro.fxml"));
        Parent root = loader.load();
        IntroController introController = loader.getController();
        introController.setPrimaryStage(stage);
        
        Scene scene = new Scene(root, 1000, 900);

        stage.setTitle("JavaFX Network-based Game Application");
        stage.setScene(scene);
        // Allow resizing so we can maximize the window without being covered by the taskbar
        stage.setResizable(true);
        // Do not maximize at startup so the intro page stays windowed
        stage.setMaximized(false);
        stage.show();
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}