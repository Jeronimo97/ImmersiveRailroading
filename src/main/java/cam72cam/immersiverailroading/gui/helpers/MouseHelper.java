package cam72cam.immersiverailroading.gui.helpers;

import cam72cam.mod.event.ClientEvents;

public class MouseHelper {
    public static int mouseX;
    public static int mouseY;

    public static int mouseClickedX;
    public static int mouseClickedY;
    public static int button;

    public static boolean clicked;


    public static void updateMousePosition(ClientEvents.MouseGuiEvent evt) {
        mouseX = evt.x;
        mouseY = evt.y;
        clicked = evt.action == ClientEvents.MouseAction.CLICK;
    }

    public static void mouseClicked(int x, int y, int mouseButton) {
        mouseClickedX = x;
        mouseClickedY = y;
        button = mouseButton;
    }
}
