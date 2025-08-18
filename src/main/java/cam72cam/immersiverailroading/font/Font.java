package cam72cam.immersiverailroading.font;

import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class Font {
    /**
     * Helper class that defines a character
     */
    public static class Glyph {
        double x;
        double y;
        int width;
        int height;
    }

    public int textureHeight;
    public int textureWidth;
    public Identifier texture;
    public Map<Character, Glyph> glyphs;

    /**
     * Constructor for the struct used by the loader
     * @see FontLoader
     */
    public Font(int textureHeight, int textureWidth, Identifier texture, Map<Character, Glyph> glyphs) {
        this.textureHeight = textureHeight;
        this.textureWidth = textureWidth;
        this.texture = texture;
        this.glyphs = glyphs;
    }

    /**
     * Get the width of a given char in px
     * @param c Character
     * @return Width of the char
     */
    public int getCharWidthPx(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) {
            g = glyphs.get(' ');
        }
        return g.width;
    }

    /**
     * Get the height of a given char in px
     * @param c Character
     * @return Height of the char
     */
    public int getCharHeightPx(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) {
            g = glyphs.get(' ');
        }
        return g.height;
    }

    /**
     * Get the UV coordinates of the char on the png
     * @param c char
     * @return UVs as a pair of two Vec2f
     */
    public Pair<Vec2f, Vec2f> getUV(char c) {
        Glyph g = glyphs.get(c);
        if (g == null) {
            g = glyphs.get(' ');
        }

        Vec2f left = new Vec2f((float) g.x / textureWidth, (float) g.y / textureHeight);
        Vec2f right = new Vec2f((float) ((g.x + g.width) / textureWidth), (float) ((g.y + g.height) / textureHeight));
        return Pair.of(left, right);
    }
}
