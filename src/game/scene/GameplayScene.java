package game.scene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Random;

import game.BackgroundLayer;
import game.Game;
import game.GameObject;
import game.Vector2D;
import game.audio.SoundManager;
import game.entity.BoxEntity;
import game.entity.PlayerEntity;
import game.entity.PlatformEntity;
import game.entity.FloorEntity;
import game.physics.PhysicsObject;

/**
 * Main gameplay scene with simplified sprite loading
 */
public class GameplayScene extends AbstractScene {
    private PlayerEntity player;
    private FloorEntity floor;
    private boolean isPaused = false;
    
    // Background layers for parallax effect
    private BackgroundLayer[] backgroundLayers;
    
    // Audio system
    private final SoundManager soundManager = SoundManager.getInstance();
    
    // Scale factors for game units (1 unit = 100 pixels)
    private static final double SCALE = 100.0;
    private static final double PLAYER_HEIGHT = 1.8 * SCALE;
    private static final double PLAYER_WIDTH = 0.6 * SCALE;
    private static final double PLATFORM_HEIGHT = 1.0 * SCALE;
    private static final double PLATFORM_WIDTH = 3.0 * SCALE;
    private static final double FLOOR_HEIGHT = 100;
    
    // Scene dimensions (200m x 30m)
    private static final int SCENE_WIDTH_METERS = 200;
    private static final int SCENE_HEIGHT_METERS = 10;
    private static final int SCENE_WIDTH = (int)(SCENE_WIDTH_METERS * SCALE);
    private static final int SCENE_HEIGHT = (int)(SCENE_HEIGHT_METERS * SCALE);
    
    // Background pattern dimensions
    private static final int BACKGROUND_TILE_WIDTH_METERS = 20;
    private static final int BACKGROUND_TILE_WIDTH = (int)(BACKGROUND_TILE_WIDTH_METERS * SCALE);
    
    /**
     * Creates a new gameplay scene
     */
    public GameplayScene(Game game) {
        super(game);
    }
    
    @Override
    protected void setupCamera() {
        super.setupCamera();
        
        // Set camera world bounds to match scene size
        camera.setWorldBounds(SCENE_WIDTH, SCENE_HEIGHT);
        
        // Set camera to follow player smoothly
        camera.setSmoothFactor(0.15);
        camera.setSmoothEnabled(true);
    }
    
    @Override
    protected void createGameObjects() {
        // Create background layers first
        createBackground();
        
        // Create a floor first
        createFloor();
        
        // Calculate floor positions
        float floorCenterY = (float)(SCENE_HEIGHT - FLOOR_HEIGHT / 2);
        float floorSurfaceY = (float) (floorCenterY - (FLOOR_HEIGHT / 2));
        
        // Create the player with the simplified PlayerEntity
        player = new PlayerEntity(
            5 * SCALE,  // Start 5m from the left edge
            floorSurfaceY - PLAYER_HEIGHT / 2 - 10, // Position player above the floor surface
            (int)PLAYER_WIDTH, // Collision box width
            (int)PLAYER_HEIGHT, // Collision box height
            game.getKeyboardInput()
        );
        
        // Add player to game objects and physics system
        addGameObject(player);
        game.getPhysicsSystem().addObject(player);
        
        // Create some platforms
        createPlatforms();
        
        // Create some physics objects
        createPhysicsObjects();
        
        // Set player as camera target
        camera.setTarget(player.getPosition());
    }
    
