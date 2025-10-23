package cam72cam.immersiverailroading.textfield.library;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;

public class TextFieldPacket extends Packet {
    @TagField
    private EntityRollingStock stock;
    @TagField
    private TextFieldConfig config;

    public TextFieldPacket() {}

    public TextFieldPacket(EntityRollingStock stock, TextFieldConfig config) {
        this.stock = stock;
        this.config = config;
    }

    @Override
    protected void handle() {
        if (!(stock instanceof EntityScriptableRollingStock)) {
            return;
        }

        EntityScriptableRollingStock scriptableStock = (EntityScriptableRollingStock) stock;

        scriptableStock.initTextField(config);

//        new TextFieldClientPacket(stock, config).sendToObserving(stock);
    }
}
