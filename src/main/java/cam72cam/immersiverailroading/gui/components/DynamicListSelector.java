package cam72cam.immersiverailroading.gui.components;

import cam72cam.mod.gui.screen.IScreenBuilder;

import java.util.Map;

public abstract class DynamicListSelector<T> extends ListSelector<T> {
    public DynamicListSelector(IScreenBuilder screen, int xOff, int width, int height, T currentValue, Map<String, T> rawOptions) {
        super(screen, xOff, width, height, currentValue, rawOptions);
    }

    public void update(T currentValue, Map<String, T> rawOptions) {
        this.currentValue = currentValue;
        this.rawOptions = rawOptions;
        this.updateSearch("");
    }

}
