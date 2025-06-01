package cam72cam.immersiverailroading.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.VertexBuffer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Font {

    private final double textureHeight;

    private Vec3d crossProduct(Vec3d vec1, Vec3d vec2) {
        double crossX = vec1.y * vec2.z - vec1.z * vec2.y;
        double crossY = vec1.z * vec2.x - vec1.x * vec2.z;
        double crossZ = vec1.x * vec2.y - vec1.y * vec2.x;
        return new Vec3d(crossX, crossY, crossZ);
    }

    private double dot(Vec3d vec1, Vec3d vec2) {
        return vec1.x * vec2.x + vec1.y * vec2.y + vec1.z * vec2.z;
    }

    private double getDistance2D(double x1, double z1, double x2, double z2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
    }

    private static class Glyph {
        double x, y;
        int width;
    }

    private final int textureId;
    private final int glyphHeight;
    private final int textureWidth;
    private final int gap;
    private final Map<Character, Glyph> glyphs = new HashMap<>();

    public Font(int textureId, int textureWidth, int glyphHeight, int gap, InputStream jsonInputStream, int textureHeight) {
        this.textureId = textureId;
        this.glyphHeight = glyphHeight;
        this.textureHeight = textureHeight;
        this.textureWidth = textureWidth;
        this.gap = gap;

        loadGlyphData(jsonInputStream);
    }

    private void loadGlyphData(InputStream jsonInputStream) {
        try (InputStreamReader reader = new InputStreamReader(jsonInputStream)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Glyph>>() {}.getType();
            Map<String, Glyph> loadedGlyphs = gson.fromJson(reader, type);

            for (Map.Entry<String, Glyph> entry : loadedGlyphs.entrySet()) {
                glyphs.put(entry.getKey().charAt(0), entry.getValue());
            }
        } catch (Exception e) {
            ModCore.error(String.format("An error occured while loading font %s : %s", jsonInputStream, e));
        }
    }

    private Glyph getGlyphForChar(char c) {
        return glyphs.getOrDefault(c, glyphs.get(' '));
    }

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    public VertexBuffer drawText(String text, Vec3d minVector, Vec3d maxVector, int resolutionWidth, int resolutionHeight,
                                 Font.TextAlign alignment, Vec3d normal, boolean switchPlusMinus, String fontColor,
                                 boolean useAlternative, int lineSpacingPixels, int offset) {

        normal = normal.normalize();
        Vec3d up = new Vec3d(0, 1, 0);

        if (Math.abs(dot(normal, up)) > 0.999) {
            up = new Vec3d(1, 0, 0);
        }

        Vec3d directionX = crossProduct(normal, up).normalize();

        if (directionX.lengthSquared() < 1e-8) {
            up = new Vec3d(1, 0, 0);
            directionX = crossProduct(normal, up).normalize();
        }
        Vec3d directionY = crossProduct(directionX, normal).normalize();

        double boxHeight = maxVector.y - minVector.y;
        double boxWidth = directionX.length();
        double boxWidth2 = useAlternative ? boxWidth : getDistance2D(maxVector.x, maxVector.z, minVector.x, minVector.z);

        double scaleX = boxWidth / resolutionWidth;
        double scaleY = boxHeight / resolutionHeight;

        double lineSpacingWorld = lineSpacingPixels * scaleY;
        double offsetWorld = offset * scaleY;

        String[] lines = text.split("\n");

        double glyphHeightScaled = glyphHeight * scaleY;
        double totalTextHeight = glyphHeightScaled * lines.length + (lines.length - 1) * lineSpacingWorld;
        double verticalOffset = (boxHeight - totalTextHeight) / 2 + offsetWorld;
        double yOffset = (boxHeight + totalTextHeight) / 2 - offsetWorld;

        Vec3d startPos = !switchPlusMinus ? minVector.add(directionY.scale(verticalOffset)) : maxVector.subtract(directionY.scale(yOffset));
        Vec3d currentPos = startPos;

        int r = 0, g = 0, b = 0;
        if (fontColor != null && fontColor.matches("^#([A-Fa-f0-9]{6})$")) {
            r = Integer.parseInt(fontColor.substring(1, 3), 16);
            g = Integer.parseInt(fontColor.substring(3, 5), 16);
            b = Integer.parseInt(fontColor.substring(5, 7), 16);
        }

        float rFloat = r / 255.0f;
        float gFloat = g / 255.0f;
        float bFloat = b / 255.0f;
        float alpha = 1.0f;

        int maxVertices = text.length() * 6;
        VertexBuffer vb = new VertexBuffer(maxVertices, true);
        int index = 0; // Index for the vertex buffer

        for (String line : lines) {
            double totalTextWidth = 0;
            for (char c : line.toCharArray()) {
                Glyph glyph = getGlyphForChar(c);
                if (glyph != null) {
                    totalTextWidth += (glyph.width + gap) * scaleX;
                }
            }
            if (totalTextWidth > 0) {
                totalTextWidth -= gap * scaleX;
            }

            double horizontalOffset;
            switch (alignment) {
                case RIGHT:
                    horizontalOffset = boxWidth2 - totalTextWidth;
                    break;
                case CENTER:
                    horizontalOffset = (boxWidth2 - totalTextWidth) / 2;
                    break;
                case LEFT:
                default:
                    horizontalOffset = 0;
                    break;
            }

            Vec3d lineStartPos = currentPos.add(directionX.scale(horizontalOffset));
            Vec3d currentLinePos = lineStartPos;

            for (char c : line.toCharArray()) {
                Glyph glyph = getGlyphForChar(c);
                if (glyph == null) continue;

                double u = glyph.x / textureWidth;
                double u1 = (glyph.x + glyph.width) / textureWidth;
                double v = (glyph.y + glyphHeight) / textureHeight;
                double v1 = glyph.y / textureHeight;

                double scaledGlyphWidth = glyph.width * scaleX;
                double scaledGlyphHeight = glyphHeightScaled;

                Vec3d bottomLeftPos = currentLinePos;
                Vec3d bottomRightPos = currentLinePos.add(directionX.scale(scaledGlyphWidth));
                Vec3d topRightPos = bottomRightPos.add(directionY.scale(scaledGlyphHeight));
                Vec3d topLeftPos = bottomLeftPos.add(directionY.scale(scaledGlyphHeight));

                index = addVertexToBuffer(vb.data, index, bottomLeftPos, u, v, rFloat, gFloat, bFloat, alpha, normal);
                index = addVertexToBuffer(vb.data, index, bottomRightPos, u1, v, rFloat, gFloat, bFloat, alpha, normal);
                index = addVertexToBuffer(vb.data, index, topRightPos, u1, v1, rFloat, gFloat, bFloat, alpha, normal);

                index = addVertexToBuffer(vb.data, index, bottomLeftPos, u, v, rFloat, gFloat, bFloat, alpha, normal);
                index = addVertexToBuffer(vb.data, index, topRightPos, u1, v1, rFloat, gFloat, bFloat, alpha, normal);
                index = addVertexToBuffer(vb.data, index, topLeftPos, u, v1, rFloat, gFloat, bFloat, alpha, normal);

                currentLinePos = currentLinePos.add(directionX.scale(scaledGlyphWidth + gap * scaleX));
            }
            currentPos = currentPos.subtract(directionY.scale(glyphHeightScaled + lineSpacingWorld));
        }

        return vb;
    }


    private int addVertexToBuffer(float[] buffer, int index, Vec3d position, double u, double v,
                                  float r, float g, float b, float a, Vec3d normal) {
        buffer[index++] = (float) position.x;
        buffer[index++] = (float) position.y;
        buffer[index++] = (float) position.z;

        buffer[index++] = (float) u;
        buffer[index++] = (float) v;

        buffer[index++] = r;
        buffer[index++] = g;
        buffer[index++] = b;
        buffer[index++] = a;

        if (normal != null) {
            buffer[index++] = (float) normal.x;
            buffer[index++] = (float) normal.y;
            buffer[index++] = (float) normal.z;
        }

        return index;
    }
}
