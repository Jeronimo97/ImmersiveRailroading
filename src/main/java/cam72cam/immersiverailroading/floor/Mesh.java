package cam72cam.immersiverailroading.floor;

import cam72cam.mod.math.Vec3d;
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

    public static Mesh loadMesh(Identifier modelLoc) throws IOException {
        List<Vec3d> vertices = new ArrayList<>();
        List<Vec3d> normals = new ArrayList<>();
        List<Pair<Float, Float>> uvs = new ArrayList<>();
        List<Obj> objects = new ArrayList<>();

        Obj currentObj = new Obj("default");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(modelLoc.getResourceStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] tokens = line.split("\\s+");
                String cmd = tokens[0];

                switch (cmd) {
                    case "o":
                    case "g":
                        if (!currentObj.faces.isEmpty()) {
                            objects.add(currentObj);
                            currentObj = new Obj(tokens[1]);
                        } else {
                            currentObj.name = tokens[1];
                        }
                        break;
                    case "v":
                        vertices.add(new Vec3d(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        ));
                        break;
                    case "vn":
                        normals.add(new Vec3d(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        ));
                        break;
                    case "vt":
                        uvs.add(Pair.of(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2])
                        ));
                        break;
                    case "f":
                        List<VertexRef> face = new ArrayList<>();
                        for (int i = 1; i < tokens.length; i++) {
                            String[] parts = tokens[i].split("/");
                            int vi = Integer.parseInt(parts[0]) - 1;
                            int ti = (parts.length > 1 && !parts[1].isEmpty()) ? Integer.parseInt(parts[1]) - 1 : -1;
                            int ni = (parts.length > 2) ? Integer.parseInt(parts[2]) - 1 : -1;
                            face.add(new VertexRef(vi, ti, ni));
                        }
                        currentObj.faces.add(face);
                        break;
                    default:
                        break;
                }
            }
        }

        if (!currentObj.faces.isEmpty()) {
            objects.add(currentObj);
        }

        Mesh mesh = new Mesh();
        for (Obj obj : objects) {
            mesh.addGroup(obj.name, vertices, normals, uvs, obj.faces);
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

    private void addGroup(String name, List<Vec3d> vertices, List<Vec3d> normals, List<Pair<Float, Float>> uvs, List<List<VertexRef>> faceData) {
        List<Face> tempFaces = new ArrayList<>();

        for (List<VertexRef> faceRefs : faceData) {
            Face face = new Face();

            for (VertexRef ref : faceRefs) {
                if (ref.vertexIndex >= 0 && ref.vertexIndex < vertices.size()) {
                    face.vertices.add(vertices.get(ref.vertexIndex));
                }

                if (face.normal == null && ref.normalIndex >= 0 && ref.normalIndex < normals.size()) {
                    face.normal = normals.get(ref.normalIndex);
                }

                if (face.uv == null && ref.uvIndex >= 0 && ref.uvIndex < uvs.size()) {
                    face.uv = uvs.get(ref.uvIndex);
                }
            }

            tempFaces.add(face);
        }

        List<Vec3d> allVerts = tempFaces.stream()
                .flatMap(f -> f.vertices.stream()).collect(Collectors.toList());

        Vec3d min = allVerts.stream().min(Comparator.comparingDouble(Vec3d::length)).orElse(null);
        Vec3d max = allVerts.stream().max(Comparator.comparingDouble(Vec3d::length)).orElse(null);

        Vec3d averageNormal = tempFaces.stream()
                .map(f -> f.normal)
                .filter(Objects::nonNull)
                .reduce(new Vec3d(0, 0, 0), (a, b) -> new Vec3d(a.x + b.x, a.y + b.y, a.z + b.z));

        int count = (int) tempFaces.stream().map(f -> f.normal).filter(Objects::nonNull).count();
        if (count > 0) {
            averageNormal = averageNormal.scale(1.0 / count).normalize();
        } else {
            averageNormal = new Vec3d(0, 1, 0);
        }

        groups.put(name, new Group(name, tempFaces, min, max, averageNormal));
    }

    private static class Obj {
        String name;
        List<List<VertexRef>> faces = new ArrayList<>();

        Obj(String name) {
            this.name = name;
        }
    }

    private static class VertexRef {
        int vertexIndex;
        int uvIndex;
        int normalIndex;

        VertexRef(int v, int uv, int n) {
            this.vertexIndex = v;
            this.uvIndex = uv;
            this.normalIndex = n;
        }
    }
}
