package application;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class WaitingRoomController {
    @FXML private Label roomCodeLabel;
    @FXML private VBox playersList;
    @FXML private Label statusLabel;

    private NetworkManager networkManager;
    private Stage primaryStage;
    private String fullRoomCode;
    private volatile boolean clientJoined = false;
    private boolean isHost = false;
    private String hostName = "";
    private List<String> playerNames = new ArrayList<>();
    private List<PrintWriter> clientWriters = new ArrayList<>();

    // We store this so the Host can send the "START" command to the client
    private PrintWriter hostOutputStream;

    /**
     * Initializes the network logic based on whether the user is hosting or joining.
     */
    public void initNetworkManager(NetworkManager nm, boolean isHost) {
        this.networkManager = nm;
        this.isHost = isHost;

        // Determine local username
        String tempName = (UserSession.getCurrentUser() != null) ? UserSession.getCurrentUser().username : (isHost ? "Host" : "Guest");
        final String myName = tempName;
        this.hostName = myName;

        if (isHost) {
            playerNames.add(myName);
            updatePlayersList();
        } else {
            updatePlayersList();
        }

        Thread networkThread = new Thread(() -> {
            try {
                if (isHost) {
                    handleHostLogic(myName);
                } else {
                    handleClientLogic(myName);
                }
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Connection lost: " + e.getMessage()));
                e.printStackTrace();
            }
        });

        networkThread.setDaemon(true);
        networkThread.start();

        // Key listener for host to start the game with ENTER
        if (isHost) {
            Platform.runLater(() -> {
                if (roomCodeLabel.getScene() != null) {
                    roomCodeLabel.getScene().setOnKeyPressed(ev -> {
                        if (ev.getCode() == KeyCode.ENTER && clientJoined) {
                            onPlay();
                        }
                    });
                }
            });
        }
    }

    private void handleHostLogic(String myName) throws Exception {
        Platform.runLater(() -> statusLabel.setText("Waiting for player to join..."));

        java.net.Socket socket = networkManager.waitForClient();
        if (socket == null) return;

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        this.hostOutputStream = out;
        clientWriters.add(out);

        // Wait for JOIN message from client
        String line = in.readLine();
        String clientName = "Player 2";
        if (line != null && line.startsWith("JOIN:")) {
            clientName = line.substring(5);
        }

        synchronized (playerNames) {
            if (!playerNames.contains(clientName)) {
                playerNames.add(clientName);
            }
        }
        clientJoined = true;

        // Confirm to client and send player names
        out.println("PLAYERS:" + String.join(",", playerNames));

        Platform.runLater(() -> {
            updatePlayersList();
            statusLabel.setText("Player joined — Press Play or ENTER to start.");
        });
        
        // Listen for messages from client
        String cmd;
        while ((cmd = in.readLine()) != null) {
            if (cmd.startsWith("KICKED:")) {
                // Client was kicked, close connection
                break;
            }
        }
    }

    private void handleClientLogic(String myName) throws Exception {
        java.net.Socket socket = networkManager.getClientSocket();
        if (socket == null) throw new IllegalStateException("Not connected to host");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Send our name to the host
        out.println("JOIN:" + myName);

        String response = in.readLine();
        if (response != null && response.startsWith("PLAYERS:")) {
            final String playerListStr = response.substring(8);
            String[] names = playerListStr.split(",");
            synchronized (playerNames) {
                playerNames.clear();
                for (String name : names) {
                    playerNames.add(name.trim());
                }
            }
            Platform.runLater(() -> {
                updatePlayersList();
                statusLabel.setText("Connected — waiting for host to start.");
            });
            clientJoined = true;

            // Stay alive and listen for the "START" or "KICKED" command from host
            String cmd;
            while ((cmd = in.readLine()) != null) {
                if ("START".equals(cmd)) {
                    Platform.runLater(() -> startGame(false));
                    break;
                } else if ("KICKED".equals(cmd)) {
                    Platform.runLater(() -> {
                        statusLabel.setText("You were kicked from the room.");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {}
                        onCancel();
                    });
                    break;
                } else if (cmd.startsWith("PLAYERS:")) {
                    // Update player list
                    final String updatedList = cmd.substring(8);
                    String[] updatedNames = updatedList.split(",");
                    synchronized (playerNames) {
                        playerNames.clear();
                        for (String name : updatedNames) {
                            playerNames.add(name.trim());
                        }
                    }
                    Platform.runLater(() -> updatePlayersList());
                }
            }
        }
    }
    
    private void updatePlayersList() {
        if (playersList == null) return;
        playersList.getChildren().clear();
        
        synchronized (playerNames) {
            for (String playerName : playerNames) {
                HBox playerRow = new HBox(10);
                playerRow.setAlignment(javafx.geometry.Pos.CENTER);
                
                Label nameLabel = new Label(playerName);
                nameLabel.setStyle("-fx-text-fill: linear-gradient(to right, #FFD700, #FFA500); -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-color: rgba(0, 0, 0, 0.4); -fx-background-radius: 8px;");
                
                playerRow.getChildren().add(nameLabel);
                
                // Add X button only for host and only for other players (not self)
                if (isHost && !playerName.equals(hostName)) {
                    Button kickButton = new Button("✕");
                    kickButton.setStyle("-fx-background-color: linear-gradient(to bottom, #FF6B6B, #E55555); -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 15px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-cursor: hand;");
                    kickButton.setOnAction(e -> kickPlayer(playerName));
                    playerRow.getChildren().add(kickButton);
                }
                
                playersList.getChildren().add(playerRow);
            }
        }
    }
    
    private void kickPlayer(String playerName) {
        synchronized (playerNames) {
            if (playerNames.remove(playerName)) {
                // Send KICKED message to the kicked player
                for (int i = 0; i < clientWriters.size(); i++) {
                    PrintWriter writer = clientWriters.get(i);
                    try {
                        writer.println("KICKED");
                        clientWriters.remove(i);
                        break;
                    } catch (Exception e) {
                        clientWriters.remove(i);
                        i--;
                    }
                }
                
                // Update all remaining clients with new player list
                String playerListStr = String.join(",", playerNames);
                for (PrintWriter writer : clientWriters) {
                    try {
                        writer.println("PLAYERS:" + playerListStr);
                    } catch (Exception e) {
                        // Connection lost, remove writer
                    }
                }
                
                Platform.runLater(() -> {
                    updatePlayersList();
                    statusLabel.setText(playerName + " was kicked from the room.");
                });
                
                if (playerNames.size() == 1) {
                    clientJoined = false;
                }
            }
        }
    }

    @FXML
    private void onPlay() {
        if (!clientJoined) {
            statusLabel.setText("No player joined yet.");
            return;
        }

        // Send START signal to the client
        if (hostOutputStream != null) {
            hostOutputStream.println("START");
        }

        // Move host to game screen
        startGame(true);
    }

    private void startGame(boolean isHost) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("game.fxml"));
            Parent root = loader.load();

            GameController controller = loader.getController();
            
            // Get saved difficulty and apply it
            GameController.Difficulty savedDifficulty = GameController.getSavedDifficulty();
            controller.setDifficulty(savedDifficulty);
            
            controller.setNetworkManager(networkManager);
            controller.setPrimaryStage(primaryStage);
            controller.setHostMode(isHost);

            if (!isHost) {
                controller.enableNetworkClientMode();
            }

            // setupGame() is already called in initialize(), but we need to re-setup after setDifficulty
            // to ensure entities are created with the correct difficulty settings
            controller.setupGameAfterDifficultyChange();

            // Don't auto-start - wait for host to press ENTER in game screen
            // Game will start in MENU state

            // Calculate scene size dynamically based on map dimensions
            double[] dimensions = controller.getSceneDimensions();
            Scene scene = new Scene(root, dimensions[0], dimensions[1]);
            scene.setOnKeyPressed(e -> controller.handleKey(e));
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
            root.requestFocus();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to launch game.");
        }
    }

    public void setPrimaryStage(Stage stage) { this.primaryStage = stage; }

    public void setRoomCode(String code) {
        this.fullRoomCode = code;
        if (roomCodeLabel != null) roomCodeLabel.setText(makeShortCode(code));
    }

    private String makeShortCode(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes("UTF-8"));
            BigInteger bi = new BigInteger(1, d);
            String s = bi.toString(36).toUpperCase();
            return (s.length() < 6) ? (s + "000000").substring(0, 6) : s.substring(0, 6);
        } catch (Exception e) {
            return "ERROR1";
        }
    }

    @FXML
    private void onCopyRoomCode() {
        if (fullRoomCode == null) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(fullRoomCode);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Full room code copied!");
    }

    @FXML
    private void onCancel() {
        try {
            if (networkManager != null) networkManager.close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("multiplayer.fxml"));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}