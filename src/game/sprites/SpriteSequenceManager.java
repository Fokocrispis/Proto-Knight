package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import game.resource.ResourceManager;

/**
 * Manages sprite animations using sequences of individual image files
 * with support for manual size and offset adjustments.
 */
public class SpriteSequenceManager {
    private static final String BASE_PATH = "JoannaD'ArcIII_v1.9.2/Sprites/";
    
    // Cache for loaded sprites and frames
    private final Map<String, Sprite> sprites = new HashMap<>();
    private final Map<String, List<BufferedImage>> frameCache = new HashMap<>();
    private final ResourceManager resourceManager;
    
    public SpriteSequenceManager() {
        this.resourceManager = ResourceManager.getInstance();
    }
    
    /**
     * Creates all player sprites using the sequence approach.
     * This method is called by SpriteSheetManager.
     */
    public void createPlayerSprites() {
        // Define standard frame size and scale
        Dimension defaultFrameSize = new Dimension(64, 64);
        Dimension displaySize = new Dimension(190, 160);
        
        // Walking animation
        loadSpriteSequence(
            "player_walk",
            "Walk/Walking",
            "Walking",
            18,
            defaultFrameSize,
            displaySize,
            0,  // Default X offset (adjust as needed)
            0,  // Default Y offset (adjust as needed)
            Duration.ofMillis(850),
            true
        );
        
        // Add more animations as needed
        // For example:
        /*
        loadSpriteSequence(
            "player_idle",
            "Idle/Idle",
            "Idle",
            6,
            defaultFrameSize,
            displaySize,
            0,
            0,
            Duration.ofMillis(1000),
            true
        );
        */
        
        System.out.println("SpriteSequenceManager: Created player sprites");
    }
    
    /**
     * Loads an animation sequence with custom size adjustments.
     * 
     * @param animationName The name of the animation
     * @param basePath The path to the animation files
     * @param baseFileName The base name of the files
     * @param frameCount The number of frames in the animation
     * @param frameSize The size of each frame in the source files
     * @param displaySize The size to display the sprite (for scaling)
     * @param offsetX X offset for positioning
     * @param offsetY Y offset for positioning
     * @param duration The duration of the complete animation
     * @param looping Whether the animation should loop
     * @return The created sprite object
     */
    public Sprite loadSpriteSequence(
            String animationName,
            String basePath,
            String baseFileName,
            int frameCount,
            Dimension frameSize,
            Dimension displaySize,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean looping) {
        
        // Check if already loaded
        if (sprites.containsKey(animationName)) {
            return sprites.get(animationName);
        }
        
        // Construct the full path
        String fullPath = BASE_PATH + basePath;
        if (!basePath.endsWith("/") && !basePath.isEmpty()) {
            fullPath += "/";
        }
        
        // Load all frames
        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            int frameIndex = i + 1; // Start from 1
            String fileName = fullPath + baseFileName + frameIndex + ".png";
            
            try {
                BufferedImage frame = resourceManager.loadImage(fileName);
                if (frame != null) {
                    frames.add(frame);
                } else {
                    System.err.println("Failed to load frame: " + fileName);
                }
            } catch (IOException e) {
                System.err.println("Error loading frame " + fileName + ": " + e.getMessage());
            }
        }
        
        // If no frames were loaded, return null
        if (frames.isEmpty()) {
            System.err.println("No frames loaded for animation: " + animationName);
            return null;
        }
        
        // Store frames in cache
        frameCache.put(animationName, frames);
        
        // Calculate scale factors based on desired display size
        double scaleX = (double)displaySize.width / frameSize.width;
        double scaleY = (double)displaySize.height / frameSize.height;
        
        // Create the sprite with adjustments
        AdjustableSequenceSprite sprite = new AdjustableSequenceSprite(
            animationName, 
            frames, 
            frameSize, 
            scaleX, 
            scaleY,
            offsetX,
            offsetY,
            duration, 
            looping
        );
        
        sprites.put(animationName, sprite);
        System.out.println(String.format(
            "Loaded sprite sequence: %s (%d frames, size: %dx%d, display: %dx%d, offset: %d,%d)",
            animationName, frames.size(), frameSize.width, frameSize.height, 
            displaySize.width, displaySize.height, offsetX, offsetY));
        
