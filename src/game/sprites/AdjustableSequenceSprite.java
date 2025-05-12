package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;

/**
 * A sequence sprite implementation that supports custom position offsets 
 * and different horizontal/vertical scaling.
 */
public class AdjustableSequenceSprite extends SequenceSprite {
    private final double scaleX;
    private final double scaleY;
    private final int offsetX;
    private final int offsetY;
    private final boolean shouldLoop;
    private boolean hasCompleted = false;
    private int maxLoops = -1; // -1 for infinite loops
    private int currentLoops = 0;
    private int lastFrameIndex = -1;
    
    /**
     * Creates a new adjustable sequence sprite
     */
    public AdjustableSequenceSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double scaleX,
            double scaleY,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean shouldLoop) {
        
        super(name, frames, frameSize, Math.max(scaleX, scaleY), duration);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.shouldLoop = shouldLoop;
    }
    
    @Override
    public void update(long deltaTime) {
        if (!shouldLoop && hasCompleted) {
            return; // Don't update if non-looping and completed
        }
        
        // Store current frame index before update
        int currentFrameIndex = getFrameIndex();
        
        super.update(deltaTime);
        
        // Check if we've completed a loop by detecting when we cycle back to frame 0
        if (shouldLoop && lastFrameIndex != -1 && 
            currentFrameIndex == 0 && lastFrameIndex != 0) {
            
            if (maxLoops > 0) {
                currentLoops++;
                if (currentLoops >= maxLoops) {
                    hasCompleted = true;
                }
            }
        }
        
        // Check if we've completed a non-looping animation
        if (!shouldLoop && isAtLastFrame()) {
            hasCompleted = true;
        }
        
        lastFrameIndex = currentFrameIndex;
    }
    
    @Override
    public void reset() {
        super.reset();
        hasCompleted = false;
        currentLoops = 0;
        lastFrameIndex = -1;
    }
    
    /**
     * Checks if the sprite is at the last frame
     */
    private boolean isAtLastFrame() {
        return getFrameIndex() == (getTotalFrames() - 1);
    }
    
    /**
     * Returns whether this sprite is configured to loop
     */
    public boolean isLooping() {
        return shouldLoop;
    }
    
    /**
     * Returns whether this sprite has completed its animation
     */
    public boolean hasCompleted() {
        return hasCompleted;
    }
    
    /**
     * Sets the maximum number of loops
     * @param maxLoops Number of loops, or -1 for infinite
     */
    public void setMaxLoops(int maxLoops) {
        this.maxLoops = maxLoops;
    }
    
    /**
     * Gets the current loop count
     */
    public int getCurrentLoops() {
        return currentLoops;
    }
    
    /**
     * Gets the size of the sprite with scaling applied
     */
    @Override
    public Dimension getSize() {
        Dimension frameSize = getFrameSize();
        return new Dimension(
            (int)(frameSize.width * scaleX),
            (int)(frameSize.height * scaleY)
        );
    }
    
    /**
     * Gets the horizontal scale factor
     */
    public double getScaleX() {
        return scaleX;
    }
    
    /**
     * Gets the vertical scale factor
     */
    public double getScaleY() {
        return scaleY;
    }
    
    /**
     * Gets the X offset for positioning
     */
    public int getOffsetX() {
        return offsetX;
    }
    
    /**
     * Gets the Y offset for positioning
     */
    public int getOffsetY() {
        return offsetY;
    }
    
    /**
     * Gets the properly centered render position for X coordinate
     * with custom offset applied
     */
    public int getRenderX(double entityX) {
        return (int)(entityX - getSize().width / 2.0) + offsetX;
    }
    
    /**
     * Gets the properly centered render position for Y coordinate
     * with custom offset applied
     */
    public int getRenderY(double entityY, int collisionHeight) {
        // Align sprite bottom with collision bottom
        int spriteBottom = (int)(entityY + collisionHeight / 2.0);
        return spriteBottom - getSize().height + offsetY;
    }
}