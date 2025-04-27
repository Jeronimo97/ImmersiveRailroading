package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.model.part.Seat;
import cam72cam.immersiverailroading.registry.WalkableSpaceDefinition;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IRidable;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import org.apache.commons.lang3.tuple.Pair;
import cam72cam.immersiverailroading.registry.WalkableSpaceDefinition.TriangleRegion;

import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityRidableRollingStock extends EntityBuildableRollingStock implements IRidable {
	public float getRidingSoundModifier() {
		return getDefinition().dampeningAmount;
	}

	@TagField(value = "payingPassengerPositions", mapper = PassengerMapper.class)
	private Map<UUID, Vec3d> payingPassengerPositions = new HashMap<>();

	@TagField(value = "seatedPassengers", mapper = SeatedMapper.class)
	@TagSync
	private Map<String, UUID> seatedPassengers = new HashMap<>();

    @TagField(value = "lastPassengerPosition", mapper = PassengerMapper.class)
    @TagSync
    private Map<UUID, Vec3d> lastPassengerPosition = new HashMap<>();

	// Hack to remount players if they were seated
	private Map<UUID, Vec3d> remount = new HashMap<>();

	private WalkableSpaceDefinition walkableSpaceDefinition;
	private Map<UUID, Boolean> isPlayerChangingStock = new HashMap<>();


    @Override
	public ClickResult onClick(Player player, Player.Hand hand) {
		ClickResult clickRes = super.onClick(player, hand);
		if (clickRes != ClickResult.PASS) {
			return clickRes;
		}

		if (player.isCrouching()) {
			return ClickResult.PASS;
		} else if (isPassenger(player)) {
			return ClickResult.PASS;
		} else {
			if (getWorld().isServer) {
				player.startRiding(this);
			}
			return ClickResult.ACCEPTED;
		}
	}

	private Vec3d getSeatPosition(UUID passenger) {
		String seat = seatedPassengers.entrySet().stream()
				.filter(x -> x.getValue().equals(passenger))
				.map(Map.Entry::getKey).findFirst().orElse(null);
		return this.getDefinition().getModel().getSeats().stream()
				.filter(s -> s.part.key.equals(seat))
				.map(s -> new Vec3d(s.part.center.z, s.part.min.y, -s.part.center.x).scale(gauge.scale()).subtract(0, 0.6, 0))
				.findFirst().orElse(null);
	}

	/*
	 * Get the current floor the player is standing on.
	 */

	private Vec3d toModelCoordinates(Vec3d entityCoord) {
		return new Vec3d(entityCoord.z * -1, entityCoord.y, entityCoord.x);
	}

	private Vec3d toEntityCoordinates(Vec3d modelCoord) {
		return new Vec3d(modelCoord.x, modelCoord.y, modelCoord.z * -1);
	}


	private String getCurrentFloor(Vec3d playerPos) {
		double tolerance = 0.05;
		Map<String, Pair<Double, Double>> level = walkableSpaceDefinition.yMap;

		String closestFloor = null;
		double closestDistance = Double.MAX_VALUE;

		for (String floor : level.keySet()) {
			Pair<Double, Double> heightRange = level.get(floor);
			double minHeight = heightRange.getLeft();
			double maxHeight = heightRange.getRight();

			double adjustedMin = minHeight - tolerance;
			double adjustedMax = maxHeight + tolerance;

			if (playerPos.y >= adjustedMin && playerPos.y <= adjustedMax) {
				double distanceToMin = Math.abs(playerPos.y - minHeight);
				double distanceToMax = Math.abs(playerPos.y - maxHeight);
				double preferredDistance = Math.min(distanceToMin, distanceToMax);

				if (preferredDistance <= closestDistance) {
					closestDistance = preferredDistance;
					closestFloor = floor;
				}
			}
		}

		return closestFloor != null ? closestFloor : level.keySet().iterator().next();
	}

	public Vec3d restrictMovement(Vec3d playerPosition, Vec3d playerVelocity, List<Vec3d> currentTriangle, Map<Vec3d[], List<Vec3d>> adjacentTriangles) {
		Vec3d normal = calculateNormal(currentTriangle);
		Vec3d projectedVelocity = projectOntoPlane(playerVelocity, normal);

		for (int i = 0; i < currentTriangle.size(); i++) {
			Vec3d edgeStart = currentTriangle.get(i);
			Vec3d edgeEnd = currentTriangle.get((i + 1) % currentTriangle.size());

			if (isCollidingWithEdge(playerPosition, projectedVelocity, edgeStart, edgeEnd)) {
				Vec3d edgeDirection = edgeEnd.subtract(edgeStart).normalize();
				projectedVelocity = WalkableSpaceDefinition.projectOnto(projectedVelocity, edgeDirection);

				// Check if there's an adjacent triangle along this edge
				Vec3d[] edgeKey = new Vec3d[]{edgeStart, edgeEnd};
				if (adjacentTriangles.containsKey(edgeKey)) {
					// Transition to adjacent triangle for continuous movement
					List<Vec3d> adjacentTriangle = adjacentTriangles.get(edgeKey);
					return restrictMovement(playerPosition.add(projectedVelocity), projectedVelocity, adjacentTriangle, adjacentTriangles);
				}
			}
		}
		return playerPosition.add(projectedVelocity);
	}

	private Vec3d restrictPlayerMovement(Vec3d playerPos, Vec3d playerMovement) {
		if (getHeightAtPlayerPosition(playerMovement.subtract(0, Math.sin(Math.toRadians(this.getRotationPitch())) * playerMovement.z, 0)) != -1) {
			return playerMovement;
		} else {
			if (getHeightAtPlayerPosition(new Vec3d(playerMovement.x, (playerMovement.y -0.3) - (Math.sin(Math.toRadians(this.getRotationPitch())) * playerMovement.z), playerMovement.z)) != -1) {
				return playerMovement;
			} else {
				return playerPos;
			}
		}
	}


	private boolean isWithinTriangle(Vec3d playerPosition, List<Vec3d> triangleVertices) {
		Vec3d v1 = triangleVertices.get(0);
		Vec3d v2 = triangleVertices.get(1);
		Vec3d v3 = triangleVertices.get(2);
		return walkableSpaceDefinition.isPlayerInsideTriangle(playerPosition, v1, v2, v3);
	}

	private Vec3d restrictOnEdge(Vec3d velocity, Vec3d playerPos, Vec3d[] edge, Map<int[], List<Vec3d>> walkableTriangles) {
		if (isBoundaryEdge(edge, walkableTriangles) && isCollidingWithEdge(playerPos, velocity, edge[0], edge[1])) {
			// Restrict velocity along the direction of the edge
			return WalkableSpaceDefinition.projectOnto(velocity, edge[1].subtract(edge[0]));
		}
		return velocity;
	}

	// Check if the player's movement is within the edge boundaries
	private boolean isCollidingWithEdge(Vec3d playerPos, Vec3d velocity, Vec3d edgeStart, Vec3d edgeEnd) {
		Vec3d edgeVector = edgeEnd.subtract(edgeStart);
		double edgeLengthSquared = edgeVector.lengthSquared();

		Vec3d playerToEdgeStart = playerPos.subtract(edgeStart);
		double projection = dotProduct(playerToEdgeStart, edgeVector) / edgeLengthSquared;

		// Ensure projection falls within edge boundaries
		if (projection < 0 || projection > 1) {
			return false;
		}

		Vec3d closestPointOnEdge = edgeStart.add(edgeVector.scale(projection));
		double distanceToEdge = playerPos.distanceTo(closestPointOnEdge);

		// Check if player is close enough to be considered "colliding"
		return distanceToEdge < 0.1; // Adjust threshold for tighter control
	}



	// Method to calculate the normal vector of a triangle
	private Vec3d calculateNormal(List<Vec3d> triangleVertices) {
		Vec3d v0 = triangleVertices.get(1).subtract(triangleVertices.get(0));
		Vec3d v1 = triangleVertices.get(2).subtract(triangleVertices.get(0));
		return crossProduct(v0, v1).normalize();
	}

	// Project a vector onto a plane defined by its normal
	private Vec3d projectOntoPlane(Vec3d vector, Vec3d normal) {
		double dotProduct = dotProduct(vector, normal);
		return vector.subtract(normal.scale(dotProduct)); // Removes component along the normal
	}

	public double dotProduct(Vec3d v1, Vec3d v2) {
		return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
	}

	private boolean isBoundaryEdge(Vec3d[] edge, Map<int[], List<Vec3d>> walkableTriangles) {
		int sharedCount = 0;

		// Iterate through triangles to check if this edge is shared with any other triangle
		for (List<Vec3d> triangle : walkableTriangles.values()) {
			if (triangleContainsEdge(triangle, edge)) {
				sharedCount++;
				if (sharedCount > 1) return false; // More than one triangle shares this edge
			}
		}

		return true; // Edge is a boundary if it is not shared with another triangle
	}

	private boolean triangleContainsEdge(List<Vec3d> triangle, Vec3d[] edge) {
		return (triangle.contains(edge[0]) && triangle.contains(edge[1]));
	}

	// Method to determine if the player is currently within a triangle
	public List<Vec3d> getCurrentTriangle(Vec3d playerPosition, List<TriangleRegion> regions) {
		playerPosition = toModelCoordinates(playerPosition);

		for (TriangleRegion region : regions) {
			if (region.isWithinBounds(playerPosition)) {
				for (List<Vec3d> triangle : region.triangles) {
					if (walkableSpaceDefinition.isPlayerInsideTriangle(playerPosition, triangle.get(0), triangle.get(1), triangle.get(2))) {
						return triangle;
					}
				}
			}
		}
		return new ArrayList<>();
	}


	// Vec3d helper method
	private Vec3d crossProduct(Vec3d vec1, Vec3d vec2) {
		double crossX = vec1.y * vec2.z - vec1.z * vec2.y;
		double crossY = vec1.z * vec2.x - vec1.x * vec2.z;
		double crossZ = vec1.x * vec2.y - vec1.y * vec2.x;
		return new Vec3d(crossX, crossY, crossZ);
	}

	/*
	 * get the y-offset for the player Position
	 */

	public double getHeightAtPlayerPosition(Vec3d playerPosition) {
		playerPosition = new Vec3d(playerPosition.z * - 1, playerPosition.y, playerPosition.x);
		String floor = getCurrentFloor(playerPosition);
		Map<int[], List<Vec3d>> faces = walkableSpaceDefinition.floorMap.get(floor);
		for (int[] faceIndices : faces.keySet()) {
			List<Vec3d> vertices = faces.get(faceIndices);
			if (vertices.size() >= 3) {
				Vec3d v1 = vertices.get(0);
				Vec3d v2 = vertices.get(1);
				Vec3d v3 = vertices.get(2);
				if (walkableSpaceDefinition.isPlayerInsideTriangle(playerPosition, v1, v2, v3)) {
					return walkableSpaceDefinition.calculateHeightInTriangle(playerPosition, v1, v2, v3);
				}
			}
		}
		return -1;
	}


	@Override
	public Vec3d getMountOffset(Entity passenger, Vec3d off) {
	    if (walkableSpaceDefinition == null) {
	        walkableSpaceDefinition = getDefinition().walkableSpaceDefinition;
	    }
		if (passenger.isVillager() && !payingPassengerPositions.containsKey(passenger.getUUID())) {
			payingPassengerPositions.put(passenger.getUUID(), passenger.getPosition());
		}

		if (walkableSpaceDefinition == null) {
			walkableSpaceDefinition = getDefinition().walkableSpaceDefinition;
		}

		if (passenger.isVillager() && !seatedPassengers.containsValue(passenger.getUUID())) {
			for (Seat<?> seat : getDefinition().getModel().getSeats()) {
				if (!seatedPassengers.containsKey(seat.part.key)) {
					seatedPassengers.put(seat.part.key, passenger.getUUID());
					break;
				}
			}
		}

		if (!walkableSpaceDefinition.yMap.isEmpty() && isPlayerChangingStock.containsKey(passenger.getUUID())) {
			WalkableSpaceDefinition.Point point = walkableSpaceDefinition.getNearestWalkablePoint(new Vec3d(off.z * -1, off.y, off.x));
			double xOffset = isPlayerChangingStock.get(passenger.getUUID()) ? -0.4d : 0.4d;
			isPlayerChangingStock.clear();
			off = new Vec3d(point.point.z, point.point.y, point.point.x * -1).add(0, 0, xOffset);
			double yOffset = getHeightAtPlayerPosition(off);
			off = new Vec3d(off.x, yOffset, off.z);
			return off;
		}

		Vec3d seat = getSeatPosition(passenger.getUUID());
		if (seat != null) {
			return seat;
		}

		int wiggle = passenger.isVillager() ? 10 : 0;
		off = off.add((Math.random()-0.5) * wiggle, 0, (Math.random()-0.5) * wiggle);

		double yOffset = walkableSpaceDefinition.yMap.isEmpty() ? off.y : getHeightAtPlayerPosition(off);
		if (!walkableSpaceDefinition.yMap.isEmpty()) {
			WalkableSpaceDefinition.Point point = walkableSpaceDefinition.getNearestWalkablePoint(new Vec3d(off.z * -1, off.y, off.x));
			off = new Vec3d(point.point.z, point.point.y, point.point.x * -1);
			yOffset = getHeightAtPlayerPosition(off);
			off = new Vec3d(off.x, yOffset, off.z);
			return off;
		}

		off = this.getDefinition().correctPassengerBounds(gauge, off, shouldRiderSit(passenger));

		off = new Vec3d(off.x, yOffset, off.z);

		return off;
	}

	@Override
	public boolean canFitPassenger(Entity passenger) {
		if (passenger instanceof Player && !((Player) passenger).hasPermission(Permissions.BOARD_STOCK)) {
			return false;
		}
		return getPassengerCount() < this.getDefinition().getMaxPassengers();
	}
	
	@Override
	public boolean shouldRiderSit(Entity passenger) {
		boolean nonSeated = this.getDefinition().shouldSit != null ? this.getDefinition().shouldSit : this.gauge.shouldSit();
		return nonSeated || this.seatedPassengers.containsValue(passenger.getUUID());
	}

	@Override
	public Vec3d onPassengerUpdate(Entity passenger, Vec3d offset) {
		if (passenger.isPlayer()) {
			offset = playerMovement(passenger.asPlayer(), offset);
		}

		double yOffset = 1;

		if (walkableSpaceDefinition == null) {
			walkableSpaceDefinition = getDefinition().walkableSpaceDefinition;
		}

		if (passenger.getWorld().isClient && !walkableSpaceDefinition.yMap.isEmpty()) {
			yOffset = getHeightAtPlayerPosition(offset.subtract(0, Math.sin(Math.toRadians(this.getRotationPitch())) * (offset.z), 0));
		}

		Vec3d seat = getSeatPosition(passenger.getUUID());
		if (seat != null) {
			offset = seat;
		} else {
			offset = this.getDefinition().correctPassengerBounds(gauge, offset, shouldRiderSit(passenger));
		}
		offset = offset.add(0, Math.sin(Math.toRadians(this.getRotationPitch())) * offset.z, 0);

		if (seat == null && !walkableSpaceDefinition.yMap.isEmpty()) {
			offset = new Vec3d(offset.x, yOffset, offset.z);
			offset = offset.add(0, Math.sin(Math.toRadians(this.getRotationPitch())) * offset.z, 0);
		}
		return offset;
	}

	private boolean isIntDoorOpen(Vec3d pos, Vec3d velocity) {
		boolean isIntersecting;

		String floor = getCurrentFloor(pos);
		Pair<Double, Double> height = walkableSpaceDefinition.yMap.get(floor);
		double yOffset = (height.getRight() + height.getLeft()) / 2;

		pos = new Vec3d(pos.z * -1, yOffset, pos.x);

		for (Door door : this.getDefinition().getModel().getDoors()) {
			if (door.type == Door.Types.CONNECTING || door.type == Door.Types.INTERNAL) {
				WalkableSpaceDefinition.BoundingBox boundingBox = new WalkableSpaceDefinition.BoundingBox(door.part.min, door.part.max);
				double minY = Math.min(door.part.min.y, door.part.max.y);
				if (getCurrentFloor(new Vec3d(door.part.center.x, minY, door.part.center.z)).equals(floor)) {
					isIntersecting = boundingBox.doesMovementCross(pos, new Vec3d(velocity.z * -1, velocity.y, velocity.x));
					if (isIntersecting && !door.isOpen(this)) {
						return true;
					}
				}
			}
		}

		return false;
	}


	private boolean isNearestDoorOpen(Player source) {
		// Find any doors that are close enough that are closed (and then negate)
		return !this.getDefinition().getModel().getDoors().stream()
				.filter(d -> d.type == Door.Types.CONNECTING)
				.filter(d -> d.center(this).distanceTo(source.getPosition()) < getDefinition().getLength(this.gauge)/3)
				.min(Comparator.comparingDouble(d -> d.center(this).distanceTo(source.getPosition())))
				.filter(x -> !x.isOpen(this))
				.isPresent();
	}

	private boolean isNearestInteriorDoorOpen(Player source) {
		return !this.getDefinition().getModel().getDoors().stream()
				.filter(d -> d.type == Door.Types.CONNECTING || d.type == Door.Types.INTERNAL)
				.filter(d -> d.center(this).distanceTo(source.getPosition()) < getDefinition().getLength(this.gauge)/3)
				.min(Comparator.comparingDouble(d -> d.center(this).distanceTo(source.getPosition())))
				.filter(x -> !x.isOpen(this))
				.isPresent();
	}

	private Vec3d playerMovement(Player source, Vec3d offset) {
		Vec3d movement = source.getMovementInput();
        /*
        if (sprinting) {
            movement = movement.scale(3);
        }
        */

        if (movement.length() < 0.1) {
            return offset;
        }

        movement = new Vec3d(movement.x, 0, movement.z).rotateYaw(this.getRotationYaw() - source.getRotationYawHead());

		Vec3d offsetM = offset.add(movement);

		if (!walkableSpaceDefinition.yMap.isEmpty()) {
			if (isIntDoorOpen(offset, movement)) {
				return offset;
			} else {
				offset = restrictPlayerMovement(offset, offsetM);
			}
		}else {
			offset = offsetM;
		}
        if (this instanceof EntityCoupleableRollingStock) {
			EntityCoupleableRollingStock couplable = (EntityCoupleableRollingStock) this;

			boolean atFront = this.getDefinition().isAtFront(gauge, offset);
			boolean atBack = this.getDefinition().isAtRear(gauge, offset);
			if (!walkableSpaceDefinition.yMap.isEmpty()) {
				atFront = this.walkableSpaceDefinition.isAtRearNew(gauge, offset);
				atBack = this.walkableSpaceDefinition.isAtFrontNew(gauge, offset);
			}
			// TODO config for strict doors
			boolean atDoor = isNearestDoorOpen(source);

			atFront &= atDoor;
			atBack &= atDoor;

			for (CouplerType coupler : CouplerType.values()) {
				boolean atCoupler = coupler == CouplerType.FRONT ? atFront : atBack;
				if (atCoupler && couplable.isCoupled(coupler)) {
					EntityCoupleableRollingStock coupled = ((EntityCoupleableRollingStock) this).getCoupled(coupler);
					if (coupled != null) {
						if (((EntityRidableRollingStock)coupled).isNearestDoorOpen(source)) {
							coupled.addPassenger(source);
							isPlayerChangingStock.put(source.getUUID(), atFront);
						}
					} else if (this.getTickCount() > 20) {
						ImmersiveRailroading.info(
								"Tried to move between cars (%s, %s), but %s was not found",
								this.getUUID(),
								couplable.getCoupledUUID(coupler),
								couplable.getCoupledUUID(coupler)
						);
					}
					return offset;
				}
			}
        }

        if (getDefinition().getModel().getDoors().stream().anyMatch(x -> x.isAtOpenDoor(source, this, Door.Types.EXTERNAL)) &&
				getWorld().isServer &&
				!this.getDefinition().correctPassengerBounds(gauge, offset, shouldRiderSit(source)).equals(offset)
		) {
        	this.removePassenger(source);
		}

		return offset;
	}

	@Override
	public void onTick() {
		if (walkableSpaceDefinition == null) {
			walkableSpaceDefinition = getDefinition().walkableSpaceDefinition;
		}
		super.onTick();

		if (getWorld().isServer) {
			remount.forEach((uuid, pos) -> {
				Player player = getWorld().getEntity(uuid, Player.class);
				if (player != null) {
					player.setPosition(pos);
					player.startRiding(this);
				}
			});
			remount.clear();
			for (Player source : getWorld().getEntities(Player.class)) {
				if (source.getRiding() == null && getDefinition().getModel().getDoors().stream().anyMatch(x -> x.isAtOpenDoor(source, this, Door.Types.EXTERNAL))) {
					this.addPassenger(source);
				}
			}
		}
	}

	public Vec3d onDismountPassenger(Entity passenger, Vec3d offset) {
		List<String> seats = seatedPassengers.entrySet().stream().filter(x -> x.getValue().equals(passenger.getUUID()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
		if (!seats.isEmpty()) {
			seats.forEach(seatedPassengers::remove);
			if (getWorld().isServer && passenger.isPlayer()) {
				remount.put(passenger.getUUID(), passenger.getPosition());
			}
		}

		//TODO calculate better dismount offset
		offset = new Vec3d(Math.copySign(getDefinition().getWidth(gauge)/2 + 1, offset.x), 0, offset.z);

		if (getWorld().isServer && passenger.isVillager() && payingPassengerPositions.containsKey(passenger.getUUID())) {
			double distanceMoved = passenger.getPosition().distanceTo(payingPassengerPositions.get(passenger.getUUID()));

			int payout = (int) Math.floor(distanceMoved * Config.ConfigBalance.villagerPayoutPerMeter);

			List<ItemStack> payouts = Config.ConfigBalance.getVillagerPayout();
			if (payouts.size() != 0) {
				int type = (int)(Math.random() * 100) % payouts.size();
				ItemStack stack = payouts.get(type).copy();
				stack.setCount(payout);
				getWorld().dropItem(stack, getBlockPosition());
				// TODO drop by player or new pos?
			}
			payingPassengerPositions.remove(passenger.getUUID());
		}

		return offset;
	}

	public void onSeatClick(String seat, Player player) {
		List<String> seats = seatedPassengers.entrySet().stream().filter(x -> x.getValue().equals(player.getUUID()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
		if (!seats.isEmpty()) {
			seats.forEach(seatedPassengers::remove);
			return;
		}

		seatedPassengers.put(seat, player.getUUID());
		lastPassengerPosition.put(player.getUUID(), player.getPosition().subtract(this.getPosition()).rotateYaw(this.getRotationYaw()));
	}

	private static class PassengerMapper implements TagMapper<Map<UUID, Vec3d>> {
		@Override
		public TagAccessor<Map<UUID, Vec3d>> apply(Class<Map<UUID, Vec3d>> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, UUID::toString, (Vec3d pos) -> new TagCompound().setVec3d("pos", pos)),
					d -> d.getMap(fieldName, UUID::fromString, t -> t.getVec3d("pos"))
			);
		}
	}

	private static class SeatedMapper implements TagMapper<Map<String, UUID>> {
		@Override
		public TagAccessor<Map<String, UUID>> apply(Class<Map<String, UUID>> type, String fieldName, TagField tag) throws SerializationException {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, i -> i, u -> new TagCompound().setUUID("uuid", u)),
					d -> d.getMap(fieldName, i -> i, t -> t.getUUID("uuid"))
			);
		}
	}
}
