package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import game.resource.ResourceManager;

/**
 * Manager for sprite sequences loaded from individual image files.
 * Updated to work with the new character folder structure.
 */
public class SpriteSequenceManager {
    // Cache for loaded sprites and frames
    private final Map<String, Sprite> sprites = new HashMap<>();
    private final Map<String, List<BufferedImage>> frameCache = new HashMap<>();
    private final ResourceManager resourceManager;
    
    public SpriteSequenceManager() {
        this.resourceManager = ResourceManager.getInstance();
    }
    
    /**
     * Creates player sprites from animation sequences
     */
    public void createPlayerSprites() {
        // Define standard frame size and scale
        Dimension frameSize = new Dimension(64, 64);
        Dimension displaySize = new Dimension(192, 192);
        double scale = 3.0;
        
        // Load basic player animations
        loadSpriteSequence(
            "player_idle",
            "Sprites/Joanna/Idle",
            "Idle",
            6,
            frameSize,
            scale,
            Duration.ofMillis(1000),
            true
        );
        
        loadSpriteSequence(
            "player_run",
            "Sprites/Joanna/Running",
            "Running",
            8,
            frameSize,
            scale,
            Duration.ofMillis(800),
            true
        );
        
        loadSpriteSequence(
            "player_to_run",
            "Sprites/Joanna/ToRun",
            "ToRun",
            3,
            frameSize,
            scale,
            Duration.ofMillis(300),
            false
        );
        
        loadSpriteSequence(
            "player_light_attack",
            "Sprites/Joanna/LightAtk",
            "LightAtk",
            12,
            frameSize,
            scale,
            Duration.ofMillis(500),
            false
        );
        
        loadSpriteSequence(
            "player_dash",
            "Sprites/Joanna/Dashing",
            "Dashing",
            3,
            frameSize,
            scale,
            Duration.ofMillis(200),
            false
        );
        
        loadSpriteSequence(
            "player_break_run",
            "Sprites/Joanna/BreakRun",
            "BreakRun",
            7,
            frameSize,
            scale,
            Duration.ofMillis(400),
            false
        );
        
        loadSpriteSequence(
            "player_land",
            "Sprites/Joanna/Land",
            "Land",
            5,
            frameSize,
            scale,
            Duration.ofMillis(300),
            false
        );
    }
    
    /**
     * Loads a sprite sequence from a folder of image files
     * 
     * @param name Animation name
     * @param path Path to the animation folder
     * @param framePrefix Prefix for frame filenames
     * @param frameCount Total frame count
     * @param frameSize Size of each frame
     * @param scale Rendering scale
     * @param duration Animation duration
     * @param looping Whether the animation should loop
     * @return The loaded sprite
     */
    public Sprite loadSpriteSequence(
            String name,
            String path,
            String framePrefix,
            int frameCount,
            Dimension frameSize,
            double scale,
            Duration duration,
            boolean looping) {
        
        // Create display size based on uniform scale
        Dimension displaySize = new Dimension(
            (int)(frameSize.width * scale),
            (int)(frameSize.height * scale)
        );
        
        // Use the more detailed method with default offsets (0,0)
        return loadSpriteSequence(
            name,
            path,
            framePrefix,
            frameCount,
            frameSize,
            displaySize,
            0,
            0,
            duration,
            looping
        );
    }
    
    /**
     * Loads a sprite sequence with position and size adjustments
     */
    public Sprite loadSpriteSequence(
            String name,
            String path,
            String framePrefix,
            int frameCount,
            Dimension frameSize,
            Dimension displaySize,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean looping) {
        
        // Check if already loaded
        if (sprites.containsKey(name)) {
            return sprites.get(name);
        }
        
        // Make sure path ends with slash
        if (!path.endsWith("/")) {
            path += "/";
        }
        
        // Load all frames
        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 1; i <= frameCount; i++) { // Start from 1 for frame numbering
            String fileName = path + framePrefix + i + ".png";
            
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
        
        // If no frames were loaded, try alternative naming conventions
        if (frames.isEmpty()) {
            System.out.println("Trying alternative naming patterns for: " + name);
            frames = tryAlternativeNamingPatterns(path, framePrefix, frameCount);
        }
        
        // If still no frames, return null
        if (frames.isEmpty()) {
            System.err.println("No frames loaded for animation: " + name + " at path: " + path);
            return null;
        }
        
        // Store frames in cache
        frameCache.put(name, frames);
        
        // Calculate scale factors based on desired display size
        double scaleX = (double)displaySize.width / frameSize.width;
        double scaleY = (double)displaySize.height / frameSize.height;
        
        // Create the appropriate sprite type
        Sprite sprite;
        if (looping) {
            sprite = new LoopingSprite(name, frames, frameSize, scaleX, scaleY, offsetX, offsetY, duration, true);
        } else {
            sprite = new AdjustableSequenceSprite(name, frames, frameSize, scaleX, scaleY, offsetX, offsetY, duration, false);
        }
        
        sprites.put(name, sprite);
        return sprite;
    }
    
    /**
     * Tries multiple naming patterns to load frames
     */
    private List<BufferedImage> tryAlternativeNamingPatterns(String path, String framePrefix, int frameCount) {
        List<BufferedImage> frames = new ArrayList<>();
        
        String[][] patterns = {
            // Try frame_01.png format
            {"frame_%02d.png", ""},
            // Try prefix_1.png format (no padding)
            {"%s_%d.png", framePrefix},
            // Try prefix01.png format
            {"%s%02d.png", framePrefix},
            // Try just numbers: 1.png
            {"%d.png", ""},
            // Try frame-1.png format
            {"frame-%d.png", ""},
            // Try lowercase prefix
            {"%s%d.png", framePrefix.toLowerCase()}
        };
        
        for (String[] pattern : patterns) {
            frames.clear();
            String format = pattern[0];
            String prefix = pattern[1];
            
            for (int i = 1; i <= frameCount; i++) {
                String fileName = path;
                
                try {
                    if (prefix.isEmpty()) {
                        fileName += String.format(format, i);
                    } else {
                        fileName += String.format(format, prefix, i);
                    }
                    
                    BufferedImage frame = resourceManager.loadImage(fileName);
                    if (frame != null) {
                        frames.add(frame);
                    } else {
                        // Frame failed to load, break out of this pattern
                        frames.clear();
                        break;
                    }
                } catch (Exception e) {
                    // Silently continue to next pattern
                    frames.clear();
                    break;
                }
            }
            
            // If all frames loaded successfully, return them
            if (frames.size() == frameCount) {
                System.out.println("Successfully loaded frames using pattern: " + format);
                return frames;
            }
        }
        
        return frames;
    }
    
    /**
     * Gets a sprite by name
     */
    public Sprite getSprite(String name) {
        return sprites.get(name);
    }
    
    /**
     * Checks if a sprite exists
     */
    public boolean hasSprite(String name) {
        return sprites.containsKey(name);
    }
    
    /**
     * Clears the cache
     */
    public void clearCache() {
        sprites.clear();
        frameCache.clear();
    }
}