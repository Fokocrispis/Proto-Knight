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
import game.sprites.LoopingSprite;
import game.sprites.Sprite;
import game.sprites.SpriteSheetManager;

public class PlayerAnimationComponent implements Component {
    private final PlayerEntity player;
    private Sprite currentSprite;
    private final Map<PlayerState, Sprite> stateSprites = new HashMap<>();
    private final Map<String, Sprite> contextualSprites = new HashMap<>();
    
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
        loadSprites();
    }
    
    @Override
    public void update(long deltaTime) {
        updateSprite(deltaTime);
    }
    
    private void loadSprites() {
        SpriteSheetManager spriteManager = new SpriteSheetManager();
        spriteManager.createPlayerSprites();
        
        // Basic movement sprites
        stateSprites.put(PlayerState.IDLE, spriteManager.getSprite("player_idle"));
        stateSprites.put(PlayerState.WALKING, spriteManager.getSprite("player_walk"));
        stateSprites.put(PlayerState.RUNNING, spriteManager.getSprite("player_run"));
        stateSprites.put(PlayerState.JUMPING, spriteManager.getSprite("player_jump"));
        stateSprites.put(PlayerState.FALLING, spriteManager.getSprite("player_fall"));
        stateSprites.put(PlayerState.DASHING, spriteManager.getSprite("player_dash"));
        stateSprites.put(PlayerState.LANDING, spriteManager.getSprite("player_land_quick"));
        stateSprites.put(PlayerState.SLIDING, spriteManager.getSprite("player_slide"));
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
    
    public void updateSpriteForState() {
        Sprite newSprite = null;
        PlayerState currentState = player.getCurrentState();
        PlayerStateComponent.MovementContext context = player.getMovementContext();
        
        // Choose sprite based on state and context
        switch (currentState) {
            case RUNNING:
                if (player.isTurning()) {
                    newSprite = contextualSprites.get("turn_" + (player.isFacingRight() ? "right" : "left"));
                } else {
                    newSprite = stateSprites.get(PlayerState.RUNNING);
                }
                break;
                
            case JUMPING:
                switch (context) {
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
                switch (context) {
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
                switch (context) {
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
                if (context == PlayerStateComponent.MovementContext.CROUCHING) {
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
                newSprite = contextualSprites.get("dash_" + (player.isDashing() ? "start" : "end"));
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
            spriteInfo = String.format("Standard: %dx%d", 
                currentSprite.getSize().width, currentSprite.getSize().height);
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
        
        g.drawString(flagsInfo.toString(), (int)player.getPosition().getX() - 80, (int)player.getPosition().getY() - 60);
    }
    
    public void toggleDebugRender() {
        debugRender = !debugRender;
    }
    
    public Sprite getCurrentSprite() {
        return currentSprite;
    }

	@Override
	public ComponentType getType() {
		// TODO Auto-generated method stub
		return ComponentType.PHYSICS;
	}
}