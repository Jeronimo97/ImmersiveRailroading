package cam72cam.immersiverailroading.model.part;

import java.util.List;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.SmokeParticle.SmokeParticleData;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Particle;
import cam72cam.mod.render.Particle.VanillaParticles;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;

public class VanillaParticle {
    private final List<ModelComponent> components;

    public static VanillaParticle get(ComponentProvider provider, ModelComponentType type) {
        return new VanillaParticle(provider.parseAll(type));
    }

    public VanillaParticle(List<ModelComponent> components) {
        this.components = components;
    }

    public void tick(EntityMoveableRollingStock stock, boolean active, VanillaParticles particle, float size) {
        if (ConfigGraphics.particlesEnabled && active) {
            Vec3d fakeMotion = stock.getVelocity();
            double scale = stock.gauge.scale();
            for (ModelComponent comp : components) {
                Vec3d particlePos = stock.getPosition().add(VecUtil
                        .rotateWrongYaw(comp.center.scale(scale), stock.getRotationYaw() + 180));
                Particle.renderVanilla(particle, particlePos, fakeMotion, size);
            }
        }
    }

    public void tickSteam(EntityMoveableRollingStock stock) {
        if (ConfigGraphics.particlesEnabled) {
            Vec3d fakeMotion = stock.getVelocity();
            double scale = stock.gauge.scale();
            World world = stock.getWorld();
            Identifier particleTex = stock.getDefinition().steamParticleTexture;
            for (ModelComponent comp : components) {
                Vec3d particlePos = stock.getPosition().add(VecUtil
                        .rotateWrongYaw(comp.center.scale(scale), stock.getRotationYaw() + 180));
                Particles.SMOKE.accept(new SmokeParticleData(world, particlePos,
                        fakeMotion.add(0, 0.05, 0), 20, 0, 0.02f, 0.2f, particleTex));
            }
        }
    }

}
