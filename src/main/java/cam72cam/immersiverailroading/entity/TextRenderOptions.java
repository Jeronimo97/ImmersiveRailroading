package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TextRenderOptions {
    public Identifier id;
    public String newText;
    public int resX;
    public int resY;
    public Font.TextAlign align;
    public boolean flipped;
    public String componentId;
    public int fontSize;
    public int fontX;
    public int fontGap;
    public List<Integer> fontId;
    public String hexCode;
    public boolean fullbright;
    public int textureHeight;
    public boolean useAlternative;
    public int lineSpacingPixels;
    public int offset;
    public boolean global;
    public List<String> linked = new ArrayList<>();
    public boolean selectable = true;
    // Used for number plates etc...
    public boolean unique = false;
    public boolean isNumberPlate = false;
    public String lastText = "";
    public List<String> filter = new ArrayList<>();
    public boolean assigned = false;
    public Vec3d min;
    public Vec3d max;
    public Vec3d normal;
    public String groupName;

    public TextRenderOptions(Identifier id, String newText, int resX, int resY, Font.TextAlign align, boolean flipped,
                             String componentId, int fontSize, int fontX, int fontGap, List<Integer> fontId, String hexCode,
                             boolean fullbright, int textureHeight, boolean useAlternative, int lineSpacingPixels, int offset, boolean global, EntityRollingStockDefinition def) {
        this.id = id;
        this.newText = newText;
        this.resX = resX;
        this.resY = resY;
        this.align = align;
        this.flipped = flipped;
        this.componentId = componentId;
        this.fontSize = fontSize;
        this.fontX = fontX;
        this.fontGap = fontGap;
        this.fontId = fontId;
        this.hexCode = hexCode;
        this.fullbright = fullbright;
        this.textureHeight = textureHeight;
        this.useAlternative = useAlternative;
        this.lineSpacingPixels = lineSpacingPixels;
        this.offset = offset;
        this.global = global;

        List<Mesh.Group> groups = def.getMesh().getGroupContains(String.format("TEXTFIELD_%s", this.componentId));
        if (groups.size() > 1) {
            ModCore.info("There are more than one text fields defined under name TEXTFIELD_&s. Using first.", this.componentId);
        }

        Mesh.Group group = groups.get(0);
        this.min = group.min;
        this.max = group.max;
        this.normal = group.normal;
        this.groupName = group.name;
    }

    private TextRenderOptions(TextRenderOptions options) {
        this.id = options.id;
        this.newText = options.newText;
        this.resX = options.resX;
        this.resY = options.resY;
        this.align = options.align;
        this.flipped = options.flipped;
        this.componentId = options.componentId;
        this.fontSize = options.fontSize;
        this.fontX = options.fontX;
        this.fontGap = options.fontGap;
        this.fontId = options.fontId;
        this.hexCode = options.hexCode;
        this.fullbright = options.fullbright;
        this.textureHeight = options.textureHeight;
        this.useAlternative = options.useAlternative;
        this.lineSpacingPixels = options.lineSpacingPixels;
        this.offset = options.offset;
        this.global = options.global;
        this.linked = options.linked;
        this.selectable = options.selectable;
        this.unique = options.unique;
        this.isNumberPlate = options.isNumberPlate;
        this.filter = options.filter;
        this.assigned = options.assigned;
        this.min = options.min;
        this.max = options.max;
        this.normal = options.normal;
        this.groupName = options.groupName;
    }

    public TextRenderOptions(TagCompound compound) {
        this.id = new Identifier(compound.getString("id"));
        this.newText = compound.getString("newText");
        this.resX = compound.getInteger("resX");
        this.resY = compound.getInteger("resY");
        this.align = compound.getEnum("align", Font.TextAlign.class);
        this.flipped = compound.getBoolean("flipped");
        this.componentId = compound.getString("componentId");
        this.fontSize = compound.getInteger("fontSize");
        this.fontX = compound.getInteger("fontX");
        this.fontGap = compound.getInteger("fontGap");
        this.fontId = compound.getList("fontId", i -> i.getInteger("id"));
        this.hexCode = compound.getString("hexCode");
        this.fullbright = compound.getBoolean("fullbright");
        this.textureHeight = compound.getInteger("textureHeight");
        this.useAlternative = compound.getBoolean("useAlternative");
        this.lineSpacingPixels = compound.getInteger("lineSpacingPixels");
        this.offset = compound.getInteger("offset");
        this.global = compound.getBoolean("global");
        this.linked = compound.getList("linked", i -> i.getString("l"));
        this.selectable = compound.getBoolean("selectable");
        this.unique = compound.getBoolean("unique");
        this.isNumberPlate = compound.getBoolean("isNumberPlate");
        this.lastText = compound.getString("lastText");
        this.filter = compound.getList("filter", i -> i.getString("f"));
        this.assigned = compound.getBoolean("assigned");
        this.min = compound.getVec3d("min");
        this.max = compound.getVec3d("max");
        this.normal = compound.getVec3d("normal");
        this.groupName = compound.getString("groupName");
    }

    public void setLinked(List<String> linked) {
        this.linked.addAll(linked);
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public void setIsNumberPlate(boolean numberPlate) {
        this.isNumberPlate = numberPlate;
        this.unique = true;
    }

    public void setFilter(List<String> filter) {
        this.filter.addAll(filter);
    }

    public TextRenderOptions clone() {
        return new TextRenderOptions(this);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TextRenderOptions options = (TextRenderOptions) object;
        return flipped == options.flipped && fontSize == options.fontSize && fontX == options.fontX && fontGap == options.fontGap && fullbright == options.fullbright && textureHeight == options.textureHeight && useAlternative == options.useAlternative && lineSpacingPixels == options.lineSpacingPixels && Objects.equals(id, options.id) && Objects.equals(newText, options.newText) && align == options.align && Objects.equals(componentId, options.componentId) && Objects.equals(hexCode, options.hexCode) && Objects.equals(groupName, options.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, newText, align, flipped, componentId, fontSize, fontX, fontGap, hexCode, fullbright, textureHeight, useAlternative, lineSpacingPixels, groupName);
    }

    public static class TextRenderOptionsMapper implements TagMapper<TextRenderOptions> {

        @Override
        public TagAccessor<TextRenderOptions> apply(Class<TextRenderOptions> type, String fieldName, TagField tagField) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> {
                        d.set(fieldName, new TagCompound()
                                .setString("id", o.id.toString())
                                .setString("newText", o.newText)
                                .setInteger("resX", o.resX)
                                .setInteger("resY", o.resY)
                                .setEnum("align", o.align)
                                .setBoolean("flipped", o.flipped)
                                .setString("componentId", o.componentId)
                                .setInteger("fontSize", o.fontSize)
                                .setInteger("fontX", o.fontX)
                                .setInteger("fontGap", o.fontGap)
                                .setList("fontId", o.fontId, i -> new TagCompound().setInteger("id", i))
                                .setString("hexCode", o.hexCode)
                                .setBoolean("fullbright", o.fullbright)
                                .setInteger("textureHeight", o.textureHeight)
                                .setBoolean("useAlternative", o.useAlternative)
                                .setInteger("lineSpacingPixels", o.lineSpacingPixels)
                                .setInteger("offset", o.offset)
                                .setBoolean("global", o.global)
                                .setList("linked", o.linked, l -> new TagCompound().setString("l", l))
                                .setBoolean("selectable", o.selectable)
                                .setBoolean("unique", o.unique)
                                .setBoolean("isNumberPlate", o.isNumberPlate)
                                .setString("lastText", o.lastText)
                                .setList("filter", o.filter, f -> new TagCompound().setString("f", f))
                                .setBoolean("assigned", o.assigned)
                                .setVec3d("min", o.min)
                                .setVec3d("max", o.max)
                                .setVec3d("normal", o.normal)
                                .setString("groupName", o.groupName));
                    },
                    d -> new TextRenderOptions(d.get(fieldName))
            );
        }
    }

    @Override
    public String toString() {
        return this.componentId;
    }
}
