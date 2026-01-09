package application;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController {

    @FXML private Canvas gameCanvas;
    @FXML private Label scoreLabel;
    @FXML private Label score2Label;
    @FXML private ImageView player1CaughtIcon;
    @FXML private ImageView player2CaughtIcon;
    private GraphicsContext gc;

    private final int TILE_SIZE = 25;
    private final double ENTITY_SPEED = 1.25;
    public enum GhostMode { SCATTER, CHASE, FRIGHTENED }
    private GhostMode currentMode = GhostMode.SCATTER;
    
    // Difficulty settings
    public enum Difficulty { EASY, NORMAL, HARD, INSANE }
    private Difficulty currentDifficulty = Difficulty.EASY;
    
    // Static variable to persist difficulty selection across scene changes
    private static Difficulty savedDifficulty = Difficulty.EASY;
    
    // Public getter and setter for saved difficulty
    public static Difficulty getSavedDifficulty() {
        return savedDifficulty;
    }
    
    public static void setSavedDifficulty(Difficulty diff) {
        savedDifficulty = diff;
    }
    
    private double entitySpeed = 0.75;
    private double scatterDuration = 10.0; // seconds
    private double chaseDuration = 10.0; // seconds
    private double pointMultiplier = 0.8;

    // Game state: menu / running / paused
    private enum GameState { MENU, RUNNING, PAUSED }
    private GameState gameState = GameState.MENU;

    private int globalTicks = 0;
    private int powerModeTicks = 0;
    private int powerPelletsEaten = 0;
    private boolean isGameOver = false;
    private boolean isGameFrozen = false; // Freeze game when player dies
    private String currentInput = "";
    private int score = 0;
    // Sound players
    private javafx.scene.media.MediaPlayer wakawakaPlayer = null;
    private javafx.scene.media.MediaPlayer wakawakaPlayer2 = null; // For player 2
    private javafx.scene.media.MediaPlayer deathPlayer = null;
    private boolean wasMoving = false; // Track if player 1 was moving to play wakawaka sound
    private boolean wasMoving2 = false; // Track if player 2 was moving to play wakawaka sound
    // Track dots eaten for weighted targeting
    private int dotsEatenP1 = 0;
    private int dotsEatenP2 = 0;
    private Stage primaryStage;

    // Ghost sprite frames
    private Image[] redGhostFrames, pinkGhostFrames, blueGhostFrames, orangeGhostFrames;
    private Image[] afraidFrames;
    private Image[] hurtFrames; // Hurt sprite for ghosts in prison
    // invulnerable Pac-Man frames
    private Image pacOpenInv, pacHalfInv, pacClosedInv;
    // Pacwoman sprite frames (for player 2)
    private Image pacwomanOpen, pacwomanHalf, pacwomanClosed;
    private Image pacwomanOpenInv, pacwomanHalfInv, pacwomanClosedInv;

    // Easy/Normal map (simpler)
    private final int[][] MAP_EASY = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,3,2,2,2,2,2,2,2,2,2,2,2,1,1,2,2,2,2,2,2,2,2,2,2,2,3,1},
            {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
            {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
            {7,2,2,2,2,2,2,2,2,2,2,2,0,0,2,2,2,2,2,2,2,2,2,2,2,2,2,7}, // Tunnel Row
            {1,1,1,1,2,1,1,1,2,1,1,4,5,5,4,1,1,2,1,1,1,2,1,1,1,1,1,1},
            {1,1,1,1,2,1,1,1,2,1,4,4,4,4,4,4,1,2,1,1,1,2,1,1,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,2,1,2,2,2,2,2,2,2,2,2,2,2,1,2,1,1,1,1,1,2,1},
            {1,2,1,1,1,1,2,1,1,1,2,1,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1},
            {1,3,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,3,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // Hard/Insane map (original complex map)
    private final int[][] MAP_HARD = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,2,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
            {1,3,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,3,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,2,1},
            {1,2,2,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,2,2,1},
            {1,1,1,1,1,1,2,1,1,1,1,1,0,1,1,0,1,1,1,1,1,2,1,1,1,1,1,1},
            {0,0,0,0,0,1,2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2,1,0,0,0,0,0},
            {1,1,1,1,1,1,2,1,1,0,1,1,4,5,5,4,1,1,0,1,1,2,1,1,1,1,1,1},
            {7,0,0,0,0,0,2,0,0,0,1,4,4,4,4,4,4,1,0,0,0,2,0,0,0,0,0,7},
            {1,1,1,1,1,1,2,1,1,0,1,4,4,4,4,4,4,1,0,1,1,2,1,1,1,1,1,1},
            {0,0,0,0,0,1,2,1,1,0,1,1,1,1,1,1,1,1,0,1,1,2,1,0,0,0,0,0},
            {1,1,1,1,1,1,2,1,1,0,2,2,2,2,2,2,2,2,0,1,1,2,1,1,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,2,1,1,1,2,1,1,1,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,1,1,1,2,1},
            {1,3,2,2,1,1,2,2,2,2,2,2,2,0,0,2,2,2,2,2,2,2,1,1,2,2,3,1},
            {1,1,1,2,1,1,2,1,1,2,1,1,1,1,1,1,1,1,2,1,1,2,1,1,2,1,1,1},
            {1,2,2,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,1,1,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,1,1,1,1,1,1,2,1,1,2,1,1,1,1,1,1,1,1,1,1,2,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };
    
    // Current map (switched based on difficulty) - initialized in setupGame
    private int[][] MAP;

    private Entity pacman;
    private Entity pacman2;
    private boolean localTwoPlayer = false;
    private boolean player1Alive = true;
    private boolean player2Alive = true;
    private String currentInput2 = "";
    private int score2 = 0;
    private NetworkManager networkManager = null;
    private boolean hostMode = false;
    private boolean networkClientMode = false;
    private List<Ghost> allGhosts = new ArrayList<>();
    // Pac-Man sprite frames
    private Image pacOpen, pacHalf, pacClosed;
    
    // Bonus fruit images (7 fruits: apple, bell, cherries, galaxian, melon, orange, strawberry)
    // Values: 9-15 for fruits, 16 for key (8 is always free)
    private Image[] bonusFruitImages = new Image[7];
    private Image keyImage;
    
    // Key and fruit state
    private boolean player1HasKey = false;
    private boolean player2HasKey = false;
    private boolean keySpawned = false;
    private int transformationTicks = 0; // Counter for transformation timing
    private static final int TRANSFORMATION_INTERVAL = 1800; // ~30 seconds at 60fps
    private static final double FRUIT_TRANSFORMATION_CHANCE = 0.15; // 15% chance
    private static final double KEY_TRANSFORMATION_CHANCE = 0.05; // 5% chance (only in multiplayer)

    @FXML
    public void initialize() {
        // Apply saved difficulty when controller initializes
        currentDifficulty = savedDifficulty;
        
        // Initialize the game map and then size the canvas to fit it so
        // the bottom rows are not clipped by a fixed canvas height.
        // setupGame() will be called, which uses currentDifficulty
        setupGame();
        if (MAP != null && gameCanvas != null) {
            int canvasWidth = MAP[0].length * TILE_SIZE;
            int canvasHeight = MAP.length * TILE_SIZE;
            gameCanvas.setWidth(canvasWidth);
            gameCanvas.setHeight(canvasHeight);
        }
        gc = gameCanvas.getGraphicsContext2D();
        loadPacmanImages();
        loadGhostImages();
        loadBonusFruitImages();
        loadSounds();
        initializeBottomBar();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // update timestamp for interpolation calculations
                if (gameState == GameState.RUNNING && !isGameOver) {
                    update();
                }
                render();
            }
        }.start();
    }

    // Networking helpers
    private final ConcurrentLinkedQueue<String> incomingNetworkInputs = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService stateBroadcaster = null;
    private volatile boolean remoteStateReceived = false;
    // State sequencing for out-of-order protection
    private int stateSeq = 0;
    private volatile int lastReceivedStateSeq = -1;

    // Fixed-tick simulation (60 ticks/second)
    private volatile int currentTick = 0;
    private final int TICKS_PER_SECOND = 60;
    private long tickStartTimeNs = 0L;

    // Client-side prediction
    private volatile int predictedTick = 0;
    private boolean enableClientPrediction = false;

    // Client reconciliation: when server state diverges from prediction, smoothly correct
    private int serverPacketX = -1, serverPacketY = -1;  // last received authoritative position (grid)
    private int predictedX = -1, predictedY = -1;         // current predicted position (grid)
    private double correctionAlpha = 0.0;                 // interpolation factor (0=predicted, 1=server)
    private volatile boolean needsReconciliation = false;

    // Interpolation helpers (client-side)
    private int prevP1px, prevP1py, tgtP1px, tgtP1py;
    private int prevP2px, prevP2py, tgtP2px, tgtP2py;
    private long interpStartNs = 0L;
    private long interpDurationNs = 33_000_000L; // ~33ms

    private void loadPacmanImages() {
        try {
            pacOpen = new Image(Paths.get("src/ressource/pacman/open.png").toUri().toString());
            pacHalf = new Image(Paths.get("src/ressource/pacman/half-open.png").toUri().toString());
            pacClosed = new Image(Paths.get("src/ressource/pacman/closed.png").toUri().toString());
            pacOpenInv = new Image(Paths.get("src/ressource/pacman/open-invulnerable.png").toUri().toString());
            pacHalfInv = new Image(Paths.get("src/ressource/pacman/half-open-invulnerable.png").toUri().toString());
            pacClosedInv = new Image(Paths.get("src/ressource/pacman/closed-invulnerable.png").toUri().toString());
            
            // Load pacwoman sprites for player 2
            pacwomanOpen = new Image(Paths.get("src/ressource/pacwoman/open.png").toUri().toString());
            pacwomanHalf = new Image(Paths.get("src/ressource/pacwoman/half-open.png").toUri().toString());
            pacwomanClosed = new Image(Paths.get("src/ressource/pacwoman/closed.png").toUri().toString());
            pacwomanOpenInv = new Image(Paths.get("src/ressource/pacwoman/open-invulnerable.png").toUri().toString());
            pacwomanHalfInv = new Image(Paths.get("src/ressource/pacwoman/half-open-invulnerable.png").toUri().toString());
            pacwomanClosedInv = new Image(Paths.get("src/ressource/pacwoman/closed-invulnerable.png").toUri().toString());
        } catch (Exception e) {
            System.err.println("Failed to load pacman/pacwoman images: " + e.getMessage());
            pacOpen = pacHalf = pacClosed = null;
            pacOpenInv = pacHalfInv = pacClosedInv = null;
            pacwomanOpen = pacwomanHalf = pacwomanClosed = null;
            pacwomanOpenInv = pacwomanHalfInv = pacwomanClosedInv = null;
        }
    }

    private void loadGhostImages() {
        try {
            redGhostFrames = new Image[8];
            pinkGhostFrames = new Image[8];
            blueGhostFrames = new Image[8];
            orangeGhostFrames = new Image[8];
            for (int i = 0; i < 8; i++) {
                redGhostFrames[i] = new Image(Paths.get("src/ressource/ghosts/red/" + (i+1) + ".png").toUri().toString());
                pinkGhostFrames[i] = new Image(Paths.get("src/ressource/ghosts/pink/" + (i+1) + ".png").toUri().toString());
                blueGhostFrames[i] = new Image(Paths.get("src/ressource/ghosts/blue/" + (i+1) + ".png").toUri().toString());
                orangeGhostFrames[i] = new Image(Paths.get("src/ressource/ghosts/orange/" + (i+1) + ".png").toUri().toString());
            }
            afraidFrames = new Image[2];
            afraidFrames[0] = new Image(Paths.get("src/ressource/ghosts/afraid/1.png").toUri().toString());
            afraidFrames[1] = new Image(Paths.get("src/ressource/ghosts/afraid/2.png").toUri().toString());
            
            // Load hurt frames for ghosts in prison
            hurtFrames = new Image[2];
            hurtFrames[0] = new Image(Paths.get("src/ressource/ghosts/hurt/1.png").toUri().toString());
            hurtFrames[1] = new Image(Paths.get("src/ressource/ghosts/hurt/2.png").toUri().toString());
        } catch (Exception e) {
            System.err.println("Failed to load ghost images: " + e.getMessage());
            redGhostFrames = pinkGhostFrames = blueGhostFrames = orangeGhostFrames = null;
            afraidFrames = null;
            hurtFrames = null;
        }
    }

    private void loadBonusFruitImages() {
        try {
            String[] fruitNames = {"apple", "bell", "cherries", "galaxian", "melon", "orange", "strawberry"};
            for (int i = 0; i < fruitNames.length; i++) {
                bonusFruitImages[i] = new Image(Paths.get("src/ressource/bonus/" + fruitNames[i] + ".png").toUri().toString());
            }
            keyImage = new Image(Paths.get("src/ressource/bonus/key.png").toUri().toString());
        } catch (Exception e) {
            System.err.println("Failed to load bonus fruit images: " + e.getMessage());
            bonusFruitImages = new Image[7];
        }
    }
    
    private void loadSounds() {
        try {
            // Load wakawaka sound for player 1
            String wakawakaPath = Paths.get("src/ressource/sound/wakawaka.mp3").toUri().toString();
            javafx.scene.media.Media wakawakaMedia = new javafx.scene.media.Media(wakawakaPath);
            wakawakaPlayer = new javafx.scene.media.MediaPlayer(wakawakaMedia);
            wakawakaPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
            wakawakaPlayer.setVolume(0.3);
            
            // Load wakawaka sound for player 2 (separate instance for multiplayer)
            wakawakaPlayer2 = new javafx.scene.media.MediaPlayer(wakawakaMedia);
            wakawakaPlayer2.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
            wakawakaPlayer2.setVolume(0.3);
            
            // Load death sound
            String deathPath = Paths.get("src/ressource/sound/death.mp3").toUri().toString();
            javafx.scene.media.Media deathMedia = new javafx.scene.media.Media(deathPath);
            deathPlayer = new javafx.scene.media.MediaPlayer(deathMedia);
            deathPlayer.setVolume(0.5);
        } catch (Exception e) {
            System.err.println("Failed to load sounds: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeBottomBar() {
        // Initialize bottom bar with default values
        if (scoreLabel != null) {
            scoreLabel.setText("Score: 0");
        }
        if (score2Label != null) {
            score2Label.setVisible(false); // Hide score2 label if not in multiplayer
        }
        if (player1CaughtIcon != null) {
            try {
                // Use closed pacman sprite as caught icon
                Image caughtIcon = new Image(Paths.get("src/ressource/pacman/closed.png").toUri().toString());
                player1CaughtIcon.setImage(caughtIcon);
                player1CaughtIcon.setVisible(false);
            } catch (Exception e) {
                System.err.println("Failed to load caught icon: " + e.getMessage());
            }
        }
        if (player2CaughtIcon != null) {
            try {
                // Use closed pacwoman sprite as caught icon
                Image caughtIcon2 = new Image(Paths.get("src/ressource/pacwoman/closed.png").toUri().toString());
                player2CaughtIcon.setImage(caughtIcon2);
                player2CaughtIcon.setVisible(false);
            } catch (Exception e) {
                System.err.println("Failed to load caught icon 2: " + e.getMessage());
            }
        }
    }

    private void updateBottomBar() {
        // Update scores
        if (scoreLabel != null) {
            scoreLabel.setText("Score: " + score);
        }
        if (score2Label != null) {
            if (localTwoPlayer || hostMode || networkClientMode) {
                score2Label.setVisible(true);
                score2Label.setText("Score 2: " + score2);
            } else {
                score2Label.setVisible(false);
            }
        }
        
        // Update caught player icons
        if (player1CaughtIcon != null) {
            player1CaughtIcon.setVisible(!player1Alive);
        }
        if (player2CaughtIcon != null) {
            if (localTwoPlayer || hostMode || networkClientMode) {
                player2CaughtIcon.setVisible(!player2Alive);
            } else {
                player2CaughtIcon.setVisible(false);
            }
        }
    }

    // Public method to re-setup game after difficulty change
    public void setupGameAfterDifficultyChange() {
        setupGame();
        // Resize canvas after setup
        if (MAP != null && gameCanvas != null) {
            int canvasWidth = MAP[0].length * TILE_SIZE;
            int canvasHeight = MAP.length * TILE_SIZE;
            gameCanvas.setWidth(canvasWidth);
            gameCanvas.setHeight(canvasHeight);
        }
    }
    
    private void setupGame() {
        // Initialize or re-initialize map based on current difficulty
        if (currentDifficulty == Difficulty.EASY || currentDifficulty == Difficulty.NORMAL) {
            MAP = deepCopyMap(MAP_EASY);
        } else {
            MAP = deepCopyMap(MAP_HARD);
        }
        
        // Determine spawn positions based on difficulty
        int mapHeight = MAP.length;
        int mapWidth = MAP[0].length;
        
        // Spawn positions determined by difficulty level
        int pacmanSpawnX, pacmanSpawnY, pacman2SpawnX, pacman2SpawnY;
        int ghostSpawnX, ghostSpawnY;

        switch (currentDifficulty) {
            case EASY:
                // Easy map: Bottom middle for pacman, ghosts inside jail
                pacmanSpawnX = 14;
                pacmanSpawnY = 10; // Bottom middle area
                pacman2SpawnX = 13;
                pacman2SpawnY = 10;
                ghostSpawnX = 13;  // Center of jail area
                ghostSpawnY = 6;   // Inside jail (row 6 has jail tiles 4)
                break;
            case NORMAL:
                // Normal map: Standard spawn positions
                pacmanSpawnX = 14;
                pacmanSpawnY = 10; // Row 16 is a clear path in MAP_HARD
                pacman2SpawnX = 13;
                pacman2SpawnY = 10;
                ghostSpawnX = 13;  // Center of jail area
                ghostSpawnY = 10;  // Inside jail
                break;
            case HARD:
                // Hard mode: spawn Pac-Man at y=14 for increased challenge
                pacmanSpawnX = 14;
                pacmanSpawnY = 16; // Hard mode: Pac-Man starts at row 14
                pacman2SpawnX = 13;
                pacman2SpawnY = 16;
                ghostSpawnX = 13;  // Center of jail area
                ghostSpawnY = 11;  // Place ghosts deeper inside jail for Hard mode
                break;
            case INSANE:
                // Insane mode: deepest starting position for maximum challenge
                pacmanSpawnX = 14;
                pacmanSpawnY = 16; // 3 blocks down from row 16
                pacman2SpawnX = 13;
                pacman2SpawnY = 16;
                ghostSpawnX = 13;  // Center of jail area
                ghostSpawnY = 10;  // Inside jail
                break;
            default:
                // Fallback to normal
                pacmanSpawnX = 14;
                pacmanSpawnY = 16;
                pacman2SpawnX = 13;
                pacman2SpawnY = 16;
                ghostSpawnX = 13;
                ghostSpawnY = 10;
                break;
        }
        
        // Clamp spawn positions to map bounds
        pacmanSpawnX = Math.max(0, Math.min(pacmanSpawnX, mapWidth - 1));
        pacmanSpawnY = Math.max(0, Math.min(pacmanSpawnY, mapHeight - 1));
        pacman2SpawnX = Math.max(0, Math.min(pacman2SpawnX, mapWidth - 1));
        pacman2SpawnY = Math.max(0, Math.min(pacman2SpawnY, mapHeight - 1));
        ghostSpawnX = Math.max(0, Math.min(ghostSpawnX, mapWidth - 1));
        ghostSpawnY = Math.max(0, Math.min(ghostSpawnY, mapHeight - 1));
        
        pacman = new Entity(pacmanSpawnX, pacmanSpawnY, Color.YELLOW, false);
        pacman.speed = entitySpeed;
        if (localTwoPlayer || hostMode || networkClientMode) {
            pacman2 = new Entity(pacman2SpawnX, pacman2SpawnY, Color.LIME, false);
            pacman2.speed = entitySpeed;
        } else {
            pacman2 = null;
        }
        player1Alive = true; player2Alive = (pacman2 != null);
        // Set ghost speeds and spawn positions
        // Jail exit: Easy map door at row 4, Hard map door at row 9 (exit path at row 8)
        int ghostJailExitX = ghostSpawnX;
        int ghostJailExitY = (mapHeight == 12) ? 4 : 8; // Easy map: row 4, Hard map: row 8
        
        // Find valid spawn positions in jail area (tiles 4 or 5, or empty space 0)
        List<int[]> validJailPositions = new ArrayList<>();
        for (int r = Math.max(0, ghostSpawnY - 2); r < Math.min(MAP.length, ghostSpawnY + 3); r++) {
            for (int c = Math.max(0, ghostSpawnX - 2); c < Math.min(MAP[0].length, ghostSpawnX + 3); c++) {
                int val = MAP[r][c];
                // Valid jail positions: empty (0), jail floor (4), or jail gate (5)
                if (val == 0 || val == 4 || val == 5) {
                    validJailPositions.add(new int[]{c, r});
                }
            }
        }
        
        // If no valid positions found, use default spawn positions
        if (validJailPositions.isEmpty()) {
            validJailPositions.add(new int[]{ghostSpawnX, ghostSpawnY});
            validJailPositions.add(new int[]{ghostSpawnX + 1, ghostSpawnY});
            validJailPositions.add(new int[]{ghostSpawnX - 1, ghostSpawnY});
            validJailPositions.add(new int[]{ghostSpawnX + 2, ghostSpawnY});
        }
        
        allGhosts.clear();
        // Assign spawn positions to ghosts, ensuring they're in valid jail positions
        int[] blinkyPos = validJailPositions.get(0 % validJailPositions.size());
        Ghost blinky = new Blinky(blinkyPos[0], blinkyPos[1]);
        blinky.speed = entitySpeed;
        blinky.setSpawnPositions(blinkyPos[0], blinkyPos[1], ghostJailExitX, ghostJailExitY);
        allGhosts.add(blinky);
        
        int[] pinkyPos = validJailPositions.get(1 % validJailPositions.size());
        Ghost pinky = new Pinky(pinkyPos[0], pinkyPos[1]);
        pinky.speed = entitySpeed;
        pinky.setSpawnPositions(pinkyPos[0], pinkyPos[1], ghostJailExitX, ghostJailExitY);
        allGhosts.add(pinky);
        
        int[] inkyPos = validJailPositions.get(2 % validJailPositions.size());
        Ghost inky = new Inky(inkyPos[0], inkyPos[1]);
        inky.speed = entitySpeed;
        inky.setSpawnPositions(inkyPos[0], inkyPos[1], ghostJailExitX, ghostJailExitY);
        allGhosts.add(inky);
        
        int[] clydePos = validJailPositions.get(3 % validJailPositions.size());
        Ghost clyde = new Clyde(clydePos[0], clydePos[1]);
        clyde.speed = entitySpeed;
        clyde.setSpawnPositions(clydePos[0], clydePos[1], ghostJailExitX, ghostJailExitY);
        allGhosts.add(clyde);
        
        // Set jail times based on difficulty
        int jailTicks = getJailTicksForDifficulty(currentDifficulty);
        for (Ghost g : allGhosts) {
            g.setJailTime(jailTicks);
        }
        score = 0;
        score2 = 0;
        dotsEatenP1 = 0;
        dotsEatenP2 = 0;
        player1HasKey = false;
        player2HasKey = false;
        keySpawned = false;
        transformationTicks = 0;
    }
    
    private int[][] deepCopyMap(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    // Public method to set difficulty and start single player
    public void initializeSinglePlayer(Difficulty difficulty) {
        savedDifficulty = difficulty;
        currentDifficulty = difficulty;
        localTwoPlayer = false;
        hostMode = false;
        networkClientMode = false;
        setupGame();
    }
    
    // Public method to set difficulty and start local multiplayer
    public void initializeLocalMultiplayer(Difficulty difficulty) {
        savedDifficulty = difficulty;
        currentDifficulty = difficulty;
        this.localTwoPlayer = true;
        setupGame();
    }
    
    // Public method to set difficulty and start as host (online multiplayer)
    public void initializeHostMode(Difficulty difficulty, NetworkManager nm) {
        savedDifficulty = difficulty;
        currentDifficulty = difficulty;
        this.networkManager = nm;
        this.hostMode = true;
        setupGame();
        if (networkManager != null) startHostNetworking();
    }
    
    // Public method to set difficulty and start as client (online multiplayer)
    public void initializeClientMode(Difficulty difficulty, NetworkManager nm) {
        savedDifficulty = difficulty;
        currentDifficulty = difficulty;
        this.networkManager = nm;
        this.networkClientMode = true;
        this.enableClientPrediction = true;
        setupGame();
        if (networkManager != null) startClientNetworking();
    }

    // Legacy methods for backward compatibility
    public void enableLocalTwoPlayer() {
        this.localTwoPlayer = true;
        setupGame();
    }

    public void setNetworkManager(NetworkManager nm) { this.networkManager = nm; }
    public void setHostMode(boolean v) {
        this.hostMode = v;
        setupGame();
        if (v && networkManager != null) startHostNetworking();
    }
    public void enableNetworkClientMode() {
        this.networkClientMode = true;
        this.enableClientPrediction = true;
        setupGame();
        if (networkClientMode && networkManager != null) startClientNetworking();
    }

    private void startHostNetworking() {
        // TCP: wait for client connection (handshake)
        Thread t = new Thread(() -> {
            try {
                java.net.Socket s = networkManager.waitForClient();
                if (s == null) return;
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(s.getInputStream(), "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("INPUT:")) {
                        String dir = line.substring(6);
                        currentInput2 = dir;
                    }
                }
            } catch (Exception e) {
                System.out.println("[GameController] host TCP listener error: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();

        // UDP: set up broadcaster for state snapshots (replaces TCP for game state)
        try {
            UdpNetworkManager udpHost = networkManager.getUdpHost();
            if (udpHost == null) {
                networkManager.initUdpHost(0); // bind to random free port
                udpHost = networkManager.getUdpHost();
            }
            // Send UDP port to client via TCP
            if (udpHost != null) {
                int udpPort = udpHost.getLocalPort();
                networkManager.sendToClient("UDPPORT:" + udpPort);
            }
            // Start UDP receiver for client inputs (tagged with tick)
            udpHost.startReceiver(msg -> {
                if (msg.startsWith("INPUT:")) {
                    String[] parts = msg.split(":");
                    if (parts.length >= 2) {
                        String dir = parts[1];
                        currentInput2 = dir;
                        System.out.println("[GameController] host UDP input: " + msg);
                    }
                } else if ("GAMESTART".equals(msg)) {
                    // Client sent game start (shouldn't happen, host controls start)
                }
            });
        } catch (Exception e) {
            System.out.println("[GameController] host UDP init error: " + e.getMessage());
        }

        stateBroadcaster = Executors.newSingleThreadScheduledExecutor();
        stateBroadcaster.scheduleAtFixedRate(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("STATE:");
                // Player 1 state: gridX,gridY,pixelX,pixelY,lastDir
                sb.append(pacman.gridX).append(',').append(pacman.gridY).append(',').append((int)pacman.pixelX).append(',').append((int)pacman.pixelY).append(',');
                sb.append(pacman.lastDir != null ? pacman.lastDir : "RIGHT").append(',');
                // Player 2 state: gridX,gridY,pixelX,pixelY,lastDir
                if (pacman2 != null) {
                    sb.append(pacman2.gridX).append(',').append(pacman2.gridY).append(',').append((int)pacman2.pixelX).append(',').append((int)pacman2.pixelY).append(',');
                    sb.append(pacman2.lastDir != null ? pacman2.lastDir : "RIGHT").append(',');
                } else {
                    sb.append("-1,-1,-1,-1,RIGHT,");
                }
                // Scores and alive flags
                sb.append(score).append(',').append(score2).append(',');
                sb.append(player1Alive ? '1' : '0').append(',').append(player2Alive ? '1' : '0').append(',');
                // Game mode state
                sb.append(currentMode == GhostMode.FRIGHTENED ? 'F' : (currentMode == GhostMode.CHASE ? 'C' : 'S')).append(',');
                sb.append(powerModeTicks).append(',').append(powerPelletsEaten).append(',');
                // Key states: player1HasKey, player2HasKey, keySpawned
                sb.append(player1HasKey ? '1' : '0').append(',').append(player2HasKey ? '1' : '0').append(',').append(keySpawned ? '1' : '0').append(',');
                // Ghost states: all 4 ghosts with full state (gridX,gridY,pixelX,pixelY,isEaten,inJail,jailTicks,lastDir)
                for (int i = 0; i < 4; i++) {
                    Ghost g = allGhosts.get(i);
                    sb.append(g.gridX).append(',').append(g.gridY).append(',').append((int)g.pixelX).append(',').append((int)g.pixelY).append(',');
                    sb.append(g.isEaten() ? 1 : 0).append(',');
                    sb.append(g.inJail ? 1 : 0).append(',').append(g.jailTicks).append(',');
                    sb.append(g.lastDir != null ? g.lastDir : "RIGHT").append(',');
                }
                // Map state: send compressed map (dots, power pellets, fruits, and keys: row,col,value)
                // Format: mapCount,row1,col1,val1,row2,col2,val2,...
                int mapCount = 0;
                StringBuilder mapData = new StringBuilder();
                for (int r = 0; r < MAP.length; r++) {
                    for (int c = 0; c < MAP[r].length; c++) {
                        int val = MAP[r][c];
                        if (val == 2 || val == 3 || (val >= 9 && val <= 16)) { // dots, power pellets, fruits (9-15), or key (16)
                            mapData.append(r).append(',').append(c).append(',').append(val).append(',');
                            mapCount++;
                        }
                    }
                }
                sb.append(mapCount).append(',').append(mapData);
                // Sequence and ticks for synchronization
                sb.append(stateSeq++).append(',').append(globalTicks).append(',').append(currentTick);
                // Send via both TCP and UDP for redundancy
                String stateMsg = sb.toString();
                networkManager.sendToClient(stateMsg);
                UdpNetworkManager udpHost = networkManager.getUdpHost();
                if (udpHost != null) {
                    udpHost.sendSnapshot(stateMsg);
                }
            } catch (Exception e) {
                System.out.println("[GameController] broadcast error: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    private void startClientNetworking() {
        // TCP: fallback listener for state (legacy)
        Thread tcpThread = new Thread(() -> {
            try {
                java.net.Socket s = networkManager.getClientSocket();
                if (s == null) return;
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(s.getInputStream(), "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("STATE:")) {
                        processStateMessage(line.substring(6));
                    } else if (line.startsWith("UDPPORT:")) {
                        // Host sent UDP port, initialize UDP client
                        try {
                            int udpPort = Integer.parseInt(line.substring(8));
                            String host = s.getInetAddress().getHostAddress();
                            networkManager.initUdpClient(host, udpPort);
                            UdpNetworkManager udpClient = networkManager.getUdpClient();
                            if (udpClient != null) {
                                // Start UDP receiver for state snapshots from host
                                udpClient.startReceiver(msg -> {
                                    if (msg.startsWith("STATE:")) {
                                        processStateMessage(msg.substring(6));
                                    } else if ("GAMESTART".equals(msg)) {
                                        // Host started the game via UDP
                                        Platform.runLater(() -> {
                                            gameState = GameState.RUNNING;
                                            isGameOver = false;
                                        });
                                    }
                                });
                            }
                        } catch (Exception e) {
                            System.out.println("[GameController] client UDP init from port message error: " + e.getMessage());
                        }
                    } else if ("GAMESTART".equals(line)) {
                        // Host started the game, start it for client too
                        Platform.runLater(() -> {
                            gameState = GameState.RUNNING;
                            isGameOver = false;
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("[GameController] client TCP listener error: " + e.getMessage());
            }
        });
        tcpThread.setDaemon(true);
        tcpThread.start();

        // UDP: try to set up if already initialized, otherwise wait for UDPPORT message
        try {
            UdpNetworkManager udpClient = networkManager.getUdpClient();
            if (udpClient != null) {
                // Start UDP receiver for state snapshots from host
                udpClient.startReceiver(msg -> {
                    if (msg.startsWith("STATE:")) {
                        processStateMessage(msg.substring(6));
                    } else if ("GAMESTART".equals(msg)) {
                        // Host started the game via UDP
                        Platform.runLater(() -> {
                            gameState = GameState.RUNNING;
                            isGameOver = false;
                        });
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("[GameController] client UDP init error (will wait for UDPPORT): " + e.getMessage());
        }
    }

    private void processStateMessage(String data) {
        String[] parts = data.split(",");
        try {
            int idx = 0;
            if (parts.length < 15) return; // not enough data for basic state
            
            // Player 1: gridX, gridY, pixelX, pixelY, lastDir
            int p1gx = Integer.parseInt(parts[idx++]);
            int p1gy = Integer.parseInt(parts[idx++]);
            int p1px = Integer.parseInt(parts[idx++]);
            int p1py = Integer.parseInt(parts[idx++]);
            String p1dir = parts[idx++];
            
            // Player 2: gridX, gridY, pixelX, pixelY, lastDir
            int p2gx = Integer.parseInt(parts[idx++]);
            int p2gy = Integer.parseInt(parts[idx++]);
            int p2px = Integer.parseInt(parts[idx++]);
            int p2py = Integer.parseInt(parts[idx++]);
            String p2dir = parts[idx++];
            
            // Scores and alive flags
            int s1 = Integer.parseInt(parts[idx++]);
            int s2 = Integer.parseInt(parts[idx++]);
            int alive1 = Integer.parseInt(parts[idx++]);
            int alive2 = Integer.parseInt(parts[idx++]);
            
            // Game mode state
            String modeStr = parts[idx++];
            int pwrTicks = Integer.parseInt(parts[idx++]);
            int pwrPellets = Integer.parseInt(parts[idx++]);
            
            // Key states: player1HasKey, player2HasKey, keySpawned
            int p1Key = 0, p2Key = 0, keySpawn = 0;
            if (idx < parts.length) {
                try { p1Key = Integer.parseInt(parts[idx++]); } catch (NumberFormatException nfe) {}
            }
            if (idx < parts.length) {
                try { p2Key = Integer.parseInt(parts[idx++]); } catch (NumberFormatException nfe) {}
            }
            if (idx < parts.length) {
                try { keySpawn = Integer.parseInt(parts[idx++]); } catch (NumberFormatException nfe) {}
            }
            
            // Ghost states: 4 ghosts, each with 8 values (gridX,gridY,pixelX,pixelY,isEaten,inJail,jailTicks,lastDir)
            int[][] ghostStates = new int[4][8];
            String[] ghostDirs = new String[4];
            for (int gi = 0; gi < 4; gi++) {
                if (idx + 7 >= parts.length) break;
                ghostStates[gi][0] = Integer.parseInt(parts[idx++]); // gridX
                ghostStates[gi][1] = Integer.parseInt(parts[idx++]); // gridY
                ghostStates[gi][2] = Integer.parseInt(parts[idx++]); // pixelX
                ghostStates[gi][3] = Integer.parseInt(parts[idx++]); // pixelY
                ghostStates[gi][4] = Integer.parseInt(parts[idx++]); // isEaten
                ghostStates[gi][5] = Integer.parseInt(parts[idx++]); // inJail
                ghostStates[gi][6] = Integer.parseInt(parts[idx++]); // jailTicks
                ghostDirs[gi] = parts[idx++]; // lastDir
            }
            
            // Map state: mapCount, then row,col,val pairs
            int mapCount = 0;
            if (idx < parts.length) {
                try {
                    mapCount = Integer.parseInt(parts[idx++]);
                    // Update map with received state - host sends all remaining dots/pellets/fruits/keys
                    // First, reset all dots, power pellets, fruits, and keys that were previously there
                    for (int r = 0; r < MAP.length; r++) {
                        for (int c = 0; c < MAP[r].length; c++) {
                            if (MAP[r][c] == 2 || MAP[r][c] == 3 || (MAP[r][c] >= 9 && MAP[r][c] <= 16)) {
                                MAP[r][c] = 0; // Clear all dots/pellets/fruits/keys first
                            }
                        }
                    }
                    // Then set the ones that still exist (from host's authoritative state)
                    for (int i = 0; i < mapCount && idx + 2 < parts.length; i++) {
                        int r = Integer.parseInt(parts[idx++]);
                        int c = Integer.parseInt(parts[idx++]);
                        int val = Integer.parseInt(parts[idx++]);
                        if (r >= 0 && r < MAP.length && c >= 0 && c < MAP[r].length) {
                            // Restore dots, power pellets, fruits (9-15), and keys (16) that still exist
                            if (val == 2 || val == 3 || (val >= 9 && val <= 16)) {
                                MAP[r][c] = val;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Map parsing failed, continue
                    System.err.println("[GameController] Map state parse error: " + e.getMessage());
                }
            }
            
            // Sequence and ticks
            int receivedSeq = -1;
            int receivedGlobalTick = -1;
            int receivedTick = -1;
            if (idx < parts.length) {
                try { receivedSeq = Integer.parseInt(parts[idx++]); } catch (NumberFormatException nfe) {}
            }
            if (idx < parts.length) {
                try { receivedGlobalTick = Integer.parseInt(parts[idx++]); } catch (NumberFormatException nfe) {}
            }
            if (idx < parts.length) {
                try { receivedTick = Integer.parseInt(parts[idx++]); } catch (NumberFormatException nfe) {}
            }
            
            // If sequence present and not newer, skip applying
            if (receivedSeq >= 0 && receivedSeq <= lastReceivedStateSeq) return;
            if (receivedSeq >= 0) lastReceivedStateSeq = receivedSeq;
            
            // Store values for Platform.runLater
            final int fp1gx = p1gx, fp1gy = p1gy, fp1px = p1px, fp1py = p1py;
            final String fp1dir = p1dir;
            final int fp2gx = p2gx, fp2gy = p2gy, fp2px = p2px, fp2py = p2py;
            final String fp2dir = p2dir;
            final int fs1 = s1, fs2 = s2;
            final int falive1 = alive1, falive2 = alive2;
            final String fmodeStr = modeStr;
            final int fpwrTicks = pwrTicks, fpwrPellets = pwrPellets;
            final int fp1Key = p1Key, fp2Key = p2Key, fkeySpawn = keySpawn;
            final int[][] fghostStates = ghostStates;
            final String[] fghostDirs = ghostDirs;
            final int fReceivedGlobalTick = receivedGlobalTick;
            final int fReceivedTick = receivedTick;
            
            Platform.runLater(() -> {
                try {
                    // Update player 1
                    if (pacman != null && fp1gx >= 0) {
                        pacman.gridX = fp1gx;
                        pacman.gridY = fp1gy;
                        pacman.lastDir = fp1dir;
                        prevP1px = (int)pacman.pixelX;
                        prevP1py = (int)pacman.pixelY;
                        tgtP1px = fp1px;
                        tgtP1py = fp1py;
                    }
                    
                    // Update player 2
                    if (pacman2 != null && fp2gx >= 0) {
                        pacman2.gridX = fp2gx;
                        pacman2.gridY = fp2gy;
                        pacman2.lastDir = fp2dir;
                        prevP2px = (int)pacman2.pixelX;
                        prevP2py = (int)pacman2.pixelY;
                        tgtP2px = fp2px;
                        tgtP2py = fp2py;
                    }
                    
                    // Update scores and alive flags
                    score = fs1;
                    score2 = fs2;
                    player1Alive = (falive1 == 1);
                    player2Alive = (falive2 == 1);
                    
                    // Update game mode
                    if (fmodeStr.equals("F")) {
                        currentMode = GhostMode.FRIGHTENED;
                    } else if (fmodeStr.equals("C")) {
                        currentMode = GhostMode.CHASE;
                    } else {
                        currentMode = GhostMode.SCATTER;
                    }
                    powerModeTicks = fpwrTicks;
                    powerPelletsEaten = fpwrPellets;
                    
                    // Update key states
                    player1HasKey = (fp1Key == 1);
                    player2HasKey = (fp2Key == 1);
                    keySpawned = (fkeySpawn == 1);
                    
                    // Update ghost states
                    for (int gi = 0; gi < 4 && gi < allGhosts.size(); gi++) {
                        Ghost g = allGhosts.get(gi);
                        g.gridX = fghostStates[gi][0];
                        g.gridY = fghostStates[gi][1];
                        g.pixelX = fghostStates[gi][2];
                        g.pixelY = fghostStates[gi][3];
                        g.setEaten(fghostStates[gi][4] == 1);
                        g.inJail = (fghostStates[gi][5] == 1);
                        g.jailTicks = fghostStates[gi][6];
                        g.lastDir = fghostDirs[gi];
                    }
                    
                    // Sync ticks
                    if (fReceivedGlobalTick >= 0) globalTicks = fReceivedGlobalTick;
                    if (fReceivedTick >= 0) currentTick = fReceivedTick;
                    
                    // Start interpolation
                    interpStartNs = System.nanoTime();
                    needsReconciliation = true;
                    remoteStateReceived = true;
                } catch (Exception ex) {
                    System.err.println("[GameController] Error applying state: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        } catch (NumberFormatException nfe) {
            System.err.println("[GameController] Failed to parse state message: " + nfe.getMessage());
        }
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Calculates the proper scene dimensions based on the current map size.
     * Returns width and height as a double array [width, height].
     */
    public double[] getSceneDimensions() {
        if (MAP == null || gameCanvas == null) {
            // Default fallback dimensions
            return new double[]{700, 560};
        }
        double canvasWidth = gameCanvas.getWidth();
        double canvasHeight = gameCanvas.getHeight();
        // Bottom bar is 60px high
        double bottomBarHeight = 60.0;
        return new double[]{canvasWidth, canvasHeight + bottomBarHeight};
    }

    public void startGame() {
        gameState = GameState.RUNNING;
        isGameOver = false;
    }

    public void setInitialPacmanSpeed(double s) {
        entitySpeed = s;
        if (pacman != null) pacman.speed = s;
        if (pacman2 != null) pacman2.speed = s;
        for (Ghost g : allGhosts) {
            g.speed = s;
        }
    }
    
    private int getJailTicksForDifficulty(Difficulty diff) {
        switch (diff) {
            case EASY: return 600;   // 10 seconds (600 ticks @ 60fps)
            case NORMAL: return 480; // 8 seconds (480 ticks @ 60fps)
            case HARD: return 360;   // 6 seconds (360 ticks @ 60fps)
            case INSANE: return 240; // 4 seconds (240 ticks @ 60fps)
            default: return 600;
        }
    }
    
    public void setDifficulty(Difficulty diff) {
        this.currentDifficulty = diff;
        savedDifficulty = diff; // Save difficulty for persistence across scene changes
        // Set difficulty-specific parameters
        switch (diff) {
            case EASY:
                entitySpeed = 0.75;
                scatterDuration = 10.0;
                chaseDuration = 10.0;
                pointMultiplier = 0.8;
                break;
            case NORMAL:
                entitySpeed = 0.75;
                scatterDuration = 8.0;
                chaseDuration = 12.0;
                pointMultiplier = 1.0;
                break;
            case HARD:
                entitySpeed = 0.9;
                scatterDuration = 7.0;
                chaseDuration = 15.0;
                pointMultiplier = 1.2;
                break;
            case INSANE:
                entitySpeed = 1.05;
                scatterDuration = 6.0;
                chaseDuration = 20.0;
                pointMultiplier = 1.5;
                break;
        }
        // Update map based on difficulty
        if (diff == Difficulty.EASY || diff == Difficulty.NORMAL) {
            MAP = deepCopyMap(MAP_EASY);
        } else {
            MAP = deepCopyMap(MAP_HARD);
        }
        // Resize canvas to match new map dimensions
        if (MAP != null && gameCanvas != null) {
            int canvasWidth = MAP[0].length * TILE_SIZE;
            int canvasHeight = MAP.length * TILE_SIZE;
            gameCanvas.setWidth(canvasWidth);
            gameCanvas.setHeight(canvasHeight);
        }
        // Apply speed to entities
        setInitialPacmanSpeed(entitySpeed);
        // Update jail times for all ghosts
        int jailTicks = getJailTicksForDifficulty(diff);
        for (Ghost g : allGhosts) {
            g.setJailTime(jailTicks);
        }
    }

    private void update() {
        // Don't update if game is frozen (player died, waiting for death sound)
        if (isGameFrozen) {
            return;
        }
        
        globalTicks++;
        // Only host calculates game mode - client receives it from network
        if (!networkClientMode) {
            updateGlobalMode();
        }

        // Client-side reconciliation: smoothly correct predicted position when server state differs
        if (networkClientMode && enableClientPrediction && needsReconciliation && pacman2 != null) {
            if (serverPacketX >= 0 && predictedX >= 0) {
                // Check if prediction diverged from server authoritative position
                if (serverPacketX != predictedX || serverPacketY != predictedY) {
                    // Smoothly interpolate grid position toward server position over ~100ms
                    correctionAlpha = Math.min(1.0, correctionAlpha + 0.033); // ~33ms per frame
                    pacman2.gridX = predictedX + (int)((serverPacketX - predictedX) * correctionAlpha);
                    pacman2.gridY = predictedY + (int)((serverPacketY - predictedY) * correctionAlpha);
                }
                if (correctionAlpha >= 1.0) {
                    needsReconciliation = false;
                    correctionAlpha = 0.0;
                }
            }
        }

        // Client-side interpolation: smoothly move displayed pixels toward last received targets
        if (networkClientMode && remoteStateReceived) {
            long nowNs = System.nanoTime();
            double t = 1.0;
            if (interpStartNs > 0) {
                t = Math.min(1.0, (double)(nowNs - interpStartNs) / (double)interpDurationNs);
            }
            try {
                if (pacman != null) {
                    pacman.pixelX = prevP1px + (t * (tgtP1px - prevP1px));
                    pacman.pixelY = prevP1py + (t * (tgtP1py - prevP1py));
                }
                if (pacman2 != null) {
                    pacman2.pixelX = prevP2px + (t * (tgtP2px - prevP2px));
                    pacman2.pixelY = prevP2py + (t * (tgtP2py - prevP2py));
                }
            } catch (Exception ignored) {}
        }
        // If client mode, host is authoritative and client receives STATE updates instead of simulating locally
        boolean player1Moving = false;
        if (!networkClientMode && player1Alive) {
            pacman.move(currentInput, MAP);
            // Check if player is actually moving
            player1Moving = (currentInput != null && !currentInput.isEmpty()) || 
                           (pacman.pixelX != pacman.gridX * TILE_SIZE || pacman.pixelY != pacman.gridY * TILE_SIZE);
        }
        if (pacman.ateDot) {
            score += 10;
            dotsEatenP1++;
            pacman.ateDot = false;
        }
        
        // Play wakawaka sound when player moves
        if (player1Moving && !wasMoving) {
            if (wakawakaPlayer != null) {
                wakawakaPlayer.play();
            }
            wasMoving = true;
        } else if (!player1Moving && wasMoving) {
            if (wakawakaPlayer != null) {
                wakawakaPlayer.stop();
            }
            wasMoving = false;
        }
        // Move pacman2 in multiplayer mode (localTwoPlayer, hostMode, or networkClientMode)
        boolean player2Moving = false;
        if ((localTwoPlayer || hostMode || networkClientMode) && pacman2 != null) {
            // On host, pacman2 moves from inputs (including remote inputs applied to currentInput2)
            if (player2Alive && !networkClientMode) {
                pacman2.move(currentInput2, MAP);
                // Check if player 2 is actually moving
                player2Moving = (currentInput2 != null && !currentInput2.isEmpty()) || 
                               (pacman2.pixelX != pacman2.gridX * TILE_SIZE || pacman2.pixelY != pacman2.gridY * TILE_SIZE);
            }
            // On client: move prediction locally while waiting for server correction
            if (networkClientMode && enableClientPrediction && player2Alive) {
                pacman2.move(currentInput2, MAP);
                // Store predicted position for reconciliation
                predictedX = pacman2.gridX;
                predictedY = pacman2.gridY;
                // Check if player 2 is actually moving
                player2Moving = (currentInput2 != null && !currentInput2.isEmpty()) || 
                               (pacman2.pixelX != pacman2.gridX * TILE_SIZE || pacman2.pixelY != pacman2.gridY * TILE_SIZE);
            }

            if (pacman2.ateDot) {
                score2 += (int)(10 * pointMultiplier);
                dotsEatenP2++;
                pacman2.ateDot = false;
            }
        }
        
        // Play wakawaka sound when player 2 moves
        if (player2Moving && !wasMoving2) {
            if (wakawakaPlayer2 != null) {
                wakawakaPlayer2.play();
            }
            wasMoving2 = true;
        } else if (!player2Moving && wasMoving2) {
            if (wakawakaPlayer2 != null) {
                wakawakaPlayer2.stop();
            }
            wasMoving2 = false;
        }


        if (powerModeTicks > 0) {
            powerModeTicks--;
            currentMode = GhostMode.FRIGHTENED;
        }

        int activeCount = (powerPelletsEaten >= 2) ? 4 : (powerPelletsEaten >= 1) ? 3 : 2;
        Ghost blinky = allGhosts.get(0);

        for (int i = 0; i < activeCount; i++) {
            Ghost g = allGhosts.get(i);
            // Weighted targeting: Each ghost uses personality-based logic in multiplayer
            Entity primary = pacman;
            boolean useMultiplayer = (pacman2 != null && player2Alive && player1Alive);
            
            if (useMultiplayer) {
                // Use personality-based targeting for each ghost
                g.setTargetMultiplayer(pacman, pacman2, blinky, dotsEatenP1, dotsEatenP2);
            } else {
                // Single player or only one alive - use standard targeting
                if (pacman2 != null && player2Alive && !player1Alive) {
                    primary = pacman2;
                }
                g.setTarget(primary, blinky);
            }
            
            // Only host runs ghost AI - client receives authoritative ghost positions
            if (!networkClientMode) {
                // Update behavior with appropriate target
                if (useMultiplayer && currentMode == GameController.GhostMode.FRIGHTENED) {
                    // Frightened mode: flee from center of mass of both players
                    int centerX = (pacman.gridX + pacman2.gridX) / 2;
                    int centerY = (pacman.gridY + pacman2.gridY) / 2;
                    // Flee in opposite direction from center of mass
                    int dx = g.gridX - centerX;
                    int dy = g.gridY - centerY;
                    // Normalize and extend
                    double dist = Math.hypot(dx, dy);
                    if (dist > 0.1) {
                        g.targetX = g.gridX + (int)(dx / dist * 10);
                        g.targetY = g.gridY + (int)(dy / dist * 10);
                        // Clamp to map bounds
                        if (g.targetX < 0) g.targetX = 0;
                        if (g.targetX >= MAP[0].length) g.targetX = MAP[0].length - 1;
                        if (g.targetY < 0) g.targetY = 0;
                        if (g.targetY >= MAP.length) g.targetY = MAP.length - 1;
                    } else {
                        // Random if too close
                        g.targetX = (int)(Math.random() * 28);
                        g.targetY = (int)(Math.random() * 22);
                    }
                }
                g.updateBehavior(MAP, primary, blinky, currentMode);
                
                // Collisions: only host processes collisions (authoritative)
                if (player1Alive) {
                    double dist1 = Math.hypot(pacman.pixelX - g.pixelX, pacman.pixelY - g.pixelY);
                    if (dist1 < TILE_SIZE * 0.7) {
                        if (currentMode == GhostMode.FRIGHTENED) {
                            if (!g.isEaten()) { g.setEaten(true); score += (int)(200 * pointMultiplier); }
                        } else {
                            // player1 caught - freeze game and play death sound
                            player1Alive = false;
                            handlePlayerDeath();
                        }
                    }
                }
                if (pacman2 != null && player2Alive) {
                    double dist2 = Math.hypot(pacman2.pixelX - g.pixelX, pacman2.pixelY - g.pixelY);
                    if (dist2 < TILE_SIZE * 0.7) {
                        if (currentMode == GhostMode.FRIGHTENED) {
                            if (!g.isEaten()) { g.setEaten(true); score2 += (int)(200 * pointMultiplier); }
                        } else {
                            // player2 caught - freeze game and play death sound
                            player2Alive = false;
                            handlePlayerDeath();
                        }
                    }
                }
            }
            // Client: collisions are handled by host and synchronized via network state
        }

        // Check win condition: all collectibles collected (only host checks)
        if (!networkClientMode) {
            boolean allCollected = true;
            for (int r = 0; r < MAP.length; r++) {
                for (int c = 0; c < MAP[r].length; c++) {
                    int val = MAP[r][c];
                    // Check for dots (2), power pellets (3), fruits (9-15), or keys (16)
                    if (val == 2 || val == 3 || (val >= 9 && val <= 16)) {
                        allCollected = false;
                        break;
                    }
                }
                if (!allCollected) break;
            }
            if (allCollected && !isGameOver) {
                // Win condition met - show win screen
                showWinScreen();
                return; // Don't continue game updates
            }
        }

        // If both players are dead, mark game over
        if (!player1Alive && (pacman2 == null || !player2Alive)) {
            isGameOver = true;
        }

        // Power pellet collection: only host processes (authoritative)
        if (!networkClientMode) {
            // Player 1 power pellet collection
            if (player1Alive && MAP[pacman.gridY][pacman.gridX] == 3) {
                MAP[pacman.gridY][pacman.gridX] = 0;
                powerModeTicks = 720; // 12 seconds @ 60fps
                powerPelletsEaten++;
                score += (int)(50 * pointMultiplier);
            }
            // Player 2 power pellet collection
            if (pacman2 != null && player2Alive && MAP[pacman2.gridY][pacman2.gridX] == 3) {
                MAP[pacman2.gridY][pacman2.gridX] = 0;
                powerModeTicks = 720; // 12 seconds @ 60fps
                powerPelletsEaten++;
                score2 += (int)(50 * pointMultiplier);
            }
            
            // Rare transformation: dots to fruits or key (only host processes)
            transformationTicks++;
            if (transformationTicks >= TRANSFORMATION_INTERVAL) {
                transformationTicks = 0;
                // Find all dots and randomly transform one
                List<int[]> dotPositions = new ArrayList<>();
                for (int r = 0; r < MAP.length; r++) {
                    for (int c = 0; c < MAP[r].length; c++) {
                        if (MAP[r][c] == 2) { // dot
                            dotPositions.add(new int[]{r, c});
                        }
                    }
                }
                if (!dotPositions.isEmpty() && Math.random() < FRUIT_TRANSFORMATION_CHANCE) {
                    int[] pos = dotPositions.get((int)(Math.random() * dotPositions.size()));
                    // Transform to random fruit (9-15, values 9-15 represent fruits, 8 is always free)
                    int fruitType = 9 + (int)(Math.random() * 7); // 9-15
                    MAP[pos[0]][pos[1]] = fruitType;
                }
                // Key transformation (only in multiplayer, only one per game)
                if ((localTwoPlayer || hostMode) && !keySpawned && !dotPositions.isEmpty() && Math.random() < KEY_TRANSFORMATION_CHANCE) {
                    int[] pos = dotPositions.get((int)(Math.random() * dotPositions.size()));
                    MAP[pos[0]][pos[1]] = 16; // 16 = key
                    keySpawned = true;
                }
            }
            
            // Bonus fruit collection (values 9-15)
            if (player1Alive) {
                int val = MAP[pacman.gridY][pacman.gridX];
                if (val >= 9 && val <= 15) {
                    MAP[pacman.gridY][pacman.gridX] = 0;
                    int bonusPoints = (val - 8) * 100; // 100, 200, 300, 400, 500, 600, 700 points
                    score += (int)(bonusPoints * pointMultiplier);
                }
            }
            if (pacman2 != null && player2Alive) {
                int val = MAP[pacman2.gridY][pacman2.gridX];
                if (val >= 9 && val <= 15) {
                    MAP[pacman2.gridY][pacman2.gridX] = 0;
                    int bonusPoints = (val - 8) * 100;
                    score2 += (int)(bonusPoints * pointMultiplier);
                }
            }
            
            // Key collection (value 16)
            if (player1Alive && MAP[pacman.gridY][pacman.gridX] == 16) {
                MAP[pacman.gridY][pacman.gridX] = 0;
                player1HasKey = true;
                score += (int)(1000 * pointMultiplier);
            }
            if (pacman2 != null && player2Alive && MAP[pacman2.gridY][pacman2.gridX] == 16) {
                MAP[pacman2.gridY][pacman2.gridX] = 0;
                player2HasKey = true;
                score2 += (int)(1000 * pointMultiplier);
            }
            
            // Key-based rescue mechanic: if player has key and passes within 8 tiles of caught player
            if ((localTwoPlayer || hostMode) && pacman2 != null) {
                if (player1HasKey && !player2Alive) {
                    double dist = Math.hypot(pacman.gridX - pacman2.gridX, pacman.gridY - pacman2.gridY);
                    if (dist <= 8) {
                        player2Alive = true;
                        player1HasKey = false;
                        // Respawn player2 at safe location
                        pacman2.gridX = pacman.gridX;
                        pacman2.gridY = pacman.gridY;
                        pacman2.pixelX = pacman2.gridX * TILE_SIZE;
                        pacman2.pixelY = pacman2.gridY * TILE_SIZE;
                    }
                }
                if (player2HasKey && !player1Alive) {
                    double dist = Math.hypot(pacman2.gridX - pacman.gridX, pacman2.gridY - pacman.gridY);
                    if (dist <= 8) {
                        player1Alive = true;
                        player2HasKey = false;
                        // Respawn player1 at safe location
                        pacman.gridX = pacman2.gridX;
                        pacman.gridY = pacman2.gridY;
                        pacman.pixelX = pacman.gridX * TILE_SIZE;
                        pacman.pixelY = pacman.gridY * TILE_SIZE;
                    }
                }
            }
        }

        if (isGameOver && !isGameFrozen) {
            // save score and return to menu
            String gameMode = (localTwoPlayer || hostMode || networkClientMode) ? "DUO" : "SOLO";
            String difficulty = currentDifficulty.toString();
            ScoreManager.addScore(score, difficulty, gameMode);
            // switch back to menu scene on FX thread
            javafx.application.Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
                    Parent root = loader.load();
                    MenuController mc = loader.getController();
                    if (primaryStage != null) {
                        mc.setPrimaryStage(primaryStage);
                        mc.setLastScore(score);
                        mc.refreshScoresDisplay();
                        Scene scene = new Scene(root);
                        scene.setOnKeyPressed(e -> mc.handleKey(e));
                        primaryStage.setScene(scene);
                        primaryStage.show();
                        root.requestFocus();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }
    
    private void handlePlayerDeath() {
        // Stop wakawaka sounds for both players
        if (wakawakaPlayer != null) {
            wakawakaPlayer.stop();
        }
        if (wakawakaPlayer2 != null) {
            wakawakaPlayer2.stop();
        }
        wasMoving = false;
        wasMoving2 = false;
        
        // Freeze the game
        isGameFrozen = true;
        
        // Play death sound
        if (deathPlayer != null) {
            deathPlayer.stop(); // Stop if already playing
            deathPlayer.play();
            
            // Wait for death sound to finish, then end game
            deathPlayer.setOnEndOfMedia(() -> {
                isGameFrozen = false;
                // Check if both players are dead
                if (!player1Alive && (pacman2 == null || !player2Alive)) {
                    isGameOver = true;
                }
            });
        } else {
            // If no sound, just wait a bit then end
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            delay.setOnFinished(e -> {
                isGameFrozen = false;
                if (!player1Alive && (pacman2 == null || !player2Alive)) {
                    isGameOver = true;
                }
            });
            delay.play();
        }
    }

    private void showWinScreen() {
        // Save score and show win screen
        String gameMode = (localTwoPlayer || hostMode || networkClientMode) ? "DUO" : "SOLO";
        String difficulty = currentDifficulty.toString();
        ScoreManager.addScore(score, difficulty, gameMode);
        if (pacman2 != null && score2 > 0) {
            ScoreManager.addScore(score2, difficulty, gameMode);
        }
        isGameOver = true;
        gameState = GameState.PAUSED;
        
        // Wait 3 seconds to show win screen, then transition to menu
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        delay.setOnFinished(e -> {
            javafx.application.Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("menu.fxml"));
                    Parent root = loader.load();
                    MenuController mc = loader.getController();
                    if (primaryStage != null) {
                        mc.setPrimaryStage(primaryStage);
                        mc.setLastScore(Math.max(score, score2));
                        mc.refreshScoresDisplay();
                        Scene scene = new Scene(root);
                        scene.setOnKeyPressed(ev -> mc.handleKey(ev));
                        primaryStage.setScene(scene);
                        primaryStage.show();
                        root.requestFocus();
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        });
        javafx.application.Platform.runLater(() -> delay.play());
    }

    private void updateGlobalMode() {
        if (powerModeTicks > 0) return;

        double seconds = globalTicks / 60.0;
        double totalCycle = scatterDuration + chaseDuration;
        double cycleTime = seconds % totalCycle;
        currentMode = (cycleTime < scatterDuration) ? GhostMode.SCATTER : GhostMode.CHASE;
    }

    private void render() {
        gc.setFill(Color.web("#000033"));
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        for (int r = 0; r < MAP.length; r++) {
            for (int c = 0; c < MAP[r].length; c++) {
                double x = c * TILE_SIZE; double y = r * TILE_SIZE;
                if (MAP[r][c] == 1) {
                    gc.setFill(Color.BLUE);
                    gc.fillRoundRect(x+2, y+2, TILE_SIZE-4, TILE_SIZE-4, 10, 10);
                } else if (MAP[r][c] == 2) {
                    gc.setFill(Color.web("#ffb8ae"));
                    gc.fillOval(x + 11, y + 11, 4, 4);
                } else if (MAP[r][c] == 3) {
                    gc.setFill(Color.WHITE);
                    gc.fillOval(x + 7, y + 7, 11, 11);
                } else if (MAP[r][c] >= 9 && MAP[r][c] <= 15) {
                    // Bonus fruits (9-15)
                    int fruitIndex = MAP[r][c] - 9;
                    if (fruitIndex < bonusFruitImages.length && bonusFruitImages[fruitIndex] != null) {
                        gc.drawImage(bonusFruitImages[fruitIndex], x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                    } else {
                        // Fallback: draw colored circle
                        gc.setFill(Color.ORANGE);
                        gc.fillOval(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                    }
                } else if (MAP[r][c] == 16) {
                    // Key
                    if (keyImage != null) {
                        gc.drawImage(keyImage, x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                    } else {
                        // Fallback: draw yellow key shape
                        gc.setFill(Color.YELLOW);
                        gc.fillRect(x + 8, y + 5, 4, 15);
                        gc.fillOval(x + 5, y + 18, 10, 4);
                    }
                } else if (MAP[r][c] == 5) {
                    gc.setStroke(Color.PINK);
                    gc.strokeLine(x, y + TILE_SIZE/2, x + TILE_SIZE, y + TILE_SIZE/2);
                }
            }
        }

        // Draw Pac-Man using sprite frames (only if alive)
        if (player1Alive) {
            Image pacImg = null;
            boolean usingInv = (currentMode == GhostMode.FRIGHTENED && pacOpenInv != null);
            int pacFrame;
            boolean pacMoving = !(pacman.pixelX == pacman.gridX * TILE_SIZE && pacman.pixelY == pacman.gridY * TILE_SIZE) || (currentInput != null && !currentInput.isEmpty());
            if (!pacMoving) {
                pacFrame = 1; // steady half-open mouth when not moving
            } else {
                pacFrame = (globalTicks / 18) % 3; // original/slower animation while moving
            }
            if (usingInv && pacOpenInv != null && pacHalfInv != null && pacClosedInv != null) {
                pacImg = (pacFrame == 0) ? pacOpenInv : (pacFrame == 1) ? pacHalfInv : pacClosedInv;
            } else if (pacOpen != null && pacHalf != null && pacClosed != null) {
                pacImg = (pacFrame == 0) ? pacOpen : (pacFrame == 1) ? pacHalf : pacClosed;
            }
            if (pacImg != null) {
                double w = TILE_SIZE - 4, h = TILE_SIZE - 4;
                double centerX = pacman.pixelX + 2 + w/2;
                double centerY = pacman.pixelY + 2 + h/2;
                double angle = 0;
                if (pacman.lastDir != null) {
                    if (pacman.lastDir.equals("UP")) angle = -90;
                    else if (pacman.lastDir.equals("DOWN")) angle = 90;
                    else if (pacman.lastDir.equals("LEFT")) angle = 180;
                    else angle = 0; // RIGHT or default
                }
                gc.save();
                gc.translate(centerX, centerY);
                gc.rotate(angle);
                gc.drawImage(pacImg, -w/2, -h/2, w, h);
                gc.restore();
            } else {
                gc.setFill(pacman.color);
                gc.fillOval(pacman.pixelX+2, pacman.pixelY+2, TILE_SIZE-4, TILE_SIZE-4);
            }
        }

        // draw second player (Pacwoman) if in multiplayer mode (local or online) and alive
        if ((localTwoPlayer || hostMode || networkClientMode) && pacman2 != null && player2Alive) {
            Image p2img = null;
            boolean usingInv2 = (currentMode == GhostMode.FRIGHTENED && pacwomanOpenInv != null);
            int p2Frame;
            boolean p2Moving = !(pacman2.pixelX == pacman2.gridX * TILE_SIZE && pacman2.pixelY == pacman2.gridY * TILE_SIZE) || (currentInput2 != null && !currentInput2.isEmpty());
            if (!p2Moving) {
                p2Frame = 1; // steady half-open mouth when not moving
            } else {
                p2Frame = (globalTicks / 18) % 3; // animation while moving
            }
            if (usingInv2 && pacwomanOpenInv != null && pacwomanHalfInv != null && pacwomanClosedInv != null) {
                p2img = (p2Frame == 0) ? pacwomanOpenInv : (p2Frame == 1) ? pacwomanHalfInv : pacwomanClosedInv;
            } else if (pacwomanOpen != null && pacwomanHalf != null && pacwomanClosed != null) {
                p2img = (p2Frame == 0) ? pacwomanOpen : (p2Frame == 1) ? pacwomanHalf : pacwomanClosed;
            }
            if (p2img != null) {
                double w2 = TILE_SIZE - 4, h2 = TILE_SIZE - 4;
                double centerX2 = pacman2.pixelX + 2 + w2/2;
                double centerY2 = pacman2.pixelY + 2 + h2/2;
                double angle2 = 0;
                if (pacman2.lastDir != null) {
                    if (pacman2.lastDir.equals("UP")) angle2 = -90;
                    else if (pacman2.lastDir.equals("DOWN")) angle2 = 90;
                    else if (pacman2.lastDir.equals("LEFT")) angle2 = 180;
                    else angle2 = 0;
                }
                gc.save(); gc.translate(centerX2, centerY2); gc.rotate(angle2);
                gc.drawImage(p2img, -w2/2, -h2/2, w2, h2);
                gc.restore();
            } else {
                gc.setFill(pacman2.color);
                gc.fillOval(pacman2.pixelX+2, pacman2.pixelY+2, TILE_SIZE-4, TILE_SIZE-4);
            }
        }

        int activeCount = (powerPelletsEaten >= 2) ? 4 : (powerPelletsEaten >= 1) ? 3 : 2;
        for (int i = 0; i < activeCount; i++) {
            Ghost g = allGhosts.get(i);
            Image[] frames = null;
            // Use hurt sprite when ghost is in jail
            if (g.inJail && hurtFrames != null) {
                frames = hurtFrames;
            } else if (currentMode == GhostMode.FRIGHTENED && afraidFrames != null) frames = afraidFrames;
            else if (g.color.equals(Color.RED) && redGhostFrames != null) frames = redGhostFrames;
            else if (g.color.equals(Color.PINK) && pinkGhostFrames != null) frames = pinkGhostFrames;
            else if (g.color.equals(Color.CYAN) && blueGhostFrames != null) frames = blueGhostFrames;
            else if (g.color.equals(Color.ORANGE) && orangeGhostFrames != null) frames = orangeGhostFrames;

            if (frames != null && frames.length >= 8) {
                // Frame pairs mapping: 1-2 = right (0-1), 3-4 = left (2-3), 5-6 = up (4-5), 7-8 = down (6-7)
                // For hurt frames (2 frames), just animate between them
                if (g.inJail && frames.length == 2) {
                    int gi = (globalTicks / 18) % 2;
                    Image gImg = frames[gi];
                    gc.drawImage(gImg, g.pixelX+2, g.pixelY+2, TILE_SIZE-4, TILE_SIZE-4);
                } else {
                    int dirStart = 0;
                    String d = g.lastDir != null ? g.lastDir : "RIGHT";
                    switch (d) {
                        case "LEFT" -> dirStart = 2;
                        case "UP" -> dirStart = 4;
                        case "DOWN" -> dirStart = 6;
                        default -> dirStart = 0; // RIGHT
                    }
                    int pairIndex = (globalTicks / 18) % 2; // choose first or second of the pair
                    int gi = (dirStart + pairIndex) % frames.length;
                    Image gImg = frames[gi];
                    gc.drawImage(gImg, g.pixelX+2, g.pixelY+2, TILE_SIZE-4, TILE_SIZE-4);
                }
            } else if (frames != null && frames.length > 0) {
                int gi = (globalTicks / 18) % frames.length; // fallback
                Image gImg = frames[gi];
                gc.drawImage(gImg, g.pixelX+2, g.pixelY+2, TILE_SIZE-4, TILE_SIZE-4);
            } else {
                gc.setFill(currentMode == GhostMode.FRIGHTENED ? Color.BLUEVIOLET : g.color);
                gc.fillRoundRect(g.pixelX+2, g.pixelY+2, TILE_SIZE-4, TILE_SIZE-4, 15, 15);
            }
        }

        // Draw visualization: Manhattan/A* path from ghost to its current target
        gc.setLineWidth(2);
        gc.setStroke(Color.rgb(255,0,0,160/255.0));
        for (int i = 0; i < activeCount; i++) {
            Ghost g = allGhosts.get(i);
            try {
                List<int[]> path = findPath(g.gridX, g.gridY, g.targetX, g.targetY, MAP, g.isEaten());
                if (path != null && path.size() > 1) {
                    for (int k = 0; k < path.size() - 1; k++) {
                        int[] a = path.get(k);
                        int[] b = path.get(k+1);
                        double ax = a[0] * TILE_SIZE + TILE_SIZE/2.0;
                        double ay = a[1] * TILE_SIZE + TILE_SIZE/2.0;
                        double bx = b[0] * TILE_SIZE + TILE_SIZE/2.0;
                        double by = b[1] * TILE_SIZE + TILE_SIZE/2.0;
                        gc.strokeLine(ax, ay, bx, by);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Update bottom bar
        updateBottomBar();

        // Overlays: menu / paused / game over
        if (gameState == GameState.MENU) {
            gc.setFill(Color.WHITE);
            gc.fillText("PAC-MAN", 300, 180);
            // Only show "Press ENTER to Start" for single player mode
            if (!hostMode && !networkClientMode && !localTwoPlayer) {
                gc.fillText("Press ENTER to Start", 275, 220);
            } else {
                gc.fillText("Waiting for game to start...", 250, 220);
            }
            gc.fillText("Use arrow keys to move after starting.", 230, 240);
            gc.fillText("Press ESC to quit.", 300, 260);
        } else if (gameState == GameState.PAUSED) {
            gc.setFill(Color.YELLOW);
            gc.fillText("PAUSED - Press P to resume", 250, 260);
        }

        if (isGameOver) {
            // Check if it's a win (all collectibles collected) or game over
            boolean allCollected = true;
            for (int r = 0; r < MAP.length; r++) {
                for (int c = 0; c < MAP[r].length; c++) {
                    int val = MAP[r][c];
                    if (val == 2 || val == 3 || (val >= 9 && val <= 16)) {
                        allCollected = false;
                        break;
                    }
                }
                if (!allCollected) break;
            }
            
            if (allCollected) {
                // Win screen
                gc.setFill(Color.rgb(0, 0, 0, 0.8)); // Semi-transparent black overlay
                gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
                gc.setFill(Color.GOLD);
                gc.setFont(javafx.scene.text.Font.font(48));
                gc.fillText("YOU WIN!", gameCanvas.getWidth() / 2 - 120, gameCanvas.getHeight() / 2 - 50);
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font(24));
                gc.fillText("All collectibles collected!", gameCanvas.getWidth() / 2 - 180, gameCanvas.getHeight() / 2 + 20);
                gc.fillText("Returning to menu...", gameCanvas.getWidth() / 2 - 120, gameCanvas.getHeight() / 2 + 60);
            } else {
                // Game over screen
                gc.setFill(Color.RED);
                gc.setFont(javafx.scene.text.Font.font(48));
                gc.fillText("GAME OVER", gameCanvas.getWidth() / 2 - 140, gameCanvas.getHeight() / 2);
            }
        }
    }

    public void handleKey(KeyEvent event) {
        String key = event.getCode().toString();
        switch (key) {
            case "ENTER" -> {
                if (gameState == GameState.MENU) {
                    // If host in multiplayer, send start command to client
                    if (hostMode && networkManager != null) {
                        networkManager.sendToClient("GAMESTART");
                        UdpNetworkManager udpHost = networkManager.getUdpHost();
                        if (udpHost != null) {
                            udpHost.sendSnapshot("GAMESTART");
                        }
                    }
                    gameState = GameState.RUNNING;
                    isGameOver = false;
                }
            }
            case "P" -> {
                if (gameState == GameState.RUNNING) gameState = GameState.PAUSED;
                else if (gameState == GameState.PAUSED) gameState = GameState.RUNNING;
            }
            case "ESCAPE" -> System.exit(0);
            case "M" -> gameState = GameState.MENU;
            default -> {
                if (gameState == GameState.RUNNING) {
                    if (networkClientMode && enableClientPrediction) {
                        // Client mode: client controls player2 with WASD, sends to host
                        String inputMsg = null;
                        switch (key) {
                            case "W" -> {
                                inputMsg = "INPUT:UP:" + currentTick;
                                currentInput2 = "UP";
                            }
                            case "S" -> {
                                inputMsg = "INPUT:DOWN:" + currentTick;
                                currentInput2 = "DOWN";
                            }
                            case "A" -> {
                                inputMsg = "INPUT:LEFT:" + currentTick;
                                currentInput2 = "LEFT";
                            }
                            case "D" -> {
                                inputMsg = "INPUT:RIGHT:" + currentTick;
                                currentInput2 = "RIGHT";
                            }
                            case "UP", "DOWN", "LEFT", "RIGHT" -> {
                                // Client ignores arrow keys (host controls player1)
                            }
                            default -> {}
                        }
                        // Send via both TCP and UDP (if UDP available)
                        if (inputMsg != null) {
                            networkManager.sendToHost(inputMsg);
                            UdpNetworkManager udpClient = networkManager.getUdpClient();
                            if (udpClient != null) {
                                udpClient.sendInput(inputMsg);
                            }
                        }
                    } else if (hostMode) {
                        // Host mode: host controls player1 with arrow keys, player2 via network
                        switch (key) {
                            case "UP", "DOWN", "LEFT", "RIGHT" -> currentInput = key;
                            case "W" -> currentInput2 = "UP";
                            case "S" -> currentInput2 = "DOWN";
                            case "A" -> currentInput2 = "LEFT";
                            case "D" -> currentInput2 = "RIGHT";
                            default -> {}
                        }
                    } else {
                        // Local mode: both players on same machine
                        switch (key) {
                            case "UP", "DOWN", "LEFT", "RIGHT" -> currentInput = key;
                            case "W" -> currentInput2 = "UP";
                            case "S" -> currentInput2 = "DOWN";
                            case "A" -> currentInput2 = "LEFT";
                            case "D" -> currentInput2 = "RIGHT";
                            default -> {}
                        }
                    }
                }
            }
        }
    }

    // A* pathfinding on grid (4-way). Returns list of {x,y} from start to goal (inclusive).
    private List<int[]> findPath(int sx, int sy, int gx, int gy, int[][] map, boolean allowGate) {
        List<int[]> empty = new ArrayList<>();
        int rows = map.length, cols = map[0].length;
        if (sx < 0 || sy < 0 || gx < 0 || gy < 0 || sy >= rows || sx >= cols || gy >= rows || gx >= cols) return empty;

        class Node { int x,y; int g,f; Node p; Node(int x,int y,int g,int f,Node p){this.x=x;this.y=y;this.g=g;this.f=f;this.p=p;} }

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n->n.f));
        boolean[][] closed = new boolean[rows][cols];
        open.add(new Node(sx, sy, 0, Math.abs(sx-gx)+Math.abs(sy-gy), null));

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (closed[cur.y][cur.x]) continue;
            closed[cur.y][cur.x] = true;
            if (cur.x == gx && cur.y == gy) {
                // reconstruct
                List<int[]> path = new ArrayList<>();
                Node n = cur;
                while (n != null) { path.add(0, new int[]{n.x, n.y}); n = n.p; }
                return path;
            }

            int[][] dirs = {{0,-1},{0,1},{-1,0},{1,0}};
            for (int[] d : dirs) {
                int nx = cur.x + d[0], ny = cur.y + d[1];
                if (nx < 0 || ny < 0 || ny >= rows || nx >= cols) continue;
                if (map[ny][nx] == 1) continue; // wall
                if (!allowGate && (map[ny][nx] == 4 || map[ny][nx] == 5)) continue; // gate blocked
                if (closed[ny][nx]) continue;
                int ng = cur.g + 1;
                int h = Math.abs(nx - gx) + Math.abs(ny - gy);
                open.add(new Node(nx, ny, ng, ng + h, cur));
            }
        }
        return empty; // no path
    }
}