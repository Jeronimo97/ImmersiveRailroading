package cam72cam.immersiverailroading.library;

import java.util.Locale;

public enum PZBMode {
    HZ1000, HZ500, HZ2000;

    @Override
    public String toString() {
        return "immersiverailroading:pzb_mode." + super.toString().toLowerCase(Locale.ROOT);
    }

}
