package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.gui.components.GuiUtils;
import cam72cam.immersiverailroading.gui.helpers.MouseHelper;
import cam72cam.immersiverailroading.registry.LuaAugmentDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import javax.annotation.Nullable;
import java.util.*;

public class LuaSelector implements IScreen {
    private TextField searchField;
    private Button dropdownToggle;

    private boolean isDropdownOpen = false;
    private List<ScriptDef> filteredScripts;
    private int selectedItemIndex = -1;

    private List<ScriptDef> scriptDef = new ArrayList<>();

    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 150;
    private int guiLeft, guiTop;
    private static final int DROPDOWN_HEIGHT = 80;
    private static final int SCRIPT_LINE_HEIGHT = 12;

    private final TileRailBase tileRailBase;

    private ScriptDef selected;

    private String lastInput = "";

    public LuaSelector(TileRailBase tile) {
        scriptDef = LuaAugmentDefinition.scriptDef;
        if (tile.selectedScript != null) {
            scriptDef.forEach(s -> {
                if (s.equals(tile.selectedScript)) {
                    this.selected = s;
                }
            });
        }

        this.tileRailBase = tile;
        filteredScripts = new ArrayList<>(scriptDef);
    }

    @Override
    public void init(IScreenBuilder screen) {
        guiLeft = (int) (GUIHelpers.getScreenWidth() / 1.5);
        guiTop = (GUIHelpers.getScreenHeight() / 4);

        searchField = new TextField(screen, guiLeft - (GUIHelpers.getScreenWidth() / 2), guiTop - (guiTop / 45), 120, 14);

        dropdownToggle = new Button(screen, guiLeft - (GUIHelpers.getScreenWidth() / 2) + (120 - 14), guiTop - (guiTop / 45), 14, 14, "▼") {
            @Override
            public void onClick(Player.Hand hand) {
                isDropdownOpen = !isDropdownOpen;
            }
        };

        if (this.selected != null) {
            searchField.setText(this.selected.name);
        }
    }

    public void updateFilter() {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            filteredScripts = new ArrayList<>(scriptDef);
        } else {
            filteredScripts = new ArrayList<>();
            for (ScriptDef def : scriptDef) {
                if (def.name != null && def.name.toLowerCase().contains(query)) {
                    filteredScripts.add(def);
                }
            }
        }
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (selected != null) {
            tileRailBase.setSelectedScript(selected);
            new TileRailBase.AugmentPacket(tileRailBase, this.selected).sendToServer();
        }
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        if (!searchField.getText().equals(lastInput)) {
            updateFilter();
            lastInput = searchField.getText();
        }

        GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xCC000000);
        state.depth_test(true);

        GUIHelpers.drawCenteredString("Selected lua Script for this Augment", guiLeft / 2, guiTop + (GUIHelpers.getScreenHeight() / 4), 0xFFD3D3D3);

        if (selected != null) {
            String[] lines = selected.desc.split("\n");
            int linePx = 10;
            for (String string : lines) {
                GUIHelpers.drawCenteredString(string, (GUIHelpers.getScreenWidth() / 2), linePx, 0xFFD3D3D3);
                linePx += 10;
            }
        }

        if (isDropdownOpen) {
            dropdownToggle.setText("▲");

            int dropdownX = guiLeft;
            int dropdownY = guiTop + (GUIHelpers.getScreenHeight() / 4) + 14;
            int dropdownWidth = 120;

            GUIHelpers.drawRect(dropdownX, dropdownY, dropdownWidth, DROPDOWN_HEIGHT, 0xFF505050);

            int visibleScripts = DROPDOWN_HEIGHT / SCRIPT_LINE_HEIGHT;
            for (int i = 0; i < Math.min(filteredScripts.size(), visibleScripts); i++) {
                String string = filteredScripts.get(i).name;
                int lineY = dropdownY + i * SCRIPT_LINE_HEIGHT;

                if (MouseHelper.mouseY >= lineY && MouseHelper.mouseY < lineY + SCRIPT_LINE_HEIGHT && MouseHelper.mouseX >= dropdownX && MouseHelper.mouseX < dropdownX + dropdownWidth) {
                    GUIHelpers.drawRect(dropdownX, lineY,  dropdownWidth, SCRIPT_LINE_HEIGHT, 0xFF808080);
                }

                GUIHelpers.drawCenteredString(string, (dropdownX + 2) + dropdownWidth / 2, lineY + 2, 0xFFFFFF);
            }
        } else {
            dropdownToggle.setText("▼");
        }

        if (MouseHelper.clicked) {
            this.mouseClicked(MouseHelper.mouseClickedX, MouseHelper.mouseClickedY, MouseHelper.button);
        }
    }

    private void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isDropdownOpen) {
            int dropdownX = guiLeft;
            int dropdownY = guiTop + (GUIHelpers.getScreenHeight() / 4) + 14;
            int dropdownWidth = 120;

            if (mouseX >= dropdownX && mouseX < dropdownX + dropdownWidth && mouseY >= dropdownY && mouseY < dropdownY + DROPDOWN_HEIGHT && mouseButton == 0) {
                int index = (mouseY - dropdownY) / SCRIPT_LINE_HEIGHT;

                if (index >=  0 && index < filteredScripts.size()) {
                    selectedItemIndex = index;

                    selected = filteredScripts.get(selectedItemIndex);

                    searchField.setText(selected.name);

                    isDropdownOpen = false;
                }
            }
        }
    }

    public static class ScriptDef {
        public String name;
        public Identifier script;
        public String desc;
        public List<String> additional;

        public ScriptDef(){}

        public ScriptDef(String name, Identifier script) {
            this.name = name;
            this.script = script;
        }

        public ScriptDef setAdditional(@Nullable List<String> additional) {
            this.additional = additional;
            return this;
        }

        public ScriptDef setDesc(@Nullable String desc) {
            this.desc = desc;
            return this;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            ScriptDef scriptDef = (ScriptDef) object;
            return Objects.equals(name, scriptDef.name) && Objects.equals(script, scriptDef.script) && Objects.equals(desc, scriptDef.desc) && Objects.equals(additional, scriptDef.additional);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, script, desc, additional);
        }
    }
}
