package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import game.resource.ResourceManager;

/**
 * Simple SpriteSheetManager using your original sprite definition
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
     * Creates all sprites for the player entity using your exact definition
     */
    public void createPlayerSprites() {
        // Load the comprehensive spritesheet
        loadSpriteSheet("player", "JoannaD'ArcIII-Sheet#1.png");
        
        // Define frame size and scale
        Dimension frameSize = new Dimension(98, 66);
        double scale = 3.0;
        
        // Create animations (exact frame positions from your definition)
        createSprite("player_idle", "player", frameSize, scale, 
                    0, 7, Duration.ofSeconds(1));
        
        createSprite("player_walk", "player", frameSize, scale,
                    37, 23, Duration.ofMillis(800));
        
        createSprite("player_run_start", "player", frameSize, scale, 
                    74, 6, Duration.ofMillis(300));
        
        createSprite("player_run", "player", frameSize, scale, 
                    80, 4, Duration.ofMillis(400));
        
        createSprite("player_jump", "player", frameSize, scale, 
                    111, 6, Duration.ofMillis(500));
        
        createSprite("player_fall", "player", frameSize, scale, 
                    117, 4, Duration.ofMillis(600));
        
        createSprite("player_land_quick", "player", frameSize, scale, 
                    122, 2, Duration.ofMillis(150));
        
        createSprite("player_land_full", "player", frameSize, scale, 
                    122, 4, Duration.ofMillis(400));
        
        // Add more animations based on your rows
        // Row 5: Jump while running with rolling landing
        // Row 6: Rolling
        createSprite("player_roll", "player", frameSize, scale, 
                    6*37, 8, Duration.ofMillis(400));
        
        // Row 7: Dashing
        createSprite("player_dash", "player", frameSize, scale, 
                    7*37, 6, Duration.ofMillis(250));
        
        // Row 8: Blocking
        createSprite("player_block", "player", frameSize, scale, 
                    8*37, 5, Duration.ofMillis(300));
    }
    
    /**
     * Clears all cached sprites
     */
    public void clearCache() {
        sprites.clear();
        spriteSheets.clear();
    }
}