package game.sprites;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;

/**
 * Sprite class with proper horizontal and vertical scaling
 */
public class ProperlyScaledSprite extends Sprite {
    private final double horizontalScale;
    private final double verticalScale;
    
    /**
     * Creates a new properly scaled sprite with separate horizontal and vertical scaling
     */
    public ProperlyScaledSprite(String name, BufferedImage spriteSheet, Dimension frameSize,
                               double horizontalScale, double verticalScale,
                               int firstFrame, int totalFrames, Duration duration) {
        super(name, spriteSheet, frameSize, 1.0, firstFrame, totalFrames, duration);
        this.horizontalScale = horizontalScale;
        this.verticalScale = verticalScale;
    }
    
    @Override
    public Dimension getSize() {
        // Use getFrameSize() instead of directly accessing frameSize
        Dimension frameSize = getFrameSize();
        return new Dimension(
            (int)(frameSize.width * horizontalScale),
            (int)(frameSize.height * verticalScale)
        );
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
}