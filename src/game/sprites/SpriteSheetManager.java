package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import game.resource.ResourceManager;

/**
 * Updated SpriteSheetManager with simpler integration for sequence sprites
 */
public class SpriteSheetManager {
    private final Map<String, BufferedImage> spriteSheets;
    private final Map<String, Sprite> sprites;
    private final SpriteSequenceManager sequenceManager;
    
    private static final String SPRITE_PATH = "JoannaD'ArcIII_v1.9.2/Sprites/";
    
    public SpriteSheetManager() {
        this.spriteSheets = new HashMap<>();
        this.sprites = new HashMap<>();
        this.sequenceManager = new SpriteSequenceManager();
    }
    
    /**
     * Create player sprites with walking animation using sequence approach
     */
    public void createPlayerSprites() {
        // Load the old sprite sheet for non-walking animations
        loadSpriteSheet("player", "JoannaD'ArcIII-Sheet#1.png");
        
        // Define frame size and scale
        Dimension frameSize = new Dimension(98, 66);
        double scale = 3.0;
        
        // Create standard animations from sprite sheet (except walking)
        createSprite("player_idle", "player", frameSize, scale, 0, 6, Duration.ofMillis(1000));
        createSprite("player_run", "player", frameSize, scale, 80, 4, Duration.ofMillis(400));
        createSprite("player_jump", "player", frameSize, scale, 111, 6, Duration.ofMillis(500));
        createSprite("player_fall", "player", frameSize, scale, 117, 4, Duration.ofMillis(600));
        
        // Add any other sprite sheet animations you need
        
        try {
            // Load the walking sprite from sequences
            Sprite walkingSprite = sequenceManager.getSprite("player_walk");
            
            // If sprite doesn't exist yet, create it
            if (walkingSprite == null) {
                // Define walking animation parameters
                Dimension walkingFrameSize = new Dimension(64, 64);  // Source size
                Dimension walkingDisplaySize = new Dimension(190, 182);  // Target size
                int walkingOffsetX = 0;  // Horizontal offset
                int walkingOffsetY = 25;  // Vertical offset
                
                // Load the walking animation
                walkingSprite = sequenceManager.loadSpriteSequence(
                    "player_walk",         // Animation name
                    "Walk/Walking",        // Path to frames
                    "Walking",             // Base filename
                    18,                    // 18 frames
                    walkingFrameSize,      // Source frame size
                    walkingDisplaySize,    // Display size
                    walkingOffsetX,        // X offset
                    walkingOffsetY,        // Y offset
                    Duration.ofMillis(850), // Duration
                    true                   // Looping animation
                );
            }
            
            // Add the walking sprite to our sprite collection
            if (walkingSprite != null) {
                sprites.put("player_walk", walkingSprite);
                System.out.println("Successfully integrated walking animation from sequence");
            } else {
                // Fallback to old sheet-based animation if sequence loading fails
                System.err.println("Walking sprite was null! Using fallback.");
                createSprite("player_walk", "player", frameSize, scale, 37, 23, Duration.ofMillis(850));
            }
        } catch (Exception e) {
            System.err.println("Error loading walking animation: " + e.getMessage());
            e.printStackTrace();
            // Fallback to old animation
            createSprite("player_walk", "player", frameSize, scale, 37, 23, Duration.ofMillis(850));
        }
    }
    
    /**
     * Load a sprite sheet from resources
     */
    public boolean loadSpriteSheet(String name, String fileName) {
        try {
            BufferedImage spriteSheet = ResourceManager.getInstance().loadImage(SPRITE_PATH + fileName);
            if (spriteSheet != null) {
                spriteSheets.put(name, spriteSheet);
                System.out.println("Loaded sprite sheet: " + name);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to load sprite sheet: " + fileName);
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Create a sprite from a sprite sheet
     */
    public Sprite createSprite(String name, String sheetName, Dimension frameSize, 
                               double scale, int firstFrame, int frameCount, 
                               Duration duration) {
        
        BufferedImage sheet = spriteSheets.get(sheetName);
        if (sheet == null) {
            System.err.println("Sprite sheet not found: " + sheetName);
            return null;
        }
        
        Sprite sprite = new Sprite(name, sheet, frameSize, scale, firstFrame, 
                                  frameCount, duration);
        sprites.put(name, sprite);
        
        return sprite;
    }
    
    /**
     * Get a sprite by name
     */
    public Sprite getSprite(String name) {
        return sprites.get(name);
    }
    
    /**
     * Clear cached sprites and sheets
     */
    public void clearCache() {
        sprites.clear();
        spriteSheets.clear();
    }
    
    /**
     * Get the sequence manager for direct access
     */
    public SpriteSequenceManager getSequenceManager() {
        return sequenceManager;
    }
}