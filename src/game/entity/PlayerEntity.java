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
 * Simple PlayerEntity with correct animations
 */
public class PlayerEntity extends AbstractEntity {
    // Character dimensions (1.6m tall)
    private static final int PLAYER_HEIGHT = 160; // 1.6m
    private static final int PLAYER_WIDTH = 80;   // 0.8m
    
    // Movement parameters
    private static final double MOVE_SPEED = 300.0;
    private static final double RUN_SPEED = 500.0;
    private static final double JUMP_FORCE = -600.0;
    private static final double MAX_FALL_SPEED = 800.0;
    private static final double DASH_SPEED = 800.0;
    private static final double DASH_DURATION = 250.0;
    
    // Player state
    private boolean isOnGround = false;
    private boolean wasOnGround = false;
    private boolean canJump = true;
    private boolean isFacingRight = true;
    private boolean isDashing = false;
    private boolean isRolling = false;
    private boolean isBlocking = false;
    
    // Timing variables
    private long lastJumpTime = 0;
    private long dashStartTime = 0;
    private long rollStartTime = 0;
    private long blockStartTime = 0;
    
    // Input
    private final KeyboardInput input;
    
    // Sprites
    public final SpriteSheetManager spriteManager;
    private Sprite currentSprite;
    private boolean isSpriteLocked = false;
    private long activeSpriteTimer = 0;
    
    // Sprite references
    private Sprite idleSprite;
    private Sprite walkSprite;
    private Sprite runStartSprite;
    private Sprite runSprite;
    private Sprite jumpSprite;
    private Sprite fallSprite;
    private Sprite landQuickSprite;
    private Sprite landFullSprite;
    private Sprite dashSprite;
    private Sprite rollSprite;
    private Sprite blockSprite;
    private boolean isRunning = false;
    private boolean isWalking = false;
    
