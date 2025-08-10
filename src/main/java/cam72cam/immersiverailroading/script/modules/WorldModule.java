package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import org.luaj.vm2.LuaValue;

public class WorldModule implements LuaModule {
    private final World world;
    
    public WorldModule(EntityScriptableRollingStock stock) {
        this.world = stock.getWorld();
    }

    @LuaFunction(module = "World")
    public LuaValue isRainingAt(LuaValue pos) {
        return LuaValue.valueOf(world.isRaining(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue isSnowingAt(LuaValue pos) {
        return LuaValue.valueOf(world.isSnowing(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getTemperatureAt(LuaValue pos) {
        return LuaValue.valueOf(world.getTemperature(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getSnowLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getSnowLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getBlockLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getBlockLightLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getSkyLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getSkyLightLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getDimension() {
        return LuaValue.valueOf(world.getId());
    }

    @LuaFunction(module = "World")
    public LuaValue getTicks() {
        return LuaValue.valueOf(world.getTicks());
    }

    @LuaFunction(module = "World")
    public LuaValue getBlock(LuaValue luaPos) {
        Vec3i pos = ScriptVectorUtil.convertToVec3i(luaPos);
        // It will work, hopefully :)
        ItemStack stack = world.getItemStack(pos);

        String name = stack.getDisplayName();

        return LuaValue.valueOf(name);
    }
}
