package cam72cam.immersiverailroading.script.library;

import cam72cam.mod.ModCore;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.List;
import java.util.Map;

public interface ILuaEvent {

    Map<String, List<LuaValue>> getLuaEventCallbacks();

    default void triggerEvent(String eventName, LuaValue... args) {
        triggerEvent(eventName, LuaValue.varargsOf(args));
    }

    default void triggerEvent(String eventName, Varargs args) {
        List<LuaValue> callBacks = getLuaEventCallbacks().get(eventName);
        if (callBacks != null) {
            for (LuaValue callback : callBacks) {
                try {
                    callback.invoke(args);
                } catch (Exception e) {
                    ModCore.error("Lua callback for event %s failed: %s", eventName, e);
                }
            }
        }
    }

}
