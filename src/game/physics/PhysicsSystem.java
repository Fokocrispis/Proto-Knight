package game.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import game.Vector2D;

/**
 * Simplified physics system with robust collision handling
 */
public class PhysicsSystem {
    private static final Vector2D DEFAULT_GRAVITY = new Vector2D(0, 1400.0);
    private static final double MIN_VELOCITY = 0.1;
    private static final double POSITION_CORRECTION = 0.8;
    private static final double PENETRATION_SLOP = 0.01;
    
    private final List<PhysicsObject> physicsObjects;
    private final Vector2D gravity;
    private final float worldWidth;
    private final float worldHeight;
    
    // Collision layers
    private final Map<String, Integer> collisionLayers = new HashMap<>();
    private final Map<Integer, List<Integer>> collisionMatrix = new HashMap<>();
    
    public PhysicsSystem(float worldWidth, float worldHeight) {
        this(worldWidth, worldHeight, DEFAULT_GRAVITY);
    }
    
    public PhysicsSystem(float worldWidth, float worldHeight, Vector2D gravity) {
        this.physicsObjects = new ArrayList<>();
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.gravity = new Vector2D(gravity);
        initializeCollisionLayers();
    }
    
    private void initializeCollisionLayers() {
        collisionLayers.put("player", 0);
        collisionLayers.put("enemy", 1);
        collisionLayers.put("ground", 2);
        collisionLayers.put("platform", 3);
        collisionLayers.put("projectile", 4);
        collisionLayers.put("trigger", 5);
        collisionLayers.put("default", 99);
        
        addCollisionRule(0, new int[]{1, 2, 3, 4}); // Player
        addCollisionRule(1, new int[]{0, 2, 3, 4}); // Enemy
        addCollisionRule(2, new int[]{0, 1, 4}); // Ground
        addCollisionRule(3, new int[]{0, 1, 4}); // Platform
        addCollisionRule(4, new int[]{0, 1, 2, 3}); // Projectile
        addCollisionRule(5, new int[]{0}); // Trigger
        addCollisionRule(99, new int[]{0, 1, 2, 3, 4, 5}); // Default
    }
    
    private void addCollisionRule(int layer, int[] collidesWithLayers) {
        List<Integer> list = new ArrayList<>();
        for (int other : collidesWithLayers) {
            list.add(other);
        }
        collisionMatrix.put(layer, list);
    }
    
    /**
     * Main physics update with simplified approach
     */
    public void update(long deltaTimeMs) {
        float dt = Math.min(deltaTimeMs / 1000.0f, 0.02f); // Cap time step
        
        // 1. Apply gravity and forces
        applyForcesAndGravity(dt);
        
        // 2. Save previous positions
        Map<PhysicsObject, Vector2D> previousPositions = new HashMap<>();
        for (PhysicsObject obj : physicsObjects) {
            if (obj.getMass() > 0) {
                previousPositions.put(obj, new Vector2D(obj.getPosition()));
            }
        }
        
        // 3. Integrate movement
        integrateMovement(dt);
        
        // 4. Resolve collisions with simple but robust method
        resolveCollisions(previousPositions);
        
        // 5. Apply friction and damping
        applyFrictionAndDamping(dt);
        
        // 6. Constrain to world bounds
        constrainToWorldBounds();
    }
    
    /**
     * Apply gravity and forces to all objects
     */
    private void applyForcesAndGravity(float dt) {
        for (PhysicsObject object : physicsObjects) {
            if (object.getMass() <= 0) continue;
            
            Vector2D velocity = object.getVelocity();
            
            // Apply gravity
            if (object.isAffectedByGravity()) {
                velocity.add(gravity.times(dt));
            }
            
            object.setVelocity(velocity);
        }
    }
    
    /**
     * Integrate movement for all objects
     */
    private void integrateMovement(float dt) {
        for (PhysicsObject object : physicsObjects) {
            if (object.getMass() <= 0) continue;
            
            Vector2D position = object.getPosition();
            Vector2D velocity = object.getVelocity();
            
            // Update position
            position.add(velocity.times(dt));
            
            // Update collision shape
            if (object.getCollisionShape() != null) {
                object.getCollisionShape().setPosition(position);
            }
        }
    }
    
