package game.physics;

import java.util.ArrayList;
import java.util.List;

import game.Vector2D;

/**
 * Improved physics system with better ground detection
 */
public class PhysicsSystem {
    private static final Vector2D DEFAULT_GRAVITY = new Vector2D(0, 1000.0);
    
    private final List<PhysicsObject> physicsObjects;
    private final Vector2D gravity;
    private final float worldWidth;
    private final float worldHeight;
    
    // Collision detection settings
    private final int maxIterations = 5;
    private final double groundDetectionTolerance = 2.0;
    
    public PhysicsSystem(float worldWidth, float worldHeight) {
        this(worldWidth, worldHeight, DEFAULT_GRAVITY);
    }
    
    public PhysicsSystem(float worldWidth, float worldHeight, Vector2D gravity) {
        this.physicsObjects = new ArrayList<>();
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.gravity = new Vector2D(gravity);
    }
    
    public void addObject(PhysicsObject object) {
        if (!physicsObjects.contains(object)) {
            physicsObjects.add(object);
        }
    }
    
    public void removeObject(PhysicsObject object) {
        physicsObjects.remove(object);
    }
    
    /**
     * Updates the physics system with improved ground detection
     */
    public void update(long deltaTimeMs) {
        float dt = deltaTimeMs / 1000.0f;
        dt = Math.min(dt, 0.05f); // Limit time step
        
        // Apply forces
        applyForces(dt);
        
        // Update positions
        updatePositions(dt);
        
        // Detect and resolve collisions with improved ground detection
        for (int i = 0; i < maxIterations; i++) {
            boolean hadCollision = detectAndResolveCollisions();
            if (!hadCollision) break;
        }
        
        // Perform additional ground checks
        performGroundDetection();
    }
    
    /**
     * Applies forces to physics objects
     */
    private void applyForces(float dt) {
        for (PhysicsObject object : physicsObjects) {
            if (object.getMass() <= 0) continue;
            
            if (object.isAffectedByGravity()) {
                Vector2D velocity = object.getVelocity();
                velocity.add(gravity.times(dt));
                object.setVelocity(velocity);
            }
        }
    }
    
