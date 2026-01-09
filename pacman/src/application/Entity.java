package application;
import javafx.scene.paint.Color;

class Entity {
    double pixelX, pixelY;
    int gridX, gridY;
    Color color;
    boolean isGhost;
    String lastDir = "RIGHT"; // Defined here for everyone to see
    double speed = 0.75;
    // State flags for interactions
    boolean ateDot = false;
    // Spawn/respawn positions (set during initialization)
    int spawnX = 14, spawnY = 14;
    int jailExitX = 14, jailExitY = 7;

    Entity(int x, int y, Color c, boolean isGhost) {
        this.gridX = x; this.gridY = y;
        this.pixelX = x * 25; this.pixelY = y * 25;
        this.color = c; this.isGhost = isGhost;
        this.spawnX = x; this.spawnY = y;
    }
    
    void setSpawnPositions(int spawnX, int spawnY, int jailExitX, int jailExitY) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.jailExitX = jailExitX;
        this.jailExitY = jailExitY;
    }

    void move(String dir, int[][] map) {
        ateDot = false;
        double targetPX = gridX * 25;
        double targetPY = gridY * 25;
        double s = speed;

        // Pixel movement
        if (pixelX < targetPX) pixelX = Math.min(pixelX + speed, targetPX);
        else if (pixelX > targetPX) pixelX = Math.max(pixelX - speed, targetPX);
        if (pixelY < targetPY) pixelY = Math.min(pixelY + speed, targetPY);
        else if (pixelY > targetPY) pixelY = Math.max(pixelY - speed, targetPY);

        // Grid decision: use threshold to avoid missing exact equality due to floating movement
        if (Math.abs(pixelX - targetPX) < s && Math.abs(pixelY - targetPY) < s) {
            // snap to exact grid to ensure consistent intersection detection
            pixelX = targetPX;
            pixelY = targetPY;
            int nx = gridX, ny = gridY;
            if (dir.equals("UP")) ny--;
            else if (dir.equals("DOWN")) ny++;
            else if (dir.equals("LEFT")) nx--;
            else if (dir.equals("RIGHT")) nx++;

            // Handle tunnel (tile 7) wraparound
            int mapHeight = map.length;
            int mapWidth = map[0].length;
            boolean teleported = false;
            
            // Tunnel wraparound: if moving through a tunnel tile (7), wrap to the other side
            if (ny >= 0 && ny < mapHeight && nx >= 0 && nx < mapWidth) {
                if (map[ny][nx] == 7) {
                    // Entering a tunnel tile - wrap around horizontally
                    if (dir.equals("LEFT")) {
                        nx = mapWidth - 1; // Wrap to the right side
                        teleported = true;
                    } else if (dir.equals("RIGHT")) {
                        nx = 0; // Wrap to the left side
                        teleported = true;
                    }
                }
                
                if (map[ny][nx] != 1 && (isGhost || (map[ny][nx] != 4 && map[ny][nx] != 5))) {
                    gridX = nx; gridY = ny;
                    // Instant teleport: immediately set pixel position when teleporting
                    if (teleported) {
                        pixelX = gridX * 25;
                        pixelY = gridY * 25;
                    }
                    lastDir = dir; // Updates the direction they are successfully moving
                    if (!isGhost && map[gridY][gridX] == 2) { map[gridY][gridX] = 0; ateDot = true; }
                }
            }
        }
    }
}


abstract class Ghost extends Entity {
    protected int scatterX, scatterY;
    protected boolean inJail = true;
    protected int jailTicks = 0; // ticks remaining while locked in jail (60 ticks = 1s)
    protected int targetX, targetY;
    protected boolean isEaten = false; // when true, ghost is eyes and returns to jail

    Ghost(int x, int y, Color c, int sx, int sy) {
        super(x, y, c, true);
        this.scatterX = sx; this.scatterY = sy;
    }

    abstract void setTarget(Entity pacman, Ghost blinky);
    // Multiplayer version - can use both players for advanced targeting
    void setTargetMultiplayer(Entity p1, Entity p2, Ghost blinky, int p1DotsEaten, int p2DotsEaten) {
        // Default: use single player version
        setTarget(p1, blinky);
    }

