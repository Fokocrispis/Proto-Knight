package game.sprites;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprite class that supports per-frame offsets
 */
public class FrameOffsetSequenceSprite extends Sprite {
    protected final Point[] frameOffsets;
    protected final List<BufferedImage> frames;
    protected final Dimension frameSize;
    protected final double scaleX;
    protected final double scaleY;
    protected final Duration duration;
    protected final int offsetX;
    protected final int offsetY;
    protected long timeElapsed;
    protected int frameIndex;
    
    /**
     * Creates a new sprite with uniform scale for X and Y
     */
    public FrameOffsetSequenceSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double scale,
            Point[] frameOffsets,
            Duration duration,
            boolean looping) {
        
        this(name, frames, frameSize, scale, scale, 0, 0, duration, looping);
    }
    
    /**
     * Creates a new sprite with separate X and Y scales and global offset
     */
    public FrameOffsetSequenceSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double scaleX,
            double scaleY,
            int offsetX,
            int offsetY,
            Duration duration,
            boolean looping) {
        
        super(name, frames.isEmpty() ? null : frames.get(0), 
              frameSize, Math.max(scaleX, scaleY), 0, frames.size(), duration);
        
        this.frames = new ArrayList<>(frames);
        this.frameSize = new Dimension(frameSize);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        
        // Create array of offsets with the global offset applied
        this.frameOffsets = new Point[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            this.frameOffsets[i] = new Point(offsetX, offsetY);
        }
        
        this.duration = duration;
        this.timeElapsed = 0;
        this.frameIndex = 0;
    }
    
    /**
     * Creates a new sprite with per-frame offsets
     */
    public FrameOffsetSequenceSprite(
            String name,
            List<BufferedImage> frames,
            Dimension frameSize,
            double scaleX,
            double scaleY,
            Point[] frameOffsets,
            Duration duration,
            boolean looping) {
        
        super(name, frames.isEmpty() ? null : frames.get(0), 
              frameSize, Math.max(scaleX, scaleY), 0, frames.size(), duration);
        
        this.frames = new ArrayList<>(frames);
        this.frameSize = new Dimension(frameSize);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = 0;
        this.offsetY = 0;
        this.frameOffsets = frameOffsets;
        this.duration = duration;
        this.timeElapsed = 0;
        this.frameIndex = 0;
    }
    
    /**
     * Gets the size of the sprite with scaling applied
     */
    @Override
    public Dimension getSize() {
        return new Dimension(
            (int)(frameSize.width * scaleX),
            (int)(frameSize.height * scaleY)
        );
    }
    
    /**
     * Gets the current frame
     */
    @Override
    public BufferedImage getFrame() {
        if (frames.isEmpty()) {
            return createErrorFrame();
        }
        
        if (frameIndex < 0 || frameIndex >= frames.size()) {
            frameIndex = 0;
        }
        
        return frames.get(frameIndex);
    }
    
    /**
     * Gets the offset for the current frame
     */
    public Point getCurrentFrameOffset() {
        if (frameIndex >= 0 && frameIndex < frameOffsets.length) {
            return frameOffsets[frameIndex];
        }
        return new Point(offsetX, offsetY); // Fall back to global offset
    }
    
    /**
     * Gets the properly centered render position for X coordinate with current frame offset
     */
    public int getRenderX(double entityX) {
        Point offset = getCurrentFrameOffset();
        return (int)(entityX - getSize().width / 2.0) + offset.x;
    }
    
    /**
     * Gets the properly centered render position for Y coordinate with current frame offset
     */
    public int getRenderY(double entityY, int collisionHeight) {
        Point offset = getCurrentFrameOffset();
        // Align sprite bottom with collision bottom
        int spriteBottom = (int)(entityY + collisionHeight / 2.0);
        return spriteBottom - getSize().height + offset.y;
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
     * Gets the frame index
     */
    @Override
    public int getFrameIndex() {
        return frameIndex;
    }
    
    /**
     * Gets the total number of frames
     */
    @Override
    public int getTotalFrames() {
        return frames.size();
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
}