    /**
     * Creates the parallax background layers
     */
    private void createBackground() {
        backgroundLayers = new BackgroundLayer[4];
        
        // Layer 0: Sky color (completely fixed)
        backgroundLayers[0] = new BackgroundLayer(
            new Color(135, 206, 235), // Sky blue
            BACKGROUND_TILE_WIDTH,
            0.0  // Completely fixed
        );
        
        // Layer 1: Far background
        backgroundLayers[1] = new BackgroundLayer(
            "background_layer_1.png",
            BACKGROUND_TILE_WIDTH,
            1.8  // Almost frozen
        );
        
        // Layer 2: Middle background
        backgroundLayers[2] = new BackgroundLayer(
            "background_layer_2.png",
            BACKGROUND_TILE_WIDTH,
            1.7   // Medium parallax
        );
        
        // Layer 3: Near background
        backgroundLayers[3] = new BackgroundLayer(
            "background_layer_3.png",
            BACKGROUND_TILE_WIDTH,
            1.2   // Moves almost with camera
        );
    }
    
    /**
     * Creates the floor for the level
     */
    private void createFloor() {
        floor = new FloorEntity(
            SCENE_WIDTH / 2,
            SCENE_HEIGHT - FLOOR_HEIGHT / 2,
            SCENE_WIDTH,
            (int)FLOOR_HEIGHT
        );
        
        floor.setAffectedByGravity(false);
        floor.setMass(0);
        floor.setVelocity(new Vector2D(0, 0));
        
        addGameObject(floor);
        game.getPhysicsSystem().addObject(floor);
    }
    