    // FIX: Refer to GameController.GhostMode
    void updateBehavior(int[][] map, Entity pacman, Ghost blinky, GameController.GhostMode mode) {
        // If ghost was recently eaten and is locked in jail, wait until timer expires
        if (inJail && jailTicks > 0) {
            jailTicks--; // stay put in jail
            return;
        } else if (inJail && jailTicks == 0) {
            // release from timed jail; allow normal in-jail behavior to move toward exit
            inJail = false;
            lastDir = "UP"; // nudge movement upwards on release
        }
        // --- DECISION: compute target and desired direction every tick (not only when centered) ---
        // 1. SELECT TARGET (always available)
        if (isEaten) {
            targetX = spawnX; targetY = spawnY;
        } else if (inJail) {
            targetX = jailExitX; targetY = jailExitY;
            // Only exit jail if we've reached the exit area and jail timer expired
            if (gridY <= jailExitY + 1 && jailTicks == 0) {
                inJail = false;
            }
        } else if (mode == GameController.GhostMode.SCATTER) {
            targetX = scatterX; targetY = scatterY;
        } else if (mode == GameController.GhostMode.FRIGHTENED) {
            targetX = (int)(Math.random() * 28); targetY = (int)(Math.random() * 22);
        } else {
            setTarget(pacman, blinky);
        }

        // 2. DIRECTION DECISION (Shortest distance to target). Prefer not reversing;
        // if there are no valid non-reverse options, allow reverse to avoid corner-hugging.
        String[] dirs = {"UP", "DOWN", "LEFT", "RIGHT"};
        String bestDir = lastDir;
        double minDist = Double.MAX_VALUE;
        int validNonReverse = 0;

        for (String d : dirs) {
            if (isOpposite(d, lastDir)) continue;

            int nx = gridX + (d.equals("LEFT") ? -1 : d.equals("RIGHT") ? 1 : 0);
            int ny = gridY + (d.equals("UP") ? -1 : d.equals("DOWN") ? 1 : 0);

            if (ny >= 0 && ny < map.length && nx >= 0 && nx < map[0].length && map[ny][nx] != 1) {
                // Allow ghosts in jail to pass through gates (4, 5) to exit
                if (!inJail && !isEaten && (map[ny][nx] == 4 || map[ny][nx] == 5)) continue;
                // If in jail, allow movement through gates but prefer paths to exit
                if (inJail && (map[ny][nx] == 4 || map[ny][nx] == 5)) {
                    // Check if this direction gets us closer to jail exit
                    double distToExit = Math.hypot(nx - jailExitX, ny - jailExitY);
                    double currentDistToExit = Math.hypot(gridX - jailExitX, gridY - jailExitY);
                    if (distToExit >= currentDistToExit) continue; // Don't move away from exit
                }

                validNonReverse++;
                double dist = Math.hypot(nx - targetX, ny - targetY);
                if (dist < minDist) {
                    minDist = dist;
                    bestDir = d;
                }
            }
        }

        if (validNonReverse == 0) {
            // allow reverse if it's the only available move
            for (String d : dirs) {
                if (!isOpposite(d, lastDir)) continue; // we want the reverse
                int nx = gridX + (d.equals("LEFT") ? -1 : d.equals("RIGHT") ? 1 : 0);
                int ny = gridY + (d.equals("UP") ? -1 : d.equals("DOWN") ? 1 : 0);
                if (ny >= 0 && ny < map.length && nx >= 0 && nx < map[0].length && map[ny][nx] != 1) {
                    if (!inJail && !isEaten && (map[ny][nx] == 4 || map[ny][nx] == 5)) continue;
                    // If in jail and reverse is only option, allow it
                    if (inJail && (map[ny][nx] == 4 || map[ny][nx] == 5)) {
                        // Allow reverse through gate if it's the only way
                    }
                    bestDir = d;
                    break;
                }
            }
        }

        // Set desired direction immediately; move() will attempt to honor it even mid-tile
        lastDir = bestDir;
        move(lastDir, map);

        // If returning as eyes and reached jail center, finalize respawn
        double s2 = speed;
        if (isEaten && gridX == spawnX && gridY == spawnY && Math.abs(pixelX - gridX * 25) < s2 && Math.abs(pixelY - gridY * 25) < s2) {
            respawn();
        }
    }

