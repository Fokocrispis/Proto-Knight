package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import game.resource.ResourceManager;

/**
 * Manages sprite sheets and creates sprites for different animations.
 */
public class SpriteSheetManager {
    private final Map<String, BufferedImage> spriteSheets;
    private final Map<String, Sprite> sprites;
    
    public SpriteSheetManager() {
        this.spriteSheets = new HashMap<>();
        this.sprites = new HashMap<>();
    }
    
    /**
     * Loads a sprite sheet from resources.
     */
    public BufferedImage loadSpriteSheet(String name, String fileName) {
        try {
            BufferedImage spriteSheet = ResourceManager.getInstance().loadImage(fileName);
            spriteSheets.put(name, spriteSheet);
            return spriteSheet;
        } catch (Exception e) {
            System.err.println("Failed to load sprite sheet: " + fileName);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a sprite from a loaded sprite sheet.
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
     * Gets a sprite by name.
     */
    public Sprite getSprite(String name) {
        return sprites.get(name);
    }
    
    /**
     * Creates all sprites for the player entity.
     */
    public void createPlayerSprites() {
        // Load the player sprite sheet
        loadSpriteSheet("player", "player-spritesheet.png");
        
        // Define frame size and scale
        Dimension frameSize = new Dimension(50, 37);
        double scale = 6.0;
        
        // Create sprites for different animations
        createSprite("player_idle", "player", frameSize, scale, 
                    0, 4, Duration.ofSeconds(1));
        
        createSprite("player_run", "player", frameSize, scale, 
                    8, 6, Duration.ofSeconds(1));
        
        createSprite("player_crouch", "player", frameSize, scale, 
                    4, 4, Duration.ofSeconds(1));
        
        createSprite("player_attack", "player", frameSize, scale, 
                    42, 4, Duration.ofMillis(350));
        
        createSprite("player_air_attack", "player", frameSize, scale, 
                    97, 4, Duration.ofMillis(450));
        
        // Add more sprites as needed
        createSprite("player_jump", "player", frameSize, scale, 
                    14, 4, Duration.ofMillis(500));
    }
}