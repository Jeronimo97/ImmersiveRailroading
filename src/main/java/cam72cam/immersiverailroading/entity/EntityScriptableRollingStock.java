package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemTypewriter;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.script.*;
import cam72cam.immersiverailroading.script.library.ILuaEvent;
import cam72cam.immersiverailroading.script.library.LuaSerialization;
import cam72cam.immersiverailroading.script.library.ScheduleEvent;
import cam72cam.immersiverailroading.script.modules.*;
import cam72cam.immersiverailroading.script.sound.SoundConfig;
import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.immersiverailroading.textfield.library.TextFieldMapMapper;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock implements ILuaEvent {
    private LuaContext context;
    /**
     * Used by {@link IRModule}
     */
    // TagSync is actually a terrible idea in this case. Due to it creating an instance of TextFieldConfig every tick, the memory usage will go up by around 1GB before the GC erases it.
    // @TagSync
    @TagField(mapper = TextFieldMapMapper.class)
    public Map<String, TextFieldConfig> textFields = new HashMap<>();
    public Map<String, SoundConfig> sounds = new HashMap<>();
    /**
     * Used by {@link EventModule}
     */
    public final Map<String, List<LuaValue>> luaEventCallbacks = new HashMap<>();
    /**
     * Used by {@link LuaContext}
     */
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

            registerModules();
            loadLuaScript();

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

    @Override
    public Map<String, List<LuaValue>> getLuaEventCallbacks() {
        return luaEventCallbacks;
    }

    private void registerModules() {
        context.registerLibrary(new ScriptVectorUtil.VectorLibrary());
        context.registerLibrary(new MarkupModule());

        context.registerLibrary(new IRModule(this));
        context.registerLibrary(new WorldModule(getWorld()));
        context.registerLibrary(new DebugModule(this));
        context.registerLibrary(new EventModule(this));
    }

    private void loadLuaScript() {
        Identifier script = getDefinition().script;

        List<String> modules = getDefinition().addScripts;
        context.loadModules(modules, script);

        if (script.canLoad()) {
            context.loadScript(script);
        }
    }

    public Globals getGlobals() {
        return context.getGlobals();
    }

    public Map<String, TextFieldConfig> getTextFieldConfig() {
        return this.textFields;
    }

    public void initTextField(TextFieldConfig config) {
        if (config.isGlobal()) {
            mapTrain(this, false, stock -> {
                EntityScriptableRollingStock next = (EntityScriptableRollingStock) stock;
                if (next.getDefinition().getModel().groups().stream().anyMatch(g -> g.contains(config.getObject()))) {
                    next.textFields.put(config.getObject(), config);
                }
            });
        }

        if (config.getLinked() != null && !config.getLinked().isEmpty()) {
            config.getLinked().forEach(l -> {
                TextFieldConfig linked = textFields.get(String.format("TEXTFIELD_%s", l));
                if (linked == null) {
                    return;
                }

                linked.copyConfig(config);
                initTextField(linked);
            });
        }

        textFields.put(config.getObject(), config);
    }

    @LuaFunction(module = "")
    private LuaValue getName() {
        return LuaValue.valueOf(getDefinition().name());
    }

    @LuaFunction(module = "")
    public void print(LuaValue... str) {
        List<String> args = Arrays.stream(str).map(LuaValue::tojstring).collect(Collectors.toList());
        String formatedArgs = String.join("    ", args);
        ModCore.info("[Lua, %s] %s", getDefinition().name(), formatedArgs);
    }


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
}
