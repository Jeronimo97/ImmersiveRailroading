package cam72cam.immersiverailroading.textfield.library;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.serialization.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GroupInfo {
    @TagField
    public Vec3d origin;
    @TagField
    public Vec3d tangent;
    @TagField
    public Vec3d bitangent;
    @TagField
    public Vec3d normal;
    @TagField
    public double pixelSizeX;
    @TagField
    public double pixelSizeY;
    @TagField
    public int resolutionX;
    @TagField
    public boolean flippedNormal = false;

    public GroupInfo() {
    }

    public GroupInfo(Vec3d origin, Vec3d tangent, Vec3d bitangent, Vec3d normal, double pixelSizeX, double pixelSizeY, int resolutionX, boolean flippedNormal) {
        this.origin = origin;
        this.tangent = tangent;
        this.bitangent = bitangent;
        this.normal = normal;
        this.pixelSizeX = pixelSizeX;
        this.pixelSizeY = pixelSizeY;
        this.resolutionX = resolutionX;
        this.flippedNormal = flippedNormal;
    }

    private GroupInfo(TagCompound compound) {
        try {
            TagSerializer.deserialize(compound, this);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
    }

    public static GroupInfo initGroup(Mesh.Group group, int resX, int resY) {
        GroupInfo info = new GroupInfo();
        Mesh.Face face = group.faces.get(0);

        List<Pair<Vec3d, Vec2f>> vertices = group.faces.stream()
                .flatMap(f -> IntStream.range(0, f.vertices.size())
                        .mapToObj(i -> Pair.of(f.vertices.get(i), f.uv.get(i))))
                .distinct()
                .collect(Collectors.toList());


        Vec3d topLeft = vertices.stream()
                .min(Comparator.comparingDouble((Pair<Vec3d, Vec2f> p ) -> p.getRight().x).thenComparingDouble(p -> p.getRight().y))
                .orElseThrow(RuntimeException::new).getLeft();

        Vec2f topLeftUv = vertices.stream()
                .map(Pair::getRight)
                .min(Comparator.comparingDouble((Vec2f uv) -> uv.x)
                        .thenComparingDouble(uv -> uv.y))
                .orElseThrow(RuntimeException::new);

        Vec3d topRight = vertices.stream()
                .filter(p -> Math.abs(p.getRight().y - topLeftUv.y) < 1e-4)
                .max(Comparator.comparingDouble(p -> p.getRight().x))
                .orElseThrow(RuntimeException::new).getLeft();

        Vec3d bottomLeft = vertices.stream()
                .filter(p -> Math.abs(p.getRight().x -  topLeftUv.x) < 1e-4)
                .max(Comparator.comparingDouble(p -> p.getRight().y))
                .orElseThrow(RuntimeException::new).getLeft();

        Vec3d tangent = topRight.subtract(topLeft);
        Vec3d bitangent = bottomLeft.subtract(topLeft);

        double fieldWidth3d = tangent.length();
        double fieldHeight3d = bitangent.length();

        info.tangent = tangent;
        info.bitangent = bitangent;
        info.pixelSizeX = fieldWidth3d / resX;
        info.pixelSizeY = fieldHeight3d / resY;
        info.resolutionX = resX;

        info.normal = face.normal;

        Vec3d windingNormal = VecUtil.crossProduct(info.tangent, info.bitangent).normalize();
        if (VecUtil.dotProduct(windingNormal, info.normal) < 0) {
            info.flippedNormal = true;
        } else {
            info.tangent = info.tangent.scale(-1);
        }

        info.origin = info.flippedNormal ? topLeft : topRight;

        return info;
}
    private TagCompound toTag() {
        TagCompound tag = new TagCompound();
        try {
            TagSerializer.serialize(tag, this);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
        return tag;
    }

    public static class GroupMapper implements TagMapper<GroupInfo> {

        @Override
        public TagAccessor<GroupInfo> apply(Class<GroupInfo> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.set(fieldName, o.toTag()),
                    d -> new GroupInfo(d.get(fieldName))
            );
        }
    }
}
