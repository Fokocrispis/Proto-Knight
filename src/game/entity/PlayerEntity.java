package game.entity;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import game.Vector2D;
import game.input.KeyboardInput;
import game.physics.AABB;
import game.physics.Collision;
import game.physics.PhysicsObject;
import game.sprites.Sprite;
import game.sprites.SpriteSheetManager;
import game.sprites.AdjustableSequenceSprite;
import game.sprites.LoopingSprite;
import game.sprites.SequenceLoopingSprite;

/**
 * PlayerEntity with properly aligned sprites and hitbox
 * Enhanced with robust state handling for advanced movement mechanics
 */
public class PlayerEntity extends AbstractEntity {
    // Character dimensions - PHYSICAL COLLIDER
    private static final int HITBOX_WIDTH = 40;  // Narrower than sprite for better feel
    private static final int HITBOX_HEIGHT = 140; // Slightly shorter than sprite
    
    // Sprite dimensions
    private static final int SPRITE_WIDTH = 80;   // Full sprite width
    private static final int SPRITE_HEIGHT = 160; // Full sprite height
    
    // Movement parameters
    private static final double WALK_SPEED = 300.0;
    private static final double RUN_SPEED = 600.0;
    private static final double WALK_ACCELERATION = 2000.0;
    private static final double RUN_ACCELERATION = 3000.0;
    private static final double AIR_CONTROL = 0.6;
    private static final double GROUND_FRICTION = 0.95;
    
    // Jump parameters
    private static final double JUMP_FORCE = -750.0;
    private static final double DOUBLE_JUMP_FORCE = -650.0;
    
    // Advanced movement
    private static final double DASH_SPEED = 1200.0;
    private static final double TELEPORT_DISTANCE = 350.0;
    private static final double HOOK_SPEED = 800.0;
    private static final double HOOK_RANGE = 300.0;
    private static final double CLOSE_TELEPORT_DISTANCE = 80.0;
    
    // Slide parameters
    private static final double SLIDE_SPEED_MULTIPLIER = 1.2;
    private static final double SLIDE_FRICTION = 0.992;
    
    // Combat parameters
    private static final int MAX_HEALTH = 100;
    private static final int MAX_MANA = 50;
    
    // Timing parameters
    private static final long DASH_DURATION = 250;
    private static final long DASH_COOLDOWN = 600;
    private static final long TELEPORT_COOLDOWN = 1000;
    private static final long HOOK_COOLDOWN = 1500;
    private static final long COYOTE_TIME = 150;
    private static final long JUMP_BUFFER_TIME = 200;
    private static final long ATTACK_COOLDOWN = 300;
    private static final long SPELL_COOLDOWN = 800;
    private static final long TURN_ANIMATION_DURATION = 150;
    private static final long LANDING_ANIMATION_DURATION = 300;
    private static final long SLIDE_DURATION = 600;
    private static final long SLIDE_COOLDOWN = 800;
    
    // Input reference
    private final KeyboardInput input;
    
    // Combat state
    private int health = MAX_HEALTH;
    private int mana = MAX_MANA;
    private boolean isAttacking = false;
    private boolean isCasting = false;
    
    // Advanced state tracking for animations
    private PlayerState currentState = PlayerState.IDLE;
    private PlayerState previousState = PlayerState.IDLE;
    private MovementContext movementContext = MovementContext.NORMAL;
    private MovementContext previousMovementContext = MovementContext.NORMAL;
    private boolean wasRunning = false;
    private boolean wasDashing = false;
    
    // Player state
    private boolean isFacingRight = true;
    private boolean previousFacingRight = true;
    private boolean wasTryingToMove = false;
    private boolean isJumping = false;
    private boolean isFalling = false;
    private boolean isDashing = false;
    private boolean isHooking = false;
    private boolean isTurning = false;
    private boolean isSliding = false;
    private boolean isCrouching = false;
    private int jumpCount = 0;
    private int maxJumps = 2;
    
    // Timing variables
    private long lastDashTime = 0;
    private long lastTeleportTime = 0;
    private long lastHookTime = 0;
    private long lastGroundTime = 0;
    private long lastJumpPressTime = 0;
    private long lastAttackTime = 0;
    private long lastSpellTime = 0;
    private long stateChangeTime = 0;
    private long dashStartTime = 0;
    private long actionStartTime = 0;
    private long turnStartTime = 0;
    private long landingStartTime = 0;
    private long slideStartTime = 0;
    private long slideEndTime = 0;
    
    // Movement state
    private Vector2D dashDirection = new Vector2D(0, 0);
    private Vector2D hookTarget = null;
    private Vector2D targetVelocity = new Vector2D(0, 0);
    
    // Sprite management
    private final SpriteSheetManager spriteManager;
    private Sprite currentSprite;
    private boolean isSpriteLocked = false;
    private long activeSpriteTimer = 0;
    
    // Sprite references
    private Map<PlayerState, Sprite> stateSprites = new HashMap<>();
    private Map<String, Sprite> contextualSprites = new HashMap<>();
    
    // Buff/Debuff system
    private Map<BuffType, BuffEffect> activeBuffs = new HashMap<>();
    
    // Debug rendering
    private boolean debugRender = false;
    
    /**
     * Creates a new player entity at the specified position with input controls.
     */
    public PlayerEntity(double x, double y, KeyboardInput input) {
        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT);
        this.input = input;
        
        // Initialize physics with the hitbox dimensions
        this.mass = 1.0f;
        this.affectedByGravity = true;
        this.collisionShape = new AABB(position, HITBOX_WIDTH, HITBOX_HEIGHT);
        this.friction = 0.9f;
        this.restitution = 0.0f;
        
