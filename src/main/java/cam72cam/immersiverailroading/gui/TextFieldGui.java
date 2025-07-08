package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.gui.components.ArrowSelector;
import cam72cam.immersiverailroading.gui.components.DynamicListSelector;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.textUtil.FontLoader;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TextFieldGui implements IScreen {
    // ticks when the text field should be updated locally
    private static final int REFRESH = 60;
    private TextField input;
    private TextField color;
    private DynamicListSelector<Identifier> fontSelector;
    private DynamicListSelector<cam72cam.immersiverailroading.textUtil.TextField> objectSelector;
    private Button alignButton;
    private Button font;
    private ArrowSelector gap;
    private ArrowSelector offset;
    private cam72cam.immersiverailroading.textUtil.TextField textField;
    private String currentObject;
    private EntityScriptableRollingStock stock;
    private Button objectButton;
    private Button fontButton;
    private Identifier selectedFont;
    private double zoom = 1.0d;
    private int frame = 0;
    private List<Identifier> availableFonts;
    private Button fullbright;
    private Button gapButton;
    private Button gapUp;
    private Button global;
    private Button verticalAlign;
    private ArrowSelector scale;

    @Override
    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int spacingHeight = 25;
        int xTop = -GUIHelpers.getScreenWidth() / 2;
        int yTop = -GUIHelpers.getScreenHeight() / 4;

        Entity ent = MinecraftClient.getEntityMouseOver();
        if (ent == null) {
            return;
        }
        stock = ent.as(EntityScriptableRollingStock.class);

        if (textField == null) {
            List<String> keys = new ArrayList<>(stock.textFields.keySet());
            for (String current : keys) {
                cam72cam.immersiverailroading.textUtil.TextField temp = stock.textFields.get(current);

                if (temp.getAvailableFonts() != null || temp.getFontIdent() != null) {
                    textField = temp;
                    currentObject = current;

                    selectedFont = textField.getFontIdent();
                    break;
                }
            }
        }

        if (textField == null) return;

        availableFonts = textField.getAvailableFonts() != null ? textField.getAvailableFonts() : new ArrayList<>();

        if (selectedFont == FontLoader.DEFAULT && textField.getAvailableFonts() != null) {
            selectedFont = textField.getAvailableFonts().get(0);
        }


        objectButton = new Button(screen, xTop + 145 / 4, yTop, width - 55, height, currentObject) {
            @Override
            public void onClick(Player.Hand hand) {
                objectSelector.setVisible(!objectSelector.isVisible());
            }
        };

        yTop += spacingHeight;

        input = new TextField(screen, xTop + 62, yTop, width - 54, height);
        input.setText(textField.getText());
        input.setFocused(true);
        Predicate<String> validator = textField.getFilter(stock);
        input.setValidator(validator);

        yTop += spacingHeight;

        fontButton = new Button(screen, xTop + 60, yTop, width - 50, height, new File(selectedFont.getPath()).getName()) {
            @Override
            public void onClick(Player.Hand hand) {
                if (textField.getAvailableFonts() != null) {
                    fontSelector.setVisible(!fontSelector.isVisible());
                }
            }
        };


        fontButton.setEnabled(textField.getAvailableFonts() != null);

        yTop += spacingHeight;

        fullbright = new Button(screen, xTop + 60, yTop, width - 145, height, String.valueOf(textField.getFullBright())) {
            @Override
            public void onClick(Player.Hand hand) {
                textField.setFullBright(!textField.getFullBright());
                this.setText(String.valueOf(textField.getFullBright()));
            }
        };

        yTop += spacingHeight;

        color = new TextField(screen, xTop + 62, yTop, width - 149, height);
        color.setText(textField.getColorAsHex());

        yTop += spacingHeight;

        alignButton = new Button(screen, xTop + 60, yTop, width - 145, height, textField.getAlignAsString()) {
            @Override
            public void onClick(Player.Hand hand) {
                cam72cam.immersiverailroading.textUtil.TextField.Align next = textField.getAlign().next();
                textField.setAlign(next);
                this.setText(next.toString());
            }
        };

        yTop += spacingHeight;

        verticalAlign = new Button(screen, xTop + 60, yTop, width - 145, height, textField.getVerticalAlignAsString()) {
            @Override
            public void onClick(Player.Hand hand) {
                cam72cam.immersiverailroading.textUtil.TextField.VerticalAlign next = textField.getVerticalAlign().next();
                textField.setVerticalAlign(next);
                this.setText(next.toString());
            }
        };

        yTop += spacingHeight + 1;

        gap = new ArrowSelector(screen, xTop + 60, yTop, width - 145, height, textField.getGap(), 0, 15) {
            @Override
            public void onUpdate(float val) {
                textField.setGap((int) val);
            }
        };

        yTop += spacingHeight + 1;

        offset = new ArrowSelector(screen, xTop + 60, yTop, width - 145, height, textField.getGap(), 0, 15) {
            @Override
            public void onUpdate(float val) {
                textField.setOffset((int) val);
            }
        };

        yTop += spacingHeight + 1;

        scale = new ArrowSelector(screen, xTop + 60, yTop, width - 145, height, textField.getScale(), 0, 8, 0.5f) {
            @Override
            public void onUpdate(float val) {
                textField.setScale(val);
            }
        };

        yTop += spacingHeight;

        global = new Button(screen, xTop + 60, yTop, width - 145, height, String.valueOf(textField.getGlobal())) {
            @Override
            public void onClick(Player.Hand hand) {
                textField.setGlobal(!textField.getGlobal());
                this.setText(String.valueOf(textField.getGlobal()));
            }
        };


        Slider zoom_slider = new Slider(screen, xTop + width + 15, (int) (GUIHelpers.getScreenHeight()*0.75 - height), "Zoom: ", 0.1, 3, 1, true) {
            @Override
            public void onSlider() {
                zoom = this.getValue();
            }
        };


        /*
         * Selectors
         */

        fontSelector = new DynamicListSelector<Identifier>(screen, width + 15, 100, height, selectedFont,
                availableFonts.stream().collect(Collectors.toMap(i -> new File(i.getPath()).getName(), g -> g, (u, v) -> u, LinkedHashMap::new))) {
            @Override
            public void onClick(Identifier option) {
                selectedFont = option;
                fontButton.setText(new File(option.getPath()).getName());
                this.setVisible(!this.isVisible());
            }
        };

        objectSelector = new DynamicListSelector<cam72cam.immersiverailroading.textUtil.TextField>(screen,
                width + 15,
                250,
                height,
                textField,
                stock.textFields.entrySet().stream().filter(t ->
                                t.getValue().isSelectable())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> u, LinkedHashMap::new))) {
            @Override
            public void onClick(cam72cam.immersiverailroading.textUtil.TextField option) {
                TextFieldGui.this.update(option);
            }
        };
    }


    private void update(cam72cam.immersiverailroading.textUtil.TextField newSelection) {
        textField = newSelection;
        selectedFont = textField.getFontIdent();

        availableFonts = textField.getAvailableFonts() != null ? textField.getAvailableFonts() : new ArrayList<>();

        if (selectedFont == FontLoader.DEFAULT && textField.getAvailableFonts() != null) {
            selectedFont = textField.getAvailableFonts().get(0);
        }

        if (textField.getAvailableFonts() != null) {
            fontSelector.update(textField.getFontIdent(), textField.getAvailableFonts().stream().collect(Collectors.toMap(i -> new File(i.getPath()).getName(), g -> g, (u, v) -> u, LinkedHashMap::new)));
        }
        fontButton.setText(new File(selectedFont.getPath()).getName());

        // This is so ugly, but I have to live with it because I don't want to change the whole integration again...
        currentObject = stock.textFields.entrySet().stream().filter(e -> textField.getObject().equals(e.getValue().getObject())).map(Map.Entry::getKey).findFirst().orElse("");

        /*
         * Update the required GUI components
         */
        objectButton.setText(currentObject);
        input.setText(textField.getText());
        input.setValidator(textField.getFilter(stock));
        color.setText(textField.getColorAsHex());
        fullbright.setText(String.valueOf(textField.getFullBright()));
        gap.updateVal(textField.getGap());
        offset.updateVal(textField.getOffset());
        scale.updateVal(textField.getScale());
        fontButton.setEnabled(textField.getAvailableFonts() != null);
        global.setText(String.valueOf(textField.getGlobal()));
        alignButton.setText(textField.getAlign().toString());
        verticalAlign.setText(textField.getVerticalAlignAsString());

        fontSelector.setVisible(false);
        objectSelector.setVisible(false);
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (textField.getUnique()) {
            stock.getDefinition().inputs.put(stock.getUUID(), Collections.singletonMap(textField.getObject(), textField.getText()));
        }

        new cam72cam.immersiverailroading.textUtil.TextField.PacketSyncTextFieldServer(stock, stock.textFields).sendToServer();
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        frame++;

        GUIHelpers.drawRect(215, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 215, GUIHelpers.getScreenHeight(), 0xEE000000);

        int xTop = 5;
        int yTop = 6;
        int height = 25;

        yTop += height;
        GUIHelpers.drawString("Text:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Font:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Fullbright:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Color:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Align:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Vert Align:", xTop, yTop, 0xFFFFFF);
        yTop += height + 1;
        GUIHelpers.drawString("Gap:", xTop, yTop, 0xFFFFFF);
        yTop += height + 1;
        GUIHelpers.drawString("Spacing:", xTop, yTop, 0xFFFFFF);
        yTop += height + 1;
        GUIHelpers.drawString("Scale:", xTop, yTop, 0xFFFFFF);
        yTop += height;
        GUIHelpers.drawString("Global:", xTop, yTop, 0xFFFFFF);

        String currentText = input.getText().replace("\\n", "\n");
        textField.setText(currentText);
        textField.setFont(selectedFont);
        if (color.getText().matches("^#([A-Fa-f0-9]{6})$")) {
            textField.setColor(color.getText());
        }

        stock.textFields.put(currentObject, textField);


        // Not quite sure if I should keep it at refreshing every 60 frames (Maybe every 30 frames?)
        if (frame >= REFRESH) {
            stock.textFields.forEach((k, v) -> v.createVBO());
            frame = 0;
        }

        StockModel<?, ?> model = stock.getDefinition().getModel();

        Mesh.Group group = stock.getDefinition().getMesh().getGroupContains(textField.getObject()).get(0);

        int scale = (int) ((double) GUIHelpers.getScreenWidth() / 40 * zoom);
        state.translate(200 + (double) (GUIHelpers.getScreenWidth() - 200) / 2, (double) builder.getHeight() / 2 + 10, 400);
        state.rotate(getRotationFromNormal(group.faces.get(0).normal), 0, 1, 0);
        state.scale(-scale, -scale, -scale);
        state.lightmap(1, 1);
        state.depth_test(true);

        model.renderEntity(stock, state, 0);
        model.postRenderEntity(stock, state, 0);
    }

    private static double getRotationFromNormal(Vec3d normal) {
        Vec3d normalized = normal.normalize().scale(-1);

        double angleRadians = Math.atan2(-normalized.x, normalized.z);

        double angleDegrees = Math.toDegrees(angleRadians);

        if (angleDegrees < 0) {
            angleDegrees += 360;
        }

        return angleDegrees;
    }
}
