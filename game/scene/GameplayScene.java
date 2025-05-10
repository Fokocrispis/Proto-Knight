package game.scene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Random;

import game.BackgroundLayer;
import game.Game;
import game.GameObject;
import game.Vector2D;
import game.entity.BoxEntity;
import game.entity.PlayerEntity;
import game.entity.PlatformEntity;
import game.entity.FloorEntity;
import game.physics.PhysicsObject;

/**
 * Main gameplay scene with the player, platforms, and physics objects using sprites.
 */
public class GameplayScene extends AbstractScene {
    private PlayerEntity player;
    private FloorEntity floor;
    private boolean isPaused = false;
    
    // Background layers for parallax effect
    private BackgroundLayer[] backgroundLayers;
    
    // Scale factors for game units (1 unit = 100 pixels)
    private static final double SCALE = 100.0; // 1 meter = 100 pixels
    private static final double PLAYER_HEIGHT = 1.8 * SCALE; // 1.8 meters
    private static final double PLAYER_WIDTH = 0.6 * SCALE; // 0.6 meters
    private static final double PLATFORM_HEIGHT = 1.0 * SCALE; // 1 meter thickness
    private static final double PLATFORM_WIDTH = 3.0 * SCALE; // 3 meters length
    private static final double FLOOR_HEIGHT = 100; // Floor height of 1 meter (100 pixels)
    
    // Scene dimensions (200m x 30m)
    private static final int SCENE_WIDTH_METERS = 200;
    private static final int SCENE_HEIGHT_METERS = 10;
    private static final int SCENE_WIDTH = (int)(SCENE_WIDTH_METERS * SCALE);
    private static final int SCENE_HEIGHT = (int)(SCENE_HEIGHT_METERS * SCALE);
    
    // Background pattern dimensions (40m wide, repeating horizontally)
    private static final int BACKGROUND_TILE_WIDTH_METERS = 20;
    private static final int BACKGROUND_TILE_WIDTH = (int)(BACKGROUND_TILE_WIDTH_METERS * SCALE);
    
    /**
     * Creates a new gameplay scene.
     * 
     * @param game The game instance.
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
        
        // Calculate floor positions consistently using SCENE_HEIGHT
        float floorCenterY = (float)(SCENE_HEIGHT - FLOOR_HEIGHT / 2);
        float floorSurfaceY = (float) (floorCenterY - (FLOOR_HEIGHT / 2));
        
        // Create the player at ground level with proper scaling
        // Player should be positioned so their feet are on the floor surface
        player = new PlayerEntity(
            5 * SCALE,  // Start 5m from the left edge
            floorSurfaceY - PLAYER_HEIGHT / 2 - 10, // Position player slightly above the floor surface
            (int)PLAYER_WIDTH, // Collision box width
            (int)PLAYER_HEIGHT, // Collision box height
            game.getKeyboardInput()
        );
        
        // Debug output to verify positions
        System.out.println("Scene height: " + SCENE_HEIGHT);
        System.out.println("Floor center Y: " + floorCenterY);
        System.out.println("Floor surface Y: " + floorSurfaceY);
        System.out.println("Player Y: " + (floorSurfaceY - PLAYER_HEIGHT / 2 - 10));
        
        // Add player to game objects and physics system
        addGameObject(player);
        game.getPhysicsSystem().addObject(player);
        
        // Create some platforms using the new PlatformEntity
        createPlatforms();
        
        // Create some physics objects
        createPhysicsObjects();
        
        // Set player as camera target
        camera.setTarget(player.getPosition());
    }
    
    /**
     * Creates the parallax background layers.
     */
    private void createBackground() {
        // Initialize the array first
        backgroundLayers = new BackgroundLayer[4];
        
        // Layer 0: Sky color (completely fixed)
        backgroundLayers[0] = new BackgroundLayer(
            new Color(135, 206, 235), // Sky blue
            BACKGROUND_TILE_WIDTH,
            0.0  // Completely fixed (farthest)
        );
        
        // Layer 1: Far background (almost frozen)
        backgroundLayers[1] = new BackgroundLayer(
            "background_layer_1.png",
            BACKGROUND_TILE_WIDTH,
            1.8  // Almost frozen (far away)
        );
        
        // Layer 2: Middle background
        backgroundLayers[2] = new BackgroundLayer(
            "background_layer_2.png",
            BACKGROUND_TILE_WIDTH,
            1.7   // Medium parallax (middle distance)
        );
        
        // Layer 3: Near background (moves almost with camera)
        backgroundLayers[3] = new BackgroundLayer(
            "background_layer_3.png",
            BACKGROUND_TILE_WIDTH,
            1.2   // Moves almost with camera (closest to player)
        );
    }
    
