package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.library.unit.PressureDisplayType;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.LocomotiveSteamDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.util.LiquidUtil;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidStack;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.util.*;
import java.util.stream.Collectors;

public class LocomotiveSteam extends Locomotive {
	// PSI
	@TagSync
	@TagField("boiler_bar")
	private float boilerPressureBar = 0;
	
    // BAR
    @TagSync
    @TagField("chest_bar")
    private float chestPressureBar = 0;

	// Celsius
	@TagSync
	@TagField("boiler_temperature")
	private float boilerTemperature;

	@TagSync
	@TagField("pressure_valve")
	private boolean pressureValve = false;
	
	// Map<Slot, TicksToBurn>
	@TagSync
	@TagField(value = "burn_time", mapper = LocomotiveSteam.SlotTagMapper.class)
	private Map<Integer, Integer> burnTime = new HashMap<>();
	@TagSync
	@TagField(value = "burn_max", mapper = LocomotiveSteam.SlotTagMapper.class)
	private Map<Integer, Integer> burnMax = new HashMap<>();

	private float drainRemainder;
	
	public LocomotiveSteam() {
		boilerTemperature = ambientTemperature();
	}

	@Override
	public LocomotiveSteamDefinition getDefinition() {
		return super.getDefinition(LocomotiveSteamDefinition.class);
	}

	@Override
	public boolean openGui(Player player) {
		if (!getDefinition().isCabCar() && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
			GuiTypes.STEAM_LOCOMOTIVE.open(player, this);
			return true;
		}
		return false;
	}

	public float getBoilerTemperature() {
		return boilerTemperature;
	}
	
	private void setBoilerTemperature(float temp) {
		boilerTemperature = temp;
	}
	
	private float getMaxBoilerPSI( ) {
	    return getDefinition().getMaxPSI(gauge);
	}
	
	public float getBoilerPressureBar() {
		return boilerPressureBar;
	}
	
	public float getBoilerPressure() {
	    return boilerPressureBar * PressureDisplayType.BarToPsi / getMaxBoilerPSI();
	}
	
	private void setBoilerPressureBar(float pressure) {
	    boilerPressureBar = pressure;
	}
	
	private void setChestPressureBar(float pressure) {
	    chestPressureBar = pressure;
	}
	
	public float getChestPressureBar() {
        return chestPressureBar;
    }

    public float getChestPressurePsi() {
        return chestPressureBar * PressureDisplayType.BarToPsi;
    }

    public float getMaxChestPressure() {
        if (Config.isFuelRequired(gauge)) {
            if (getBoilerPressureBar() > 0.5f)
                return getBoilerPressureBar() - 0.5f;
            else
                return 0;
        } else
            return getMaxBoilerPSI() * PressureDisplayType.psiToBar - 0.5f;
    }

    public float getMaxChestPressurePsi() {
        return getMaxChestPressure() * PressureDisplayType.BarToPsi;
    }

    public float getChestPressurePercent() {
        return chestPressureBar / (getMaxBoilerPSI() * PressureDisplayType.psiToBar);
    }

	public Map<Integer, Integer> getBurnTime() {
		return burnTime;
	}
	public Map<Integer, Integer> getBurnMax() {
		return burnMax;
	}

	@Override
    public double getAppliedTractiveEffort(final Speed speed) {
        LocomotiveSteamDefinition def = getDefinition();
        if (def.isCabCar())
            return 0;
        
        float reverser = getReverser();
        if (reverser == 0 || getBoilerPressureBar() == 0 && ConfigBalance.FuelRequired)
            return 0;
        
        double speedPerc = speedPercent(speed);
        float absReverser = getAbsReverser(speedPerc, Math.abs(reverser));
        
        double expansion = 1.05 / (absReverser * (absReverser + 0.05));
        double expansionPressure = getChestPressureBar() / expansion * (1 + Math.log(expansion));
        double backPressure = expansionPressure * Math.log(1 + 2.67 * speedPerc
                * absReverser * (def.getCylinderCount() == 3 ? 1.15 : 1));
        double pressurePercent = (expansionPressure - backPressure) / getMaxChestPressure();

        if (pressurePercent <= 0)
            return 0;
        
        return 50445 * def.getCylinderCount() * def.getPistonDiameter(gauge) * def.getPistonDiameter(gauge)
                * def.getPistonStroke(gauge) * getMaxChestPressure() / def.getWheelDiameter(gauge)
                * def.getPowerMultiplier() * Math.pow(pressurePercent, 1.5 * (0.3 * absReverser + 0.7))
                * ConfigBalance.powerMultiplier * Math.copySign(1, reverser);
    }
    
