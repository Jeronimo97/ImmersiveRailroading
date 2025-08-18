package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.immersiverailroading.script.library.ScheduleEvent;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import org.luaj.vm2.LuaValue;

import java.util.HashSet;
import java.util.Set;

public class AugmentModule implements LuaModule {
    private final TileRailBase tile;

    private final Set<ScheduleEvent> schedule = new HashSet<>();

    public AugmentModule(TileRailBase tile) {
        this.tile = tile;

        World.onTick(world -> {
            if (world.isClient) {
                return;
            }

            schedule.removeIf(t -> {
                t.ticks--;
                if (t.ticks <= 0) {
                    t.runnable.run();
                    return true;
                }
                return false;
            });
        });
    }

    @LuaFunction(module = "Augment")
    private LuaValue getRedstone() {
        int redstone = tile.getWorld().getRedstone(tile.getPos());
        return LuaValue.valueOf(redstone);
    }

    @LuaFunction(module = "Augment")
    private void setRedstone(LuaValue level) {
        tile.setRedstoneLevel(level.toint());
    }

    @LuaFunction(module = "Utils")
    private void wait(LuaValue sec, LuaValue func) {
        float seconds = sec.tofloat();

        Runnable runnable = () -> {
            try {
                func.call();
            } catch (Exception e) {
                ModCore.error("[Lua] Error while executing scheduled wait function: " + e.getMessage());
            }
        };

        int ticks = Math.round(seconds * 20);

        ScheduleEvent event = new ScheduleEvent(runnable, ticks, func);
        schedule.add(event);
    }

    @LuaFunction(module = "Utils")
    private void writeToChat(LuaValue arg) {
        tile.getWorld().getEntities(Player.class).forEach(p -> p.sendMessage(PlayerMessage.direct(arg.tojstring())));
    }
}
