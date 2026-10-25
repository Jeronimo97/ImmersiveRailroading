package cam72cam.immersiverailroading.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.entity.physics.Simulation;
import cam72cam.immersiverailroading.items.ItemMultipleUnit;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.UnitDefinition;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.immersiverailroading.thirdparty.trackapi.IRPathingData;
import cam72cam.mod.entity.Player;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.mod.util.DegreeFuncs;
import cam72cam.mod.world.World;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

public class SpawnUtil {
	public static ClickResult placeStock(Player player, Player.Hand hand, World worldIn, Vec3i pos, EntityRollingStockDefinition def, List<ItemComponentType> list) {
		ItemRollingStock.Data data = new ItemRollingStock.Data(player.getHeldItem(hand));

		ITrack initte = ITrack.get(worldIn, new Vec3d(pos).add(0, 0.7, 0), true);
		if (initte == null) {
			return ClickResult.REJECTED;
		}
		double trackGauge = initte.getTrackGauges()[0];
		Gauge gauge = Gauge.from(trackGauge);
		double spawnGauge = gauge.value();
		
		
		if (!player.isCreative() && gauge != data.gauge) {
			player.sendMessage(ChatText.STOCK_WRONG_GAUGE.getMessage());
			return ClickResult.REJECTED;
		}
		
		double offset = def.getCouplerPosition(CouplerType.BACK, gauge) - ConfigDebug.couplerRange;
		@SuppressWarnings("deprecation")
		float yaw = player.getYawHead();

		if (worldIn.isServer) {
			EntityRollingStock stock = def.spawn(worldIn, new Vec3d(pos).add(0.5, 0.1, 0.5), yaw, gauge, data.texture);


			IRPathingData center = new IRPathingData(stock.getPosition(), 0);//only pos is needed
			initte.getNextPosition(center, VecUtil.fromWrongYaw(-0.1, yaw), spawnGauge);
			initte.getNextPosition(center, VecUtil.fromWrongYaw(0.1, yaw), spawnGauge);
			initte.getNextPosition(center, VecUtil.fromWrongYaw(offset, yaw), spawnGauge);
			stock.setPosition(center.getUMCPos());

			if (stock instanceof EntityMoveableRollingStock) {
				EntityMoveableRollingStock moveable = (EntityMoveableRollingStock)stock;
				ITrack centerte = ITrack.get(worldIn, center.getUMCPos(), true);
				if (centerte != null) {
					float frontDistance = moveable.getDefinition().getBogeyFront(gauge);
					float rearDistance = moveable.getDefinition().getBogeyRear(gauge);
					IRPathingData frontTemp = center.clone();
					IRPathingData rearTemp = center.clone();
					centerte.getNextPosition(frontTemp, VecUtil.fromWrongYaw(frontDistance, yaw), spawnGauge);
					centerte.getNextPosition(rearTemp, VecUtil.fromWrongYaw(rearDistance, yaw), spawnGauge);
					Vec3d front = frontTemp.getUMCPos();
					Vec3d rear = rearTemp.getUMCPos();

					moveable.setRotationYaw(VecUtil.toWrongYaw(front.subtract(rear)));
					float pitch = (-VecUtil.toPitch(front.subtract(rear)) - 90);
					if (DegreeFuncs.delta(pitch, 0) > 90) {
						pitch = 180 - pitch;
					}
					moveable.setRotationPitch(pitch);

					moveable.setPosition(rear.add(front.subtract(rear).scale(frontDistance / (frontDistance - rearDistance))));

					ITrack frontte = ITrack.get(worldIn, front, true);
					if (frontte != null) {
						IRPathingData frontNext = new IRPathingData(front, 0);
						frontte.getNextPosition(frontNext, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()), spawnGauge);//only pos is needed to provide
						moveable.setFrontYaw(VecUtil.toWrongYaw(frontNext.getUMCPos().subtract(front)));
						moveable.setFrontRoll((float) -frontNext.getRoll());
					}

					ITrack rearte = ITrack.get(worldIn, rear, true);
					if (rearte != null) {
						IRPathingData rearNext = new IRPathingData(rear, 0);
						rearte.getNextPosition(rearNext, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()), spawnGauge);
						moveable.setRearYaw(VecUtil.toWrongYaw(rearNext.getUMCPos().subtract(rear)));
						moveable.setRearRoll((float) -rearNext.getRoll());
					}

					moveable.setRotationRoll((float) Simulation.calculateRoll(moveable.getFrontRoll(), moveable.getRearRoll()));
				}

				moveable.newlyPlaced = true;
			}

			if (stock instanceof EntityBuildableRollingStock) {
				((EntityBuildableRollingStock)stock).setComponents(list);
			}


