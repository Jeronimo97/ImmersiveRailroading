package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.registry.UnitDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.*;

public class ItemMultipleUnit extends BaseItemRollingStock {

    public ItemMultipleUnit() {
        super(ImmersiveRailroading.MODID, "item_multiple_unit");
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public String getCustomName(ItemStack stack) {
        Data data = new Data(stack);
        UnitDefinition definition = DefinitionManager.getUnit(data.name);
        return definition != null ? definition.name : "";
    }

    @Override
    public List<String> getTooltip(ItemStack stack) {
        List<String> tooltip = new ArrayList<>();

        Data data = new Data(stack);

        UnitDefinition unitDefinition = DefinitionManager.getUnit(data.name);

        int stocks = unitDefinition.unitList.size();

        if (!unitDefinition.tooltips.isEmpty()) {
            tooltip.addAll(unitDefinition.tooltips);
        }

        tooltip.add(GuiText.UNIT_COUNT.toString(stocks));

        Gauge gauge = data.gauge;

        int weight = 0;
        int horsePower = 0;
        int traction = 0;
        double maxSpeed = 0;
        Set<String> works = new HashSet<>();
        for (UnitDefinition.Stock stock : unitDefinition.unitList) {
            EntityRollingStockDefinition definition = stock.definition;

            weight += definition.getWeight(gauge);
            if (definition instanceof LocomotiveDefinition) {
                LocomotiveDefinition locomotiveDefinition = (LocomotiveDefinition) definition;
                works.add(locomotiveDefinition.works);
                if (!locomotiveDefinition.isCabCar()) {
                    horsePower += locomotiveDefinition.getHorsePower(gauge);
                    traction += locomotiveDefinition.getStartingTractionNewtons(gauge);
                    maxSpeed = maxSpeed == 0 ? locomotiveDefinition.getMaxSpeed(gauge).metric() : Math.min(maxSpeed, locomotiveDefinition.getMaxSpeed(gauge).metric());
                }
            }
        }

        if (weight != 0) {
            tooltip.add(GuiText.WEIGHT_TOOLTIP.toString(weight));
        }

        EntityRollingStockDefinition def = data.def;
        if (def != null) {
            tooltip.addAll(def.getModelerTooltip());
        }

        if (!works.isEmpty()) {
            tooltip.add(GuiText.LOCO_WORKS.toString(String.join(", ", works)));
        }

        if (horsePower != 0) {
            tooltip.add(GuiText.LOCO_HORSE_POWER.toString(horsePower));
        }

        if (traction != 0) {
            tooltip.add(GuiText.LOCO_TRACTION.toString(traction));
        }

        if (maxSpeed != 0) {
            tooltip.add(GuiText.LOCO_MAX_SPEED.toString(maxSpeed));
        }


        tooltip.add(GuiText.GAUGE_TOOLTIP.toString(gauge));
        String texture = data.texture;
        if (texture != null && def != null && def.textureNames.get(texture) != null) {
            tooltip.add(GuiText.TEXTURE_TOOLTIP.toString(def.textureNames.get(texture)));
        }

        if (def != null) {
            tooltip.addAll(def.getExtraTooltipInfo());
        }

        return tooltip;
    }

    @Override
    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> items = new ArrayList<>();
        if (creativeTab != null) {
            if (creativeTab.equals(ItemTabs.UNIT_TAB)) {
                for (UnitDefinition unitDefinition : DefinitionManager.getUnits()) {
                    ItemStack stack = new ItemStack(this, 1);
                    Data data = new Data(stack);
                    EntityRollingStockDefinition firstDef = unitDefinition.unitList.getFirst().definition;
                    data.name = unitDefinition.defId;
                    data.def = firstDef;
                    data.gauge = firstDef.recommended_gauge;
                    data.multipleUnit = true;
                    data.write();

                    items.add(stack);
                }
            }
        }

        return items;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.UNIT_TAB);
    }

    @Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
        if (BlockUtil.isIRRail(world, pos)) {
            TileRailBase te = world.getBlockEntity(pos, TileRailBase.class);
            if (te.getAugment() != null) {
                switch(te.getAugment()) {
                    case DETECTOR:
                    case LOCO_CONTROL:
                    case FLUID_LOADER:
                    case FLUID_UNLOADER:
                    case ITEM_LOADER:
                    case ITEM_UNLOADER:
                        if (world.isServer) {
                            ItemRollingStock.Data data = new ItemRollingStock.Data(player.getHeldItem(hand));
                            boolean set = te.setAugmentFilter(data.def != null ? data.def.defID : null);
                            if (set) {
                                player.sendMessage(ChatText.SET_AUGMENT_FILTER.getMessage(data.def != null ? data.def.name() : "Unknown"));
                            } else {
                                player.sendMessage(ChatText.RESET_AUGMENT_FILTER.getMessage());
                            }
                        }
                        return ClickResult.ACCEPTED;
                    default:
                        break;
                }
            }
        }
        return tryPlaceStock(player, world, pos, hand, null);
    }


    public static class Data extends BaseItemRollingStock.Data {
        @TagField(value = "name")
        public String name;

        public Data(ItemStack stack) {
            super(stack);
        }
    }
}
