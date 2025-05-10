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
 * A player entity with sprite animations and dynamic hitbox.
 */
public class PlayerEntity extends AbstractEntity {
    // Movement parameters
    private static final double MOVE_SPEED = 25;
    private static final double JUMP_FORCE = -550.0;
    private static final double MAX_SPEED = 1000.0;
    private static final double GROUND_FRICTION = 0.9;
    private static final double AIR_RESISTANCE = 0.8;
    
    // Sprite constants
    private static final int X_OFFSET = 96; // Offset for collision box relative to sprite
    private static final double SCALE = 100.0; // 1 meter = 100 pixels
    
    // Hitbox dimensions (smaller than the actual sprite)
    private final int baseWidth;  // Width when idle
    private final int baseHeight; // Height when idle
    
    // Player state
    private boolean isOnGround = false;
    private boolean canJump = true;
    private boolean isFacingRight = true;
    private boolean isRunning = false;
    private long lastJumpTime = 0;
    
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
    private Sprite crouchSprite;
    private Sprite attackSprite;
    private Sprite airAttackSprite;
    private Sprite jumpSprite;
    
    // Sprite position offset adjustments
    private static final int FEET_OFFSET_Y = 15; // Offset for feet alignment
    
    /**
     * Creates a new player entity with dynamic hitbox.
     */
    public PlayerEntity(double x, double y, int width, int height, KeyboardInput input) {
        super(x, y, width, height);
        this.input = input;
        this.baseWidth = width;
        this.baseHeight = height;
        
        // Initialize sprite manager
        this.spriteManager = new SpriteSheetManager();
        
        // Load player sprites
        spriteManager.createPlayerSprites();
        
        // Get sprite references
        idleSprite = spriteManager.getSprite("player_idle");
        runSprite = spriteManager.getSprite("player_run");
        crouchSprite = spriteManager.getSprite("player_crouch");
        attackSprite = spriteManager.getSprite("player_attack");
        airAttackSprite = spriteManager.getSprite("player_air_attack");
        jumpSprite = spriteManager.getSprite("player_jump");
        
        // Set initial sprite
        currentSprite = idleSprite;
        
        // Player-specific physics properties
        this.mass = 1.0F;
        this.affectedByGravity = true;
        
        // Create initial collision shape
        updateCollisionShape();
    }
    
    /**
     * Updates the collision shape based on current state.
     */
    private void updateCollisionShape() {
        if (isRunning && currentSprite != null) {
            // Get the actual sprite dimensions
            int spriteWidth = currentSprite.getSize().width;
            int spriteHeight = currentSprite.getSize().height;
            
            // Calculate the extension from the center to 3/5 of the sprite width
            int halfBaseWidth = baseWidth / 2;
            int threeFifthSpriteWidth = (int)(spriteWidth * 0.6) / 2;
            
            if (isFacingRight) {
                // Extend to 3/5 of the sprite width to the right
                int extension = threeFifthSpriteWidth - halfBaseWidth;
                this.width = baseWidth + extension;
                this.collisionShape = new AABB(
                    (float)(position.getX() + extension / 2),
                    (float)position.getY(),
                    this.width,
                    this.height
                );
            } else {
                // Extend to 3/5 of the sprite width to the left
                int extension = threeFifthSpriteWidth - halfBaseWidth;
                this.width = baseWidth + extension;
                this.collisionShape = new AABB(
                    (float)(position.getX() - extension / 2),
                    (float)position.getY(),
                    this.width,
                    this.height
                );
            }
        } else {
            // Normal hitbox when idle
            this.width = baseWidth;
            this.collisionShape = new AABB(position, this.width, this.height);
        }
    }
    
    @Override
    public void update(long deltaTime) {
        // Reset ground status for this frame
        boolean wasOnGround = isOnGround;
        isOnGround = false;
        
        // Process input and update sprite
        handleInput(deltaTime);
        
        // Update current sprite animation
        if (currentSprite != null) {
            currentSprite.update(deltaTime);
        }
        
        // Apply physics limits
        limitVelocity();
        
        // Base update (updates collision shape based on current state)
        updateCollisionShape();
        super.update(deltaTime);
        
        // Reset jump ability when landing
        if (!wasOnGround && isOnGround) {
            canJump = true;
        }
        
        // Handle sprite lock timer
        if (isSpriteLocked && activeSpriteTimer >= currentSprite.getDuration().toMillis()) {
            activeSpriteTimer = 0;
            isSpriteLocked = false;
        }
        if (isSpriteLocked) {
            activeSpriteTimer += deltaTime;
        }
    }
    
