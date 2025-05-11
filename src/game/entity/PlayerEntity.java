package game.entity;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
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
 * Improved PlayerEntity with better movement and state management
 */
public class PlayerEntity extends AbstractEntity {
    // Character dimensions (1.6m tall)
    private static final int PLAYER_HEIGHT = 160; // 1.6m
    private static final int PLAYER_WIDTH = 80;   // 0.8m
    
    // Movement parameters (adjusted for better control)
    private static final double WALK_SPEED = 300.0;
    private static final double RUN_SPEED = 500.0;
    private static final double WALK_ACCELERATION = 2000.0;
    private static final double RUN_ACCELERATION = 3000.0;
    private static final double AIR_CONTROL = 0.6;
    private static final double GROUND_FRICTION = 0.95;
    
    // Jump parameters
    private static final double JUMP_FORCE = -650.0;
    private static final double DOUBLE_JUMP_FORCE = -550.0;
    
    // Advanced movement parameters
    private static final double DASH_SPEED = 1000.0;
    private static final double TELEPORT_DISTANCE = 150.0;
    private static final double HOOK_SPEED = 800.0;
    private static final double HOOK_RANGE = 300.0;
    private static final double CLOSE_TELEPORT_DISTANCE = 80.0;
    
    // Timing parameters
    private static final long DASH_DURATION = 200;
    private static final long DASH_COOLDOWN = 600;
    private static final long TELEPORT_COOLDOWN = 1000;
    private static final long HOOK_COOLDOWN = 1500;
    private static final long COYOTE_TIME = 150;
    private static final long JUMP_BUFFER_TIME = 200;
    
    // Input reference
    private final KeyboardInput input;
    
    // Enhanced state tracking
    private PlayerState currentState = PlayerState.IDLE;
    private boolean isFacingRight = true;
    private boolean wasTryingToMove = false;
    private boolean isJumping = false;
    private boolean isFalling = false;
    private boolean isDashing = false;
    private boolean isHooking = false;
    private int jumpCount = 0;
    private int maxJumps = 2;
    
    // Timing variables
    private long lastDashTime = 0;
    private long lastTeleportTime = 0;
    private long lastHookTime = 0;
    private long lastGroundTime = 0;
    private long lastJumpPressTime = 0;
    private long stateChangeTime = 0;
    private long dashStartTime = 0;
    
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
    
    // Buff/Debuff system
    private Map<BuffType, BuffEffect> activeBuffs = new HashMap<>();
    
    public PlayerEntity(double x, double y, KeyboardInput input) {
        super(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
        this.input = input;
        
        // Initialize physics
        this.mass = 1.0f;
        this.affectedByGravity = true;
        this.collisionShape = new AABB(position, PLAYER_WIDTH, PLAYER_HEIGHT);
        this.friction = 0.8f;  // High friction for good control
        this.restitution = 0.0f;  // No bounce
        
        // Initialize sprite management
        this.spriteManager = new SpriteSheetManager();
        loadSprites();
    }
    
    private void loadSprites() {
        spriteManager.createPlayerSprites();
        
        // Map sprites to states
        stateSprites.put(PlayerState.IDLE, spriteManager.getSprite("player_idle"));
        stateSprites.put(PlayerState.WALKING, spriteManager.getSprite("player_walk"));
        stateSprites.put(PlayerState.RUNNING, spriteManager.getSprite("player_run"));
        stateSprites.put(PlayerState.JUMPING, spriteManager.getSprite("player_jump"));
        stateSprites.put(PlayerState.FALLING, spriteManager.getSprite("player_fall"));
        stateSprites.put(PlayerState.DASHING, spriteManager.getSprite("player_dash"));
        stateSprites.put(PlayerState.HOOKING, spriteManager.getSprite("player_dash"));
        stateSprites.put(PlayerState.LANDING, spriteManager.getSprite("player_land_quick"));
        stateSprites.put(PlayerState.TELEPORTING, spriteManager.getSprite("player_dash"));
        
        // Set initial sprite
        currentSprite = stateSprites.get(PlayerState.IDLE);
    }
    
    @Override
    public void update(long deltaTime) {
        long currentTime = System.currentTimeMillis();
        float dt = deltaTime / 1000.0f;
        
        // Update buff/debuff system
        updateBuffs(deltaTime);
        
        // Track ground state
        boolean wasOnGround = isOnGround();
        
        // Check for landing
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
        
        // Update state based on movement
        updatePlayerState();
        
        // Update sprite animation
        updateSprite(deltaTime);
    }
    
    private void handleInput(long deltaTime) {
        long currentTime = System.currentTimeMillis();
        
        // Store if we're trying to move this frame
        wasTryingToMove = false;
        
        // Movement input
        if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            isFacingRight = false;
            wasTryingToMove = true;
            if (!isDashing && !isHooking) {
                if (input.isKeyPressed(KeyEvent.VK_X)) {
                    // Running
                    targetVelocity.setX(-RUN_SPEED);
                } else {
                    // Walking
                    targetVelocity.setX(-WALK_SPEED);
                }
            }
        } else if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            isFacingRight = true;
            wasTryingToMove = true;
            if (!isDashing && !isHooking) {
                if (input.isKeyPressed(KeyEvent.VK_X)) {
                    // Running
                    targetVelocity.setX(RUN_SPEED);
                } else {
                    // Walking
                    targetVelocity.setX(WALK_SPEED);
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
                jump(JUMP_FORCE);
                lastJumpPressTime = 0;
            } else if (jumpCount < maxJumps && hasActiveBuff(BuffType.DOUBLE_JUMP)) {
                // Double jump (if available)
                jump(DOUBLE_JUMP_FORCE);
                lastJumpPressTime = 0;
            }
        }
        
        // Advanced movement abilities
        handleAdvancedMovement(currentTime);
    }
    
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
    
