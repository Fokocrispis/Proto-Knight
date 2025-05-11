package game.entity;

import java.awt.Color;
import java.awt.Graphics2D;

import game.Vector2D;
import game.physics.Collision;
import game.physics.PhysicsObject;

/**
 * A simple box entity with physics properties.
 */
public class BoxEntity extends AbstractEntity {
    private Color color;
    
    /**
     * Creates a new box entity.
     * 
     * @param x Initial X position
     * @param y Initial Y position
     * @param width Width of the box
     * @param height Height of the box
     * @param color Color of the box
     */
    public BoxEntity(double x, double y, int width, int height, Color color) {
        super(x, y, width, height);
        this.color = color;
    }
    
    /**
     * Creates a new box entity with a default color.
     * 
     * @param x Initial X position
     * @param y Initial Y position
     * @param width Width of the box
     * @param height Height of the box
     */
    public BoxEntity(double x, double y, int width, int height) {
        this(x, y, width, height, Color.WHITE);
    }
    
    @Override
    public void update(long deltaTime) {
        super.update(deltaTime);
        
        // Add any box-specific update logic here
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible) return;
        
        g.setColor(color);
        g.fillRect(
            (int)(position.getX() - width / 2),
            (int)(position.getY() - height / 2),
            width,
            height
        );
        
        // Optionally draw a border
        g.setColor(Color.BLACK);
        g.drawRect(
            (int)(position.getX() - width / 2),
            (int)(position.getY() - height / 2),
            width,
            height
        );
    }
    
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Handle collision, e.g., bounce, change color, play sound, etc.
        
        // For example, you could change color briefly on collision
        flashOnCollision();
    }
    
    /**
     * Temporarily changes the color of the box to indicate a collision.
     * In a real game, you might use a timer to restore the original color.
     */
    private void flashOnCollision() {
        // This is just a placeholder. In a real game, you'd likely use a timer
        // to change the color back after a short delay.
        // For now, we'll just lighten the color
        color = color.brighter();
    }
    
    /**
     * Gets the color of this box.
     * 
     * @return The color
     */
    public Color getColor() {
        return color;
    }
    
    /**
     * Sets the color of this box.
     * 
     * @param color The new color
     */
    public void setColor(Color color) {
        this.color = color;
    }
    
    /**
     * Applies a force to this box.
     * 
     * @param forceX X component of the force
     * @param forceY Y component of the force
     */
    public void applyForce(double forceX, double forceY) {
        // F = ma, so a = F/m
        double accelerationX = forceX / mass;
        double accelerationY = forceY / mass;
        
        // Apply acceleration to velocity
        velocity.add(accelerationX, accelerationY);
    }
    
    /**
     * Applies an impulse to this box.
     * An impulse is an instantaneous change in velocity.
     * 
     * @param impulseX X component of the impulse
     * @param impulseY Y component of the impulse
     */
    public void applyImpulse(double impulseX, double impulseY) {
        // Apply impulse directly to velocity
        velocity.add(impulseX / mass, impulseY / mass);
    }
}