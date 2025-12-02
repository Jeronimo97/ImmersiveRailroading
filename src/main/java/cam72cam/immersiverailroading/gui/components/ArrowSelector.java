package cam72cam.immersiverailroading.gui.components;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreenBuilder;

/**
 * Class that represents an GUI arrow selector
 * @author poizzy
 */
public abstract class ArrowSelector {
    Button textField;
    Button up;
    Button down;
    int max;
    int min;
    float currentVal;

    /**
     * Standard constructor
     * @param currentValue Starting value of the ArrowSelector
     * @param minValue Minimal value of the ArrowSelector
     * @param maxValue Maximal value of the ArrowSelector
     */
    public ArrowSelector(IScreenBuilder screen, int xOff, int yOff, int width, int height, int currentValue, int minValue, int maxValue) {
        this.min = minValue;
        this.max = maxValue;
        this.currentVal = currentValue;

        textField = new Button(screen, xOff, yOff, width, height, String.valueOf(currentValue)) {
            @Override
            public void onClick(Player.Hand hand) {
                /* */
            }
        };

        textField.setEnabled(false);
        textField.setTextColor(0xFFFFFF);

        int buttonOff = xOff + width - height / 2;

        up = new Button(screen, buttonOff, yOff, height / 2, height / 2, "▲") {
            @Override
            public void onClick(Player.Hand hand) {
                if (ArrowSelector.this.currentVal < ArrowSelector.this.max) {
                    // Maybe implement step variable?
                    ArrowSelector.this.currentVal += 1;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                } else if (ArrowSelector.this.currentVal == ArrowSelector.this.max) {
                    ArrowSelector.this.currentVal = ArrowSelector.this.min;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                }
                ArrowSelector.this.onUpdate(ArrowSelector.this.currentVal);
            }
        };

        down = new Button(screen, buttonOff, yOff + height / 2, height / 2, height / 2, "▼") {
            @Override
            public void onClick(Player.Hand hand) {
                if (ArrowSelector.this.currentVal > ArrowSelector.this.min) {
                    // Maybe implement step variable?
                    ArrowSelector.this.currentVal -= 1;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                } else if (ArrowSelector.this.currentVal == ArrowSelector.this.min) {
                    ArrowSelector.this.currentVal = ArrowSelector.this.max;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                }
                ArrowSelector.this.onUpdate(ArrowSelector.this.currentVal);
            }
        };
    }

    /**
     * Alternative constructor supporting floating point precision
     * @param currentFloat Starting value of the ArrowSelector
     * @param minValue Minimal value of the ArrowSelector
     * @param maxValue Maximal value of the ArrowSelector
     * @param increment Value of which this Selector should be incremented
     */

    public ArrowSelector(IScreenBuilder screen, int xOff, int yOff, int width, int height, float currentFloat, int minValue, int maxValue, float increment) {
        this.min = minValue;
        this.max = maxValue;
        this.currentVal = currentFloat;

        textField = new Button(screen, xOff, yOff, width, height, String.valueOf(currentFloat)) {
            @Override
            public void onClick(Player.Hand hand) {
                /* */
            }
        };

        textField.setEnabled(false);
        textField.setTextColor(0xFFFFFF);

        int buttonOff = xOff + width - height / 2;

        up = new Button(screen, buttonOff, yOff, height / 2, height / 2, "▲") {
            @Override
            public void onClick(Player.Hand hand) {
                if (ArrowSelector.this.currentVal < ArrowSelector.this.max) {
                    // Maybe implement step variable?
                    ArrowSelector.this.currentVal += increment;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                } else if (ArrowSelector.this.currentVal == ArrowSelector.this.max) {
                    ArrowSelector.this.currentVal = ArrowSelector.this.min;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                }
                ArrowSelector.this.onUpdate(ArrowSelector.this.currentVal);
            }
        };

        down = new Button(screen, buttonOff, yOff + height / 2, height / 2, height / 2, "▼") {
            @Override
            public void onClick(Player.Hand hand) {
                if (ArrowSelector.this.currentVal > ArrowSelector.this.min) {
                    // Maybe implement step variable?
                    ArrowSelector.this.currentVal -= increment;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                } else if (ArrowSelector.this.currentVal == ArrowSelector.this.min) {
                    ArrowSelector.this.currentVal = ArrowSelector.this.max;
                    ArrowSelector.this.textField.setText(String.valueOf(ArrowSelector.this.currentVal));
                }
                ArrowSelector.this.onUpdate(ArrowSelector.this.currentVal);
            }
        };
    }

    /**
     * Method called when the value is updated
     */
    public abstract void onUpdate(float val);

    /**
     * Update the value of the ArrowSelector
     * @param val New value
     */
    public void updateVal(float val) {
        this.currentVal = val;
        this.textField.setText(String.valueOf(val));
    }
}
