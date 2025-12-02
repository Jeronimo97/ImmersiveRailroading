package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.world.World;

import java.util.Collections;
import java.util.List;

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

    public static void onStockInteract(EntityScriptableRollingStock stock, Player player, Player.Hand hand) {
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
}
