package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Particles;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.render.CustomParticle;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomParticleEmitter {
    private final List<ModelComponent> components;
    private final Map<ModelComponent, CustomParticleConfig> config = new HashMap<>();

    public static CustomParticleEmitter get(ComponentProvider provider) {
        return new CustomParticleEmitter(provider.parseAll(ModelComponentType.CUSTOM_PARTICLE_X));
    }

    public CustomParticleEmitter(List<ModelComponent> components) {
        this.components = components;
        components.forEach(c -> config.put(c, CustomParticleConfig.getInstance(c)));
    }

    private static Vec3d crossProduct(Vec3d vec1, Vec3d vec2) {
        double crossX = vec1.y * vec2.z - vec1.z * vec2.y;
        double crossY = vec1.z * vec2.x - vec1.x * vec2.z;
        double crossZ = vec1.x * vec2.y - vec1.y * vec2.x;
        return new Vec3d(crossX, crossY, crossZ);
    }

    public static Vec3d transformWorldToObject(Vec3d worldVector, Vec3d forward, Vec3d worldUp) {
        Vec3d forwardNorm = forward.normalize();
        Vec3d right = crossProduct(forwardNorm, worldUp).normalize();

        Vec3d up = crossProduct(right, forwardNorm).normalize();

        Matrix4 transformationMatrix = new Matrix4(
                right.x, up.x, -forwardNorm.x, 0,
                right.y, up.y, -forwardNorm.y, 0,
                right.z, up.z, -forwardNorm.z, 0,
                0, 0, 0, 1
        );

        return transformationMatrix.apply(worldVector);
    }

    public void effects(EntityMoveableRollingStock stock) {
        if (ConfigGraphics.particlesEnabled) {
            for (ModelComponent component : components) {
                CustomParticleConfig particleConfig = config.get(component);
                if (!stock.getEngineState() && !particleConfig.alwaysRunning) {
                    continue;
                }
                if(!particleConfig.shouldRender) {
                    continue;
                }

                double diameter = particleConfig.diameter;
                if (particleConfig.normalWidth) {
                    diameter = component.width() * stock.gauge.scale();
                }

                Vec3d particlePos = stock.getPosition().add(VecUtil.rotateWrongYaw(particleConfig.pos.scale(stock.gauge.scale()), stock.getRotationYaw() + 180));
                Vec3d fakeMotion = transformWorldToObject(particleConfig.motion, stock.getLookVector(), new Vec3d(0, 1, 0));
                try {
//                    Particles.SMOKE.accept(new SmokeParticle.SmokeParticleData(stock.getWorld(), particlePos, fakeMotion, particleConfig.lifespan, particleConfig.darken, particleConfig.thickness, particleConfig.diameter, particleConfig.texture));
                    Particles.CUSTOM.accept(new CustomParticle.CustomParticleData(stock.getWorld(), particlePos, fakeMotion, particleConfig.lifespan, particleConfig.darken, particleConfig.thickness, diameter, particleConfig.texture, particleConfig.expansionRate, particleConfig.rgba));
                } catch (Exception ignored) {
                }
            }
        }
    }
}
