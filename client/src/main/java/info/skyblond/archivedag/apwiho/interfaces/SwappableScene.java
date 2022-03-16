package info.skyblond.archivedag.apwiho.interfaces;

import javafx.scene.Scene;

import java.util.Objects;

/**
 * An implementation for scene that can switch to other scene.
 */
public abstract class SwappableScene extends BasicScene {
    private final Scene currentScene;


    /**
     * To swap to other scene, current scene is needed.
     */
    public SwappableScene(Scene currentScene) {
        this.currentScene = Objects.requireNonNull(currentScene);
    }

    /**
     * Swap is replace root component in the current scene.
     * The root is acquired from {@link Renderable#renderRoot()}
     */
    protected void swapTo(Renderable r) {
        var root = r.renderRoot();
        this.currentScene.setRoot(root);
    }

    public Scene getCurrentScene() {
        return this.currentScene;
    }

}
