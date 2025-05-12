package game.entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;

import game.Vector2D;
import game.entity.component.BuffComponent;
import game.entity.component.PlayerAnimationComponent;
import game.entity.component.PlayerInputComponent;
import game.entity.component.PlayerPhysicsComponent;
import game.entity.component.PlayerStateComponent;
import game.entity.component.PlayerStateComponent.MovementContext;
import game.input.KeyboardInput;
import game.physics.AABB;
import game.physics.Collision;
import game.physics.PhysicsObject;
import game.sprites.Sprite;

public class PlayerEntity extends AbstractEntity {
    // Dimensions
    private static final int HITBOX_WIDTH = 40;
    private static final int HITBOX_HEIGHT = 140;
    
    // Components
    private final PlayerInputComponent inputComponent;
    private final PlayerPhysicsComponent physicsComponent;
    private final PlayerStateComponent stateComponent;
    private final PlayerAnimationComponent animationComponent;
    private final BuffComponent buffComponent;
    
    // State (minimized but accessible to components)
    private PlayerState currentState = PlayerState.IDLE;
    private MovementContext movementContext = MovementContext.NORMAL;
    private boolean isFacingRight = true;
    private boolean isCrouching = false;
    private boolean isDashing = false;
    private boolean isHooking = false;
    private boolean isTurning = false;
    private boolean isSliding = false;
    private boolean isJumping = false;
    private boolean isFalling = false;
    private boolean isAttacking = false;
    private boolean isCasting = false;
    private boolean wasTryingToMove = false;
    
    // Movement data
    private Vector2D targetVelocity = new Vector2D(0, 0);
    private Vector2D dashDirection = new Vector2D(0, 0);
    private Vector2D hookTarget = null;
    
    // Timing data
    private long stateChangeTime = 0;
    private long actionStartTime = 0;
    private long dashStartTime = 0;
    private long turnStartTime = 0;
    private long slideStartTime = 0;
    private long slideEndTime = 0;
    private long lastGroundTime = 0;
    private long lastSpellTime = 0;
    
    // Jump variables
    private int jumpCount = 0;
    private int maxJumps = 2;
    
    // Combat data
    private static final int MAX_HEALTH = 100;
    private static final int MAX_MANA = 50;
    private static final long SPELL_COOLDOWN = 800;
    private int health = MAX_HEALTH;
    private int mana = MAX_MANA;
    
    public PlayerEntity(double x, double y, KeyboardInput input) {
        super(x, y, HITBOX_WIDTH, HITBOX_HEIGHT);
        
        // Initialize components
        this.inputComponent = new PlayerInputComponent(this, input);
        this.physicsComponent = new PlayerPhysicsComponent(this);
        this.stateComponent = new PlayerStateComponent(this);
        this.animationComponent = new PlayerAnimationComponent(this);
        this.buffComponent = new BuffComponent();
        
        // Initialize physics properties
        this.mass = 1.0f;
        this.affectedByGravity = true;
        this.collisionShape = new AABB(position, HITBOX_WIDTH, HITBOX_HEIGHT);
        this.friction = 0.9f;
        this.restitution = 0.0f;
    }
    
    @Override
    public void update(long deltaTime) {
        // Update all components
        buffComponent.update(deltaTime);
        inputComponent.update(deltaTime);
        physicsComponent.update(deltaTime);
        stateComponent.update(deltaTime);
        animationComponent.update(deltaTime);
        
        // Update base entity physics
        super.update(deltaTime);
    }
    
    @Override
    public void render(Graphics2D g) {
        animationComponent.render(g);
    }
    
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Basic collision response
        