    /**
     * Creates a 1.6m tall player entity
     */
    public PlayerEntity(double x, double y, KeyboardInput input) {
        super(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
        this.input = input;
        
        // Initialize sprite manager and load sprites
        this.spriteManager = new SpriteSheetManager();
        loadSprites();
        
        // Player physics properties
        this.mass = 1.0F;
        this.affectedByGravity = true;
        
        // Create collision shape
        this.collisionShape = new AABB(position, PLAYER_WIDTH, PLAYER_HEIGHT);
    }
    
    /**
     * Loads all player sprites
     */
    private void loadSprites() {
        spriteManager.createPlayerSprites();
        
        // Get sprite references
        idleSprite = spriteManager.getSprite("player_idle");
        walkSprite = spriteManager.getSprite("player_walk");
        runStartSprite = spriteManager.getSprite("player_run_start");
        runSprite = spriteManager.getSprite("player_run");
        jumpSprite = spriteManager.getSprite("player_jump");
        fallSprite = spriteManager.getSprite("player_fall");
        landQuickSprite = spriteManager.getSprite("player_land_quick");
        landFullSprite = spriteManager.getSprite("player_land_full");
        dashSprite = spriteManager.getSprite("player_dash");
        rollSprite = spriteManager.getSprite("player_roll");
        blockSprite = spriteManager.getSprite("player_block");
        
        // Set initial sprite
        currentSprite = idleSprite;
        
        System.out.println("Player sprites loaded successfully");
    }
    
    @Override
    public void update(long deltaTime) {
        // Reset ground status
        wasOnGround = isOnGround;
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
            
            // Play landing animation
            if (Math.abs(velocity.getY()) > 400) {
                currentSprite = landFullSprite;
                isSpriteLocked = true;
                activeSpriteTimer = 0;
            } else {
                currentSprite = landQuickSprite;
                isSpriteLocked = true;
                activeSpriteTimer = 0;
            }
        }
        
        // Handle sprite locking
        if (isSpriteLocked && activeSpriteTimer >= currentSprite.getDuration().toMillis()) {
            activeSpriteTimer = 0;
            isSpriteLocked = false;
            
            // Reset states
            isDashing = false;
            isRolling = false;
            isBlocking = false;
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
        
        // Handle blocking
        if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
            if (!isSpriteLocked) {
                currentSprite = blockSprite;
                isSpriteLocked = true;
                isBlocking = true;
                blockStartTime = currentTime;
                activeSpriteTimer = 0;
            }
            return; // Skip other inputs while blocking
        }
        
        // Handle dash
        if (input.isKeyPressed(KeyEvent.VK_W) && !isDashing && currentTime - dashStartTime > 500) {
            if (!isSpriteLocked) {
                currentSprite = dashSprite;
                isSpriteLocked = true;
                isDashing = true;
                dashStartTime = currentTime;
                activeSpriteTimer = 0;
                
                // Apply dash velocity
                double dashDirection = isFacingRight ? 1.0 : -1.0;
                velocity.setX(DASH_SPEED * dashDirection);
            }
        }
        
        // Handle roll
        else if (input.isKeyPressed(KeyEvent.VK_E) && !isRolling && currentTime - rollStartTime > 500) {
            if (!isSpriteLocked) {
                currentSprite = rollSprite;
                isSpriteLocked = true;
                isRolling = true;
                rollStartTime = currentTime;
                activeSpriteTimer = 0;
            }
        }
        
        // Handle movement
        else if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            if (!isSpriteLocked) {
                isFacingRight = true;
                
                // Check if running
                if (input.isKeyPressed(KeyEvent.VK_X)) {
                    // Use run animations
                    if (currentSprite != runSprite && currentSprite != runStartSprite) {
                        currentSprite = runStartSprite;
                        activeSpriteTimer = 0;
                    } else if (currentSprite == runStartSprite && 
                              activeSpriteTimer >= runStartSprite.getDuration().toMillis()) {
                        currentSprite = runSprite;
                    }
                    velocity.setX(RUN_SPEED);
                } else {
                    // Use walk animation
                    currentSprite = walkSprite;
                    velocity.setX(MOVE_SPEED);
                }
            }
        }
        else if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            if (!isSpriteLocked) {
                isFacingRight = false;
                
                // Check if running
                if (input.isKeyPressed(KeyEvent.VK_X)) {
                    // Use run animations
                    if (currentSprite != runSprite && currentSprite != runStartSprite) {
                        currentSprite = runStartSprite;
                        activeSpriteTimer = 0;
                    } else if (currentSprite == runStartSprite && 
                              activeSpriteTimer >= runStartSprite.getDuration().toMillis()) {
                        currentSprite = runSprite;
                    }
                    velocity.setX(-RUN_SPEED);
                } else {
                    // Use walk animation
                    currentSprite = walkSprite;
                    velocity.setX(-MOVE_SPEED);
                }
            }
        }
        // Handle idle
        else {
            if (!isSpriteLocked) {
                currentSprite = idleSprite;
            }
            if(!isDashing)
                velocity.setX(0);
        }
        
        // Handle jump
        if (input.isKeyPressed(KeyEvent.VK_SPACE) && canJump && currentTime - lastJumpTime > 500) {
            velocity.setY(JUMP_FORCE);
            lastJumpTime = currentTime;
            isOnGround = false;
            canJump = false;
            
            if (!isSpriteLocked) {
                currentSprite = jumpSprite;
            }
        }
        
        // Handle fall sprite
        if (!isOnGround && velocity.getY() > 0 && !isSpriteLocked) {
            currentSprite = fallSprite;
        }
        
        // Update animation timer for non-locked sprites
        if (!isSpriteLocked) {
            activeSpriteTimer += deltaTime;
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible || currentSprite == null) return;
        
        // Calculate sprite position
        int spriteX = (int)(position.getX() - currentSprite.getSize().width / 2);
        int spriteY = (int)(position.getY() - currentSprite.getSize().height / 2);
        
        // Adjust Y position for 1.6m height scaling
        spriteY += 10; // Fine-tune this value as needed
        
        // Draw sprite (flip if facing left)
        if (isFacingRight) {
            g.drawImage(
                currentSprite.getFrame(),
                spriteX,
                spriteY,
                currentSprite.getSize().width,
                (int)(currentSprite.getSize().height * 1.28),
                null
            );
        } else {
            g.drawImage(
                currentSprite.getFrame(),
                spriteX + currentSprite.getSize().width,
                spriteY,
                -currentSprite.getSize().width,
                (int)(currentSprite.getSize().height * 1.28),
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
    public boolean isRolling() { return isRolling; }
    public boolean isBlocking() { return isBlocking; }
    public boolean isFacingRight() { return isFacingRight; }
    public Sprite getCurrentSprite() { return currentSprite; }
    public boolean isRunning() { return isRunning;}
    public boolean isWalking() { return isWalking; }

}