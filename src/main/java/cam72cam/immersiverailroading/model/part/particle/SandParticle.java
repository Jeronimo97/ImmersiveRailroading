package cam72cam.immersiverailroading.model.part.particle;

import java.util.List;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Particle;
import cam72cam.mod.render.Particle.VanillaParticles;

public class SandParticle {
    private final List<ModelComponent> components;

    public static SandParticle get(ComponentProvider provider) {
        return new SandParticle(provider.parseAll(ModelComponentType.SAND_PARTICLE_X));
    }

    public SandParticle(List<ModelComponent> components) {
        this.components = components;
    }

    public void effects(EntityMoveableRollingStock stock) {
        if (ConfigGraphics.particlesEnabled) {
            Vec3d fakeMotion = stock.getVelocity();
            double scale = stock.gauge.scale();
            for (ModelComponent exhaust : components) {
                Vec3d particlePos = stock.getPosition().add(VecUtil
                        .rotateWrongYaw(exhaust.center.scale(scale), stock.getRotationYaw() + 180));
                Particle.renderVanilla(VanillaParticles.SAND_DUST, particlePos, fakeMotion, 2);
            }
        }
    }

}