    /**
     * Handles keyboard input and sprite selection.
     */
    private void handleInput(long deltaTime) {
        // Track movement state
        boolean wasRunning = isRunning;
        isRunning = false;
        
        // Convert deltaTime to seconds for more intuitive values
        double dt = deltaTime / 1000.0;
        
        // Handle attack input
        if (input.isKeyPressed(KeyEvent.VK_E) && input.isKeyPressed(KeyEvent.VK_DOWN)) {
            if (!isSpriteLocked) {
                currentSprite = airAttackSprite;
                isSpriteLocked = true;
                currentSprite.reset();
            }
        } else if (input.isKeyPressed(KeyEvent.VK_E)) {
            if (!isSpriteLocked) {
                currentSprite = attackSprite;
                isSpriteLocked = true;
                currentSprite.reset();
            }
        }
        // Handle crouch input
        else if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            if (!isSpriteLocked) {
                currentSprite = crouchSprite;
                velocity.setY(0);
                isOnGround = true;
                canJump = true;
            }
        }
        // Handle movement input
        else if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            if (!isSpriteLocked) {
                currentSprite = runSprite;
                velocity.add(MOVE_SPEED * 60 * dt, 0);
                isFacingRight = true;
                isRunning = true;
            }
        } else if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            if (!isSpriteLocked) {
                currentSprite = runSprite;
                velocity.add(-MOVE_SPEED * 60 * dt, 0);
                isFacingRight = false;
                isRunning = true;
            }
        }
        // Handle idle state
        else {
            if (!isSpriteLocked) {
                currentSprite = idleSprite;
            }
            velocity.setX(0);
        }
        
        // Check if we need to update hitbox due to state change
        if (wasRunning != isRunning) {
            updateCollisionShape();
        }
        
        // Apply friction/air resistance when not actively moving
        if (!isRunning) {
            double frictionFactor = isOnGround ? GROUND_FRICTION : AIR_RESISTANCE;
            velocity.setX(velocity.getX() * frictionFactor);
        }
        
        // Handle jump input
        long currentTime = System.currentTimeMillis();
        if (input.isKeyPressed(KeyEvent.VK_SPACE) && 
            (currentTime - lastJumpTime > 750)) {
            velocity.setY(JUMP_FORCE);
            lastJumpTime = currentTime;
            isOnGround = false;
            
            if (!isSpriteLocked) {
                currentSprite = jumpSprite;
                currentSprite.reset();
            }
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
        if (!visible || currentSprite == null) return;
        
        // Calculate the sprite position (with feet offset adjustment)
        int spriteX, spriteY;
        
        // Base sprite position centered on the collision box
        if (isFacingRight) {
            spriteX = (int)(position.getX() - currentSprite.getSize().width / 2);
            spriteY = (int)(position.getY() - currentSprite.getSize().height / 2 - FEET_OFFSET_Y); // Subtract to move the sprite up
        } else {
            // When facing left, maintain the same positioning
            spriteX = (int)(position.getX() - currentSprite.getSize().width / 2);
            spriteY = (int)(position.getY() - currentSprite.getSize().height / 2 - FEET_OFFSET_Y); // Subtract to move the sprite up
        }
        
        // Handle sprite flipping
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
            // Flip sprite horizontally
            g.drawImage(
                currentSprite.getFrame(),
                spriteX + currentSprite.getSize().width,
                spriteY,
                -currentSprite.getSize().width,
                currentSprite.getSize().height,
                null
            );
        }
        
        // Debug: Draw collision box (optional - uncomment to see hitbox)
        /*
        g.setColor(new java.awt.Color(255, 0, 0, 80));
        g.fillRect(
            (int)(position.getX() - width / 2),
            (int)(position.getY() - height / 2),
            width,
            height
        );
        
        // Debug: Draw base hitbox for comparison
        g.setColor(new java.awt.Color(0, 255, 0, 80));
        g.drawRect(
            (int)(position.getX() - baseWidth / 2),
            (int)(position.getY() - baseHeight / 2),
            baseWidth,
            baseHeight
        );
        */
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
     */
    public boolean isOnGround() {
        return isOnGround;
    }
    
    /**
     * Gets the current sprite.
     */
    public Sprite getCurrentSprite() {
        return currentSprite;
    }
    
    /**
     * Sets the current sprite.
     */
    public void setCurrentSprite(Sprite sprite) {
        this.currentSprite = sprite;
        if (sprite != null) {
            sprite.reset();
        }
    }
}