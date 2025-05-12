package game.entity;

/**
 * Base interface for all game entities
 */
public interface Entity {
    /**
     * Gets the X position
     */
    int getX();
    
    /**
     * Gets the Y position
     */
    int getY();
    
    /**
     * Sets the X position
     */
    void setX(int x);
    
    /**
     * Sets the Y position
     */
    void setY(int y);
    
    /**
     * Gets the width
     */
    int getWidth();
    
    /**
     * Gets the height
     */
    int getHeight();
    
    /**
     * Checks if the entity is visible
     */
    boolean isVisible();
}