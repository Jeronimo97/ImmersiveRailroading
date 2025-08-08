package cam72cam.immersiverailroading.script.library;

import org.luaj.vm2.LuaValue;

import java.util.Objects;

public class ScheduleEvent {
    public Runnable runnable;
    public Integer ticks;
    public LuaValue func;

    public ScheduleEvent(Runnable runnable, Integer ticks, LuaValue func) {
        this.runnable = runnable;
        this.ticks = ticks;
        this.func = func;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ScheduleEvent that = (ScheduleEvent) object;
        return func.equals(that.func);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(func);
    }
}
