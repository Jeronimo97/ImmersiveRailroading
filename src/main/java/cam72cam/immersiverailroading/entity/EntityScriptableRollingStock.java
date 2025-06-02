package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.script.LuaLibrary;
import cam72cam.immersiverailroading.script.ScriptVectorUtil;
import cam72cam.immersiverailroading.textUtil.TextField;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.*;
import cam72cam.mod.text.PlayerMessage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.LuaValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock {
    private LuaValue load;
    private LuaValue save;
    private LuaValue tickEvent = LuaValue.NIL;
    public boolean isLuaLoaded = false;
    private boolean isSleeping;
    private long lastExecutionTime;
    private boolean wakeLuaScriptCalled = false;
    private final Map<String, InputStream> moduleMap = new HashMap<>();
    private final Map<String, String> componentTextMap = new HashMap<>();
    private final Map<Integer, ParticleState> particleStates = new HashMap<>();
    private final Map<Integer, ParticleState> oldParticleState = new HashMap<>();

    @TagField(value = "textFields", mapper = TextField.TextFieldMapMapper.class)
    public Map<String, TextField> textFields = new HashMap<>();

    public Globals globals;

    private final Set<ScheduleEvent> schedule = new HashSet<>();

    @Override
    public void onTick() {
        super.onTick();
        if (getWorld().isClient) {
            return;
        }
        if (getDefinition().script != null && !ConfigPerformance.disableLuaScript) {
            if (!isLuaLoaded) {
                try {
                    loadLuaFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


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
            callFunction();
        }

        schedule.removeIf(t -> {
            t.ticks--;
            if (t.ticks <= 0) {
                t.runnable.run();
                return true;
            }
            return false;
        });
    }


    @Override
    public ClickResult onClick(Player player, Player.Hand hand) {
        if (player.getHeldItem(hand).is(IRItems.ITEM_TYPEWRITER) && !textFields.isEmpty() && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
            ItemTypewriter.onStockInteract(this, player, hand);
            return ClickResult.ACCEPTED;
        }
        return super.onClick(player, hand);
    }

    @Override
    public void kill() {
        super.kill();
        getDefinition().inputs.remove(getUUID());
    }

    public void loadLuaFile() throws IOException {
        globals = JsePlatform.standardGlobals();

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
            ModCore.error(String.format("Script file %s does not exist", identifier));
            return;
        }

        String luaScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        if (luaScript == null) {
            ModCore.error(String.format("Lua script file %s not found", identifier));
            return;
        } else if (luaScript.isEmpty()) {
            ModCore.error(String.format("Lua script %s 's content is empty", identifier));
            return;
        }

        LuaLibrary.create("IR")
                .addFunction("setCG", (control, val) -> this.setControlGroup(control.tojstring(), val.tofloat()))
                .addFunctionWithReturn("getCG", control -> LuaValue.valueOf(this.getControlGroup(control.tojstring())))
                .addFunction("setPaint", newTexture -> this.setNewTexture(newTexture.tojstring()))
                .addFunctionWithReturn("getPaint", () -> LuaValue.valueOf(this.getCurrentTexture()))
                .addFunctionWithReturn("getReadout", readout -> LuaValue.valueOf(this.getReadout(readout.tojstring())))
                .addFunction("setPerformance", this::setPerformance)
                .addFunctionWithReturn("getPerformance", this::getPerformance)
                .addFunction("couplerEngaged", this::setCouplerEngagedLua)
                .addFunction("setThrottle", this::setThrottleLua)
                .addFunctionWithReturn("getThrottle", this::getThrottleLua)
                .addFunction("setReverser", this::setReverserLua)
                .addFunctionWithReturn("getReverser", this::getReverserLua)
                .addFunction("setTrainBrake", this::setTrainBrakeLua)
                .addFunctionWithReturn("getTrainBrake", this::getTrainBrakeLua)
                .addFunction("setIndependentBrake", this::setIndependentBrakeLua)
                .addFunction("setSound", val -> {/*this.setNewSound(val)*/})
                .addFunction("setGlobal", (control, val) -> this.setGlobalControlGroup(control.tojstring(), val.tofloat()))
                .addFunction("setUnit", (control, val) -> this.setUnitControlGroup(control.tojstring(), val.tofloat()))
                .addFunction("setText", table -> {/*textFieldDef(table)*/})
                .addFunction("setTag", val -> this.setEntityTag(val.tojstring()))
                .addFunctionWithReturn("getTag", () -> LuaValue.valueOf(this.getTag()))
                .addFunctionWithReturn("getTrain", this::getTrainConsist)
                .addFunction("setIndividualCG", this::setIndividualCG)
                .addFunctionWithReturn("getIndividualCG", this::getIndividualCG)
                .addFunctionWithReturn("isTurnedOn", () -> LuaValue.valueOf(this.getEngineState()))
                .addFunction("engineStartStop", this::setTurnedOnLua)
                .addFunction("newParticle", this::particleDefinition)
                // TODO re-add nbt saving and loading
                .addFunction("setNBTTag", (k, v) -> {/*this.setNBTTag(k.tojstring(), v)*/})
                .addFunction("getNBTTag", (k) -> {/*this.getNBTTag(k.tojstring())*/})
                .addFunctionWithReturn("getStockPosition", () -> ScriptVectorUtil.constructVec3Table(this.getPosition()))
                .addFunctionWithReturn("getStockMatrix", () -> ScriptVectorUtil.constructMatrix4Table(this.getModelMatrix()))
                .addFunctionWithReturn("newVector", (x, y, z) -> ScriptVectorUtil.constructVec3Table(x, y, z))
                .addFunctionWithReturn("getCoupled", this::getCoupled)
                .addFunctionWithReturn("initTextField", this::initTextField)
                .setInGlobals(globals);

        LuaLibrary.create("World")
                .addFunctionWithReturn("isRainingAt", pos -> LuaValue.valueOf(this.getWorld().isRaining(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getTemperatureAt", pos -> LuaValue.valueOf(this.getWorld().getTemperature(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getSnowLevelAt", pos -> LuaValue.valueOf(this.getWorld().getSnowLevel(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getBlockLightLevelAt", pos -> LuaValue.valueOf(this.getWorld().getBlockLightLevel(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getSkyLightLevelAt", pos -> LuaValue.valueOf(this.getWorld().getSkyLightLevel(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getTicks", () -> LuaValue.valueOf(this.getWorld().getTicks()))
                .setInGlobals(globals);

        LuaLibrary.create("Debug")
                .addFunction("printToInfoLog", arg -> ModCore.info(arg.tojstring()))
                .addFunction("printToWarnLog", arg -> ModCore.warn(arg.tojstring()))
                .addFunction("printToErrorLog", arg -> ModCore.error(arg.tojstring()))
                .addFunction("printToPassengerDialog", arg -> this.getPassengers().stream()
                        .filter(Entity::isPlayer)
                        .map(Entity::asPlayer)
                        .forEach(player -> player.sendMessage(PlayerMessage.direct(arg.tojstring()))))
                .setInGlobals(globals);

        LuaLibrary.create("Utils")
                .addFunction("wait", this::luaWait)
                .setInGlobals(globals);

        ScriptVectorUtil.VecUtil.setInGlobals(globals);

        if (getDefinition().addScripts != null) {
            for (String modules : getDefinition().addScripts) {
                Identifier newModule = identifier.getRelative(modules);
                moduleMap.put(modules.replace(".lua", ""), newModule.getResourceStream());
            }
            preloadModules(globals, moduleMap);
        }

        LuaValue chunk = globals.load(luaScript);
        chunk.call();

        tickEvent = globals.get("tickEvent");
        if (tickEvent.isnil()) {
            ModCore.error(String.format("Function \"tickEvent\" in lua script %s is not defined!", identifier));
        }

        load = globals.get("load");
        ModCore.info(String.format("Lua environment from %s initialized and script loaded successfully", this.defID));
        isLuaLoaded = true;
    }

    private void preloadModules(Globals globals, Map<String, InputStream> moduleStreams) {
        LuaValue packageLib = globals.get("package");
        LuaValue preloadTable = packageLib.get("preload");

        for (Map.Entry<String, InputStream> entry : moduleStreams.entrySet()) {
            String moduleName = entry.getKey();
            InputStream moduleStream = entry.getValue();

            try {
                LuaValue chunk = globals.load(moduleStream, moduleName, "bt", globals);
                preloadTable.set(moduleName, chunk);
            } catch (Exception e) {
                ModCore.error("An error occurred while preloading lua modules", e);
            }
        }
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
        Readouts readouts = Readouts.valueOf(readout.toUpperCase());
        return readouts.getValue(this);
    }

    public void luaWait(LuaValue sec, LuaValue func) {
        float seconds = sec.tofloat();
        Runnable runnable = () -> {
            try {
                func.call();
            } catch (Exception e) {
                ModCore.error("[Lua] Error while executing scheduled wait function: " + e.getMessage());
            }
        };

        int ticks = Math.round(seconds * 20);
        ScheduleEvent event = new ScheduleEvent(runnable, ticks, func);
        schedule.add(event);
    }

    protected void setPerformance(LuaValue performanceType, LuaValue val) {
        /* */
    }

    public void setCouplerEngagedLua(LuaValue positionLua, LuaValue engagedLua) {
        String position = positionLua.tojstring();
        boolean engaged = engagedLua.toboolean();
        switch (position) {
            case "FRONT":
                setCouplerEngaged(CouplerType.FRONT, engaged);
                break;
            case "BACK":
                setCouplerEngaged(CouplerType.BACK, engaged);
                break;
        }
    }

    public LuaValue initTextField(LuaValue group, LuaValue resX, LuaValue resY) {
        String groupName = String.format("TEXTFIELD_%s", group.tojstring());
        List<Mesh.Group> groupList = this.getDefinition().getMesh().getGroupContains(groupName);

        if (groupList.isEmpty()) {
            ModCore.error("[Lua] Found no TextField named %s in: %s", groupName, this.defID);
            return new LuaTable();
        }

        if (groupList.size() > 1) {
            ModCore.info("[Lua] Found more than one TextField defined as %s, using first!", groupName);
        }

        TextField textField = this.textFields.computeIfAbsent(groupName, t -> TextField.createTextField(groupList.get(0), resX.toint(), resY.toint(), f -> f.setSelectable(false)));
        LuaLibrary lib = LuaLibrary.create();

        LuaTable[] funcHolder = new LuaTable[1];

        lib.addFunctionWithReturn("setFont", f -> {
            textField.setFont(new Identifier(f.tojstring()));
            return funcHolder[0];
        }).addFunctionWithReturn("setColor", c -> {
            textField.setColor(c.tojstring());
            return funcHolder[0];
        }).addFunctionWithReturn("setFullbright", f -> {
            textField.setFullBright(f.toboolean());
            return funcHolder[0];
        }).addFunctionWithReturn("setAlign", a -> {
            textField.setAlign(a.tojstring());
            return funcHolder[0];
        }).addFunctionWithReturn("setGap", g -> {
            textField.setGap(g.toint());
            return funcHolder[0];
        }).addFunctionWithReturn("setSpacing", s -> {
            textField.setOffset(s.toint());
            return funcHolder[0];
        }).addFunctionWithReturn("setText", t -> {
            textField.setText(t.tojstring());
            return funcHolder[0];
        }).addFunctionWithReturn("setGlobal", g -> {
            textField.setGlobal(g.toboolean());
            return funcHolder[0];
        }).addFunctionWithReturn("update", () -> {
            new TextField.PacketSyncTextField(this, this.textFields).sendToObserving(this);
            return funcHolder[0];
        });

        funcHolder[0] = lib.getAsTable();

        return funcHolder[0];
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

    private LuaTable getCoupled(LuaValue type) {
        LuaTable table = new LuaTable();
        String sType = type.tojstring();

        EntityCoupleableRollingStock stock = getCoupled(sType.equalsIgnoreCase("front") ? CouplerType.FRONT : CouplerType.BACK);
        CouplerType coupler = this.getCouplerFor(stock);
        UUID uuid = stock.getUUID();
        String defID = stock.defID;
        String tag = stock.tag;

        table.set("coupler", coupler.toString());
        table.set("uuid", uuid.toString());
        table.set("defID", defID);
        table.set("tag", tag);
        return table;
    }

    private String getTag() {
        return this.tag;
    }

    public void callFunction() {
        if (!tickEvent.isnil()) {
            tickEvent.call();
        }
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

    @Deprecated
    public void setNewSound(LuaValue result) {
    }

    protected LuaValue getPerformance(LuaValue type) {
        String strType = type.tojstring();
        switch (strType) {
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

    @Deprecated
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

    public void addAllTextFields(Map<String, TextField> textFields) {
        this.textFields.putAll(textFields);
        new TextField.PacketSyncTextField(this, this.textFields).sendToObserving(this);
    }

    @Deprecated
    private void particleDefinition(LuaValue result) {
    }

    @Deprecated
    private void newParticle(Map<Integer, ParticleState> particle) {
    }

    public void setThrottleLua(LuaValue val) {

    }

    public void setReverserLua(LuaValue val) {

    }

    public void setTrainBrakeLua(LuaValue val) {

    }

    public void setIndependentBrakeLua(LuaValue val) {

    }

    public LuaValue getThrottleLua() {
        return LuaValue.valueOf(0);
    }

    public LuaValue getReverserLua() {
        return LuaValue.valueOf(0);
    }

    public LuaValue getTrainBrakeLua() {
        return LuaValue.valueOf(0);
    }

    @Override
    public void wakeLuaScript() {
        super.wakeLuaScript();
        wakeLuaScriptCalled = true;
    }

    public void setTurnedOnLua(LuaValue b) {
    }

    private static class ScheduleEvent {
        public Runnable runnable;
        public Integer ticks;
        public LuaValue func;

        public ScheduleEvent(Runnable runnable, Integer ticks, LuaValue func) {
            this.runnable = runnable;
            this.ticks = ticks;
            this.func = func;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            ScheduleEvent that = (ScheduleEvent) object;
            return func.equals(that.func);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(func);
        }
    }
}
