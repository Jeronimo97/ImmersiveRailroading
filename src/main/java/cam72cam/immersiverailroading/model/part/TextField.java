package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.ModelState;
import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.textfield.TextFieldRenderer;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextField<ENTITY extends EntityRollingStock> {
    private final Map<ENTITY, TextFieldRenderer> cachedRenderer = new HashMap<>();
    private final String object;
    private final String key;

    public static <T extends EntityMoveableRollingStock> List<TextField<T>> get(ComponentProvider provider, ModelState state) {
        return provider.parseAll(ModelComponentType.TEXTFIELD_X).stream().map(p -> new TextField<T>(p, state, provider.internal_model_scale)).collect(Collectors.toList());
    }

    public static <T extends EntityMoveableRollingStock> List<TextField<T>> get(ComponentProvider provider, ModelState state, ModelComponentType.ModelPosition pos) {
        return provider.parseAll(ModelComponentType.TEXTFIELD_X, pos).stream().map(p -> new TextField<T>(p, state, provider.internal_model_scale)).collect(Collectors.toList());
    }

    public TextField(ModelComponent component, ModelState state, double scale) {
        state = state.push(builder -> builder.add((ModelState.GroupVisibility) (stock, group) -> false));

        Pattern pattern = Pattern.compile("TEXTFIELD_[^_]+");
        Matcher matcher = pattern.matcher(component.key);

        this.key = component.key;

        if (matcher.find()) {
            this.object = matcher.group();
        } else {
            this.object = "";
        }

        ModelState modelState = state;
        modelState.include(component);
    }

    public final void render(ENTITY stock, RenderState state, List<StockAnimation> animations, float partialTicks) {
        TextFieldConfig config = ((EntityScriptableRollingStock) stock).getTextFieldConfig().get(object);
        if (config == null) {
            return;
        }

        TextFieldRenderer renderer = cachedRenderer.computeIfAbsent(stock, s -> new TextFieldRenderer(config, stock));

        RenderState animState = state.clone();
        Matrix4 anim;
        for (StockAnimation animation : animations) {
            if ((anim = animation.getMatrix(stock, key, partialTicks)) != null) {
                animState.model_view().multiply(anim);
                break;
            }
        }

        renderer.render(config, animState);

        if (config.isDirty()) {
            renderer.refresh(config, stock);
        }
    }

    public void removed(ENTITY stock) {
        cachedRenderer.remove(stock);
    }
}
