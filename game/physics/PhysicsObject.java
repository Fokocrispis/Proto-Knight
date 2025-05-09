package game.physics;

import game.GameObject;
import game.Vector2D;

/**
 * Interface for objects that can interact with the physics system.
 */
public interface PhysicsObject extends GameObject {
    /**
     * Gets the collision bounds for this object.
     * 
     * @return The collision shape.
     */
    CollisionShape getCollisionShape();
    
    /**
     * Gets the position of this object.
     * 
     * @return The position vector.
     */
    Vector2D getPosition();
    
    /**
     * Gets the velocity of this object.
     * 
     * @return The velocity vector.
     */
    Vector2D getVelocity();
    
    /**
     * Sets the velocity of this object.
     * 
     * @param velocity The new velocity.
     */
    void setVelocity(Vector2D velocity);
    
    /**
     * Gets the mass of this object.
     * 
     * @return The mass.
     */
    float getMass();
    
    /**
     * Checks if this object is affected by gravity.
     * 
     * @return True if affected by gravity, false otherwise.
     */
    boolean isAffectedByGravity();
    
    /**
     * Checks if this object can collide with others.
     * 
     * @return True if can collide, false otherwise.
     */
    boolean isCollidable();
    
    /**
     * Called when this object collides with another.
     * 
     * @param other The other object.
     * @param collision The collision information.
     */
    void onCollision(PhysicsObject other, Collision collision);
}