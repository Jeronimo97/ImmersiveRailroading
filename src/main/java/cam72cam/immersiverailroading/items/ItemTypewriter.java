package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.entity.RenderText;
import cam72cam.immersiverailroading.entity.TextRenderOptions;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.net.Packet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemTypewriter extends CustomItem {
    public ItemTypewriter() {
        super(ImmersiveRailroading.MODID, "item_typewriter");
        Recipes.shapedRecipe(this, 3,
                Fuzzy.IRON_INGOT, Fuzzy.PAPER, Fuzzy.IRON_INGOT,
                Fuzzy.IRON_BLOCK, null, Fuzzy.IRON_BLOCK,
                Fuzzy.WOOD_PLANK, Fuzzy.WOOD_PLANK, Fuzzy.WOOD_PLANK);
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }

    public static void onStockInteract(EntityRollingStock stock, Player player, Player.Hand hand) {
        if (player.getWorld().isClient) {
            GuiTypes.TEXT_FIELD.open(player);
        }
    }

    @Override
    public void onClickAir(Player player, World world, Player.Hand hand) {
        if (world.isServer) {
            player.sendMessage(ChatText.TYPEWRITER_NO_STOCK.getMessage());
        }
    }

    public static class TypewriterPacket extends Packet {
        @TagField("stock")
        public EntityRollingStock stock;
        @TagField(value = "settings", mapper = TextRenderOptions.TextRenderOptionsMapper.class)
        public TextRenderOptions settings;

        public TypewriterPacket() {
        }

        public TypewriterPacket(EntityRollingStock stock, TextRenderOptions settings) {
            this.stock = stock;
            this.settings = settings;

        }

        @Override
        protected void handle() {
            if (settings.global) {
                ((EntityScriptableRollingStock)stock).setTextGlobal(settings);
            } else {
                ((EntityScriptableRollingStock)stock).setText(settings);
            }
            new TypewriterSyncPacket(stock, settings).sendToObserving(stock);
        }
    }

    public static class TypewriterSyncPacket extends Packet {
        @TagField("stock")
        public EntityRollingStock stock;
        @TagField(value = "settings", mapper = TextRenderOptions.TextRenderOptionsMapper.class)
        public TextRenderOptions settings;

        public TypewriterSyncPacket() {

        }

        public TypewriterSyncPacket(EntityRollingStock stock, TextRenderOptions settings) {
            this.stock = stock;
            this.settings = settings;
        }

        @Override
        protected void handle() {
            if (settings.global) {
                ((EntityScriptableRollingStock)stock).setTextGlobal(settings);
            } else {
                ((EntityScriptableRollingStock)stock).setText(settings);
            }
        }
    }
}