        return sprite;
    }
    
    /**
     * Simplified version with default offsets (0,0)
     */
    public Sprite loadSpriteSequence(
            String animationName,
            String basePath,
            String baseFileName,
            int frameCount,
            Dimension frameSize,
            double scale,
            Duration duration,
            boolean looping) {
        
        // Use default offsets (0,0)
        return loadSpriteSequence(
            animationName,
            basePath,
            baseFileName,
            frameCount,
            frameSize,
            scale,
            0,  // Default X offset
            0,  // Default Y offset
            duration,
            looping
        );
    }
    
    /**
     * Simplified version that assumes uniform scaling and requires offset values
     */
    public Sprite loadSpriteSequence(
            String animationName,
            String basePath,
            String baseFileName,
            int frameCount,
            Dimension frameSize,
            double scale,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean looping) {
        
        // Calculate display size based on uniform scale
        Dimension displaySize = new Dimension(
            (int)(frameSize.width * scale),
            (int)(frameSize.height * scale)
        );
        
        return loadSpriteSequence(
            animationName,
            basePath,
            baseFileName,
            frameCount,
            frameSize,
            displaySize,
            offsetX,
            offsetY,
            duration,
            looping
        );
    }
    
    /**
     * Complete version with custom file extension and start index
     */
    public Sprite loadSpriteSequence(
            String animationName,
            String basePath,
            String baseFileName,
            int frameCount,
            int startIndex,
            String fileExtension,
            Dimension frameSize,
            double scale,
            Duration duration,
            boolean looping) {
        
        // Use default offsets
        return loadSpriteSequence(
            animationName,
            basePath,
            baseFileName,
            frameCount,
            startIndex,
            fileExtension,
            frameSize,
            scale,
            0,  // Default X offset
            0,  // Default Y offset
            duration,
            looping
        );
    }
    
    /**
     * Complete version with custom file extension, start index and offsets
     */
    public Sprite loadSpriteSequence(
            String animationName,
            String basePath,
            String baseFileName,
            int frameCount,
            int startIndex,
            String fileExtension,
            Dimension frameSize,
            double scale,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean looping) {
        
        // Calculate display size based on uniform scale
        Dimension displaySize = new Dimension(
            (int)(frameSize.width * scale),
            (int)(frameSize.height * scale)
        );
        
        // Check if already loaded
        if (sprites.containsKey(animationName)) {
            return sprites.get(animationName);
        }
        
        // Construct the full path
        String fullPath = BASE_PATH + basePath;
        if (!basePath.endsWith("/") && !basePath.isEmpty()) {
            fullPath += "/";
        }
        
        // Load all frames
        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            int frameIndex = startIndex + i;
            String fileName = fullPath + baseFileName + frameIndex + fileExtension;
            
            try {
                BufferedImage frame = resourceManager.loadImage(fileName);
                if (frame != null) {
                    frames.add(frame);
                } else {
                    System.err.println("Failed to load frame: " + fileName);
                }
            } catch (IOException e) {
                System.err.println("Error loading frame " + fileName + ": " + e.getMessage());
            }
        }
        
        // If no frames were loaded, return null
        if (frames.isEmpty()) {
            System.err.println("No frames loaded for animation: " + animationName);
            return null;
        }
        
        // Store frames in cache
        frameCache.put(animationName, frames);
        
        // Calculate scale factors
        double scaleX = scale;
        double scaleY = scale;
        
        // Create the sprite with adjustments
        AdjustableSequenceSprite sprite = new AdjustableSequenceSprite(
            animationName, 
            frames, 
            frameSize, 
            scaleX, 
            scaleY,
            offsetX,
            offsetY,
            duration, 
            looping
        );
        
        sprites.put(animationName, sprite);
        System.out.println(String.format(
            "Loaded sprite sequence: %s (%d frames, start: %d, ext: %s, scale: %.1f, offset: %d,%d)",
            animationName, frames.size(), startIndex, fileExtension, scale, offsetX, offsetY));
        
        return sprite;
    }
    
    /**
     * Gets a loaded sprite by name
     */
    public Sprite getSprite(String name) {
        Sprite sprite = sprites.get(name);
        if (sprite == null) {
            System.err.println("Warning: Sprite sequence not found: " + name);
        }
        return sprite;
    }
    
    /**
     * Check if a sprite with the given name exists
     */
    public boolean hasSprite(String name) {
        return sprites.containsKey(name);
    }
    
    /**
     * Clears all cached frames and sprites
     */
    public void clearCache() {
        sprites.clear();
        frameCache.clear();
    }
}