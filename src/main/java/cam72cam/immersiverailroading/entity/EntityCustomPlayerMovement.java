package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.floor.CollisionBox;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.floor.NavMesh;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public abstract class EntityCustomPlayerMovement extends EntityRidableRollingStock {

    private static final double STEP_HEIGHT = 0.5;

    @Override
    public Vec3d getMountOffset(Entity passenger, Vec3d off) {
        return super.getMountOffset(passenger, off);
    }

    @Override
    public Vec3d onPassengerUpdate(Entity passenger, Vec3d offset) {
        if (getDefinition().customFloor.root != null) {
            Vec3d targetXZ = movement(passenger.asPlayer(), offset);
            Vec3d rayStart = new Vec3d(targetXZ.x, offset.y + 1.0, targetXZ.z);
            Vec3d rayDir = new Vec3d(0, -1, 0);

            Vec3d localTarget = new Vec3d(targetXZ.z, targetXZ.y, -targetXZ.x);

            CollisionBox rayBox = new CollisionBox(localTarget.subtract(0.5f, 0.5f, 0.5f), localTarget.add(0.5f, 0.5f, 0.5f));
            List<Mesh.Face> nearby = new ArrayList<>();
            NavMesh navMesh = getDefinition().customFloor;
            navMesh.queryBVH(navMesh.root, rayBox, nearby);

            double closestY = Float.NEGATIVE_INFINITY;
            boolean hit = false;

            for(Mesh.Face tri : nearby) {
                Double t = intersectRayTriangle(new Vec3d(rayStart.z, rayStart.y, -rayStart.x), rayDir, tri);
                if (t != null && t >= 0) {
                    Vec3d hitPoint = rayStart.add(rayDir.scale(t));
                    if (!hit || hitPoint.y > closestY) {
                        closestY = hitPoint.y;
                        hit = true;
                    }
                }

            }

            if (hit) {
                offset = new Vec3d(targetXZ.x, closestY, targetXZ.z);
            }
            return offset;
        } else {
            return super.onPassengerUpdate(passenger, offset);
        }
    }

    public Vec3d movement(Player source, Vec3d offset) {
        Vec3d movement = source.getMovementInput();
        if (movement.length() <= 0.1) {
            return offset;
        }

        movement = new Vec3d(movement.x, 0, movement.z).rotateYaw(this.getRotationYaw() - source.getRotationYawHead());

        offset = offset.add(movement);

        if (getDefinition().getModel().getDoors().stream().anyMatch(x -> x.isAtOpenDoor(source, this, Door.Types.EXTERNAL)) &&
                getWorld().isServer &&
                !this.getDefinition().correctPassengerBounds(gauge, offset, shouldRiderSit(source)).equals(offset)
        ) {
            this.removePassenger(source);
        }

        return offset;
    }

    public static Double intersectRayTriangle(Vec3d rayOrigin, Vec3d rayDir, Mesh.Face face) {
        final float EPSILON = 1e-6f;
        
        List<Vec3d> tri = face.vertices;

        Vec3d edge1 = tri.get(1).subtract(tri.get(0));
        Vec3d edge2 = tri.get(2).subtract(tri.get(0));

        Vec3d h = VecUtil.crossProduct(rayDir, edge2);
        double a = VecUtil.dotProduct(edge1, h);

        if (Math.abs(a) < EPSILON) return null;

        double f = 1.0f / a;
        Vec3d s = rayOrigin.subtract(tri.get(0));
        double u = f * VecUtil.dotProduct(s, h);

        if (u < 0.0f || u > 1.0f) return null;

        Vec3d q = VecUtil.crossProduct(s, edge1);
        double v = f * VecUtil.dotProduct(rayDir, q);

        if (v < 0.0f || u + v > 1.0f) return null;

        double t = f * VecUtil.dotProduct(edge2, q);

        return t >= 0 ? t : null;
    }
}
