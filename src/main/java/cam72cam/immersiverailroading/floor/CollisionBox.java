package cam72cam.immersiverailroading.floor;

import cam72cam.mod.math.Vec3d;

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
}
