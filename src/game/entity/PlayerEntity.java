package game.entity;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import game.Vector2D;
import game.input.KeyboardInput;
import game.physics.AABB;
import game.physics.Collision;
import game.physics.PhysicsObject;
import game.sprites.Sprite;
import game.sprites.SpriteSheetManager;

/**
 * Simple PlayerEntity with basic sprite animations
 */
public class PlayerEntity extends AbstractEntity {
    // Movement parameters
    private static final double MOVE_SPEED = 25;
    private static final double JUMP_FORCE = -550.0;
    private static final double MAX_SPEED = 1000.0;
    private static final double DASH_SPEED = 500.0;
    private static final double DASH_DURATION = 200.0;
    
    // Player state
    private boolean isOnGround = false;
    private boolean canJump = true;
    private boolean isFacingRight = true;
    private boolean isDashing = false;
    private long lastJumpTime = 0;
    private long dashStartTime = 0;
    
    // Input
    private final KeyboardInput input;
    
    // Sprites
    private final SpriteSheetManager spriteManager;
    private Sprite currentSprite;
    private boolean isSpriteLocked = false;
    private long activeSpriteTimer = 0;
    
    // Sprite references
    private Sprite idleSprite;
    private Sprite runSprite;
    private Sprite jumpSprite;
    private Sprite dashSprite;
    private Sprite attackSprite;
    
    /**
     * Creates a new player entity
     */
    public PlayerEntity(double x, double y, int width, int height, KeyboardInput input) {
        super(x, y, width, height);
        this.input = input;
        
        // Initialize sprite manager and load sprites
        this.spriteManager = new SpriteSheetManager();
        loadSprites();
        
        // Player physics properties
        this.mass = 1.0F;
        this.affectedByGravity = true;
        
        // Create collision shape
        this.collisionShape = new AABB(position, width, height);
    }
    
    /**
     * Loads all player sprites
     */
    private void loadSprites() {
        spriteManager.createPlayerSprites();
        
        // Get sprite references
        idleSprite = spriteManager.getSprite("player_idle");
        runSprite = spriteManager.getSprite("player_run");
        jumpSprite = spriteManager.getSprite("player_jump");
        dashSprite = spriteManager.getSprite("player_dash");
        attackSprite = spriteManager.getSprite("player_attack");
        
        // Set initial sprite
        currentSprite = idleSprite;
        
        System.out.println("Player sprites loaded successfully");
    }
    
    @Override
    public void update(long deltaTime) {
        // Reset ground status
        boolean wasOnGround = isOnGround;
        isOnGround = false;
        
        // Handle input
        handleInput(deltaTime);
        
        // Update sprite animation
        if (currentSprite != null) {
            currentSprite.update(deltaTime);
        }
        
        // Update physics
        super.update(deltaTime);
        
        // Reset jump ability when landing
        if (!wasOnGround && isOnGround) {
            canJump = true;
        }
        
        // Handle sprite locking
        if (isSpriteLocked && activeSpriteTimer >= currentSprite.getDuration().toMillis()) {
            activeSpriteTimer = 0;
            isSpriteLocked = false;
            isDashing = false;
        }
        
        if (isSpriteLocked) {
            activeSpriteTimer += deltaTime;
        }
    }
    
    /**
     * Handles player input
     */
    private void handleInput(long deltaTime) {
        double dt = deltaTime / 1000.0;
        long currentTime = System.currentTimeMillis();
        
        // Handle dash
        if (input.isKeyPressed(KeyEvent.VK_W) && !isDashing && currentTime - dashStartTime > 500) {
            if (!isSpriteLocked) {
                currentSprite = dashSprite;
                isSpriteLocked = true;
                isDashing = true;
                dashStartTime = currentTime;
                currentSprite.reset();
                
                // Apply dash velocity
                double dashDirection = isFacingRight ? 1.0 : -1.0;
                velocity.setX(DASH_SPEED * dashDirection);
            }
        }
        
        // Handle attack
        else if (input.isKeyPressed(KeyEvent.VK_E)) {
            if (!isSpriteLocked) {
                currentSprite = attackSprite;
                isSpriteLocked = true;
                currentSprite.reset();
            }
        }
        
        // Handle movement
        else if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            if (!isSpriteLocked) {
                currentSprite = runSprite;
                velocity.add(MOVE_SPEED * 60 * dt, 0);
                isFacingRight = true;
            }
        } else if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            if (!isSpriteLocked) {
                currentSprite = runSprite;
                velocity.add(-MOVE_SPEED * 60 * dt, 0);
                isFacingRight = false;
            }
        }
        
        // Handle idle
        else {
            if (!isSpriteLocked) {
                currentSprite = idleSprite;
            }
            velocity.setX(0);
        }
        
        // Handle jump
        if (input.isKeyPressed(KeyEvent.VK_SPACE) && (currentTime - lastJumpTime > 750)) {
            if (canJump) {
                velocity.setY(JUMP_FORCE);
                
                if (!isSpriteLocked) {
                    currentSprite = jumpSprite;
                    currentSprite.reset();
                }
            }
            
            lastJumpTime = currentTime;
            isOnGround = false;
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible || currentSprite == null) return;
        
        // Calculate sprite position
        int spriteX = (int)(position.getX() - currentSprite.getSize().width / 2);
        int spriteY = (int)(position.getY() - currentSprite.getSize().height / 2);
        
        // Draw sprite (flip if facing left)
        if (isFacingRight) {
            g.drawImage(
                currentSprite.getFrame(),
                spriteX,
                spriteY,
                currentSprite.getSize().width,
                currentSprite.getSize().height,
                null
            );
        } else {
            g.drawImage(
                currentSprite.getFrame(),
                spriteX + currentSprite.getSize().width,
                spriteY,
                -currentSprite.getSize().width,
                currentSprite.getSize().height,
                null
            );
        }
    }
    
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Check if standing on something
        if (collision.getNormal().getY() < -0.5) {
            isOnGround = true;
        }
    }
    
    // Getters
    public boolean isDashing() { return isDashing; }
    public boolean isOnGround() { return isOnGround; }
    public Sprite getCurrentSprite() { return currentSprite; }
}