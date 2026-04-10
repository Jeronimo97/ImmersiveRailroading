package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.CarPassenger;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

public class CarPassengerDefinition extends CarFreightDefinition {
    
    private boolean hasBattery;

    public CarPassengerDefinition(String defID, DataBlock data) throws Exception {
        super(CarPassenger.class, defID, data);
    }

    @Override
    protected Identifier defaultDataLocation() {
        return new Identifier(ImmersiveRailroading.MODID, "rolling_stock/default/passenger.caml");
    }
    
    @Override
    public void loadData(DataBlock data) throws Exception {
        super.loadData(data);
        
        
        DataBlock properties = data.getBlock("properties");
        this.hasBattery = properties.getValue("has_battery").asBoolean(false);
    }

    @Override
    public boolean acceptsPassengers() {
        return true;
    }

    @Override
    public boolean acceptsLivestock() {
        return false;
    }
    
    public boolean hasBattery() {
        return hasBattery;
    }
    
}