    /**
     * Resolve all collisions with position correction
     */
    private void resolveCollisions(Map<PhysicsObject, Vector2D> previousPositions) {
        // Reset ground state
        for (PhysicsObject obj : physicsObjects) {
            obj.setOnGround(false);
        }
        
        // Multiple iterations for stability
        for (int iteration = 0; iteration < 3; iteration++) {
            boolean collisionOccurred = false;
            
            // Check all object pairs
            for (int i = 0; i < physicsObjects.size(); i++) {
                PhysicsObject objA = physicsObjects.get(i);
                if (!objA.isCollidable()) continue;
                
                for (int j = i + 1; j < physicsObjects.size(); j++) {
                    PhysicsObject objB = physicsObjects.get(j);
                    if (!objB.isCollidable()) continue;
                    
                    // Check if they can collide
                    if (!canCollide(objA.getCollisionLayer(), objB.getCollisionLayer())) {
                        continue;
                    }
                    
                    // Check for collision
                    Collision collision = checkCollision(objA, objB);
                    if (collision != null) {
                        collisionOccurred = true;
                        resolveCollision(collision);
                        
                        // Notify objects
                        objA.onCollision(objB, collision);
                        objB.onCollision(objA, collision);
                    }
                }
            }
            
            // If no collisions occurred, we're done
            if (!collisionOccurred) {
                break;
            }
        }
        
        // Simple ground detection
        detectGround();
    }
    
    /**
     * Check collision between two objects
     */
    private Collision checkCollision(PhysicsObject objectA, PhysicsObject objectB) {
        CollisionShape shapeA = objectA.getCollisionShape();
        CollisionShape shapeB = objectB.getCollisionShape();
        
        if (shapeA == null || shapeB == null) return null;
        
        if (!shapeA.intersects(shapeB)) return null;
        
        if (shapeA instanceof AABB && shapeB instanceof AABB) {
            return calculateAABBCollision((AABB)shapeA, (AABB)shapeB, objectA, objectB);
        }
        
        return null;
    }
    
    /**
     * Calculate AABB collision details
     */
    private Collision calculateAABBCollision(AABB aabbA, AABB aabbB, 
                                           PhysicsObject objectA, PhysicsObject objectB) {
        // Calculate overlap
        float overlapX = Math.min(aabbA.getRight(), aabbB.getRight()) - 
                        Math.max(aabbA.getLeft(), aabbB.getLeft());
        float overlapY = Math.min(aabbA.getBottom(), aabbB.getBottom()) - 
                        Math.max(aabbA.getTop(), aabbB.getTop());
        
        // Determine collision normal and penetration
        Vector2D normal;
        float penetration;
        
        if (overlapX < overlapY) {
            // Horizontal collision
            if (objectA.getPosition().getX() < objectB.getPosition().getX()) {
                normal = new Vector2D(-1, 0);
            } else {
                normal = new Vector2D(1, 0);
            }
            penetration = overlapX;
        } else {
            // Vertical collision
            if (objectA.getPosition().getY() < objectB.getPosition().getY()) {
                normal = new Vector2D(0, -1);
            } else {
                normal = new Vector2D(0, 1);
            }
            penetration = overlapY;
        }
        
        // Calculate contact point
        Vector2D contactPoint = new Vector2D(
            (Math.max(aabbA.getLeft(), aabbB.getLeft()) + Math.min(aabbA.getRight(), aabbB.getRight())) / 2,
            (Math.max(aabbA.getTop(), aabbB.getTop()) + Math.min(aabbA.getBottom(), aabbB.getBottom())) / 2
        );
        
        return new Collision(objectA, objectB, normal, penetration, contactPoint);
    }
    
