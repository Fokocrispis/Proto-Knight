package game.entity.component;

import java.awt.Graphics2D;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import game.Vector2D;
import game.entity.PlayerEntity;
import game.entity.PlayerState;
import game.entity.component.Component.ComponentType;
import game.sprites.AdjustableSequenceSprite;
import game.sprites.CharacterAnimationManager;
import game.sprites.LoopingSprite;
import game.sprites.Sprite;

public class PlayerAnimationComponent implements Component {
    private final PlayerEntity player;
    private Sprite currentSprite;
    private final Map<PlayerState, Sprite> stateSprites = new HashMap<>();
    private final Map<String, Sprite> contextualSprites = new HashMap<>();
    
    // Character animation manager
    private final CharacterAnimationManager animationManager;
    
    // Sprite dimensions
    private static final int HITBOX_WIDTH = 40;
    private static final int HITBOX_HEIGHT = 140;
    private static final int SPRITE_WIDTH = 80;
    private static final int SPRITE_HEIGHT = 160;
    
    private boolean isSpriteLocked = false;
    private long activeSpriteTimer = 0;
    private boolean debugRender = false;
    
    public PlayerAnimationComponent(PlayerEntity player) {
        this.player = player;
        
        // Initialize animation manager with character ID
        this.animationManager = new CharacterAnimationManager("Joanna");
        
        // Load all character animations
        animationManager.loadAllAnimations();
        
        // Map animations to states
        loadSprites();
    }
    
    @Override
    public void update(long deltaTime) {
        updateSprite(deltaTime);
    }
    
    private void loadSprites() {
        // Map character animations to player states
        stateSprites.put(PlayerState.IDLE, animationManager.getAnimation("idle"));
        stateSprites.put(PlayerState.WALKING, animationManager.getAnimation("run"));
        stateSprites.put(PlayerState.RUNNING, animationManager.getAnimation("run"));
        stateSprites.put(PlayerState.ATTACKING, animationManager.getAnimation("light_attack"));
        stateSprites.put(PlayerState.DASHING, animationManager.getAnimation("dash"));
        stateSprites.put(PlayerState.LANDING, animationManager.getAnimation("land"));
        
        // Setup contextual sprites for different states/transitions
        contextualSprites.put("turn_left", animationManager.getAnimation("break_run"));
        contextualSprites.put("turn_right", animationManager.getAnimation("break_run"));
        contextualSprites.put("run_to_stop", animationManager.getAnimation("break_run"));
        contextualSprites.put("run_start", animationManager.getAnimation("to_run"));
        
        // Common action sprites
        contextualSprites.put("light_attack", animationManager.getAnimation("light_attack"));
        contextualSprites.put("dash", animationManager.getAnimation("dash"));
        contextualSprites.put("land", animationManager.getAnimation("land"));
        
        // Set initial sprite
        currentSprite = stateSprites.get(PlayerState.IDLE);
        
        // Print loaded animations for debugging
        animationManager.printLoadedAnimations();
    }
    
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
    
    private void handleAnimationComplete() {
        if (player.getCurrentState() == PlayerState.LANDING) {
            player.setCurrentState(PlayerState.IDLE);
            player.setMovementContext(PlayerStateComponent.MovementContext.NORMAL);
            updateSpriteForState();
        } else if (player.getCurrentState() == PlayerState.ATTACKING) {
            player.setAttacking(false);
            player.setCurrentState(PlayerState.IDLE);
            updateSpriteForState();
        }
    }
    