        // Initialize sprite management
        this.spriteManager = new SpriteSheetManager();
        loadSprites();
    }
    
    /**
     * Loads all sprites needed for the player's animations
     */
    private void loadSprites() {
        spriteManager.createPlayerSprites();
        
        // Basic movement sprites
        stateSprites.put(PlayerState.IDLE, spriteManager.getSprite("player_idle"));
        stateSprites.put(PlayerState.WALKING, spriteManager.getSprite("player_walk"));
        stateSprites.put(PlayerState.RUNNING, spriteManager.getSprite("player_run"));
        stateSprites.put(PlayerState.JUMPING, spriteManager.getSprite("player_jump"));
        stateSprites.put(PlayerState.FALLING, spriteManager.getSprite("player_fall"));
        stateSprites.put(PlayerState.DASHING, spriteManager.getSprite("player_dash"));
        stateSprites.put(PlayerState.LANDING, spriteManager.getSprite("player_land_quick"));
        stateSprites.put(PlayerState.SLIDING, spriteManager.getSprite("player_slide")); // Add this line
        stateSprites.put(PlayerState.ATTACKING, spriteManager.getSprite("player_roll"));
        
        // Contextual sprites for combined states
        contextualSprites.put("turn_left", spriteManager.getSprite("player_run_turning"));
        contextualSprites.put("turn_right", spriteManager.getSprite("player_run_turning"));
        contextualSprites.put("run_to_stop", spriteManager.getSprite("player_run_stop"));
        contextualSprites.put("run_start", spriteManager.getSprite("player_run_start"));
        
        // Air movement
        contextualSprites.put("jump_running", spriteManager.getSprite("player_jump"));
        contextualSprites.put("jump_dashing", spriteManager.getSprite("player_jump"));
        contextualSprites.put("fall_running", spriteManager.getSprite("player_fall"));
        contextualSprites.put("fall_dashing", spriteManager.getSprite("player_fall"));
        
        // Landing animations
        contextualSprites.put("landing_normal", spriteManager.getSprite("player_land_quick"));
        contextualSprites.put("landing_running", spriteManager.getSprite("player_land_full"));
        contextualSprites.put("landing_dashing", spriteManager.getSprite("player_roll"));
        contextualSprites.put("landing_crouching", spriteManager.getSprite("player_land_full"));
        
        // Sliding and crouching
        contextualSprites.put("sliding", spriteManager.getSprite("player_slide"));
        contextualSprites.put("crouching", spriteManager.getSprite("player_roll"));
        
        // Dash states
        contextualSprites.put("dash_start", spriteManager.getSprite("player_dash"));
        contextualSprites.put("dash_end", spriteManager.getSprite("player_land_quick"));
        
        // Set initial sprite
        currentSprite = stateSprites.get(PlayerState.IDLE);
    }
    
    @Override
    public void update(long deltaTime) {
        long currentTime = System.currentTimeMillis();
        float dt = deltaTime / 1000.0f;
        
        // Store previous state for context
        previousState = currentState;
        previousFacingRight = isFacingRight;
        previousMovementContext = movementContext;
        wasRunning = (currentState == PlayerState.RUNNING);
        wasDashing = isDashing;
        
        // Update buff/debuff system
        updateBuffs(deltaTime);
        
        // Track ground state
        boolean wasOnGround = isOnGround();
        
        // Check for landing with context
        if (!wasOnGround && isOnGround()) {
            onLand();
        }
        
        // Update coyote time
        if (isOnGround()) {
            lastGroundTime = currentTime;
            jumpCount = 0;
        }
        
        // Handle input first
        handleInput(deltaTime);
        
        // Apply movement if not in special state
        if (!isDashing && !isHooking && !isSliding) {
            applyMovement(dt);
        } else if (isSliding) {
            applySlideMovement(dt);
        }
        
        // Call parent update for physics
        super.update(deltaTime);
        
        // Limit maximum speeds
        Vector2D vel = getVelocity();
        if (vel.getX() > 1000) vel.setX(1000);
        if (vel.getX() < -1000) vel.setX(-1000);
        if (vel.getY() > 1500) vel.setY(1500);
        setVelocity(vel);
        
        // Update state and movement context
        updatePlayerState();
        
        // Update sprite animation
        updateSprite(deltaTime);
        
        // Update collision shape to match position
        if (collisionShape != null) {
            collisionShape.setPosition(position);
        }
    }
    
    /**
     * Handles player input and initiates state changes
     */
    private void handleInput(long deltaTime) {
        long currentTime = System.currentTimeMillis();
        
        // Store if we're trying to move this frame
        wasTryingToMove = false;
        boolean facingChanged = false;
        
        // Check for direction change
        if (input.isKeyPressed(KeyEvent.VK_LEFT) && isFacingRight && isOnGround()) {
            facingChanged = true;
        } else if (input.isKeyPressed(KeyEvent.VK_RIGHT) && !isFacingRight && isOnGround()) {
            facingChanged = true;
        }
        
        // Detect crouching and sliding
        if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            if (isOnGround()) {
                if (!isSliding && (currentState == PlayerState.RUNNING || 
                    Math.abs(velocity.getX()) > WALK_SPEED * 1.2) && 
                    currentTime - slideEndTime > SLIDE_COOLDOWN) {
                    // Start sliding (running + down)
                    enterSlidingState();
                } else if (!isSliding) {
                    // Just crouching
                    isCrouching = true;
                    movementContext = MovementContext.CROUCHING;
                }
            }
        } else {
            // Exit crouching when down is released
            isCrouching = false;
            if (movementContext == MovementContext.CROUCHING && !isSliding) {
                movementContext = MovementContext.NORMAL;
            }
        }
        
        // Movement input
        if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            if (isFacingRight && isOnGround() && wasRunning) {
                startTurnAnimation(false);
            }
            isFacingRight = false;
            wasTryingToMove = true;
            if (!isDashing && !isHooking && !isTurning && !isSliding) {
                if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                    // Running
                    targetVelocity.setX(-RUN_SPEED);
                    if (!isCrouching) {
                        movementContext = MovementContext.RUNNING;
                    }
                } else {
                    // Walking
                    targetVelocity.setX(-WALK_SPEED);
                    if (!isCrouching) {
                        movementContext = MovementContext.NORMAL;
                    }
                }
            }
        } else if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            if (!isFacingRight && isOnGround() && wasRunning) {
                startTurnAnimation(true);
            }
            isFacingRight = true;
            wasTryingToMove = true;
            if (!isDashing && !isHooking && !isTurning && !isSliding) {
                if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                    // Running
                    targetVelocity.setX(RUN_SPEED);
                    if (!isCrouching) {
                        movementContext = MovementContext.RUNNING;
                    }
                } else {
                    // Walking
                    targetVelocity.setX(WALK_SPEED);
                    if (!isCrouching) {
                        movementContext = MovementContext.NORMAL;
                    }
                }
            }
        } else {
            // No input - set target to 0
            if (!isDashing && !isHooking && !isSliding) {
                targetVelocity.setX(0);
            }
        }
        
        // Jump input with buffering
        if (input.isKeyJustPressed(KeyEvent.VK_SPACE)) {
            lastJumpPressTime = currentTime;
        }
        
        if (lastJumpPressTime != 0 && currentTime - lastJumpPressTime <= JUMP_BUFFER_TIME) {
            if (isOnGround() || (currentTime - lastGroundTime <= COYOTE_TIME)) {
                // First jump
                if (input.isKeyPressed(KeyEvent.VK_SHIFT) || wasRunning) {
                    movementContext = MovementContext.RUNNING;
                }
                jump(JUMP_FORCE);
                lastJumpPressTime = 0;
            } else if (jumpCount < maxJumps && hasActiveBuff(BuffType.DOUBLE_JUMP)) {
                // Double jump
                jump(DOUBLE_JUMP_FORCE);
                lastJumpPressTime = 0;
            }
        }
        
        // Advanced movement abilities
        handleSpecialMovement(currentTime);
        
        // Combat input
        handleCombatInput(currentTime);
    }
    
    /**
     * Begins a sliding state when running + pressing down
     */
    private void enterSlidingState() {
        isSliding = true;
        slideStartTime = System.currentTimeMillis();
        
        // Apply initial slide momentum
        double slideSpeed = velocity.getX() * SLIDE_SPEED_MULTIPLIER;
        if (Math.abs(slideSpeed) < RUN_SPEED) {
            slideSpeed = isFacingRight ? RUN_SPEED : -RUN_SPEED;
        }
        velocity.setX(slideSpeed);
        
        // Change state
        currentState = PlayerState.SLIDING;
        movementContext = MovementContext.SLIDING;
        stateChangeTime = System.currentTimeMillis();
        
        // Update sprite
        updateSpriteForState();
        
        // Play sound effect if available
        // soundManager.playSoundEffect("player_slide.wav", 0.5f);
    }
    
    /**
     * Exits the sliding state and transitions to an appropriate state
     */
    private void exitSlidingState() {
        isSliding = false;
        slideEndTime = System.currentTimeMillis();
        
        // Apply slight speed reduction on exit
        velocity.setX(velocity.getX() * 0.8);
        
        // Reset context if we're not doing something else
        if (movementContext == MovementContext.SLIDING) {
            movementContext = MovementContext.NORMAL;
        }
        
        // Update state based on current conditions
        if (isOnGround()) {
            if (isCrouching) {
                movementContext = MovementContext.CROUCHING;
            } else if (Math.abs(velocity.getX()) > WALK_SPEED * 1.2) {
                currentState = PlayerState.RUNNING;
                movementContext = MovementContext.RUNNING;
            } else if (Math.abs(velocity.getX()) > 5) {
                currentState = PlayerState.WALKING;
            } else {
                currentState = PlayerState.IDLE;
            }
        } else {
            currentState = velocity.getY() < 0 ? PlayerState.JUMPING : PlayerState.FALLING;
        }
        
        // Update sprite
        updateSpriteForState();
    }
    
    /**
     * Applies sliding movement physics
     */
    private void applySlideMovement(float dt) {
        if (!isSliding) return;
        
        Vector2D velocity = getVelocity();
        
        // Apply sliding friction (higher than normal friction)
        velocity.setX(velocity.getX() * SLIDE_FRICTION);
        
        // Check if sliding is done based on duration
        long currentTime = System.currentTimeMillis();
        if (currentTime - slideStartTime >= SLIDE_DURATION) {
            exitSlidingState();
        }
        // Check if sliding speed is too low
        else if (Math.abs(velocity.getX()) < WALK_SPEED * 0.5) {
            exitSlidingState();
        }
        
        setVelocity(velocity);
    }
    
    /**
     * Starts a turning animation when changing directions while running
     */
    private void startTurnAnimation(boolean turningRight) {
        isTurning = true;
        turnStartTime = System.currentTimeMillis();
        currentState = PlayerState.RUNNING; // Use running state with turn sprite
        updateSpriteForState();
    }
    
    /**
     * Handles special movement input like dash, teleport, hook
     */
    private void handleSpecialMovement(long currentTime) {
        // Handle dash
        if (input.isKeyJustPressed(KeyEvent.VK_W) && currentTime - lastDashTime >= DASH_COOLDOWN) {
            performDash();
            lastDashTime = currentTime;
        }
        
        // Handle teleports
        if (input.isKeyJustPressed(KeyEvent.VK_E) && currentTime - lastTeleportTime >= TELEPORT_COOLDOWN) {
            performTeleport(TELEPORT_DISTANCE);
            lastTeleportTime = currentTime;
        }
        
        if (input.isKeyJustPressed(KeyEvent.VK_Q) && currentTime - lastTeleportTime >= TELEPORT_COOLDOWN / 2) {
            performTeleport(CLOSE_TELEPORT_DISTANCE);
            lastTeleportTime = currentTime;
        }
        
        // Handle hook
        if (input.isKeyJustPressed(KeyEvent.VK_R) && currentTime - lastHookTime >= HOOK_COOLDOWN) {
            performHook();
            lastHookTime = currentTime;
        }
        
        // Update dash state
        if (isDashing && currentTime - dashStartTime >= DASH_DURATION) {
            endDash();
        }
        
        // Update turning state
        if (isTurning && currentTime - turnStartTime >= TURN_ANIMATION_DURATION) {
            isTurning = false;
        }
    }
    
    /**
     * Handles combat input like attacks and spells
     */
    private void handleCombatInput(long currentTime) {
        // Basic attack (using X)
        if (input.isKeyJustPressed(KeyEvent.VK_X) && currentTime - lastAttackTime >= ATTACK_COOLDOWN) {
            performBasicAttack();
            lastAttackTime = currentTime;
        }
        
        // Heavy attack
        if (input.isKeyJustPressed(KeyEvent.VK_C) && currentTime - lastAttackTime >= ATTACK_COOLDOWN * 2) {
            performHeavyAttack();
            lastAttackTime = currentTime;
        }
        
        // Spells
        if (input.isKeyJustPressed(KeyEvent.VK_1) && currentTime - lastSpellTime >= SPELL_COOLDOWN) {
            castFireball();
            lastSpellTime = currentTime;
        }
        
        if (input.isKeyJustPressed(KeyEvent.VK_2) && currentTime - lastSpellTime >= SPELL_COOLDOWN) {
            castHeal();
            lastSpellTime = currentTime;
        }
    }
    
    /**
     * Performs a dash in the current direction
     */
    private void performDash() {
        isDashing = true;
        dashStartTime = System.currentTimeMillis();
        
        // Calculate dash direction
        double dashX = isFacingRight ? 1 : -1;
        double dashY = 0;
        
        // Allow directional dashing
        if (input.isKeyPressed(KeyEvent.VK_UP)) {
            dashY = -0.7;
        } else if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            dashY = 0.7;
        }
        
        // Normalize and apply dash
        dashDirection.set(dashX, dashY);
        if (dashDirection.length() > 0) {
            dashDirection.normalize();
            Vector2D dashVel = dashDirection.times(DASH_SPEED);
            setVelocity(dashVel);
            
            // Cancel gravity during dash if buffed
            if (hasActiveBuff(BuffType.GRAVITY_DASH)) {
                affectedByGravity = false;
            }
        }
        
        currentState = PlayerState.DASHING;
        movementContext = MovementContext.DASHING;
        stateChangeTime = System.currentTimeMillis();
        addBuffEffect(BuffType.DASH_IMMUNITY, DASH_DURATION);
    }
    
    /**
     * Ends the dash state
     */
    private void endDash() {
        isDashing = false;
        affectedByGravity = true;
        
        // Preserve dashing context for landing animations
        if (movementContext == MovementContext.DASHING) {
            if (isOnGround()) {
                // If we're on ground, determine next state
                if (Math.abs(velocity.getX()) > WALK_SPEED * 1.5) {
                    currentState = PlayerState.RUNNING;
                    movementContext = MovementContext.RUNNING;
                } else if (Math.abs(velocity.getX()) > 5) {
                    currentState = PlayerState.WALKING;
                    movementContext = MovementContext.NORMAL;
                } else {
                    currentState = PlayerState.IDLE;
                    movementContext = MovementContext.NORMAL;
                }
            } else {
                // If in air, keep dash context but update state
                currentState = velocity.getY() < 0 ? PlayerState.JUMPING : PlayerState.FALLING;
                // Keep DASHING context
            }
        }
    }
    
    /**
     * Handles landing on the ground, determining the appropriate context and animation
     */
    private void onLand() {
        isJumping = false;
        isFalling = false;
        jumpCount = 0;
        affectedByGravity = true;
        landingStartTime = System.currentTimeMillis();
        
        // Determine landing type based on previous context
        currentState = PlayerState.LANDING;
        
        // Preserve context for landing to determine correct animation
        // We keep the current context (NORMAL, RUNNING, DASHING) when landing
        if (isCrouching) {
            movementContext = MovementContext.CROUCHING;
        } else if (isSliding) {
            // Continue sliding if already sliding
            movementContext = MovementContext.SLIDING;
        }
        // else keep the existing context for proper landing animation
        
        stateChangeTime = System.currentTimeMillis();
        
        // End special states as needed
        if (isHooking) {
            isHooking = false;
            hookTarget = null;
        }
        
        // Play landing sound based on context
        // if (wasDashing) soundManager.playSoundEffect("land_dash.wav", 0.7f);
        // else if (wasRunning) soundManager.playSoundEffect("land_run.wav", 0.6f);
        // else soundManager.playSoundEffect("land.wav", 0.5f);
    }
    
    /**
     * Updates player state based on current conditions and inputs
     */
    private void updatePlayerState() {
        PlayerState newState = currentState;
        long currentTime = System.currentTimeMillis();
        double velocityX = Math.abs(getVelocity().getX());
        double velocityY = getVelocity().getY();
        
        // Handle special states first - these take priority
        if (isSliding) {
            newState = PlayerState.SLIDING;
        } else if (isDashing) {
            if (!isOnGround() && velocityY > 0) {
                // Falling while dashing
                newState = PlayerState.FALLING;
                movementContext = MovementContext.DASHING;
            } else {
                newState = PlayerState.DASHING;
            }
        } else if (isTurning) {
            newState = PlayerState.RUNNING;
        } else if (isAttacking && currentTime - actionStartTime < 300) {
            newState = PlayerState.ATTACKING;
        } else if (isHooking) {
            newState = PlayerState.HOOKING;
        } else if (currentState == PlayerState.LANDING) {
            // Keep landing state until animation completes
            if (currentTime - landingStartTime > LANDING_ANIMATION_DURATION) {
                if (isCrouching) {
                    if (velocityX < 5) {
                        newState = PlayerState.IDLE;
                    } else {
                        newState = PlayerState.WALKING;
                    }
                    movementContext = MovementContext.CROUCHING;
                } else if (velocityX < 5) {
                    newState = PlayerState.IDLE;
                    movementContext = MovementContext.NORMAL;
                } else if (velocityX > WALK_SPEED * 1.5) {
                    newState = PlayerState.RUNNING;
                    movementContext = MovementContext.RUNNING;
                } else {
                    newState = PlayerState.WALKING;
                    movementContext = MovementContext.NORMAL;
                }
            } else {
                newState = PlayerState.LANDING;
                // Keep existing context for landing animation
            }
        } else {
            // Normal state determination
            if (isOnGround()) {
                if (isCrouching) {
                    // Maintain current state but with crouching context
                    if (velocityX < 5) {
                        newState = PlayerState.IDLE;
                    } else {
                        newState = PlayerState.WALKING;
                    }
                    movementContext = MovementContext.CROUCHING;
                } else if (velocityX < 5) {
                    newState = PlayerState.IDLE;
                    movementContext = MovementContext.NORMAL;
                } else {
                    // Check if running (shift key or high speed)
                    if (input.isKeyPressed(KeyEvent.VK_SHIFT) || velocityX > WALK_SPEED * 1.5) {
                        newState = PlayerState.RUNNING;
                        movementContext = MovementContext.RUNNING;
                    } else {
                        newState = PlayerState.WALKING;
                        movementContext = MovementContext.NORMAL;
                    }
                }
            } else {
                // Air states
                if (velocityY < 0) {
                    newState = PlayerState.JUMPING;
                    // Context is preserved
                } else {
                    newState = PlayerState.FALLING;
                    // Context is preserved
                }
            }
        }
        
        // Update state if changed
        if (newState != currentState) {
            previousState = currentState;
            currentState = newState;
            stateChangeTime = currentTime;
            
            // Update sprite based on state and context
            updateSpriteForState();
        } else if (movementContext != previousMovementContext) {
            // If context changed but state didn't, still need to update sprite
            previousMovementContext = movementContext;
            updateSpriteForState();
        }
    }
    
    /**
     * Updates the sprite animation for the current frame
     */
    private void updateSprite(long deltaTime) {
        if (currentSprite != null) {
            currentSprite.update(deltaTime);
            
            // Handle non-looping sprites
            if (currentSprite instanceof LoopingSprite) {
                LoopingSprite loopingSprite = (LoopingSprite) currentSprite;
                if (!loopingSprite.isLooping() && loopingSprite.hasCompleted()) {
                    handleAnimationComplete();
                }
            } else if (currentSprite instanceof AdjustableSequenceSprite) {
                AdjustableSequenceSprite adjustableSprite = (AdjustableSequenceSprite) currentSprite;
                if (!adjustableSprite.isLooping() && adjustableSprite.hasCompleted()) {
                    handleAnimationComplete();
                }
            }
        }
    }
    
    /**
     * Handles state transitions when non-looping animations complete
     */
    private void handleAnimationComplete() {
        if (currentState == PlayerState.LANDING) {
            currentState = PlayerState.IDLE;
            movementContext = MovementContext.NORMAL;
            updateSpriteForState();
        } else if (currentState == PlayerState.ATTACKING) {
            isAttacking = false;
            currentState = PlayerState.IDLE;
            updateSpriteForState();
        }
    }
    
    /**
     * Selects the appropriate sprite based on state and context
     */
    private void updateSpriteForState() {
        Sprite newSprite = null;
        
        // Choose sprite based on state and context
        switch (currentState) {
            case RUNNING:
                if (isTurning) {
                    newSprite = contextualSprites.get("turn_" + (isFacingRight ? "right" : "left"));
                } else {
                    newSprite = stateSprites.get(PlayerState.RUNNING);
                }
                break;
                
            case JUMPING:
                switch (movementContext) {
                    case RUNNING:
                        newSprite = contextualSprites.get("jump_running");
                        break;
                    case DASHING:
                        newSprite = contextualSprites.get("jump_dashing");
                        break;
                    default:
                        newSprite = stateSprites.get(PlayerState.JUMPING);
                        break;
                }
                break;
                
            case FALLING:
                switch (movementContext) {
                    case DASHING:
                        newSprite = contextualSprites.get("fall_dashing");
                        break;
                    case RUNNING:
                        newSprite = contextualSprites.get("fall_running");
                        break;
                    default:
                        newSprite = stateSprites.get(PlayerState.FALLING);
                        break;
                }
                break;
                
            case LANDING:
                switch (movementContext) {
                    case RUNNING:
                        newSprite = contextualSprites.get("landing_running");
                        break;
                    case DASHING:
                        newSprite = contextualSprites.get("landing_dashing");
                        break;
                    case CROUCHING:
                        newSprite = contextualSprites.get("landing_crouching");
                        break;
                    default:
                        newSprite = contextualSprites.get("landing_normal");
                        break;
                }
                break;
                
            case SLIDING:
                newSprite = contextualSprites.get("sliding");
                if (newSprite == null) {
                    newSprite = stateSprites.get(PlayerState.SLIDING);
                }
                break;
                
            case IDLE:
            case WALKING:
                if (movementContext == MovementContext.CROUCHING) {
                    newSprite = contextualSprites.get("crouching");
                    if (newSprite == null) {
                        // Fallback to normal sprites if no crouching sprite
                        newSprite = stateSprites.get(currentState);
                    }
                } else {
                    newSprite = stateSprites.get(currentState);
                }
                break;
                
            case DASHING:
                newSprite = contextualSprites.get("dash_" + (isDashing ? "start" : "end"));
                if (newSprite == null) {
                    newSprite = stateSprites.get(PlayerState.DASHING);
                }
                break;
                
            default:
                newSprite = stateSprites.get(currentState);
                break;
        }
        
        // Update sprite if changed
        if (newSprite != null && newSprite != currentSprite) {
            currentSprite = newSprite;
            resetCurrentSprite();
        }
    }
    
    /**
     * Resets the current sprite animation
     */
    private void resetCurrentSprite() {
        if (currentSprite instanceof LoopingSprite) {
            ((LoopingSprite) currentSprite).reset();
        } else if (currentSprite instanceof AdjustableSequenceSprite) {
            ((AdjustableSequenceSprite) currentSprite).reset();
        } else if (currentSprite != null) {
            currentSprite.reset();
        }
    }
    
    /**
     * Applies standard movement physics
     */
    private void applyMovement(float dt) {
        Vector2D velocity = getVelocity();
        double currentSpeed = velocity.getX();
        double targetSpeed = targetVelocity.getX();
        
        // Get movement modifiers
        double speedMultiplier = getBuffMultiplier(BuffType.SPEED);
        targetSpeed *= speedMultiplier;
        
        // Reduce speed while crouching
        if (isCrouching && isOnGround()) {
            targetSpeed *= 0.5; // Half speed while crouching
        }
        
        // Calculate acceleration based on whether we're on ground
        double acceleration = isOnGround() ? 
            (Math.abs(targetSpeed) > WALK_SPEED ? RUN_ACCELERATION : WALK_ACCELERATION) :
            (WALK_ACCELERATION * AIR_CONTROL);
        
        // Apply acceleration towards target speed
        if (Math.abs(targetSpeed - currentSpeed) < acceleration * dt) {
            velocity.setX(targetSpeed);
        } else if (targetSpeed > currentSpeed) {
            velocity.setX(currentSpeed + acceleration * dt);
        } else if (targetSpeed < currentSpeed) {
            velocity.setX(currentSpeed - acceleration * dt);
        }
        
        // Apply friction when on ground and not trying to move
        if (isOnGround() && !wasTryingToMove) {
            velocity.setX(velocity.getX() * GROUND_FRICTION);
            // Stop completely if very slow
            if (Math.abs(velocity.getX()) < 5) {
                velocity.setX(0);
            }
        }
        
        setVelocity(velocity);
    }
    
    /**
     * Performs a jump
     */
    private void jump(double force) {
        Vector2D velocity = getVelocity();
        double jumpMultiplier = getBuffMultiplier(BuffType.JUMP_HEIGHT);
        velocity.setY(force * jumpMultiplier);
        setVelocity(velocity);
        
        isJumping = true;
        jumpCount++;
        currentState = PlayerState.JUMPING;
        stateChangeTime = System.currentTimeMillis();
    }
    
    /**
     * Performs a teleport in the current facing direction
     */
    private void performTeleport(double distance) {
        double teleportX = isFacingRight ? distance : -distance;
        double teleportY = 0;
        
        // Allow directional teleporting
        if (input.isKeyPressed(KeyEvent.VK_UP)) {
            teleportY = -distance * 0.7;
        } else if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            teleportY = distance * 0.7;
        }
        
        // Check if teleport destination is valid (simplified check)
        Vector2D targetPos = position.plus(teleportX, teleportY);
        
        // Teleport
        position.set(targetPos);
        currentState = PlayerState.DASHING; // Use dash state for teleport animation
        stateChangeTime = System.currentTimeMillis();
        addBuffEffect(BuffType.TELEPORT_IMMUNITY, 500);
    }
    
    /**
     * Performs a hook shot towards the current facing direction
     */
    private void performHook() {
        double hookX = isFacingRight ? HOOK_RANGE : -HOOK_RANGE;
        double hookY = -HOOK_RANGE * 0.5;
        
        Vector2D hookDirection = new Vector2D(hookX, hookY);
        hookTarget = position.plus(hookDirection);
        
        // Check if we have a valid hook target (simplified)
        if (position.distance(hookTarget) <= HOOK_RANGE) {
            isHooking = true;
            affectedByGravity = false;
            currentState = PlayerState.HOOKING;
            stateChangeTime = System.currentTimeMillis();
            
            // Apply immediate velocity toward hook point
            Vector2D hookVelocity = hookTarget.minus(position).normalized().times(HOOK_SPEED);
            setVelocity(hookVelocity);
        }
    }
    
    /**
     * Performs a basic attack
     */
    private void performBasicAttack() {
        isAttacking = true;
        currentState = PlayerState.ATTACKING;
        actionStartTime = System.currentTimeMillis();
        
        // Create attack hitbox (simplified)
        Rectangle attackBox = new Rectangle(
            (int)(position.getX() + (isFacingRight ? 40 : -80)),
            (int)(position.getY() - 40),
            80, 80
        );
    }
    
    /**
     * Performs a heavy attack
     */
    private void performHeavyAttack() {
        isAttacking = true;
        currentState = PlayerState.ATTACKING;
        actionStartTime = System.currentTimeMillis();
        
        // Create larger attack hitbox
        Rectangle attackBox = new Rectangle(
            (int)(position.getX() + (isFacingRight ? 30 : -100)),
            (int)(position.getY() - 50),
            100, 100
        );
        
        // Apply knockback to self
        Vector2D knockback = new Vector2D(isFacingRight ? -100 : 100, -50);
        velocity.add(knockback);
    }
    
    /**
     * Casts a fireball spell
     */
    private void castFireball() {
        isCasting = true;
        currentState = PlayerState.CASTING;
        
        // Consume mana
        mana -= 5;
        if (mana < 0) mana = 0;
        
        System.out.println("Casting Fireball!");
    }
    
    /**
     * Casts a healing spell
     */
    private void castHeal() {
        isCasting = true;
        currentState = PlayerState.CASTING;
        
        // Consume mana
        mana -= 10;
        if (mana < 0) mana = 0;
        
        // Heal player
        health += 20;
        if (health > MAX_HEALTH) health = MAX_HEALTH;
        
        System.out.println("Casting Heal! Health: " + health);
    }
    
    /**
     * Renders the player sprite and UI elements
     */
    public void render(Graphics2D g) {
        if (!visible || currentSprite == null) return;
        
        // Default sprite offset adjustment (used for sprite sheet sprites)
        int SPRITE_OFFSET_X = 0;
        int SPRITE_OFFSET_Y = 50;
        
        // Get sprite dimensions
        int renderedWidth = currentSprite.getSize().width;
        int renderedHeight = currentSprite.getSize().height;
        
        // Calculate sprite position - this depends on the sprite type
        int spriteX, spriteY;
        
        if (currentSprite instanceof AdjustableSequenceSprite) {
            // Use the built-in positioning for adjustable sprites
            AdjustableSequenceSprite adjustableSprite = (AdjustableSequenceSprite) currentSprite;
            spriteX = adjustableSprite.getRenderX(position.getX());
            spriteY = adjustableSprite.getRenderY(position.getY(), HITBOX_HEIGHT);
        } else {
            // Standard positioning for regular sprites
            spriteX = (int)(position.getX() - renderedWidth / 2.0) + SPRITE_OFFSET_X;
            spriteY = (int)(position.getY() - HITBOX_HEIGHT / 2.0 - (renderedHeight - HITBOX_HEIGHT)) + SPRITE_OFFSET_Y;
        }
        
        // Draw sprite (flipped if facing left)
        if (isFacingRight) {
            g.drawImage(currentSprite.getFrame(), 
                       spriteX, spriteY, 
                       renderedWidth, renderedHeight, null);
        } else {
            g.drawImage(currentSprite.getFrame(), 
                       spriteX + renderedWidth, spriteY,
                       -renderedWidth, renderedHeight, null);
        }
        
        // Draw health and mana bars
        drawHealthManaBar(g);
        
        // Draw buff effects
        renderBuffEffects(g);
        
        // Draw hook line if hooking
        if (isHooking && hookTarget != null) {
            g.setColor(Color.CYAN);
            g.drawLine((int)position.getX(), (int)position.getY(), 
                      (int)hookTarget.getX(), (int)hookTarget.getY());
        }
        
        // Draw debug info when enabled
        if (debugRender) {
            renderDebugInfo(g);
        }
    }
    
    /**
     * Draws health and mana bars above the player
     */
    private void drawHealthManaBar(Graphics2D g) {
        // Health bar
        int barWidth = 60;
        int barHeight = 8;
        int barX = (int)position.getX() - barWidth / 2;
        int barY = (int)position.getY() - HITBOX_HEIGHT / 2 - 15; // Above the hitbox
        
        // Background
        g.setColor(Color.BLACK);
        g.fillRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2);
        
        // Background bar
        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Health bar
        g.setColor(Color.RED);
        int healthWidth = (int)((double)health / MAX_HEALTH * barWidth);
        g.fillRect(barX, barY, healthWidth, barHeight);
        
        // Mana bar
        barY += 10;
        barHeight = 6;
        
        // Background
        g.setColor(Color.BLACK);
        g.fillRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2);
        
        // Background bar
        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Mana bar
        g.setColor(Color.BLUE);
        int manaWidth = (int)((double)mana / MAX_MANA * barWidth);
        g.fillRect(barX, barY, manaWidth, barHeight);
    }
    
    /**
     * Renders active buff icons
     */
    private void renderBuffEffects(Graphics2D g) {
        int buffX = (int)position.getX() - 30;
        int buffY = (int)position.getY() - HITBOX_HEIGHT / 2 - 25;
        int buffSize = 10;
        int buffSpacing = 12;
        
        for (Map.Entry<BuffType, BuffEffect> entry : activeBuffs.entrySet()) {
            BuffType type = entry.getKey();
            BuffEffect buff = entry.getValue();
            
            // Draw buff icon
            switch (type) {
                case SPEED:
                    g.setColor(Color.GREEN);
                    break;
                case DEFENSE:
                    g.setColor(Color.BLUE);
                    break;
                case ATTACK_SPEED:
                    g.setColor(Color.ORANGE);
                    break;
                default:
                    g.setColor(Color.WHITE);
                    break;
            }
            
            g.fillOval(buffX, buffY, buffSize, buffSize);
            g.setColor(Color.BLACK);
            g.drawOval(buffX, buffY, buffSize, buffSize);
            
            buffX += buffSpacing;
        }
    }
    
    /**
     * Renders debug information for sprite and hitbox alignment
     */
    private void renderDebugInfo(Graphics2D g) {
        // Draw hitbox
        g.setColor(new Color(255, 0, 0, 128));
        g.drawRect(
            (int)(position.getX() - HITBOX_WIDTH / 2),
            (int)(position.getY() - HITBOX_HEIGHT / 2),
            HITBOX_WIDTH, 
            HITBOX_HEIGHT
        );
        
        // Draw center point
        g.setColor(Color.YELLOW);
        g.fillOval((int)position.getX() - 2, (int)position.getY() - 2, 4, 4);
        
        // Display sprite info
        g.setColor(Color.WHITE);
        String spriteInfo = "Unknown";
        
        if (currentSprite instanceof AdjustableSequenceSprite) {
            AdjustableSequenceSprite sprite = (AdjustableSequenceSprite) currentSprite;
            spriteInfo = String.format("AdjustableSequence: %dx%d, offset: %d,%d",
                sprite.getSize().width, sprite.getSize().height,
                sprite.getOffsetX(), sprite.getOffsetY());
        } else if (currentSprite != null) {
            spriteInfo = String.format("Standard: %dx%d", 
                currentSprite.getSize().width, currentSprite.getSize().height);
        }
        
        g.drawString(spriteInfo, (int)position.getX() - 80, (int)position.getY() - 100);
        
        // Display state info
        g.drawString("State: " + currentState + ", Context: " + movementContext, 
                   (int)position.getX() - 80, (int)position.getY() - 80);
                   
        // Display flags
        StringBuilder flagsInfo = new StringBuilder();
        if (isOnGround()) flagsInfo.append("GROUND ");
        if (isCrouching) flagsInfo.append("CROUCH ");
        if (isSliding) flagsInfo.append("SLIDE ");
        if (isDashing) flagsInfo.append("DASH ");
        if (isJumping) flagsInfo.append("JUMP ");
        
        g.drawString(flagsInfo.toString(), (int)position.getX() - 80, (int)position.getY() - 60);
    }
    
    /**
     * Handles collision with other physics objects
     */
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Reset special states on collision
        if (isDashing && other.getMass() > 0) {
            endDash();
        }
        
        // Handle sliding collisions - end slide if hitting a wall
        if (isSliding && Math.abs(collision.getNormal().getX()) > 0.5) {
            exitSlidingState();
        }
        
        // Check for ground collision
        if (collision.getNormal().getY() < -0.5 && other.getMass() <= 0) {
            setOnGround(true);
        }
    }
    
    // Buff system methods
    
    /**
     * Updates buff timers
     */
    private void updateBuffs(long deltaTime) {
        Iterator<Map.Entry<BuffType, BuffEffect>> iterator = activeBuffs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BuffType, BuffEffect> entry = iterator.next();
            BuffEffect buff = entry.getValue();
            buff.timeRemaining -= deltaTime;
            
            if (buff.timeRemaining <= 0) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Adds a buff effect to the player
     */
    public void addBuffEffect(BuffType type, long duration) {
        BuffEffect effect = new BuffEffect(1.0f, duration);
        
        // Set specific multipliers for different buff types
        switch (type) {
            case SPEED:
                effect.multiplier = 1.5f;
                break;
            case JUMP_HEIGHT:
                effect.multiplier = 1.3f;
                break;
            case DOUBLE_JUMP:
                maxJumps = 2;
                effect.multiplier = 1.0f;
                break;
            case GRAVITY_DASH:
            case DASH_IMMUNITY:
            case TELEPORT_IMMUNITY:
                effect.multiplier = 1.0f;
                break;
            default:
                effect.multiplier = 1.0f;
                break;
        }
        
        activeBuffs.put(type, effect);
    }
    
    /**
     * Gets the multiplier value for a buff type
     */
    private double getBuffMultiplier(BuffType type) {
        BuffEffect buff = activeBuffs.get(type);
        return buff != null ? buff.multiplier : 1.0;
    }
    
    /**
     * Checks if a buff type is active
     */
    private boolean hasActiveBuff(BuffType type) {
        return activeBuffs.containsKey(type);
    }
    
    /**
     * Toggles debug rendering
     */
    public void toggleDebugRender() {
        debugRender = !debugRender;
    }
    
    // Getters and setters
    
    public PlayerState getCurrentState() { return currentState; }
    public boolean isOnGround() { return onGround; }
    public boolean isWalking() { return currentState == PlayerState.WALKING; }
    public boolean isRunning() { return currentState == PlayerState.RUNNING; }
    public boolean isDashing() { return isDashing; }
    public boolean isHooking() { return isHooking; }
    public boolean isSliding() { return isSliding; }
    public boolean isCrouching() { return isCrouching; }
    public boolean isFacingRight() { return isFacingRight; }
    public Sprite getCurrentSprite() { return currentSprite; }
    public Map<BuffType, BuffEffect> getActiveBuffs() { return new HashMap<>(activeBuffs); }
    public int getHealth() { return health; }
    public int getMana() { return mana; }
    public boolean isAttacking() { return isAttacking; }
    public boolean isCasting() { return isCasting; }
    public MovementContext getMovementContext() { return movementContext; }
    
    /**
     * Movement context enum for animation decisions
     */
    private enum MovementContext {
        NORMAL,
        RUNNING,
        DASHING,
        CROUCHING,
        SLIDING
    }
}