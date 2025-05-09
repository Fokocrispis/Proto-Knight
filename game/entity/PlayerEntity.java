package game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import game.Vector2D;
import game.input.KeyboardInput;
import game.physics.Collision;
import game.physics.PhysicsObject;

/**
 * A player entity that can be controlled by the user.
 */
public class PlayerEntity extends AbstractEntity {
    // Movement parameters - significantly increased for better feel
    private static final double MOVE_SPEED = 25; // increased from 0.3
    private static final double JUMP_FORCE = -2500.0; // increased from -10.0
    private static final double MAX_SPEED = 1000.0; // increased from 0.5
    private static final double GROUND_FRICTION = 0.9;
    private static final double AIR_RESISTANCE = 0.8;
    
    // Player state
    private boolean isOnGround = false;
    private boolean canJump = true;
    private boolean isFacingRight = true;
    private long lastJumpTime = 0;
    
    // Input
    private final KeyboardInput input;
    
    // Appearance
    private final Color color;
    
    /**
     * Creates a new player entity.
     * 
     * @param x Initial X position
     * @param y Initial Y position
     * @param width Width of the player
     * @param height Height of the player
     * @param input Keyboard input handler
     */
    public PlayerEntity(double x, double y, int width, int height, KeyboardInput input) {
        super(x, y, width, height);
        this.input = input;
        this.color = Color.RED;
        
        // Player-specific physics properties
        this.mass = 1.0f;
        this.affectedByGravity = true;
    }
    
    @Override
    public void update(long deltaTime) {
        // Reset ground status for this frame
        boolean wasOnGround = isOnGround;
        isOnGround = false;
        
        // Process input
        handleInput(deltaTime);
        
        // Apply physics limits
        limitVelocity();
        
        // Base update (updates collision shape)
        super.update(deltaTime);
        
        // Reset jump ability when landing
        if (!wasOnGround && isOnGround) {
            canJump = true;
        }
    }
    
    /**
     * Handles keyboard input for player movement.
     * 
     * @param deltaTime Time elapsed since last update in milliseconds
     */
    private void handleInput(long deltaTime) {
        // Track horizontal movement
        boolean movingHorizontally = false;
        
        // Convert deltaTime to seconds for more intuitive values
        double dt = deltaTime / 1000.0;
        
        // Movement left/right - apply constant acceleration for responsive controls
        if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            // Apply direct acceleration for more responsive movement
            velocity.add(MOVE_SPEED * 60 * dt, 0);
            isFacingRight = true;
            movingHorizontally = true;
        }
        
        if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            velocity.add(-MOVE_SPEED * 60 * dt, 0);
            isFacingRight = false;
            movingHorizontally = true;
        }
        
        // Apply friction/air resistance when not actively moving
        if (!movingHorizontally) {
            double frictionFactor = isOnGround ? GROUND_FRICTION : AIR_RESISTANCE;
            velocity.setX(velocity.getX() * frictionFactor);
        }
        
        // Jump - ensure there's a small delay between jumps
        long currentTime = System.currentTimeMillis();
        if (input.isKeyPressed(KeyEvent.VK_SPACE) && isOnGround && canJump && 
            (currentTime - lastJumpTime > 250)) {
            velocity.setY(JUMP_FORCE);
            isOnGround = false;
            canJump = false;
            lastJumpTime = currentTime;
        }
    }
    
    /**
     * Limits the player's velocity to prevent excessive speeds.
     */
    private void limitVelocity() {
        // Limit horizontal speed
        if (velocity.getX() > MAX_SPEED) {
            velocity.setX(MAX_SPEED);
        } else if (velocity.getX() < -MAX_SPEED) {
            velocity.setX(-MAX_SPEED);
        }
        
        // Small velocity threshold to prevent sliding
        if (Math.abs(velocity.getX()) < 0.1) {
            velocity.setX(0);
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        // Draw the player rectangle
        g.setColor(color);
        g.fillRect(
            (int)(position.getX() - width / 2),
            (int)(position.getY() - height / 2),
            width,
            height
        );
        
        // Draw a line indicating the direction the player is facing
        g.setColor(Color.WHITE);
        int centerX = (int)position.getX();
        int centerY = (int)position.getY();
        int directionX = isFacingRight ? centerX + 20 : centerX - 20;
        g.drawLine(centerX, centerY, directionX, centerY);
        
        // Optionally draw velocity indicator
        g.setColor(Color.YELLOW);
        g.drawLine(
            centerX, 
            centerY, 
            (int)(centerX + velocity.getX() * 5), 
            (int)(centerY + velocity.getY() * 5)
        );
    }
    
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Check if we're standing on something (collision from below)
        if (collision.getNormal().getY() < -0.5) {
            isOnGround = true;
        }
    }
    
    /**
     * Checks if the player is on the ground.
     * 
     * @return True if on ground, false otherwise
     */
    public boolean isOnGround() {
        return isOnGround;
    }
}