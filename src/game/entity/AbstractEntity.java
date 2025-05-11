package game.entity;

import java.awt.Graphics2D;

import game.GameObject;
import game.Vector2D;
import game.physics.AABB;
import game.physics.Collision;
import game.physics.CollisionShape;
import game.physics.PhysicsObject;

/**
 * Base class for all game entities with position, size, and optional physics.
 */
public abstract class AbstractEntity implements GameObject, PhysicsObject {
    // Position and size
    protected Vector2D position;
    protected Vector2D velocity;
    protected static int maxSpeedY = 1000;
    protected int width;
    protected int height;
    
    // Physics properties
    protected float mass;
    protected boolean affectedByGravity;
    protected boolean collidable;
    protected CollisionShape collisionShape;
    
    // Entity state
    protected boolean active = true;
    protected boolean visible = true;
    
    // Entity tag for identification
    protected String tag = "";
    
    /**
     * Creates a new entity.
     * 
     * @param x Initial X position
     * @param y Initial Y position
     * @param width Width of the entity
     * @param height Height of the entity
     */
    public AbstractEntity(double x, double y, int width, int height) {
        this.position = new Vector2D(x, y);
        this.velocity = new Vector2D(0, 0);
        this.width = width;
        this.height = height;
        
        // Default physics properties
        this.mass = 1.0f;
        this.affectedByGravity = true;
        this.collidable = true;
        
        // Create a default AABB collision shape
        this.collisionShape = new AABB(position, width, height);
    }
    
    @Override
    public void update(long deltaTime) {
        // Update collision shape position
        collisionShape.setPosition(position);
    }
    
    @Override
    public void render(Graphics2D g) {
        // Base implementation does nothing, subclasses should override
    }
    
    @Override
    public CollisionShape getCollisionShape() {
        return collisionShape;
    }
    
    @Override
    public Vector2D getPosition() {
        return position;
    }
    
    @Override
    public Vector2D getVelocity() {
        return velocity;
    }
    
    @Override
    public void setVelocity(Vector2D velocity) {
    	if(velocity.getY()>maxSpeedY) {
    		this.velocity= new Vector2D(velocity.getX(), maxSpeedY);
    	}
    	else {
    		this.velocity = velocity;
    	}
    }
    
    @Override
    public float getMass() {
        return mass;
    }
    
    @Override
    public boolean isAffectedByGravity() {
        return affectedByGravity;
    }
    
    @Override
    public boolean isCollidable() {
        return collidable;
    }
    
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Base implementation does nothing, subclasses should override
    }
    
    /**
     * Sets the position of this entity.
     * 
     * @param x The new X position
     * @param y The new Y position
     */
    public void setPosition(double x, double y) {
        this.position.set(x, y);
    }
    
    /**
     * Sets the position of this entity.
     * 
     * @param position The new position
     */
    public void setPosition(Vector2D position) {
        this.position.set(position);
    }
    
    /**
     * Sets whether this entity is affected by gravity.
     * 
     * @param affectedByGravity True if affected by gravity, false otherwise
     */
    public void setAffectedByGravity(boolean affectedByGravity) {
        this.affectedByGravity = affectedByGravity;
    }
    
    /**
     * Sets whether this entity can collide with others.
     * 
     * @param collidable True if can collide, false otherwise
     */
    public void setCollidable(boolean collidable) {
        this.collidable = collidable;
    }
    
    /**
     * Sets the collision shape for this entity.
     * 
     * @param shape The new collision shape
     */
    public void setCollisionShape(CollisionShape shape) {
        this.collisionShape = shape;
    }
    
    /**
     * Sets the mass of this entity.
     * 
     * @param mass The new mass
     */
    public void setMass(float mass) {
        this.mass = mass;
    }
    
    /**
     * Gets the width of this entity.
     * 
     * @return The width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height of this entity.
     * 
     * @return The height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Sets the width of this entity.
     * 
     * @param width The new width
     */
    public void setWidth(int width) {
        this.width = width;
    }
    
    /**
     * Sets the height of this entity.
     * 
     * @param height The new height
     */
    public void setHeight(int height) {
        this.height = height;
    }
    
    /**
     * Checks if this entity is active.
     * 
     * @return True if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Sets whether this entity is active.
     * 
     * @param active True if active, false otherwise
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Checks if this entity is visible.
     * 
     * @return True if visible, false otherwise
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Sets whether this entity is visible.
     * 
     * @param visible True if visible, false otherwise
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Gets the tag of this entity.
     * 
     * @return The tag
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * Sets the tag of this entity.
     * 
     * @param tag The new tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }
}