    private float getAbsReverser(double speed, float reverser) {
    	return Config.ImmersionConfig.automaticReverser && speed > 0.05 ? Math.min((float) (-0.3f * Math.log(speed) + 0.28f), reverser) : reverser;
    }
    
	@Override
	public void onDissassemble() {
		super.onDissassemble();
		this.setBoilerTemperature(ambientTemperature());
		this.setBoilerPressureBar(0);
		
		for (Integer slot : burnTime.keySet()) {
			burnTime.put(slot, 0);
		}
	}
    
    private void chestPressureCalc() {
        float pressure = getChestPressureBar();
        float throttle = getThrottle();
        if (throttle == 0 && pressure == 0)
            return;
        double speedPercent = speedPercent(super.getCurrentSpeed());
        // Chest pressure inflow; Idk why but the "* (1 + speedPercent)" is needed
        pressure += 0.06f * Math.sqrt(Config.isFuelRequired(gauge) ? getBoilerPressureBar() : (getMaxBoilerPSI() * PressureDisplayType.psiToBar)) * throttle * (1 + speedPercent);
        // Chest pressure outflow through cylinder drains
        if (cylinderDrainsEnabled()) {
            pressure -= 0.05f;
        }
        // Chest pressure outflow through reverser and speed
        pressure -= 0.095f * pressure * getAbsReverser(speedPercent, Math.abs(getReverser())) * speedPercent;
        // Default chest pressure leakage
        pressure -= 0.005f;
        // Chest pressure outflow through slipping wheels (fast cylinder movement)
        if (slipping) {
            pressure -= Math.abs(0.3f * simulateWheelSlip());
        }
        
        setChestPressureBar(MathUtil.clamp(pressure, 0, getMaxChestPressure()));
    }

	@Override
	public double getTractiveEffortNewtons(Speed speed) {
		return (getDefinition().cab_forward ? -1 : 1) * super.getTractiveEffortNewtons(speed);
	}

    @Override
	protected double simulateWheelSlip() {
		return (getDefinition().cab_forward ? -1 : 1) * super.simulateWheelSlip();
	}


