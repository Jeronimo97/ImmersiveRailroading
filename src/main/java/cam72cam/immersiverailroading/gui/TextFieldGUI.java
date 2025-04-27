package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.model.StockModel;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
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
import java.util.stream.Collectors;

public class TextFieldGUI implements IScreen {
    long frame;

    private TextField input;
    private TextRenderOptions settings;
    private ListSelector<Font.TextAlign> align;
    private Button alignButton;
    private double zoom = 1.0;
    private ListSelector<String> textFieldSelector;
    private Button textFieldButton;
    private Button fontButton;
    private ListSelector<EntityRollingStockDefinition.Fonts> fontSelector;
    private TextField color;
    private Slider offset;
    private Slider lineSpacing;

    private Vec3d vec3dNormal = new Vec3d(0, 0, 1);

    private EntityScriptableRollingStock stock;

    @Override
    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        Entity ent = MinecraftClient.getEntityMouseOver();
        this.stock = ent.as(EntityScriptableRollingStock.class);
        EntityRollingStockDefinition def = stock.getDefinition();

        if (settings == null) {
            settings = stock.textRenderOptions.entrySet().stream().filter(t -> t.getValue().selectable).iterator().next().getValue();
            assert settings.fontId != null;
            settings.id = def.fontDef.get(settings.fontId.get(0)).font;
            settings.fontSize = def.fontDef.get(settings.fontId.get(0)).size;
            settings.textureHeight = def.fontDef.get(settings.fontId.get(0)).resY;
            settings.fontX = def.fontDef.get(settings.fontId.get(0)).resX;
        }

        textFieldSelector = new ListSelector<String>(screen, width, 250, height,
                settings.componentId,
                stock.textRenderOptions.entrySet().stream().filter(t -> t.getValue().selectable).collect(Collectors.toMap(entry -> entry.getValue().toString(), Map.Entry::getKey, (u, v) -> u, LinkedHashMap::new))) {
            @Override
            public void onClick(String option) {
                settings = stock.textRenderOptions.get(option);
                textFieldButton.setText(GuiText.SELECTOR_TEXTFIELD.toString(settings.componentId));
                if (settings.fontId != null) {
                    fontSelector(screen, width, height, def);
                    if (settings.id == null) {
                        settings.id = def.fontDef.get(settings.fontId.get(0)).font;
                        settings.fontSize = def.fontDef.get(settings.fontId.get(0)).size;
                        settings.textureHeight = def.fontDef.get(settings.fontId.get(0)).resY;
                        settings.fontX = def.fontDef.get(settings.fontId.get(0)).resX;
                    }
                }
                if (input != null) {
                    input.setText(settings.lastText);
                }
                textFieldSelector.setVisible(false);
            }
        };

