package cam72cam.immersiverailroading.textfield;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.font.FontLoader;
import cam72cam.immersiverailroading.textfield.library.*;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextFieldConfig {
    @TagField(typeHint = Align.class)
    private Align align = Align.LEFT;
    @TagField(mapper = IdentifierMapper.class)
    private Identifier font = FontLoader.DEFAULT;
    @TagField(mapper = IdentifierListMapper.class)
    private List<Identifier> availableFonts;
    @TagField(mapper = StringListMapper.class)
    private List<String> linked;
    @TagField(mapper = StringListMapper.class)
    private List<String> filter;
    @TagField(mapper = RGBA.TagMapper.class)
    private RGBA color = RGBA.fromHex("#ffffff");
    @TagField
    private String object;
    @TagField
    private String text = "";
    @TagField
    private boolean fullbright = false;
    @TagField
    private boolean global = false;
    @TagField
    private boolean selectable = true;
    @TagField
    private boolean numberPlate = false;
    @TagField
    private boolean unique = false;
    @TagField
    private int resolutionX;
    @TagField
    private int resolutionY;
    @TagField
    private int gap = 1;
    @TagField
    private int offset = 0;
    @TagField
    private float scale = 1;
    @TagField
    private VerticalAlign verticalAlign = VerticalAlign.CENTER;
    /**
     * Server Only
     */
    private EntityRollingStock stock;
    /**
     * Client Only
     */
    private boolean dirty = true;


    public enum Align {
        RIGHT,
        CENTER,
        LEFT;

        private static final Align[] vals = values();

        public Align next() {
            return vals[(this.ordinal() + 1) % values().length];
        }
    }

    public enum VerticalAlign {
        TOP,
        CENTER,
        BOTTOM;

        private static final VerticalAlign[] vals = values();

        public VerticalAlign next() {
            return vals[(this.ordinal() + 1) % values().length];
        }
    }

    public TextFieldConfig() {
    }

    public TextFieldConfig(String object, int resolutionX, int resolutionY, Consumer<TextFieldConfig> defaults) {
        defaults.accept(this);
        this.object = object;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
    }

    public Align getAlign() {
        return align;
    }

    public TextFieldConfig setAlign(Align align) {
        if (!align.equals(this.align)) {
            this.align = align;
            markDirty(true);
        }
        return this;
    }

    public Identifier getFont() {
        return font;
    }

    public TextFieldConfig setFont(Identifier font) {
        if (!font.equals(this.font)) {
            this.font = font;
            markDirty(true);
        }
        return this;
    }

    public List<Identifier> getAvailableFonts() {
        return availableFonts;
    }

    public TextFieldConfig setAvailableFonts(List<Identifier> availableFonts) {
        if (!availableFonts.equals(this.availableFonts)) {
            this.availableFonts = availableFonts;
            markDirty(true);
        }
        return this;
    }

    public List<String> getLinked() {
        return linked;
    }

    public TextFieldConfig setLinked(List<String> linked) {
        if (!linked.equals(this.linked)) {
            this.linked = linked;
            markDirty(true);
        }
        return this;
    }

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
                    if (num >= range[0] && num <= range[1]) {
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                /* */
            }

            return false;

        };
    }

    public TextFieldConfig setFilter(List<String> filter) {
        if (!filter.equals(this.filter)) {
            this.filter = filter;
            markDirty(true);
        }
        return this;
    }

    public RGBA getColor() {
        return color;
    }

    public TextFieldConfig setColor(RGBA color) {
        if (!color.equals(this.color)) {
            this.color = color;
            markDirty(true);
        }
        return this;
    }

    public String getObject() {
        return object;
    }

    public TextFieldConfig setObject(String object) {
        if (!object.equals(this.object)) {
            this.object = object;
            markDirty(true);
        }
        return this;
    }

    public String getText() {
        return text;
    }

    public TextFieldConfig setText(String text) {
        if (!text.equals(this.text)) {
            this.text = text;
            markDirty(true);
        }
        return this;
    }

    public boolean isFullbright() {
        return fullbright;
    }

    public TextFieldConfig setFullbright(boolean fullbright) {
        if (fullbright != this.fullbright) {
            this.fullbright = fullbright;
            markDirty(true);
        }
        return this;
    }

    public boolean isGlobal() {
        return global;
    }

    public TextFieldConfig setGlobal(boolean global) {
        if (global != this.global) {
            this.global = global;
            markDirty(true);
        }
        return this;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public TextFieldConfig setSelectable(boolean selectable) {
        if (selectable != this.selectable) {
            this.selectable = selectable;
            markDirty(true);
        }
        return this;
    }

    public boolean isNumberPlate() {
        return numberPlate;
    }

    public TextFieldConfig setNumberPlate(boolean numberPlate) {
        if (numberPlate != this.numberPlate) {
            this.numberPlate = numberPlate;
            markDirty(true);
        }
        return this;
    }

    public boolean isUnique() {
        return unique;
    }

    public TextFieldConfig setUnique(boolean unique) {
        if (unique != this.unique) {
            this.unique = unique;
            markDirty(true);
        }
        return this;
    }

    public int getGap() {
        return gap;
    }

    public TextFieldConfig setGap(int gap) {
        if (gap != this.gap) {
            this.gap = gap;
            markDirty(true);
        }
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public TextFieldConfig setOffset(int offset) {
        if (offset != this.offset) {
            this.offset = offset;
            markDirty(true);
        }
        return this;
    }

    public float getScale() {
        return scale;
    }

    public TextFieldConfig setScale(float scale) {
        if (scale != this.scale) {
            this.scale = scale;
            markDirty(true);
        }
        return this;
    }

    public VerticalAlign getVerticalAlign() {
        return verticalAlign;
    }

    public TextFieldConfig setVerticalAlign(VerticalAlign verticalAlign) {
        if (!verticalAlign.equals(this.verticalAlign)) {
            this.verticalAlign = verticalAlign;
            markDirty(true);
        }
        return this;
    }

    public int getResolutionX() {
        return resolutionX;
    }

    public void setResolutionX(int resolutionX) {
        this.resolutionX = resolutionX;
    }

    public int getResolutionY() {
        return resolutionY;
    }

    public void setResolutionY(int resolutionY) {
        this.resolutionY = resolutionY;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty(boolean dirty) {
        this.dirty = dirty;
        if (stock != null && dirty && stock.getWorld().isServer) {
            new TextFieldClientPacket(stock, this).sendToObserving(stock);
            this.dirty = false;
        }
    }

    public EntityRollingStock getStock() {
        return stock;
    }

    public void setStock(EntityRollingStock stock) {
        this.stock = stock;
    }

    public void copyConfig(TextFieldConfig other) {
        this.setText(other.getText());
        this.setAlign(other.getAlign());
        this.setFullbright(other.isFullbright());
        this.setVerticalAlign(other.getVerticalAlign());
    }

    @Override
    public boolean equals(Object object1) {
        if (this == object1) return true;
        if (object1 == null || getClass() != object1.getClass()) return false;
        TextFieldConfig config = (TextFieldConfig) object1;
        return fullbright == config.fullbright && global == config.global && selectable == config.selectable && numberPlate == config.numberPlate && unique == config.unique && resolutionX == config.resolutionX && resolutionY == config.resolutionY && gap == config.gap && offset == config.offset && Float.compare(scale, config.scale) == 0 && align == config.align && Objects.equals(font, config.font) && Objects.equals(availableFonts, config.availableFonts) && Objects.equals(linked, config.linked) && Objects.equals(filter, config.filter) && Objects.equals(color, config.color) && Objects.equals(object, config.object) && Objects.equals(text, config.text) && verticalAlign == config.verticalAlign;
    }

    @Override
    public int hashCode() {
        return Objects.hash(align, font, availableFonts, linked, filter, color, object, text, fullbright, global, selectable, numberPlate, unique, resolutionX, resolutionY, gap, offset, scale, verticalAlign);
    }
}
