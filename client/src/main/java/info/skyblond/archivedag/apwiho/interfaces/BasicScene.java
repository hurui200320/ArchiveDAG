package info.skyblond.archivedag.apwiho.interfaces;

import javafx.scene.Parent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A very basic implementation of {@link Renderable}.
 */
public abstract class BasicScene implements Renderable {
    private Parent layout = null;

    /**
     * When called, construct a component tree.
     *
     * @return Generated layout, cannot be null.
     */
    abstract protected @NotNull Parent generateLayout();

    /**
     * When called, remove current saved result.
     * The layout will be re-rendered when needed.
     */
    protected void rerenderNextTime() {
        this.layout = null;
    }

    /**
     * Called right before the layout is needed to be rendered.
     * When called, refresh the components in the {@link BasicScene#layout} to the latest status.
     */
    abstract protected void refreshLayout();

    /**
     * Here the render root is a 2-step operation:
     * 1. generating the components
     * 2. update the components
     * <p>
     * The components will be saved to {@link BasicScene#layout} unless the
     * {@link BasicScene#rerenderNextTime()} is called.
     * <p>
     * The second step will be useful if the content need updates when switching back.
     * <p>
     * Finally, the debug short will be executed if we're in debug mode.
     */
    @Override
    public Parent renderRoot() {
        if (this.layout == null) {
            this.layout = Objects.requireNonNull(this.generateLayout(), "Failed to generate layout");
        }
        this.refreshLayout();
        return this.layout;
    }
}
