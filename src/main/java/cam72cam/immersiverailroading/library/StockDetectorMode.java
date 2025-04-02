package cam72cam.immersiverailroading.library;

import java.util.Locale;

public enum StockDetectorMode {
    SIMPLE, SPEED, PASSENGERS, CARGO, LIQUID, COMPUTER, STOCKCOUNT,;

    @Override
    public String toString() {
        return "immersiverailroading:detector_mode." + super.toString().toLowerCase(Locale.ROOT);
    }
}
