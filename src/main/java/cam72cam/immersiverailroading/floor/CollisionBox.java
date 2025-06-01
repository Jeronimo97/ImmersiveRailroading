package cam72cam.immersiverailroading.floor;

import cam72cam.mod.math.Vec3d;

import java.util.List;

public class CollisionBox {
    public Vec3d min;
    public Vec3d max;

    public CollisionBox(Vec3d p1, Vec3d p2) {
        this.min = p1.min(p2);
        this.max = p1.max(p2);
    }

    public CollisionBox() {
    }

    public boolean intersects(CollisionBox other) {
        return this.max.x >= other.min.x && this.min.x <= other.max.x &&
                this.max.y >= other.min.y && this.min.y <= other.max.y &&
                this.max.z >= other.min.z && this.min.z <= other.max.z;
    }

    public void expandToFit(CollisionBox other) {
        min = min != null ? min.min(other.min) : other.min;
        max = max != null ? max.max(other.max): other.max;
    }

    public Vec3d getCenter() {
        return new Vec3d(
                (min.x + max.x) * 0.5d,
                (min.y + max.y) * 0.5d,
                (min.z + max.z) * 0.5f
        );
    }

    public boolean intersects(Vec3d startVec, Vec3d endVec) {
        double tmin = 0.0;
        double tmax = 1.0;

        for (int i = 0; i < 2; i++) {
            double start = i == 0 ? startVec.x : startVec.z;
            double end = i == 0 ? endVec.x : endVec.z;
            double boxMin = i == 0 ? min.x : min.z;
            double boxMax = i == 0 ? max.x : max.z;

            if (endVec.y < min.y || endVec.y > max.y) return false;

            double direction = end - start;
            if (Math.abs(direction) < 1e-8) {
                if (start < boxMin || start > boxMax) return false;
            } else {
                double t1 = (boxMin - start) / direction;
                double t2 = (boxMax - start) / direction;

                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);

                if (tmin > tmax) return false;
            }
        }
        return true;
    }
}
