package game.entity.component;

import game.entity.PlayerEntity;
import game.entity.PlayerState;
import game.entity.component.Component.ComponentType;

public class PlayerStateComponent implements Component {
    private final PlayerEntity player;
    
    // Timing parameters
    private static final long LANDING_ANIMATION_DURATION = 300;
    
    // States
    private PlayerState previousState = PlayerState.IDLE;
    private MovementContext previousMovementContext = MovementContext.NORMAL;
    private boolean wasRunning = false;
    private boolean wasDashing = false;
    
    // Times
    private long lastGroundTime = 0;
    private long landingStartTime = 0;
    private long stateChangeTime = 0;
    
    public PlayerStateComponent(PlayerEntity player) {
        this.player = player;
    }
    
    @Override
    public void update(long deltaTime) {
        long currentTime = System.currentTimeMillis();
        
        // Store previous state for context
        previousState = player.getCurrentState();
        previousMovementContext = player.getMovementContext();
        wasRunning = (player.getCurrentState() == PlayerState.RUNNING);
        wasDashing = player.isDashing();
        
        // Track ground state
        boolean wasOnGround = player.isOnGround();
        
        // Check for landing with context
        if (!wasOnGround && player.isOnGround()) {
            onLand(currentTime);
        }
        
        // Update coyote time
        if (player.isOnGround()) {
            lastGroundTime = currentTime;
            player.setJumpCount(0);
        }
        
        // Update player state
        updatePlayerState(currentTime);
    }
    
    private void onLand(long currentTime) {
        player.setJumping(false);
        player.setFalling(false);
        player.setJumpCount(0);
        player.setAffectedByGravity(true);
        landingStartTime = currentTime;
        
        // Determine landing type based on previous context
        player.setCurrentState(PlayerState.LANDING);
        
        // Preserve context for landing to determine correct animation
        // We keep the current context (NORMAL, RUNNING, DASHING) when landing
        if (player.isCrouching()) {
            player.setMovementContext(MovementContext.CROUCHING);
        } else if (player.isSliding()) {
            // Continue sliding if already sliding
            player.setMovementContext(MovementContext.SLIDING);
        }
        // else keep the existing context for proper landing animation
        
        player.setStateChangeTime(currentTime);
        
        // End special states as needed
        if (player.isHooking()) {
            player.setHooking(false);
            player.setHookTarget(null);
        }
    }
    
    private void updatePlayerState(long currentTime) {
        PlayerState newState = player.getCurrentState();
        double velocityX = Math.abs(player.getVelocity().getX());
        double velocityY = player.getVelocity().getY();
        
        // Handle special states first - these take priority
        if (player.isSliding()) {
            newState = PlayerState.SLIDING;
        } else if (player.isDashing()) {
            if (!player.isOnGround() && velocityY > 0) {
                // Falling while dashing
                newState = PlayerState.FALLING;
                player.setMovementContext(MovementContext.DASHING);
            } else {
                newState = PlayerState.DASHING;
            }
        } else if (player.isTurning()) {
            newState = PlayerState.RUNNING;
        } else if (player.isAttacking() && currentTime - player.getActionStartTime() < 300) {
            newState = PlayerState.ATTACKING;
        } else if (player.isHooking()) {
            newState = PlayerState.HOOKING;
        } else if (player.getCurrentState() == PlayerState.LANDING) {
            // Keep landing state until animation completes
            if (currentTime - landingStartTime > LANDING_ANIMATION_DURATION) {
                if (player.isCrouching()) {
                    if (velocityX < 5) {
                        newState = PlayerState.IDLE;
                    } else {
                        newState = PlayerState.WALKING;
                    }
                    player.setMovementContext(MovementContext.CROUCHING);
                } else if (velocityX < 5) {
                    newState = PlayerState.IDLE;
                    player.setMovementContext(MovementContext.NORMAL);
                } else if (velocityX > 450) { // WALK_SPEED * 1.5
                    newState = PlayerState.RUNNING;
                    player.setMovementContext(MovementContext.RUNNING);
                } else {
                    newState = PlayerState.WALKING;
                    player.setMovementContext(MovementContext.NORMAL);
                }
            } else {
                newState = PlayerState.LANDING;
                // Keep existing context for landing animation
            }
        } else {
            // Normal state determination
            if (player.isOnGround()) {
                if (player.isCrouching()) {
                    // Maintain current state but with crouching context
                    if (velocityX < 5) {
                        newState = PlayerState.IDLE;
                    } else {
                        newState = PlayerState.WALKING;
                    }
                    player.setMovementContext(MovementContext.CROUCHING);
                } else if (velocityX < 5) {
                    newState = PlayerState.IDLE;
                    player.setMovementContext(MovementContext.NORMAL);
                } else {
                    // Check if running
                    if (velocityX > 450) { // WALK_SPEED * 1.5
                        newState = PlayerState.RUNNING;
                        player.setMovementContext(MovementContext.RUNNING);
                    } else {
                        newState = PlayerState.WALKING;
                        player.setMovementContext(MovementContext.NORMAL);
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
        if (newState != player.getCurrentState()) {
            previousState = player.getCurrentState();
            player.setCurrentState(newState);
            player.setStateChangeTime(currentTime);
            
            // Update sprite based on state and context
            player.updateSpriteForState();
        } else if (player.getMovementContext() != previousMovementContext) {
            // If context changed but state didn't, still need to update sprite
            previousMovementContext = player.getMovementContext();
            player.updateSpriteForState();
        }
    }
    
    public long getLastGroundTime() {
        return lastGroundTime;
    }
    
    public enum MovementContext {
        NORMAL,
        RUNNING,
        DASHING,
        CROUCHING,
        SLIDING
    }

	@Override
	public ComponentType getType() {
		return ComponentType.STATE;
	}
}