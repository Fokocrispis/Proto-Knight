package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import game.resource.ResourceManager;

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
    public BackgroundLayer(String imageName, double scrollFactorX, double scrollFactorY) {
        try {
            this.image = ResourceManager.getInstance().loadImage(imageName);
            if (this.image != null) {
                this.width = image.getWidth();
                this.height = image.getHeight();
                System.out.println("Loaded background layer: " + imageName + " (" + width + "x" + height + ")");
            } else {
                System.err.println("Failed to load background image: " + imageName);
                // Use a default size if image fails to load
                this.width = 1280;
                this.height = 720;
            }
        } catch (Exception e) {
            System.err.println("Error loading background image: " + imageName);
            e.printStackTrace();
            this.width = 1280;
            this.height = 720;
        }
        
        this.scrollFactorX = scrollFactorX;
        this.scrollFactorY = scrollFactorY;
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
     * @param floorY Y position of the floor
     * @param backgroundWidth Background area width in pixels
     * @param backgroundHeight Background area height in pixels
     */
    public void renderWithCamera(Graphics2D g, double cameraX, double cameraY, int viewportWidth, int viewportHeight, 
                                float floorY, int backgroundWidth, int backgroundHeight) {
        // Calculate parallax scroll position
        double scrollX = cameraX * scrollFactorX;
        double scrollY = cameraY * scrollFactorY;
        
        // Calculate the background area (starting from floor, extending up 20m)
        int backgroundY = (int)(floorY - backgroundHeight);
        
        // Calculate initial tile positions
        int startX = (int) (scrollX % width);
        if (startX > 0) startX -= width;
        
        // For Y, we want to start from the background top
        int startY = backgroundY;
        
        // Draw the tiled background
        if (image != null) {
            // Draw image background only in the defined area
            for (int x = startX; x < backgroundWidth + width; x += width) {
                for (int y = startY; y < floorY + height; y += height) {
                    // Only draw if within the background area
                    if (y + height > backgroundY && y < floorY && x < backgroundWidth) {
                        g.drawImage(image, x, y, null);
                    }
                }
            }
        } else {
            // Draw solid color background only in the defined area
            g.setColor(color);
            g.fillRect(0, backgroundY, backgroundWidth, backgroundHeight);
        }
    }
    
    /**
     * Gets the scroll factor X.
     */
    public double getScrollFactorX() {
        return scrollFactorX;
    }
    
    /**
     * Gets the scroll factor Y.
     */
    public double getScrollFactorY() {
        return scrollFactorY;
    }
}