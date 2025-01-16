package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.gui.overlay.ReadoutsEventHandler;
import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.items.ItemGoldenSpike;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.CustomParticleConfig;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.LuaValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.luaj.vm2.LuaFunction;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock implements ReadoutsEventHandler {

    private LuaValue tickEvent;
    public boolean isLuaLoaded = false;
    private boolean isSleeping;
    private long lastExecutionTime;
    private boolean wakeLuaScriptCalled = false;
    LuaTable luaFunction = new LuaTable();
    private final Map<String, InputStream> moduleMap = new HashMap<>();
    private final Map<String, String> componentTextMap = new HashMap<>();
    private final Map<Integer, ParticleState> particleStates = new HashMap<>();
    private final Map<Integer, ParticleState> oldParticleState = new HashMap<>();

    private final List<TextRenderOptions> textFields = new ArrayList<>();

    /**
     *
     * Sad to say that this doesn't work, the data required for the text fields needs to much memory to save to NBT data,
     * maybe I will come back to it later.
     * TODO find another way of saving data from the text fields.
     *
     */

//    @Override
//    public void load(TagCompound data) {
//        super.load(data);
//        int index = 0;
//        while (data.hasKey("textField_" + index)) {
//            TagCompound optionNBT = data.get("textField_" + index);
//            TextRenderOptions textRenderOptions = null;
//            try {
//                textRenderOptions = new TextRenderOptions(optionNBT);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            textFields.add(textRenderOptions);
//            index++;
//        }
//        textFields.forEach(o -> {
//            if (o.global) {
//                setAllText(o);
//            } else {
//                try {
//                    setText(o);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//    }
//
//    @Override
//    public void save(TagCompound data) {
//        super.save(data);
//
//        for (int i = 0; i < textFields.size(); i++) {
//            TagCompound optionNBT = new TagCompound();
//            try {
//                textFields.get(i).serializeTextRenderOptions(optionNBT);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            data.set("textField_" + i, optionNBT);
//        }
//    }



    @Override
    public void onTick() {
        super.onTick();
        if (!getWorld().isServer) {
            return;
        }
        if (getDefinition().script != null && !ConfigPerformance.disableLuaScript) {
            long currentTime = System.currentTimeMillis();

            // Handle sleep logic if luaScriptSleep is greater than 0
            if (ConfigPerformance.luaScriptSleep > 0) {
                long elapsedTime = currentTime - lastExecutionTime;

                if (!isSleeping && elapsedTime >= ConfigPerformance.luaScriptSleep * 1000L) {
                    isSleeping = true;
                }

                if (isSleeping) {
                    if (wakeLuaScriptCalled || getPassengerCount() > 0) {
                        wakeLuaScriptCalled = false;
                        isSleeping = false;
                        lastExecutionTime = currentTime;
                    } else {
                        return;
                    }
                }
            } else {
                // If luaScriptSleep is 0, reset sleep state
                isSleeping = false;
                lastExecutionTime = currentTime;
            }

            // Execute Lua script if not sleeping or luaScriptSleep is 0
            try {
                if (LoadLuaFile()) return;
                callFuction();
                getReadout();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ClickResult onClick(Player player, Player.Hand hand) {
        if (player.getHeldItem(hand).is(IRItems.ITEM_GOLDEN_SPIKE)) {
            GuiTypes.TEXT_FIELD.open(player);
            return ClickResult.ACCEPTED;
        }
        return super.onClick(player, hand);
    }

    public boolean LoadLuaFile() throws IOException {
        if (!isLuaLoaded) {
            Globals globals = JsePlatform.standardGlobals();

            LuaValue safeOs = LuaValue.tableOf();
            safeOs.set("time", globals.get("os").get("time"));
            safeOs.set("date", globals.get("os").get("date"));
            safeOs.set("clock", globals.get("os").get("clock"));
            safeOs.set("difftime", globals.get("os").get("difftime"));


            globals.set("os", safeOs);

            globals.set("io", LuaValue.NIL);
            globals.set("luajava", LuaValue.NIL);
            globals.set("coroutine", LuaValue.NIL);
            globals.set("debug", LuaValue.NIL);


            // Get Lua file from Json
            Identifier script = getDefinition().script;
            Identifier identifier = new Identifier(script.getDomain(), script.getPath());
            InputStream inputStream = identifier.getResourceStream();

            if (inputStream == null) {
                ModCore.error(String.format("File %s does not exist", script.getDomain() + ":" + script.getPath()));
                return true;
            }

            String luaScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (luaScript == null || luaScript.isEmpty()) {
                ModCore.error("Lua script content is empty | file not found");
                return true;
            }

            if (getDefinition().addScripts != null) {
                for (String modules : getDefinition().addScripts) {
                    Identifier newModule = identifier.getRelative(modules);
                    moduleMap.put(modules.replace(".lua", ""), newModule.getResourceStream());
                }
                preloadModules(globals, moduleMap);
            }

            setLuaFunctions();

            globals.set("IR", luaFunction);

            LuaValue chunk = globals.load(luaScript);
            chunk.call();

            tickEvent = globals.get("tickEvent");
            if (tickEvent.isnil()) {
                ModCore.error("Function 'tickEvent' is not Defined!");
            }

            isLuaLoaded = true;
            ModCore.info("Lua environment initialized and script loaded successfully");
        }
        return false;
    }

    private void preloadModules(Globals globals, Map<String, InputStream> moduleStreams) {
        LuaValue packageLib = globals.get("package");
        LuaValue preloadTable = packageLib.get("preload");

        for (Map.Entry<String, InputStream> entry : moduleStreams.entrySet()) {
            String moduleName = entry.getKey();
            InputStream moduleStream = entry.getValue();

            try {
                // Step 5: Load the Lua chunk from the InputStream
                LuaValue chunk = globals.load(moduleStream, moduleName, "bt", globals);

                // Step 6: Add the chunk to package.preload with the module name
                preloadTable.set(moduleName, chunk);
            } catch (Exception e) {
                ModCore.error("An error occurred while preloading lua modules", e);
            }
        }
    }

    public void setLuaFunctions() {
        luaFunction.set("getCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control) {
                float result = getControlGroup(control.tojstring());
                return LuaValue.valueOf(result);
            }
        });
        luaFunction.set("setCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control, LuaValue val) {
                setControlGroup(control.tojstring(), val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getPaint", new LuaFunction() {
            @Override
            public LuaValue call() {
                String result = getCurrentTexture();
                return LuaValue.valueOf(result);
            }
        });
        luaFunction.set("setPaint", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue newTexture) {
                setNewTexture(newTexture.tojstring());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getReadout", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue readout) {
                float result = getReadout(readout.tojstring());
                return LuaValue.valueOf(result);
            }
        });
        luaFunction.set("setPerformance", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue performanceType, LuaValue newVal) {
                setPerformance(performanceType.tojstring(), newVal.todouble());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("couplerEngaged", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue position, LuaValue newState) {
                setCouplerEngaged(position.tojstring(), newState.toboolean());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setThrottle", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setThrottleLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setReverser", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setReverserLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setTrainBrake", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setBrakeLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setIndependentBrake", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setIndependentBrakeLua(val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getThrottle", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getThrottleLua());
            }
        });
        luaFunction.set("getReverser", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getReverserLua());
            }
        });
        luaFunction.set("getTrainBrake", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getTrainBrakeLua());
            }
        });
        luaFunction.set("setSound", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue val) {
                setNewSound(val);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setGlobal", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control, LuaValue val) {
                setGlobalControlGroup(control.tojstring(), val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setUnit", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue control, LuaValue val) {
                setUnitControlGroup(control.tojstring(), val.tofloat());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("setText", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue table) {
                textFieldDef(table);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getTag", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getTag());
            }
        });
        luaFunction.set("getTrain", new LuaFunction() {
            @Override
            public LuaValue call() {
                return getTrainConsist();
            }
        });
        luaFunction.set("setIndividualCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                setIndividualCG(luaValue);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getIndividualCG", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                return getIndividualCG(luaValue);
            }
        });
        luaFunction.set("engineStartStop", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                setTurnedOnLua(luaValue.toboolean());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("isTurnedOn", new LuaFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(getEngineState());
            }
        });
        luaFunction.set("setTag", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue tag) {
                setEntityTag(tag.tojstring());
                return LuaValue.NIL;
            }
        });
        luaFunction.set("newParticle", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                particleDefinition(luaValue);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getPerformance", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                return getPerformance(luaValue.tojstring());
            }
        });
        luaFunction.set("setNBTTag", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue key, LuaValue value) {
                setNBTTag(key.tojstring(), value);
                return LuaValue.NIL;
            }
        });
        luaFunction.set("getNBTTag", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue key) {
                return getNBTTag(key.tojstring());
            }
        });
    }

    public float getControlGroup(String control) {
        return getControlPosition(control);
    }

    public void setControlGroup(String control, float val) {
        controlPositions.put(control, Pair.of(false, val));
    }

    public void setGlobalControlGroup(String controlName, float newValControl) {
        this.mapTrain(this, false, stock -> {
            stock.controlPositions.put(controlName, Pair.of(false, newValControl));
        });
    }

    public void setUnitControlGroup(String controlName, float newValControl) {
        List<List<EntityCoupleableRollingStock>> units = this.getUnit(false);
        for (List<EntityCoupleableRollingStock> unit : units) {
            if (unit.contains(this)) {
                for (EntityCoupleableRollingStock stock : unit) {
                    stock.controlPositions.put(controlName, Pair.of(false, newValControl));
                }
                break;
            }
        }
    }

    public String getCurrentTexture() {
        String currentTexture = getTexture();
        if (currentTexture == null) {
            currentTexture = "Default";
        }
        return currentTexture;
    }

    public void setNewTexture(String texture) {
        setTexture(texture);
    }

    public float getReadout(String readout) {
        getReadout();
        return readoutState.get(readout);
    }

    public void setPerformance(String performanceType, double val) {
        switch (performanceType) {
            case "max_speed_kmh":
                getDefinition().setMaxSpeed(val);
                break;
            case "tractive_effort_lbf":
                getDefinition().setTraction(val);
                break;
            case "horsepower":
                getDefinition().setHorsepower(val);
                break;
        }
    }

    public void setCouplerEngaged(String position, Boolean engaged) {
        switch (position) {
            case "FRONT":
                setCouplerEngaged(CouplerType.FRONT, engaged);
                break;
            case "BACK":
                setCouplerEngaged(CouplerType.BACK, engaged);
                break;
        }
    }

    public void textFieldDef(LuaValue result) {
        Map<String, DataBlock.Value> textField = new HashMap<>();
        if (result.istable()) {
            for (LuaValue key : result.checktable().keys()) {
                DataBlock.Value value = new ObjectValue(convertLuaValueText(key, result.get(key))); // Fixed here
                textField.put(key.tojstring(), value);
            }
        }

        Identifier font = textField.get("font") != null ? textField.get("font").asIdentifier() : null;
        String textFieldId = textField.get("ID") != null ? textField.get("ID").asString() : null;
        int resX = textField.get("resX") != null ? textField.get("resX").asInteger() : 0;
        int resY = textField.get("resY") != null ? textField.get("resY").asInteger() : 0;
        boolean flipped = textField.get("flipped") != null && textField.get("flipped").asBoolean();
        int textureHeight = textField.get("textureHeight") != null ? textField.get("textureHeight").asInteger() : 12;
        int fontSize = textField.get("fontSize") != null ? textField.get("fontSize").asInteger() : textureHeight;
        int fontLength = textField.get("textureWidth") != null ? textField.get("textureWidth").asInteger() : 512;
        int fontGap = textField.get("fontGap") != null ? textField.get("fontGap").asInteger() : 1;
        Identifier overlay = textField.get("overlay") != null ? textField.get("overlay").asIdentifier() : null;
        String hexCode = textField.get("color") != null ? textField.get("color").asString() : null;
        boolean fullbright = textField.get("fullbright") != null ? textField.get("fullbright").asBoolean() : false;
        boolean allStock = textField.get("global") != null ? textField.get("global").asBoolean() : false;
        boolean useAlternative = textField.get("useAltAlignment") != null ? textField.get("useAltAlignment").asBoolean() : false;
        int lineSpacingPixels = textField.get("lineSpacing") != null ? textField.get("lineSpacing").asInteger() : 1;
        int offset = textField.get("offset") != null ? textField.get("offset").asInteger() : 0;

        Font.TextAlign align;
        if (textField.get("align") != null) {
            String alignStr = textField.get("align").asString();
            if (alignStr.equalsIgnoreCase("right")) {
                align = Font.TextAlign.RIGHT;
            } else if (alignStr.equalsIgnoreCase("center")) {
                align = Font.TextAlign.CENTER;
            } else {
                align = Font.TextAlign.LEFT;
            }
        } else {
            align = Font.TextAlign.LEFT;
        }

        String text = textField.get("text") != null ? textField.get("text").asString() : "";
        try {
            TextRenderOptions options = new TextRenderOptions(
                    font, text, resX, resY, align, flipped, textFieldId, fontSize, fontLength, fontGap, overlay, hexCode, fullbright, textureHeight, useAlternative, lineSpacingPixels, offset, allStock
            );
            textFields.add(options);
            if (allStock) {
                setAllText(options);
            } else {
                setText(options);
            }
        } catch (IOException e) {
            ModCore.error(String.format("Couldn't load Font %s", font != null ? font.toString() : "null"));
        }
    }

    private Vec3d getVec3dmin (List<Vec3d> vectors) {
        return vectors.stream().min(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    private Vec3d getVec3dmax (List<Vec3d> vectors) {
        return vectors.stream().max(Comparator.comparingDouble(Vec3d::length)).orElse(null);
    }

    public void setAllText(TextRenderOptions options) {
        this.mapTrain(this, false, stock -> {
            if (stock == null) {
                ModCore.error("Stock is null when setting text.");
                return;
            }
            if (options == null) {
                ModCore.error("Options is null when setting text.");
                return;
            }
            stock.setTextTrain(options);
        });
    }

    private void setText(TextRenderOptions options) throws IOException {
        String currentText = componentTextMap.get(options.componentId);
        if (!options.newText.equals(currentText)) {
            LinkedHashMap<String, OBJGroup> group = this.getDefinition().getModel().groups;
            for (Map.Entry<String, OBJGroup> entry : group.entrySet()) {
                if (entry.getKey().contains(String.format("TEXTFIELD_%s", options.componentId))) {
                    EntityRollingStockDefinition.Position getPosition = getDefinition().normals.get(entry.getKey());
                    Vec3d vec3dmin = getVec3dmin(getPosition.vertices);
                    Vec3d vec3dmax = getVec3dmax(getPosition.vertices);
                    Vec3d vec3dNormal = getPosition.normal;
                    RenderText renderText = RenderText.getInstance(String.valueOf(getUUID()));
                    File file = new File(options.id.getPath());
                    String jsonPath = file.getName();
                    Identifier jsonId = options.id.getRelative(jsonPath.replaceAll(".png", ".json"));
                    InputStream json = jsonId.getResourceStream();

                    renderText.setText(
                            options.componentId, options.newText, options.id, vec3dmin, vec3dmax, json,
                            options.resX, options.resY, options.align, options.flipped, options.fontSize, options.fontX,
                            options.fontGap, options.overlay, vec3dNormal, options.hexCode, options.fullbright, options.textureHeight, options.useAlternative, options.lineSpacingPixels, options.offset, entry.getKey()
                    );

                    componentTextMap.put(options.componentId, options.newText);
                }
            }
        }
    }

    public LuaValue getTrainConsist() {
        List<List<EntityCoupleableRollingStock>> unit = getUnit(false);
        LuaValue table = LuaValue.tableOf();

        for (int i = 0; i < unit.size(); i++) {
            List<EntityCoupleableRollingStock> oneUnit = unit.get(i);
            LuaValue luaUnit = LuaValue.tableOf();

            for (int j = 0; j < oneUnit.size(); j++) {
                EntityCoupleableRollingStock obj = oneUnit.get(j);

                LuaValue objTable = LuaValue.tableOf();

                objTable.set("UUID", LuaValue.valueOf(obj.getUUID().toString()));

                objTable.set("defID", obj.defID != null ? LuaValue.valueOf(obj.defID) : LuaValue.NIL);

                objTable.set("coupledFront", obj.coupledFront != null ? LuaValue.valueOf(obj.coupledFront.toString()) : LuaValue.valueOf(""));

                objTable.set("coupledBack", obj.coupledBack != null ? LuaValue.valueOf(obj.coupledBack.toString()) : LuaValue.valueOf(""));

                objTable.set("distanceTraveled", obj.distanceTraveled != 0 ? LuaValue.valueOf(obj.distanceTraveled) : LuaValue.valueOf(0));
                LuaTable controlPosition = LuaValue.tableOf();

                obj.controlPositions.forEach((key, pair) -> {
                    LuaValue booleanValue = LuaValue.valueOf(pair.getLeft());
                    LuaValue floatValue = LuaValue.valueOf(pair.getRight());

                    LuaValue pairTable = LuaValue.tableOf(new LuaValue[]{ booleanValue, floatValue });

                    controlPosition.set(LuaValue.valueOf(key), pairTable);
                });

                objTable.set("controlPositions", controlPosition);
                objTable.set("tag", obj.tag);
                luaUnit.set(j + 1, objTable);
            }
            table.set(i + 1, luaUnit);
        }
        return table;
    }

    private String getTag() {
        return this.tag;
    }

    public void callFuction() {
        tickEvent.call();
    }

    @TagField(value = "luaData", mapper = LuaDataMapper.class)
    protected Map<String, Object> luaData = new HashMap<>();

    public void setNBTTag(String key, LuaValue table) {
        luaData.put(key, convertData(table));
    }

    private LuaValue getNBTTag(String key) {
        return convertToLuaValue(luaData.get(key));
    }
    
    public void setLuaTag(String key, Object value) {
        luaData.put(key, value);
    }
    
    public Object getLuaTag(String key) {
        return luaData.get(key);
    }

    public void setIndividualCG(LuaValue stockUnit) {
        List<List<EntityCoupleableRollingStock>> allUnit = getUnit(false);
        int unit = stockUnit.checktable().get(1).toint();
        int stock = stockUnit.checktable().get(2).toint();
        String control = stockUnit.checktable().get(3).tojstring();
        float value = stockUnit.checktable().get(4).tofloat();
        List<EntityCoupleableRollingStock> individualUnit = allUnit.get(unit - 1);
        if (allUnit.size() <= unit && individualUnit.size() <= stock) {
            EntityCoupleableRollingStock individualStock = individualUnit.get(stock - 1);
            individualStock.controlPositions.put(control, Pair.of(false, value));
        }
    }

    public LuaValue getIndividualCG(LuaValue stockUnit) {
        List<List<EntityCoupleableRollingStock>> allUnit = getUnit(false);
        int unit = stockUnit.checktable().get(1).toint();
        int stock = stockUnit.checktable().get(2).toint();
        String control = stockUnit.checktable().get(3).tojstring();

        List<EntityCoupleableRollingStock> individualUnit = allUnit.get(unit - 1);
        if (allUnit.size() <= unit && individualUnit.size() <= stock) {
            EntityCoupleableRollingStock individualStock = individualUnit.get(stock - 1);
            Pair<Boolean, Float> value = individualStock.controlPositions.containsKey(control) ? individualStock.controlPositions.get(control) : Pair.of(false, 0f);
            return LuaValue.valueOf(value.getRight());
        } else {
            return LuaValue.valueOf(0);
        }
    }


    public void setNewSound(LuaValue result) {
        List<Map<String, DataBlock.Value>> newSound = new ArrayList<>();

        if (result.istable()) {

            for (LuaValue key : result.checktable().keys()) {

                LuaValue entry = result.get(key);
                Map<String, DataBlock.Value> soundDefinition = new HashMap<>();

                for (LuaValue entrykey : entry.checktable().keys()) {
                    LuaValue value = entry.get(entrykey);
                    DataBlock.Value soundDef = new ObjectValue(convertLuaValue(value));
                    soundDefinition.put(entrykey.tojstring(), soundDef);
                }
                newSound.add(soundDefinition);
            }
            getDefinition().setSounds(newSound, this);
        }
    }

    private LuaValue getPerformance(String type) {
        switch (type) {
            case "max_speed_kmh":
                return LuaValue.valueOf(getDefinition().getMaxSpeed());
            case "horsepower":
                return LuaValue.valueOf(getDefinition().getHorsepower());
            case "traction":
                return LuaValue.valueOf(getDefinition().getTraction());
            default:
                return LuaValue.valueOf(0);
        }
    }

    private void accept(Boolean key, List<TextRenderOptions> options) {
        if (key.equals(true)) {
            options.forEach(this::setAllText);
        } else {
            try {
                for (TextRenderOptions option : options) {
                    setText(option);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ParticleState {
        Vec3d motion;
        int lifespan;
        float darken;
        float thickness;
        double diameter;
        boolean alwaysRunning;
        Identifier texture;
        boolean render;
        int r;
        int g;
        int b;
        double a;
        double expansionRate;
        boolean normalWidth;

        ParticleState(Vec3d motion, int lifespan, float darken, float thickness, double diameter,Identifier texture, boolean alwaysRunning, boolean render, int r, int g, int b, double a, double expansionRate, boolean normalWidth) {
            this.motion = motion;
            this.lifespan = lifespan;
            this.darken = darken;
            this.thickness = thickness;
            this.diameter = diameter;
            this.alwaysRunning = alwaysRunning;
            this.texture = texture;
            this.render = render;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.expansionRate = expansionRate;
            this.normalWidth = normalWidth;

        }
        boolean equals(ParticleState other) {
            if (other == null) return false;
            return this.motion.equals(other.motion) &&
                    this.lifespan == other.lifespan &&
                    this.darken == other.darken &&
                    this.thickness == other.thickness &&
                    this.diameter == other.diameter &&
                    this.texture.equals(other.texture) &&
                    this.alwaysRunning == other.alwaysRunning;
        }
    }

    private void particleDefinition(LuaValue result) {
        if (result.istable()) {
            DataBlock data = luaTableToDataBlock(result);

            Identifier texture = data.getValue("texture").asIdentifier(new Identifier(ImmersiveRailroading.MODID, "textures/light.png"));
            int id = data.getValue("id").asInteger(1);
            List<DataBlock.Value> motionData = data.getValuesMap().get("motion");
            Vec3d motion = new Vec3d(motionData.get(2).asDouble(0), motionData.get(1).asDouble(1), motionData.get(0).asDouble(0));
            int lifespan = data.getValue("lifespan").asInteger(25);
//            float darken = data.getValue("darken").asFloat(0);
            float thickness = data.getValue("thickness").asFloat(0.1f);
            Double diameter = data.getValue("diameter").asDouble();
            boolean normalWidth = diameter == null;
            boolean alwaysRunning = data.getValue("alwaysRunning").asBoolean(false);
            boolean render = data.getValue("renders").asBoolean(true);
            List<DataBlock.Value> rgba = data.getValuesMap().get("colorRGBA");
            int r;
            int g;
            int b;
            double a;
            if (rgba != null) {
                r = rgba.get(0).asInteger(240);
                g = rgba.get(1).asInteger(240);
                b = rgba.get(2).asInteger(240);
                a = rgba.get(3).asDouble(10);
            } else {
                r = 240;
                g = 240;
                b = 240;
                a = 10;
            }
            String hex = data.getValue("colorHEX").asString();
            if (hex != null && hex.matches("^#([A-Fa-f0-9]{6})$")) {
                r = Integer.parseInt(hex.substring(1, 3), 16);
                g = Integer.parseInt(hex.substring(3, 5), 16);
                b = Integer.parseInt(hex.substring(5, 7), 16);
            }
            double expansionRate = data.getValue("expansionRate").asDouble(16);

            particleStates.put(id, new ParticleState(motion, lifespan, 0f, thickness, diameter != null ? diameter : 0.25, texture, alwaysRunning, render, r, g, b, a, expansionRate, normalWidth));

            newParticle(particleStates);
        }
    }

    private void newParticle(Map<Integer, ParticleState> particle) {
        particle.forEach((id, particleState) -> {
            if (!particleState.equals(oldParticleState.get(id))) {
                List<ModelComponent> components = this.getDefinition().getModel().allComponents;
                ModelComponent component = components.stream().filter(c -> c.id.equals(id) && c.type == ModelComponentType.CUSTOM_PARTICLE_X).findFirst().orElse(null);
                if (component == null) {
                    ModCore.error(String.format("Custom particle object CUSTOM_PARTICLE_%s couldn't be found in model %s", id, this.getDefinition().modelLoc.toString()));
                    return;
                }
                CustomParticleConfig customParticleConfig = CustomParticleConfig.getInstance(component);
                Vec3d position = component.center;
                customParticleConfig.setConfig(
                        position, particleState.motion, particleState.lifespan, particleState.darken,
                        particleState.thickness, particleState.diameter, particleState.texture, particleState.alwaysRunning,
                        particleState.render, particleState.r, particleState.g, particleState.b, particleState.a, particleState.expansionRate, particleState.normalWidth);
                oldParticleState.put(id, particleState);
            }
        });
    }

    private static DataBlock luaTableToDataBlock(LuaValue luaTable) {
        return new DataBlock() {
            private final Map<String, Value> valueMap = new HashMap<>();
            private final Map<String, List<Value>> valuesMap = new HashMap<>();
            private final Map<String, DataBlock> blockMap = new HashMap<>();
            private final Map<String, List<DataBlock>> blocksMap = new HashMap<>();
            {
                for (LuaValue key : luaTable.checktable().keys()) {
                    String keyString = key.tojstring();
                    LuaValue value = luaTable.get(key);
                    if (value.istable()) {
                        if (isListTable(value)) {
                            List<Value> valueList = new ArrayList<>();
                            for (int i = 1; i <= value.length(); i++) {
                                valueList.add(new ObjectValue(convertLuaValue(value.get(i))));
                            }
                            valuesMap.put(keyString, valueList);
                        } else {
                            blockMap.put(keyString, luaTableToDataBlock(value));
                        }
                    } else {
                        valueMap.put(keyString, new ObjectValue(convertLuaValue(value)));
                    }
                }
            }

            @Override
            public Map<String, Value> getValueMap() {
                return valueMap;
            }

            @Override
            public Map<String, List<Value>> getValuesMap() {
                return valuesMap;
            }

            @Override
            public Map<String, DataBlock> getBlockMap() {
                return blockMap;
            }

            @Override
            public Map<String, List<DataBlock>> getBlocksMap() {
                return blocksMap;
            }
        };
    }

    private static boolean isListTable(LuaValue luaTable) {
        int index = 1;
        for (LuaValue key : luaTable.checktable().keys()) {
            if (!key.isint() || key.toint() != index) {
                return false;
            }
            index++;
        }
        return true;
    }

    private static Object convertLuaValue(LuaValue luaValue) {
        if (luaValue.isboolean()) {
            return luaValue.toboolean();
        } else if (luaValue.isint()) {
            return luaValue.toint();
        } else if (luaValue.isnumber()) {
            return luaValue.todouble();
        } else if (luaValue.isstring()) {
            return luaValue.tojstring();
        } else if (luaValue.istable()) {
            return luaTableToDataBlock(luaValue);
        }
        return null;
    }

    private static Object convertLuaValueText(LuaValue k, LuaValue value) {
        if ("text".equals(k.tojstring())) {
            return value.tojstring();  // Force the value to be a string
        }

        if (value.isboolean()) {
            return value.toboolean();
        } else if (value.isnumber()) {
            return value.todouble();
        } else if (value.isstring()) {
            return value.tojstring();
        } else if (value.istable()) {
            Map<String, Object> nestedMap = new HashMap<>();
            for (LuaValue key : value.checktable().keys()) {
                LuaValue val = value.get(key);
                nestedMap.put(key.tojstring(), convertLuaValue(val));
            }
            return nestedMap;
        } else {
            return value; // Return raw LuaValue if type is unknown
        }
    }

    public void setThrottleLua(float val) {

    }

    public void setReverserLua(float val) {

    }

    public void setBrakeLua(float val) {

    }

    public void setIndependentBrakeLua(float val) {

    }

    public float getThrottleLua() {
        return 0;
    }

    public float getReverserLua() {
        return 0;
    }

    public float getTrainBrakeLua() {
        return 0;
    }

    @Override
    public void wakeLuaScript() {
        super.wakeLuaScript();
        wakeLuaScriptCalled = true;
    }

    public void setTurnedOnLua(boolean b) {
    }


    @Override
    public void setTextTrain(TextRenderOptions options) {
        try {
            setText(options);
        } catch (IOException e) {
            ModCore.error("An error occurred while creating textfields", e);
        }
    }

    public static class Data {
        boolean b;
        int i;
        long l;
        double d;
        String s;
        Map<?, ?> map = new HashMap<>();
        List<?> list;

        Data(LuaValue value) {
            if (value.istable()) {
                LuaTable table = value.checktable();
                boolean isList = true;
                for (LuaValue key : table.keys()) {
                    if (!key.isint()) {
                        isList = false;
                        break;
                    }
                }

                if (isList) {
                    List<Object> tempList = new ArrayList<>();
                    for (int i = 1; i <= table.length(); i++) {
                        LuaValue element = table.get(i);
                        tempList.add(convertData(element));
                    }
                    this.list = tempList;
                } else {
                    Map<Object, Object> tempMap = new HashMap<>();
                    for (LuaValue key : table.keys()) {
                        LuaValue val = table.get(key);
                        tempMap.put(convertData(key), convertData(val));
                    }
                    this.map = tempMap;
                }
            } else if (value.isboolean()) {
                this.b = value.toboolean();
            } else if (value.isint()) {
                this.i = value.toint();
            } else if (value.islong()) {
                this.l = value.tolong();
            } else if (value.isnumber()) {
                this.d = value.todouble();
            } else if (value.isstring()) {
                this.s = value.tojstring();
            }
        }
        public LuaValue toLuaValue() {
            LuaTable table = new LuaTable();

            // Add each field to the Lua table if it is non-null
            if (this.s != null) {
                table.set("s", LuaValue.valueOf(this.s));
            }
            if (this.b) {
                table.set("b", LuaValue.valueOf(this.b));
            }
            if (this.i != 0) {
                table.set("i", LuaValue.valueOf(this.i));
            }
            if (this.l != 0L) {
                table.set("l", LuaValue.valueOf(this.l));
            }
            if (this.d != 0.0) {
                table.set("d", LuaValue.valueOf(this.d));
            }

            if (this.map != null && !this.map.isEmpty()) {
                LuaTable mapTable = new LuaTable();
                for (Map.Entry<?, ?> entry : this.map.entrySet()) {
                    LuaValue key = convertToLuaValue(entry.getKey());
                    LuaValue val = convertToLuaValue(entry.getValue());
                    mapTable.set(key, val);
                }
                table.set("map", mapTable);
            }

            if (this.list != null && !this.list.isEmpty()) {
                LuaTable listTable = new LuaTable();
                int index = 1; // Lua lists are 1-based index
                for (Object element : this.list) {
                    LuaValue luaElement = convertToLuaValue(element);
                    listTable.set(index++, luaElement);
                }
                table.set("list", listTable);
            }

            return table;
        }

        private LuaValue convertToLuaValue(Object value) {
            if (value == null) {
                return LuaValue.NIL;
            } else if (value instanceof String) {
                return LuaValue.valueOf((String) value);
            } else if (value instanceof Boolean) {
                return LuaValue.valueOf((Boolean) value);
            } else if (value instanceof Integer) {
                return LuaValue.valueOf((Integer) value);
            } else if (value instanceof Long) {
                return LuaValue.valueOf((Long) value);
            } else if (value instanceof Double) {
                return LuaValue.valueOf((Double) value);
            } else if (value instanceof Data) {
                return ((Data) value).toLuaValue();
            } else if (value instanceof List) {
                LuaTable listTable = new LuaTable();
                int index = 1;
                for (Object element : (List<?>) value) {
                    listTable.set(index++, convertToLuaValue(element));
                }
                return listTable;
            } else if (value instanceof Map) {
                LuaTable mapTable = new LuaTable();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    LuaValue key = convertToLuaValue(entry.getKey());
                    LuaValue val = convertToLuaValue(entry.getValue());
                    mapTable.set(key, val);
                }
                return mapTable;
            }

            return LuaValue.NIL;
        }
    }

    private static Object convertData(LuaValue value) {
        if (value.isboolean()) {
            return value.toboolean();
        } else if (value.isint()) {
            return value.toint();
        } else if (value.islong()) {
            return value.tolong();
        } else if (value.isnumber()) {
            return value.todouble();
        } else if (value.isstring()) {
            return value.tojstring();
        } else if (value.istable()) {
            return new Data(value);
        }
        return null;
    }

    private static LuaValue convertToLuaValue(Object obj) {
        if (obj instanceof Boolean) {
            return LuaValue.valueOf((Boolean) obj);
        } else if (obj instanceof Integer) {
            return LuaValue.valueOf((Integer) obj);
        } else if (obj instanceof Long) {
            return LuaValue.valueOf((Long) obj);
        } else if (obj instanceof Double) {
            return LuaValue.valueOf((Double) obj);
        } else if (obj instanceof String) {
            return LuaValue.valueOf((String) obj);
        } else if (obj instanceof Data) {
            return ((Data) obj).toLuaValue();
        }
        return LuaValue.NIL;  // If the type doesn't match, return Lua NIL
    }

    private static class LuaDataMapper implements TagMapper<Map<String, Object>> {
        @Override
        public TagAccessor<Map<String, Object>> apply(Class<Map<String, Object>> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (tagCompound, map) -> {
                        // Serializer logic
                        tagCompound.setMap(fieldName, map, String::toString, this::convertToNBT);
                    },
                    tagCompound -> {
                        // Deserializer logic
                        return tagCompound.getMap(fieldName, key -> key, this::convertToLuaValue);
                    }
            );
        }

        private TagCompound convertToNBT(Object value) {
            TagCompound tag = new TagCompound();

            if (value instanceof String) {
                tag.setString("value", (String) value);
            } else if (value instanceof Integer) {
                tag.setInteger("value", (Integer) value);
            } else if (value instanceof Boolean) {
                tag.setBoolean("value", (Boolean) value);
            } else if (value instanceof Double) {
                tag.setDouble("value", (Double) value);
            } else if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                tag.setMap("value", map, String::toString, this::convertToNBT);
            }

            return tag;
        }

        private Object convertToLuaValue(TagCompound compound) {
            if (compound.hasKey("value")) {
                if (compound.getString("value") != null) {
                    return compound.getString("value");
                } else if (compound.getInteger("value") != null) {
                    return compound.getInteger("value");
                } else if (compound.getBoolean("value") != null) {
                    return compound.getBoolean("value");
                } else if (compound.getDouble("value") != null) {
                    return compound.getDouble("value");
                } else if (compound.get("value") != null) {
                    return compound.getMap("value", key -> key, this::convertToLuaValue);
                }
            }
            return null;
        }
    }
}
