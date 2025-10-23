package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.immersiverailroading.textfield.TextFieldCache;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TextField<ENTITY extends EntityRollingStock> {
    private final Map<UUID, TextFieldCache> cache = new HashMap<>();

    public void postRender(ENTITY stock, RenderState state, List<StockAnimation> animations, float partialTicks) {
        if (!(stock instanceof EntityScriptableRollingStock)) {
            return;
        }

        for (TextFieldConfig config : ((EntityScriptableRollingStock) stock).getTextFieldConfig().values()) {
            TextFieldCache cachedField = cache.computeIfAbsent(stock.getUUID(), u -> new TextFieldCache().create(config, stock));

            RenderState animState = state.clone();

            Matrix4 anim;
            for (StockAnimation animation : animations) {
                if ((anim = animation.getMatrix(stock, config.getObject(), partialTicks)) != null) {
                    animState.model_view().multiply(anim);
                    break;
                };
            }

            cachedField.renderFunction.accept(config, animState);

            if (config.isDirty()) {
                cachedField.create(config, stock);
            }
        }
    }

    public void removed(ENTITY stock) {
        cache.remove(stock.getUUID());
    }
}
