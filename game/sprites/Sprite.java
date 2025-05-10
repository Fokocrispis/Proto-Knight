package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;

/**
 * Represents an animated sprite with multiple frames.
 */
public class Sprite {
    private final BufferedImage spriteSheet;
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
     * Creates a new sprite.
     * 
     * @param name The name of the sprite
     * @param spriteSheet The sprite sheet image
     * @param frameSize The size of each frame
     * @param scale The scale factor
     * @param firstFrame The index of the first frame
     * @param totalFrames The total number of frames
     * @param duration The duration of the animation
     */
    public Sprite(String name, BufferedImage spriteSheet, Dimension frameSize, 
                  double scale, int firstFrame, int totalFrames, Duration duration) {
        this.name = name;
        this.spriteSheet = spriteSheet;
        this.frameSize = frameSize;
        this.scale = scale;
        this.firstFrame = firstFrame;
        this.totalFrames = totalFrames;
        this.duration = duration;
        this.timePerFrame = this.duration.toMillis() / this.totalFrames;
        
        this.timeElapsed = 0;
        this.frameIndex = 0;
        this.offset = calculateOffset();
    }
    
    /**
     * Gets the current frame of the sprite.
     */
    public BufferedImage getFrame() {
        return spriteSheet.getSubimage(
            offset.getX(),
            offset.getY(),
            frameSize.width,
            frameSize.height
        );
    }
    
    /**
     * Gets the size of the sprite with scaling applied.
     */
    public Dimension getSize() {
        return new Dimension(
            (int)(frameSize.width * scale),
            (int)(frameSize.height * scale)
        );
    }
    
    /**
     * Updates the sprite animation.
     * 
     * @param deltaTime Time elapsed since last update in milliseconds
     */
    public void update(long deltaTime) {
        timeElapsed += deltaTime;
        
        if (timeElapsed >= timePerFrame) {
            frameIndex = (frameIndex + 1) % totalFrames;
            timeElapsed -= timePerFrame;
            offset = calculateOffset();
        }
    }
    
    /**
     * Resets the sprite animation to the first frame.
     */
    public void reset() {
        frameIndex = 0;
        timeElapsed = 0;
        offset = calculateOffset();
    }
    
    /**
     * Gets the duration of the animation.
     */
    public Duration getDuration() {
        return duration;
    }
    
    /**
     * Gets the name of the sprite.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Calculates the offset of the current frame in the sprite sheet.
     */
    private Offset calculateOffset() {
        int columns = spriteSheet.getWidth() / frameSize.width;
        int currentFrame = firstFrame + frameIndex;
        
        int x = (currentFrame % columns) * frameSize.width;
        int y = (currentFrame / columns) * frameSize.height;
        
        return new Offset(x, y);
    }
}

/**
 * Represents an offset in the sprite sheet.
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