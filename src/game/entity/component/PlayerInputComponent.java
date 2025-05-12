package game.entity.component;

import java.awt.event.KeyEvent;
import game.Vector2D;
import game.entity.BuffType;
import game.entity.PlayerEntity;
import game.entity.PlayerState;
import game.entity.component.Component.ComponentType;
import game.input.KeyboardInput;

public class PlayerInputComponent implements Component {
    private final PlayerEntity player;
    private final KeyboardInput input;
    
    // Movement parameters
    private static final double WALK_SPEED = 300.0;
    private static final double RUN_SPEED = 600.0;
    private static final double JUMP_FORCE = -750.0;
    private static final double DOUBLE_JUMP_FORCE = -650.0;
    private static final double DASH_SPEED = 1200.0;
    private static final double TELEPORT_DISTANCE = 350.0;
    private static final double CLOSE_TELEPORT_DISTANCE = 80.0;
    private static final double HOOK_RANGE = 300.0;
    private static final double HOOK_SPEED = 800.0;
    
    // Timing parameters
    private static final long DASH_COOLDOWN = 600;
    private static final long DASH_DURATION = 250;
    private static final long TELEPORT_COOLDOWN = 1000;
    private static final long HOOK_COOLDOWN = 1500;
    private static final long ATTACK_COOLDOWN = 300;
    private static final long COYOTE_TIME = 150;
    private static final long JUMP_BUFFER_TIME = 200;
    private static final long SLIDE_COOLDOWN = 800;
    
    // Cooldown tracking
    private long lastDashTime = 0;
    private long lastTeleportTime = 0;
    private long lastHookTime = 0;
    private long lastAttackTime = 0;
    private long lastJumpPressTime = 0;
    private boolean wasTryingToMove = false;
    
    public PlayerInputComponent(PlayerEntity player, KeyboardInput input) {
        this.player = player;
        this.input = input;
    }
    
    @Override
    public void update(long deltaTime) {
        long currentTime = System.currentTimeMillis();
        handleMovementInput(currentTime);
        handleJumpInput(currentTime);
        handleSpecialMovement(currentTime);
        handleCombatInput(currentTime);
    }
    
    private void handleMovementInput(long currentTime) {
        wasTryingToMove = false;
        
        // Detect crouching and sliding
        if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            if (player.isOnGround()) {
                if (!player.isSliding() && (player.getCurrentState() == PlayerState.RUNNING || 
                    Math.abs(player.getVelocity().getX()) > WALK_SPEED * 1.2) && 
                    currentTime - player.getSlideEndTime() > SLIDE_COOLDOWN) {
                    // Start sliding (running + down)
                    player.enterSlidingState();
                } else if (!player.isSliding()) {
                    // Just crouching
                    player.setCrouching(true);
                    player.setMovementContext(PlayerStateComponent.MovementContext.CROUCHING);
                }
            }
        } else {
            // Exit crouching when down is released
            player.setCrouching(false);
            if (player.getMovementContext() == PlayerStateComponent.MovementContext.CROUCHING && !player.isSliding()) {
                player.setMovementContext(PlayerStateComponent.MovementContext.NORMAL);
            }
        }
        
