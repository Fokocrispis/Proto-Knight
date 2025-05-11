package game;

import game.scene.GameplayScene;
import game.scene.MainMenuScene;
import game.scene.SettingsScene;

/**
 * Custom game implementation for our platformer.
 */
public class PlatformerGame extends Game {
    
    /**
     * Creates a new platformer game.
     */
    public PlatformerGame() {
        super();
    }
   
    
    @Override
    protected void initializeScenes() {
        MainMenuScene mainMenuScene = new MainMenuScene(this);
        getSceneManager().registerScene("menu", mainMenuScene);
        
        GameplayScene gameplayScene = new GameplayScene(this);
        getSceneManager().registerScene("gameplay", gameplayScene);
        
        SettingsScene settingsScene = new SettingsScene(this);
        getSceneManager().registerScene("settings", settingsScene);
        
        // Start with main menu instead of gameplay
        getSceneManager().setInitialScene("menu");
    }
}