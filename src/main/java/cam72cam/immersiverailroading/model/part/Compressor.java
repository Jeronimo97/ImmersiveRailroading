package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;

public class Compressor extends PartSound {
    @SuppressWarnings("unused")
    private final ModelComponent component;
    
    public static Compressor get(ComponentProvider provider, ModelState state, EntityRollingStockDefinition.SoundDefinition soundFile) {
        ModelComponent component = provider.parse(ModelComponentType.COMPRESSOR);
        state.include(component);
        return new Compressor(component, soundFile);
    }
    
    public Compressor(ModelComponent component, EntityRollingStockDefinition.SoundDefinition soundFile) {
        super(soundFile, true, 150, ConfigSound.SoundCategories.Locomotive::compressor);
        this.component = component;
    }
}
