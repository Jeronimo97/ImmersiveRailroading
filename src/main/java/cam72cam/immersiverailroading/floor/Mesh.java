package cam72cam.immersiverailroading.floor;

import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class Mesh {
    public static class Group {
        public String name;
        public List<Face> faces;
        public Vec3d min;
        public Vec3d max;
        public Vec3d normal;

        public Group(String name, List<Face> faces, Vec3d min, Vec3d max, Vec3d normal) {
            this.name = name;
            this.faces = faces;
            this.min = min;
            this.max = max;
            this.normal = normal;
        }
    }
    public static class Face {
        public List<Vec3d> vertices = new ArrayList<>();
        public Vec3d normal;
        public Pair<Float, Float> uv;

        public CollisionBox getCollisionBox() {
            Vec3d min = vertices.get(0).min(vertices.get(1).min(vertices.get(2)));
            Vec3d max = vertices.get(0).max(vertices.get(1).max(vertices.get(2)));
            return new CollisionBox(min, max);
        }
    }

    public final Map<String, Group> groups = new HashMap<>();

    private static final Set<String> groupsToBeLoaded;

    private enum Groups {
        FLOOR,
        COLLISION,
        TEXTFIELD
    }

    static {
        groupsToBeLoaded = Arrays.stream(Groups.values()).map(Enum::name).collect(Collectors.toSet());
    }

    public static Mesh loadMesh(StockModel<?, ?> model) {
        List<OBJGroup> groupList = model.groups.entrySet().stream()
                .filter(e -> groupsToBeLoaded.stream().anyMatch(name -> e.getKey().contains(name)))
                .map(Map.Entry::getValue).collect(Collectors.toList());

        Mesh mesh = new Mesh();

        for (OBJGroup group : groupList) {
            VertexBuffer vbo = model.vbo.buffer.get();
            int stride = vbo.stride;
            int vertsPerFace = vbo.vertsPerFace;
            float[] data = vbo.data;

            int vertexStart = group.faceStart * vertsPerFace;
            int vertexEnd = (group.faceStop + 1) * vertsPerFace;

            List<Face> faces = new ArrayList<>();

            for (int vIdx = vertexStart; vIdx < vertexEnd; vIdx += vertsPerFace) {
                Face face = new Face();

                for (int i = 0; i < vertsPerFace; i++) {
                    int baseIndex = (vIdx + i) * stride;

                    float vx = data[baseIndex + vbo.vertexOffset + 0];
                    float vy = data[baseIndex + vbo.vertexOffset + 1];
                    float vz = data[baseIndex + vbo.vertexOffset + 2];

                    float u = data[baseIndex + vbo.textureOffset + 0];
                    float v = data[baseIndex + vbo.textureOffset + 1];

                    Vec3d vertex = new Vec3d(vx, vy, vz);
                    face.vertices.add(vertex);

                    if (i == 0) {
                        face.uv = Pair.of(u, v);
                    }
                }

                if (vbo.hasNormals) {
                    int baseIndex = vIdx * stride;
                    float nx = data[baseIndex + vbo.normalOffset + 0];
                    float ny = data[baseIndex + vbo.normalOffset + 1];
                    float nz = data[baseIndex + vbo.normalOffset + 2];

                    face.normal = new Vec3d(nx, ny, nz);
                } else {
                    Vec3d v0 = face.vertices.get(0);
                    Vec3d v1 = face.vertices.get(1);
                    Vec3d v2 = face.vertices.get(2);
                    Vec3d normal = VecUtil.crossProduct(v1.subtract(v0), v2.subtract(v0)).normalize();
                    face.normal = normal;
                }
                faces.add(face);
            }
            Group group1 = new Group(
                    group.name,
                    faces,
                    group.min,
                    group.max,
                    group.normal
            );
            mesh.groups.put(group.name, group1);
        }
        return mesh;
    }

    public Mesh() {
    }

    public Group getGroup(String name) {
        return groups.get(name);
    }

    public List<Group> getGroupContains(String name) {
        return groups.entrySet().stream().filter(e -> e.getKey().contains(name)).map(Map.Entry::getValue).collect(Collectors.toList());
    }
}
