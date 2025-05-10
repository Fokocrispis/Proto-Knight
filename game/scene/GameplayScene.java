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
    
    // Background dimensions (30m x 20m)
    private static final int BACKGROUND_WIDTH_METERS = 30;
    private static final int BACKGROUND_HEIGHT_METERS = 20;
    private static final int BACKGROUND_WIDTH = (int)(BACKGROUND_WIDTH_METERS * SCALE);
    private static final int BACKGROUND_HEIGHT = (int)(BACKGROUND_HEIGHT_METERS * SCALE);
    
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
        
        // Set camera world bounds to match game world size
        camera.setWorldBounds((int)game.getWorldWidth(), (int)game.getWorldHeight());
        
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
        
        // Create the player at ground level with proper scaling
        float floorY = (float)(game.getWorldHeight() - FLOOR_HEIGHT);
        
        // Create player with proper scaling (centered in the 30m width area)
        player = new PlayerEntity(
            BACKGROUND_WIDTH / 2,  // Center within the background area
            floorY - PLAYER_HEIGHT / 2 - 10, // Position player slightly above the floor
            (int)PLAYER_WIDTH, // Collision box width
            (int)PLAYER_HEIGHT, // Collision box height
            game.getKeyboardInput()
        );
        
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
        // Create multiple background layers with different parallax speeds
        backgroundLayers = new BackgroundLayer[4];
        
        // Layer 0: Sky color (no parallax)
        backgroundLayers[0] = new BackgroundLayer(
            new Color(135, 206, 235), // Sky blue
            BACKGROUND_WIDTH,
            BACKGROUND_HEIGHT,
            0.0, 0.0
        );
        
        // Layer 1: Far background (slowest parallax)
        backgroundLayers[1] = new BackgroundLayer(
            "background_layer_1.png",
            0.2, 0.1
        );
        
        // Layer 2: Middle background
        backgroundLayers[2] = new BackgroundLayer(
            "background_layer_2.png",
            0.5, 0.3
        );
        
        // Layer 3: Near background (fastest parallax)
        backgroundLayers[3] = new BackgroundLayer(
            "background_layer_3.png",
            0.8, 0.5
        );
        
        // Add background layers to game objects
        for (BackgroundLayer layer : backgroundLayers) {
            addGameObject(layer);
        }
    }
    
    /**
     * Creates the floor for the level.
     */
    private void createFloor() {
        // Create a floor with 2m wide tiles and 1m height (only within the background area)
        floor = new FloorEntity(
            BACKGROUND_WIDTH / 2,  // Center within the background area
            game.getWorldHeight() - FLOOR_HEIGHT / 2,
            BACKGROUND_WIDTH,      // Same width as background
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
        // Create some platforms within the 30x20m background area
        float floorY = (float) (game.getWorldHeight() - FLOOR_HEIGHT);
        
        // Platform 1 (3m x 1m)
        createPlatform(5 * SCALE, floorY - 3 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 2 (3m x 1m)
        createPlatform(15 * SCALE, floorY - 5 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 3 (3m x 1m)
        createPlatform(25 * SCALE, floorY - 4 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 4 (3m x 1m)
        createPlatform(10 * SCALE, floorY - 8 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Platform 5 (3m x 1m)
        createPlatform(20 * SCALE, floorY - 10 * SCALE, PLATFORM_WIDTH, PLATFORM_HEIGHT);
        
        // Print debug info
        System.out.println("Created platforms - Size: " + (PLATFORM_WIDTH/SCALE) + "m x " + (PLATFORM_HEIGHT/SCALE) + "m");
        System.out.println("Background dimensions: " + BACKGROUND_WIDTH_METERS + "m x " + BACKGROUND_HEIGHT_METERS + "m");
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
        // Create some boxes with physics within the background area
        Random random = new Random();
        int numBoxes = 10;
        
        for (int i = 0; i < numBoxes; i++) {
            // Random position within the background area
            double x = random.nextDouble() * BACKGROUND_WIDTH;
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
        
        // Force the floor to stay in place
        if (floor != null) {
            floor.setVelocity(new Vector2D(0, 0));
            floor.setPosition(BACKGROUND_WIDTH / 2, game.getWorldHeight() - FLOOR_HEIGHT / 2);
        }
        
        // Check if player has reached the background boundaries (30m x 20m area)
        float floorY = (float)(game.getWorldHeight() - FLOOR_HEIGHT);
        float backgroundTop = floorY - BACKGROUND_HEIGHT;
        
        if (player != null) {
            // Constrain player position within the 30x20m background area
            double playerX = player.getPosition().getX();
            double playerY = player.getPosition().getY();
            
            // Check horizontal boundaries
            if (playerX < 0) {
                player.setPosition(0, playerY);
                player.setVelocity(new Vector2D(Math.max(0, player.getVelocity().getX()), player.getVelocity().getY()));
            } else if (playerX > BACKGROUND_WIDTH) {
                player.setPosition(BACKGROUND_WIDTH, playerY);
                player.setVelocity(new Vector2D(Math.min(0, player.getVelocity().getX()), player.getVelocity().getY()));
            }
            
            // Check vertical boundaries
            if (playerY < backgroundTop) {
                player.setPosition(playerX, backgroundTop);
                player.setVelocity(new Vector2D(player.getVelocity().getX(), 0));
            }
        }
        
        // Check for scene change (example: pressing ESC for menu)
        if (game.getKeyboardInput().isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            // You would transition to a menu scene here
            // game.getSceneManager().pushScene("menu");
        }
        
        // Check if player has fallen off the world
        if (player.getPosition().getY() > game.getWorldHeight() + 100) {
            resetPlayer();
        }
    }
    
    /**
     * Resets the player position.
     */
    private void resetPlayer() {
        // Reset player to ground level within the background area
        float floorY = (float)(game.getWorldHeight() - FLOOR_HEIGHT);
        
        player.setPosition(BACKGROUND_WIDTH / 2, floorY - PLAYER_HEIGHT / 2 - 10);
        player.setVelocity(new Vector2D(0, 0));
    }
    
    @Override
    public void render(Graphics2D g) {
        // Draw the parallax background layers
        renderBackground(g);
        
        // Apply camera transformations for game objects
        camera.apply(g);
        
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
        // Save the current transform
        var originalTransform = g.getTransform();
        
        // Get camera position
        double cameraX = camera.getPosition().getX();
        double cameraY = camera.getPosition().getY();
        
        // Calculate floor Y position
        float floorY = (float)(game.getWorldHeight() - FLOOR_HEIGHT);
        
        // Render each background layer with parallax effect within the 30x20m area
        for (BackgroundLayer layer : backgroundLayers) {
            if (layer != null) {
                layer.renderWithCamera(g, cameraX, cameraY, game.getWidth(), game.getHeight(), 
                                     floorY, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
            }
        }
        
        // Restore the original transform
        g.setTransform(originalTransform);
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
        g.drawString("Background dimensions: " + BACKGROUND_WIDTH_METERS + "m x " + BACKGROUND_HEIGHT_METERS + "m", 20, 170);
        
        // Player info (only if player exists)
        if (player != null) {
            g.drawString("Player Position: " + formatVector(player.getPosition()), 20, 210);
            g.drawString("Player Velocity: " + formatVector(player.getVelocity()), 20, 230);
            g.drawString("On Ground: " + (player.isOnGround() ? "Yes" : "No"), 20, 250);
            g.drawString("Current Sprite: " + 
                (player.getCurrentSprite() != null ? player.getCurrentSprite().getName() : "None"), 20, 270);
            
            // Player size info
            g.drawString("Player Size: " + (PLAYER_HEIGHT/SCALE) + "m x " + (PLAYER_WIDTH/SCALE) + "m", 20, 290);
        } else {
            g.drawString("Loading player...", 20, 210);
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