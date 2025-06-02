package cam72cam.immersiverailroading.textUtil;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.model.animation.StockAnimation;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.util.With;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * New Integration but somehow I'm not really happy with it, it seems a bit clunky and it does too much
 * @author poizzy
 */
public class TextField {
    private Align align = Align.LEFT;
    private Font font = FontLoader.getOrCreateFont(FontLoader.DEFAULT);
    private GroupInfo group;
    private Identifier lastFont = FontLoader.DEFAULT;
    private List<Identifier> availableFonts;
    private List<String> linked;
    private List<String> filter;
    private RGBA color = RGBA.fromHex("#ffffff");
    private String object;
    private String text = "";
    private String lastText = "";
    private String readout;
    private VBO buffer;
    private VertexBuffer currentVBO = new VertexBuffer(0, false);
    private VertexBuffer lastVBO = new VertexBuffer(0, false);
    private boolean fullbright = false;
    private boolean global = false;
    private boolean selectable = true;
    private boolean numberPlate = false;
    private boolean unique = false;
    private float radius = 0;
    private int gap = 1;
    private int offset = 0;

    @SuppressWarnings("unused")
    public static TextField createTextField(Mesh.Group group, int resolutionX, int resolutionY) {
        return createTextField(GroupInfo.initGroup(group, resolutionX, resolutionY), group.name, t -> {});
    }

    public static TextField createTextField(Mesh.Group group, int resolutionX, int resolutionY, Consumer<TextField> defaults) {
        return createTextField(GroupInfo.initGroup(group, resolutionX, resolutionY), group.name, defaults);
    }

    private static TextField createTextField(GroupInfo group, String name, Consumer<TextField> defaults) {
        TextField instance = new TextField();
        defaults.accept(instance);
        instance.group = group;
        instance.object = name;
        return instance;
    }

    private TextField(){}

    /**
     * Set the text of the given instance of TextField
     * @param text new text for the given text field
     * @return Instance of TextField
     */
    public TextField setText(String text) {
        if (text == null) return this;
        if (!text.equals(this.text)) {
            this.text = text;
        }
        return this;
    }

    /**
     * Set the font for the given instance of TextField
     * @param font Identifier that points to the png of the font
     * @return Instance of TextField
     */
    public TextField setFont(Identifier font) {
        if (!font.equals(this.lastFont)) {
            this.font = FontLoader.getOrCreateFont(font);
            this.lastFont = font;
        }
        return this;
    }

    /**
     * Should the rendered text be fullbright?
     * @param b fullbright?
     * @return Instance of TextField
     */
    public TextField setFullBright(boolean b) {
        if (this.fullbright != b) {
            this.fullbright = b;
        }
        return this;
    }

    /**
     * Color of the rendered text
     * @param hex Hex color code as a string
     * @return Instance of TextField
     */
    public TextField setColor(String hex) {
        if (!hex.equals(this.color.toString())) {
            this.color = RGBA.fromHex(hex);
        }
        return this;
    }

    /**
     * Set the alignment of the rendered text
     * @param align "left", "center" or "right"
     * @return Instance of TextField
     */

    public TextField setAlign(String align) {
        align = align.toUpperCase();
        if (!align.equals(this.align.toString())) {
            this.align = Align.valueOf(align);
        }
        return this;
    }

    public TextField setAlign(Align align) {
        if (!align.equals(this.align)) {
            this.align = align;
        }
        return this;
    }

    // Curved text fields?
    // TODO add possibility to set a radius for the text
    public TextField setRadius(float radius) {
        if (radius != this.radius) {
            this.radius = radius;
        }
        return this;
    }

    /**
     * Set the gap between the rendered characters
     * @param gap Gap in px
     * @return Instance of TextField
     */
    public TextField setGap(int gap) {
        if (gap != this.gap) {
            this.gap = gap;
        }
        return this;
    }

    /**
     * Set the offset between two lines of text
     * @param offset Offset in px
     * @return Instance of TextField
     */
    public TextField setOffset(int offset) {
        if (offset != this.offset) {
            this.offset = offset;
        }
        return this;
    }

    /**
     * Set a list of linked text fields
     * @param linked List of object names, which corresponds to other text fields
     * @return Instance of TextField
     */
    @SuppressWarnings("unused")
    public TextField setLinked(List<String> linked) {
        if (!linked.equals(this.linked)) {
            this.linked = linked;
        }
        return this;
    }

