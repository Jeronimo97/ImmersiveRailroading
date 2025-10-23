package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.ModCore;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class UnitDefinition {
    private final DataBlock block;
    public String defId;
    public String name;
    public Set<String> tooltips = new HashSet<>();
    public LinkedList<Stock> unitList = new LinkedList<>();

    public UnitDefinition(String defId, DataBlock block) {
        this.defId = defId;
        this.block = block;
    }

    public void initDefinitions() {
        if (block == null) {
            return;
        }

        name = block.getValue("name").asString("");

        List<DataBlock.Value> tooltips = block.getValues("add_tooltip");
        if (tooltips != null) {
            this.tooltips = tooltips.stream().map(DataBlock.Value::asString).collect(Collectors.toSet());
        }

        Map<String, Float> controlGroup = Collections.emptyMap();
        DataBlock defaults = block.getBlock("default_CG");
        if (defaults != null) {
            controlGroup = defaults.getValueMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().asFloat()));
        }

        List<DataBlock> consist = block.getBlocks("consist");
        for (DataBlock dataBlock : consist) {
            Map<String, DataBlock.Value> valueMap = dataBlock.getValueMap();
            String stock = valueMap.get("stock").asString();

            EntityRollingStockDefinition stockDef = DefinitionManager.getDefinitions().stream().filter(s -> s.defID.contains(stock)).findFirst().orElseGet(() -> {
                ModCore.warn("RollingStock %s of consist %s doesn't exist, this stock will be skipped!", stock, name);
                return null;
            });

            if (stockDef == null) {
                continue;
            }

            DataBlock.Value text = valueMap.get("texture");

            String texture = text != null ? text.asString() : "";
            Direction flipped = Direction.parse(valueMap.get("direction"));

            Stock s = new Stock(stockDef, flipped, texture, controlGroup);
            unitList.add(s);
        }

    }

    public static class Stock {
        public EntityRollingStockDefinition definition;
        public Direction direction;
        public String texture;
        public Map<String, Float> controlGroup;

        public Stock(EntityRollingStockDefinition stock, Direction direction, @Nullable String texture, Map<String, Float> controlGroup) {
            this.definition = stock;
            this.direction = direction;
            this.texture = texture;
            this.controlGroup = controlGroup;
        }
    }

    public enum Direction{
        DEFAULT,
        FLIPPED,
        RANDOM;

        public static Direction parse(@Nullable DataBlock.Value val) {
            if (val == null) {
                return DEFAULT;
            }

            String str = val.asString("default").toUpperCase();

            Direction dir;
            try {
                dir = Direction.valueOf(str);
            } catch (IllegalArgumentException e) {
                return DEFAULT;
            }

            return dir;
        }

        public boolean getDirection() {
            switch (this) {
                case FLIPPED:
                    return true;
                case RANDOM:
                    Random random = new Random();
                    return random.nextBoolean();
                case DEFAULT:
                default:
                    return false;
            }
        }
    }
}
