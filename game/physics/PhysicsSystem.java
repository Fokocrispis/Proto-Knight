package game.physics;

import java.util.ArrayList;
import java.util.List;

import game.Vector2D;

/**
 * Manages physics simulation for a collection of physics objects.
 */
public class PhysicsSystem {
    // Increased gravity for better game feel
    private static final Vector2D DEFAULT_GRAVITY = new Vector2D(0, 30.0);
    
    private final List<PhysicsObject> physicsObjects;
    private final Vector2D gravity;
    private final float worldWidth;
    private final float worldHeight;
    
    // Collision detection and resolution settings
    private final int maxIterations = 5;
    
    /**
     * Creates a new physics system with default gravity.
     * 
     * @param worldWidth The width of the world.
     * @param worldHeight The height of the world.
     */
    public PhysicsSystem(float worldWidth, float worldHeight) {
        this(worldWidth, worldHeight, DEFAULT_GRAVITY);
    }
    
    /**
     * Creates a new physics system.
     * 
     * @param worldWidth The width of the world.
     * @param worldHeight The height of the world.
     * @param gravity The gravity vector.
     */
    public PhysicsSystem(float worldWidth, float worldHeight, Vector2D gravity) {
        this.physicsObjects = new ArrayList<>();
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.gravity = new Vector2D(gravity);
    }
    
    /**
     * Adds a physics object to the system.
     * 
     * @param object The physics object to add.
     */
    public void addObject(PhysicsObject object) {
        if (!physicsObjects.contains(object)) {
            physicsObjects.add(object);
        }
    }
    
    /**
     * Removes a physics object from the system.
     * 
     * @param object The physics object to remove.
     */
    public void removeObject(PhysicsObject object) {
        physicsObjects.remove(object);
    }
    
    /**
     * Updates the physics system.
     * 
     * @param deltaTimeMs The time elapsed since the last update in milliseconds.
     */
    public void update(long deltaTimeMs) {
        // Convert milliseconds to seconds for physics calculations
        float dt = deltaTimeMs / 1000.0f;
        
        // Limit the time step to avoid large jumps
        dt = Math.min(dt, 0.05f);
        
        // Apply forces (gravity, etc.)
        applyForces(dt);
        
        // Update positions
        updatePositions(dt);
        
        // Detect and resolve collisions
        for (int i = 0; i < maxIterations; i++) {
            boolean hadCollision = detectAndResolveCollisions();
            if (!hadCollision) break;
        }
    }
    
    /**
     * Applies forces to physics objects.
     * 
     * @param dt The time step in seconds.
     */
    private void applyForces(float dt) {
        for (PhysicsObject object : physicsObjects) {
            // Skip objects with zero mass (static objects)
            if (object.getMass() <= 0) continue;
            
            if (object.isAffectedByGravity()) {
                Vector2D velocity = object.getVelocity();
                velocity.add(gravity.times(dt));
                object.setVelocity(velocity);
            }
        }
    }
    
    /**
     * Updates positions of physics objects.
     * 
     * @param dt The time step in seconds.
     */
    private void updatePositions(float dt) {
        for (PhysicsObject object : physicsObjects) {
            // Skip objects with zero mass (static objects)
            if (object.getMass() <= 0) continue;
            
            Vector2D position = object.getPosition();
            Vector2D velocity = object.getVelocity();
            
            // Simple Euler integration
            position.add(velocity.times(dt));
            
            // Apply world boundaries
            constrainToWorld(object);
        }
    }
    
    /**
     * Constrains the object to the world boundaries.
     * 
     * @param object The physics object to constrain.
     */
    private void constrainToWorld(PhysicsObject object) {
        AABB bounds = object.getCollisionShape().getBoundingBox();
        Vector2D position = object.getPosition();
        Vector2D velocity = object.getVelocity();
        
        // Check horizontal boundaries
        if (bounds.getLeft() < 0) {
            position.setX(bounds.getWidth() / 2);
            velocity.setX(Math.abs(velocity.getX()) * 0.5); // Bounce with damping
        } else if (bounds.getRight() > worldWidth) {
            position.setX(worldWidth - bounds.getWidth() / 2);
            velocity.setX(-Math.abs(velocity.getX()) * 0.5); // Bounce with damping
        }
        
        // Check vertical boundaries
        if (bounds.getTop() < 0) {
            position.setY(bounds.getHeight() / 2);
            velocity.setY(Math.abs(velocity.getY()) * 0.5); // Bounce with damping
        } else if (bounds.getBottom() > worldHeight) {
            position.setY(worldHeight - bounds.getHeight() / 2);
            velocity.setY(-Math.abs(velocity.getY()) * 0.5); // Bounce with damping
        }
        
        object.setVelocity(velocity);
    }
    
    /**
     * Detects and resolves collisions between physics objects.
     * 
     * @return True if any collisions were detected and resolved.
     */
    private boolean detectAndResolveCollisions() {
        boolean hadCollision = false;
        
        // Simple O(n²) collision detection
        for (int i = 0; i < physicsObjects.size(); i++) {
            PhysicsObject objectA = physicsObjects.get(i);
            
            if (!objectA.isCollidable()) continue;
            
            for (int j = i + 1; j < physicsObjects.size(); j++) {
                PhysicsObject objectB = physicsObjects.get(j);
                
                if (!objectB.isCollidable()) continue;
                
                // Check for collision
                Collision collision = checkCollision(objectA, objectB);
                
                if (collision != null) {
                    hadCollision = true;
                    
                    // Resolve collision
                    resolveCollision(collision);
                    
                    // Notify objects
                    objectA.onCollision(objectB, collision);
                    objectB.onCollision(objectA, collision);
                }
            }
        }
        
        return hadCollision;
    }
    
