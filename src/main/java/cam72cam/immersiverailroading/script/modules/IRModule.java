package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.gui.overlay.Stat;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.immersiverailroading.script.library.LuaLibrary;
import cam72cam.immersiverailroading.script.sound.SoundConfig;
import cam72cam.immersiverailroading.font.Font;
import cam72cam.immersiverailroading.font.FontLoader;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.immersiverailroading.textfield.library.RGBA;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.resource.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.Map.Entry;

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
    public LuaValue getStat(LuaValue stat) {
        Stat stats = Stat.valueOf(stat.tojstring().toUpperCase());
        String value = stats.getValue(stock);
        return LuaValue.valueOf(value);
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getStat(LuaValue stat, LuaValue digit) {
        Stat stats = Stat.valueOf(stat.tojstring().toUpperCase());
        String value = stats.getValue(stock, digit.toint());
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
            float brake = ((Locomotive) stock).getTrainBrakePos();
            return LuaValue.valueOf(brake);
        }

        return (LuaValue.valueOf(0));
    }

    @LuaFunction(module = "IR")
    public void setIndependentBrake(LuaValue value) {
        stock.setIndependentBrake(value.tofloat());
    }

    @LuaFunction(module = "IR")
    public LuaValue getIndependentBrake() {
        return LuaValue.valueOf(stock.getIndependentBrake());
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
            return LuaValue.valueOf(((LocomotiveDiesel) stock).getDynamicBrake());
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
            return LuaValue.valueOf(((Locomotive) stock).isSanding);
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
    	Collection<EntityCoupleableRollingStock.DirectionalStock> train = stock.getDirectionalTrain(false);

        LuaTable consist = new LuaTable();

        int i = 1;
        for (EntityCoupleableRollingStock.DirectionalStock rollingStock : train) {
            LuaTable stockTable = new LuaTable();

            stockTable.set("UUID", LuaValue.valueOf(rollingStock.stock().getUUID().toString()));
            stockTable.set("stock", ((EntityScriptableRollingStock) rollingStock.stock()).getGlobals());
            stockTable.set("ID", LuaValue.valueOf(rollingStock.stock().getDefinitionID()));
            stockTable.set("coupledFront", rollingStock.stock().coupledFront != null ? LuaValue.valueOf(rollingStock.stock().coupledFront.toString()) : LuaValue.NIL);
            stockTable.set("coupledBack", rollingStock.stock().coupledBack != null ? LuaValue.valueOf(rollingStock.stock().coupledBack.toString()) : LuaValue.NIL);
            stockTable.set("flipped", LuaValue.valueOf(!rollingStock.direction()));

            consist.set(i++, stockTable);
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

        long objectCount = stock.getDefinition().getModel().model.groups().stream().filter(s -> s.contains(groupName)).count();

        if (objectCount == 0L) {
            ModCore.error("[Lua] Found no TextField named %s in: %s", groupName, stock.getDefinitionID());
            return LuaValue.tableOf();
        }

        if (objectCount > 1L) {
            ModCore.info("[Lua] Found more than one TextField defined as %s, using first!", groupName);
        }

        TextFieldConfig textField = stock.textFields.computeIfAbsent(groupName, t -> new TextFieldConfig(groupName, resX.toint(), resY.toint(), f -> f.setSelectable(false).setStock(stock)));
        LuaLibrary lib = LuaLibrary.create();

        LuaTable[] funcHolder = new LuaTable[1];

        lib.addFunctionWithReturn("setFont", f -> {
            textField.setFont(new Identifier(f.tojstring()));
            return funcHolder[0];
        }).addFunctionWithReturn("setColor", c -> {
            textField.setColor(RGBA.fromHex(c.tojstring()));
            return funcHolder[0];
        }).addFunctionWithReturn("setFullbright", f -> {
            textField.setFullbright(f.toboolean());
            return funcHolder[0];
        }).addFunctionWithReturn("setAlign", a -> {
            textField.setAlign(TextFieldConfig.Align.valueOf(a.tojstring().toUpperCase()));
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
            textField.setVerticalAlign(TextFieldConfig.VerticalAlign.valueOf(a.tojstring().toUpperCase()));
            return funcHolder[0];
        }).addFunctionWithReturn("setScale", s -> {
            textField.setScale(s.tofloat());
            return funcHolder[0];
        }).addFunctionWithReturn("update", () -> {
//            new TextFieldClientPacket(stock, textField).sendToObserving(stock);
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
        Optional<Entry<String, ModelGroup>> entryMap = stock.getDefinition().getModel().model.getGroups().entrySet().stream().filter(e -> e.getKey().contains(name.tojstring())).findFirst();

        if (!entryMap.isPresent()) {
            return LuaValue.NIL;
        }

        ModelGroup group = entryMap.get().getValue();

        Vec3d center = group.min().add(group.max().subtract(group.min()).scale(0.5));
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
            if (directionalStock.stock().getUUID().equals(stock.getUUID())) {
                flipped = !directionalStock.direction();
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
    
    @LuaFunction(module = "IR")
    public void setBoilerTemp(LuaValue value) {
        if (stock instanceof LocomotiveSteam) {
            ((LocomotiveSteam) stock).setBoilerTemperature(value.tofloat());
        }
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getBoilerTemp() {
        if (stock instanceof LocomotiveSteam) {
            LuaValue.valueOf(((LocomotiveSteam) stock).getBoilerTemperature());
        }
        return LuaValue.valueOf(0);
    }
    
    @LuaFunction(module = "IR")
    public void setLiquidCargoAmount(LuaValue amount, LuaValue liquid) {
        if (stock instanceof FreightTank) {
            ((FreightTank) stock).setLiquidAmount(amount.toint(), liquid.toString());
        }
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getLiquidCargoAmount() {
        if (stock instanceof FreightTank) {
            return LuaValue.valueOf(((FreightTank) stock).getLiquidAmount());
        }
        return LuaValue.valueOf(0);
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getMaxLiquidCargoAmount() {
        if (stock instanceof FreightTank) {
            return LuaValue.valueOf(((FreightTank) stock).getTankCapacity().MilliBuckets());
        }
        return LuaValue.valueOf(0);
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getLiquidCargoType() {
        if (stock instanceof FreightTank) {
            String type = ((FreightTank) stock).getLiquidType();
            return LuaValue.valueOf(type != null ? type : "");
        }
        return LuaValue.valueOf("");
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
    public void setBoilerPressure(LuaValue value) {
        if (stock instanceof LocomotiveSteam)
            ((LocomotiveSteam) stock).setBoilerPressureBar(value.tofloat());
    }
    
    @LuaFunction(module = "IR")
    public void setMaxBoilerPressure(LuaValue value) {
        if (stock instanceof LocomotiveSteam)
            ((LocomotiveSteam) stock).setMaxBoilerPressure(value.tofloat());
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getChestPressure() {
        if (stock instanceof LocomotiveSteam) {
            float pressure = ((LocomotiveSteam) stock).getChestPressurePercent();
            return LuaValue.valueOf(pressure);
        }
        return LuaValue.valueOf(0);
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getBrakingWeight() {
        return LuaValue.valueOf(stock.getBrakingWeight());
    }
    
    @LuaFunction(module = "IR")
    public void setBrakingWeight(LuaValue value) {
        stock.setBrakingWeight(value.todouble());
    }

    @LuaFunction(module = "IR")
    public LuaValue getTractiveEffort() {
        if (stock instanceof Locomotive) {
            return LuaValue.valueOf(((Locomotive) stock).getCurrentTractiveEffort());
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR")
    public void setTractiveEffort(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setCurrentTractiveEffort(value.tofloat());
        }
    }
    
    @LuaFunction(module = "IR")
    public LuaValue isSliding() {
        return LuaValue.valueOf(stock.isSliding());
    }

    @LuaFunction(module = "IR")
    public LuaValue getBrakePressure() {
        return LuaValue.valueOf(stock.getBrakePressure());
    }

    @LuaFunction(module = "IR")
    public LuaValue getWeight() {
        return LuaValue.valueOf(stock.getWeight());
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getDistanceTraveled() {
        return LuaValue.valueOf(stock.distanceTraveledReal);
    }
    
    @LuaFunction(module = "IR")
    public void setBrakeCylinderPressure(LuaValue value) {
        stock.setBrakeCylinderPressure(value.tofloat());
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getBrakeCylinderPressure() {
        return LuaValue.valueOf(stock.getBrakeCylinderPressure());
    }
    
    @LuaFunction(module = "IR")
    public LuaValue getCargoPercent() {
        if (stock instanceof Freight) {
            return LuaValue.valueOf(((Freight) stock).getPercentCargoFull());
        }
        return LuaValue.valueOf(0);
    }
    
    @LuaFunction(module = "IR")
    public void setGuiText(LuaValue identifier, LuaValue value) {
        stock.setGuiText(identifier.tostring(), value.tostring());
    }
}
