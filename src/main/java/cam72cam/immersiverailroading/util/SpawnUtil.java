package cam72cam.immersiverailroading.util;

import java.util.*;
import java.util.stream.Collectors;

import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.Config.ConfigDebug;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.UnitDefinition;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
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
		double trackGauge = initte.getTrackGauge();
		Gauge gauge = Gauge.from(trackGauge);
		
		
		if (!player.isCreative() && gauge != data.gauge) {
			player.sendMessage(ChatText.STOCK_WRONG_GAUGE.getMessage());
			return ClickResult.REJECTED;
		}
		
		double offset = def.getCouplerPosition(CouplerType.BACK, gauge) - ConfigDebug.couplerRange;
		float yaw = player.getYawHead();

		if (worldIn.isServer) {
			EntityRollingStock stock = def.spawn(worldIn, new Vec3d(pos).add(0.5, 0.1, 0.5), yaw, gauge, data.texture);

			Vec3d center = stock.getPosition();
			center = initte.getNextPosition(center, VecUtil.fromWrongYaw(-0.1, yaw));
			center = initte.getNextPosition(center, VecUtil.fromWrongYaw(0.1, yaw));
			center = initte.getNextPosition(center, VecUtil.fromWrongYaw(offset, yaw));
			stock.setPosition(center);

			if (stock instanceof EntityMoveableRollingStock) {
				EntityMoveableRollingStock moveable = (EntityMoveableRollingStock)stock;
				ITrack centerte = ITrack.get(worldIn, center, true);
				if (centerte != null) {
					float frontDistance = moveable.getDefinition().getBogeyFront(gauge);
					float rearDistance = moveable.getDefinition().getBogeyRear(gauge);
					Vec3d front = centerte.getNextPosition(center, VecUtil.fromWrongYaw(frontDistance, yaw));
					Vec3d rear = centerte.getNextPosition(center, VecUtil.fromWrongYaw(rearDistance, yaw));

					moveable.setRotationYaw(VecUtil.toWrongYaw(front.subtract(rear)));
					float pitch = (-VecUtil.toPitch(front.subtract(rear)) - 90);
					if (DegreeFuncs.delta(pitch, 0) > 90) {
						pitch = 180 - pitch;
					}
					moveable.setRotationPitch(pitch);

					moveable.setPosition(rear.add(front.subtract(rear).scale(frontDistance / (frontDistance - rearDistance))));

					ITrack frontte = ITrack.get(worldIn, front, true);
					if (frontte != null) {
						Vec3d frontNext = frontte.getNextPosition(front, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()));
						moveable.setFrontYaw(VecUtil.toWrongYaw(frontNext.subtract(front)));
					}

					ITrack rearte = ITrack.get(worldIn, rear, true);
					if (rearte != null) {
						Vec3d rearNext = rearte.getNextPosition(rear, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()));
						moveable.setRearYaw(VecUtil.toWrongYaw(rearNext.subtract(rear)));
					}
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
		Vec3d spawnPos = new Vec3d(pos);

		for (UnitDefinition.Stock rollingStock : unit.unitList) {
			EntityRollingStockDefinition def = rollingStock.definition;
			boolean isFlipped = rollingStock.direction.getDirection();

			List<ItemComponentType> list = def.getItemComponents();

			ItemRollingStock.Data data = new ItemRollingStock.Data(player.getHeldItem(hand));

			ITrack initte = ITrack.get(worldIn, spawnPos.add(0, 0.7, 0), true);
			if (initte == null) {
				return ClickResult.REJECTED;
			}
			double trackGauge = initte.getTrackGauge();
			Gauge gauge = Gauge.from(trackGauge);


			if (!player.isCreative() && gauge != data.gauge) {
				player.sendMessage(ChatText.STOCK_WRONG_GAUGE.getMessage());
				return ClickResult.REJECTED;
			}

			// That's the reason why I don't call placeStock inside this loop
			double offset = def.getCouplerPosition(isFlipped ? CouplerType.FRONT : CouplerType.BACK, gauge) - ConfigDebug.couplerRange;
			float yaw = player.getYawHead();

			float originalRot = yaw;
			if (isFlipped) {
				// Flip rotation
				yaw = (originalRot + 180);
			}

			if (worldIn.isServer) {
				String texture = rollingStock.texture != null ? rollingStock.texture : data.texture;

				EntityRollingStock stock = def.spawn(worldIn, spawnPos.add(0.5, 0.1, 0.5), yaw, gauge, texture);

				Vec3d center = stock.getPosition();
				center = initte.getNextPosition(center, VecUtil.fromWrongYaw(-0.1, originalRot));
				center = initte.getNextPosition(center, VecUtil.fromWrongYaw(0.1, originalRot));
				center = initte.getNextPosition(center, VecUtil.fromWrongYaw(offset, originalRot));
				stock.setPosition(center);

				// Set default control group values
				rollingStock.controlGroup.forEach(stock::setControlPosition);

				if (stock instanceof EntityMoveableRollingStock) {
					EntityMoveableRollingStock moveable = (EntityMoveableRollingStock)stock;
					ITrack centerte = ITrack.get(worldIn, center, true);
					if (centerte != null) {
						float frontDistance = moveable.getDefinition().getBogeyFront(gauge);
						float rearDistance = moveable.getDefinition().getBogeyRear(gauge);
						Vec3d front = centerte.getNextPosition(center, VecUtil.fromWrongYaw(frontDistance, yaw));
						Vec3d rear = centerte.getNextPosition(center, VecUtil.fromWrongYaw(rearDistance, yaw));

						moveable.setRotationYaw(VecUtil.toWrongYaw(front.subtract(rear)));
						float pitch = (-VecUtil.toPitch(front.subtract(rear)) - 90);
						if (DegreeFuncs.delta(pitch, 0) > 90) {
							pitch = 180 - pitch;
						}
						moveable.setRotationPitch(pitch);


						moveable.setPosition(rear.add(front.subtract(rear).scale(frontDistance / (frontDistance - rearDistance))));

						ITrack frontte = ITrack.get(worldIn, front, true);
						if (frontte != null) {
							Vec3d frontNext = frontte.getNextPosition(front, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()));
							moveable.setFrontYaw(VecUtil.toWrongYaw(frontNext.subtract(front)));
						}

						ITrack rearte = ITrack.get(worldIn, rear, true);
						if (rearte != null) {
							Vec3d rearNext = rearte.getNextPosition(rear, VecUtil.fromWrongYaw(0.1 * gauge.scale(), moveable.getRotationYaw()));
							moveable.setRearYaw(VecUtil.toWrongYaw(rearNext.subtract(rear)));
						}
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

//				player.sendMessage(PlayerMessage.direct(String.format("Placed stock %s at position %s With offset %s", stock.getDefinition().defID, spawnPos, length)));
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
