package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.ObjectValue;
import cam72cam.immersiverailroading.model.part.PartSound;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import cam72cam.immersiverailroading.util.DataBlock;

import java.util.HashMap;
import java.util.Map;

public class SetSound {
    private PartSound idle;
    private PartSound newIdle;
    private static Map<String, SetSound> instances = new HashMap<>();
    private PartSound newRunning;
    private PartSound running;
    private String defID;

    private static Map<String, SetSound> defIDInstance = new HashMap<>();
    private static Map<String, SetSound> uuidInstance = new HashMap<>();
    private EntityRollingStockDefinition.SoundDefinition idleInstance;
    private EntityRollingStockDefinition.SoundDefinition oldValue;

    private String soundsIdle = "idle";
    private String soundsRunning = "running";
    private EntityRollingStockDefinition.SoundDefinition runningInstance;
    private EntityRollingStockDefinition.SoundDefinition oldRunning;

    private SetSound(String defID) {
        this.defID = defID;
        this.idle = null;
        this.newIdle = null;
    }

    public static SetSound getInstance(String name) {
        SetSound instance = defIDInstance.get(name);
        if (instance == null) {
            instance = new SetSound(name);
            defIDInstance.put(name, instance);
        }
        return instance;
    }

    public static SetSound getInstance(String uuid, String defID) {
        SetSound instance = uuidInstance.get(uuid);
        if (instance == null) {
            instance = new SetSound(defID);
            uuidInstance.put(uuid, instance);
            if (defIDInstance.get(defID) != null && defIDInstance.get(defID).getDefID().equals(defID)) {
                uuidInstance.get(uuid).idle = defIDInstance.get(defID).idle;
                uuidInstance.get(uuid).running = defIDInstance.get(defID).running;
            }
        }
        return instance;
    }

    public void defaultIdle(LocomotiveDieselDefinition def) {
        idle = def.isCabCar() ? null : new PartSound(def.idle, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
    }

    public void newIdle(LocomotiveDieselDefinition def, EntityRollingStockDefinition.SoundDefinition sound){
        newIdle = def.isCabCar() ? null : new PartSound(sound, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
    }

    public PartSound getIdle(){
        if (newIdle != null) {
            return newIdle;
        }else {
            return idle;
        }
    }

    public void newRunning(LocomotiveDieselDefinition def, EntityRollingStockDefinition.SoundDefinition sound) {
        newRunning = def.isCabCar() ? null : new PartSound(sound, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running);
    }

    public void defaultRunning(LocomotiveDieselDefinition def) {
        running = def.isCabCar() || def.running == null ? null : new PartSound(def.running, true, 80, ConfigSound.SoundCategories.Locomotive.Diesel::running);
    }

    public PartSound getRunning() {
        if (newRunning != null) {
            return newRunning;
        } else {
            return running;
        }
    }

    public String getDefID() {
        return defID;
    }

    public void newSound(Map<String, DataBlock.Value> soundDef, ObjectValue objectValue, SetSound setSound, ObjectValue objectValueRunning, LocomotiveDieselDefinition def) {
        this.idleInstance = EntityRollingStockDefinition.SoundDefinition.getOrDefault(objectValue, soundDef);
        if (soundDef.containsKey(soundsIdle)) {
            assert this.idleInstance != null;
            if (!this.idleInstance.equals(oldValue)) {
                newIdle(def, this.idleInstance);
                oldValue = this.idleInstance;
            }
        }
        if (soundDef.containsKey(soundsRunning)) {
            this.runningInstance = EntityRollingStockDefinition.SoundDefinition.getOrDefault(objectValueRunning, soundDef);
            assert this.runningInstance != null;
            if (!this.runningInstance.equals(oldRunning)) {
                newRunning(def, this.runningInstance);
                oldRunning = this.runningInstance;
            }
        }
    }
}
