package game;

import game.scene.GameplayScene;
import game.scene.MainMenuScene;
import game.scene.PhysicsTestScene;
import game.scene.SettingsScene;

/**
 * Custom game implementation for our platformer with enhanced physics
 */
public class PlatformerGame extends Game {
    
    /**
     * Creates a new platformer game
     */
    public PlatformerGame() {
        super();
    }
    
    @Override
    protected void initializeScenes() {
        // Create main menu scene
        MainMenuScene mainMenuScene = new MainMenuScene(this);
        getSceneManager().registerScene("menu", mainMenuScene);
        
        // Create the main gameplay scene
        GameplayScene gameplayScene = new GameplayScene(this);
        getSceneManager().registerScene("gameplay", gameplayScene);
        
        // Create settings scene
        SettingsScene settingsScene = new SettingsScene(this);
        getSceneManager().registerScene("settings", settingsScene);
        
        // Create physics test scene for debugging
        PhysicsTestScene testScene = new PhysicsTestScene(this);
        getSceneManager().registerScene("test", testScene);
        
        // You can choose which scene to start with:
        // For testing physics, use "test"
        // For regular gameplay, use "menu"
        
        // Start with test scene to verify physics is working
        getSceneManager().setInitialScene("test");
        
        System.out.println("Initialized scenes - Starting with physics test");
        System.out.println("Physics system loaded with enhanced collision detection");
    }
}