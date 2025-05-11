package game.resource;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Fixed ResourceManager that tries multiple paths and provides better error handling
 */
public class ResourceManager {
    private static ResourceManager instance;
    private final Map<String, BufferedImage> imageCache;
    
    private ResourceManager() {
        this.imageCache = new HashMap<>();
    }
    
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }
    
    /**
     * Loads an image trying multiple possible paths
     */
    public BufferedImage loadImage(String fileName) throws IOException {
        // Check cache first
        if (imageCache.containsKey(fileName)) {
            System.out.println("Using cached image: " + fileName);
            return imageCache.get(fileName);
        }
        
        System.out.println("Attempting to load: " + fileName);
        
        // Try multiple paths
        String[] pathVariations = {
            // Direct file paths
            fileName,
            "resources/" + fileName,
            "resources/Sprites/" + fileName,
            "src/resources/Sprites/" + fileName,
            "Sprites/" + fileName,
            
            // Alternative naming (without apostrophes)
            fileName.replace("'", ""),
            "resources/Sprites/" + fileName.replace("'", ""),
            "Sprites/" + fileName.replace("'", ""),
            
            // Without special characters
            fileName.replace("#", ""),
            "resources/Sprites/" + fileName.replace("#", ""),
        };
        
        BufferedImage image = null;
        
        // Try each path variation
        for (String path : pathVariations) {
            System.out.println("  Trying: " + path);
            
            // First try as file
            image = tryLoadAsFile(path);
            if (image != null) {
                System.out.println("  ✓ Loaded as file: " + path);
                imageCache.put(fileName, image);
                return image;
            }
            
            // Then try as resource
            image = tryLoadAsResource(path);
            if (image != null) {
                System.out.println("  ✓ Loaded as resource: " + path);
                imageCache.put(fileName, image);
                return image;
            }
        }
        
        // If still not found, provide detailed debug info
        printDetailedDebugInfo(fileName);
        
        // Create a fallback image instead of throwing exception
        System.err.println("Could not find image: " + fileName + ", creating fallback");
        image = createFallbackImage(fileName);
        imageCache.put(fileName, image);
        return image;
    }
    
    /**
     * Tries to load image as a file
     */
    private BufferedImage tryLoadAsFile(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    return img;
                }
            }
        } catch (Exception e) {
            // Silent fail, will try next path
        }
        return null;
    }
    
    /**
     * Tries to load image as a resource
     */
    private BufferedImage tryLoadAsResource(String path) {
        try {
            // Try without leading slash
            URL resource = getClass().getResource(path);
            if (resource == null) {
                // Try with leading slash
                resource = getClass().getResource("/" + path);
            }
            
            if (resource != null) {
                BufferedImage img = ImageIO.read(resource);
                if (img != null) {
                    return img;
                }
            }
        } catch (Exception e) {
            // Silent fail, will try next path
        }
        return null;
    }
    
    /**
     * Prints detailed debug information when a file is not found
     */
    private void printDetailedDebugInfo(String fileName) {
        System.err.println("\n=== DETAILED DEBUG INFO ===");
        System.err.println("Failed to load: " + fileName);
        System.err.println("Current working directory: " + System.getProperty("user.dir"));
        
        // List contents of common directories
        listDirectoryContents("resources");
        listDirectoryContents("resources/Sprites");
        listDirectoryContents("Sprites");
        listDirectoryContents(".");
        
        System.err.println("=== END DEBUG INFO ===\n");
    }
    
    /**
     * Lists contents of a directory for debugging
     */
    private void listDirectoryContents(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            System.err.println("Contents of '" + dirPath + "':");
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    System.err.println("  " + file.getName() + 
                                      (file.isDirectory() ? " (dir)" : ""));
                }
            } else {
                System.err.println("  (empty)");
            }
        } else {
            System.err.println("Directory '" + dirPath + "' does not exist");
        }
    }
    
    /**
     * Creates a fallback image when the sprite cannot be loaded
     */
    private BufferedImage createFallbackImage(String fileName) {
        int width = 64, height = 64;
        BufferedImage fallback = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        java.awt.Graphics2D g = fallback.createGraphics();
        // Create a simple colored square
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, width, height);
        
        // Add a label
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 8));
        g.drawString("MISSING", 5, 30);
        g.drawString("SPRITE", 5, 45);
        
        g.dispose();
        
        return fallback;
    }
    
    /**
     * Loads a player spritesheet with automatic path detection
     */
    public BufferedImage loadPlayerSpritesheet() throws IOException {
        // Try different common names for the player spritesheet
        String[] spriteNames = {
            "JoannaD'ArcIII-Sheet#1.png",
            "JoannaDArcIII-Sheet1.png",
            "player-spritesheet.png",
            "player_sprites.png"
        };
        
        for (String name : spriteNames) {
            try {
                BufferedImage img = loadImage(name);
                if (img != null) {
                    System.out.println("Player spritesheet loaded: " + name);
                    return img;
                }
            } catch (Exception e) {
                // Try next name
            }
        }
        
        throw new IOException("Could not find player spritesheet");
    }
    
    /**
     * Clears the image cache
     */
    public void clearCache() {
        imageCache.clear();
    }
}