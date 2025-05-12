package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;

/**
 * An extended version of SequenceSprite with looping control.
 * Allows defining whether the animation should loop and how many times.
 */
public class SequenceLoopingSprite extends SequenceSprite {
    private final boolean shouldLoop;
    private boolean hasCompleted = false;
    private int maxLoops = -1; // -1 for infinite loops
    private int currentLoops = 0;
    private int lastFrameIndex = -1;
    private final double horizontalScale;
    private final double verticalScale;
    
    /**
     * Creates a new looping sequence sprite
     */
    public SequenceLoopingSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double horizontalScale,
            double verticalScale,
            Duration duration,
            boolean shouldLoop) {
        
        super(name, frames, frameSize, horizontalScale, duration);
        this.shouldLoop = shouldLoop;
        this.horizontalScale = horizontalScale;
        this.verticalScale = verticalScale;
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
     * Gets the horizontal scale factor
     */
    public double getHorizontalScale() {
        return horizontalScale;
    }
    
    /**
     * Gets the vertical scale factor
     */
    public double getVerticalScale() {
        return verticalScale;
    }
    
    /**
     * Gets the properly centered render position for X coordinate
     */
    public int getRenderX(double entityX) {
        return (int)(entityX - getSize().width / 2.0);
    }
    
    /**
     * Gets the properly centered render position for Y coordinate
     * Aligns sprite bottom with collision bottom for proper grounding
     */
    public int getRenderY(double entityY, int collisionHeight) {
        // Align sprite bottom with collision bottom
        int spriteBottom = (int)(entityY + collisionHeight / 2.0);
        return spriteBottom - getSize().height;
    }
}