	@Override
	public void onTick() {
		super.onTick();

		if (getWorld().isClient) {
			return;
		}

		if (this.getTickCount() < 2) {
			// Prevent explosions
			return;
		}


		OptionalDouble control = this.getDefinition().getModel().getControls().stream()
				.filter(x -> x.part.type == ModelComponentType.WHISTLE_CONTROL_X)
				.mapToDouble(this::getControlPosition)
				.max();
		if (control.isPresent() && control.getAsDouble() > 0) {
			this.setHorn(10, hornPlayer);
		}

		if (!this.isBuilt() || getDefinition().isCabCar()) {
			return;
		}

		EntityCoupleableRollingStock stock = this;
		CouplerType coupler = getDefinition().cab_forward ? CouplerType.FRONT : CouplerType.BACK;
		while (coupler != null && stock.getCoupled(coupler) instanceof Tender) {
			Tender tender = (Tender) stock.getCoupled(coupler);

			// Only drain 10mb at a time from the tender
			int desiredDrain = 10;
			if (getTankCapacity().MilliBuckets() - getServerLiquidAmount() >= 10) {
				theTank.drain(tender.theTank, desiredDrain, false);
			}

			if (this.getTickCount() % 20 == 0 && this.getDefinition().tender_auto_feed) {
				// Top off stacks
				for (int slot = 2; slot < this.cargoItems.getSlotCount(); slot ++) {
					if (BurnUtil.getBurnTime(this.cargoItems.get(slot)) != 0) {
						for (int tenderSlot = 0; tenderSlot < tender.cargoItems.getSlotCount(); tenderSlot ++) {
							if (this.cargoItems.get(slot).is(tender.cargoItems.get(tenderSlot))) {
								if (this.cargoItems.get(slot).getLimit() > this.cargoItems.get(slot).getCount()) {
									ItemStack extracted = tender.cargoItems.extract(tenderSlot, 1, false);
									this.cargoItems.insert(slot, extracted, false);
								}
							}
						}
					}
				}
			}
			coupler = tender.getCouplerFor(stock);
			if (coupler == null) {
				break;
			}
			coupler = coupler.opposite();
			stock = tender;
		}
		
		float boilerTemperature = getBoilerTemperature();
		float boilerPressurePSI = getBoilerPressureBar() * PressureDisplayType.BarToPsi;
		float waterLevelMB = this.getLiquidAmount();
		int burningSlots = 0;
		float waterUsed = 0;
		
		if (this.getLiquidAmount() > 0) {
			for (int slot = 2; slot < this.cargoItems.getSlotCount(); slot ++) {
				int remainingTime = burnTime.getOrDefault(slot, 0);
				if (remainingTime <= 0) {
					ItemStack stack = this.cargoItems.get(slot);
					if (stack.getCount() <= 0 || BurnUtil.getBurnTime(stack) == 0) {
						continue;
					}
					remainingTime = (int) (BurnUtil.getBurnTime(stack) /gauge.scale() * (Config.ConfigBalance.locoSteamFuelEfficiency / 100.0));
					burnTime.put(slot, remainingTime);
					burnMax.put(slot, remainingTime);
					stack.setCount(stack.getCount()-1);
					this.cargoItems.set(slot, stack);
				} else {
					burnTime.put(slot, remainingTime - 1);
				}
				burningSlots += 1;
			}
		}
		
		double energyKCalDeltaTick = 0;
		
		if (burningSlots != 0 && this.getLiquidAmount() > 0) {
			energyKCalDeltaTick += burningSlots * coalEnergyKCalTick();
		}

		// Assume the boiler is a cube...
		double boilerVolume = this.getTankCapacity().Buckets();
		double boilerEdgeM = Math.pow(boilerVolume, 1.0/3.0);
		double boilerAreaM = 6 * Math.pow(boilerEdgeM, 2);

		if (boilerTemperature > 0) {
			// Decrease temperature due to heat loss
			// Estimate Kw emitter per m^2: (TdegC/10)^2 / 100
			// TODO consider ambientTemperature
			double radiatedKwHr = Math.pow(boilerTemperature/10, 2) / 100 * boilerAreaM * 2;
			double radiatedKCalHr = radiatedKwHr * 859.85;
			double radiatedKCalTick = radiatedKCalHr / 60 / 60 / 20 * ConfigBalance.locoHeatTimeScale;
			energyKCalDeltaTick -= radiatedKCalTick / 1000;
		}
		
		if (energyKCalDeltaTick != 0) {
			// Change temperature
			// 1 KCal raises 1KG water at STP 1 degree
			// 1 KG of water == 1 m^3 of water 
			// TODO what happens when we change liters per mb FluidQuantity.FromMillibuckets((int) waterLevelMB).Liters()
			//  +1 prevents div by zero
			boilerTemperature += energyKCalDeltaTick / ((waterLevelMB + 1) / 1000);
		}
		
	    float maxPSI = getMaxBoilerPSI();
		if (boilerTemperature > 100) {
			// Assume linear relationship between temperature and pressure
			float heatTransfer = boilerTemperature - 100;
			boilerPressurePSI += heatTransfer;

			if (this.getPercentLiquidFull() > 25) {
				boilerTemperature -= heatTransfer;
			}
			
			// Pressure relief valve
			pressureValve = boilerPressurePSI > maxPSI;
			if (pressureValve) {
				waterUsed += boilerPressurePSI - maxPSI;
				boilerPressurePSI = maxPSI;
			}
		} else {
			if (boilerPressurePSI > 0) {
				// Reduce pressure by needed temperature
			    boilerPressurePSI = Math.max(0, boilerPressurePSI - (100 - boilerTemperature));
				boilerTemperature = 100;
			}

			pressureValve = false;
		}
		
		float throttle = getThrottle() * Math.abs(getReverser());
		if (throttle != 0 && boilerPressurePSI > 0) {
			double burnableSlots = this.cargoItems.getSlotCount()-2;
			double maxKCalTick = burnableSlots * coalEnergyKCalTick();
			double maxPressureTick = maxKCalTick / (this.getTankCapacity().MilliBuckets() / 1000);
			maxPressureTick = maxPressureTick * 0.8; // 20% more pressure gen energyCapability to balance heat loss
			
			float delta = (float) (throttle * maxPressureTick);
			
			boilerPressurePSI = MathUtil.clamp(boilerPressurePSI, 0, boilerPressurePSI - delta);
			waterUsed += delta;
		}
		
		if (waterUsed != 0) {
            waterUsed *= Config.ConfigBalance.locoWaterUsage;
            waterUsed += drainRemainder;
            if (waterUsed > 0 && theTank.getContents() != null) {
                theTank.drain(new FluidStack(theTank.getContents().getFluid(), (int) Math.floor(waterUsed)), false);
                drainRemainder = waterUsed % 1;
            }
        }
		
		setBoilerPressureBar(boilerPressurePSI * PressureDisplayType.psiToBar);
		setBoilerTemperature(Math.max(boilerTemperature, ambientTemperature()));
		
		// EXPLOSION
		if (boilerPressurePSI > maxPSI * 1.1 || (boilerPressurePSI > maxPSI * 0.5 && boilerTemperature > 150)) {
			// 10% over max pressure OR
			// Half max pressure and high boiler temperature
			//EXPLODE

			Vec3d pos = this.getPosition();
			if (Config.ConfigDamage.explosionsEnabled) {
				this.createExplosion(pos, boilerPressurePSI / 5, Config.ConfigDamage.explosionEnvDamageEnabled);
			}
			getWorld().removeEntity(this);
		}
        
        if (boilerPressurePSI > 0 || !Config.isFuelRequired(gauge)) {
            chestPressureCalc();
        }
	}

