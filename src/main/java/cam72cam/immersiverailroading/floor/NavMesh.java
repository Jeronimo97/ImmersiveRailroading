package cam72cam.immersiverailroading.floor;

import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class NavMesh {
    public BVHNode root;

    public NavMesh(Mesh mesh) {
        this.root = buildBVH(mesh.getGroupContains("FLOOR").stream().flatMap(m -> m.faces.stream()).collect(Collectors.toList()), 20);
    }

    public static class BVHNode {
        CollisionBox bounds;
        BVHNode left;
        BVHNode right;
        List<Mesh.Face> triangles;

        boolean isLeaf() {
            return triangles != null;
        }
    }

    public BVHNode buildBVH(List<Mesh.Face> triangles, int depth) {
        if (triangles.size() <= 5 || depth > 20) {
            CollisionBox bounds = new CollisionBox();
            triangles.forEach(t -> bounds.expandToFit(t.getCollisionBox()));
            BVHNode node = new BVHNode();
            node.bounds = bounds;
            node.triangles = triangles;
            return node;
        }

        CollisionBox bounds = new CollisionBox();
        triangles.forEach(t -> bounds.expandToFit(t.getCollisionBox()));

        Vec3d size = bounds.max.subtract(bounds.min);
        int axis = (size.x > size.y && size.x > size.z) ? 0 : (size.y > size.z ? 1 : 2);

        triangles.sort((a, b) -> Double.compare(getCentroid(a, axis), getCentroid(b, axis)));
        int mid = triangles.size() / 2;

        BVHNode node = new BVHNode();
        node.left = buildBVH(triangles.subList(0, mid), depth + 1);
        node.right = buildBVH(triangles.subList(mid, triangles.size()), depth + 1);
        node.bounds = bounds;
        return node;
    }

    public void queryBVH(BVHNode node, CollisionBox query, List<Mesh.Face> result) {
//        if (!node.bounds.intersects(query)) return;

        if (node.isLeaf()) {
            for (Mesh.Face tri : node.triangles) {
                if (tri.getCollisionBox().intersects(query)) {
                    result.add(tri);
                }
            }
        } else {
            queryBVH(node.left, query, result);
            queryBVH(node.right, query, result);
        }
    }

    private double getCentroid(Mesh.Face tri, int axis) {
        return (VecUtil.getAxis(tri.vertices.get(0), axis) + VecUtil.getAxis(tri.vertices.get(1), axis) + VecUtil.getAxis(tri.vertices.get(2), axis)) / 3f;
    }
}
