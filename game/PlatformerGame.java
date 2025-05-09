package game;

import game.scene.GameplayScene;

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
        // Create the gameplay scene
        GameplayScene gameplayScene = new GameplayScene(this);
        
        // Register the scene with the scene manager
        getSceneManager().registerScene("gameplay", gameplayScene);
        
        // Set it as the initial scene
        getSceneManager().setInitialScene("gameplay");
    }
}