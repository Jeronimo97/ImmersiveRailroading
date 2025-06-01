package cam72cam.immersiverailroading.gui.container;

import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.mod.gui.container.IContainerBuilder;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;

public class DieselLocomotiveContainer extends BaseContainer {
    public final FreightTank stock;
    private final ItemStack template;
    private final ItemStack templateSand;

    public DieselLocomotiveContainer(FreightTank tank) {
        this.stock = tank;
        this.template = Fuzzy.BUCKET.example();
        this.templateSand = Fuzzy.SAND.example();
    }

    public void draw(IContainerBuilder container){
        int currY = 0;
        int horizSlots = 10;
        int inventoryRows = 4;
        int slotY = 0;

        currY = container.drawTopBar(0, currY, horizSlots);

        int tankY = currY;
        for (int i = 0; i < 4; i++) {
            currY = container.drawSlotRow(null, 0, horizSlots, 0, currY);
            if (i == 0) {
                slotY = currY;
            }
        }

        container.drawTankBlock(0, tankY, horizSlots, inventoryRows, stock.getLiquid(), stock.getLiquidAmount() / (float) stock.getTankCapacity().MilliBuckets());

        container.drawSlotOverlay(template, 1, slotY);
        container.drawSlot(stock.cargoItems, 0, 1, slotY);
        container.drawSlot(stock.cargoItems, 1,  1 + horizSlots * 16, slotY);

        String quantityStr = String.format("%s/%s", stock.getLiquidAmount(), stock.getTankCapacity().MilliBuckets());
        container.drawCenteredString(quantityStr, 0, slotY);

        currY = container.drawPlayerInventoryConnector(0, currY, horizSlots);
        currY = container.drawPlayerInventory(currY, horizSlots);
        drawName(container, stock);
        
        int Ysand = 0;
        Ysand = container.drawTopBar(horizSlots * 20, Ysand, 1);
        container.drawCenteredString("Sand", horizSlots * 12, Ysand - 12);
        Ysand = container.drawSlotRow(stock.cargoItems, 2, 1, horizSlots * 20, Ysand);
        container.drawSlot(stock.cargoItems, 2, horizSlots * 20, Ysand - 18);
        container.drawSlotOverlay(templateSand, horizSlots * 20, Ysand - 18);
        Ysand = container.drawBottomBar(horizSlots * 20, Ysand, 1);
    }

    @Override
    public int getSlotsX() {
        return 10;
    }

    @Override
    public int getSlotsY() {
        return 4;
    }
}
