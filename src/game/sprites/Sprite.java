package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;

/**
 * Updated Sprite class with getters for frame control
 */
public class Sprite {
    private final BufferedImage spritesheet;
    private final Dimension frameSize;
    private final double scale;
    private final int firstFrame;
    private final int totalFrames;
    private final double timePerFrame;
    private final Duration duration;
    private final String name;
    
    private long timeElapsed;
    private int frameIndex;
    private Offset offset;
    
    /**
     * Creates a new sprite with bounds checking
     */
    public Sprite(String name, BufferedImage spritesheet, Dimension frameSize, 
                  double scale, int firstFrame, int totalFrames, Duration duration) {
        this.name = name;
        this.spritesheet = spritesheet;
        this.frameSize = frameSize;
        this.scale = scale;
        this.firstFrame = firstFrame;
        this.totalFrames = totalFrames;
        this.duration = duration;
        this.timePerFrame = this.duration.toMillis() / this.totalFrames;
        
        this.timeElapsed = 0;
        this.frameIndex = 0;
        
        // Verify the sprite sheet can accommodate all frames
        validateSpritesheet();
        
        this.offset = calculateOffset();
    }
    
    /**
     * Validates that the spritesheet can accommodate all requested frames
     */
    private void validateSpritesheet() {
        int columns = getSpritesheetColumns();
        int totalFramesInSheet = (spritesheet.getWidth() / frameSize.width) * 
                                (spritesheet.getHeight() / frameSize.height);
        
        int lastFrameIndex = firstFrame + totalFrames - 1;
        
        if (lastFrameIndex >= totalFramesInSheet) {
            System.err.println("Warning: Sprite '" + name + "' requests frames beyond spritesheet bounds!");
            System.err.println("  Requested: " + (firstFrame + totalFrames) + " frames");
            System.err.println("  Available: " + totalFramesInSheet + " frames");
            System.err.println("  Spritesheet size: " + spritesheet.getWidth() + "x" + spritesheet.getHeight());
            System.err.println("  Frame size: " + frameSize.width + "x" + frameSize.height);
        }
    }
    
    /**
     * Gets the current frame with proper bounds checking
     */
    public BufferedImage getFrame() {
        try {
            // Calculate the offset with bounds checking
            this.offset = calculateOffset();
            
            // Verify the calculated bounds are valid
            if (!isValidFrame(offset)) {
                System.err.println("Invalid frame bounds for sprite '" + name + "' at frame " + frameIndex);
                return createErrorFrame();
            }
            
            return spritesheet.getSubimage(
                offset.getX(),
                offset.getY(),
                frameSize.width,
                frameSize.height
            );
        } catch (Exception e) {
            System.err.println("Error getting frame for sprite '" + name + "' at frame " + frameIndex + ": " + e.getMessage());
            return createErrorFrame();
        }
    }
    
    /**
     * Checks if the calculated frame bounds are valid
     */
    private boolean isValidFrame(Offset offset) {
        int x = offset.getX();
        int y = offset.getY();
        
        // Check if frame is within spritesheet bounds
        if (x < 0 || y < 0) return false;
        if (x + frameSize.width > spritesheet.getWidth()) return false;
        if (y + frameSize.height > spritesheet.getHeight()) return false;
        
        return true;
    }
    
    /**
     * Creates an error frame when the requested frame is invalid
     */
    private BufferedImage createErrorFrame() {
        BufferedImage errorFrame = new BufferedImage(frameSize.width, frameSize.height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = errorFrame.createGraphics();
        
        // Fill with pink to indicate error
        g.setColor(java.awt.Color.PINK);
        g.fillRect(0, 0, frameSize.width, frameSize.height);
        
        // Add error text
        g.setColor(java.awt.Color.BLACK);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 8));
        g.drawString("ERROR", frameSize.width / 4, frameSize.height / 2);
        
        g.dispose();
        return errorFrame;
    }
    
    /**
     * Gets the size of the sprite with scaling applied
     */
    public Dimension getSize() {
        return new Dimension(
            (int)(frameSize.width * scale),
            (int)(frameSize.height * scale)
        );
    }
    
    /**
     * Gets the frame size without scaling
     */
    public Dimension getFrameSize() {
        return new Dimension(frameSize.width, frameSize.height);
    }
    
    /**
     * Gets the scale factor
     */
    public double getScale() {
        return scale;
    }
    
    /**
     * Gets the current frame index
     */
    public int getFrameIndex() {
        return frameIndex;
    }
    
    /**
     * Gets the total number of frames
     */
    public int getTotalFrames() {
        return totalFrames;
    }
    
    /**
     * Gets the first frame index
     */
    public int getFirstFrame() {
        return firstFrame;
    }
    
    /**
     * Updates the sprite animation with frame bounds checking
     */
    public void update(long deltaTime) {
        timeElapsed += deltaTime;
        
        if (timeElapsed >= timePerFrame) {
            // Advance to next frame
            frameIndex = (frameIndex + 1) % totalFrames;
            timeElapsed -= timePerFrame;
            
            // Recalculate offset for new frame
            this.offset = calculateOffset();
        }
    }
    
    /**
     * Resets the sprite animation to the first frame
     */
    public void reset() {
        frameIndex = 0;
        timeElapsed = 0;
        offset = calculateOffset();
    }
    
    /**
     * Gets the duration of the animation
     */
    public Duration getDuration() {
        return duration;
    }
    
    /**
     * Gets the name of the sprite
     */
    public String getName() {
        return name;
    }
    
    /**
     * Calculates the offset of the current frame in the sprite sheet with bounds checking
     */
    private Offset calculateOffset() {
        int columns = getSpritesheetColumns();
        int currentFrame = firstFrame + frameIndex;
        
        // Ensure we don't exceed the available frames
        int totalPossibleFrames = (spritesheet.getWidth() / frameSize.width) * 
                                 (spritesheet.getHeight() / frameSize.height);
        
        if (currentFrame >= totalPossibleFrames) {
            System.err.println("Frame " + currentFrame + " exceeds available frames (" + totalPossibleFrames + ") for sprite '" + name + "'");
            currentFrame = totalPossibleFrames - 1; // Use last available frame
        }
        
        int x = (currentFrame % columns) * frameSize.width;
        int y = (currentFrame / columns) * frameSize.height;
        
        return new Offset(x, y);
    }
    
    /**
     * Gets the number of columns in the spritesheet
     */
    private int getSpritesheetColumns() {
        return spritesheet.getWidth() / frameSize.width;
    }
    
    /**
     * Gets debug information about the sprite
     */
    public String getDebugInfo() {
        return String.format("Sprite '%s': frame %d/%d, offset (%d,%d), size %dx%d", 
            name, frameIndex, totalFrames, offset.getX(), offset.getY(), 
            frameSize.width, frameSize.height);
    }
}

/**
 * Represents an offset in the sprite sheet
 */
class Offset {
    private final int x;
    private final int y;
    
    public Offset(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
}