    private boolean isOpposite(String d1, String d2) {
        return (d1.equals("UP") && d2.equals("DOWN")) || (d1.equals("DOWN") && d2.equals("UP")) ||
                (d1.equals("LEFT") && d2.equals("RIGHT")) || (d1.equals("RIGHT") && d2.equals("LEFT"));
    }

    void respawn() {
        // Find a valid position in jail area if spawn position is invalid
        // This is handled by GameController, but we ensure we're in a valid position
        gridX = spawnX; gridY = spawnY;
        pixelX = gridX * 25; pixelY = gridY * 25;
        inJail = true;
        // Jail time will be set by GameController based on difficulty
        // Default to 600 ticks (10 seconds) if not set
        if (jailTicks == 0) jailTicks = 600;
        isEaten = false;
        // Reset direction to allow movement
        lastDir = "UP";
    }
    
    // Method to set jail time based on difficulty
    void setJailTime(int ticks) {
        jailTicks = ticks;
    }

    void setEaten(boolean v) { isEaten = v; if (v) lastDir = "UP"; }
    boolean isEaten() { return isEaten; }
}


// 🔴 BLINKY - Directly targets Pac-Man
class Blinky extends Ghost {
    Blinky(int x, int y) {
        super(x, y, Color.RED, 27, 0);
    }

    @Override
    void setTarget(Entity pacman, Ghost blinky) {
        this.targetX = pacman.gridX;
        this.targetY = pacman.gridY;
    }
    
    @Override
    void setTargetMultiplayer(Entity p1, Entity p2, Ghost blinky, int p1DotsEaten, int p2DotsEaten) {
        // Blinky: The Enforcer - Target player with lowest distance, prioritize if they've eaten more dots
        double dist1 = Math.hypot(gridX - p1.gridX, gridY - p1.gridY);
        double dist2 = Math.hypot(gridX - p2.gridX, gridY - p2.gridY);
        
        Entity target = p1;
        if (dist2 < dist1) {
            target = p2;
        } else if (dist2 == dist1) {
            // Tie-breaker: prioritize player who has eaten more dots (punish speed-running)
            if (p2DotsEaten > p1DotsEaten) {
                target = p2;
            }
        }
        
        this.targetX = target.gridX;
        this.targetY = target.gridY;
    }
}

// 🌸 PINKY - Ambush: Targets 4 tiles ahead of Pac-Man
class Pinky extends Ghost {
    Pinky(int x, int y) {
        super(x, y, Color.PINK, 0, 0);
    }

    @Override
    void setTarget(Entity pacman, Ghost blinky) {
        int tx = pacman.gridX;
        int ty = pacman.gridY;

        // Target 4 tiles ahead based on lastDir
        switch (pacman.lastDir) {
            case "UP" -> { tx -= 4; ty -= 4; } // Classic overflow bug
            case "DOWN" -> ty += 4;
            case "LEFT" -> tx -= 4;
            case "RIGHT" -> tx += 4;
        }
        this.targetX = tx;
        this.targetY = ty;
    }
    
    @Override
    void setTargetMultiplayer(Entity p1, Entity p2, Ghost blinky, int p1DotsEaten, int p2DotsEaten) {
        // Pinky: The Ambusher - Predict 4 tiles ahead of both, choose based on splitting the team
        int tx1 = p1.gridX, ty1 = p1.gridY;
        int tx2 = p2.gridX, ty2 = p2.gridY;
        
        // Predict P1's future position
        switch (p1.lastDir) {
            case "UP" -> { tx1 -= 4; ty1 -= 4; }
            case "DOWN" -> ty1 += 4;
            case "LEFT" -> tx1 -= 4;
            case "RIGHT" -> tx1 += 4;
        }
        
        // Predict P2's future position
        switch (p2.lastDir) {
            case "UP" -> { tx2 -= 4; ty2 -= 4; }
            case "DOWN" -> ty2 += 4;
            case "LEFT" -> tx2 -= 4;
            case "RIGHT" -> tx2 += 4;
        }
        
        // Choose prediction that's closer to splitting the team (midpoint between both players)
        int midX = (p1.gridX + p2.gridX) / 2;
        int midY = (p1.gridY + p2.gridY) / 2;
        
        double dist1 = Math.hypot(tx1 - midX, ty1 - midY);
        double dist2 = Math.hypot(tx2 - midX, ty2 - midY);
        
        // Target the prediction that better splits the team
        if (dist1 < dist2) {
            this.targetX = tx1;
            this.targetY = ty1;
        } else {
            this.targetX = tx2;
            this.targetY = ty2;
        }
    }
}