    /**
     * Resolve collision with position correction
     */
    private void resolveCollision(Collision collision) {
        PhysicsObject objectA = collision.getObjectA();
        PhysicsObject objectB = collision.getObjectB();
        Vector2D normal = collision.getNormal();
        float penetration = collision.getPenetration();
        
        if (penetration <= PENETRATION_SLOP) return;
        
        // Calculate mass-based correction
        float totalMass = objectA.getMass() + objectB.getMass();
        float massRatioA = objectB.getMass() / Math.max(0.1f, totalMass);
        float massRatioB = objectA.getMass() / Math.max(0.1f, totalMass);
        
        // Special handling for static objects
        if (objectA.getMass() <= 0) {
            // A is static, move B
            Vector2D correction = normal.times(penetration * POSITION_CORRECTION);
            objectB.getPosition().add(correction);
            resolveVelocity(objectB, normal.negated());
            
            // Set ground state
            if (normal.getY() < -0.5) {
                objectB.setOnGround(true);
            }
        } else if (objectB.getMass() <= 0) {
            // B is static, move A
            Vector2D correction = normal.times(-penetration * POSITION_CORRECTION);
            objectA.getPosition().add(correction);
            resolveVelocity(objectA, normal);
            
            // Set ground state
            if (normal.getY() > 0.5) {
                objectA.setOnGround(true);
            }
        } else {
            // Both dynamic, use mass ratios
            Vector2D correction = normal.times(penetration * POSITION_CORRECTION);
            objectA.getPosition().subtract(correction.times(massRatioA));
            objectB.getPosition().add(correction.times(massRatioB));
            
            // Resolve velocities
            resolveVelocities(objectA, objectB, normal);
        }
        
        // Update collision shapes
        if (objectA.getCollisionShape() != null) {
            objectA.getCollisionShape().setPosition(objectA.getPosition());
        }
        if (objectB.getCollisionShape() != null) {
            objectB.getCollisionShape().setPosition(objectB.getPosition());
        }
    }
    
    /**
     * Resolve velocity for a single object against a static surface
     */
    private void resolveVelocity(PhysicsObject object, Vector2D normal) {
        Vector2D velocity = object.getVelocity();
        double velocityAlongNormal = velocity.dot(normal);
        
        if (velocityAlongNormal < 0) {
            // Remove velocity component along normal
            Vector2D normalComponent = normal.times(velocityAlongNormal);
            velocity.subtract(normalComponent);
            
            // Apply restitution
            if (object.getRestitution() > 0) {
                velocity.subtract(normalComponent.times(object.getRestitution()));
            }
            
            object.setVelocity(velocity);
        }
    }
    
    /**
     * Resolve velocities between two dynamic objects
     */
    private void resolveVelocities(PhysicsObject objectA, PhysicsObject objectB, Vector2D normal) {
        Vector2D relativeVelocity = objectB.getVelocity().minus(objectA.getVelocity());
        double velocityAlongNormal = relativeVelocity.dot(normal);
        
        // Don't resolve if velocities are separating
        if (velocityAlongNormal > 0) return;
        
        // Calculate restitution
        float e = Math.min(objectA.getRestitution(), objectB.getRestitution());
        
        // Calculate impulse
        float totalMass = objectA.getMass() + objectB.getMass();
        double j = -(1 + e) * velocityAlongNormal / Math.max(0.1f, totalMass);
        
        Vector2D impulse = normal.times(j);
        
        // Apply impulse
        objectA.getVelocity().subtract(impulse.times(1.0f / Math.max(0.1f, objectA.getMass())));
        objectB.getVelocity().add(impulse.times(1.0f / Math.max(0.1f, objectB.getMass())));
    }
    
