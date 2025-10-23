package cam72cam.immersiverailroading.textfield.library;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class TextFieldClientPacket extends Packet {
    @TagField
    private EntityRollingStock stock;
    @TagField
    private TextFieldConfig config;

    public TextFieldClientPacket() {}

    public TextFieldClientPacket(EntityRollingStock stock, TextFieldConfig config) {
        this.stock = stock;
        this.config = config;
    }

    @Override
    protected void handle() {
        if (!(stock instanceof EntityScriptableRollingStock)) {
            return;
        }

        EntityScriptableRollingStock scriptableRollingStock = (EntityScriptableRollingStock) stock;

        config.markDirty(true);

        scriptableRollingStock.textFields.put(config.getObject(), config);
    }
}