			worldIn.spawnEntity(stock);
		}
		if (!player.isCreative()) {
			ItemStack stack = player.getHeldItem(hand);
			stack.setCount(stack.getCount()-1);
			player.setHeldItem(hand, stack);
		}
		return ClickResult.ACCEPTED;
	}
	
	public static ClickResult placeUnit(Player player, Player.Hand hand, World worldIn, Vec3i pos, UnitDefinition unit) {
	    ItemRollingStock.Data data = new ItemRollingStock.Data(player.getHeldItem(hand));
	    Vec3d spawnPos = new Vec3d(pos);

		for (UnitDefinition.Stock rollingStock : unit.unitList) {
			EntityRollingStockDefinition def = rollingStock.definition;
			boolean isFlipped = rollingStock.direction.getDirection();

			List<ItemComponentType> list = def.getItemComponents();

			ITrack initte = ITrack.get(worldIn, spawnPos.add(0, 0.7, 0), true);
			if (initte == null) {
				return ClickResult.REJECTED;
			}
			double trackGauge = initte.getTrackGauges()[0];
			Gauge gauge = Gauge.from(trackGauge);
			double spawnGauge = gauge.value();


			if (!player.isCreative() && gauge != data.gauge) {
				player.sendMessage(ChatText.STOCK_WRONG_GAUGE.getMessage());
				return ClickResult.REJECTED;
			}

			// That's the reason why I don't call placeStock inside this loop
			double offset = def.getCouplerPosition(isFlipped ? CouplerType.FRONT : CouplerType.BACK, gauge) - ConfigDebug.couplerRange;
			@SuppressWarnings("deprecation")
            float yaw = player.getYawHead();

			float originalRot = yaw;
			if (isFlipped) {
				// Flip rotation
				yaw = (originalRot + 180);
			}

			if (worldIn.isServer) {
				String texture = rollingStock.texture != null ? rollingStock.texture : data.texture;

				EntityRollingStock stock = def.spawn(worldIn, spawnPos.add(0.5, 0.1, 0.5), yaw, gauge, texture);

				IRPathingData center = new IRPathingData(stock.getPosition(), 0);//only pos is needed
				initte.getNextPosition(center, VecUtil.fromWrongYaw(-0.1, yaw), spawnGauge);
				initte.getNextPosition(center, VecUtil.fromWrongYaw(0.1, yaw), spawnGauge);
				initte.getNextPosition(center, VecUtil.fromWrongYaw(offset, yaw), spawnGauge);
				stock.setPosition(center.getUMCPos());

				// Set default control group values
				rollingStock.controlGroup.forEach(stock::setControlPosition);

				if (stock instanceof EntityMoveableRollingStock moveable) {
					ITrack centerte = ITrack.get(worldIn, center.getUMCPos(), true);
					if (centerte != null) {
						float frontDistance = moveable.getDefinition().getBogeyFront(gauge);
						float rearDistance = moveable.getDefinition().getBogeyRear(gauge);
						IRPathingData frontTemp = center.clone();
						IRPathingData rearTemp = center.clone();
						centerte.getNextPosition(frontTemp, VecUtil.fromWrongYaw(frontDistance, yaw), spawnGauge);
						centerte.getNextPosition(rearTemp, VecUtil.fromWrongYaw(rearDistance, yaw), spawnGauge);
						Vec3d front = frontTemp.getUMCPos();
						Vec3d rear = rearTemp.getUMCPos();

						moveable.setRotationYaw(VecUtil.toWrongYaw(front.subtract(rear)));
						float pitch = (-VecUtil.toPitch(front.subtract(rear)) - 90);
						if (DegreeFuncs.delta(pitch, 0) > 90) {
							pitch = 180 - pitch;
						}
						moveable.setRotationPitch(pitch);

						moveable.setPosition(rear.add(front.subtract(rear).scale(frontDistance / (frontDistance - rearDistance))));

						ITrack frontte = ITrack.get(worldIn, front, true);
						if (frontte != null) {
							IRPathingData frontNext = new IRPathingData(front, 0);
							frontte.getNextPosition(frontNext, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()), spawnGauge);//only pos is needed to provide
							moveable.setFrontYaw(VecUtil.toWrongYaw(frontNext.getUMCPos().subtract(front)));
							moveable.setFrontRoll((float) -frontNext.getRoll());
						}

						ITrack rearte = ITrack.get(worldIn, rear, true);
						if (rearte != null) {
							IRPathingData rearNext = new IRPathingData(rear, 0);
							rearte.getNextPosition(rearNext, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()), spawnGauge);
							moveable.setRearYaw(VecUtil.toWrongYaw(rearNext.getUMCPos().subtract(rear)));
							moveable.setRearRoll((float) -rearNext.getRoll());
						}
						
						moveable.setRotationRoll((float) Simulation.calculateRoll(moveable.getFrontRoll(), moveable.getRearRoll()));
					}

					moveable.newlyPlaced = true;
				}

				if (stock instanceof EntityBuildableRollingStock) {
					((EntityBuildableRollingStock)stock).setComponents(list);
				}

				if (stock instanceof EntityScriptableRollingStock && !def.textFields.isEmpty()) {
					EntityScriptableRollingStock scriptable  = (EntityScriptableRollingStock) stock;

					String number = null;

					for (TextFieldConfig config : def.textFields.values()) {

						config.setStock(stock);

						if (config.getAvailableFonts() != null) {
							config.setFont(config.getAvailableFonts().get(0));
						}


						if (config.isNumberPlate()) {

							List<String> filter = config.getFilterAsList().stream().filter(s -> !def.inputs.containsValue(Collections.singletonMap(config.getObject(), s))).collect(Collectors.toList());
							if (number == null) {
								Random random = new Random();
								number = filter.get(random.nextInt(filter.size()));
							}

							config.setText(number);
							def.inputs.put(stock.getUUID(), Collections.singletonMap(config.getObject(), number));
						}

						scriptable.initTextField(config);
					}
				}

				Vec3d length = VecUtil.fromWrongYaw(def.getCouplerPosition(isFlipped ? CouplerType.BACK : CouplerType.FRONT, stock.gauge), originalRot);

				// player.sendMessage(PlayerMessage.direct(String.format("Placed stock %s at position %s With offset %s", stock.getDefinition().defID, spawnPos, length)));
				worldIn.spawnEntity(stock);

				// TODO Support for non Straight tracks?? I have no clue how tho
				Vec3d stockPos = stock.getPosition();
                spawnPos = stockPos.add(length);
			}
			if (!player.isCreative()) {
				ItemStack stack = player.getHeldItem(hand);
				stack.setCount(stack.getCount()-1);
				player.setHeldItem(hand, stack);
			}
		}

		return ClickResult.ACCEPTED;
	}
}
