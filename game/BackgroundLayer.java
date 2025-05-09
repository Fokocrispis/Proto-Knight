// src/game/BackgroundLayer.java
package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Represents a scrolling background layer with parallax effect.
 */
public class BackgroundLayer implements GameObject {
    private BufferedImage image;
    private Color color;
    
    private double scrollFactorX;
    private double scrollFactorY;
    
    private int width;
    private int height;
    
    /**
     * Creates a new background layer with an image.
     */
    public BackgroundLayer(BufferedImage image, double scrollFactorX, double scrollFactorY) {
        this.image = image;
        this.scrollFactorX = scrollFactorX;
        this.scrollFactorY = scrollFactorY;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }
    
    /**
     * Creates a new background layer with a solid color.
     */
    public BackgroundLayer(Color color, int width, int height, double scrollFactorX, double scrollFactorY) {
        this.color = color;
        this.image = null;
        this.width = width;
        this.height = height;
        this.scrollFactorX = scrollFactorX;
        this.scrollFactorY = scrollFactorY;
    }
    
    @Override
    public void update(long deltaTime) {
        // Background layers don't need updates
    }
    
    @Override
    public void render(Graphics2D g) {
        // This is implemented in renderWithCamera
    }
    
    /**
     * Renders the background layer with parallax scrolling.
     * 
     * @param g The graphics context
     * @param cameraX Camera X position
     * @param cameraY Camera Y position
     * @param viewportWidth Viewport width
     * @param viewportHeight Viewport height
     */
    public void renderWithCamera(Graphics2D g, double cameraX, double cameraY, int viewportWidth, int viewportHeight) {
        // Calculate parallax scroll position
        double scrollX = cameraX * scrollFactorX;
        double scrollY = cameraY * scrollFactorY;
        
        // Calculate initial tile positions
        int startX = (int) (scrollX % width);
        if (startX > 0) startX -= width;
        
        int startY = (int) (scrollY % height);
        if (startY > 0) startY -= height;
        
        // Draw the tiled background
        if (image != null) {
            // Draw image background
            for (int x = startX; x < viewportWidth; x += width) {
                for (int y = startY; y < viewportHeight; y += height) {
                    g.drawImage(image, x, y, null);
                }
            }
        } else {
            // Draw solid color background
            g.setColor(color);
            g.fillRect(0, 0, viewportWidth, viewportHeight);
        }
    }
}