    /**
     * Checks for a collision between two physics objects.
     * 
     * @param objectA The first object.
     * @param objectB The second object.
     * @return The collision info if colliding, null otherwise.
     */
    private Collision checkCollision(PhysicsObject objectA, PhysicsObject objectB) {
        CollisionShape shapeA = objectA.getCollisionShape();
        CollisionShape shapeB = objectB.getCollisionShape();
        
        if (!shapeA.intersects(shapeB)) {
            return null;
        }
        
        // For simplicity, we'll handle just AABB-AABB collisions here
        // More complex collision detection would be added for other shape combinations
        if (shapeA instanceof AABB && shapeB instanceof AABB) {
            AABB aabbA = (AABB) shapeA;
            AABB aabbB = (AABB) shapeB;
            
            // Calculate collision normal and penetration
            Vector2D posA = objectA.getPosition();
            Vector2D posB = objectB.getPosition();
            
            Vector2D direction = posB.minus(posA);
            
            float xOverlap = (float) ((aabbA.getWidth() + aabbB.getWidth()) / 2 - Math.abs(direction.getX()));
            float yOverlap = (float) ((aabbA.getHeight() + aabbB.getHeight()) / 2 - Math.abs(direction.getY()));
            
            Vector2D normal;
            float penetration;
            
            // Determine the collision normal by checking which overlap is smaller
            if (xOverlap < yOverlap) {
                // X-axis collision
                normal = new Vector2D(direction.getX() < 0 ? -1 : 1, 0);
                penetration = xOverlap;
            } else {
                // Y-axis collision
                normal = new Vector2D(0, direction.getY() < 0 ? -1 : 1);
                penetration = yOverlap;
            }
            
            // Calculate contact point (approximate)
            Vector2D contactPoint = new Vector2D(
                posA.getX() + normal.getX() * (aabbA.getWidth() / 2),
                posA.getY() + normal.getY() * (aabbA.getHeight() / 2)
            );
            
            return new Collision(objectA, objectB, normal, penetration, contactPoint);
        }
        
        // Add other shape combination checks here
        // For now, just return a simple collision with estimated values
        Vector2D normal = objectB.getPosition().minus(objectA.getPosition()).normalized();
        
        return new Collision(
            objectA, 
            objectB, 
            normal, 
            0.1f, // Estimated penetration
            objectA.getPosition().plus(normal.times(0.5)) // Estimated contact point
        );
    }
    
    /**
     * Resolves a collision between two objects.
     * 
     * @param collision The collision information.
     */
    private void resolveCollision(Collision collision) {
        PhysicsObject objectA = collision.getObjectA();
        PhysicsObject objectB = collision.getObjectB();
        
        Vector2D normal = collision.getNormal();
        float penetration = collision.getPenetration();
        
        float massA = objectA.getMass();
        float massB = objectB.getMass();
        
        // Handle collisions with static objects (infinite mass)
        if (massA <= 0 && massB <= 0) {
            // Both objects are static - do nothing
            return;
        }
        
        float totalMass;
        if (massA <= 0) {
            // ObjectA is static, only move objectB
            totalMass = massB;
        } else if (massB <= 0) {
            // ObjectB is static, only move objectA
            totalMass = massA;
        } else {
            // Both objects can move
            totalMass = massA + massB;
        }
        
        // Calculate position correction to prevent sinking
        float percent = 0.8f; // Increased from 0.2f for better correction
        float slop = 0.01f; // Penetration allowance
        
        Vector2D correction = normal.times(
            Math.max(penetration - slop, 0) * percent / totalMass
        );
        
        // Apply position correction
        Vector2D posA = objectA.getPosition();
        Vector2D posB = objectB.getPosition();
        
        if (massA > 0) {
            // Only move object A if it's not static
            posA.subtract(correction.times(massB > 0 ? massB : 1));
        }
        
        if (massB > 0) {
            // Only move object B if it's not static
            posB.add(correction.times(massA > 0 ? massA : 1));
        }
        
        // Calculate impulse for velocity change
        Vector2D velA = objectA.getVelocity();
        Vector2D velB = objectB.getVelocity();
        
        Vector2D relativeVelocity = velB.minus(velA);
        float velocityAlongNormal = (float) relativeVelocity.dot(normal);
        
        // Objects moving away from each other - no impulse needed
        if (velocityAlongNormal > 0) {
            return;
        }
        
        // Calculate restitution (bounciness)
        float restitution = 0.1f; // Reduced from 0.2f for less bouncy collisions
        
        // Calculate impulse scalar
        float impulseScalar = -(1 + restitution) * velocityAlongNormal;
        impulseScalar /= totalMass;
        
        // Apply impulse
        Vector2D impulse = normal.times(impulseScalar);
        
        if (massA > 0) {
            velA.subtract(impulse.times(massB > 0 ? massB : 1));
            objectA.setVelocity(velA);
        }
        
        if (massB > 0) {
            velB.add(impulse.times(massA > 0 ? massA : 1));
            objectB.setVelocity(velB);
        }
    }
    
    /**
     * Gets all physics objects in the system.
     * 
     * @return The list of physics objects.
     */
    public List<PhysicsObject> getPhysicsObjects() {
        return new ArrayList<>(physicsObjects);
    }
    
    /**
     * Sets the gravity vector.
     * 
     * @param gravity The new gravity vector.
     */
    public void setGravity(Vector2D gravity) {
        this.gravity.set(gravity);
    }
    
    /**
     * Gets the gravity vector.
     * 
     * @return The gravity vector.
     */
    public Vector2D getGravity() {
        return new Vector2D(gravity);
    }
}