// 🔵 INKY - Flanker: Uses Blinky's position to pinch Pac-Man
class Inky extends Ghost {
    Inky(int x, int y) {
        super(x, y, Color.CYAN, 27, 21);
    }

    @Override
    void setTarget(Entity pacman, Ghost blinky) {
        // 1. Get tile 2 spaces ahead of Pac-Man
        int px = pacman.gridX;
        int py = pacman.gridY;
        switch (pacman.lastDir) {
            case "UP" -> { px -= 2; py -= 2; }
            case "DOWN" -> py += 2;
            case "LEFT" -> px -= 2;
            case "RIGHT" -> px += 2;
        }

        // 2. Double the vector from Blinky to that tile
        this.targetX = blinky.gridX + 2 * (px - blinky.gridX);
        this.targetY = blinky.gridY + 2 * (py - blinky.gridY);
    }
    
    @Override
    void setTargetMultiplayer(Entity p1, Entity p2, Ghost blinky, int p1DotsEaten, int p2DotsEaten) {
        // Inky: The Chaos Engine - Find midpoint between P1 and P2, use Blinky's position for vector
        int midX = (p1.gridX + p2.gridX) / 2;
        int midY = (p1.gridY + p2.gridY) / 2;
        
        // Use classic vector logic: blinky + 2 × (mid - blinky)
        // This creates a pincer attack that collapses the map and creates unavoidable attacks
        this.targetX = blinky.gridX + 2 * (midX - blinky.gridX);
        this.targetY = blinky.gridY + 2 * (midY - blinky.gridY);
    }
}

// 🟠 CLYDE - Coward: Chases if far, retreats if close
class Clyde extends Ghost {
    Clyde(int x, int y) {
        super(x, y, Color.ORANGE, 0, 21);
    }

    @Override
    void setTarget(Entity pacman, Ghost blinky) {
        double distance = Math.hypot(gridX - pacman.gridX, gridY - pacman.gridY);

        // Become more aggressive: reduce the distance threshold from 8 to 4
        if (distance > 4) {
            this.targetX = pacman.gridX;
            this.targetY = pacman.gridY;
        } else {
            // Retreat to scatter corner
            this.targetX = scatterX;
            this.targetY = scatterY;
        }
    }
    
    @Override
    void setTargetMultiplayer(Entity p1, Entity p2, Ghost blinky, int p1DotsEaten, int p2DotsEaten) {
        // Clyde: The Troll - If far from both, chase closer one. If close to either, flee to other side
        double dist1 = Math.hypot(gridX - p1.gridX, gridY - p1.gridY);
        double dist2 = Math.hypot(gridX - p2.gridX, gridY - p2.gridY);
        
        if (dist1 < 8 || dist2 < 8) {
            // Close to at least one player - flee to the other player's scatter corner
            if (dist1 < dist2) {
                // Close to P1, flee toward P2's area (use P2's scatter corner concept)
                this.targetX = scatterX; // Use Clyde's scatter corner
                this.targetY = scatterY;
            } else {
                // Close to P2, flee toward opposite corner
                this.targetX = scatterX;
                this.targetY = scatterY;
            }
        } else {
            // Far from both - chase the closer one
            if (dist1 < dist2) {
                this.targetX = p1.gridX;
                this.targetY = p1.gridY;
            } else {
                this.targetX = p2.gridX;
                this.targetY = p2.gridY;
            }
        }
    }
}