package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.mod.gui.container.IContainerBuilder;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import java.util.Map;

public class SteamLocomotiveContainer extends BaseContainer {
    public final LocomotiveSteam stock;
    private final ItemStack template;
    private final ItemStack templateSand;

    public SteamLocomotiveContainer(LocomotiveSteam stock) {
        this.stock = stock;
        this.template = Fuzzy.BUCKET.example();
        this.templateSand = Fuzzy.SAND.example();
    }

    public void draw(IContainerBuilder container, RenderState state){
        /*
        int currY = 0;
        int horizSlots = stock.getInventoryWidth();
        int inventoryRows = (int) Math.ceil(((double)stock.getInventorySize()-2) / horizSlots);
        int slotY = 0;

        currY = container.drawTopBar(0, currY, horizSlots * 2);

        int tankY = currY;
        for (int i = 0; i < inventoryRows; i++) {
            currY = container.drawSlotRow(null, 0, horizSlots * 2, 0, currY);
            if (i == 0) {
                slotY = currY;
            }
        }

        container.drawTankBlock(0, tankY, horizSlots * 2, inventoryRows, stock.getLiquid(), stock.getLiquidAmount() / (float)stock.getTankCapacity().MilliBuckets());

        currY = container.drawBottomBar(0, currY, horizSlots*2);

        int containerY = currY;
        currY = container.drawSlotBlock(stock.cargoItems, 3, stock.getInventoryWidth(), 0, currY);
        Map<Integer, Integer> burnTime = stock.getBurnTime();
        Map<Integer, Integer> burnMax = stock.getBurnMax();
        for (int slot : burnTime.keySet()) {
            int time = stock.getBurnTime().get(slot);
            if (time != 0) {
                float perc = Math.min(1f, (float)time / burnMax.get(slot));

                int xSlot = (slot-2) % horizSlots;
                int ySlot = (slot-2) / horizSlots;


                container.drawSlotOverlay("minecraft:blocks/fire_layer_1", xSlot * 18 + ((horizSlots) * 9), containerY + ySlot * 18, perc, 0x77c64306);
            }
        }

        container.drawSlotOverlay(template, 1, slotY);
        container.drawSlot(stock.cargoItems, 0, 1, slotY);
        container.drawSlot(stock.cargoItems, 1, (horizSlots * 2 - 1) * 18 -1, slotY);

        String quantityStr = String.format("%s/%s", stock.getLiquidAmount(), stock.getTankCapacity().MilliBuckets());
        container.drawCenteredString(quantityStr, 0, slotY);

        currY = container.drawPlayerInventoryConnector(0, currY, horizSlots);
        currY = container.drawPlayerInventory(currY, horizSlots*2);
        drawName(container, stock);
        
        int Ysand = 0;
        Ysand = container.drawTopBar(horizSlots * 45, Ysand, 1);
        container.drawCenteredString("Sand", horizSlots * 30, Ysand - 12);
        Ysand = container.drawSlotRow(stock.cargoItems, 2, 1, horizSlots * 45, Ysand);
        container.drawSlot(stock.cargoItems, 2, horizSlots * 45, Ysand - 18);
        container.drawSlotOverlay(templateSand, horizSlots * 45, Ysand - 18);
        Ysand = container.drawBottomBar(horizSlots * 45, Ysand, 1);
        */
        
        // Init
        int horizSlots = stock.getInventoryWidth();
        int inventoryRows = (int) Math.ceil(((double)stock.getInventorySize()-2) / horizSlots);
        int y = 110;
        
        // Background
        container.drawImage(new Identifier(ImmersiveRailroading.MODID, "gui/steam_engine.png"), 100, -10, 270, 180, 1);
        
        // Boilertank
        int tankX = 0;
        int tankY = -8;
        int tankOverlayY = tankY + 7;
        tankY = container.drawTopBar(tankX, tankY, horizSlots * 2);
        tankY -= 10;
        for (int i = 0; i < 2; i++) {
            tankY = container.drawSlotRow(null, 0, horizSlots * 2, tankX, tankY);
            if (i == 0) {
            }
        }
        container.drawTankBlock(tankX, tankOverlayY, horizSlots * 2, 2, stock.getLiquid(), stock.getLiquidAmount() / (float)stock.getTankCapacity().MilliBuckets());
        String quantityStr = String.format("%s/%s", stock.getLiquidAmount(), stock.getTankCapacity().MilliBuckets());
        container.drawCenteredString(quantityStr, horizSlots + tankX, tankOverlayY + 5);
        tankY = container.drawBottomBar(tankX, tankY, horizSlots * 2);
        tankY -= 26;
        container.drawSlotOverlay(template, tankX + 1, tankY);
        container.drawSlot(stock.cargoItems, 0, tankX + 1, tankY);
        container.drawSlot(stock.cargoItems, 1, tankX + (horizSlots * 2 - 1) * 18 -1, tankY);
        
        // Firebox
        int containerX = 0;
        int containerY = 40;
        containerY = container.drawTopBar(containerX + horizSlots * 9, containerY, horizSlots);
        containerY = container.drawSlotBlock(stock.cargoItems, 3, horizSlots, containerX, containerY - 10);
        containerY = container.drawBottomBar(containerX + horizSlots * 9, containerY, horizSlots);
        Map<Integer, Integer> burnTime = stock.getBurnTime();
        Map<Integer, Integer> burnMax = stock.getBurnMax();
        for (int slot : burnTime.keySet()) {
            int time = stock.getBurnTime().get(slot);
            if (time != 0) {
                float perc = Math.min(1f, (float)time / burnMax.get(slot));

                int xSlot = (slot - 3) % horizSlots;
                int ySlot = (slot - 3) / horizSlots;
                container.drawSlotOverlay("minecraft:blocks/fire_layer_1", xSlot * 18 + ((horizSlots) * 9) + containerX, containerY + ySlot * 18 - 61, perc, 0x77c64306);
            }
        }
        
        // Sand
        int sandX = 115;
        int sandY = 70;
        sandY = container.drawTopBar(sandX, sandY, 1);
        container.drawCenteredString("Sand", horizSlots - 48 + sandX, sandY - 12);
        sandY = container.drawSlotRow(stock.cargoItems, 2, 1, sandX, sandY);
        container.drawSlot(stock.cargoItems, 2, sandX, sandY - 18);
        container.drawSlotOverlay(templateSand, sandX, sandY - 18);
        sandY = container.drawBottomBar(sandX, sandY, 1);
        
        // Player Inventory
        y = container.drawPlayerInventoryConnector(0, y, horizSlots);
        y = container.drawPlayerInventory(y, horizSlots*2);
        container.drawCenteredString(stock.getDefinition().getName(), 0, 115);
    }

    @Override
    public int getSlotsX() {
        return stock.getInventoryWidth() * 2;
    }

    @Override
    public int getSlotsY() {
        return stock.getInventorySize() / stock.getInventoryWidth();
    }
}
