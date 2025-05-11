package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;

/**
 * Extended sprite class with proper looping control
 */
public class LoopingSprite extends ProperlyScaledSprite {
    private final boolean shouldLoop;
    private boolean hasCompleted = false;
    private int maxLoops = -1; // -1 for infinite loops
    private int currentLoops = 0;
    private int lastFrameIndex = -1;
    
    public LoopingSprite(String name, BufferedImage spriteSheet, Dimension frameSize,
                        double horizontalScale, double verticalScale,
                        int firstFrame, int totalFrames, Duration duration, boolean shouldLoop) {
        super(name, spriteSheet, frameSize, horizontalScale, verticalScale, firstFrame, totalFrames, duration);
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
        if (shouldLoop && lastFrameIndex != -1 && currentFrameIndex == 0 && lastFrameIndex != 0) {
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
    
    public boolean isLooping() {
        return shouldLoop;
    }
    
    public boolean hasCompleted() {
        return hasCompleted;
    }
    
    public void setMaxLoops(int maxLoops) {
        this.maxLoops = maxLoops;
    }
    
    public int getCurrentLoops() {
        return currentLoops;
    }
}