    /**
     * Set a filter for the text field
     * <p>Only applies to text set via the gui not lua!</p>
     * @param filter Predicate that dictates the filter
     * @return Instance of TextField
     */
    @SuppressWarnings("unused")
    public TextField setFilter(List<String> filter) {
        if (!filter.equals(this.filter)) {
            this.filter = filter;
        }
        return this;
    }

    /**
     * Set if the text field should be selectable in the TypeWriter GUI
     * @param selectable Is selectable?
     * @return Instance of TextField
     */
    @SuppressWarnings("unused")
    public TextField setSelectable(boolean selectable) {
        if (selectable != this.selectable) {
            this.selectable = selectable;
        }
        return this;
    }

    /**
     * Set the avialableFont from the json
     * @param fonts List of aviable fonts
     * @return Instance of TextField
     */
    @SuppressWarnings("All")
    public TextField setAvailableFont(List<Identifier> fonts) {
        if (!fonts.equals(this.availableFonts)) {
            this.availableFonts = fonts;
        }
        return this;
    }

    /**
     * Set if the textField should be set globally
     * @param global Should be global?
     * @return Instance of TextField
     */
    public TextField setGlobal(boolean global) {
        if (global != this.global) {
            this.global = global;
        }
        return this;
    }

    /**
     * Set if the textField should be unique to the stock.
     * @param unique Is this textField unique?
     * @return Instance of TextField
     */
    public TextField setUnique(boolean unique) {
        this.unique = unique;
        return this;
    }

    /**
     * Set if the textField is a number plate
     * <p>If this is true, the text field is also unique</p>
     * @param numberPlate Is this textField a number plate?
     * @return Instance of TextField
     */
    public TextField setNumberPlate(boolean numberPlate) {
        this.unique = numberPlate;
        this.numberPlate = numberPlate;
        return this;
    }


    /**
     * @return Is the current text field selectable?
     */
    public boolean isSelectable() {
        return this.selectable;
    }

    /**
     * @return List of available fonts
     */
    public List<Identifier> getAvailableFonts() {
        return this.availableFonts;
    }

    /**
     * @return The current rendered text
     */
    public String getText() {
        return this.text;
    }

    /**
     * @return Current alignment as String
     */
    public String getAlignAsString() {
        return this.align.toString();
    }

    /**
     * @return Current alignment
     */
    public Align getAlign() {
        return this.align;
    }

    /**
     * @return Current object
     */
    public String getObject() {
        return this.object;
    }

    /**
     * @return Current color as hexadecimal
     */
    public String getColorAsHex() {
        return this.color.toString();
    }

    /**
     * @return Current gap
     */
    public int getGap() {
        return this.gap;
    }

    /**
     * @return Current Font as Identifier
     */
    public Identifier getFontIdent() {
        return this.lastFont;
    }

    /**
     * @return Gap between the lines
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return Is the text fullbright?
     */
    public boolean getFullBright() {
        return this.fullbright;
    }

    /**
     * @return Is the textField global?
     */
    public boolean getGlobal() {
        return this.global;
    }

    /**
     * @return Linked textFields;
     */
    public List<String> getLinked() {
        return this.linked;
    }

    /**
     * @return Is the textField unique?
     */
    public boolean getUnique() {
        return this.unique;
    }

    /**
     * @return Is the textField a number plate?
     */
    public boolean getNumberPlate() {
        return this.numberPlate;
    }

    /**
     * @return The actual List of filter as a Set
     */
    public List<String> getFilterAsList() {
        List<String> expanded = new ArrayList<>();

        List<String> temp;
        if (filter != null) {
            temp = filter;
        } else {
            temp = Collections.singletonList("001-999");
        }

        for (String value : temp) {
            if (value.matches("\\d+-\\d+")) {
                String[] parts = value.split("-");
                try {
                    String startStr = parts[0].trim();
                    String endStr = parts[1].trim();

                    int start = Integer.parseInt(startStr);
                    int end = Integer.parseInt(endStr);

                    int width = Math.max(startStr.length(), endStr.length());

                    if (start <= end) {
                        for (int i = start; i <= end; i++) {
                            expanded.add(String.format("%0" + width + "d", i));
                        }
                    }
                } catch (NumberFormatException e) {
                    /* */
                }
            } else {
                expanded.add(value);
            }
        }

        return expanded;
    }