    private void onLand() {
        if (Math.abs(velocity.getY()) > 400) {
            currentState = PlayerState.LANDING;
            stateChangeTime = System.currentTimeMillis();
        }
        
        isJumping = false;
        isFalling = false;
        jumpCount = 0;
        affectedByGravity = true;
        
        // Stop hook if landing
        if (isHooking) {
            isHooking = false;
            hookTarget = null;
        }
    }
    
    private void handleAdvancedMovement(long currentTime) {
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
            isDashing = false;
            affectedByGravity = true;
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
        stateChangeTime = System.currentTimeMillis();
        addBuffEffect(BuffType.DASH_IMMUNITY, DASH_DURATION);
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
        currentState = PlayerState.TELEPORTING;
        stateChangeTime = System.currentTimeMillis();
        addBuffEffect(BuffType.TELEPORT_IMMUNITY, 500);
    }
    
    private void performHook() {
        double hookX = isFacingRight ? HOOK_RANGE : -HOOK_RANGE;
        double hookY = -HOOK_RANGE * 0.5;
        
        Vector2D hookDirection = new Vector2D(hookX, hookY);
        hookTarget = position.plus(hookDirection);
        
        // Check if we have a valid hook target (simplified)
        // In a real game, you'd raycast to find hookable surfaces
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
    
    private void updatePlayerState() {
        PlayerState newState = currentState;
        long currentTime = System.currentTimeMillis();
        double velocityX = Math.abs(getVelocity().getX());
        double velocityY = getVelocity().getY();
        
        // Override states for special abilities
        if (isDashing) {
            newState = PlayerState.DASHING;
        } else if (isHooking) {
            newState = PlayerState.HOOKING;
            // Stop hooking if we've reached the target
            if (hookTarget != null && position.distance(hookTarget) < 30) {
                isHooking = false;
                hookTarget = null;
                affectedByGravity = true;
            }
        } else if (currentState == PlayerState.TELEPORTING) {
            if (currentTime - stateChangeTime > 200) {
                newState = PlayerState.FALLING;
            }
        } else {
            // Normal state determination
            if (isOnGround()) {
                if (velocityX < 5) {
                    newState = PlayerState.IDLE;
                } else {
                    newState = input.isKeyPressed(KeyEvent.VK_X) ? PlayerState.RUNNING : PlayerState.WALKING;
                }
            } else {
                if (velocityY < 0) {
                    newState = PlayerState.JUMPING;
                } else {
                    newState = PlayerState.FALLING;
                }
            }
        }
        
        // Handle landing animation
        if (newState == PlayerState.LANDING && currentTime - stateChangeTime > 300) {
            newState = PlayerState.IDLE;
        }
        
        // Update state if changed
        if (newState != currentState) {
            currentState = newState;
            stateChangeTime = currentTime;
            
            // Update sprite
            Sprite newSprite = stateSprites.get(currentState);
            if (newSprite != null && newSprite != currentSprite) {
                currentSprite = newSprite;
                if (currentSprite != null) {
                    currentSprite.reset();
                }
            }
        }
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
                        currentSprite = stateSprites.get(PlayerState.IDLE);
                        if (currentSprite != null) {
                            currentSprite.reset();
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible || currentSprite == null) return;
        
        // Draw sprite
        int spriteX = (int)(position.getX() - currentSprite.getSize().width / 2);
        int spriteY = (int)(position.getY() - currentSprite.getSize().height / 2 + 10);
        
        // Flip sprite based on facing direction
        if (isFacingRight) {
            g.drawImage(currentSprite.getFrame(), spriteX, spriteY, 
                       currentSprite.getSize().width, 
                       (int)(currentSprite.getSize().height * 1.28), null);
        } else {
            g.drawImage(currentSprite.getFrame(), 
                       spriteX + currentSprite.getSize().width, spriteY,
                       -currentSprite.getSize().width, 
                       (int)(currentSprite.getSize().height * 1.28), null);
        }
        
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
        if (isDashing) {
            isDashing = false;
            affectedByGravity = true;
        }
        
        // Check for ground collision
        if (collision.getNormal().getY() < -0.5) {
            setOnGround(true);
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
    
    private void renderBuffEffects(Graphics2D g) {
        int offsetY = -50;
        
        // Draw buff indicators
        if (hasActiveBuff(BuffType.SPEED)) {
            g.setColor(Color.GREEN);
            g.fillOval((int)position.getX() - 5, (int)position.getY() + offsetY, 10, 10);
        }
        
        if (hasActiveBuff(BuffType.DASH_IMMUNITY) || hasActiveBuff(BuffType.TELEPORT_IMMUNITY)) {
            g.setColor(Color.YELLOW);
            g.drawOval((int)position.getX() - 30, (int)position.getY() - 30, 60, 60);
        }
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
}