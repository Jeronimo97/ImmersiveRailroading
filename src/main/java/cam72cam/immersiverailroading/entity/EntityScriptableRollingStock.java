package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.immersiverailroading.script.*;
import cam72cam.immersiverailroading.script.library.LuaLibrary;
import cam72cam.immersiverailroading.script.library.LuaSerialization;
import cam72cam.immersiverailroading.script.library.ScheduleEvent;
import cam72cam.immersiverailroading.script.modules.ScriptVectorUtil;
import cam72cam.immersiverailroading.script.sound.SoundConfig;
import cam72cam.immersiverailroading.textUtil.Font;
import cam72cam.immersiverailroading.textUtil.FontLoader;
import cam72cam.immersiverailroading.textUtil.TextField;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.*;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock {
    private LuaContext context;

    @TagField(value = "textFields", mapper = TextField.TextFieldMapMapper.class)
    public Map<String, TextField> textFields = new HashMap<>();
    public Map<String, SoundConfig> sounds = new HashMap<>();
    protected final Map<String, List<LuaValue>> luaEventCallbacks = new HashMap<>();
    @TagSync
    @TagField(value = "luaTagField", mapper = LuaSerialization.LuaMapper.class)
    private final Map<String, LuaValue> tagFields = new HashMap<>();
    protected final Set<ScheduleEvent> schedule = new HashSet<>();

    /**
     * <h2>Overrides</h2>
     */

    @Override
    public void onTick() {
        super.onTick();

        if (getWorld().isClient) {
            return;
        }

        if (Config.ConfigPerformance.disableLuaScript) {
            return;
        }

        if (getDefinition().script == null) {
            return;
        }

        if (context == null) {
            context = LuaContext.create(this);

            Identifier script = getDefinition().script;

            List<String> modules = getDefinition().addScripts;
            context.loadModules(modules, script);

            if (script.canLoad()) {
                context.loadScript(script);
            }

            context.refreshSerialization(tagFields);
        }

        schedule.removeIf(t -> {
            t.ticks--;
            if (t.ticks <= 0) {
                t.runnable.run();
                return true;
            }
            return false;
        });

        triggerEvent("onTick");
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

    /**
     * <h2>ImmersiveRailroading Functions (IR)</h2>
     */

    @LuaFunction(module = "IR")
    public void setCG(LuaValue ctrl, LuaValue value) {
        String control = ctrl.tojstring();
        float val = value.tofloat();

        setControlPosition(control, val);
    }

    @LuaFunction(module = "IR")
    public LuaValue getCG(LuaValue control) {
        float pos = getControlPosition(control.tojstring());
        return LuaValue.valueOf(pos);
    }

    @LuaFunction(module = "IR")
    public LuaValue getPaint() {
        return LuaValue.valueOf(getTexture());
    }

    @LuaFunction(module = "IR")
    public void setPaint(LuaValue textureName) {
        setTexture(textureName.tojstring());
    }

    @LuaFunction(module = "IR")
    public LuaValue getReadout(LuaValue readout) {
        Readouts readouts = Readouts.valueOf(readout.tojstring().toUpperCase());
        float value = readouts.getValue(this);
        return LuaValue.valueOf(value);
    }

    @LuaFunction(module = "IR")
    protected void setPerformance(LuaValue performanceType, LuaValue val) {
        /* */
    }

    @LuaFunction(module = "IR")
    protected LuaValue getPerformance(LuaValue type) {
        /* */
        return LuaValue.NIL;
    }

    @LuaFunction(module = "IR")
    public void couplerEngaged(LuaValue position, LuaValue engaged) {
        CouplerType type = CouplerType.valueOf(position.tojstring().toUpperCase());
        setCouplerEngaged(type, engaged.toboolean());
    }

    @LuaFunction(module = "IR", name = "setThrottle")
    public void setThrottleLua(LuaValue value) {
        /* */
    }

    @LuaFunction(module = "IR", name = "getThrottle")
    public LuaValue getThrottleLua() {
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR", name = "setReverser")
    public void setReverserLua(LuaValue value) {
        /* */
    }

    @LuaFunction(module = "IR", name = "getReverser")
    public LuaValue getReverserLua() {
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR", name = "setTrainBrake")
    public void setTrainBrakeLua(LuaValue value) {
        /* */
    }

    @LuaFunction(module = "IR", name = "getTrainBrake")
    protected LuaValue getTrainBrakeLua() {
        if (this instanceof Locomotive) {
            float brake = ((Locomotive) this).getTrainBrake();
            return LuaValue.valueOf(brake);
        }

        return (LuaValue.valueOf(0));
    }

    @LuaFunction(module = "IR", name = "setIndependentBrake")
    public void setIndependentBrakeLua(LuaValue value) {
        setIndependentBrake(value.tofloat());
    }

    @LuaFunction(module = "IR", name = "getIndependentBrake")
    public LuaValue getIndependentBrakeLua() {
        return LuaValue.valueOf(getIndependentBrake());
    }

    @LuaFunction(module = "IR")
    public void setGlobal(LuaValue control, LuaValue value) {
        this.mapTrain(this, false, stock -> stock.setControlPosition(control.tojstring(), value.tofloat()));
    }

    @LuaFunction(module = "IR")
    public void setTag(LuaValue tag) {
        setEntityTag(tag.tojstring());
    }

    @LuaFunction(module = "IR")
    public LuaValue getTag(LuaValue tag) {
        return LuaValue.valueOf(this.tag);
    }

    @LuaFunction(module = "IR", name = "getTrain")
    public LuaTable getTrainConsist() {
        List<EntityCoupleableRollingStock> train = getTrain();

        LuaTable consist = new LuaTable();

        for (EntityCoupleableRollingStock stock : train) {
            LuaTable stockTable = new LuaTable();

            stockTable.set("UUID", LuaValue.valueOf(stock.getUUID().toString()));
            stockTable.set("coupledFront", LuaValue.valueOf(stock.coupledFront.toString()));
            stockTable.set("coupledBack", LuaValue.valueOf(stock.coupledBack.toString()));

            consist.set(stock.defID, stockTable);
        }

        return consist;
    }

    @LuaFunction(module = "IR", name = "isTurnedOn")
    public LuaValue isTurnedOnLua() {
        return LuaValue.valueOf(getEngineState());
    }

    @LuaFunction(module = "IR", name = "engineStartStop")
    public void setTurnedOnLua(LuaValue bool) {
        /* */
    }

    @LuaFunction(module = "IR")
    public void newParticle() {
        // Currently Disabled
    }

    @LuaFunction(module = "IR")
    public LuaValue getStockPosition() {
        return ScriptVectorUtil.constructVec3Table(getPosition());
    }

    @LuaFunction(module = "IR")
    public LuaValue getStockMatrix() {
        return ScriptVectorUtil.constructMatrix4Table(getModelMatrix());
    }

    @LuaFunction(module = "IR")
    public LuaValue newVector(LuaValue x, LuaValue y, LuaValue z) {
        return ScriptVectorUtil.constructVec3Table(x, y, z);
    }

    @LuaFunction(module = "IR")
    public LuaTable getCoupled(LuaValue type) {
        LuaTable table = new LuaTable();
        String sType = type.tojstring();

        EntityCoupleableRollingStock stock = getCoupled(CouplerType.valueOf(sType.toUpperCase()));
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

    @LuaFunction(module = "IR")
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

    @LuaFunction(module = "IR")
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

    @LuaFunction(module = "IR", name = "isBuilt")
    public LuaValue isBuiltLua() {
        return LuaValue.valueOf(isBuilt());
    }

    @LuaFunction(module = "IR")
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

    @LuaFunction(module = "IR")
    public LuaValue getObjectPos(LuaValue name) {
        Optional<Map.Entry<String, OBJGroup>> entryMap = getDefinition().getModel().groups.entrySet().stream().filter(e -> e.getKey().contains(name.tojstring())).findFirst();

        if (!entryMap.isPresent()) {
            return LuaValue.NIL;
        }

        OBJGroup group = entryMap.get().getValue();

        Vec3d center = group.min.add(group.max.subtract(group.min).scale(0.5));
        return ScriptVectorUtil.constructVec3Table(center);
    }

    @LuaFunction(module = "IR")
    public LuaValue initSound(LuaValue... args) {
        String soundLocation = args[0].tojstring();
        boolean repeats = args.length > 1 && args[1].toboolean();
        int distance = args.length > 2 ? args[2].toint() : 10;


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

    @LuaFunction(module = "IR", name = "getPassengerCount")
    public LuaValue getPassengerCountLua() {
        return LuaValue.valueOf(getPassengerCount());
    }

    @LuaFunction(module = "IR")
    public LuaValue isStockFlipped() {
        Collection<DirectionalStock> train = getDirectionalTrain(false);

        boolean flipped = false;
        for (DirectionalStock stock : train) {
            if (stock.stock.getUUID().equals(this.getUUID())) {
                flipped = !stock.direction;
                break;
            }
        }

        return LuaValue.valueOf(flipped);
    }

    @LuaFunction(module = "IR")
    public LuaValue getSpeedKmh() {
        Speed speed = getCurrentSpeed();
        return LuaValue.valueOf(speed.metric());
    }

    @LuaFunction(module = "IR")
    public LuaValue getSpeedMph() {
        Speed speed = getCurrentSpeed();
        return LuaValue.valueOf(speed.imperial());
    }

    @LuaFunction(module = "IR")
    public LuaValue getTemperature() {
        if (this instanceof LocomotiveSteam) {
            float temp = ((LocomotiveSteam) this).getBoilerTemperature();
            return LuaValue.valueOf(temp);
        } else if (this instanceof LocomotiveDiesel) {
            float temp = ((LocomotiveDiesel) this).getEngineTemperature();
            return LuaValue.valueOf(temp);
        }

        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR", name = "getBoilerPressure")
    public LuaValue getBoilerPressureLua() {
        if (this instanceof LocomotiveSteam) {
            float pressure = ((LocomotiveSteam) this).getBoilerPressure();
            return LuaValue.valueOf(pressure);
        }

        return LuaValue.valueOf(0);
    }


    /**
     * <h2>World Functions (World)</h2>
     */

    @LuaFunction(module = "World")
    public LuaValue isRainingAt(LuaValue pos) {
        return LuaValue.valueOf(this.getWorld().isRaining(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue isSnowingAt(LuaValue pos) {
        return LuaValue.valueOf(this.getWorld().isSnowing(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getTemperatureAt(LuaValue pos) {
        return LuaValue.valueOf(this.getWorld().getTemperature(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getSnowLevelAt(LuaValue pos) {
        return LuaValue.valueOf(this.getWorld().getSnowLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getBlockLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(this.getWorld().getBlockLightLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getSkyLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(this.getWorld().getSkyLightLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction(module = "World")
    public LuaValue getDimension() {
        return LuaValue.valueOf(this.getWorld().getId());
    }

    @LuaFunction(module = "World")
    public LuaValue getTicks() {
        return LuaValue.valueOf(this.getWorld().getTicks());
    }

    @LuaFunction(module = "World")
    public LuaValue getBlock(LuaValue luaPos) {
        Vec3i pos = ScriptVectorUtil.convertToVec3i(luaPos);
        // It will work, hopefully :)
        ItemStack stack = getWorld().getItemStack(pos);

        String name = stack.getDisplayName();

        return LuaValue.valueOf(name);
    }

    /**
     * <h2>Debug Functions (Debug)</h2>
     */

    @LuaFunction(module = "Debug")
    public void printToInfoLog(LuaValue arg) {
        ModCore.info(arg.tojstring());
    }

    @LuaFunction(module = "Debug")
    public void printToWarnLog(LuaValue arg) {
        ModCore.warn(arg.tojstring());
    }

    @LuaFunction(module = "Debug")
    public void printToErrorLog(LuaValue arg) {
        ModCore.error(arg.tojstring());
    }

    @LuaFunction(module = "Debug")
    public void printToPassengerDialog(LuaValue arg) {
        this.getPassengers().stream().filter(Entity::isPlayer)
                .map(Entity::asPlayer)
                .forEach(player -> player.sendMessage(PlayerMessage.direct(arg.tojstring())));
    }

    /**
     * <h2>Utility Functions (Utils)</h2>
     */

    @LuaFunction(module = "Utils")
    public void wait(LuaValue sec, LuaValue func) {
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

    /**
     * <h2>Event Functions (Events)</h2>
     */

    @LuaFunction(module = "Events")
    public void registerEvent(LuaValue name, LuaValue func) {
        if (func.isfunction()) {
            luaEventCallbacks.computeIfAbsent(name.tojstring(), k -> new ArrayList<>()).add(func);
        } else {
            ModCore.warn("registerEvent called with non-function for event: " + name.tojstring());
        }
    }

    @LuaFunction(module = "Events")
    public void triggerEvent(LuaValue... args) {
        String funcName = args[0].tojstring();
        if (funcName.equals("onTick") || funcName.equals("onControlGroupChange")) {
            ModCore.error("[Lua] Cannot call \"onTick\" or \"onControlGroupChange\" manually. Please use another name for your event!");
            return;
        }
        Varargs varargs = LuaValue.varargsOf(Arrays.copyOfRange(args, 1, args.length));
        this.mapTrain(this, false, stock ->  stock.triggerEvent(funcName, varargs));
    }

    @LuaFunction(module = "Events")
    public void triggerCouplerEvent(LuaValue... args) {
        String functionName = args[0].tojstring();
        CouplerType coupler = CouplerType.valueOf(args[1].tojstring().toUpperCase());

        EntityScriptableRollingStock stock = (EntityScriptableRollingStock) getCoupled(coupler);

        stock.triggerEvent(functionName, LuaValue.varargsOf(Arrays.copyOfRange(args, 2, args.length)));
    }
}