        input = new TextField(screen, xtop, ytop, width-1, height);
        this.input.setText(settings.lastText);
        this.input.setFocused(true);
        if (settings.isNumberPlate) {
            this.input.setValidator(input -> {
                if (input == null || input.isEmpty()) {
                    return true;
                }
                try {
                    String intInput = input.replace(" ", "");
                    Integer.parseInt(intInput);
                    settings.newText = input;
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
        }

        if (settings.unique) {
            this.input.setValidator(input -> {
                if (input == null) {
                    return true;
                }

                return stock.getDefinition().inputs.values().stream().noneMatch(input::equalsIgnoreCase);
            });
        }

        ytop += height;

        this.align = new ListSelector<Font.TextAlign>(screen, width, 100, height, settings.align,
                Arrays.stream(Font.TextAlign.values()).collect(Collectors.toMap(Font.TextAlign::toString, g -> g, (u, v) -> u, LinkedHashMap::new))
        ) {
            @Override
            public void onClick(Font.TextAlign option) {
                settings.align = option;
                alignButton.setText(GuiText.SELECTOR_ALIGNMENT.toString(settings.align));
                align.setVisible(false);
            }
        };

        this.alignButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_ALIGNMENT.toString(settings.align)) {
            @Override
            public void onClick(Player.Hand hand) {
                showSelector(align);
            }
        };

        ytop += height;

        this.textFieldButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_TEXTFIELD.toString(settings.componentId)) {
            @Override
            public void onClick(Player.Hand hand) {
                showSelector(textFieldSelector);
            }
        };

        ytop += height;

        if (settings.fontId != null) {

            this.fontButton = new Button(screen, xtop, ytop, width, height, GuiText.SELECTOR_FONT.toString(new File(def.fontDef.get(settings.fontId.get(0)).font.getPath()).getName())) {
                @Override
                public void onClick(Player.Hand hand) {
                    showSelector(fontSelector);
                }
            };

            fontSelector(screen, width, height, def);
        }

        ytop += height;

        this.color = new TextField(screen, xtop, ytop, width -1,height);
        this.color.setText(settings.hexCode);
        this.color.setValidator(hex -> {
                    if (hex == null) {
                        return false;
                    }
                    return hex.isEmpty() || hex.matches("#([0-9a-fA-F]{0,6}|[0-9a-fA-F]{0,3})");
                }
        );

        ytop += height + 1;

        this.offset = new Slider(screen, xtop, ytop, GuiText.SLIDER_OFFSET.toString(settings.offset), -100d, 100d, settings.offset, false) {
            @Override
            public void onSlider() {
                settings.offset = offset.getValueInt();
                offset.setText(GuiText.SLIDER_OFFSET.toString(settings.offset));
            }
        };

        ytop += height + 1;

        this.lineSpacing = new Slider(screen, xtop, ytop, GuiText.SLIDER_LINE_SPACING.toString(settings.lineSpacingPixels), 0, 100d, settings.lineSpacingPixels, false) {
            @Override
            public void onSlider() {
                settings.lineSpacingPixels = lineSpacing.getValueInt();
                lineSpacing.setText(GuiText.SLIDER_LINE_SPACING.toString(settings.lineSpacingPixels));
            }
        };

        ytop += height;

        CheckBox global = new CheckBox(screen, xtop, ytop, GuiText.CHECKBOX_GLOBAL.toString(), settings.global) {
            @Override
            public void onClick(Player.Hand hand) {
                settings.global = !settings.global;
            }
        };

        Slider zoom_slider = new Slider(screen, xtop + width, (int) (GUIHelpers.getScreenHeight()*0.75 - height), "Zoom: ", 0.1, 3, 1, true) {
            @Override
            public void onSlider() {
                zoom = this.getValue();
            }
        };
    }

    private void fontSelector(IScreenBuilder screen, int width, int height, EntityRollingStockDefinition def) {
        this.fontSelector = new ListSelector<EntityRollingStockDefinition.Fonts>(screen, width, 100, height, def.fontDef.get(settings.fontId.get(0)),
                this.settings.fontId.stream().filter(index -> index >= 0 && index < def.fontDef.size()).map(def.fontDef::get).collect(Collectors.toList())
                        .stream().collect(Collectors.toMap(i -> new File(i.font.getPath()).getName(), g -> g, (u, v) -> u, LinkedHashMap::new))
        ) {
            @Override
            public void onClick(EntityRollingStockDefinition.Fonts option) {
                settings.id = option.font;
                settings.fontX = option.resX;
                settings.textureHeight = option.resY;
                settings.fontSize = option.size;
                fontButton.setText(GuiText.SELECTOR_FONT.toString(new File(settings.id.getPath()).getName()));
                fontSelector.setVisible(false);
            }
        };
    }

    private void showSelector(ListSelector<?> selector) {
        boolean isVisible = selector.isVisible();

        align.setVisible(false);
        textFieldSelector.setVisible(false);
        fontSelector.setVisible(false);

        selector.setVisible(!isVisible);
    }


