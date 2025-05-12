package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;

/**
 * A sprite implementation that uses a sequence of individual images
 * instead of a sprite sheet for animation frames.
 */
public class SequenceSprite extends Sprite {
    private final List<BufferedImage> frames;
    private final String name;
    private final double scale;
    private final Duration duration;
    private final Dimension frameSize;
    
    private long timeElapsed;
    private int frameIndex;
    
    /**
     * Creates a new sequence sprite with the given frames
     */
    public SequenceSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double scale,
            Duration duration) {
        
        // Call parent constructor with a dummy sprite sheet (first frame)
        // We'll override the getFrame method to use our frame list instead
        super(name, frames.isEmpty() ? null : frames.get(0), 
              frameSize, scale, 0, frames.size(), duration);
        
        this.name = name;
        this.frames = frames;
        this.scale = scale;
        this.duration = duration;
        this.frameSize = frameSize;
        this.timeElapsed = 0;
        this.frameIndex = 0;
    }
    
    /**
     * Gets the current frame directly from the frame list
     */
    @Override
    public BufferedImage getFrame() {
        if (frames.isEmpty()) {
            return createErrorFrame();
        }
        
        // Ensure frame index is valid
        if (frameIndex < 0 || frameIndex >= frames.size()) {
            System.err.println("Invalid frame index for sprite '" + name + "': " + frameIndex);
            frameIndex = 0;
        }
        
        return frames.get(frameIndex);
    }
    
    /**
     * Updates the animation state
     */
    @Override
    public void update(long deltaTime) {
        if (frames.size() <= 1) return;
        
        timeElapsed += deltaTime;
        double timePerFrame = (double) duration.toMillis() / frames.size();
        
        if (timeElapsed >= timePerFrame) {
            // Advance to next frame
            frameIndex = (frameIndex + 1) % frames.size();
            timeElapsed -= timePerFrame;
        }
    }
    
    /**
     * Resets the animation to the first frame
     */
    @Override
    public void reset() {
        frameIndex = 0;
        timeElapsed = 0;
    }
    
    /**
     * Creates an error frame for when loading fails
     */
    private BufferedImage createErrorFrame() {
        BufferedImage errorFrame = new BufferedImage(
                frameSize.width, frameSize.height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = errorFrame.createGraphics();
        
        // Fill with pink to indicate error
        g.setColor(java.awt.Color.PINK);
        g.fillRect(0, 0, frameSize.width, frameSize.height);
        
        // Add error text
        g.setColor(java.awt.Color.BLACK);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 8));
        String errorText = "ERROR";
        g.drawString(errorText, frameSize.width / 4, frameSize.height / 2);
        
        g.dispose();
        return errorFrame;
    }
    
    /**
     * Gets the number of frames in this sprite
     */
    @Override
    public int getTotalFrames() {
        return frames.size();
    }
    
    /**
     * Gets the current frame index
     */
    @Override
    public int getFrameIndex() {
        return frameIndex;
    }
    
    /**
     * Gets the name of this sprite
     */
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Gets debug information about the sprite
     */
    @Override
    public String getDebugInfo() {
        return String.format("SequenceSprite '%s': frame %d/%d, duration: %d ms", 
            name, frameIndex + 1, frames.size(), duration.toMillis());
    }
}