package game.entity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import game.physics.Collision;
import game.physics.PhysicsObject;
import game.physics.AABB;
import game.resource.ResourceManager;
import game.Vector2D;

/**
 * Fixed FloorEntity configured to work properly with new physics system
 */
public class FloorEntity extends AbstractEntity {
    private BufferedImage textureDirt;
    
    // Fixed tile width in pixels (2 meters)
    private static final int TILE_WIDTH = 200; // 2 meters * 100 pixels per meter
    
    /**
     * Creates a new floor entity.
     */
    public FloorEntity(double x, double y, int width, int height) {
        super(x, y, width, height);
        
        // Floor-specific physics properties
        this.mass = 0; // Static object (immovable)
        this.affectedByGravity = false;
        this.restitution = 0.0f; // No bounce
        this.friction = 0.9f; // High friction for good control
        
        // Ensure collision shape is properly sized and positioned
        this.collisionShape = new AABB(position, width, height);
        
        // Load floor sprites
        loadSprites();
    }
    
    /**
     * Loads the sprite textures for the floor.
     */
    private void loadSprites() {
        try {
            ResourceManager resourceManager = ResourceManager.getInstance();
            textureDirt = resourceManager.loadImage("dirt_orange.png");
            
            if (textureDirt == null) {
                System.err.println("Warning: dirt_orange.png not found");
            } else {
                System.out.println("Floor texture size: " + textureDirt.getWidth() + "x" + textureDirt.getHeight());
            }
        } catch (Exception e) {
            System.err.println("Failed to load floor sprites: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void update(long deltaTime) {
        // No update needed for static floor
        // Just ensure the collision shape stays in place
        if (collisionShape != null) {
            collisionShape.setPosition(position);
        }
    }
    
    @Override
    public void render(Graphics2D g) {
        if (!visible || textureDirt == null) return;
        
        // Calculate position for rendering
        int startX = (int)(position.getX() - width / 2);
        int startY = (int)(position.getY() - height / 2);
        
        // Calculate how many tiles we need to cover the floor width
        int tilesNeeded = (int)Math.ceil((double)width / TILE_WIDTH);
        
        // Draw tiles with consistent 2m width
        for (int i = 0; i < tilesNeeded; i++) {
            int tileX = startX + i * TILE_WIDTH;
            
            // Draw the dirt texture tile (always 2m wide)
            g.drawImage(
                textureDirt,
                tileX,
                startY,
                TILE_WIDTH, // Always use 2m width
                height,
                null
            );
        }
        
        // Debug: Draw collision box outline (optional - uncomment to see hitbox)
        /*
        g.setColor(new java.awt.Color(255, 255, 0, 80));
        g.drawRect(
            startX,
            startY,
            width,
            height
        );
        */
    }
    
    @Override
    public void onCollision(PhysicsObject other, Collision collision) {
        // Floor doesn't react to collisions
        // The physics system will handle all resolution for static objects
    }
    
    @Override
    public void setPosition(Vector2D position) {
        // Override to ensure floor stays in place
        super.setPosition(position);
        // Make sure collision shape is always updated
        if (collisionShape != null) {
            collisionShape.setPosition(this.position);
        }
    }
    
    @Override
    public void setPosition(double x, double y) {
        // Override to ensure floor stays in place
        super.setPosition(x, y);
        // Make sure collision shape is always updated
        if (collisionShape != null) {
            collisionShape.setPosition(this.position);
        }
    }
}