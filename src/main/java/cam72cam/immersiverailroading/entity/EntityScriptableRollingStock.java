package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.Config.ConfigPerformance;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.immersiverailroading.script.LuaLibrary;
import cam72cam.immersiverailroading.script.MarkupLibrary;
import cam72cam.immersiverailroading.script.ScriptVectorUtil;
import cam72cam.immersiverailroading.script.SoundConfig;
import cam72cam.immersiverailroading.textUtil.Font;
import cam72cam.immersiverailroading.textUtil.FontLoader;
import cam72cam.immersiverailroading.textUtil.TextField;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock {
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
    @TagSync
    @TagField(value = "luaTagField", mapper = LuaMapper.class)
    private final Map<String, LuaValue> tagFields = new HashMap<>();
    private final Map<String, List<LuaValue>> luaEventCallbacks = new HashMap<>();
    public Map<String, SoundConfig> sounds = new HashMap<String, SoundConfig>();

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
            triggerEvent("onTick");
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
        //TODO DEBUG Test
        this.getDefinition().textFields.forEach((i,t) -> {
            textFields.put(i, t);
        });
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

        Map<String, LuaValue> tempMap = new HashMap<>();
        globals.set("_TagField", createTagField(tempMap));

        // Get Lua file from Json
        Identifier script = getDefinition().script;
        InputStream inputStream = script.getResourceStream();

        if (inputStream == null) {
            ModCore.error("Script file %s does not exist", script);
            return;
        }

        String luaScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        if (luaScript == null) {
            ModCore.error("Lua script file %s not found", script);
            return;
        } else if (luaScript.isEmpty()) {
            ModCore.error("Lua script %s 's content is empty", script);
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
                // Replaced by meta table _TagField
//                .addFunction("setNBTTag", (k, v) -> {/*this.setNBTTag(k.tojstring(), v)*/})
//                .addFunction("getNBTTag", (k) -> {/*this.getNBTTag(k.tojstring())*/})
                .addFunctionWithReturn("getStockPosition", () -> ScriptVectorUtil.constructVec3Table(this.getPosition()))
                .addFunctionWithReturn("getStockMatrix", () -> ScriptVectorUtil.constructMatrix4Table(this.getModelMatrix()))
                .addFunctionWithReturn("newVector", (x, y, z) -> ScriptVectorUtil.constructVec3Table(x, y, z))
                .addFunctionWithReturn("getCoupled", this::getCoupled)
                .addFunctionWithReturn("initTextField", this::initTextField)
                .addFunctionWithReturn("getFont", this::getFont)
                .addFunctionWithReturn("isBuilt", () -> LuaValue.valueOf(this.isBuilt()))
                .addFunction("playSound", this::playSound)
                .addFunctionWithReturn("getObjectPos", this::getObjectPos)
                .addVarArgsFunctionWithReturn("initSound", this::initSound)
                .setInGlobals(globals);

        LuaLibrary.create("World")
                .addFunctionWithReturn("isRainingAt", pos -> LuaValue.valueOf(this.getWorld().isRaining(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getTemperatureAt", pos -> LuaValue.valueOf(this.getWorld().getTemperature(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getSnowLevelAt", pos -> LuaValue.valueOf(this.getWorld().getSnowLevel(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getBlockLightLevelAt", pos -> LuaValue.valueOf(this.getWorld().getBlockLightLevel(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getSkyLightLevelAt", pos -> LuaValue.valueOf(this.getWorld().getSkyLightLevel(ScriptVectorUtil.convertToVec3i(pos))))
                .addFunctionWithReturn("getDimension", () -> LuaValue.valueOf(this.getWorld().getId()))
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

        LuaLibrary.create("Events")
                .addFunction("registerEvent", (name, func) -> {
                    if (func.isfunction()) {
                        luaEventCallbacks.computeIfAbsent(name.tojstring(), k -> new ArrayList<>()).add(func);
                    } else {
                        ModCore.warn("registerEvent called with non-function for event: " + name.tojstring());
                    }
                }).addVarArgsFunction("triggerEvent", args -> {
                    String funcName = args.arg1().tojstring();
                    if (funcName.equals("onTick") || funcName.equals("onControlGroupChange")) {
                        ModCore.error("[Lua] Cannot call \"onTick\" or \"onControlGroupChange\" manually. Please use another name for your event!");
                        return;
                    }
                    Varargs varargs = args.subargs(2);
                    this.mapTrain(this, false, stock -> ((EntityScriptableRollingStock) stock).triggerEvent(funcName, varargs));
                })
                .setInGlobals(globals);

        ScriptVectorUtil.VecUtil.setInGlobals(globals);
        MarkupLibrary.FUNCTION.setInGlobals(globals);

        if (getDefinition().addScripts != null) {
            for (String modules : getDefinition().addScripts) {
                Identifier newModule = script.getRelative(modules);
                moduleMap.put(modules.replace(".lua", ""), newModule.getResourceStream());
            }
            preloadModules(globals, moduleMap);
        }

        LuaValue chunk = globals.load(luaScript);
        chunk.call();

        for (Map.Entry<String, LuaValue> entry : tempMap.entrySet()) {
            tagFields.putIfAbsent(entry.getKey(), entry.getValue());
        }

        globals.set("_TagField", createTagField(tagFields));
        LuaTable tagField = (LuaTable) globals.get("_TagField");
        for (Map.Entry<String, LuaValue> entry : tagFields.entrySet()) {
            tagField.set(entry.getKey(), entry.getValue());
        }

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

    private LuaTable createTagField(Map<String, LuaValue> tagFields) {
        LuaTable table = new LuaTable();
        LuaTable meta = LuaLibrary.create()
                .addFunctionWithReturn("__index", (self, key) -> tagFields.getOrDefault(key.tojstring(), LuaValue.NIL))
                .addFunctionWithReturn("__newindex", (self, key, value) -> {
                    if (value.isboolean() || value.isnumber() || value.isstring()) {
                        tagFields.put(key.tojstring(), value);
                    }
                    return LuaValue.NIL;
                })
                .getAsTable();

        table.setmetatable(meta);
        return table;
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

    protected void triggerEvent(String eventName, LuaValue... args) {
        List<LuaValue> callBacks = luaEventCallbacks.get(eventName);
        if (callBacks != null) {
            for (LuaValue callback : callBacks) {
                try {
                    callback.invoke(LuaValue.varargsOf(args));
                } catch (Exception e) {
                    ModCore.error("Lua callback for event %s failed: %s", eventName, e.getMessage());
                }
            }
        }
    }

    protected void triggerEvent(String eventName, Varargs args) {
        List<LuaValue> callBacks = luaEventCallbacks.get(eventName);
        if (callBacks != null) {
            for (LuaValue callback : callBacks) {
                try {
                    callback.invoke(args);
                } catch (Exception e) {
                    ModCore.error("Lua callback for event %s failed: %s", eventName, e.getMessage());
                }
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

    public LuaValue getObjectPos(LuaValue name) {
        Optional<Map.Entry<String, OBJGroup>> entryMap = getDefinition().getModel().groups.entrySet().stream().filter(e -> e.getKey().contains(name.tojstring())).findFirst();

        if (!entryMap.isPresent()) {
            return LuaValue.NIL;
        }

        OBJGroup group = entryMap.get().getValue();

        Vec3d center = group.min.add(group.max.subtract(group.min).scale(0.5));
        return ScriptVectorUtil.constructVec3Table(center);
    }

    public void playSound(LuaValue identifier, LuaValue luaPos, LuaValue volume) {
        Vec3d pos;
        if (luaPos != LuaValue.NIL) {
            Vec3d objPos = ScriptVectorUtil.convertToVec3d(luaPos);
            pos = getPosition().add(objPos);
        } else {
            pos = getPosition();
        }

        if (volume == LuaValue.NIL) {
            volume = LuaValue.valueOf(1);
        }

        Identifier sound = new Identifier(ImmersiveRailroading.MODID, identifier.tojstring());

        if (!sound.canLoad()) {
            ModCore.error("[Lua] Sound file %s does not exist! Not playing sound.", sound.toString());
            return;
        }

        new SoundPacket(sound, pos, getVelocity(), volume.tofloat(), 1, 10, ConfigSound.SoundCategories.controls(), SoundPacket.PacketSoundCategory.SCRIPTED).sendToObserving(this);
    }

    public LuaValue initSound(Varargs args) {
        String soundLocation = args.arg1().tojstring();
        boolean repeats = args.narg() > 1 && args.arg(2).toboolean();
        int distance = args.narg() > 2 ? args.arg(3).toint() : 10;


        SoundConfig config = sounds.computeIfAbsent(soundLocation, k -> new SoundConfig(this, soundLocation, repeats, distance));

        LuaLibrary lib = LuaLibrary.create();

        lib.addFunctionWithReturn("play", (s, p) -> {
            config.play(ScriptVectorUtil.convertToVec3d(p));
            return s;
        }).addFunctionWithReturn("play", s -> {
            config.play();
            return s;
        }).addFunctionWithReturn("setPosition", (s, p) -> {
            config.setPos(ScriptVectorUtil.convertToVec3d(p));
            return s;
        }).addFunctionWithReturn("setPitch", (s, f) -> {
            config.setPitch(f.tofloat());
            return s;
        }).addFunctionWithReturn("setVolume", (s, f) -> {
            config.setVolume(f.tofloat());
            return s;
        }).addFunctionWithReturn("setVelocity", (s, v) -> {
            if (v == LuaValue.NIL) v = ScriptVectorUtil.constructVec3Table(getVelocity());
            config.setMotion(ScriptVectorUtil.convertToVec3d(v));
            return s;
        }).addFunctionWithReturn("stop", s -> {
            config.stop();
            return s;
        });

        return lib.getAsTable();
    }

    public void clientSound() {}

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
        }).addFunctionWithReturn("setSelectable", b -> {
            textField.setSelectable(b.toboolean());
            return funcHolder[0];
        }).addFunctionWithReturn("setVerticalAlign", a -> {
            textField.setVerticalAlign(a.tojstring());
            return funcHolder[0];
        }).addFunctionWithReturn("setScale", s -> {
            textField.setScale(s.tofloat());
            return funcHolder[0];
        }).addFunctionWithReturn("update", () -> {
            new TextField.PacketSyncTextField(this, this.textFields).sendToObserving(this);
            return funcHolder[0];
        });

        funcHolder[0] = lib.getAsTable();

        return funcHolder[0];
    }

    public LuaValue getFont(LuaValue ident) {
        Identifier identifier = new Identifier(ident.tojstring());
        Font font = FontLoader.getOrCreateFont(identifier);

        return LuaLibrary.create()
                .addFunctionWithReturn("getCharWidth", c -> {
                    String javaString = c.tojstring();
                    char javaChar = javaString.charAt(0);
                    return LuaValue.valueOf(font.getCharWidthPx(javaChar));
                })
                .addFunctionWithReturn("getCharHeight", c -> {
                    String javaString = c.tojstring();
                    char javaChar = javaString.charAt(0);
                    return LuaValue.valueOf(font.getCharHeightPx(javaChar));
                })
                .addFunctionWithReturn("getIdentifier", () -> ident)
                .getAsTable();
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

                objTable.set("distanceTraveled", obj.distanceTraveledReal != 0 ? LuaValue.valueOf(obj.distanceTraveledReal) : LuaValue.valueOf(0));
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

    public void setTextField(String name, TextField field) {
        if (getDefinition().getMesh().getGroup(name) != null) {
            this.textFields.put(name, field);
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

    public void wakeLuaScript() {
        wakeLuaScriptCalled = true;
    }

    public void setTurnedOnLua(LuaValue b) {
    }

    private static TagCompound serializeLuaValue(TagCompound compound, String key, LuaValue value) {
        switch (value.typename()) {
            case "boolean":
                compound.setBoolean(key, value.toboolean());
                break;
            case "number":
                if (value.isint()) {
                    compound.setInteger(key, value.toint());
                } else if (value.isnumber()) {
                    compound.setFloat(key, value.tofloat());
                }
                break;
            case "string":
                compound.setString(key, value.tojstring());
                break;
            case "table":
                List<LuaValue> list = Arrays.stream(value.checktable().keys()).collect(Collectors.toList());
                compound.setList(key, list, l -> serializeLuaValue(compound, "index", l));
                break;
            case "nil":
            default:
                /* not supported */
        }

        return compound;
    }

    private static LuaValue deserializeLuaValue(TagCompound compound, String key) {
        if (compound.hasKey(key)) {
            if (compound.getBoolean(key) != null) {
                return LuaValue.valueOf(compound.getBoolean(key));
            } else if (compound.getInteger(key) != null) {
                return LuaValue.valueOf(compound.getInteger(key));
            } else if (compound.getFloat(key) != null) {
                return LuaValue.valueOf(compound.getFloat(key));
            } else if (compound.getString(key) != null) {
                return LuaValue.valueOf(compound.getString(key));
            } else if (compound.getList(key, v -> deserializeLuaValue(v, "index")) != null) {
                List<LuaValue> values = compound.getList(key, v -> deserializeLuaValue(v, "index"));
                return LuaTable.tableOf(values.toArray(new LuaValue[0]));
            }
        }
        return LuaValue.NIL;
    }

    private static class LuaMapper implements TagMapper<Map<String, LuaValue>> {

        @Override
        public TagAccessor<Map<String, LuaValue>> apply(Class<Map<String, LuaValue>> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.setMap(fieldName, o, Function.identity(), v -> serializeLuaValue(new TagCompound(), "value", v)),
                    d -> d.getMap(fieldName, Function.identity(), v -> deserializeLuaValue(v, "value"))
            );
        }
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