    /**
     * @return Predicate for the input in the GUI
     */
    public Predicate<String> getFilter(EntityRollingStock stock) {
        if (this.filter == null) {
            return s -> {
                if (s == null || s.isEmpty()) {
                    return true;
                }

                if (this.numberPlate) {
                    try {
                        String intInut = s.replace(" ", "");
                        Integer.parseInt(intInut);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                return true;
            };
        }


        Set<String> exactMatches = new HashSet<>();
        List<int[]> numberRanges = new ArrayList<>();

        for (String value : this.filter) {
            if (value.matches("\\d+-\\d+")) {
                String[] parts = value.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                numberRanges.add(new int[]{start, end});
            } else {
                exactMatches.add(value);
            }
        }

        return s -> {
            if (s == null || s.isEmpty()) {
                return true;
            }

            if (this.unique) {
                if (stock.getDefinition().inputs.containsValue(Collections.singletonMap(object, s))) {
                    return false;
                }
            }

            if (this.numberPlate) {
                try {
                    String intInput = s.replace(" ", "");
                    Integer.parseInt(intInput);
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            if (exactMatches.contains(s)) {
                return true;
            }

            try {
                String intInput = s.replace(" ", "");
                int num = Integer.parseInt(intInput);
                for (int[] range : numberRanges) {
                    if (num >= range[0] && num  <= range[1]) {
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                /* */
            }

            return false;

        };
    }

    /**
     * Method to render the current instance of TextField
     */
    public final void postRender(EntityRollingStock stock, RenderState state, List<StockAnimation> animations, float partialTicks) {
        if (this.font == null) {
            return;
        }

        Matrix4 anim = null;
        for (StockAnimation animation : animations) {
            anim = animation.getMatrix(stock, this.object, partialTicks);
            if (anim != null) break;
        }

        Matrix4 finalAnim = anim;

        if (!Arrays.equals(this.currentVBO.data, this.lastVBO.data) || buffer == null) {
            if (buffer != null) {
                buffer.free();
            }

            if (this.currentVBO.data.length < 1) {
                createVBO();
            }

            buffer = new VBO(() -> this.currentVBO, s -> {
                s.texture(this.font.texture).lightmap(this.fullbright ? 1 : 0, 1);
                if (finalAnim != null) {
                    s.model_view().multiply(finalAnim);
                }
            });
            this.lastVBO = this.currentVBO;
        }
        double scale = 1;

        // I will probably remove this in the future
        if (Config.ConfigDebug.renderDebugLines) {
            try (With ctx = RenderContext.apply(state)) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(2.0f);

                GL11.glBegin(GL11.GL_LINES);

                // Tangent - Red
                GL11.glColor4d(1, 0, 0, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d tEnd = group.origin.add(group.tangent.scale(scale));
                GL11.glVertex3d(tEnd.x, tEnd.y, tEnd.z);

                // Bitangent - Green
                GL11.glColor4d(0, 1, 0, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d bEnd = group.origin.add(group.bitangent.scale(scale));
                GL11.glVertex3d(bEnd.x, bEnd.y, bEnd.z);

                // Normal - Blue
                GL11.glColor4d(0, 0, 1, 1);
                GL11.glVertex3d(group.origin.x, group.origin.y, group.origin.z);
                Vec3d nEnd = group.origin.add(group.normal.scale(scale));
                GL11.glVertex3d(nEnd.x, nEnd.y, nEnd.z);

                GL11.glEnd();

                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }
        }

        try (VBO.Binding binding = buffer.bind(state)) {
            binding.draw();
        }
    }

    /*
     * TODO If possible I should make this method a bit more performant to support supllier / readouts
     * But I'm not quite sure if it's even a good idea
     * Or maybe Multithreading??
     */
    @SuppressWarnings("All")
    public void createVBO() {
        if (this.font == null) {
            return;
        }

        List<float[]> vertexList = new ArrayList<>();

        String[] lines = text.split("\n");

        int[] lineWidths = new int[lines.length];
        int[] lineHeights = new int[lines.length];
        int totalVertexCount = 0;

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

                lineWidth += width + gap;
                maxHeight = Math.max(maxHeight, height);
                totalVertexCount += 6;
                index++;
            }

            lineWidth -= gap;

            lineWidths[i] = lineWidth;
            lineHeights[i] = maxHeight;
        }

        int totalTextHeight = 0;
        for (int i = 0; i < lines.length; i++) {
            totalTextHeight += lineHeights[i];
            totalTextHeight += offset;
        }
        totalTextHeight -= offset;

        Vec3d layoutTangent = group.tangent.normalize();
        Vec3d layoutBitangent = group.bitangent.normalize();

        VertexBuffer vbo = new VertexBuffer(totalVertexCount / 3, true);
        float[] buffer = vbo.data;
        int stride = vbo.stride;

        double textfieldHeight = group.bitangent.length() / group.pixelSizeY;

        double verticalOffset = 0.5 * (textfieldHeight - totalTextHeight);
        Vec3d baseYOffset = layoutBitangent.scale(verticalOffset * group.pixelSizeY);
        Vec3d lineCursor = group.origin.add(baseYOffset);

        Vec3d normal = group.normal;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            int lineWidth = lineWidths[lineIndex];
            int lineHeight = lineHeights[lineIndex];

            float alignOffset;
            switch (align) {
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

            Vec3d xOffset = layoutTangent.scale((group.resolutionX - lineWidth) * group.pixelSizeX * alignOffset);

            Vec3d cursor = lineCursor.add(xOffset);

            RGBA currentColor = this.color;
            int charIndex = 0;

            int bufferIndex = 0;

            while (charIndex < line.length()) {
                char c = line.charAt(charIndex);

                if (c == '&' && charIndex + 1 < line.length()) {
                    char code = line.charAt(charIndex + 1);
                    currentColor = RGBA.fromMinecraftCode(code, this.color);
                    charIndex += 2;
                    continue;
                }

                double charWidth = font.getCharWidthPx(c) * group.pixelSizeX;
                double charHeight = font.getCharHeightPx(c) * group.pixelSizeY;

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

                cursor = cursor.add(layoutTangent.scale(charWidth + gap * group.pixelSizeX));
                charIndex++;
            }

            if (lineIndex < lines.length -1) {
                double advance = (lineHeight + offset) * group.pixelSizeY;
                lineCursor = lineCursor.add(layoutBitangent.scale(advance));
            }
        }

        this.currentVBO = vbo;
    }


    // Maybe move this to its own class
    private static class RGBA {
        float r;
        float g;
        float b;
        final float a = 1.0f;

        static RGBA fromHex(String hex) {
            RGBA instance = new RGBA();
            instance.r = Integer.parseInt(hex.substring(1, 3), 16) / 255f;
            instance.g = Integer.parseInt(hex.substring(3, 5), 16) / 255f;
            instance.b = Integer.parseInt(hex.substring(5, 7), 16) / 255f;
            return instance;
        }

        static RGBA fromMinecraftCode(char code, RGBA defaultColor) {
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
    }

    public static class GroupInfo {
        public Vec3d origin;
        public Vec3d tangent;
        public Vec3d bitangent;
        public Vec3d normal;
        double pixelSizeX;
        double pixelSizeY;
        int resolutionX;
        boolean flippedNormal = false;

        public GroupInfo() {
        }

        public GroupInfo(Vec3d origin, Vec3d tangent, Vec3d bitangent, Vec3d normal, double pixelSizeX, double pixelSizeY, int resolutionX, boolean flippedNormal) {
            this.origin = origin;
            this.tangent = tangent;
            this.bitangent = bitangent;
            this.normal = normal;
            this.pixelSizeX = pixelSizeX;
            this.pixelSizeY = pixelSizeY;
            this.resolutionX = resolutionX;
            this.flippedNormal = flippedNormal;
        }

        public static GroupInfo initGroup(Mesh.Group group, int resX, int resY) {
            GroupInfo info = new GroupInfo();
            Mesh.Face face = group.faces.get(0);

            List<Pair<Vec3d, Vec2f>> vertices = group.faces.stream()
                    .flatMap(f -> IntStream.range(0, f.vertices.size())
                            .mapToObj(i -> Pair.of(f.vertices.get(i), f.uv.get(i))))
                    .distinct()
                    .collect(Collectors.toList());


            Vec3d topLeft = vertices.stream()
                    .min(Comparator.comparingDouble((Pair<Vec3d, Vec2f> p ) -> p.getRight().x).thenComparingDouble(p -> p.getRight().y))
                    .orElseThrow(RuntimeException::new).getLeft();

            Vec2f topLeftUv = vertices.stream()
                    .map(Pair::getRight)
                    .min(Comparator.comparingDouble((Vec2f uv) -> uv.x)
                            .thenComparingDouble(uv -> uv.y))
                    .orElseThrow(RuntimeException::new);

            Vec3d topRight = vertices.stream()
                    .filter(p -> Math.abs(p.getRight().y - topLeftUv.y) < 1e-4)
                    .max(Comparator.comparingDouble(p -> p.getRight().x))
                    .orElseThrow(RuntimeException::new).getLeft();

            Vec3d bottomLeft = vertices.stream()
                    .filter(p -> Math.abs(p.getRight().x -  topLeftUv.x) < 1e-4)
                    .max(Comparator.comparingDouble(p -> p.getRight().y))
                    .orElseThrow(RuntimeException::new).getLeft();

            Vec3d tangent = topRight.subtract(topLeft);
            Vec3d bitangent = bottomLeft.subtract(topLeft);

            double fieldWidth3d = tangent.length();
            double fieldHeight3d = bitangent.length();

            info.tangent = tangent;
            info.bitangent = bitangent;
            info.pixelSizeX = fieldWidth3d / resX;
            info.pixelSizeY = fieldHeight3d / resY;
            info.resolutionX = resX;

            info.normal = face.normal;

            Vec3d windingNormal = VecUtil.crossProduct(info.tangent, info.bitangent).normalize();
            if (VecUtil.dotProduct(windingNormal, info.normal) < 0) {
                info.flippedNormal = true;
            } else {
                info.tangent = info.tangent.scale(-1);
            }

            info.origin = info.flippedNormal ? topLeft : topRight;

            return info;
        }
    }

    public enum Align {
        RIGHT,
        CENTER,
        LEFT;

        private static final Align[] vals = values();

        public Align next() {
            return vals[(this.ordinal() + 1) % values().length];
        }
    }

    // Ohh man I hate this...
    public static class TextFieldMapMapper implements TagMapper<Map<String, TextField>> {

        @Override
        public TagAccessor<Map<String, TextField>> apply(Class<Map<String, TextField>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.setMap(fieldName, o, k -> k, v -> new TagCompound()
                                    .setString("font", v.lastFont.toString())
                            .setString("color", v.color.toString())
                            .setBoolean("fullbright", v.fullbright)
                            .setEnum("align", v.align)
                            .setInteger("gap", v.gap)
                            .setString("text", v.text)
                            .setString("name", v.object)
                            .setList("availableFonts", v.availableFonts, i -> new TagCompound().setString("identifier", i.toString()))
                            .setList("linked", v.linked, l -> new TagCompound().setString("l", l))
                            .setList("filter", v.filter, f -> new TagCompound().setString("f", f))
                            .setBoolean("global", v.global)
                            .setBoolean("unique", v.unique)
                            .setBoolean("numberPlate", v.numberPlate)
                            .setBoolean("selectable", v.selectable)
                            .set("group", new TagCompound()
                                    .setVec3d("origin", v.group.origin)
                                    .setVec3d("tangent", v.group.tangent)
                                    .setVec3d("bitangent", v.group.bitangent)
                                    .setVec3d("normal", v.group.normal)
                                    .setDouble("pixelSizeX", v.group.pixelSizeX)
                                    .setDouble("pixelSizeY", v.group.pixelSizeY)
                                    .setInteger("resX", v.group.resolutionX)
                                    .setBoolean("flippedNormal", v.group.flippedNormal))),
                    (d) -> d.getMap(fieldName, k -> k, v -> {
                        TagCompound g = v.get("group");
                                TextField.GroupInfo group = new TextField.GroupInfo(
                                        g.getVec3d("origin"),
                                        g.getVec3d("tangent"),
                                        g.getVec3d("bitangent"),
                                        g.getVec3d("normal"),
                                        g.getDouble("pixelSizeX"),
                                        g.getDouble("pixelSizeY"),
                                        g.getInteger("resX"),
                                        g.getBoolean("flippedNormal")
                                );
                                return TextField.createTextField(group, v.getString("name"), t -> {
                                            t.setFont(new Identifier(v.getString("font")))
                                                    .setColor(v.getString("color"))
                                                    .setFullBright(v.getBoolean("fullbright"))
                                                    .setAlign(v.getEnum("align", Align.class).toString())
                                                    .setGap(v.getInteger("gap"))
                                                    .setText(v.getString("text"))
                                                    .setGlobal(v.getBoolean("global"))
                                                    .setUnique(v.getBoolean("unique"))
                                                    .setNumberPlate(v.getBoolean("numberPlate"))
                                                    .setSelectable(v.getBoolean("selectable"));
                                            Optional.ofNullable(v.getList("availableFonts", i -> new Identifier(i.getString("identifier")))).ifPresent(t::setAvailableFont);
                                            Optional.ofNullable(v.getList("linked", i -> i.getString("l"))).ifPresent(t::setLinked);
                                            Optional.ofNullable(v.getList("filter", f -> f.getString("f"))).ifPresent(t::setFilter);
                                        }
                                );
                            }
                    )
            );
        }
    }

    /*
     * I don't exactly know what I should think of this double Packet, but it's needed for the GUI.
     * If I use @TagSync I would still need the double Packet to create the VBO
     */
    public static class PacketSyncTextFieldServer extends Packet {
        @TagField("stock")
        private EntityRollingStock stock;
        @TagField(value = "textFields", mapper = TextFieldMapMapper.class)
        private Map<String, TextField> textFields;

        public PacketSyncTextFieldServer() {
        }

        public PacketSyncTextFieldServer(EntityRollingStock stock, Map<String, TextField> textFields) {
            this.stock = stock;
            this.textFields = textFields;
        }

        @Override
        protected void handle() {
            if (stock instanceof EntityScriptableRollingStock) {
                EntityScriptableRollingStock s = (EntityScriptableRollingStock) stock;

                textFields.forEach((n, t) -> {
                    if (t.getLinked() != null) {
                        t.getLinked().forEach(l -> {
                            TextField linked = textFields.get(l);
                            linked.setText(t.getText()).setAlign(t.getAlign()).setFullBright(t.getFullBright());
                            textFields.put(l, linked);
                        });
                    }


                    if (t.getGlobal()) {
                        s.mapTrain(s, false, next -> ((EntityScriptableRollingStock) next).textFields.put(n, t));
                    }
                });

                s.textFields.putAll(textFields);

                new PacketSyncTextField(stock, textFields).sendToObserving(stock);
            }
        }
    }

    public static class PacketSyncTextField extends Packet {
        @TagField("stock")
        private EntityRollingStock stock;
        @TagField(value = "textFields", mapper = TextFieldMapMapper.class)
        private Map<String, TextField> textFields;

        public PacketSyncTextField() {
        }

        public PacketSyncTextField(EntityRollingStock stock, Map<String, TextField> textFields) {
            this.stock = stock;
            this.textFields = textFields;
        }

        @Override
        protected void handle() {
            if (stock instanceof EntityScriptableRollingStock) {
                EntityScriptableRollingStock s = (EntityScriptableRollingStock) stock;
                if (!s.textFields.equals(textFields)) {
                    textFields.forEach((n, t) -> {
                        if (t.getGlobal()) {
                            s.mapTrain(s, false, next -> {
                                EntityScriptableRollingStock nextStock = (EntityScriptableRollingStock) next;
                                if (!t.equals(nextStock.textFields.get(n))) {
                                    t.createVBO();
                                    nextStock.textFields.put(n, t);
                                }
                            });
                        }
                    });

                    s.textFields.putAll(textFields);
                    s.textFields.forEach((v, k) -> k.createVBO());
                }
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TextField textField = (TextField) object;
        return fullbright == textField.fullbright && Float.compare(radius, textField.radius) == 0 && gap == textField.gap && Objects.equals(text, textField.text) && lastFont.equals(textField.lastFont) && color.toString().equals(textField.color.toString()) && align == textField.align;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, lastFont, color, fullbright, align, radius, gap);
    }
}