        // Movement input
        if (input.isKeyPressed(KeyEvent.VK_LEFT)) {
            if (player.isFacingRight() && player.isOnGround() && player.getCurrentState() == PlayerState.RUNNING) {
                player.startTurnAnimation(false);
            }
            player.setFacingRight(false);
            wasTryingToMove = true;
            
            if (!player.isDashing() && !player.isHooking() && !player.isTurning() && !player.isSliding()) {
                if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                    // Running
                    player.setTargetVelocityX(-RUN_SPEED);
                    if (!player.isCrouching()) {
                        player.setMovementContext(PlayerStateComponent.MovementContext.RUNNING);
                    }
                } else {
                    // Walking
                    player.setTargetVelocityX(-WALK_SPEED);
                    if (!player.isCrouching()) {
                        player.setMovementContext(PlayerStateComponent.MovementContext.NORMAL);
                    }
                }
            }
        } else if (input.isKeyPressed(KeyEvent.VK_RIGHT)) {
            if (!player.isFacingRight() && player.isOnGround() && player.getCurrentState() == PlayerState.RUNNING) {
                player.startTurnAnimation(true);
            }
            player.setFacingRight(true);
            wasTryingToMove = true;
            
            if (!player.isDashing() && !player.isHooking() && !player.isTurning() && !player.isSliding()) {
                if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                    // Running
                    player.setTargetVelocityX(RUN_SPEED);
                    if (!player.isCrouching()) {
                        player.setMovementContext(PlayerStateComponent.MovementContext.RUNNING);
                    }
                } else {
                    // Walking
                    player.setTargetVelocityX(WALK_SPEED);
                    if (!player.isCrouching()) {
                        player.setMovementContext(PlayerStateComponent.MovementContext.NORMAL);
                    }
                }
            }
        } else {
            // No input - set target to 0
            if (!player.isDashing() && !player.isHooking() && !player.isSliding()) {
                player.setTargetVelocityX(0);
            }
        }
        
        player.setWasTryingToMove(wasTryingToMove);
    }
    
    private void handleJumpInput(long currentTime) {
        // Jump input with buffering
        if (input.isKeyJustPressed(KeyEvent.VK_SPACE)) {
            lastJumpPressTime = currentTime;
        }
        
        if (lastJumpPressTime != 0 && currentTime - lastJumpPressTime <= JUMP_BUFFER_TIME) {
            if (player.isOnGround() || (currentTime - player.getLastGroundTime() <= COYOTE_TIME)) {
                // First jump
                if (input.isKeyPressed(KeyEvent.VK_SHIFT) || player.getCurrentState() == PlayerState.RUNNING) {
                    player.setMovementContext(PlayerStateComponent.MovementContext.RUNNING);
                }
                player.jump(JUMP_FORCE);
                lastJumpPressTime = 0;
            } else if (player.getJumpCount() < player.getMaxJumps() && player.hasActiveBuff(BuffType.DOUBLE_JUMP)) {
                // Double jump
                player.jump(DOUBLE_JUMP_FORCE);
                lastJumpPressTime = 0;
            }
        }
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
    }
    
    private void handleCombatInput(long currentTime) {
        // Basic attack (using X)
        if (input.isKeyJustPressed(KeyEvent.VK_X) && currentTime - lastAttackTime >= ATTACK_COOLDOWN) {
            player.performBasicAttack();
            lastAttackTime = currentTime;
        }
        
        // Heavy attack
        if (input.isKeyJustPressed(KeyEvent.VK_C) && currentTime - lastAttackTime >= ATTACK_COOLDOWN * 2) {
            player.performHeavyAttack();
            lastAttackTime = currentTime;
        }
        
        // Spells
        if (input.isKeyJustPressed(KeyEvent.VK_1) && currentTime - player.getLastSpellTime() >= player.getSpellCooldown()) {
            player.castFireball();
        }
        
        if (input.isKeyJustPressed(KeyEvent.VK_2) && currentTime - player.getLastSpellTime() >= player.getSpellCooldown()) {
            player.castHeal();
        }
    }
    
    private void performDash() {
        player.setDashing(true);
        player.setDashStartTime(System.currentTimeMillis());
        
        // Calculate dash direction
        double dashX = player.isFacingRight() ? 1 : -1;
        double dashY = 0;
        
        // Allow directional dashing
        if (input.isKeyPressed(KeyEvent.VK_UP)) {
            dashY = -0.7;
        } else if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            dashY = 0.7;
        }
        
        // Normalize and apply dash
        Vector2D dashDirection = new Vector2D(dashX, dashY);
        if (dashDirection.length() > 0) {
            dashDirection.normalize();
            Vector2D dashVel = dashDirection.times(DASH_SPEED);
            player.setVelocity(dashVel);
            
            // Cancel gravity during dash if buffed
            if (player.hasActiveBuff(BuffType.GRAVITY_DASH)) {
                player.setAffectedByGravity(false);
            }
        }
        
        player.setCurrentState(PlayerState.DASHING);
        player.setMovementContext(PlayerStateComponent.MovementContext.DASHING);
        player.setStateChangeTime(System.currentTimeMillis());
        player.addBuffEffect(BuffType.DASH_IMMUNITY, DASH_DURATION);
        player.setDashDirection(dashDirection);
    }
    
    private void performTeleport(double distance) {
        double teleportX = player.isFacingRight() ? distance : -distance;
        double teleportY = 0;
        
        // Allow directional teleporting
        if (input.isKeyPressed(KeyEvent.VK_UP)) {
            teleportY = -distance * 0.7;
        } else if (input.isKeyPressed(KeyEvent.VK_DOWN)) {
            teleportY = distance * 0.7;
        }
        
        // Check if teleport destination is valid (simplified check)
        Vector2D targetPos = player.getPosition().plus(teleportX, teleportY);
        
        // Teleport
        player.setPosition(targetPos);
        player.setCurrentState(PlayerState.DASHING); // Use dash state for teleport animation
        player.setStateChangeTime(System.currentTimeMillis());
        player.addBuffEffect(BuffType.TELEPORT_IMMUNITY, 500);
    }
    
    private void performHook() {
        double hookX = player.isFacingRight() ? HOOK_RANGE : -HOOK_RANGE;
        double hookY = -HOOK_RANGE * 0.5;
        
        Vector2D hookDirection = new Vector2D(hookX, hookY);
        Vector2D hookTarget = player.getPosition().plus(hookDirection);
        
        // Check if we have a valid hook target (simplified)
        if (player.getPosition().distance(hookTarget) <= HOOK_RANGE) {
            player.setHooking(true);
            player.setAffectedByGravity(false);
            player.setCurrentState(PlayerState.HOOKING);
            player.setStateChangeTime(System.currentTimeMillis());
            player.setHookTarget(hookTarget);
            
            // Apply immediate velocity toward hook point
            Vector2D hookVelocity = hookTarget.minus(player.getPosition()).normalized().times(HOOK_SPEED);
            player.setVelocity(hookVelocity);
        }
    }

	@Override
	public ComponentType getType() {
		// TODO Auto-generated method stub
		return ComponentType.INPUT;
	}
}