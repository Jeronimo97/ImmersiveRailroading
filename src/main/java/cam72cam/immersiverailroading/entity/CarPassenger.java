package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.registry.CarPassengerDefinition;

public class CarPassenger extends CarFreight {

    @Override
    public CarPassengerDefinition getDefinition() {
        return super.getDefinition(CarPassengerDefinition.class);
    }

    @Override
    public void onTick() {
        super.onTick();
    }
    
    @Override
    public boolean hasElectricalPower() {
        return getDefinition().hasBattery() || super.hasElectricalPower();
    }
}