    /**
     * Creates platforms in the level
     */
    private void createPlatforms() {
        float floorY = (float) (SCENE_HEIGHT - FLOOR_HEIGHT);
        
        // Create platforms at various positions
        createPlatform(20 * SCALE, floorY - 3 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        createPlatform(50 * SCALE, floorY - 5 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        createPlatform(80 * SCALE, floorY - 4 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        createPlatform(120 * SCALE, floorY - 8 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        createPlatform(160 * SCALE, floorY - 6 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
    }
    
    /**
     * Helper method to create a single platform
     */
    private void createPlatform(double x, double y, double width, double height) {
        PlatformEntity platform = new PlatformEntity(x, y, (int)width, (int)height);
        
        platform.setAffectedByGravity(false);
        platform.setMass(0);
        platform.setVelocity(new Vector2D(0, 0));
        
        addGameObject(platform);
        game.getPhysicsSystem().addObject(platform);
    }
    
    /**
     * Creates physics objects in the level
     */
    private void createPhysicsObjects() {
        Random random = new Random();
        int numBoxes = 20;
        
        for (int i = 0; i < numBoxes; i++) {
            double x = random.nextDouble() * SCENE_WIDTH;
            double y = random.nextDouble() * 300;
            int size = (int)(20 + random.nextInt(30));
            
            Color color = new Color(
                random.nextInt(200) + 55,
                random.nextInt(200) + 55,
                random.nextInt(200) + 55
            );
            
            BoxEntity box = new BoxEntity(x, y, size, size, color);
            addGameObject(box);
            game.getPhysicsSystem().addObject(box);
        }
    }
    
    @Override
    public void initialize() {
        if (!initialized) {
            setupCamera();
            createGameObjects();
            startBackgroundMusic();
            
            initialized = true;
        }
    }
    
    /**
     * Starts the background music
     */
    private void startBackgroundMusic() {
        soundManager.playBackgroundMusic("background_music.wav");
        soundManager.setMusicVolume(0.7f);
        System.out.println("Started background music");
    }
    
    @Override
    public void onEnter() {
        super.onEnter();
        
        if (!initialized) {
            initialize();
        } else {
            soundManager.resumeBackgroundMusic();
            System.out.println("Resumed background music");
        }
    }
    
    @Override
    public void onExit() {
        super.onExit();
        soundManager.stopBackgroundMusic();
        System.out.println("Stopped background music");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        soundManager.pauseBackgroundMusic();
        System.out.println("Paused background music");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        soundManager.resumeBackgroundMusic();
        System.out.println("Resumed background music");
    }
    
    @Override
    public void update(long deltaTime) {
        // Check for pause toggle
        if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_P)) {
            isPaused = !isPaused;
            
            if (isPaused) {
                soundManager.pauseBackgroundMusic();
            } else {
                soundManager.resumeBackgroundMusic();
            }
        }
        
        // Quick volume controls
        handleVolumeControls();
        
        // Don't update if paused
        if (isPaused) {
            return;
        }
        
        // Update all game objects
        super.update(deltaTime);
        
        // Ensure camera follows player
        camera.setTarget(player.getPosition());
        
        // Force the floor to stay at the bottom
        if (floor != null) {
            floor.setVelocity(new Vector2D(0, 0));
            floor.setPosition(SCENE_WIDTH / 2, SCENE_HEIGHT - FLOOR_HEIGHT / 2);
        }
        
        // Check if player has reached the scene boundaries
        float floorY = (float)(SCENE_HEIGHT - FLOOR_HEIGHT);
        float sceneTop = 0;
        
        if (player != null) {
            // Constrain player position within the scene
            double playerX = player.getPosition().getX();
            double playerY = player.getPosition().getY();
            
            // Check horizontal boundaries
            if (playerX < 0) {
                player.setPosition(0, playerY);
                player.setVelocity(new Vector2D(Math.max(0, player.getVelocity().getX()), player.getVelocity().getY()));
            } else if (playerX > SCENE_WIDTH) {
                player.setPosition(SCENE_WIDTH, playerY);
                player.setVelocity(new Vector2D(Math.min(0, player.getVelocity().getX()), player.getVelocity().getY()));
            }
            
            // Check vertical boundaries
            if (playerY < sceneTop) {
                player.setPosition(playerX, sceneTop);
                player.setVelocity(new Vector2D(player.getVelocity().getX(), 0));
            }
        }
        
        // Check for scene change
        if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            // Transition to menu scene
        }
        
        // Check if player has fallen off the world
        if (player.getPosition().getY() > SCENE_HEIGHT + 100) {
            resetPlayer();
        }
    }
    
    /**
     * Handles quick volume control inputs
     */
    private void handleVolumeControls() {
        // Master volume controls
        if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_SUBTRACT)) {
            float newVolume = Math.max(0.0f, soundManager.getMasterVolume() - 0.1f);
            soundManager.setMasterVolume(newVolume);
        }
        if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_ADD)) {
            float newVolume = Math.min(1.0f, soundManager.getMasterVolume() + 0.1f);
            soundManager.setMasterVolume(newVolume);
        }
        
        // Music volume controls (with CTRL modifier)
        if (game.getKeyboardInput().isKeyPressed(KeyEvent.VK_CONTROL)) {
            if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_SUBTRACT)) {
                float newVolume = Math.max(0.0f, soundManager.getMusicVolume() - 0.1f);
                soundManager.setMusicVolume(newVolume);
            }
            if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_ADD)) {
                float newVolume = Math.min(1.0f, soundManager.getMusicVolume() + 0.1f);
                soundManager.setMusicVolume(newVolume);
            }
        }
    }
    
    /**
     * Resets the player position
     */
    private void resetPlayer() {
        float floorY = (float)(SCENE_HEIGHT - FLOOR_HEIGHT);
        
        player.setPosition(5 * SCALE, floorY - PLAYER_HEIGHT / 2 - 10);
        player.setVelocity(new Vector2D(0, 0));
    }
    
    @Override
    public void render(Graphics2D g) {
        // Apply camera transformations
        camera.apply(g);
        
        // Render the parallax background layers
        renderBackground(g);
        
        // Render all game objects (floor, player, platforms, etc.)
        for (GameObject gameObject : gameObjects) {
            if (!(gameObject instanceof BackgroundLayer)) {
                gameObject.render(g);
            }
        }
        
        // Reset camera transformation for UI
        camera.reset(g);
        
        // Render UI
        renderUI(g);
    }
    
    /**
     * Renders the scene background with parallax layers
     */
    private void renderBackground(Graphics2D g) {
        double cameraX = camera.getPosition().getX();
        float floorCenterY = (float)(SCENE_HEIGHT - FLOOR_HEIGHT / 2);
        
        for (int i = 0; i < backgroundLayers.length; i++) {
            if (backgroundLayers[i] != null) {
                backgroundLayers[i].renderWithCamera(g, cameraX, SCENE_HEIGHT, (int)FLOOR_HEIGHT, 
                                                    (int)floorCenterY, SCENE_WIDTH);
            }
        }
    }
    
    @Override
    protected void renderUI(Graphics2D g) {
        // Draw game UI elements
        g.setColor(Color.WHITE);
        
        // Display controls
        g.drawString("Controls:", 20, 30);
        g.drawString("Arrow Keys - Move", 20, 50);
        g.drawString("Space - Jump", 20, 70);
        g.drawString("E - Attack", 20, 90);
        g.drawString("W - Dash", 20, 110);
        g.drawString("P - Pause", 20, 130);
        g.drawString("ESC - Menu", 20, 150);
        
        // Audio controls
        g.drawString("NumPad +/- - Volume", 20, 180);
        g.drawString("Ctrl + NumPad +/- - Music Volume", 20, 200);
        
        // Scene info
        g.drawString("Scene dimensions: " + SCENE_WIDTH_METERS + "m x " + SCENE_HEIGHT_METERS + "m", 20, 230);
        
        // Volume indicators
        g.drawString(String.format("Master Volume: %d%%", (int)(soundManager.getMasterVolume() * 100)), 20, 260);
        g.drawString(String.format("Music Volume: %d%%", (int)(soundManager.getMusicVolume() * 100)), 20, 280);
        g.drawString(String.format("SFX Volume: %d%%", (int)(soundManager.getSfxVolume() * 100)), 20, 300);
        
        // Player info
        if (player != null) {
            g.drawString("Player Position: " + formatVector(player.getPosition()), 20, 330);
            g.drawString("Player Velocity: " + formatVector(player.getVelocity()), 20, 350);
            g.drawString("On Ground: " + (player.isOnGround() ? "Yes" : "No"), 20, 370);
            g.drawString("Current Sprite: " + 
                (player.getCurrentSprite() != null ? player.getCurrentSprite().getName() : "None"), 20, 390);
            
            // Player state info
            g.drawString("Dashing: " + (player.isDashing() ? "Yes" : "No"), 20, 410);
        } else {
            g.drawString("Loading player...", 20, 330);
        }
        
        // Draw pause screen if paused
        if (isPaused) {
            drawPauseScreen(g);
        }
    }
    
    /**
     * Formats a vector for display
     */
    private String formatVector(Vector2D vector) {
        return String.format("(%.1f, %.1f)", vector.getX(), vector.getY());
    }
    
    /**
     * Draws the pause screen overlay
     */
    private void drawPauseScreen(Graphics2D g) {
        // Semi-transparent overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, game.getWidth(), game.getHeight());
        
        // Pause text
        g.setColor(Color.WHITE);
        String pauseText = "PAUSED";
        
        // Center the text
        int textX = game.getWidth() / 2 - 50;
        int textY = game.getHeight() / 2;
        
        g.setFont(g.getFont().deriveFont(48f));
        g.drawString(pauseText, textX, textY);
        
        // Additional instructions
        g.setFont(g.getFont().deriveFont(24f));
        g.drawString("Press P to resume", textX - 40, textY + 50);
        g.drawString("Press ESC for menu", textX - 50, textY + 80);
        
        // Audio status
        g.setFont(g.getFont().deriveFont(16f));
        g.setColor(Color.GRAY);
        g.drawString("Music: " + (soundManager.getMusicVolume() > 0 ? "ON" : "OFF"), textX - 30, textY + 120);
        g.drawString("Volume: " + (int)(soundManager.getMasterVolume() * 100) + "%", textX - 30, textY + 140);
    }
}