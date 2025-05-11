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
import game.sprites.LoopingSprite;

/**
 * PlayerEntity with properly aligned sprites and hitbox
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
        stateSprites.put(PlayerState.ATTACKING, spriteManager.getSprite("player_roll"));
        
        // Contextual sprites for better animations
        contextualSprites.put("turn_left", spriteManager.getSprite("player_run_turning"));    // You may need to create these
        contextualSprites.put("turn_right", spriteManager.getSprite("player_run_turning"));   // Mirror this in rendering
        contextualSprites.put("run_to_stop", spriteManager.getSprite("player_run_stop"));
        contextualSprites.put("run_start", spriteManager.getSprite("player_run_start"));
        contextualSprites.put("jump_running", spriteManager.getSprite("player_jump"));
        contextualSprites.put("landing_normal", spriteManager.getSprite("player_land_quick"));
        contextualSprites.put("landing_running", spriteManager.getSprite("player_land_full"));
        contextualSprites.put("landing_dashing", spriteManager.getSprite("player_roll"));
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
        if (!isDashing && !isHooking) {
            applyMovement(dt);
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
        
        // Movement input
        if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            if (isFacingRight && isOnGround() && wasRunning) {
                startTurnAnimation(false);
            }
            isFacingRight = false;
            wasTryingToMove = true;
            if (!isDashing && !isHooking && !isTurning) {
                if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                    // Running
                    targetVelocity.setX(-RUN_SPEED);
                    movementContext = MovementContext.RUNNING;
                } else {
                    // Walking
                    targetVelocity.setX(-WALK_SPEED);
                    movementContext = MovementContext.NORMAL;
                }
            }
        } else if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            if (!isFacingRight && isOnGround() && wasRunning) {
                startTurnAnimation(true);
            }
            isFacingRight = true;
            wasTryingToMove = true;
            if (!isDashing && !isHooking && !isTurning) {
                if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                    // Running
                    targetVelocity.setX(RUN_SPEED);
                    movementContext = MovementContext.RUNNING;
                } else {
                    // Walking
                    targetVelocity.setX(WALK_SPEED);
                    movementContext = MovementContext.NORMAL;
                }
            }
        } else {
            // No input - set target to 0
            if (!isDashing && !isHooking) {
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
    
    private void startTurnAnimation(boolean turningRight) {
        isTurning = true;
        turnStartTime = System.currentTimeMillis();
        currentState = PlayerState.RUNNING; // Use running state with turn sprite
        updateSpriteForState();
    }
    
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
    
    private void endDash() {
        isDashing = false;
        affectedByGravity = true;
        movementContext = wasDashing ? MovementContext.DASHING : movementContext;
        currentState = PlayerState.IDLE;
    }
    
    private void onLand() {
        isJumping = false;
        isFalling = false;
        jumpCount = 0;
        affectedByGravity = true;
        landingStartTime = System.currentTimeMillis();
        
        // Determine landing animation based on previous context
        if (wasDashing) {
            currentState = PlayerState.LANDING;
            movementContext = MovementContext.DASHING;
        } else if (wasRunning) {
            currentState = PlayerState.LANDING;
            movementContext = MovementContext.RUNNING;
        } else {
            currentState = PlayerState.LANDING;
            movementContext = MovementContext.NORMAL;
        }
        
        stateChangeTime = System.currentTimeMillis();
        
        // Stop hook if landing
        if (isHooking) {
            isHooking = false;
            hookTarget = null;
        }
    }
    
    private void updatePlayerState() {
        PlayerState newState = currentState;
        long currentTime = System.currentTimeMillis();
        double velocityX = Math.abs(getVelocity().getX());
        double velocityY = getVelocity().getY();
        
        // Handle special states first
        if (isDashing) {
            newState = PlayerState.DASHING;
        } else if (isTurning) {
            newState = PlayerState.RUNNING; // Use running state with turn sprite
        } else if (isAttacking && currentTime - actionStartTime < 300) {
            newState = PlayerState.ATTACKING;
        } else if (isHooking) {
            newState = PlayerState.JUMPING; // Could use a specific hook state later
        } else if (currentState == PlayerState.LANDING) {
            // Handle landing animation duration
            if (currentTime - landingStartTime > LANDING_ANIMATION_DURATION) {
                if (velocityX < 5) {
                    newState = PlayerState.IDLE;
                } else if (velocityX > WALK_SPEED * 1.5) {
                    newState = PlayerState.RUNNING;
                } else {
                    newState = PlayerState.WALKING;
                }
                movementContext = MovementContext.NORMAL;
            } else {
                newState = PlayerState.LANDING; // Keep landing state
            }
        } else {
            // Normal state determination
            if (isOnGround()) {
                if (velocityX < 5) {
                    newState = PlayerState.IDLE;
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
                if (velocityY < 0) {
                    newState = PlayerState.JUMPING;
                } else {
                    newState = PlayerState.FALLING;
                }
            }
        }
        
        // Update state if changed
        if (newState != currentState) {
            currentState = newState;
            stateChangeTime = currentTime;
            
            // Update sprite based on state and context
            updateSpriteForState();
        }
    }
    
    private void updateSpriteForState() {
        Sprite newSprite = null;
        
        // Choose sprite based on state and context
        switch (currentState) {
            case RUNNING:
                if (isTurning) {
                    newSprite = contextualSprites.get("turn_left"); // Will be flipped as needed
                } else {
                    newSprite = stateSprites.get(PlayerState.RUNNING);
                }
                break;
                
            case JUMPING:
                if (movementContext == MovementContext.RUNNING) {
                    newSprite = contextualSprites.get("jump_running");
                }
                if (newSprite == null) {
                    newSprite = stateSprites.get(PlayerState.JUMPING);
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
                    default:
                        newSprite = contextualSprites.get("landing_normal");
                        break;
                }
                break;
                
            case DASHING:
                if (isDashing) {
                    newSprite = contextualSprites.get("dash_start");
                } else {
                    newSprite = contextualSprites.get("dash_end");
                }
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
            if (currentSprite != null) {
                currentSprite.reset();
            }
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible || currentSprite == null) return;
        
        // SPRITE OFFSET ADJUSTMENT - Modify these values
        int SPRITE_OFFSET_X = 0;  // Adjust this for horizontal offset
        int SPRITE_OFFSET_Y = 50 ;  // Adjust this for vertical offset
        
        // Calculate sprite rendering position to align with hitbox
        // The sprite is larger than the hitbox, so we need to offset it properly
        
        // Sprite width/height includes scaling
        int renderedWidth = currentSprite.getSize().width;
        int renderedHeight = currentSprite.getSize().height;
        
        // Calculate sprite position to align the bottom with the hitbox bottom
        int spriteX = (int)(position.getX() - renderedWidth / 2.0) + SPRITE_OFFSET_X;
        int spriteY = (int)(position.getY() - HITBOX_HEIGHT / 2.0 - (renderedHeight - HITBOX_HEIGHT)) + SPRITE_OFFSET_Y;
        
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
        
        // Draw health and mana bars (also offset if needed)
        drawHealthManaBar(g);
        
        // Draw buff effects
        renderBuffEffects(g);
        
        // Draw hook line if hooking
        if (isHooking && hookTarget != null) {
            g.setColor(Color.CYAN);
            g.drawLine((int)position.getX(), (int)position.getY(), 
                      (int)hookTarget.getX(), (int)hookTarget.getY());
        }
    }
    
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Reset special states on collision
        if (isDashing && other.getMass() > 0) {
            endDash();
        }
        
        // Check for ground collision
        if (collision.getNormal().getY() < -0.5 && other.getMass() <= 0) {
            setOnGround(true);
        }
    }
    
    // [Rest of the methods remain the same as before...]
    
    private void applyMovement(float dt) {
        Vector2D velocity = getVelocity();
        double currentSpeed = velocity.getX();
        double targetSpeed = targetVelocity.getX();
        
        // Get movement modifiers
        double speedMultiplier = getBuffMultiplier(BuffType.SPEED);
        targetSpeed *= speedMultiplier;
        
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
    
    private void performHook() {
        double hookX = isFacingRight ? HOOK_RANGE : -HOOK_RANGE;
        double hookY = -HOOK_RANGE * 0.5;
        
        Vector2D hookDirection = new Vector2D(hookX, hookY);
        hookTarget = position.plus(hookDirection);
        
        // Check if we have a valid hook target (simplified)
        if (position.distance(hookTarget) <= HOOK_RANGE) {
            isHooking = true;
            affectedByGravity = false;
            currentState = PlayerState.JUMPING; // Use jumping state for hook
            stateChangeTime = System.currentTimeMillis();
            
            // Apply immediate velocity toward hook point
            Vector2D hookVelocity = hookTarget.minus(position).normalized().times(HOOK_SPEED);
            setVelocity(hookVelocity);
        }
    }
    
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
    
    private void castFireball() {
        isCasting = true;
        currentState = PlayerState.IDLE; // No cast animation yet
        
        // Consume mana
        mana -= 5;
        if (mana < 0) mana = 0;
        
        System.out.println("Casting Fireball!");
    }
    
    private void castHeal() {
        isCasting = true;
        currentState = PlayerState.IDLE; // No cast animation yet
        
        // Consume mana
        mana -= 10;
        if (mana < 0) mana = 0;
        
        // Heal player
        health += 20;
        if (health > MAX_HEALTH) health = MAX_HEALTH;
        
        System.out.println("Casting Heal! Health: " + health);
    }
    
    private void updateSprite(long deltaTime) {
        if (currentSprite != null) {
            currentSprite.update(deltaTime);
            
            // Handle non-looping sprites
            if (currentSprite instanceof LoopingSprite) {
                LoopingSprite loopingSprite = (LoopingSprite) currentSprite;
                if (!loopingSprite.isLooping() && loopingSprite.hasCompleted()) {
                    // Transition to appropriate state after animation completes
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
            }
        }
    }
    
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
    
    // Buff system methods
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
    
    private double getBuffMultiplier(BuffType type) {
        BuffEffect buff = activeBuffs.get(type);
        return buff != null ? buff.multiplier : 1.0;
    }
    
    private boolean hasActiveBuff(BuffType type) {
        return activeBuffs.containsKey(type);
    }
    
    // Getters
    public PlayerState getCurrentState() { return currentState; }
    public boolean isOnGround() { return onGround; }
    public boolean isWalking() { return currentState == PlayerState.WALKING; }
    public boolean isRunning() { return currentState == PlayerState.RUNNING; }
    public boolean isDashing() { return isDashing; }
    public boolean isHooking() { return isHooking; }
    public boolean isFacingRight() { return isFacingRight; }
    public Sprite getCurrentSprite() { return currentSprite; }
    public Map<BuffType, BuffEffect> getActiveBuffs() { return new HashMap<>(activeBuffs); }
    public int getHealth() { return health; }
    public int getMana() { return mana; }
    public boolean isAttacking() { return isAttacking; }
    public boolean isCasting() { return isCasting; }
    
    /**
     * Movement context enum for animation decisions
     */
    private enum MovementContext {
        NORMAL,
        RUNNING,
        DASHING
    }
}