	@Override
	public boolean providesElectricalPower() {
		return getBoilerPressureBar() > 0 || !ConfigBalance.FuelRequired;
	}

    @Override
	public void onDrag(Control<?> component, double newValue) {
		super.onDrag(component, newValue);

		if (component.part.type == ModelComponentType.WHISTLE_CONTROL_X) {
			this.setHorn(10, null);
		}
	}

	@Override
	public void onDragRelease(Control<?> component) {
		super.onDragRelease(component);
		if (component.part.type == ModelComponentType.WHISTLE_CONTROL_X) {
			this.setControlPosition(component, 0);
		}
	}

	@Override
	protected void initContainerFilter() {
		cargoItems.filter.clear();
		this.cargoItems.filter.put(0, SlotFilter.FLUID_CONTAINER);
		this.cargoItems.filter.put(1, SlotFilter.FLUID_CONTAINER);
		this.cargoItems.filter.put(2, SlotFilter.SAND);
		this.cargoItems.defaultFilter = SlotFilter.BURNABLE;
	}

	@Override
	public int getInventorySize() {
		return this.getDefinition().getInventorySize(gauge) + 3;
	}

	@Override
	public int getInventoryWidth() {
		return this.getDefinition().getInventoryWidth(gauge);
	}
	
	@Override
	protected int[] getContainerInputSlots() {
		return new int[] { 0 };
	}
	@Override
	protected int[] getContainertOutputSlots() {
		return new int[] { 1 };
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return this.getDefinition().getTankCapacity(gauge);
	}

	@Override
	public List<Fluid> getFluidFilter() {
		return LiquidUtil.getWater();
	}

	public boolean isOverpressure() {
		return pressureValve;
	}

	private double coalEnergyKCalTick() {
		// Coal density = 800 KG/m3 (engineering toolbox)
		double coalEnergyDensity = 30000; // KJ/KG (engineering toolbox)
		double coalEnergyKJ = coalEnergyDensity / 9; // Assume each slot is burning 1/9th of a coal block
		double coalEnergyBTU = coalEnergyKJ * 0.958; // 1 KJ = 0.958 BTU
		double coalEnergyKCal = coalEnergyBTU / (3.968 * 1000); // 3.968 BTU = 1 KCal
		double coalBurnTicks = 1600; // This is a bit of fudge
		return coalEnergyKCal / coalBurnTicks * ConfigBalance.locoHeatTimeScale;
	}

	private static class SlotTagMapper implements TagMapper<Map<Integer, Integer>> {
		@Override
		public TagAccessor<Map<Integer, Integer>> apply(Class<Map<Integer, Integer>> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, Objects::toString, i -> new TagCompound().setInteger("val", i)),
					d -> d.getMap(fieldName, Integer::parseInt, t -> {
						Integer val = t.getInteger("val");
						if (val == null) {
							val = 0;
						}
						return val;
					})
			);
		}
	}

	public boolean cylinderDrainsEnabled() {
		// This could be optimized to once-per-tick, but I'm not sure that is necessary
		List<Control<?>> drains = getDefinition().getModel().getControls().stream().filter(x -> x.part.type == ModelComponentType.CYLINDER_DRAIN_CONTROL_X).collect(Collectors.toList());
		if (drains.isEmpty()) {
			double csm = Math.abs(super.getCurrentSpeed().metric()) / gauge.scale();
			return csm < 20;
		}

		return drains.stream().anyMatch(c -> getControlPosition(c) > 0.9f);
	}

	public void setCylinderDrains(boolean enabled) {
		// This could be optimized to once-per-tick, but I'm not sure that is necessary
		List<Control<?>> drains = getDefinition().getModel().getControls().stream().filter(x -> x.part.type == ModelComponentType.CYLINDER_DRAIN_CONTROL_X).collect(Collectors.toList());

		for (Control<?> drain : drains) {
			setControlPosition(drain, enabled ? 1 : 0);
		}
	}
}
