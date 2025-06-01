package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CustomParticleConfig {
    private static final Map<ModelComponent, CustomParticleConfig> instances = new HashMap<>();

    public Vec3d pos;
    public Vec3d motion;
    public int lifespan;
    public float darken;
    public float thickness;
    public double diameter;
    public Identifier texture;
    public boolean alwaysRunning;
    public boolean shouldRender;
    public rgba rgba;
    public double expansionRate;
    public boolean normalWidth;

    private CustomParticleConfig(){}

    public static CustomParticleConfig getInstance(ModelComponent component) {
        return instances.computeIfAbsent(component, k -> new CustomParticleConfig());
    }

    public void setConfig(Vec3d pos, Vec3d motion, int lifespan, float darken, float thickness, double diameter, Identifier texture, boolean alwaysRunning, boolean shouldRender, int r, int g, int b, double a, double expansionRate, boolean normalWidth) {
        this.pos = pos;
        this.motion = motion;
        this.lifespan = lifespan;
        this.darken = darken;
        this.thickness = thickness;
        this.diameter = diameter;
        this.texture = texture;
        this.alwaysRunning = alwaysRunning;
        this.shouldRender = shouldRender;
        this.rgba = new rgba(r, g, b, a);
        this.expansionRate = expansionRate;
        this.normalWidth = normalWidth;
    }

    public static class rgba{
        public double r, g, b, a;
        public rgba(int r, int g, int b, double a) {
            this.r = r / 255f;
            this.g = g / 255f;
            this.b = b / 255f;
            this.a = a;
        }
    }
}
