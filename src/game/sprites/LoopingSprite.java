package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;

/**
 * Extended sprite class with proper looping control
 */
public class LoopingSprite extends AdjustableSequenceSprite {
    private final boolean shouldLoop;
    private boolean hasCompleted = false;
    private int maxLoops = -1; // -1 for infinite loops
    private int currentLoops = 0;
    private int lastFrameIndex = -1;
    
    /**
     * Creates a new looping sprite with adjustable position and scaling
     */
    public LoopingSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double scaleX,
            double scaleY,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean shouldLoop) {
        
        super(name, frames, frameSize, scaleX, scaleY, offsetX, offsetY, duration, shouldLoop);
        this.shouldLoop = shouldLoop;
    }
    
    /**
     * Creates a simplified looping sprite with uniform scaling
     */
    public LoopingSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double scale,
            Duration duration,
            boolean shouldLoop) {
        
        this(name, frames, frameSize, scale, scale, 0, 0, duration, shouldLoop);
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
    
    @Override
    public boolean isLooping() {
        return shouldLoop;
    }
    
    @Override
    public boolean hasCompleted() {
        return hasCompleted;
    }
    
    @Override
    public void setMaxLoops(int maxLoops) {
        this.maxLoops = maxLoops;
    }
    
    @Override
    public int getCurrentLoops() {
        return currentLoops;
    }
}