    /**
     * Creates the floor for the level.
     */
    private void createFloor() {
        // Create a floor at the very bottom of the scene
        // Floor center Y should be at SCENE_HEIGHT - FLOOR_HEIGHT/2
        floor = new FloorEntity(
            SCENE_WIDTH / 2,  // Center of the scene
            SCENE_HEIGHT - FLOOR_HEIGHT / 2,  // Bottom of the scene
            SCENE_WIDTH,      // Full scene width
            (int)FLOOR_HEIGHT
        );
        
        // Floor doesn't move with physics
        floor.setAffectedByGravity(false);
        floor.setMass(0); // Infinite mass (immovable)
        floor.setVelocity(new Vector2D(0, 0));
        
        // Add floor to game objects and physics system
        addGameObject(floor);
        game.getPhysicsSystem().addObject(floor);
    }
    
    /**
     * Creates platforms in the level using sprite-based PlatformEntity.
     */
    private void createPlatforms() {
        // Create platforms throughout the scene using SCENE_HEIGHT
        float floorY = (float) (SCENE_HEIGHT - FLOOR_HEIGHT);
        
        // Create platforms at various positions across the scene
        // Platform 1 (3m x 1m)
        createPlatform(20 * SCALE, floorY - 3 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 2 (3m x 1m)
        createPlatform(50 * SCALE, floorY - 5 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 3 (3m x 1m)
        createPlatform(80 * SCALE, floorY - 4 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 4 (3m x 1m)
        createPlatform(120 * SCALE, floorY - 8 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 5 (3m x 1m)
        createPlatform(160 * SCALE, floorY - 6 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Print debug info
        System.out.println("Created platforms - Size: " + (PLATFORM_WIDTH/SCALE) + "m x " + (PLATFORM_HEIGHT/SCALE) + "m");
        System.out.println("Scene dimensions: " + SCENE_WIDTH_METERS + "m x " + SCENE_HEIGHT_METERS + "m");
        System.out.println("Background tile size: " + BACKGROUND_TILE_WIDTH_METERS + "m x " + (SCENE_HEIGHT - FLOOR_HEIGHT)/SCALE + "m");
    }
    
    /**
     * Helper method to create a single platform.
     */
    private void createPlatform(double x, double y, double width, double height) {
        PlatformEntity platform = new PlatformEntity(x, y, (int)width, (int)height);
        
        // Platforms don't move
        platform.setAffectedByGravity(false);
        platform.setMass(0); // Infinite mass
        platform.setVelocity(new Vector2D(0, 0));
        
        // Add to game objects and physics
        addGameObject(platform);
        game.getPhysicsSystem().addObject(platform);
    }
    
    /**
     * Creates physics objects in the level.
     */
    private void createPhysicsObjects() {
        // Create some boxes with physics throughout the scene
        Random random = new Random();
        int numBoxes = 20; // More boxes for the larger scene
        
        for (int i = 0; i < numBoxes; i++) {
            // Random position within the scene
            double x = random.nextDouble() * SCENE_WIDTH;
            double y = random.nextDouble() * 300;
            
            // Random size (use scale for consistency)
            int size = (int)(20 + random.nextInt(30));
            
            // Random color
            Color color = new Color(
                random.nextInt(200) + 55,
                random.nextInt(200) + 55,
                random.nextInt(200) + 55
            );
            
            // Create the box
            BoxEntity box = new BoxEntity(x, y, size, size, color);
            
            // Add to game objects and physics
            addGameObject(box);
            game.getPhysicsSystem().addObject(box);
        }
    }
    
    @Override
    public void initialize() {
        if (!initialized) {
            setupCamera();
            createGameObjects();
            initialized = true;
        }
    }
    
    @Override
    public void onEnter() {
        super.onEnter();
        // Ensure the scene is fully initialized before rendering
        if (!initialized) {
            initialize();
        }
    }
    
    @Override
    public void update(long deltaTime) {
        // Check for pause toggle
        if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_P)) {
            isPaused = !isPaused;
        }
        
        // Don't update if paused
        if (isPaused) {
            return;
        }
        
        // Update all game objects
        super.update(deltaTime);
        
        // Ensure camera follows player
        camera.setTarget(player.getPosition());
        
        // Force the floor to stay at the bottom of the scene
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
        
        // Check for scene change (example: pressing ESC for menu)
        if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            // You would transition to a menu scene here
            // game.getSceneManager().pushScene("menu");
        }
        
        // Check if player has fallen off the world
        if (player.getPosition().getY() > SCENE_HEIGHT + 100) {
            resetPlayer();
        }
    }
    
    /**
     * Resets the player position.
     */
    private void resetPlayer() {
        // Reset player to ground level near the start of the scene
        float floorY = (float)(SCENE_HEIGHT - FLOOR_HEIGHT);
        
        player.setPosition(5 * SCALE, floorY - PLAYER_HEIGHT / 2 - 10);
        player.setVelocity(new Vector2D(0, 0));
    }
    
    @Override
    public void render(Graphics2D g) {
        // Apply camera transformations for everything, including background
        camera.apply(g);
        
        // Render the parallax background layers (now in world coordinates)
        renderBackground(g);
        
        // Render all game objects (floor, player, platforms, etc.)
        for (GameObject gameObject : gameObjects) {
            // Skip background layers as they're already rendered
            if (!(gameObject instanceof BackgroundLayer)) {
                gameObject.render(g);
            }
        }
        
        // Reset camera transformation for UI
        camera.reset(g);
        
        // Render UI (after resetting camera)
        renderUI(g);
    }
    
    /**
     * Renders the scene background with parallax layers.
     */
    private void renderBackground(Graphics2D g) {
        // Get camera position for parallax calculation
        double cameraX = camera.getPosition().getX();
        
        // Calculate the floor Y position using SCENE_HEIGHT
        // The floor center is at SCENE_HEIGHT - FLOOR_HEIGHT / 2
        float floorCenterY = (float)(SCENE_HEIGHT - FLOOR_HEIGHT / 2);
        
        // Render each background layer using SCENE_HEIGHT
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
        g.drawString("Down Arrow - Crouch", 20, 110);
        g.drawString("P - Pause", 20, 130);
        g.drawString("ESC - Menu", 20, 150);
        g.drawString("Scene dimensions: " + SCENE_WIDTH_METERS + "m x " + SCENE_HEIGHT_METERS + "m", 20, 170);
        g.drawString("Background tile: " + BACKGROUND_TILE_WIDTH_METERS + "m x " + (SCENE_HEIGHT - FLOOR_HEIGHT)/SCALE + "m", 20, 190);
        
        // Player info (only if player exists)
        if (player != null) {
            g.drawString("Player Position: " + formatVector(player.getPosition()), 20, 230);
            g.drawString("Player Velocity: " + formatVector(player.getVelocity()), 20, 250);
            g.drawString("On Ground: " + (player.isOnGround() ? "Yes" : "No"), 20, 270);
            g.drawString("Current Sprite: " + 
                (player.getCurrentSprite() != null ? player.getCurrentSprite().getName() : "None"), 20, 290);
            
            // Player size info
            g.drawString("Player Size: " + (PLAYER_HEIGHT/SCALE) + "m x " + (PLAYER_WIDTH/SCALE) + "m", 20, 310);
        } else {
            g.drawString("Loading player...", 20, 230);
        }
        
        // Draw pause screen if paused
        if (isPaused) {
            drawPauseScreen(g);
        }
    }
    
    /**
     * Formats a vector for display.
     */
    private String formatVector(Vector2D vector) {
        return String.format("(%.1f, %.1f)", vector.getX(), vector.getY());
    }
    
    /**
     * Draws the pause screen overlay.
     */
    private void drawPauseScreen(Graphics2D g) {
        // Semi-transparent overlay
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, game.getWidth(), game.getHeight());
        
        // Pause text
        g.setColor(Color.WHITE);
        String pauseText = "PAUSED";
        
        // Center the text (simplified)
        int textX = game.getWidth() / 2 - 50;
        int textY = game.getHeight() / 2;
        
        g.setFont(g.getFont().deriveFont(36f));
        g.drawString(pauseText, textX, textY);
        
        // Additional instructions
        g.setFont(g.getFont().deriveFont(18f));
        g.drawString("Press P to resume", textX - 30, textY + 40);
    }
}