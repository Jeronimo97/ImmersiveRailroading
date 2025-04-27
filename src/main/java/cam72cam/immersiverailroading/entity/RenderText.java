package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.*;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RenderText {
    private static final Map<String, RenderText> instances = new HashMap<>();
    private final ConcurrentMap<String, TextField> textFields = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> precomputedGroupNames = new ConcurrentHashMap<>();

    public RenderText() {
    }

    public static RenderText getInstance(String name) {
        return instances.computeIfAbsent(name, k -> new RenderText());
    }

    public static class TextField {
        private final int texY;
        boolean fullbright;
        Vec3d normal;
        String text;
        Identifier id;
        Vec3d vec3dmin;
        Vec3d vec3dmax;
        InputStream json;
        int resX;
        int resY;
        Font.TextAlign textAlign;
        boolean flipDirection;
        int fontSize;
        int fontLength;
        int fontGap;
        String hexCode;
        Font myFont;
        boolean fontInitialized = false;
        boolean useAlternative;
        int lineSpacingPixels;
        int offset;

        private VBO cachedVBO = null;
        private VertexBuffer cachedVertexBuffer = null;
        private String lastText = null;
        private Vec3d lastVec3dmin = null;
        private Vec3d lastVec3dmax = null;
        private int lastResX = -1;
        private int lastResY = -1;

        public void markDirty() {
            if (cachedVBO != null) {
                cachedVBO.free();
                cachedVBO = null;
            }
            cachedVertexBuffer = null;
        }

        TextField(String text, Identifier id, Vec3d vec3dmin, Vec3d vec3dmax,
                  InputStream json, int resX, int resY, Font.TextAlign align,
                  boolean flipDir, int fontSize, int fontLength, int fontGap,
                  Vec3d normal, String hexCode, boolean fullbright, int texY, boolean useAlternative, int lineSpacingPixels, int offset) {
            this.text = text;
            this.id = id;
            this.vec3dmin = vec3dmin;
            this.vec3dmax = vec3dmax;
            this.json = json;
            this.resX = resX;
            this.resY = resY;
            this.textAlign = align;
            this.flipDirection = flipDir;
            this.fontSize = fontSize;
            this.fontLength = fontLength;
            this.fontGap = fontGap;
            this.normal = normal;
            this.hexCode = hexCode;
            this.fullbright = fullbright;
            this.texY = texY;
            this.useAlternative = useAlternative;
            this.lineSpacingPixels = lineSpacingPixels;
            this.offset = offset;
        }

        void initializeFont() {
            if (!fontInitialized) {
                int textureId = new MinecraftTexture(this.id).getId();
                this.myFont = new Font(textureId, this.fontLength, this.fontSize, this.fontGap, this.json, this.texY);
                this.fontInitialized = true;
            }
        }
    }

    public void setText(TextRenderOptions options, EntityRollingStockDefinition def) {
        InputStream json;
        try {
            if (options.id != null) {
                File file = new File(options.id.getPath());
                String jsonPath = file.getName();
                json = options.id.getRelative(jsonPath.replaceAll(".png", ".json")).getResourceStream();
            }
            else {
                EntityRollingStockDefinition.Fonts fonts = def.fontDef.get(options.fontId.get(0));
                File file = new File(fonts.font.getPath());
                String jsonPath = file.getName();
                json = fonts.font.getRelative(jsonPath.replaceAll(".png", ".json")).getResourceStream();
            }
        } catch (IOException e) {
            ModCore.error("Couldn't load json data for font %s. Error: %s", options.id, e);
            return;
        }

        TextField textData;
        textData = new TextField(options.newText, options.id, options.min, options.max, json, options.resX, options.resY, options.align, options.flipped, options.fontSize, options.fontX, options.fontGap, options.normal, options.hexCode, options.fullbright, options.textureHeight, options.useAlternative, options.lineSpacingPixels, options.offset);
        textFields.put(options.componentId, textData);
        textData.markDirty();
        precomputedGroupNames.put(options.componentId, options.groupName);
    }

    public void textRender(RenderState state, List<StockAnimation> animations, EntityRollingStock stock, float partialTicks) {

        for (Map.Entry<String, TextField> entry : textFields.entrySet()) {
            TextField field = entry.getValue();
            field.initializeFont();

            RenderState renderText = state.clone()
                    .texture(Texture.wrap(field.id))
                    .lightmap(field.fullbright ? 1 : 0, 1);

            Matrix4 transformationMatrix = null;
            String entryKey = entry.getKey();

            String matchingGroupName = precomputedGroupNames.get(entryKey);

            if (matchingGroupName != null) {
                for (StockAnimation animation : animations) {
                    transformationMatrix = animation.getMatrix(stock, matchingGroupName, partialTicks);
                    if (transformationMatrix != null) {
                        break;
                    }
                }
            }

            Vec3d animatedVec3dmin = transformationMatrix != null ? transformationMatrix.apply(field.vec3dmin) : field.vec3dmin;
            Vec3d animatedVec3dmax = transformationMatrix != null ? transformationMatrix.apply(field.vec3dmax) : field.vec3dmax;

            boolean needsUpdate = field.cachedVBO == null ||
                    field.cachedVertexBuffer == null ||
                    !field.text.equals(field.lastText) ||
                    !animatedVec3dmin.equals(field.lastVec3dmin) ||
                    !animatedVec3dmax.equals(field.lastVec3dmax) ||
                    field.resX != field.lastResX ||
                    field.resY != field.lastResY;

            if (needsUpdate) {
                field.lastText = field.text;
                field.lastVec3dmin = animatedVec3dmin;
                field.lastVec3dmax = animatedVec3dmax;
                field.lastResX = field.resX;
                field.lastResY = field.resY;

                field.cachedVertexBuffer = field.myFont.drawText(
                        field.text, animatedVec3dmin, animatedVec3dmax,
                        field.resX, field.resY, field.textAlign,
                        field.normal, field.flipDirection, field.hexCode,
                        field.useAlternative, field.lineSpacingPixels,
                        field.offset
                );

                if (field.cachedVBO != null) {
                    field.cachedVBO.free();
                }
                field.cachedVBO = new VBO(() -> field.cachedVertexBuffer, renderState -> {});
            }

            try (VBO.Binding binding = field.cachedVBO.bind(renderText)) {
                binding.draw();
            }
        }
    }
}