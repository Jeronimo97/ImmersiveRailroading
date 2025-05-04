package cam72cam.immersiverailroading.registry;

import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.mod.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WalkableSpaceDefinition {

    private final EntityRollingStockDefinition definition;

    public Map<String, List<Vec3d>> floorHeight;
    public Map<String, List<int[]>> faces;
    public final Map<String, Map<int[], List<Vec3d>>> floorMap = new HashMap<>();
    public final Map<String, Pair<Double, Double>> yLevel = new HashMap<>();
    private final LinkedList<Vec3d> allVertices = new LinkedList<>();
    public final LinkedHashMap<String, Pair<Double, Double>> yMap = new LinkedHashMap<>();
    public List<TriangleRegion> regions = new ArrayList<>();
    public Map<Vec3d[], List<Vec3d>> adjacentTriangles = new HashMap<>();

    /*
     * Vec3d helper Methods
     */

    public static double dotProduct(Vec3d v1, Vec3d v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    private static Vec3d crossProduct(Vec3d vec1, Vec3d vec2) {
        double crossX = vec1.y * vec2.z - vec1.z * vec2.y;
        double crossY = vec1.z * vec2.x - vec1.x * vec2.z;
        double crossZ = vec1.x * vec2.y - vec1.y * vec2.x;
        return new Vec3d(crossX, crossY, crossZ);
    }

    public static Vec3d projectOnto(Vec3d vector, Vec3d onto) {
        double dotProduct = dotProduct(vector, onto);
        double ontoLengthSquared = dotProduct (onto, onto);
        return onto.scale(dotProduct / ontoLengthSquared);
    }

    public int compareTo(Vec3d v1, Vec3d other) {
        int xComparison = Double.compare(v1.x, other.x);
        if (xComparison != 0) {
            return xComparison;
        }
        int yComparison = Double.compare(v1.y, other.y);
        if (yComparison != 0) {
            return yComparison;
        }
        return Double.compare(v1.z, other.z);
    }

    /*
     * Actual Code
     */

    public WalkableSpaceDefinition(Map<String, List<int[]>> faces, LinkedList<Vec3d> allVertices, Map<String, List<Vec3d>> floorHeight, EntityRollingStockDefinition definition) {
        this.faces = faces;
        this.allVertices.addAll(allVertices);
        this.definition = definition;
        this.floorHeight = floorHeight;
    }

    public void mapFacesToVertices() {
        Map<String, Map<int[], List<Vec3d>>> tempFloorMap = new HashMap<>();
        for (Map.Entry<String, List<int[]>> entry : faces.entrySet()) {
            String object = entry.getKey();
            List<int[]> objectFaces = entry.getValue();
            int number = extractFloorNumber(object);
            String key = "FLOOR_" + number;
            Map<int[], List<Vec3d>> faceVertexMap = tempFloorMap.computeIfAbsent(key, k -> new HashMap<>());
            for (int[] face : objectFaces) {
                List<Vec3d> faceVertices = new ArrayList<>();
                for (int vertexIndex : face) {
                    Vec3d vertex = allVertices.get(vertexIndex);
                    faceVertices.add(vertex);
                }

                faceVertexMap.put(face, faceVertices);
            }
        }
        floorMap.putAll(tempFloorMap);
        precomputeYLevel();
        allVertices.clear();
        filterFloorMap();
    }

    public boolean isPlayerInsideTriangle(Vec3d playerPosition, Vec3d v1, Vec3d v2, Vec3d v3) {
        Vec3d p = new Vec3d(playerPosition.x, playerPosition.z, 0);

        Vec3d a = new Vec3d(v1.x, v1.z, 0);
        Vec3d b = new Vec3d(v2.x, v2.z, 0);
        Vec3d c = new Vec3d(v3.x, v3.z, 0);

        Vec3d v0 = b.subtract(a);
        Vec3d v1Vec = c.subtract(a);
        Vec3d v2Vec = p.subtract(a);

        double dot00 = dotProduct(v0, v0);
        double dot01 = dotProduct(v0, v1Vec);
        double dot02 = dotProduct(v0, v2Vec);
        double dot11 = dotProduct(v1Vec, v1Vec);
        double dot12 = dotProduct(v1Vec, v2Vec);

        double invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }

    public void precomputeYLevel() {
        floorMap.forEach((floor, face) -> {
            List<Vec3d> level = new ArrayList<>();
            face.forEach((f, v) -> level.addAll(v));
            Optional<Double> minY = level.stream().map(vec -> vec.y).min(Double::compareTo);
            Optional<Double> maxY = level.stream().map(vec -> vec.y).max(Double::compareTo);
            minY.ifPresent(yMin -> maxY.ifPresent(yMax -> yLevel.put(floor, Pair.of(yMin, yMax))));
        });
        yMap.putAll(sortFloorMap(yLevel));
        precomputeRegions();
    }

    private void precomputeRegions() {
        for (Map<int[], List<Vec3d>> floorTriangles : floorMap.values()) {
            List<List<Vec3d>> triangles = new ArrayList<>();

            // Iterate over each triangle within the floor
            for (List<Vec3d> triangleVertices : floorTriangles.values()) {
                triangles.add(triangleVertices);
            }

            // Create a TriangleRegion with these triangles
            regions.add(new TriangleRegion(triangles));
        }
        buildAdjacentTriangles();
    }

    public void buildAdjacentTriangles() {
        // Temporary map to store edges and the triangles that share each edge
        Map<Vec3d[], List<List<Vec3d>>> edgeToTrianglesMap = new HashMap<>();

        // Iterate over all triangles in the floorMap
        for (Map<int[], List<Vec3d>> floorTriangles : floorMap.values()) {
            for (List<Vec3d> triangle : floorTriangles.values()) {
                // Extract edges from the current triangle
                Vec3d[][] edges = {
                        {triangle.get(0), triangle.get(1)},
                        {triangle.get(1), triangle.get(2)},
                        {triangle.get(2), triangle.get(0)}
                };

                // Normalize edges by sorting vertices to ensure consistency (smallest vertex first)
                for (Vec3d[] edge : edges) {
                    if (compareTo(edge[0], edge[1]) > 0) {
                        // Swap edge vertices to enforce a consistent order (smallest vertex first)
                        Vec3d temp = edge[0];
                        edge[0] = edge[1];
                        edge[1] = temp;
                    }

                    // Add the current triangle to the list of triangles for this edge
                    edgeToTrianglesMap.computeIfAbsent(edge, k -> new ArrayList<>()).add(triangle);
                }
            }
        }

        // Filter edgeToTrianglesMap to keep only edges that have exactly two triangles (indicating adjacency)
        Map<Vec3d[], List<Vec3d>> adjacentTrianglesMap = new HashMap<>();
        for (Map.Entry<Vec3d[], List<List<Vec3d>>> entry : edgeToTrianglesMap.entrySet()) {
            List<List<Vec3d>> triangles = entry.getValue();

            // Only consider edges that are shared by exactly two triangles
            if (triangles.size() == 2) {
                Vec3d[] edge = entry.getKey();

                // Map this edge to the adjacent triangle
                // We can pick either one as the "adjacent" since both are connected by the edge
                adjacentTrianglesMap.put(edge, triangles.get(1));
            }
        }

        adjacentTriangles.putAll(adjacentTrianglesMap);
    }

    public Map<String, Pair<Double, Double>> sortFloorMap(Map<String, Pair<Double, Double>> map) {
        List<Map.Entry<String, Pair<Double, Double>>> entryList = new ArrayList<>(map.entrySet());
        entryList.sort(Comparator.comparingInt(entry -> extractFloorNumber(entry.getKey())));
        Map<String, Pair<Double, Double>> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Pair<Double, Double>> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static int extractFloorNumber(String key) {
        Pattern pattern = Pattern.compile("FLOOR_(\\d+)");
        Matcher matcher = pattern.matcher(key);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Fancy Lambda stuff, that no one can read... Basically what this method does is it takes the map of the floors
     * and filters it for doubled faces, and puts those who are higher back into the map.
     */

    public void filterFloorMap() {
        floorMap.replaceAll((floor, trianglesMap) ->
                trianglesMap.entrySet().stream()
                        .collect(Collectors.groupingBy(
                                entry -> entry.getValue().stream()
                                        .map(vec -> Arrays.asList(vec.x, vec.z))
                                        .sorted(Comparator.comparingDouble(v -> v.get(0)))
                                        .collect(Collectors.toList()),
                                Collectors.collectingAndThen(
                                        Collectors.maxBy(Comparator.comparingDouble(e -> getMaxY(e.getValue()))),
                                        optionalEntry -> optionalEntry.orElse(null)
                                )
                        ))
                        .values().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
        );
    }

    private double getMaxY(List<Vec3d> vertices) {
        return vertices.stream().mapToDouble(v -> v.y).max().orElse(Double.NEGATIVE_INFINITY);
    }

    public double getFloorWidthAtPoint(String floorName, Vec3d point) {
        point = new Vec3d(point.z * - 1, point.y, point.x);
        Map<int[], List<Vec3d>> floor = floorMap.get(floorName);

        if (floor == null) {
            throw new IllegalArgumentException("Floor not found: " + floorName);
        }

        double minZ = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;
        double tolerance = 0.5; // Tolerance to check X-value proximity

        for (Map.Entry<int[], List<Vec3d>> entry : floor.entrySet()) {
            List<Vec3d> vertices = entry.getValue();

            // Filter vertices based on X proximity to the given point
            for (Vec3d vertex : vertices) {
                if (Math.abs(vertex.x - point.x) < tolerance) {
                    double z = vertex.z;
                    if (z < minZ) {
                        minZ = z;
                    }
                    if (z > maxZ) {
                        maxZ = z;
                    }
                }
            }
        }

        if (minZ == Double.MAX_VALUE || maxZ == Double.MIN_VALUE) {
            throw new IllegalArgumentException("No faces near the given X coordinate of the point.");
        }

        // Return the width (Z difference)
        return maxZ - minZ;
    }

    public double getFloorLength(String floorName) {
        Map<int[], List<Vec3d>> floor = floorMap.get(floorName);

        if (floor == null) {
            throw new IllegalArgumentException("Floor not found: " + floorName);
        }

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;

        for (Map.Entry<int[], List<Vec3d>> entry : floor.entrySet()) {
            List<Vec3d> vertices = entry.getValue();
            for (Vec3d vertex : vertices) {
                double x = vertex.x;
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
            }
        }

        // Return the length (X difference)
        return maxX - minX;
    }

    public double[] getFloorDef(String floorName) {
        Map<int[], List<Vec3d>> floor = floorMap.get(floorName);

        if (floor == null) {
            throw new IllegalArgumentException("Floor not found: " + floorName);
        }

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;

        for (Map.Entry<int[], List<Vec3d>> entry : floor.entrySet()) {
            List<Vec3d> vertices = entry.getValue();
            for (Vec3d vertex : vertices) {
                double x = vertex.x;
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
            }
        }

        // Return the length (X difference)
        return new double[] {maxX, minX};
    }

    public Point getNearestWalkablePoint(Vec3d playerPosition) {
        Point nearestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Map<int[], List<Vec3d>>> floorEntry : floorMap.entrySet()) {
            Map<int[], List<Vec3d>> floor = floorEntry.getValue();

            for (Map.Entry<int[], List<Vec3d>> entry : floor.entrySet()) {
                List<Vec3d> vertices = entry.getValue();

                if (vertices.size() == 3) {
                    Vec3d closestPoint = getClosestPointOnTriangle(playerPosition, vertices.get(0), vertices.get(1), vertices.get(2), 0.1);
                    double distance = playerPosition.distanceTo(closestPoint);

                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestPoint = new Point(floorEntry.getKey(), closestPoint);
                    }
                }
            }
        }

        return nearestPoint;
    }

    public static class Point {
        public String floor;
        public Vec3d point;

        Point(String floor, Vec3d point) {
            this.floor = floor;
            this.point = point;
        }
    }


    private Vec3d getClosestPointOnTriangle(Vec3d p, Vec3d a, Vec3d b, Vec3d c, double edgeOffset) {
        Vec3d ab = b.subtract(a);
        Vec3d ac = c.subtract(a);
        Vec3d ap = p.subtract(a);

        double d1 = dotProduct(ab, ap);
        double d2 = dotProduct(ac, ap);

        if (d1 <= 0.0 && d2 <= 0.0) return a;

        Vec3d bp = p.subtract(b);
        double d3 = dotProduct(ab, bp);
        double d4 = dotProduct(ac, bp);

        if (d3 >= 0.0 && d4 <= d3) return b;

        Vec3d cp = p.subtract(c);
        double d5 = dotProduct(ab, cp);
        double d6 = dotProduct(ac, cp);

        if (d6 >= 0.0 && d5 <= d6) return c;

        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0.0 && d1 >= 0.0 && d3 <= 0.0) {
            double v = d1 / (d1 - d3);
            Vec3d closestPoint = a.add(ab.scale(v));
            Vec3d edgeNormal = crossProduct(ab, ac).normalize();
            return closestPoint.add(edgeNormal.scale(edgeOffset));
        }

        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0.0 && d2 >= 0.0 && d6 <= 0.0) {
            double w = d2 / (d2 - d6);
            Vec3d closestPoint = a.add(ac.scale(w));
            Vec3d edgeNormal = crossProduct(ac, ab).normalize();
            return closestPoint.add(edgeNormal.scale(edgeOffset));
        }

        double va = d3 * d6 - d5 * d4;
        if (va <= 0.0 && (d4 - d3) >= 0.0 && (d5 - d6) >= 0.0) {
            double w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
            Vec3d closestPoint = b.add((c.subtract(b)).scale(w));
            Vec3d edgeNormal = crossProduct(c.subtract(b), ab).normalize();
            return closestPoint.add(edgeNormal.scale(edgeOffset));
        }

        double denom = 1.0 / (va + vb + vc);
        double v = vb * denom;
        double w = vc * denom;
        Vec3d closestPoint = a.add(ab.scale(v)).add(ac.scale(w));

        return closestPoint;
    }


    public static class Entry {
        public int[] face;
        public List<Vec3d> vertices;

        public Entry(int[] face, List<Vec3d> vertices) {
            this.face = face;
            this.vertices = vertices;
        }
    }

    public boolean isAtFrontNew(Gauge gauge, Vec3d pos) {
        pos = pos.subtract(definition.passengerCenter.scale(gauge.scale()));
        Optional<String> longestFloor = yMap.keySet().stream().max((k1, k2) -> Double.compare(getFloorLength(k1), getFloorLength(k2)));
        double front = this.getFloorDef(longestFloor.get())[1] * gauge.scale();
        return pos.z -0.5 <= front;
    }

    public boolean isAtRearNew(Gauge gauge, Vec3d pos) {
        pos = pos.subtract(definition.passengerCenter.scale(gauge.scale()));
        Optional<String> shortestFloor = yMap.keySet().stream().max((k1, k2) -> Double.compare(getFloorLength(k1), getFloorLength(k2)));
        double back = this.getFloorDef(shortestFloor.get())[0] * gauge.scale();
        return pos.z +0.5 >= back;
    }

    public double calculateHeightInTriangle(Vec3d playerPosition, Vec3d v1, Vec3d v2, Vec3d v3) {
        double denominator = (v2.z - v3.z) * (v1.x - v3.x) + (v3.x - v2.x) * (v1.z - v3.z);
        double w1 = ((v2.z - v3.z) * (playerPosition.x - v3.x) + (v3.x - v2.x) * (playerPosition.z - v3.z)) / denominator;
        double w2 = ((v3.z - v1.z) * (playerPosition.x - v3.x) + (v1.x - v3.x) * (playerPosition.z - v3.z)) / denominator;
        double w3 = 1 - w1 - w2;

        return w1 * v1.y + w2 * v2.y + w3 * v3.y;
    }

    public static class TriangleRegion {
        public List<List<Vec3d>> triangles;
        public Vec3d minBound;
        public Vec3d maxBound;

        public TriangleRegion(List<List<Vec3d>> triangles) {
            this.triangles = triangles;
            calculateBounds();
        }

        private void calculateBounds() {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

            for (List<Vec3d> triangle : triangles) {
                for (Vec3d vertex : triangle) {
                    if (vertex.x < minX) minX = vertex.x;
                    if (vertex.y < minY) minY = vertex.y;
                    if (vertex.z < minZ) minZ = vertex.z;
                    if (vertex.x > maxX) maxX = vertex.x;
                    if (vertex.y > maxY) maxY = vertex.y;
                    if (vertex.z > maxZ) maxZ = vertex.z;
                }
            }
            minBound = new Vec3d(minX, minY, minZ);
            maxBound = new Vec3d(maxX, maxY, maxZ);
        }

        public boolean isWithinBounds(Vec3d position) {
            return position.x >= minBound.x && position.x <= maxBound.x &&
                    position.y >= minBound.y && position.y <= maxBound.y &&
                    position.z >= minBound.z && position.z <= maxBound.z;
        }
    }

    public static class BoundingBox {
        private final Vec3d min;
        private final Vec3d max;

        public BoundingBox(Vec3d min, Vec3d max) {
            this.min = min;
            this.max = max;
        }

        public double getDistanceTo(Vec3d playerPosition) {
            double clampedX = Math.max(min.x, Math.min(playerPosition.x, max.x));
            double clampedY = Math.max(min.y, Math.min(playerPosition.y, max.y));
            double clampedZ = Math.max(min.z, Math.min(playerPosition.z, max.z));

            double dx = clampedX - playerPosition.x;
            double dy = clampedY - playerPosition.y;
            double dz = clampedZ - playerPosition.z;

            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public boolean doesMovementCross(Vec3d startPosition, Vec3d movement) {
            Vec3d endPosition = startPosition.add(movement);

            double tmin = 0.0;
            double tmax = 1.0;

            for (int i = 0; i < 2; i++) {
                double start = i == 0 ? startPosition.x : startPosition.z;
                double end = i == 0 ? endPosition.x : endPosition.z;
                double boxMin = i == 0 ? min.x : min.z;
                double boxMax = i == 0 ? max.x : max.z;

                double direction = end - start;

                if (Math.abs(direction) < 1e-8) {
                    if (start < boxMin || start > boxMax) {
                        return false;
                    }
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

                    if (tmin > tmax) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

}
