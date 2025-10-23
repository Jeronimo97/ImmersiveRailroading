package cam72cam.immersiverailroading.textfield;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.font.Font;
import cam72cam.immersiverailroading.font.FontLoader;
import cam72cam.immersiverailroading.textfield.library.GroupInfo;
import cam72cam.immersiverailroading.textfield.library.RGBA;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.util.With;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.function.BiConsumer;

public class TextFieldCache {
    private final Map<String, VBO> buffers = new HashMap<>();
    private final Map<String, GroupInfo> groupInfos = new HashMap<>();
    private final Map<Font, Texture> textureCache = new HashMap<>();

    public final BiConsumer<TextFieldConfig, RenderState> renderFunction = ((config, state) -> {
        VBO vbo = buffers.get(config.getObject());

        if (vbo == null) {
            return;
        }

        // I will probably remove this in the future
        if (Config.ConfigDebug.renderDebugLines) {
            try (With ctx = RenderContext.apply(state)) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(2.0f);

                GL11.glBegin(GL11.GL_LINES);

                GroupInfo group = groupInfos.get(config.getObject());

                // Tangent - Red
                GL11.glColor4d(1, 0, 0, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d tEnd = group.origin.add(group.tangent.scale(1));
                GL11.glVertex3d(tEnd.x, tEnd.y, tEnd.z);

                // Bitangent - Green
                GL11.glColor4d(0, 1, 0, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d bEnd = group.origin.add(group.bitangent.scale(1));
                GL11.glVertex3d(bEnd.x, bEnd.y, bEnd.z);

                // Normal - Blue
                GL11.glColor4d(0, 0, 1, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d nEnd = group.origin.add(group.normal.scale(1));
                GL11.glVertex3d(nEnd.x, nEnd.y, nEnd.z);

                GL11.glEnd();

                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
        }

        try (VBO.Binding binding = vbo.bind(state)) {
            binding.draw();
        }
    });

    public TextFieldCache create(TextFieldConfig config, EntityRollingStock stock) {
        GroupInfo groupInfo = groupInfos.computeIfAbsent(config.getObject(), conf -> {
            // TODO replace runtime exception
            Optional<Mesh.Group> group = stock.getDefinition().getMesh().getGroupContains(config.getObject()).stream().findFirst();
            return group.map(value -> GroupInfo.initGroup(value, config.getResolutionX(), config.getResolutionY())).orElseThrow(RuntimeException::new);
        });

        Font font = FontLoader.getOrCreateFont(config.getFont());

        VertexBuffer buffer = createVBO(config, groupInfo, font);

        Texture texture = textureCache.computeIfAbsent(font, f -> Texture.wrap(f.texture));

        VBO vbo = new VBO(() -> buffer, s -> {
            s.texture(texture).lightmap(config.isFullbright() ? 1 : 0, 1);
        });

        buffers.put(config.getObject(), vbo);

        config.markDirty(false);
        return this;
    }

    private VertexBuffer createVBO(TextFieldConfig config, GroupInfo group, Font font) {
        List<float[]> vertexList = new ArrayList<>();

        String[] lines = config.getText().split("\n");

        int[] lineWidths = new int[lines.length];
        int[] lineHeights = new int[lines.length];
        int totalVertexCount = 0;

        double pixelSizeX = group.pixelSizeX * config.getScale();
        double pixelSizeY = group.pixelSizeY * config.getScale();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineWidth = 0;
            int maxHeight = 0;

            int index = 0;

            while (index < line.length()) {
                char c = line.charAt(index);

                if (c == '&' && index + 1 < line.length()) {
                    index += 2;
                    continue;
                }

                int width = font.getCharWidthPx(c);
                int height = font.getCharHeightPx(c);

                lineWidth += width + config.getGap();
                maxHeight = Math.max(maxHeight, height);
                totalVertexCount += 6;
                index++;
            }

            lineWidth -= config.getGap();

            lineWidths[i] = lineWidth;
            lineHeights[i] = maxHeight;
        }

        int totalTextHeight = 0;
        for (int i = 0; i < lines.length; i++) {
            totalTextHeight += lineHeights[i];
            totalTextHeight += config.getOffset();
        }
        totalTextHeight -= config.getOffset();

        Vec3d layoutTangent = group.tangent.normalize();
        Vec3d layoutBitangent = group.bitangent.normalize();

        VertexBuffer vbo = new VertexBuffer(totalVertexCount / 3, true);
        float[] buffer = vbo.data;
        int stride = vbo.stride;

        double textfieldHeight = group.bitangent.length() / pixelSizeY;

        float alignMultpl;

        switch (config.getVerticalAlign()) {
            case TOP: alignMultpl = 0f; break;
            case BOTTOM: alignMultpl = 1f; break;
            case CENTER:
            default: alignMultpl = 0.5f; break;
        }

        double verticalOffset = alignMultpl * (textfieldHeight - totalTextHeight);
        Vec3d baseYOffset = layoutBitangent.scale(verticalOffset * pixelSizeY);
        Vec3d lineCursor = group.origin.add(baseYOffset);

        Vec3d normal = group.normal;

        int bufferIndex = 0;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            int lineWidth = lineWidths[lineIndex];
            int lineHeight = lineHeights[lineIndex];

            float alignOffset;
            switch (config.getAlign()) {
                case CENTER:
                    alignOffset = 0.5f;
                    break;
                case RIGHT:
                    alignOffset = 1.0f;
                    break;
                case LEFT:
                default:
                    alignOffset = 0.0f;
            }

            Vec3d xOffset = layoutTangent.scale((group.resolutionX * config.getScale() - lineWidth) * pixelSizeX * alignOffset);

            Vec3d cursor = lineCursor.add(xOffset);

            RGBA currentColor = config.getColor();
            int charIndex = 0;

            while (charIndex < line.length()) {
                char c = line.charAt(charIndex);

                if (c == '&' && charIndex + 1 < line.length()) {
                    char code = line.charAt(charIndex + 1);
                    currentColor = RGBA.fromMinecraftCode(code, config.getColor());
                    charIndex += 2;
                    continue;
                }

                double charWidth = font.getCharWidthPx(c) * pixelSizeX;
                double charHeight = font.getCharHeightPx(c) * pixelSizeY;

                Pair<Vec2f, Vec2f> uvs = font.getUV(c);

                Vec3d p0 = cursor;
                Vec3d p1 = cursor.add(layoutTangent.scale(charWidth));
                Vec3d p2 = p1.add(layoutBitangent.scale(charHeight));
                Vec3d p3 = cursor.add(layoutBitangent.scale(charHeight));

                float u0 = uvs.getLeft().x;
                float v0 = uvs.getLeft().y;
                float u1 = uvs.getRight().x;
                float v1 = uvs.getRight().y;

                float[][] quad = {
                        {(float) p2.x, (float) p2.y, (float) p2.z, u1, v1},
                        {(float) p1.x, (float) p1.y, (float) p1.z, u1, v0},
                        {(float) p0.x, (float) p0.y, (float) p0.z, u0, v0},
                        {(float) p3.x, (float) p3.y, (float) p3.z, u0, v1},
                        {(float) p2.x, (float) p2.y, (float) p2.z, u1, v1},
                        {(float) p0.x, (float) p0.y, (float) p0.z, u0, v0}
                };

                for (float[] v : quad) {
                    int b = bufferIndex * stride;
                    buffer[b + vbo.vertexOffset + 0] = v[0];
                    buffer[b + vbo.vertexOffset + 1] = v[1];
                    buffer[b + vbo.vertexOffset + 2] = v[2];

                    buffer[b + vbo.textureOffset + 0] = v[3];
                    buffer[b + vbo.textureOffset + 1] = v[4];

                    buffer[b + vbo.colorOffset + 0] = currentColor.r;
                    buffer[b + vbo.colorOffset + 1] = currentColor.g;
                    buffer[b + vbo.colorOffset + 2] = currentColor.b;
                    buffer[b + vbo.colorOffset + 3] = currentColor.a;

                    buffer[b + vbo.normalOffset + 0] = (float) normal.x;
                    buffer[b + vbo.normalOffset + 1] = (float) normal.y;
                    buffer[b + vbo.normalOffset + 2] = (float) normal.z;

                    bufferIndex++;
                }

                cursor = cursor.add(layoutTangent.scale(charWidth + config.getGap() * pixelSizeX));
                charIndex++;
            }

            if (lineIndex < lines.length -1) {
                double advance = (lineHeight + config.getOffset()) * pixelSizeY;
                lineCursor = lineCursor.add(layoutBitangent.scale(advance));
            }
        }

        return vbo;
    }
}
