package cam72cam.immersiverailroading.floor;

import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.FaceAccessor;
import cam72cam.mod.model.obj.OBJFace;
import cam72cam.mod.util.Axis;

import java.util.*;

public class NavMesh {
    public BVHNode root;
    public BVHNode collisionRoot;
    private final boolean hasNavMesh;
    // Theoretically this could be much lower. IR floor meshes probably won't use the whole depth, but who knows
    private static final int MAX_DEPTH = 20;
    private static final int LEAF_SIZE = 8;

    public NavMesh(StockModel<?, ?> model) {
        hasNavMesh = model.groups().stream().anyMatch(s -> s.contains("FLOOR"));
        if (!hasNavMesh) return;

        FaceAccessor accessor = model.getFaceAccessor();

        List<OBJFace> floor = new ArrayList<>();
        if (model.floor != null) {
            model.floor.groups().forEach(group -> {
                FaceAccessor sub = accessor.getSubByGroup(group.name);
                sub.forEach(a -> floor.add(a.asOBJFace()));
            });
        }
        this.root = buildBVH(floor, 0);

        List<OBJFace> collision = new ArrayList<>();
        if (model.collision != null) {
            model.collision.groups().forEach(group -> {
                FaceAccessor sub = accessor.getSubByGroup(group.name);
                sub.forEach(a -> collision.add(a.asOBJFace()));
            });
        }
        this.collisionRoot = buildBVH(collision, 0);
    }

    public boolean hasNavMesh() {
        return hasNavMesh;
    }

    public static class BVHNode {
        IBoundingBox bounds;
        BVHNode left;
        BVHNode right;
        List<OBJFace> triangles;

        boolean isLeaf() {
            return triangles != null;
        }
    }

    public BVHNode buildBVH(List<OBJFace> triangles, int depth) {
        if (triangles.size() <= LEAF_SIZE || depth > MAX_DEPTH) {
            IBoundingBox bounds = IBoundingBox.from(Vec3i.ZERO);
            for (OBJFace face : triangles) {
                bounds = bounds.expandToFit(face.getBoundingBox());
            }
            BVHNode node = new BVHNode();
            node.bounds = bounds;
            node.triangles = triangles;
            return node;
        }

        IBoundingBox bounds = IBoundingBox.from(Vec3i.ZERO);
        for (OBJFace face : triangles) {
            bounds = bounds.expandToFit(face.getBoundingBox());
        }

        Vec3d size = bounds.max().subtract(bounds.min());
        Axis axis = (size.x > size.y && size.x > size.z) ? Axis.X : (size.y > size.z ? Axis.Y : Axis.Z);

        triangles.sort((a, b) -> Double.compare(getCentroid(a, axis), getCentroid(b, axis)));
        int mid = triangles.size() / 2;

        BVHNode node = new BVHNode();
        node.left = buildBVH(triangles.subList(0, mid), depth + 1);
        node.right = buildBVH(triangles.subList(mid, triangles.size()), depth + 1);
        node.bounds = bounds;
        return node;
    }

    public void queryBVH(BVHNode node, IBoundingBox query, List<OBJFace> result, double scale) {
        query = unscaleBoxUniform(query, scale);
        queryBVHInternal(node, query, result, scale);
    }



    public void queryBVHInternal(BVHNode node, IBoundingBox query, List<OBJFace> result, double scale) {
        if (node == null) return;
        if (!node.bounds.intersects(query)) return;

//        query = unscaleBoxUniform(query, scale);

        if (node.isLeaf()) {
            for (OBJFace tri : node.triangles) {
                if (tri.getBoundingBox().intersects(query)) {
                    result.add(tri.scale(scale));
                }
            }
        } else {
            queryBVHInternal(node.left, query, result, scale);
            queryBVHInternal(node.right, query, result, scale);
        }
    }

    private static IBoundingBox unscaleBoxUniform(IBoundingBox box, double s) {
        Vec3d a = box.min().scale(1.0 / s);
        Vec3d b = box.max().scale(1.0 / s);

        IBoundingBox out = IBoundingBox.from(
                new Vec3d(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z)),
                new Vec3d(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z))
        );
        return out;
    }

    private double getCentroid(OBJFace tri, Axis axis) {
        return (VecUtil.getByAxis(tri.vertices.get(0), axis) + VecUtil.getByAxis(tri.vertices.get(1), axis) + VecUtil.getByAxis(tri.vertices.get(2), axis)) / 3f;
    }
}