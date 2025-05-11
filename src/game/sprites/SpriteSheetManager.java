package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import game.resource.ResourceManager;

/**
 * Fixed sprite sheet manager with proper configuration
 */
public class SpriteSheetManager {
    private final Map<String, BufferedImage> spriteSheets;
    private final Map<String, Sprite> sprites;
    private final Map<String, SpriteConfig> configurations;
    
    private static final String SPRITE_PATH = "JoannaD'ArcIII_v1.9.2/Sprites/";
    
    public SpriteSheetManager() {
        this.spriteSheets = new HashMap<>();
        this.sprites = new HashMap<>();
        this.configurations = new HashMap<>();
        
        // Initialize sprite configurations
        setupSpriteConfigurations();
    }
    
    /**
     * Setup all sprite configurations
     */
    private void setupSpriteConfigurations() {
        // Main player sprite sheet (fixed frame counts)
        SpriteConfig playerConfig = new SpriteConfig("player", "JoannaD'ArcIII-Sheet#1.png", 
                                                    new Dimension(98, 66), 3.0);
        
        // Add animations with corrected frame counts (based on actual spritesheet)
        playerConfig.addAnimation("idle", 0, 6, 1000, true);
        playerConfig.addAnimation("walk", 37, 23, 800, true);
        playerConfig.addAnimation("run_start", 74, 6, 300, false);
        playerConfig.addAnimation("run", 80, 4, 400, true);
        playerConfig.addAnimation("run_stop", 95, 6, 300, false);
        playerConfig.addAnimation("jump", 111, 6, 500, false);
        playerConfig.addAnimation("fall", 117, 4, 600, true);
        playerConfig.addAnimation("land_quick", 122, 2, 150, false);
        playerConfig.addAnimation("land_full", 122, 4, 400, false);
        playerConfig.addAnimation("roll", 222, 8, 400, false);
        playerConfig.addAnimation("dash", 259, 6, 250, false);
        playerConfig.addAnimation("block", 296, 3, 300, true); // Fixed frame count
        playerConfig.addAnimation("hurt", 330, 3, 500, false); // Fixed frame position and count
        // Removed death animation as it seems to exceed bounds
        
        // Register player configuration
        registerConfiguration(playerConfig);
        
        // Don't try to load attack sprites for now since the path is incorrect
        // You can add them later when you have the correct path
    }
    
    /**
     * Register a sprite configuration and load sprites
     */
    private void registerConfiguration(SpriteConfig config) {
        configurations.put(config.getName(), config);
        
        // Load the sprite sheet
        if (!loadSpriteSheet(config.getName(), config.getFileName())) {
            System.err.println("Failed to load sprite sheet: " + config.getFileName());
            return;
        }
        
        // Create all sprites for this configuration
        createSpritesFromConfig(config);
    }
    