    @Override
    public void onEnterKey(IScreenBuilder builder) {
        builder.close();
    }

    @Override
    public void onClose() {
        if (settings.unique) {
            stock.getDefinition().inputs.put(stock.getUUID(), settings.newText);
        }
        settings.newText = "";
    }

    private Vec3d getVec3dmin (List<Vec3d> vectors) {
        return vectors.stream().min(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    private Vec3d getVec3dmax (List<Vec3d> vectors) {
        return vectors.stream().max(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    private void updateText () {
        if (!color.getText().isEmpty()) {
            settings.hexCode = color.getText();
        }
        if (settings.id != null) {
            this.vec3dNormal = settings.normal;

            settings.newText = input.getText().replace("\\n", "\n");
            settings.lastText = settings.newText;

            if (!settings.linked.isEmpty()) {
                for (int i = 0; i < settings.linked.size(); i++) {
                    String textField = settings.linked.get(i);
                    TextRenderOptions options = stock.getDefinition().textFieldDef.get(textField);
                    options.newText = settings.newText;
                    options.align = settings.align;

                    EntityRollingStockDefinition def = stock.getDefinition();

                    assert options.fontId != null;
                    options.fontId.forEach(id -> {
                        Identifier font = def.fontDef.get(id).font;
                        if (font.equals(settings.id)) {
                            options.id = settings.id;
                            options.fontSize = settings.fontSize;
                            options.textureHeight = settings.textureHeight;
                            options.fontX = settings.fontX;
                        }
                    });

                    if (options.id == null) {
                        options.id = def.fontDef.get(options.fontId.get(0)).font;
                        options.fontSize = def.fontDef.get(options.fontId.get(0)).size;
                        options.textureHeight = def.fontDef.get(options.fontId.get(0)).resY;
                        options.fontX = def.fontDef.get(options.fontId.get(0)).resX;
                    }
                    new ItemTypewriter.TypewriterPacket(stock, options).sendToServer();
                }
            }
//            stock.setText(settings);
            new ItemTypewriter.TypewriterPacket(stock, settings).sendToServer();
        }
    }

    public static double getRotationFromNormal(Vec3d normal) {
        Vec3d normalized = normal.normalize();

        double angleRadians = Math.atan2(-normalized.x, normalized.z);

        double angleDegrees = Math.toDegrees(angleRadians);

        if (angleDegrees < 0) {
            angleDegrees += 360;
        }

        return angleDegrees;
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        frame ++;
        GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth() - 200, GUIHelpers.getScreenHeight(), 0xCC000000);
        GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);

        Entity ent = MinecraftClient.getEntityMouseOver();
        if (ent == null) {
            return;
        }
        EntityCoupleableRollingStock rollingStock = ent.as(EntityCoupleableRollingStock.class);

        StockModel<?, ?> model = rollingStock.getDefinition().getModel();

        //int scale = 8;
        int scale = (int) (GUIHelpers.getScreenWidth() / 40 * zoom);
        float speed = 0.75f;
        state.translate(200 + (GUIHelpers.getScreenWidth()-200) / 2, builder.getHeight() / 2 + 10, 400);
        state.rotate(getRotationFromNormal(vec3dNormal), 0, 1, 0);
        state.scale(-scale, -scale, -scale);
        state.lightmap(1, 1);
        state.depth_test(true);

        double prevDist = rollingStock.distanceTraveled;
        String prevTex = rollingStock.getTexture();
        Gauge prevGauge = rollingStock.gauge;

        rollingStock.setTexture(rollingStock.getTexture());
        rollingStock.distanceTraveled += frame * 0.02;
        rollingStock.gauge = Gauge.standard();

        model.renderEntity(rollingStock, state, 0);
        model.postRenderEntity(rollingStock, state, 0);

        updateText();
        rollingStock.setTexture(prevTex);
        rollingStock.distanceTraveled = prevDist;
        rollingStock.gauge = prevGauge;
    }
}
