package game.resource;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Manages loading and caching of game resources.
 */
public class ResourceManager {
    private static final String RESOURCE_FOLDER = "resources";
    private static ResourceManager instance;
    
    // Cache for loaded images
    private final Map<String, BufferedImage> imageCache;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ResourceManager() {
        this.imageCache = new HashMap<>();
    }
    
    /**
     * Gets the singleton instance of ResourceManager.
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }
    
    /**
     * Loads a player spritesheet from resources.
     */
    public BufferedImage loadPlayerSpritesheet() throws IOException {
        return loadImage("player-spritesheet.png");
    }
    
    /**
     * Loads an image from the resources folder.
     */
    public BufferedImage loadImage(String fileName) throws IOException {
        // Check cache first
        if (imageCache.containsKey(fileName)) {
            return imageCache.get(fileName);
        }
        
        // Try to load from resources folder
        URL resource = getResource(fileName);
        
        if (resource != null) {
            BufferedImage image = ImageIO.read(resource);
            imageCache.put(fileName, image);
            System.out.println("Loaded image: " + fileName);
            return image;
        }
        
        // Fallback to file loading
        File file = new File(RESOURCE_FOLDER + File.separator + fileName);
        if (file.exists()) {
            BufferedImage image = ImageIO.read(file);
            imageCache.put(fileName, image);
            System.out.println("Loaded image from file: " + fileName);
            return image;
        }
        
        throw new IOException("Could not find image: " + fileName);
    }
    
    /**
     * Gets a resource URL from the classpath.
     */
    private URL getResource(String name) {
        String resourcePath = String.format("/%s/%s", RESOURCE_FOLDER, name);
        return ResourceManager.class.getResource(resourcePath);
    }
    
    /**
     * Clears the image cache.
     */
    public void clearCache() {
        imageCache.clear();
    }
    
    
}