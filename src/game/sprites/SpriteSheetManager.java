package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import game.resource.ResourceManager;

/**
 * Simple SpriteSheetManager using the correct path structure
 */
public class SpriteSheetManager {
    private final Map<String, BufferedImage> spriteSheets;
    private final Map<String, Sprite> sprites;
    
    // Correct path based on user's structure
    private static final String SPRITE_PATH = "JoannaD'ArcIII_v1.9.2/Sprites/";
    
    public SpriteSheetManager() {
        this.spriteSheets = new HashMap<>();
        this.sprites = new HashMap<>();
    }
    
    /**
     * Loads a sprite sheet from resources
     */
    public BufferedImage loadSpriteSheet(String name, String fileName) {
        try {
            BufferedImage spriteSheet = ResourceManager.getInstance().loadImage(SPRITE_PATH + fileName);
            spriteSheets.put(name, spriteSheet);
            System.out.println("Loaded sprite sheet: " + name);
            return spriteSheet;
        } catch (Exception e) {
            System.err.println("Failed to load sprite sheet: " + fileName);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a sprite from a loaded sprite sheet
     */
    public Sprite createSprite(String spriteName, String spriteSheetName, 
                              Dimension frameSize, double scale, 
                              int firstFrame, int totalFrames, Duration duration) {
        BufferedImage spriteSheet = spriteSheets.get(spriteSheetName);
        if (spriteSheet == null) {
            System.err.println("Sprite sheet not loaded: " + spriteSheetName);
            return null;
        }
        
        Sprite sprite = new Sprite(spriteName, spriteSheet, frameSize, scale, 
                                  firstFrame, totalFrames, duration);
        sprites.put(spriteName, sprite);
        return sprite;
    }
    
    /**
     * Gets a sprite by name
     */
    public Sprite getSprite(String name) {
        return sprites.get(name);
    }
    
    /**
     * Creates all sprites for the player entity
     */
    public void createPlayerSprites() {
        // Load the single comprehensive spritesheet
        loadSpriteSheet("player", "JoannaD'ArcIII-Sheet#1.png");
        
        // Define frame size and scale
        Dimension frameSize = new Dimension(98, 66);
        double scale = 3.0;
        
        // Create animations (estimate frame positions - adjust as needed)
        createSprite("player_idle", "player", frameSize, scale, 
                    0, 6, Duration.ofSeconds(1));
        
        createSprite("player_run", "player", frameSize, scale,
        		
                    37, 23, Duration.ofMillis(800));
        
        createSprite("player_dash", "player", frameSize, scale, 
                    /*222*/74, 12, Duration.ofMillis(300));
        
        createSprite("player_attack", "player", frameSize, scale, 
                    20, 6, Duration.ofMillis(350));
        
        createSprite("player_air_attack", "player", frameSize, scale, 
                    26, 6, Duration.ofMillis(450));
        
        createSprite("player_jump", "player", frameSize, scale, 
                    111, 10, Duration.ofMillis(750));
        
        createSprite("player_fall", "player", frameSize, scale, 
                    117, 4, Duration.ofMillis(300));
        
        createSprite("player_crouch", "player", frameSize, scale, 
                    41, 4, Duration.ofSeconds(1));
        
        createSprite("player_slide", "player", frameSize, scale, 
                    45, 5, Duration.ofMillis(300));
        
        createSprite("player_hurt", "player", frameSize, scale, 
                    50, 3, Duration.ofMillis(500));
        
        createSprite("player_death", "player", frameSize, scale, 
                    53, 8, Duration.ofMillis(800));
        
        createSprite("player_wall_slide", "player", frameSize, scale, 
                    61, 3, Duration.ofMillis(400));
        
        createSprite("player_wall_jump", "player", frameSize, scale, 
                    64, 5, Duration.ofMillis(400));
        
        createSprite("player_climb", "player", frameSize, scale, 
                    69, 4, Duration.ofMillis(600));
    }
    
    /**
     * Clears all cached sprites
     */
    public void clearCache() {
        sprites.clear();
        spriteSheets.clear();
    }
}