    /**
     * Updates positions of physics objects
     */
    private void updatePositions(float dt) {
        for (PhysicsObject object : physicsObjects) {
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
     * Constrains objects to world boundaries
     */
    private void constrainToWorld(PhysicsObject object) {
        AABB bounds = object.getCollisionShape().getBoundingBox();
        Vector2D position = object.getPosition();
        Vector2D velocity = object.getVelocity();
        
        // Check horizontal boundaries
        if (bounds.getLeft() < 0) {
            position.setX(bounds.getWidth() / 2);
            if (velocity.getX() < 0) velocity.setX(0);
        } else if (bounds.getRight() > worldWidth) {
            position.setX(worldWidth - bounds.getWidth() / 2);
            if (velocity.getX() > 0) velocity.setX(0);
        }
        
        // Check vertical boundaries
        if (bounds.getTop() < 0) {
            position.setY(bounds.getHeight() / 2);
            if (velocity.getY() < 0) velocity.setY(0);
        } else if (bounds.getBottom() > worldHeight) {
            position.setY(worldHeight - bounds.getHeight() / 2);
            if (velocity.getY() > 0) velocity.setY(0);
        }
        
        object.setVelocity(velocity);
    }
    
    /**
     * Detects and resolves collisions with improved precision
     */
    private boolean detectAndResolveCollisions() {
        boolean hadCollision = false;
        
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
     * Performs additional ground detection for more accurate results
     */
    private void performGroundDetection() {
        for (PhysicsObject object : physicsObjects) {
            if (!object.isCollidable() || object.getMass() <= 0) continue;
            
            // Check if object is near ground
            AABB bounds = object.getCollisionShape().getBoundingBox();
            
            // Create ground detection probes below the object
            float probeY = bounds.getBottom() + (float)groundDetectionTolerance;
            
            // Check multiple points along the bottom
            AABB leftProbe = new AABB(bounds.getLeft() + 2, probeY, 2, 2);
            AABB centerProbe = new AABB((float)object.getPosition().getX(), probeY, 2, 2);
            AABB rightProbe = new AABB(bounds.getRight() - 2, probeY, 2, 2);
            
            // Check probes against other objects
            for (PhysicsObject other : physicsObjects) {
                if (other == object || !other.isCollidable()) continue;
                
                AABB otherBounds = other.getCollisionShape().getBoundingBox();
                
                // Check if any probe intersects with ground
                if (leftProbe.intersects(otherBounds) || 
                    centerProbe.intersects(otherBounds) || 
                    rightProbe.intersects(otherBounds)) {
                    
                    // Create a special ground collision
                    Vector2D normal = new Vector2D(0, -1);
                    float penetration = (float)(probeY - otherBounds.getTop());
                    Vector2D contactPoint = new Vector2D(object.getPosition().getX(), otherBounds.getTop());
                    
                    Collision groundCollision = new Collision(object, other, normal, penetration, contactPoint);
                    object.onCollision(other, groundCollision);
                    break;
                }
            }
        }
    }
    
    /**
     * Checks for collision between two objects
     */
    private Collision checkCollision(PhysicsObject objectA, PhysicsObject objectB) {
        CollisionShape shapeA = objectA.getCollisionShape();
        CollisionShape shapeB = objectB.getCollisionShape();
        
        if (!shapeA.intersects(shapeB)) {
            return null;
        }
        
        // Handle AABB-AABB collisions
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
            
            // Determine collision normal by checking which overlap is smaller
            if (xOverlap < yOverlap) {
                // X-axis collision
                normal = new Vector2D(direction.getX() < 0 ? -1 : 1, 0);
                penetration = xOverlap;
            } else {
                // Y-axis collision
                normal = new Vector2D(0, direction.getY() < 0 ? -1 : 1);
                penetration = yOverlap;
            }
            
            // Calculate contact point
            Vector2D contactPoint = new Vector2D(
                posA.getX() + normal.getX() * (aabbA.getWidth() / 2),
                posA.getY() + normal.getY() * (aabbA.getHeight() / 2)
            );
            
            return new Collision(objectA, objectB, normal, penetration, contactPoint);
        }
        
        // Default collision
        Vector2D normal = objectB.getPosition().minus(objectA.getPosition()).normalized();
        
        return new Collision(
            objectA, 
            objectB, 
            normal, 
            0.1f,
            objectA.getPosition().plus(normal.times(0.5))
        );
    }
    
    /**
     * Resolves a collision between two objects
     */
    private void resolveCollision(Collision collision) {
        PhysicsObject objectA = collision.getObjectA();
        PhysicsObject objectB = collision.getObjectB();
        
        Vector2D normal = collision.getNormal();
        float penetration = collision.getPenetration();
        
        float massA = objectA.getMass();
        float massB = objectB.getMass();
        
        // Handle static objects
        if (massA <= 0 && massB <= 0) {
            return;
        }
        
        float totalMass;
        if (massA <= 0) {
            totalMass = massB;
        } else if (massB <= 0) {
            totalMass = massA;
        } else {
            totalMass = massA + massB;
        }
        
        // Calculate position correction
        float percent = 0.8f;
        float slop = 0.01f;
        
        Vector2D correction = normal.times(
            Math.max(penetration - slop, 0) * percent / totalMass
        );
        
        // Apply position correction
        Vector2D posA = objectA.getPosition();
        Vector2D posB = objectB.getPosition();
        
        if (massA > 0) {
            posA.subtract(correction.times(massB > 0 ? massB : 1));
        }
        
        if (massB > 0) {
            posB.add(correction.times(massA > 0 ? massA : 1));
        }
        
        // Calculate velocity collision response
        Vector2D velA = objectA.getVelocity();
        Vector2D velB = objectB.getVelocity();
        
        Vector2D relativeVelocity = velB.minus(velA);
        float velocityAlongNormal = (float) relativeVelocity.dot(normal);
        
        // Objects moving away from each other
        if (velocityAlongNormal > 0) {
            return;
        }
        
        // Calculate restitution
        float restitution = 0.1f;
        
        // Calculate impulse
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
    
    // Getters and setters
    public List<PhysicsObject> getPhysicsObjects() {
        return new ArrayList<>(physicsObjects);
    }
    
    public void setGravity(Vector2D gravity) {
        this.gravity.set(gravity);
    }
    
    public Vector2D getGravity() {
        return new Vector2D(gravity);
    }
}