package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.floor.Mesh;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.immersiverailroading.script.library.LuaLibrary;
import cam72cam.immersiverailroading.script.sound.SoundConfig;
import cam72cam.immersiverailroading.textUtil.Font;
import cam72cam.immersiverailroading.textUtil.FontLoader;
import cam72cam.immersiverailroading.textUtil.TextField;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.resource.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;

public class IRModule implements LuaModule {
    private final EntityScriptableRollingStock stock;

    public IRModule(EntityScriptableRollingStock stock) {
        this.stock = stock;
    }

    @LuaFunction(module = "IR")
    public void setCG(LuaValue ctrl, LuaValue value) {
        String control = ctrl.tojstring();
        float val = value.tofloat();

        stock.setControlPosition(control, val);
    }

    @LuaFunction(module = "IR")
    public LuaValue getCG(LuaValue control) {
        float pos = stock.getControlPosition(control.tojstring());
        return LuaValue.valueOf(pos);
    }

    @LuaFunction(module = "IR")
    public LuaValue getPaint() {
        return LuaValue.valueOf(stock.getTexture());
    }

    @LuaFunction(module = "IR")
    public void setPaint(LuaValue textureName) {
        stock.setTexture(textureName.tojstring());
    }

    @LuaFunction(module = "IR")
    public LuaValue getReadout(LuaValue readout) {
        Readouts readouts = Readouts.valueOf(readout.tojstring().toUpperCase());
        float value = readouts.getValue(stock);
        return LuaValue.valueOf(value);
    }

