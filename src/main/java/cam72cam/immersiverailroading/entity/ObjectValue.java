package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

import java.util.List;
import java.util.Map;

public class ObjectValue implements DataBlock.Value {
    private final Object object;

    public DataBlock.Value getValueMap(String key, Map<String, DataBlock.Value> getValueLua) {
        return getValueLua.getOrDefault(key, DataBlock.Value.NULL);
    }

    public ObjectValue(Object object) {
        this.object = object;
    }

    @Override
    public Boolean asBoolean() {
        if (object instanceof Boolean) {
            return (Boolean) object;
        }
        return null;
    }

    @Override
    public Integer asInteger() {
        if (object instanceof Integer) {
            return (Integer) object;
        } else if (object instanceof Number) {
            return ((Number) object).intValue();
        }
        return null;
    }

    @Override
    public Float asFloat() {
        if (object instanceof Float) {
            return (Float) object;
        } else if (object instanceof Number) {
            return ((Number) object).floatValue();
        }
        return null;
    }

    @Override
    public Double asDouble() {
        if (object instanceof Double) {
            return (Double) object;
        } else if (object instanceof Number) {
            return ((Number) object).doubleValue();
        }
        return null;
    }

    @Override
    public String asString() {
        if (object instanceof String) {
            return (String) object;
        }
        return object != null ? object.toString() : null;
    }

    @Override
    public Identifier asIdentifier() {
        String value = asString();
        return value != null ? new Identifier(ImmersiveRailroading.MODID, new Identifier(value).getPath()) : null;
    }

    public DataBlock asDataBlock() {
        if (object instanceof DataBlock) {
            return (DataBlock) object;
        }
        return null;
    }

    public List<DataBlock> asDataBlockList() {
        if (object instanceof List) {
            return (List<DataBlock>) object;
        }
        return null;
    }

    public List<DataBlock.Value> asValueList() {
        if (object instanceof List) {
            return (List<DataBlock.Value>) object;
        }
        return null;
    }
}
