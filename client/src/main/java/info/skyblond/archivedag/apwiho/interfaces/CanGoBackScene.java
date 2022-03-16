package info.skyblond.archivedag.apwiho.interfaces;

import javafx.scene.Scene;

import java.util.Objects;

/**
 * An implementation to allow return to last scene.
 */
public abstract class CanGoBackScene extends SwappableScene {
    /**
     * The father scene, aka the scene to go back to.
     */
    protected final Renderable fatherScene;

    public CanGoBackScene(Scene currentScene, Renderable fatherScene) {
        super(currentScene);
        this.fatherScene = Objects.requireNonNull(fatherScene);
    }

    /**
     * Go back is just swap to the father scene.
     */
    protected void goBack() {
        this.swapTo(this.fatherScene);
    }
}
