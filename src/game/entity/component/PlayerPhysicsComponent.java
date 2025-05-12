package game.entity.component;

import game.Vector2D;
import game.entity.BuffType;
import game.entity.PlayerEntity;
import game.entity.PlayerState;
import game.entity.component.Component.ComponentType;

public class PlayerPhysicsComponent implements Component {
    private final PlayerEntity player;
    
    // Movement parameters
    private static final double WALK_ACCELERATION = 2000.0;
    private static final double RUN_ACCELERATION = 3000.0;
    private static final double AIR_CONTROL = 0.6;
    private static final double GROUND_FRICTION = 0.95;
    private static final double SLIDE_FRICTION = 0.992;
    private static final double SLIDE_SPEED_MULTIPLIER = 1.2;
    
    // Timing parameters
    private static final long DASH_DURATION = 250;
    private static final long TURN_ANIMATION_DURATION = 150;
    private static final long SLIDE_DURATION = 600;
    
    public PlayerPhysicsComponent(PlayerEntity player) {
        this.player = player;
    }
    
    @Override
    public void update(long deltaTime) {
        float dt = deltaTime / 1000.0f;
        long currentTime = System.currentTimeMillis();
        
        // Update states based on timing
        if (player.isDashing() && currentTime - player.getDashStartTime() >= DASH_DURATION) {
            endDash();
        }
        
        if (player.isTurning() && currentTime - player.getTurnStartTime() >= TURN_ANIMATION_DURATION) {
            player.setTurning(false);
        }
        
        // Apply movement
        if (!player.isDashing() && !player.isHooking() && !player.isSliding()) {
            applyMovement(dt);
        } else if (player.isSliding()) {
            applySlideMovement(dt, currentTime);
        }
        
        // Limit maximum speeds
        Vector2D vel = player.getVelocity();
        if (vel.getX() > 1000) vel.setX(1000);
        if (vel.getX() < -1000) vel.setX(-1000);
        if (vel.getY() > 1500) vel.setY(1500);
        player.setVelocity(vel);
    }
    
    private void applyMovement(float dt) {
        Vector2D velocity = player.getVelocity();
        double currentSpeed = velocity.getX();
        double targetSpeed = player.getTargetVelocityX();
        
        // Get movement modifiers
        double speedMultiplier = player.getBuffMultiplier(BuffType.SPEED);
        targetSpeed *= speedMultiplier;
        
        // Reduce speed while crouching
        if (player.isCrouching() && player.isOnGround()) {
            targetSpeed *= 0.5; // Half speed while crouching
        }
        
        // Calculate acceleration based on whether we're on ground
        double acceleration = player.isOnGround() ? 
            (Math.abs(targetSpeed) > 300 ? RUN_ACCELERATION : WALK_ACCELERATION) :
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
        if (player.isOnGround() && !player.wasTryingToMove()) {
            velocity.setX(velocity.getX() * GROUND_FRICTION);
            // Stop completely if very slow
            if (Math.abs(velocity.getX()) < 5) {
                velocity.setX(0);
            }
        }
        
        player.setVelocity(velocity);
    }
    
    private void applySlideMovement(float dt, long currentTime) {
        if (!player.isSliding()) return;
        
        Vector2D velocity = player.getVelocity();
        
        // Apply sliding friction (higher than normal friction)
        velocity.setX(velocity.getX() * SLIDE_FRICTION);
        
        // Check if sliding is done based on duration
        if (currentTime - player.getSlideStartTime() >= SLIDE_DURATION) {
            player.exitSlidingState();
        }
        // Check if sliding speed is too low
        else if (Math.abs(velocity.getX()) < 150) {  // WALK_SPEED * 0.5
            player.exitSlidingState();
        }
        
        player.setVelocity(velocity);
    }
    
    private void endDash() {
        player.setDashing(false);
        player.setAffectedByGravity(true);
        
        // Preserve dashing context for landing animations
        if (player.getMovementContext() == PlayerStateComponent.MovementContext.DASHING) {
            if (player.isOnGround()) {
                // If we're on ground, determine next state
                if (Math.abs(player.getVelocity().getX()) > 450) {  // WALK_SPEED * 1.5
                    player.setCurrentState(PlayerState.RUNNING);
                    player.setMovementContext(PlayerStateComponent.MovementContext.RUNNING);
                } else if (Math.abs(player.getVelocity().getX()) > 5) {
                    player.setCurrentState(PlayerState.WALKING);
                    player.setMovementContext(PlayerStateComponent.MovementContext.NORMAL);
                } else {
                    player.setCurrentState(PlayerState.IDLE);
                    player.setMovementContext(PlayerStateComponent.MovementContext.NORMAL);
                }
            } else {
                // If in air, keep dash context but update state
                player.setCurrentState(player.getVelocity().getY() < 0 ? 
                    PlayerState.JUMPING : PlayerState.FALLING);
                // Keep DASHING context
            }
        }
    }

	@Override
	public ComponentType getType() {
		// TODO Auto-generated method stub
		return ComponentType.PHYSICS;
	}
}