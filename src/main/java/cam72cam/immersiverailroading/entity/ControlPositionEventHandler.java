package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.mod.ModCore;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

/**
 * by poizzy
 *
 */

public interface ControlPositionEventHandler {

    default void handleControlPositionEvent(Control<?> control, float val, Map<String, Pair<Boolean, Float>> controlPositions, boolean pressed) {
//        ModCore.info(String.format("Control %s Changed to %f", control.controlGroup, val));
        controlPositions.put(control.controlGroup, Pair.of(pressed, val));
        wakeLuaScript();
    }

    default void wakeLuaScript() {

    }
}