    /**
     * Gets an animation by name
     */
    public Sprite getAnimation(String name) {
        // First check state sprites
        for (Map.Entry<PlayerState, Sprite> entry : stateSprites.entrySet()) {
            if (entry.getKey().name().toLowerCase().equals(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        
        // Then check contextual sprites
        Sprite sprite = contextualSprites.get(name);
        if (sprite != null) {
            return sprite;
        }
        
        // Then check if animation manager has it
        return animationManager.getAnimation(name);
    }
    
    public void updateSpriteForState() {
        Sprite newSprite = null;
        PlayerState currentState = player.getCurrentState();
        PlayerStateComponent.MovementContext context = player.getMovementContext();
        
        // Check if there's a Combat component for special attack states
        PlayerAttackComponent attackComponent = null;
        if (player.hasComponent(ComponentType.COMBAT)) {
            attackComponent = player.getComponent(ComponentType.COMBAT);
        }
        
        // Choose sprite based on state and context
        switch (currentState) {
            case ATTACKING:
                newSprite = contextualSprites.get("light_attack");
                if (newSprite == null) {
                    newSprite = stateSprites.get(PlayerState.ATTACKING);
                }
                break;
                
            case RUNNING:
                if (player.isTurning()) {
                    newSprite = contextualSprites.get("turn_" + (player.isFacingRight() ? "right" : "left"));
                } else {
                    // Check if we're just starting to run
                    double speed = Math.abs(player.getVelocity().getX());
                    long runTime = System.currentTimeMillis() - player.getStateChangeTime();
                    
                    if (runTime < 200 && speed < 400) {
                        // We're just starting to run
                        newSprite = contextualSprites.get("run_start");
                    } else {
                        newSprite = stateSprites.get(PlayerState.RUNNING);
                    }
                }
                break;
                
            case DASHING:
                newSprite = contextualSprites.get("dash");
                if (newSprite == null) {
                    newSprite = stateSprites.get(PlayerState.DASHING);
                }
                break;
                
            case LANDING:
                newSprite = contextualSprites.get("land");
                if (newSprite == null) {
                    newSprite = stateSprites.get(PlayerState.LANDING);
                }
                break;
                
            case WALKING:
                // Check velocity to decide between walking and running
                double speed = Math.abs(player.getVelocity().getX());
                if (speed > 400) {
                    newSprite = stateSprites.get(PlayerState.RUNNING);
                } else {
                    newSprite = stateSprites.get(PlayerState.WALKING);
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
    private void resetCurrentSprite() {
        if (currentSprite instanceof LoopingSprite) {
            ((LoopingSprite) currentSprite).reset();
        } else if (currentSprite instanceof AdjustableSequenceSprite) {
            ((AdjustableSequenceSprite) currentSprite).reset();
        } else if (currentSprite != null) {
            currentSprite.reset();
        }
    }
    
    public void render(Graphics2D g) {
        if (!player.isVisible() || currentSprite == null) return;
        
        // Default sprite offset adjustment
        int SPRITE_OFFSET_X = 0;
        int SPRITE_OFFSET_Y = 50;
        
        // Get sprite dimensions
        int renderedWidth = currentSprite.getSize().width;
        int renderedHeight = currentSprite.getSize().height;
        
        // Calculate sprite position
        int spriteX, spriteY;
        
        if (currentSprite instanceof AdjustableSequenceSprite) {
            // Use the built-in positioning for adjustable sprites
            AdjustableSequenceSprite adjustableSprite = (AdjustableSequenceSprite) currentSprite;
            spriteX = adjustableSprite.getRenderX(player.getPosition().getX());
            spriteY = adjustableSprite.getRenderY(player.getPosition().getY(), HITBOX_HEIGHT);
        } else {
            // Standard positioning for regular sprites
            spriteX = (int)(player.getPosition().getX() - renderedWidth / 2.0) + SPRITE_OFFSET_X;
            spriteY = (int)(player.getPosition().getY() - HITBOX_HEIGHT / 2.0 - (renderedHeight - HITBOX_HEIGHT)) + SPRITE_OFFSET_Y;
        }
        
        // Draw sprite (flipped if facing left)
        if (player.isFacingRight()) {
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
        
        // Draw hook line if hooking
        if (player.isHooking() && player.getHookTarget() != null) {
            g.setColor(Color.CYAN);
            g.drawLine((int)player.getPosition().getX(), (int)player.getPosition().getY(), 
                      (int)player.getHookTarget().getX(), (int)player.getHookTarget().getY());
        }
        
        // Draw attack hitboxes if in combat and debug mode
        if (debugRender && player.hasComponent(ComponentType.COMBAT)) {
            PlayerAttackComponent attackComponent = player.getComponent(ComponentType.COMBAT);
            attackComponent.renderDebug(g);
        }
        
        // Draw debug info when enabled
        if (debugRender) {
            renderDebugInfo(g);
        }
    }
    
    private void drawHealthManaBar(Graphics2D g) {
        // Health bar
        int barWidth = 60;
        int barHeight = 8;
        int barX = (int)player.getPosition().getX() - barWidth / 2;
        int barY = (int)player.getPosition().getY() - HITBOX_HEIGHT / 2 - 15; // Above the hitbox
        
        // Background
        g.setColor(Color.BLACK);
        g.fillRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2);
        
        // Background bar
        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Health bar
        g.setColor(Color.RED);
        int healthWidth = (int)((double)player.getHealth() / player.getMaxHealth() * barWidth);
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
        int manaWidth = (int)((double)player.getMana() / player.getMaxMana() * barWidth);
        g.fillRect(barX, barY, manaWidth, barHeight);
    }
    
    private void renderDebugInfo(Graphics2D g) {
        // Draw hitbox
        g.setColor(new Color(255, 0, 0, 128));
        g.drawRect(
            (int)(player.getPosition().getX() - HITBOX_WIDTH / 2),
            (int)(player.getPosition().getY() - HITBOX_HEIGHT / 2),
            HITBOX_WIDTH, 
            HITBOX_HEIGHT
        );
        
        // Draw center point
        g.setColor(Color.YELLOW);
        g.fillOval((int)player.getPosition().getX() - 2, (int)player.getPosition().getY() - 2, 4, 4);
        
        // Display sprite info
        g.setColor(Color.WHITE);
        String spriteInfo = "Unknown";
        
        if (currentSprite instanceof AdjustableSequenceSprite) {
            AdjustableSequenceSprite sprite = (AdjustableSequenceSprite) currentSprite;
            spriteInfo = String.format("AdjustableSequence: %dx%d, offset: %d,%d",
                sprite.getSize().width, sprite.getSize().height,
                sprite.getOffsetX(), sprite.getOffsetY());
        } else if (currentSprite != null) {
            spriteInfo = String.format("Standard: %dx%d [%d/%d]", 
                currentSprite.getSize().width, currentSprite.getSize().height,
                currentSprite.getFrameIndex() + 1, currentSprite.getTotalFrames());
        }
        
        g.drawString(spriteInfo, (int)player.getPosition().getX() - 80, (int)player.getPosition().getY() - 100);
        
        // Display state info
        g.drawString("State: " + player.getCurrentState() + ", Context: " + player.getMovementContext(), 
                   (int)player.getPosition().getX() - 80, (int)player.getPosition().getY() - 80);
                   
        // Display flags
        StringBuilder flagsInfo = new StringBuilder();
        if (player.isOnGround()) flagsInfo.append("GROUND ");
        if (player.isCrouching()) flagsInfo.append("CROUCH ");
        if (player.isSliding()) flagsInfo.append("SLIDE ");
        if (player.isDashing()) flagsInfo.append("DASH ");
        if (player.isJumping()) flagsInfo.append("JUMP ");
        if (player.isAttacking()) flagsInfo.append("ATTACK ");
        
        // Add combo info if in combat state
        PlayerAttackComponent attackComponent = null;
        if (player.hasComponent(ComponentType.COMBAT)) {
            attackComponent = player.getComponent(ComponentType.COMBAT);
            if (attackComponent.isComboAttacking()) {
                flagsInfo.append("COMBO(").append(attackComponent.getComboCount()).append(") ");
            }
        }
        
        g.drawString(flagsInfo.toString(), (int)player.getPosition().getX() - 80, (int)player.getPosition().getY() - 60);
        
        // Show animation info
        g.drawString("Animation: " + (currentSprite != null ? currentSprite.getName() : "none"), 
                   (int)player.getPosition().getX() - 80, (int)player.getPosition().getY() - 40);
    }
    
    public void toggleDebugRender() {
        debugRender = !debugRender;
    }
    
    public Sprite getCurrentSprite() {
        return currentSprite;
    }
    
    /**
     * Handles transition from running to walking or idle
     */
    private void handleRunningSlowdown(float dt) {
        // Only handle when on ground and not animation-locked
        if (!player.isOnGround() || player.isAnimationLocked()) return;
        
        // Check if we're running but slowing down
        if (player.getCurrentState() == PlayerState.RUNNING) {
            double speed = Math.abs(player.getVelocity().getX());
            double targetSpeed = Math.abs(player.getTargetVelocityX());
            
            // If we're slowing down significantly
            if (speed > 400 && targetSpeed < 100) {
                // Special transition animation from running to stop
                player.setCurrentState(PlayerState.IDLE);
                player.setMovementContext(PlayerStateComponent.MovementContext.NORMAL);
                
                // Use the run_to_stop animation
                if (player.hasComponent(ComponentType.ANIMATION)) {
                    PlayerAnimationComponent animComponent = player.getComponent(ComponentType.ANIMATION);
                    Sprite runToStopSprite = animComponent.getAnimation("run_to_stop");
                        
                    if (runToStopSprite != null) {
                        // Lock animation for the transition
                        player.lockAnimation(400);
                        player.updateSpriteForState();
                    }
                }
            }
        }
    }

    @Override
    public ComponentType getType() {
        return ComponentType.ANIMATION;
    }
}