// src/game/scene/GameplayScene.java
package game.scene;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Random;

import game.Game;
import game.Vector2D;
import game.entity.BoxEntity;
import game.entity.PlayerEntity;
import game.physics.PhysicsObject;

/**
 * Main gameplay scene with the player, platforms, and physics objects.
 */
public class GameplayScene extends AbstractScene {
    private PlayerEntity player;
    private BoxEntity floor;
    private boolean isPaused = false;
    
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
    }
    
    @Override
    protected void createGameObjects() {
        // Create a floor first
        createFloor();
        
        // Create the player at ground level
        float floorY = game.getWorldHeight() - 80; // Floor Y position (from createFloor method)
        int playerHeight = 60;
        player = new PlayerEntity(
            game.getWidth() / 2,
            floorY - playerHeight / 2, // Position player on top of the floor
            40,
            playerHeight,
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
     * Creates the floor for the level.
     */
    private void createFloor() {
        // Create a floor
        floor = new BoxEntity(
            game.getWorldWidth() / 2,
            game.getWorldHeight() - 40,
            (int)game.getWorldWidth(),
            80,
            new Color(100, 70, 30) // Brown color for the floor
        );
        
        // Floor doesn't move with physics
        floor.setAffectedByGravity(false);
        floor.setMass(0); // Infinite mass (immovable)
        
        // Set velocity explicitly to zero to ensure it doesn't move
        floor.setVelocity(new game.Vector2D(0, 0));
        
        // Add floor to game objects and physics system
        addGameObject(floor);
        game.getPhysicsSystem().addObject(floor);
    }
    
    /**
     * Creates platforms in the level.
     */
    private void createPlatforms() {
        // Create some platforms at different heights
        int numPlatforms = 5;
        int platformWidth = 200;
        int platformHeight = 20;
        float worldWidth = game.getWorldWidth();
        float worldHeight = game.getWorldHeight();
        
        for (int i = 0; i < numPlatforms; i++) {
            // Calculate position
            double x = worldWidth * (i + 1) / (numPlatforms + 1);
            double y = worldHeight - 200 - (i * 100);
            
            // Create platform
            BoxEntity platform = new BoxEntity(
                x, y, platformWidth, platformHeight, new Color(50, 150, 50)
            );
            
            // Platforms don't move
            platform.setAffectedByGravity(false);
            platform.setMass(0); // Infinite mass
            platform.setVelocity(new game.Vector2D(0, 0)); // Explicitly set velocity to zero
            
            // Add to game objects and physics
            addGameObject(platform);
            game.getPhysicsSystem().addObject(platform);
        }
    }
    
    /**
     * Creates physics objects in the level.
     */
    private void createPhysicsObjects() {
        // Create some boxes with physics
        Random random = new Random();
        int numBoxes = 10;
        
        for (int i = 0; i < numBoxes; i++) {
            // Random position near the top
            double x = random.nextDouble() * game.getWorldWidth();
            double y = random.nextDouble() * 300;
            
            // Random size
            int size = 20 + random.nextInt(40);
            
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
        
        // Force the floor to stay in place
        if (floor != null) {
            // Reset the floor's velocity to zero each frame to ensure it doesn't move
            floor.setVelocity(new Vector2D(0, 0));
            // Reset the floor's position to its original location
            floor.setPosition(game.getWorldWidth() / 2, game.getWorldHeight() - 40);
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
        // Reset player to ground level
        float floorY = game.getWorldHeight() - 80;
        int playerHeight = player.getHeight();
        
        player.setPosition(game.getWidth() / 2, floorY - playerHeight / 2);
        player.setVelocity(new game.Vector2D(0, 0));
    }
    
    @Override
    public void render(Graphics2D g) {
        // Draw a background gradient
        renderBackground(g);
        
        // Render all game objects with camera transform
        super.render(g);
        
        // Render UI (after resetting camera)
        renderUI(g);
    }
    
    /**
     * Renders the scene background.
     * 
     * @param g The graphics context.
     */
    private void renderBackground(Graphics2D g) {
        // Apply camera transformations
        camera.apply(g);
        
        // Draw a gradient background representing the sky
        float worldWidth = game.getWorldWidth();
        float worldHeight = game.getWorldHeight();
        
        // Draw sky gradient (simplified version - in a real game, use proper gradient painting)
        g.setColor(new Color(135, 206, 235)); // Sky blue
        g.fillRect(0, 0, (int)worldWidth, (int)worldHeight);
        
        // Draw some decorative elements (clouds, mountains, etc.)
        g.setColor(new Color(255, 255, 255, 100)); // Semi-transparent white
        
        // Draw a few "clouds"
        g.fillOval(200, 150, 200, 80);
        g.fillOval(600, 100, 300, 100);
        g.fillOval(1200, 200, 250, 90);
        g.fillOval(1800, 150, 220, 70);
        g.fillOval(2400, 120, 280, 110);
        g.fillOval(3000, 180, 320, 90);
        
        // Reset camera for normal rendering
        camera.reset(g);
    }
    
    @Override
    protected void renderUI(Graphics2D g) {
        // Draw game UI elements
        g.setColor(Color.WHITE);
        
        // Display controls
        g.drawString("Controls:", 20, 30);
        g.drawString("Arrow Keys - Move", 20, 50);
        g.drawString("Space - Jump", 20, 70);
        g.drawString("P - Pause", 20, 90);
        g.drawString("ESC - Menu", 20, 110);
        
        // Player info
        g.drawString("Player Position: " + formatVector(player.getPosition()), 20, 150);
        g.drawString("Player Velocity: " + formatVector(player.getVelocity()), 20, 170);
        g.drawString("On Ground: " + (player.isOnGround() ? "Yes" : "No"), 20, 190);
        
        // Draw pause screen if paused
        if (isPaused) {
            drawPauseScreen(g);
        }
    }
    
    /**
     * Formats a vector for display.
     * 
     * @param vector The vector to format.
     * @return A formatted string representation.
     */
    private String formatVector(game.Vector2D vector) {
        return String.format("(%.1f, %.1f)", vector.getX(), vector.getY());
    }
    
    /**
     * Draws the pause screen overlay.
     * 
     * @param g The graphics context.
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