package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.floor.CollisionBox;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.floor.NavMesh;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EntityCustomPlayerMovement extends EntityRidableRollingStock {

    @Override
    public Vec3d getMountOffset(Entity passenger, Vec3d off) {
        NavMesh navMesh = getDefinition().navMesh;
        if (navMesh.hasNavMesh()) {
            Vec3d seat = getSeatPosition(passenger.getUUID());
            if (seat != null) {
                return seat;
            }

            Vec3d realOffset = off.rotateYaw(-90);
            CollisionBox queryBox = new CollisionBox(
                    realOffset.subtract(4f, 4f, 4f),
                    realOffset.add(4f, 4f, 4f)
            );

            List<Mesh.Face> nearby = new ArrayList<>();
            navMesh.queryBVH(navMesh.root, queryBox, nearby);

            Vec3d closestPoint = null;
            double closestDistanceSq = Double.MAX_VALUE;

            for (Mesh.Face tri : nearby) {
                Vec3d p0 = tri.vertices.get(0);
                Vec3d p1 = tri.vertices.get(1);
                Vec3d p2 = tri.vertices.get(2);

                Vec3d pointOnTri = closestPointOnTriangle(realOffset, p0, p1, p2);
                double distSq = realOffset.subtract(pointOnTri).lengthSquared();

                if (distSq < closestDistanceSq) {
                    closestDistanceSq = distSq;
                    closestPoint = pointOnTri;
                }
            }

            if (closestPoint != null) {
                return closestPoint.rotateYaw(90);
            }
        }
        return super.getMountOffset(passenger, off);
    }

    @Override
    public Vec3d onPassengerUpdate(Entity passenger, Vec3d offset) {
        if (getDefinition().navMesh.hasNavMesh()) {
            Vec3d movement = new Vec3d(0, 0, 0);
            if (passenger.isPlayer()) {
                movement = movement(passenger.asPlayer(), offset);
            }
            Vec3d targetXZ = removePitch(movement, this.getRotationPitch());

            Vec3d rayStart = targetXZ.rotateYaw(-90).add(0, 1, 0);
            Vec3d rayDir = new Vec3d(0, -1, 0);

            Vec3d localTarget = targetXZ.rotateYaw(-90);

            CollisionBox rayBox = new CollisionBox(
                    localTarget.subtract(0.5f, 0.5f, 0.5f),
                    localTarget.add(0.5f, 0.5f, 0.5f)
            );
            List<Mesh.Face> nearby = new ArrayList<>();
            NavMesh navMesh = getDefinition().navMesh;
            navMesh.queryBVH(navMesh.root, rayBox, nearby);

            double closestY = Float.NEGATIVE_INFINITY;
            boolean hit = false;

            for(Mesh.Face tri : nearby) {
                Double t = intersectRayTriangle(rayStart, rayDir, tri);
                if (t != null && t >= 0) {
                    Vec3d hitPoint = rayStart.add(rayDir.scale(t));
                    if (!hit || hitPoint.y > closestY) {
                        closestY = hitPoint.y;
                        hit = true;
                    }
                }

            }

            if (hit) {
                offset = reapplyPitch(new Vec3d(targetXZ.x, closestY, targetXZ.z), this.getRotationPitch());
            }

            Vec3d seat = getSeatPosition(passenger.getUUID());
            if (seat != null) {
                offset = seat;
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
        Vec3d localOffset = offset.rotateYaw(-90).add(movement.rotateYaw(-90));

        CollisionBox rayBox = new CollisionBox(
                localOffset.subtract(0.2f, 0.2f, 0.2f),
                localOffset.add(0.2f, 0.2f, 0.2f)
        );
        List<Mesh.Face> nearby = new ArrayList<>();
        NavMesh navMesh = getDefinition().navMesh;
        navMesh.queryBVH(navMesh.collisionRoot, rayBox, nearby);

        Vec3d rayStart = localOffset.add(0, 1, 0);
        Vec3d rayDir = movement.rotateYaw(-90).normalize();

        for (Mesh.Face tri : nearby) {
            Double t = intersectRayTriangle(rayStart, rayDir, tri);
            if (t != null && t >= 0) {
                return offset;
            }
        }

        if (isDoorOpen(offset, movement)) {
            return offset;
        }

        offset = offset.add(movement);

        if (getWorld().isServer) {
            for (Door<?> door : getDefinition().getModel().getDoors()) {
                if (door.isAtOpenDoor(source, this, Door.Types.EXTERNAL)) {
                    Vec3d doorCenter = door.center(this);
                    Vec3d toDoor = doorCenter.subtract(offset).normalize();
                    double dot = VecUtil.dotProduct(toDoor, movement.normalize());
                    if (dot > 0.5) {
                        this.removePassenger(source);
                        break;
                    }
                }
            }
        }

        if (this instanceof EntityCoupleableRollingStock) {
            EntityCoupleableRollingStock coupleable = (EntityCoupleableRollingStock) this;

            boolean isAtFront = isAtCoupler(offset, movement, EntityCoupleableRollingStock.CouplerType.FRONT);
            boolean isAtBack =  isAtCoupler(offset, movement, EntityCoupleableRollingStock.CouplerType.BACK);
            boolean atDoor = isNearestDoorOpen(source);

            isAtFront &= atDoor;
            isAtBack &= atDoor;

            for (EntityCoupleableRollingStock.CouplerType coupler : EntityCoupleableRollingStock.CouplerType.values()) {
                boolean atCoupler = coupler == EntityCoupleableRollingStock.CouplerType.FRONT ? isAtFront : isAtBack;
                if (atCoupler && coupleable.isCoupled(coupler)) {
                    EntityCoupleableRollingStock coupled = ((EntityCoupleableRollingStock) this).getCoupled(coupler);
                    if (coupled != null) {
                        if (coupled.isNearestDoorOpen(source)) {
                            coupled.addPassenger(source);
                        }
                    } else if (this.getTickCount() > 20) {
                        ImmersiveRailroading.info(
                                "Tried to move between cars (%s, %s), but %s was not found",
                                this.getUUID(),
                                coupleable.getCoupledUUID(coupler),
                                coupleable.getCoupledUUID(coupler)
                        );
                    }
                }
            }
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

    private boolean isDoorOpen(Vec3d start, Vec3d end) {
        start = removePitch(start, this.getRotationPitch());
        end  = removePitch(end, this.getRotationPitch());

        start = start.rotateYaw(-90);
        end = start.add(end.rotateYaw(-90));

        List<Door<?>> doors = getDefinition().getModel().getDoors().stream()
                .filter(d -> d.type == Door.Types.INTERNAL || d.type == Door.Types.CONNECTING)
                .filter(d -> !d.isOpen(this)).collect(Collectors.toList());
        boolean intersects = false;
        for (Door<?> door : doors) {
            CollisionBox box = new CollisionBox(
                    door.part.min,
                    door.part.max
            );
            intersects = box.intersects(start, end);
            if (intersects) {
                break;
            }
        }
        return intersects;
    }

    private boolean isAtCoupler(Vec3d offset, Vec3d movement, EntityCoupleableRollingStock.CouplerType type) {
        offset = offset.rotateYaw(-90);
        double coupler = getDefinition().getCouplerPosition(type, this.gauge);
        Vec3d couplerPos = new Vec3d(type == EntityCoupleableRollingStock.CouplerType.FRONT ? coupler : -coupler, offset.y, offset.z);

        CollisionBox queryBox = new CollisionBox(
                couplerPos.subtract(0.2, 0.2, 0.2),
                couplerPos.add(0.2, 0.2, 0.2)
        );

        List<Mesh.Face> nearby = new ArrayList<>();
        NavMesh navMesh = getDefinition().navMesh;
        navMesh.queryBVH(navMesh.root, queryBox, nearby);

        for (Mesh.Face tri : nearby) {
            Vec3d p0 = tri.vertices.get(0);
            Vec3d p1 = tri.vertices.get(1);
            Vec3d p2 = tri.vertices.get(2);

            Vec3d closestPoint = closestPointOnTriangle(offset, p0, p1, p2);
            double distance = offset.subtract(closestPoint).length();
            if (distance < 0.5) {
                Vec3d toCoupler = couplerPos.subtract(offset).normalize();
                double dot = VecUtil.dotProduct(toCoupler, movement.rotateYaw(90).normalize());
                if (dot > 0.5) return true;
            }
        }
        return false;
    }

    public static Vec3d closestPointOnTriangle(Vec3d p, Vec3d p0, Vec3d p1, Vec3d p2) {
        Vec3d edge0 = p1.subtract(p0);
        Vec3d edge1 = p2.subtract(p0);
        Vec3d v0 = p0.subtract(p);

        double a = VecUtil.dotProduct(edge0, edge0);
        double b = VecUtil.dotProduct(edge0, edge1);
        double c = VecUtil.dotProduct(edge1, edge1);
        double d = VecUtil.dotProduct(edge0, v0);
        double e = VecUtil.dotProduct(edge1, v0);

        double det = a * c - b * b;
        double s = b * e - c * d;
        double t = b * d - a * e;

        if (s + t < det) {
            if (s < 0.0) {
                if (t < 0.0) {
                    if (d < 0.0) {
                        s = clamp(-d / a, 0.0, 1.0);
                        t = 0.0;
                    } else {
                        s = 0.0;
                        t = clamp(-e / c, 0.0, 1.0);
                    }
                } else {
                    s = 0.0;
                    t = clamp(-e / c, 0.0, 1.0);
                }
            } else if (t < 0.0) {
                s = clamp(-d / a, 0.0, 1.0);
                t = 0.0;
            } else {
                double invDet = 1.0 / det;
                s *= invDet;
                t *= invDet;
            }
        } else {
            if (s < 0.0) {
                double tmp0 = b + d;
                double tmp1 = c + e;
                if (tmp1 > tmp0) {
                    double numer = tmp1 - tmp0;
                    double denom = a - 2 * b + c;
                    s = clamp(numer / denom, 0.0, 1.0);
                    t = 1 - s;
                } else {
                    t = clamp(-e / c, 0.0, 1.0);
                    s = 0.0;
                }
            } else if (t < 0.0) {
                if ((a + d) > (b + e)) {
                    double numer = c + e - b - d;
                    double denom = a - 2 * b + c;
                    s = clamp(numer / denom, 0.0, 1.0);
                    t = 1 - s;
                } else {
                    s = clamp(-e / c, 0.0, 1.0);
                    t = 0.0;
                }
            } else {
                double numer = c + e - b - d;
                double denom = a - 2 * b + c;
                s = clamp(numer / denom, 0.0, 1.0);
                t = 1.0 - s;
            }
        }

        return p0.add(edge0.scale(s)).add(edge1.scale(t));
    }

    public boolean isBBIntersectingGroup(CollisionBox bb, List<Mesh.Face> faces) {
        Vec3d center = bb.getCenter();
        Vec3d boxHalfSize = new Vec3d(
                (bb.max.x - bb.min.x) * 0.5d,
                (bb.max.y - bb.min.y) * 0.5d,
                (bb.max.z - bb.min.z) * 0.5d
        );

        for (Mesh.Face face : faces) {
            List<Vec3d> tris = face.vertices;

            Vec3d v0 = tris.get(0).subtract(center);
            Vec3d v1 = tris.get(1).subtract(center);
            Vec3d v2 = tris.get(2).subtract(center);

            Vec3d e0 = v1.subtract(v0);
            Vec3d e1 = v2.subtract(v1);
            Vec3d e2 = v0.subtract(v2);

            double minX = Math.min(v0.x, Math.min(v1.x, v2.x));
            double maxX = Math.max(v0.x, Math.max(v1.x, v2.x));
            if (minX > boxHalfSize.x || maxX < -boxHalfSize.x) continue;

            double minY = Math.min(v0.y, Math.min(v1.y, v2.y));
            double maxY = Math.max(v0.y, Math.max(v1.y, v2.y));
            if (minY > boxHalfSize.y || maxY < -boxHalfSize.y) continue;

            double minZ = Math.min(v0.z, Math.min(v1.z, v2.z));
            double maxZ = Math.max(v0.z, Math.max(v1.z, v2.z));
            if (minZ > boxHalfSize.z || maxZ < -boxHalfSize.y) continue;

            Vec3d normal = VecUtil.crossProduct(e0, e1);
            double r = boxHalfSize.x * Math.abs(normal.x) +
                    boxHalfSize.y * Math.abs(normal.y) +
                    boxHalfSize.z * Math.abs(normal.z);

            double s = VecUtil.dotProduct(v0, normal);
            if (Math.abs(s) > r) continue;

            Vec3d axis = new Vec3d(0, -e0.z, e0.y);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(e0.z, 0, -e0.x);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(-e0.y, e0.x, 0);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(0, -e1.z, e1.y);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(e1.z, 0, -e1.x);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(-e1.y, e1.x, 0);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(0, -e2.z, e2.y);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(e2.z, 0, -e2.x);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            axis = new Vec3d(-e2.y, e2.x, 0);
            if (!axisTest(axis, v0, v1, v2, boxHalfSize)) continue;

            return true;
        }
        return false;
    }

    private boolean axisTest(Vec3d axis, Vec3d v0, Vec3d v1, Vec3d v2, Vec3d boxHalfSize) {
        if (axis.lengthSquared() < 1e-6) return true;

        double p0 = VecUtil.dotProduct(v0, axis);
        double p1 = VecUtil.dotProduct(v1, axis);
        double p2 = VecUtil.dotProduct(v2, axis);
        double min = Math.min(p0, Math.min(p1, p2));
        double max = Math.max(p0, Math.max(p1, p2));

        double r = boxHalfSize.x * Math.abs(axis.x) +
                boxHalfSize.y * Math.abs(axis.y) +
                boxHalfSize.z * Math.abs(axis.z);

        return !(min > r || max < -r);
    }

    private Vec3d removePitch(Vec3d vec, double pitchDegrees) {
        double pitch = Math.toRadians(pitchDegrees);
        double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);

        double y = vec.y * cos - vec.z * sin;
        double z = vec.y * sin + vec.z * cos;
        return new Vec3d(vec.x, y, z);
    }

    private Vec3d reapplyPitch(Vec3d vec, double pitchDegrees) {
        double pitch  = Math.toRadians(pitchDegrees);
        double cos = Math.cos(-pitch);
        double sin = Math.sin(-pitch);

        double y = vec.y * cos - vec.z * sin;
        double z = vec.y * sin + vec.z * cos;
        return new Vec3d(vec.x, y, z);
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
