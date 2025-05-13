package game.sprites;

import java.awt.Dimension;
import java.awt.Point;
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
 * Enhanced to support per-frame offsets and dimensions.
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
        double scale = 3.0;
        
        // Load basic player animations with standard parameters
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
        
        // Example with per-frame offsets for running animation
        Point[] runOffsets = new Point[8];
        // Customize each frame offset for the run animation
        runOffsets[0] = new Point(0, 0);    // Frame 1
        runOffsets[1] = new Point(2, -1);   // Frame 2
        runOffsets[2] = new Point(4, -2);   // Frame 3
        runOffsets[3] = new Point(5, -1);   // Frame 4
        runOffsets[4] = new Point(3, 0);    // Frame 5
        runOffsets[5] = new Point(2, 1);    // Frame 6
        runOffsets[6] = new Point(0, 2);    // Frame 7
        runOffsets[7] = new Point(-2, 1);   // Frame 8
        
        // Load with per-frame offsets
        loadSpriteSequenceWithFrameOffsets(
            "player_run",
            "Sprites/Joanna/Running",
            "Running",
            8,
            frameSize,
            scale,
            runOffsets,
            Duration.ofMillis(800),
            true
        );
        
        // Example with custom dimensions
        Dimension walkingDisplaySize = new Dimension(192, 192);
        loadSpriteSequence(
            "player_walk",
            "Sprites/Joanna/Walk",
            "Walking",
            18,
            frameSize,
            walkingDisplaySize, 
            0,  // offsetX
            10, // offsetY
            Duration.ofMillis(850),
            true
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
        
        // Use the method with specific display size and default offsets
        return loadSpriteSequence(
            name,
            path,
            framePrefix,
            frameCount,
            frameSize,
            displaySize,
            0,  // Default X offset
            0,  // Default Y offset
            duration,
            looping
        );
    }
    
    /**
     * Loads a sprite sequence with custom display size and offsets.
     * 
     * @param name Animation name
     * @param path Path to the animation folder
     * @param framePrefix Prefix for frame filenames
     * @param frameCount Total frame count
     * @param frameSize Size of each frame
     * @param displaySize Desired display size (after scaling)
     * @param offsetX Horizontal position offset
     * @param offsetY Vertical position offset
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
            Dimension displaySize,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean looping) {
        
        // Check if already loaded
        if (sprites.containsKey(name)) {
            return sprites.get(name);
        }
        
        // Calculate scale factors from display size
        double scaleX = (double)displaySize.width / frameSize.width;
        double scaleY = (double)displaySize.height / frameSize.height;
        
        // Create uniform offsets for all frames
        Point[] offsets = new Point[frameCount];
        for (int i = 0; i < frameCount; i++) {
            offsets[i] = new Point(offsetX, offsetY);
        }
        
        // Load frames and create sprite
        List<BufferedImage> frames = loadFrames(path, framePrefix, frameCount);
        
        // If no frames, return null
        if (frames.isEmpty()) {
            return null;
        }
        
        // Store frames in cache
        frameCache.put(name, frames);
        
        // Create the appropriate sprite type
        Sprite sprite;
        if (looping) {
            sprite = new FrameOffsetLoopingSprite(
                name, frames, frameSize, scaleX, scaleY, offsetX, offsetY, duration, true);
        } else {
            sprite = new FrameOffsetSequenceSprite(
                name, frames, frameSize, scaleX, scaleY, offsetX, offsetY, duration, false);
        }
        
        sprites.put(name, sprite);
        return sprite;
    }
    
    /**
     * Loads a sprite sequence with custom offsets for each frame.
     * 
     * @param name Animation name
     * @param path Path to the animation folder
     * @param framePrefix Prefix for frame filenames
     * @param frameCount Total frame count
     * @param frameSize Size of each frame
     * @param scale Rendering scale
     * @param frameOffsets Array of Point objects containing (x,y) offsets for each frame
     * @param duration Animation duration
     * @param looping Whether the animation should loop
     * @return The loaded sprite
     */
    public Sprite loadSpriteSequenceWithFrameOffsets(
            String name,
            String path,
            String framePrefix,
            int frameCount,
            Dimension frameSize,
            double scale,
            Point[] frameOffsets,
            Duration duration,
            boolean looping) {
        
        // Check if already loaded
        if (sprites.containsKey(name)) {
            return sprites.get(name);
        }
        
        // Verify frameOffsets is the right size
        if (frameOffsets.length != frameCount) {
            System.err.println("Warning: Frame offsets array size (" + frameOffsets.length + 
                              ") doesn't match frame count (" + frameCount + ") for animation: " + name);
            
            // Create new array with correct size
            Point[] adjustedOffsets = new Point[frameCount];
            for (int i = 0; i < frameCount; i++) {
                if (i < frameOffsets.length) {
                    adjustedOffsets[i] = frameOffsets[i];
                } else {
                    adjustedOffsets[i] = new Point(0, 0);
                }
            }
            frameOffsets = adjustedOffsets;
        }
        
        // Load frames
        List<BufferedImage> frames = loadFrames(path, framePrefix, frameCount);
        
        // If no frames, return null
        if (frames.isEmpty()) {
            return null;
        }
        
        // Store frames in cache
        frameCache.put(name, frames);
        
        // Create the appropriate sprite type with per-frame offsets
        Sprite sprite;
        if (looping) {
            sprite = new FrameOffsetLoopingSprite(
                name, frames, frameSize, scale, frameOffsets, duration, true);
        } else {
            sprite = new FrameOffsetSequenceSprite(
                name, frames, frameSize, scale, frameOffsets, duration, false);
        }
        
        sprites.put(name, sprite);
        return sprite;
    }
    
    /**
     * Loads frames from the file system
     */
    private List<BufferedImage> loadFrames(String path, String framePrefix, int frameCount) {
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
        
        // If no frames were loaded, try alternative naming patterns
        if (frames.isEmpty()) {
            System.out.println("Trying alternative naming patterns for: " + path);
            frames = tryAlternativeNamingPatterns(path, framePrefix, frameCount);
        }
        
        // If still no frames, return empty list
        if (frames.isEmpty()) {
            System.err.println("No frames loaded for animation at path: " + path);
        }
        
        return frames;
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