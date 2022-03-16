package info.skyblond.archivedag.apwiho.interfaces;

import javafx.scene.Parent;

/**
 * An interface for easier switch between scenes.
 */
public interface Renderable {

    /**
     * Called when the scene is need to display
     */
    Parent renderRoot();
}
