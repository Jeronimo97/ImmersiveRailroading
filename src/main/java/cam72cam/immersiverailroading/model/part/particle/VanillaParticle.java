package cam72cam.immersiverailroading.model.part.particle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Particle;
import cam72cam.mod.render.Particle.VanillaParticles;

public class VanillaParticle {
    private final List<ModelComponent> components;
    private Map<ModelComponent, VanillaParticles> particles = new HashMap<>();

    public static VanillaParticle get(ComponentProvider provider) {
        return new VanillaParticle(provider.parseAll(ModelComponentType.VANILLA_PARTICLE_X,
                ModelComponentType.SAND_PARTICLE_X, ModelComponentType.FIRE_PARTICLE_X));
    }

    public VanillaParticle(List<ModelComponent> components) {
        this.components = components;
        components.forEach(c -> {
            if (c.type.equals(ModelComponentType.VANILLA_PARTICLE_X)) {
                particles.put(c, translateTable(c.key));
            }
        });
    }

    private static VanillaParticles translateTable(String component) {
        Matcher matcher = Pattern.compile("PT_([^_]+)").matcher(component);
        String name = matcher.find() ? matcher.group(1) : null;
        if (name != null) {
            switch (name.toUpperCase()) {
                case "SAND":
                    return VanillaParticles.SAND_DUST;
                case "PT":
                    return VanillaParticles.HEART;
                default:
                    return VanillaParticles.NOTE;
            }
        }
        return null;
    }

    public void effects(EntityMoveableRollingStock stock) {
        if (ConfigGraphics.particlesEnabled) {
            Vec3d fakeMotion = stock.getVelocity();
            double scale = stock.gauge.scale();
            int size = components.size();
            if (size != 0)
                for (ModelComponent exhaust : components) {
                    if (particles.get(exhaust) != null) {
                        VanillaParticles vanParticle = particles.get(exhaust);
                        Vec3d particlePos = stock.getPosition().add(VecUtil.rotateWrongYaw(
                                exhaust.center.scale(scale), stock.getRotationYaw() + 180));
                        Particle.renderVanilla(vanParticle, particlePos, fakeMotion, 2);
                    }

                }
        }
    }

}
