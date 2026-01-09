package application;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class IntroController {
    @FXML private VBox introContainer;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private GridPane creatorsGrid;
    @FXML private VBox creatorBox1;
    @FXML private VBox creatorBox2;
    @FXML private VBox creatorBox3;
    @FXML private VBox creatorBox4;
    @FXML private Label creatorName1;
    @FXML private Label creatorName2;
    @FXML private Label creatorName3;
    @FXML private Label creatorName4;
    @FXML private Label creatorDesc1;
    @FXML private Label creatorDesc2;
    @FXML private Label creatorDesc3;
    @FXML private Label creatorDesc4;
    @FXML private Label pressKeyLabel;

    private Stage primaryStage;

    @FXML
    public void initialize() {
        // Set initial opacity to 0 for fade-in effect
        introContainer.setOpacity(0);
        titleLabel.setOpacity(0);
        subtitleLabel.setOpacity(0);
        creatorsGrid.setOpacity(0);
        pressKeyLabel.setOpacity(0);
        
        // Apply impressive effects
        applyImpressiveEffects();
        
        // Create cinematic sequence
        createCinematicSequence();
    }

    private void applyImpressiveEffects() {
        // Title effects - only drop shadow, no glow
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.GOLD);
        titleShadow.setRadius(20);
        titleShadow.setSpread(0.5);
        titleLabel.setEffect(titleShadow);
        
        // Subtitle effects - only drop shadow, no glow
        DropShadow subtitleShadow = new DropShadow();
        subtitleShadow.setColor(Color.YELLOW);
        subtitleShadow.setRadius(30);
        subtitleShadow.setSpread(0.7);
        subtitleLabel.setEffect(subtitleShadow);
        
        // Creator boxes effects - only drop shadow, no glow on text
        applyCreatorBoxEffects(creatorBox1, Color.CYAN);
        applyCreatorBoxEffects(creatorBox2, Color.MAGENTA);
        applyCreatorBoxEffects(creatorBox3, Color.LIME);
        applyCreatorBoxEffects(creatorBox4, Color.ORANGE);
        
        // Press key label effect - only drop shadow
        DropShadow pressShadow = new DropShadow();
        pressShadow.setColor(Color.YELLOW);
        pressShadow.setRadius(10);
        pressKeyLabel.setEffect(pressShadow);
        
        // Create pulsing animation for press key label
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(pressKeyLabel.opacityProperty(), 0.5)),
            new KeyFrame(Duration.seconds(1), new KeyValue(pressKeyLabel.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(2), new KeyValue(pressKeyLabel.opacityProperty(), 0.5))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    private void applyCreatorBoxEffects(VBox box, Color glowColor) {
        // Only drop shadow on boxes, no glow
        DropShadow boxShadow = new DropShadow();
        boxShadow.setColor(glowColor);
        boxShadow.setRadius(15);
        boxShadow.setSpread(0.3);
        box.setEffect(boxShadow);
        
        // Add border effect
        box.setStyle(box.getStyle() + 
            "-fx-border-color: " + toHex(glowColor) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 10px;" +
            "-fx-background-radius: 10px;");
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    private void createCinematicSequence() {
        SequentialTransition sequence = new SequentialTransition();

        // Fade in title with scale animation (2.5 seconds)
        FadeTransition titleFadeIn = new FadeTransition(Duration.seconds(2.5), titleLabel);
        titleFadeIn.setFromValue(0);
        titleFadeIn.setToValue(1);
        
        ScaleTransition titleScale = new ScaleTransition(Duration.seconds(2.5), titleLabel);
        titleScale.setFromX(0.5);
        titleScale.setFromY(0.5);
        titleScale.setToX(1.0);
        titleScale.setToY(1.0);
        
        ParallelTransition titleAnim = new ParallelTransition(titleFadeIn, titleScale);
        sequence.getChildren().add(titleAnim);

        // Hold title (1 second)
        PauseTransition titleHold = new PauseTransition(Duration.seconds(1));
        sequence.getChildren().add(titleHold);

        // Fade in subtitle with scale and rotation (2 seconds)
        FadeTransition subtitleFadeIn = new FadeTransition(Duration.seconds(2), subtitleLabel);
        subtitleFadeIn.setFromValue(0);
        subtitleFadeIn.setToValue(1);
        
        ScaleTransition subtitleScale = new ScaleTransition(Duration.seconds(2), subtitleLabel);
        subtitleScale.setFromX(0.3);
        subtitleScale.setFromY(0.3);
        subtitleScale.setToX(1.0);
        subtitleScale.setToY(1.0);
        
        RotateTransition subtitleRotate = new RotateTransition(Duration.seconds(2), subtitleLabel);
        subtitleRotate.setFromAngle(-10);
        subtitleRotate.setToAngle(0);
        
        ParallelTransition subtitleAnim = new ParallelTransition(subtitleFadeIn, subtitleScale, subtitleRotate);
        sequence.getChildren().add(subtitleAnim);

        // Hold subtitle (0.5 seconds)
        PauseTransition subtitleHold = new PauseTransition(Duration.seconds(0.5));
        sequence.getChildren().add(subtitleHold);

        // Fade in all creators simultaneously in 2x2 grid (2 seconds)
        FadeTransition creatorsFadeIn = new FadeTransition(Duration.seconds(2), creatorsGrid);
        creatorsFadeIn.setFromValue(0);
        creatorsFadeIn.setToValue(1);
        
        ScaleTransition creatorsScale = new ScaleTransition(Duration.seconds(2), creatorsGrid);
        creatorsScale.setFromX(0.8);
        creatorsScale.setFromY(0.8);
        creatorsScale.setToX(1.0);
        creatorsScale.setToY(1.0);
        
        ParallelTransition creatorsAnim = new ParallelTransition(creatorsFadeIn, creatorsScale);
        sequence.getChildren().add(creatorsAnim);

        // Hold all creators (3 seconds)
        PauseTransition creatorsHold = new PauseTransition(Duration.seconds(3   ));
        sequence.getChildren().add(creatorsHold);

        // Fade in "Press any key" message
        FadeTransition pressKeyFadeIn = new FadeTransition(Duration.seconds(1.5), pressKeyLabel);
        pressKeyFadeIn.setFromValue(0);
        pressKeyFadeIn.setToValue(1);
        sequence.getChildren().add(pressKeyFadeIn);

        // Hold before fade out (2 seconds)
        PauseTransition finalHold = new PauseTransition(Duration.seconds(2));
        sequence.getChildren().add(finalHold);

        // Fade out everything (2 seconds)
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), introContainer);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        sequence.getChildren().add(fadeOut);

        // On finished, transition to sign-in
        sequence.setOnFinished(e -> transitionToSignIn());

        sequence.play();
    }

    @FXML
    private void onKeyPressed() {
        // Allow skipping the intro by pressing any key
        transitionToSignIn();
    }

    private void transitionToSignIn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("signin.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            if (primaryStage != null) {
                primaryStage.setScene(scene);
                primaryStage.show();
                root.requestFocus();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
}