    /**
     * Load a sprite sheet from resources
     */
    public boolean loadSpriteSheet(String name, String fileName) {
        try {
            BufferedImage spriteSheet = ResourceManager.getInstance().loadImage(SPRITE_PATH + fileName);
            if (spriteSheet != null) {
                spriteSheets.put(name, spriteSheet);
                System.out.println("Loaded sprite sheet: " + name + " (" + spriteSheet.getWidth() + "x" + spriteSheet.getHeight() + ")");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to load sprite sheet: " + fileName);
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Create sprites from a configuration with proper bounds checking
     */
    private void createSpritesFromConfig(SpriteConfig config) {
        BufferedImage sheet = spriteSheets.get(config.getName());
        if (sheet == null) return;
        
        // Calculate maximum frames available
        int maxColumns = sheet.getWidth() / config.getFrameSize().width;
        int maxRows = sheet.getHeight() / config.getFrameSize().height;
        int maxFrames = maxColumns * maxRows;
        
        for (Map.Entry<String, AnimationData> entry : config.getAnimations().entrySet()) {
            String animName = entry.getKey();
            AnimationData anim = entry.getValue();
            
            // Validate frame indices
            int lastFrame = anim.getFirstFrame() + anim.getFrameCount() - 1;
            if (lastFrame >= maxFrames) {
                System.err.println("Warning: Animation '" + animName + "' exceeds sprite sheet bounds!");
                System.err.println("  Requested: " + (anim.getFirstFrame() + anim.getFrameCount()) + " frames");
                System.err.println("  Available: " + maxFrames + " frames");
                
                // Adjust frame count to fit
                int adjustedFrameCount = Math.max(1, maxFrames - anim.getFirstFrame());
                System.err.println("  Adjusted to: " + adjustedFrameCount + " frames");
                
                // Create adjusted animation data
                anim = new AnimationData(
                    anim.getName(),
                    anim.getFirstFrame(),
                    adjustedFrameCount,
                    anim.getDuration(),
                    anim.isLooping()
                );
            }
            
            String spriteName = config.getName() + "_" + animName;
            
            // Create the appropriate sprite type based on looping
            Sprite sprite;
            if (anim.isLooping()) {
                sprite = new LoopingSprite(
                    spriteName,
                    sheet,
                    config.getFrameSize(),
                    config.getScale(),
                    config.getScale(),
                    anim.getFirstFrame(),
                    anim.getFrameCount(),
                    Duration.ofMillis(anim.getDuration()),
                    true
                );
            } else {
                sprite = new Sprite(
                    spriteName,
                    sheet,
                    config.getFrameSize(),
                    config.getScale(),
                    anim.getFirstFrame(),
                    anim.getFrameCount(),
                    Duration.ofMillis(anim.getDuration())
                );
            }
            
            sprites.put(spriteName, sprite);
            System.out.println("Created sprite: " + spriteName);
        }
    }
    
    /**
     * Get a sprite by name
     */
    public Sprite getSprite(String name) {
        return sprites.get(name);
    }
    
    /**
     * Get all sprites from a specific configuration
     */
    public Map<String, Sprite> getSpritesForConfig(String configName) {
        Map<String, Sprite> configSprites = new HashMap<>();
        String prefix = configName + "_";
        
        for (Map.Entry<String, Sprite> entry : sprites.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                configSprites.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        
        return configSprites;
    }
    
    /**
     * Create all player sprites (for backwards compatibility)
     */
    public void createPlayerSprites() {
        // Sprites are automatically created when configurations are registered
        // This method is kept for backwards compatibility
    }
    
    /**
     * Clear all caches
     */
    public void clearCache() {
        sprites.clear();
        spriteSheets.clear();
        configurations.clear();
    }
    
    /**
     * Get all available sprite names
     */
    public String[] getAvailableSprites() {
        return sprites.keySet().toArray(new String[0]);
    }
}

/**
 * Configuration class for a sprite sheet
 */
class SpriteConfig {
    private final String name;
    private final String fileName;
    private final Dimension frameSize;
    private final double scale;
    private final Map<String, AnimationData> animations;
    
    public SpriteConfig(String name, String fileName, Dimension frameSize, double scale) {
        this.name = name;
        this.fileName = fileName;
        this.frameSize = frameSize;
        this.scale = scale;
        this.animations = new HashMap<>();
    }
    
    /**
     * Add an animation to this configuration
     */
    public SpriteConfig addAnimation(String name, int firstFrame, int frameCount, 
                                   long duration, boolean looping) {
        animations.put(name, new AnimationData(name, firstFrame, frameCount, duration, looping));
        return this; // For method chaining
    }
    
    // Getters
    public String getName() { return name; }
    public String getFileName() { return fileName; }
    public Dimension getFrameSize() { return frameSize; }
    public double getScale() { return scale; }
    public Map<String, AnimationData> getAnimations() { return animations; }
}

/**
 * Data class for animation configuration
 */
class AnimationData {
    private final String name;
    private final int firstFrame;
    private final int frameCount;
    private final long duration;
    private final boolean looping;
    
    public AnimationData(String name, int firstFrame, int frameCount, long duration, boolean looping) {
        this.name = name;
        this.firstFrame = firstFrame;
        this.frameCount = frameCount;
        this.duration = duration;
        this.looping = looping;
    }
    
    // Getters
    public String getName() { return name; }
    public int getFirstFrame() { return firstFrame; }
    public int getFrameCount() { return frameCount; }
    public long getDuration() { return duration; }
    public boolean isLooping() { return looping; }
}