    /**
     * Simple ground detection
     */
    private void detectGround() {
        for (PhysicsObject obj : physicsObjects) {
            if (obj.getMass() <= 0 || !obj.isCollidable()) continue;
            
            // Check if velocity is downward or very small
            if (obj.getVelocity().getY() < -10) continue;
            
            // Get object bounds
            AABB bounds = obj.getCollisionShape().getBoundingBox();
            
            // Check slightly below the object
            double checkY = bounds.getBottom() + 1;
            
            for (PhysicsObject other : physicsObjects) {
                if (other == obj || other.getMass() > 0 || !other.isCollidable()) continue;
                
                // Check if this is ground or platform
                if (other.getCollisionLayer() != collisionLayers.get("ground") && 
                    other.getCollisionLayer() != collisionLayers.get("platform")) {
                    continue;
                }
                
                AABB otherBounds = other.getCollisionShape().getBoundingBox();
                
                // Simple point-in-box check
                if (bounds.getLeft() < otherBounds.getRight() && 
                    bounds.getRight() > otherBounds.getLeft() &&
                    checkY >= otherBounds.getTop() && 
                    checkY <= otherBounds.getBottom()) {
                    obj.setOnGround(true);
                    break;
                }
            }
        }
    }
    
    /**
     * Apply friction and damping
     */
    private void applyFrictionAndDamping(float dt) {
        for (PhysicsObject object : physicsObjects) {
            if (object.getMass() <= 0) continue;
            
            Vector2D velocity = object.getVelocity();
            
            // Ground friction
            if (object.isOnGround()) {
                double friction = object.getFriction();
                double frictionForce = friction * 1500.0 * dt;
                
                // Apply horizontal friction
                if (Math.abs(velocity.getX()) < frictionForce) {
                    velocity.setX(0);
                } else {
                    double sign = velocity.getX() > 0 ? 1 : -1;
                    velocity.setX(velocity.getX() - sign * frictionForce);
                }
            }
            
            // Air resistance
            if (!object.isOnGround()) {
                velocity.multiply(0.999f);
            }
            
            // Stop very small movements
            if (Math.abs(velocity.getX()) < MIN_VELOCITY) velocity.setX(0);
            if (Math.abs(velocity.getY()) < MIN_VELOCITY && object.isOnGround()) velocity.setY(0);
            
            object.setVelocity(velocity);
        }
    }
    
    /**
     * Check if two layers can collide
     */
    private boolean canCollide(int layerA, int layerB) {
        List<Integer> validCollisions = collisionMatrix.get(layerA);
        if (validCollisions == null) {
            validCollisions = collisionMatrix.get(layerB);
            return validCollisions != null && validCollisions.contains(layerA);
        }
        return validCollisions.contains(layerB);
    }
    
    /**
     * Constrain objects to world boundaries
     */
    private void constrainToWorldBounds() {
        for (PhysicsObject object : physicsObjects) {
            if (object.getMass() <= 0) continue;
            
            AABB bounds = object.getCollisionShape().getBoundingBox();
            Vector2D position = object.getPosition();
            Vector2D velocity = object.getVelocity();
            
            // Left boundary
            if (bounds.getLeft() < 0) {
                position.setX(bounds.getWidth() / 2);
                if (velocity.getX() < 0) {
                    velocity.setX(-velocity.getX() * object.getRestitution());
                }
            }
            
            // Right boundary
            if (bounds.getRight() > worldWidth) {
                position.setX(worldWidth - bounds.getWidth() / 2);
                if (velocity.getX() > 0) {
                    velocity.setX(-velocity.getX() * object.getRestitution());
                }
            }
            
            // Top boundary
            if (bounds.getTop() < 0) {
                position.setY(bounds.getHeight() / 2);
                if (velocity.getY() < 0) {
                    velocity.setY(-velocity.getY() * object.getRestitution());
                }
            }
            
            // Bottom boundary
            if (bounds.getBottom() > worldHeight) {
                position.setY(worldHeight - bounds.getHeight() / 2);
                if (velocity.getY() > 0) {
                    velocity.setY(-velocity.getY() * object.getRestitution());
                    object.setOnGround(true);
                }
            }
            
            object.setVelocity(velocity);
        }
    }
    
    // Utility methods
    public void addObject(PhysicsObject object, String layerName) {
        if (!physicsObjects.contains(object)) {
            physicsObjects.add(object);
            object.setCollisionLayer(collisionLayers.getOrDefault(layerName, 99));
        }
    }
    
    public void addObject(PhysicsObject object) {
        addObject(object, "default");
    }
    
    public void removeObject(PhysicsObject object) {
        physicsObjects.remove(object);
    }
    
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