    @LuaFunction(module = "IR")
    protected void setPerformance(LuaValue performanceType, LuaValue val) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setPerformance(performanceType, val);
        }
    }

    @LuaFunction(module = "IR")
    protected LuaValue getPerformance(LuaValue type) {
        if (stock instanceof Locomotive) {
            return ((Locomotive) stock).getPerformance(type);
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR")
    public void couplerEngaged(LuaValue position, LuaValue engaged) {
        EntityCoupleableRollingStock.CouplerType type = EntityCoupleableRollingStock.CouplerType.valueOf(position.tojstring().toUpperCase());
        stock.setCouplerEngaged(type, engaged.toboolean());
    }

    @LuaFunction(module = "IR")
    public void setThrottle(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setThrottle(value.tofloat());
        }
    }

    @LuaFunction(module = "IR")
    public LuaValue getThrottle() {
        if (stock instanceof Locomotive) {
            return LuaValue.valueOf(((Locomotive) stock).getThrottle());
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR")
    public void setReverser(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setReverser(value.tofloat());
        }
    }

    @LuaFunction(module = "IR")
    public LuaValue getReverser() {
        if (stock instanceof Locomotive) {
            return LuaValue.valueOf(((Locomotive) stock).getReverser());
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR")
    public void setTrainBrake(LuaValue value) {
        if (stock instanceof Locomotive) {
           ((Locomotive) stock).setTrainBrake(value.tofloat());
        }
    }

    @LuaFunction(module = "IR")
    protected LuaValue getTrainBrake() {
        if (stock instanceof Locomotive) {
            float brake = ((Locomotive) stock).getTrainBrake();
            return LuaValue.valueOf(brake);
        }

        return (LuaValue.valueOf(0));
    }

    @LuaFunction(module = "IR")
    public void setIndependentBrake(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setIndependentBrake(value.tofloat());
        }
    }

    @LuaFunction(module = "IR")
    public LuaValue getIndependentBrake() {
        if (stock instanceof Locomotive) {
            float indBrake = ((Locomotive) stock).getIndependentBrake();
            return LuaValue.valueOf(indBrake);
        }
        return (LuaValue.valueOf(0));
    }
    
    @LuaFunction(module = "IR")
    public void setDynamicBrake(LuaValue value) {
        if (stock instanceof LocomotiveDiesel) {
            ((LocomotiveDiesel) stock).setDynamicBrake(value.tofloat());
        }
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getDynamicBrake() {
        if (stock instanceof LocomotiveDiesel) {
            float dynBrake = ((LocomotiveDiesel) stock).getDynamicBrake();
            return LuaValue.valueOf(dynBrake);
        }
        return LuaValue.valueOf(0);
    }
    
    @LuaFunction(module = "IR")
    public void setHandBrake(LuaValue value) {
        stock.setHandBrake(value.tofloat());
    }

    @LuaFunction(module = "IR")
    public LuaValue getHandBrake() {
        return LuaValue.valueOf(stock.getHandBrake());
    }
    
    @LuaFunction(module = "IR")
    public void setSanding(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setSanding(value.toboolean());
        }
    }
    
    @LuaFunction(module = "IR")
    public LuaValue isSanding() {
        if (stock instanceof Locomotive) {
            boolean sanding = ((Locomotive) stock).isSanding();
            return LuaValue.valueOf(sanding);
        }
        return LuaValue.valueOf(false);
    }

    @LuaFunction(module = "IR")
    public void setGlobal(LuaValue control, LuaValue value) {
        stock.mapTrain(stock, false, stock -> stock.setControlPosition(control.tojstring(), value.tofloat()));
    }

    @LuaFunction(module = "IR")
    public void setTag(LuaValue tag) {
        stock.setEntityTag(tag.tojstring());
    }

    @LuaFunction(module = "IR")
    public LuaValue getTag(LuaValue tag) {
        return LuaValue.valueOf(stock.tag);
    }

    @LuaFunction(module = "IR", name = "getTrain")
    public LuaTable getTrainConsist() {
        List<EntityCoupleableRollingStock> train = stock.getTrain();

        LuaTable consist = new LuaTable();

        for (EntityCoupleableRollingStock rollingStock : train) {
            LuaTable stockTable = new LuaTable();

            stockTable.set("UUID", LuaValue.valueOf(stock.getUUID().toString()));
            stockTable.set("coupledFront", LuaValue.valueOf(stock.coupledFront.toString()));
            stockTable.set("coupledBack", LuaValue.valueOf(stock.coupledBack.toString()));

            consist.set(rollingStock.getDefinitionID(), stockTable);
        }

        return consist;
    }

    @LuaFunction(module = "IR")
    public LuaValue isTurnedOn() {
        return LuaValue.valueOf(stock.getEngineState());
    }

    @LuaFunction(module = "IR")
    public void engineStartStop(LuaValue bool) {
       if (stock instanceof LocomotiveDiesel) {
           ((LocomotiveDiesel) stock).setTurnedOn(bool.toboolean());
       }
    }

    @LuaFunction(module = "IR")
    public void newParticle() {
        // Currently Disabled
    }

    @LuaFunction(module = "IR")
    public LuaValue getStockPosition() {
        return ScriptVectorUtil.constructVec3Table(stock.getPosition());
    }

    @LuaFunction(module = "IR")
    public LuaValue getStockMatrix() {
        return ScriptVectorUtil.constructMatrix4Table(stock.getModelMatrix());
    }

    @LuaFunction(module = "IR")
    public LuaValue newVector(LuaValue x, LuaValue y, LuaValue z) {
        return ScriptVectorUtil.constructVec3Table(x, y, z);
    }

    @LuaFunction(module = "IR")
    public LuaTable getCoupled(LuaValue type) {
        LuaTable table = new LuaTable();
        String sType = type.tojstring();

        EntityCoupleableRollingStock rollingStock = stock.getCoupled(EntityCoupleableRollingStock.CouplerType.valueOf(sType.toUpperCase()));
        EntityCoupleableRollingStock.CouplerType coupler = stock.getCouplerFor(rollingStock);
        UUID uuid = rollingStock.getUUID();
        String defID = rollingStock.getDefinitionID();
        String tag = rollingStock.tag;

        table.set("coupler", coupler.toString());
        table.set("uuid", uuid.toString());
        table.set("defID", defID);
        table.set("tag", tag);
        return table;
    }

    @LuaFunction(module = "IR")
    public LuaValue initTextField(LuaValue group, LuaValue resX, LuaValue resY) {
        String groupName = String.format("TEXTFIELD_%s", group.tojstring());
        List<Mesh.Group> groupList = stock.getDefinition().getMesh().getGroupContains(groupName);

        if (groupList.isEmpty()) {
            ModCore.error("[Lua] Found no TextField named %s in: %s", groupName, stock.getDefinitionID());
            return new LuaTable();
        }

        if (groupList.size() > 1) {
            ModCore.info("[Lua] Found more than one TextField defined as %s, using first!", groupName);
        }

        TextField textField = stock.textFields.computeIfAbsent(groupName, t -> TextField.createTextField(groupList.get(0), resX.toint(), resY.toint(), f -> f.setSelectable(false)));
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
            new TextField.PacketSyncTextField(stock, stock.textFields).sendToObserving(stock);
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
        return LuaValue.valueOf(stock.isBuilt());
    }

    @LuaFunction(module = "IR")
    public void playSound(LuaValue identifier, LuaValue luaPos, LuaValue volume) {
        Vec3d pos;
        if (luaPos != LuaValue.NIL) {
            Vec3d objPos = ScriptVectorUtil.convertToVec3d(luaPos);
            pos = stock.getPosition().add(objPos);
        } else {
            pos = stock.getPosition();
        }

        if (volume == LuaValue.NIL) {
            volume = LuaValue.valueOf(1);
        }

        Identifier sound = new Identifier(ImmersiveRailroading.MODID, identifier.tojstring());

        if (!sound.canLoad()) {
            ModCore.error("[Lua] Sound file %s does not exist! Not playing sound.", sound.toString());
            return;
        }

        new SoundPacket(sound, pos, stock.getVelocity(), volume.tofloat(), 1, 10, ConfigSound.SoundCategories.controls(), SoundPacket.PacketSoundCategory.SCRIPTED).sendToObserving(stock);
    }

    @LuaFunction(module = "IR")
    public LuaValue getObjectPos(LuaValue name) {
        Optional<Map.Entry<String, OBJGroup>> entryMap = stock.getDefinition().getModel().groups.entrySet().stream().filter(e -> e.getKey().contains(name.tojstring())).findFirst();

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


        SoundConfig config = stock.sounds.computeIfAbsent(soundLocation, k -> new SoundConfig(stock, soundLocation, repeats, distance));

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
            if (v == LuaValue.NIL) v = ScriptVectorUtil.constructVec3Table(stock.getVelocity());
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
        return LuaValue.valueOf(stock.getPassengerCount());
    }

    @LuaFunction(module = "IR")
    public LuaValue isStockFlipped() {
        Collection<EntityCoupleableRollingStock.DirectionalStock> train = stock.getDirectionalTrain(false);

        boolean flipped = false;
        for (EntityCoupleableRollingStock.DirectionalStock directionalStock : train) {
            if (directionalStock.stock.getUUID().equals(stock.getUUID())) {
                flipped = !directionalStock.direction;
                break;
            }
        }

        return LuaValue.valueOf(flipped);
    }

    @LuaFunction(module = "IR")
    public LuaValue getSpeedKmh() {
        Speed speed = stock.getCurrentSpeed();
        return LuaValue.valueOf(speed.metric());
    }

    @LuaFunction(module = "IR")
    public LuaValue getSpeedMph() {
        Speed speed = stock.getCurrentSpeed();
        return LuaValue.valueOf(speed.imperial());
    }

    @LuaFunction(module = "IR")
    public LuaValue getTemperature() {
        if (stock instanceof LocomotiveSteam) {
            float temp = ((LocomotiveSteam) stock).getBoilerTemperature();
            return LuaValue.valueOf(temp);
        } else if (stock instanceof LocomotiveDiesel) {
            float temp = ((LocomotiveDiesel) stock).getEngineTemperature();
            return LuaValue.valueOf(temp);
        }

        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR", name = "getBoilerPressure")
    public LuaValue getBoilerPressureLua() {
        if (stock instanceof LocomotiveSteam) {
            float pressure = ((LocomotiveSteam) stock).getBoilerPressure();
            return LuaValue.valueOf(pressure);
        }

        return LuaValue.valueOf(0);
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getChestPressure() {
        if (stock instanceof LocomotiveSteam) {
            float pressure = ((LocomotiveSteam) stock).getChestPressurePercent();
            return LuaValue.valueOf(pressure);
        }
        return LuaValue.valueOf(0);
    }


}