        // Reset special states on collision
        if (isDashing && other.getMass() > 0) {
            isDashing = false;
            affectedByGravity = true;
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
    
    // Movement methods
    
    public void jump(double force) {
        Vector2D velocity = getVelocity();
        double jumpMultiplier = buffComponent.getBuffMultiplier(BuffType.JUMP_HEIGHT);
        velocity.setY(force * jumpMultiplier);
        setVelocity(velocity);
        
        isJumping = true;
        jumpCount++;
        currentState = PlayerState.JUMPING;
        stateChangeTime = System.currentTimeMillis();
    }
    
    public void enterSlidingState() {
        isSliding = true;
        slideStartTime = System.currentTimeMillis();
        
        // Apply initial slide momentum
        double slideSpeed = velocity.getX() * 1.2; // SLIDE_SPEED_MULTIPLIER
        if (Math.abs(slideSpeed) < 600) { // RUN_SPEED
            slideSpeed = isFacingRight ? 600 : -600;
        }
        velocity.setX(slideSpeed);
        
        // Change state
        currentState = PlayerState.SLIDING;
        movementContext = MovementContext.SLIDING;
        stateChangeTime = System.currentTimeMillis();
        
        // Update sprite
        updateSpriteForState();
    }
    
    public void exitSlidingState() {
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
            } else if (Math.abs(velocity.getX()) > 450) {
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
    
    public void startTurnAnimation(boolean turningRight) {
        isTurning = true;
        turnStartTime = System.currentTimeMillis();
        currentState = PlayerState.RUNNING; // Use running state with turn sprite
        updateSpriteForState();
    }
    
    // Combat methods
    
    public void performBasicAttack() {
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
    
    public void performHeavyAttack() {
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
    
    public void castFireball() {
        isCasting = true;
        currentState = PlayerState.CASTING;
        lastSpellTime = System.currentTimeMillis();
        
        // Consume mana
        mana -= 5;
        if (mana < 0) mana = 0;
        
        System.out.println("Casting Fireball!");
    }
    
    public void castHeal() {
        isCasting = true;
        currentState = PlayerState.CASTING;
        lastSpellTime = System.currentTimeMillis();
        
        // Consume mana
        mana -= 10;
        if (mana < 0) mana = 0;
        
        // Heal player
        health += 20;
        if (health > MAX_HEALTH) health = MAX_HEALTH;
        
        System.out.println("Casting Heal! Health: " + health);
    }
    
    // Integration with other components
    
    public void updateSpriteForState() {
        animationComponent.updateSpriteForState();
    }
    
    public void toggleDebugRender() {
        animationComponent.toggleDebugRender();
    }
    
    public void addBuffEffect(BuffType type, long duration) {
        buffComponent.addBuffEffect(type, duration);
    }
    
    public boolean hasActiveBuff(BuffType type) {
        return buffComponent.hasActiveBuff(type);
    }
    
    public double getBuffMultiplier(BuffType type) {
        return buffComponent.getBuffMultiplier(type);
    }
    
    public Map<BuffType, BuffEffect> getActiveBuffs() {
        return buffComponent.getActiveBuffs();
    }
    
    // Getters and setters
    
    // State getters/setters
    public PlayerState getCurrentState() { return currentState; }
    public void setCurrentState(PlayerState state) { this.currentState = state; }
    
    public MovementContext getMovementContext() { return movementContext; }
    public void setMovementContext(MovementContext context) { this.movementContext = context; }
    
    public boolean isFacingRight() { return isFacingRight; }
    public void setFacingRight(boolean facingRight) { this.isFacingRight = facingRight; }
    
    public boolean isCrouching() { return isCrouching; }
    public void setCrouching(boolean crouching) { this.isCrouching = crouching; }
    
    public boolean isDashing() { return isDashing; }
    public void setDashing(boolean dashing) { this.isDashing = dashing; }
    
    public boolean isHooking() { return isHooking; }
    public void setHooking(boolean hooking) { this.isHooking = hooking; }
    
    public boolean isTurning() { return isTurning; }
    public void setTurning(boolean turning) { this.isTurning = turning; }
    
    public boolean isSliding() { return isSliding; }
    public void setSliding(boolean sliding) { this.isSliding = sliding; }
    
    public boolean isJumping() { return isJumping; }
    public void setJumping(boolean jumping) { this.isJumping = jumping; }
    
    public boolean isFalling() { return isFalling; }
    public void setFalling(boolean falling) { this.isFalling = falling; }
    
    public boolean isAttacking() { return isAttacking; }
    public void setAttacking(boolean attacking) { this.isAttacking = attacking; }
    
    public boolean isCasting() { return isCasting; }
    public void setCasting(boolean casting) { this.isCasting = casting; }
    
    public boolean wasTryingToMove() { return wasTryingToMove; }
    public void setWasTryingToMove(boolean trying) { this.wasTryingToMove = trying; }
    
    // Movement data getters/setters
    public double getTargetVelocityX() { return targetVelocity.getX(); }
    public void setTargetVelocityX(double x) { this.targetVelocity.setX(x); }
    
    public Vector2D getDashDirection() { return dashDirection; }
    public void setDashDirection(Vector2D direction) { this.dashDirection.set(direction); }
    
    public Vector2D getHookTarget() { return hookTarget; }
    public void setHookTarget(Vector2D target) { this.hookTarget = target; }
    
    // Timing getters/setters
    public long getStateChangeTime() { return stateChangeTime; }
    public void setStateChangeTime(long time) { this.stateChangeTime = time; }
    
    public long getActionStartTime() { return actionStartTime; }
    public void setActionStartTime(long time) { this.actionStartTime = time; }
    
    public long getDashStartTime() { return dashStartTime; }
    public void setDashStartTime(long time) { this.dashStartTime = time; }
    
    public long getTurnStartTime() { return turnStartTime; }
    public void setTurnStartTime(long time) { this.turnStartTime = time; }
    
    public long getSlideStartTime() { return slideStartTime; }
    public void setSlideStartTime(long time) { this.slideStartTime = time; }
    
    public long getSlideEndTime() { return slideEndTime; }
    public void setSlideEndTime(long time) { this.slideEndTime = time; }
    
    public long getLastGroundTime() { return lastGroundTime; }
    public void setLastGroundTime(long time) { this.lastGroundTime = time; }
    
    public long getLastSpellTime() { return lastSpellTime; }
    public void setLastSpellTime(long time) { this.lastSpellTime = time; }
    
    // Jump variables getters/setters
    public int getJumpCount() { return jumpCount; }
    public void setJumpCount(int count) { this.jumpCount = count; }
    
    public int getMaxJumps() { return maxJumps; }
    public void setMaxJumps(int max) { this.maxJumps = max; }
    
    // Combat data getters/setters
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    
    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }
    
    public int getMaxHealth() { return MAX_HEALTH; }
    public int getMaxMana() { return MAX_MANA; }
    public long getSpellCooldown() { return SPELL_COOLDOWN; }
    
    // Sprite getter
    public Sprite getCurrentSprite() { return animationComponent.getCurrentSprite(); }
}