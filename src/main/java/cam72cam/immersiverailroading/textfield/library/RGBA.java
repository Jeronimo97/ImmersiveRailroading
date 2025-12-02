package cam72cam.immersiverailroading.textfield.library;

import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagField;

public class RGBA {
    public float r;
    public float g;
    public float b;
    public final float a = 1.0f;

    public static RGBA fromHex(String hex) {
        RGBA instance = new RGBA();
        instance.r = Integer.parseInt(hex.substring(1, 3), 16) / 255f;
        instance.g = Integer.parseInt(hex.substring(3, 5), 16) / 255f;
        instance.b = Integer.parseInt(hex.substring(5, 7), 16) / 255f;
        return instance;
    }

    public static RGBA fromMinecraftCode(char code, RGBA defaultColor) {
        switch (Character.toLowerCase(code)) {
            case '0': return RGBA.fromHex("#000000"); // Black
            case '1': return RGBA.fromHex("#0000AA"); // Dark Blue
            case '2': return RGBA.fromHex("#00AA00"); // Dark Green
            case '3': return RGBA.fromHex("#00AAAA"); // Dark Aqua
            case '4': return RGBA.fromHex("#AA0000"); // Dark Red
            case '5': return RGBA.fromHex("#AA00AA"); // Dark Purple
            case '6': return RGBA.fromHex("#FFAA00"); // Gold
            case '7': return RGBA.fromHex("#AAAAAA"); // Gray
            case '8': return RGBA.fromHex("#555555"); // Dark Gray
            case '9': return RGBA.fromHex("#5555FF"); // Blue
            case 'a': return RGBA.fromHex("#55FF55"); // Green
            case 'b': return RGBA.fromHex("#55FFFF"); // Aqua
            case 'c': return RGBA.fromHex("#FF5555"); // Red
            case 'd': return RGBA.fromHex("#FF55FF"); // Light Purple
            case 'e': return RGBA.fromHex("#FFFF55"); // Yellow
            case 'f': return RGBA.fromHex("#FFFFFF"); // White
            case 'r':
            default:  return defaultColor;
        }
    }

    @Override
    public String toString() {
        return String.format("#%02X%02X%02X", (int) (this.r * 255f), (int) (this.g * 255f), (int) (this.b * 255f));
    }

    public static class TagMapper implements cam72cam.mod.serialization.TagMapper<RGBA> {

        @Override
        public TagAccessor<RGBA> apply(Class<RGBA> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.setString(fieldName, o.toString()),
                    d -> RGBA.fromHex(d.getString(fieldName))
            );
        }
    }
}
