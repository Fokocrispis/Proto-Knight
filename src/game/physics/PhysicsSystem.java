package game.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import game.Vector2D;

/**
 * Fixed physics system with proper static object collision handling
 */
public class PhysicsSystem {
    private static final Vector2D DEFAULT_GRAVITY = new Vector2D(0, 1400.0);
    
    private final List<PhysicsObject> physicsObjects;
    private final Vector2D gravity;
    private final float worldWidth;
    private final float worldHeight;
    
    // Physics parameters
    private final int maxIterations = 10;
    private final double groundRayDistance = 5.0;
    private final double minimumPenetration = 0.01;
    private final float groundFriction = 0.9f;
    private final float airDrag = 0.999f;
    private final float positionCorrectionPercent = 0.8f;
    private final float slop = 0.02f;
    
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
        // Define collision layers
        collisionLayers.put("player", 0);
        collisionLayers.put("enemy", 1);
        collisionLayers.put("ground", 2);
        collisionLayers.put("platform", 3);
        collisionLayers.put("projectile", 4);
        collisionLayers.put("trigger", 5);
        collisionLayers.put("default", 99);
        
        // Define which layers can collide with each other
        addCollisionRule(0, new int[]{1, 2, 3}); // Player collides with enemies, ground, platforms
        addCollisionRule(1, new int[]{0, 2, 3, 4}); // Enemies collide with player, ground, platforms, projectiles
        addCollisionRule(2, new int[]{0, 1, 4}); // Ground collides with player, enemies, projectiles
        addCollisionRule(3, new int[]{0, 1, 4}); // Platforms collides with player, enemies, projectiles
        addCollisionRule(4, new int[]{1, 2, 3}); // Projectiles collide with enemies, ground, platforms
        addCollisionRule(5, new int[]{0}); // Triggers only collide with player
        addCollisionRule(99, new int[]{0, 1, 2, 3, 4, 5}); // Default layer collides with all
    }
    
    private void addCollisionRule(int layer, int[] collidesWithLayers) {
        List<Integer> list = new ArrayList<>();
        for (int other : collidesWithLayers) {
            list.add(other);
        }
        collisionMatrix.put(layer, list);
    }
    
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
    
    /**
     * Main physics update loop
     */
    public void update(long deltaTimeMs) {
        float dt = deltaTimeMs / 1000.0f;
        dt = Math.min(dt, 0.02f); // Cap time step
        
        // Multiple substeps for stability
        int substeps = 3;
        float subDt = dt / substeps;
        
        for (int step = 0; step < substeps; step++) {
            // First: Apply forces and gravity
            applyForcesAndGravity(subDt);
            
            // Second: Move objects
            integratePositions(subDt);
            
            // Third: Detect and resolve all collisions (including floor)
            for (int i = 0; i < maxIterations; i++) {
                boolean hadCollision = detectAndResolveAllCollisions();
                if (!hadCollision) break;
            }
            
            // Fourth: Additional ground detection for setting ground state
            detectGroundState();
            
            // Fifth: Apply ground friction
            applyGroundFriction(subDt);
        }
        
        // Constrain to world bounds
        constrainToWorldBounds();
    }
    
    /**
     * Apply forces including gravity
     */
    private void applyForcesAndGravity(float dt) {
        for (PhysicsObject object : physicsObjects) {
            if (object.getMass() <= 0) continue;
            
            Vector2D velocity = object.getVelocity();
            
            // Apply gravity
            if (object.isAffectedByGravity()) {
                velocity.add(gravity.times(dt));
            }
            
            // Apply air resistance
            if (!object.isOnGround()) {
                velocity.multiply(airDrag);
            }
            
            object.setVelocity(velocity);
        }
    }
    
    /**
     * Update positions based on velocity
     */
    private void integratePositions(float dt) {
        for (PhysicsObject object : physicsObjects) {
            if (object.getMass() <= 0) continue;
            
            Vector2D position = object.getPosition();
            Vector2D velocity = object.getVelocity();
            
            // Move object
            position.add(velocity.times(dt));
            
            // Update collision shape
            object.getCollisionShape().setPosition(position);
        }
    }
    
    /**
     * Detect and resolve all collisions including floor
     */
    private boolean detectAndResolveAllCollisions() {
        boolean hadCollision = false;
        
        for (int i = 0; i < physicsObjects.size(); i++) {
            PhysicsObject objectA = physicsObjects.get(i);
            if (!objectA.isCollidable()) continue;
            
            for (int j = i + 1; j < physicsObjects.size(); j++) {
                PhysicsObject objectB = physicsObjects.get(j);
                if (!objectB.isCollidable()) continue;
                
                // Check collision layers
                if (!canCollide(objectA.getCollisionLayer(), objectB.getCollisionLayer())) {
                    continue;
                }
                
                // Detect collision
                Collision collision = checkCollision(objectA, objectB);
                
                if (collision != null) {
                    hadCollision = true;
                    
                    // Resolve collision properly for static objects
                    resolveCollisionWithStatic(collision);
                    
                    // Notify objects
                    objectA.onCollision(objectB, collision);
                    objectB.onCollision(objectA, collision);
                }
            }
        }
        
        return hadCollision;
    }
    
    /**
     * Separate ground detection that doesn't interfere with collision resolution
     */
    private void detectGroundState() {
        for (PhysicsObject object : physicsObjects) {
            if (!object.isCollidable() || object.getMass() <= 0) continue;
            
            // Reset ground state - it will be set by collision if on ground
            object.setOnGround(false);
            
            // Get object bounds
            AABB bounds = object.getCollisionShape().getBoundingBox();
            Vector2D velocity = object.getVelocity();
            
            // Only check for ground if moving downward or stationary
            if (velocity.getY() < -0.1) {
                continue; // Moving upward, can't be on ground
            }
            
            // Create ground detection rays
            float rayLength = (float)groundRayDistance;
            Vector2D[] rayStarts = {
                new Vector2D(bounds.getLeft() + 2, bounds.getBottom()),
                new Vector2D(object.getPosition().getX(), bounds.getBottom()),
                new Vector2D(bounds.getRight() - 2, bounds.getBottom())
            };
            
            // Check each ray
            for (Vector2D rayStart : rayStarts) {
                Vector2D rayEnd = new Vector2D(rayStart.getX(), rayStart.getY() + rayLength);
                
                // Check against ground and platform layers
                for (PhysicsObject other : physicsObjects) {
                    if (other == object || !other.isCollidable()) continue;
                    
                    // Only check static objects for ground detection
                    if (other.getMass() > 0) continue;
                    
                    // Check if object is in ground/platform layer
                    if (other.getCollisionLayer() != collisionLayers.get("ground") && 
                        other.getCollisionLayer() != collisionLayers.get("platform")) {
                        continue;
                    }
                    
                    AABB otherBounds = other.getCollisionShape().getBoundingBox();
                    
                    // Simple ray intersection check
                    if (rayStart.getX() >= otherBounds.getLeft() && 
                        rayStart.getX() <= otherBounds.getRight() &&
                        rayStart.getY() <= otherBounds.getTop() &&
                        rayEnd.getY() >= otherBounds.getTop()) {
                        
                        object.setOnGround(true);
                        break;
                    }
                }
                
                if (object.isOnGround()) break;
            }
        }
    }
    
    /**
     * Apply friction when on ground
     */
    private void applyGroundFriction(float dt) {
        for (PhysicsObject object : physicsObjects) {
            if (!object.isOnGround() || object.getMass() <= 0) continue;
            
            Vector2D velocity = object.getVelocity();
            double friction = object.getFriction();
            
            // Apply horizontal friction
            double frictionForce = friction * 1500.0 * dt;
            double horizontalVel = velocity.getX();
            
            if (Math.abs(horizontalVel) < frictionForce) {
                velocity.setX(0); // Stop completely
            } else {
                double sign = horizontalVel > 0 ? 1 : -1;
                velocity.setX(horizontalVel - sign * frictionForce);
            }
            
            object.setVelocity(velocity);
        }
    }
    
    /**
     * Enhanced collision detection
     */
    private Collision checkCollision(PhysicsObject objectA, PhysicsObject objectB) {
        CollisionShape shapeA = objectA.getCollisionShape();
        CollisionShape shapeB = objectB.getCollisionShape();
        
        if (!shapeA.intersects(shapeB)) {
            return null;
        }
        
        if (shapeA instanceof AABB && shapeB instanceof AABB) {
            return calculateAABBCollision((AABB)shapeA, (AABB)shapeB, objectA, objectB);
        }
        
        // Default collision
        return createDefaultCollision(objectA, objectB);
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
            normal = new Vector2D(objectA.getPosition().getX() < objectB.getPosition().getX() ? -1 : 1, 0);
            penetration = overlapX;
        } else {
            // Vertical collision
            normal = new Vector2D(0, objectA.getPosition().getY() < objectB.getPosition().getY() ? -1 : 1);
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
     * Create a default collision for non-AABB shapes
     */
    private Collision createDefaultCollision(PhysicsObject objectA, PhysicsObject objectB) {
        Vector2D direction = objectB.getPosition().minus(objectA.getPosition());
        
        if (direction.lengthSquared() < 0.001) {
            direction = new Vector2D(1, 0);
        } else {
            direction.normalize();
        }
        
        return new Collision(objectA, objectB, direction, 1.0f, 
                           objectA.getPosition().plus(direction.times(0.5)));
    }
    
    /**
     * Properly resolve collision with static objects
     */
    private void resolveCollisionWithStatic(Collision collision) {
        PhysicsObject objectA = collision.getObjectA();
        PhysicsObject objectB = collision.getObjectB();
        
        Vector2D normal = collision.getNormal();
        float penetration = collision.getPenetration();
        
        if (penetration < minimumPenetration) return;
        
        // Determine which object is static
        boolean objectAStatic = objectA.getMass() <= 0;
        boolean objectBStatic = objectB.getMass() <= 0;
        
        // If both are static, don't resolve
        if (objectAStatic && objectBStatic) return;
        
        // If one is static, move only the dynamic object
        if (objectAStatic && !objectBStatic) {
            // A is static, move B away
            Vector2D correction = normal.times(-penetration);
            objectB.getPosition().add(correction);
            
            // Update velocity to prevent sinking
            Vector2D velB = objectB.getVelocity();
            double velAlongNormal = velB.dot(normal);
            if (velAlongNormal < 0) {
                velB.subtract(normal.times(velAlongNormal));
                // Apply restitution
                float restitution = Math.min(objectA.getRestitution(), objectB.getRestitution());
                velB.subtract(normal.times(velAlongNormal * restitution));
                objectB.setVelocity(velB);
            }
            
            // Set ground state for vertical collision
            if (normal.getY() < -0.5) {
                objectB.setOnGround(true);
            }
        } else if (!objectAStatic && objectBStatic) {
            // B is static, move A away
            Vector2D correction = normal.times(penetration);
            objectA.getPosition().subtract(correction);
            
            // Update velocity to prevent sinking
            Vector2D velA = objectA.getVelocity();
            double velAlongNormal = velA.dot(normal);
            if (velAlongNormal > 0) {
                velA.subtract(normal.times(velAlongNormal));
                // Apply restitution
                float restitution = Math.min(objectA.getRestitution(), objectB.getRestitution());
                velA.add(normal.times(velAlongNormal * restitution));
                objectA.setVelocity(velA);
            }
            
            // Set ground state for vertical collision
            if (normal.getY() > 0.5) {
                objectA.setOnGround(true);
            }
        } else {
            // Both are dynamic, use standard resolution
            resolveDynamicCollision(collision);
        }
    }
    
    /**
     * Resolve collision between two dynamic objects
     */
    private void resolveDynamicCollision(Collision collision) {
        PhysicsObject objectA = collision.getObjectA();
        PhysicsObject objectB = collision.getObjectB();
        
        Vector2D normal = collision.getNormal();
        float penetration = collision.getPenetration();
        
        // Calculate inverse masses
        float massA = objectA.getMass();
        float massB = objectB.getMass();
        float invMassA = massA > 0 ? 1.0f / massA : 0.0f;
        float invMassB = massB > 0 ? 1.0f / massB : 0.0f;
        float totalInvMass = invMassA + invMassB;
        
        if (totalInvMass <= 0) return;
        
        // Position correction
        Vector2D correction = normal.times(
            Math.max(penetration - slop, 0) * positionCorrectionPercent / totalInvMass
        );
        
        if (massA > 0) {
            objectA.getPosition().subtract(correction.times(invMassA));
        }
        if (massB > 0) {
            objectB.getPosition().add(correction.times(invMassB));
        }
        
        // Velocity resolution
        Vector2D velA = objectA.getVelocity();
        Vector2D velB = objectB.getVelocity();
        Vector2D relativeVelocity = velB.minus(velA);
        float velocityAlongNormal = (float) relativeVelocity.dot(normal);
        
        // Don't resolve if velocities are separating
        if (velocityAlongNormal > 0) return;
        
        // Calculate restitution
        float e = Math.min(objectA.getRestitution(), objectB.getRestitution());
        
        // Calculate impulse
        float j = -(1 + e) * velocityAlongNormal;
        j /= totalInvMass;
        
        Vector2D impulse = normal.times(j);
        
        // Apply impulse
        if (massA > 0) {
            velA.subtract(impulse.times(invMassA));
            objectA.setVelocity(velA);
        }
        if (massB > 0) {
            velB.add(impulse.times(invMassB));
            objectB.setVelocity(velB);
        }
    }
    
    /**
     * Check if two collision layers can collide
     */
    private boolean canCollide(int layerA, int layerB) {
        List<Integer> validCollisions = collisionMatrix.get(layerA);
        if (validCollisions == null) {
            // If layer is not defined, check the opposite direction
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
            boolean hitBoundary = false;
            
            // Left boundary
            if (bounds.getLeft() < 0) {
                position.setX(bounds.getWidth() / 2);
                if (velocity.getX() < 0) {
                    velocity.setX(-velocity.getX() * object.getRestitution());
                    hitBoundary = true;
                }
            }
            
            // Right boundary
            if (bounds.getRight() > worldWidth) {
                position.setX(worldWidth - bounds.getWidth() / 2);
                if (velocity.getX() > 0) {
                    velocity.setX(-velocity.getX() * object.getRestitution());
                    hitBoundary = true;
                }
            }
            
            // Top boundary
            if (bounds.getTop() < 0) {
                position.setY(bounds.getHeight() / 2);
                if (velocity.getY() < 0) {
                    velocity.setY(-velocity.getY() * object.getRestitution());
                    hitBoundary = true;
                }
            }
            
            // Bottom boundary
            if (bounds.getBottom() > worldHeight) {
                position.setY(worldHeight - bounds.getHeight() / 2);
                if (velocity.getY() > 0) {
                    velocity.setY(-velocity.getY() * object.getRestitution());
                    hitBoundary = true;
                    object.setOnGround(true);
                }
            }
            
            if (hitBoundary) {
                object.setVelocity(velocity);
            }
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