package cam72cam.immersiverailroading.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemPaintBrush;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.entity.CustomEntity;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IClickable;
import cam72cam.mod.entity.custom.IKillable;
import cam72cam.mod.entity.custom.ITickable;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.sound.SoundCategory;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.SingleCache;
import util.Matrix4;

public class EntityRollingStock extends CustomEntity
        implements ITickable, IClickable, IKillable, ControlPositionEventHandler {
    @TagField("defID")
    protected String defID;
    @TagField("gauge")
    public Gauge gauge;
    @TagField("tag")
    @TagSync
    public String tag = "";

    @TagSync
    @TagField(value = "texture", mapper = StrictTagMapper.class)
    private String texture = null;
    private final SingleCache<Vec3d, Matrix4> modelMatrix = new SingleCache<>(v -> new Matrix4()
            .translate(this.getPosition().x, this.getPosition().y, this.getPosition().z)
            .rotate(Math.toRadians(180 - this.getRotationYaw()), 0, 1, 0)
            .rotate(Math.toRadians(this.getRotationPitch()), 1, 0, 0)
            .rotate(Math.toRadians(-90), 0, 1, 0)
            .scale(this.gauge.scale(), this.gauge.scale(), this.gauge.scale()));

    public void setup(final EntityRollingStockDefinition def, final Gauge gauge,
            final String texture) {
        this.defID = def.defID;
        this.gauge = gauge;
        this.texture = texture;
        def.cgDefaults.forEach(this::setControlPosition);
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    @Override
    public float getCollisionReduction() {
        return 1;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean allowsDefaultMovement() {
        return false;
    }

    /*
     * TODO?
     * 
     * @Override public String getName() { return this.getDefinition().name(); }
     */

    @Override
    public String tryJoinWorld() {
        if (DefinitionManager.getDefinition(defID) == null) {
            String error = String.format(
                    "Missing definition %s, do you have all of the required resource packs?",
                    defID);
            ImmersiveRailroading.error(error);
            return error;
        }
        return null;
    }

    public EntityRollingStockDefinition getDefinition() {
        return this.getDefinition(EntityRollingStockDefinition.class);
    }

    public <T extends EntityRollingStockDefinition> T getDefinition(final Class<T> type) {
        EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
        if (def == null)
            // This should not be hit, entity should be removed handled by tryJoinWorld
            throw new RuntimeException(String.format(
                    "Definition %s has been removed!  This stock will not function!", defID));
        return (T) def;
    }

    public String getDefinitionID() {
        return this.defID;
    }

    @Override
    public void onTick() {
        if (getWorld().isServer && this.getTickCount() % 5 == 0) {
            EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
            if (def == null) {
                this.kill();
            }
        }
    }

    /*
     * Player Interactions
     */

    @Override
    public ClickResult onClick(final Player player, final Player.Hand hand) {
        if (player.getHeldItem(hand).is(IRItems.ITEM_PAINT_BRUSH)
                && player.hasPermission(Permissions.PAINT_BRUSH)) {
            ItemPaintBrush.onStockInteract(this, player, hand);
            return ClickResult.ACCEPTED;
        }

        if (player.getHeldItem(hand).is(Fuzzy.NAME_TAG)
                && player.hasPermission(Permissions.STOCK_ASSEMBLY)) {
            if (getWorld().isClient)
                return ClickResult.ACCEPTED;
            tag = player.getHeldItem(hand).getDisplayName();
            player.sendMessage(PlayerMessage.direct(tag));
            return ClickResult.ACCEPTED;
        }
        return ClickResult.PASS;
    }

    public void setTexture(final String variant) {
        if (getDefinition().textureNames.containsKey(variant)) {
            this.texture = variant;
        }
    }

    @Override
    public void onDamage(final DamageType type, final Entity source, final float amount,
            final boolean bypassesArmor) {
        if (getWorld().isClient)
            return;

        if (type == DamageType.EXPLOSION) {
            if (source == null || !source.isMob()) {
                if (amount > 5 && ConfigDamage.trainMobExplosionDamage) {
                    this.kill();
                }
            }
        }

        if (type == DamageType.OTHER && source != null && source.isPlayer()) {
            Player player = source.asPlayer();
            if (player.isCrouching()) {
                this.kill();
            }
        }
    }

    @Override
    public void onRemoved() {

    }

    protected boolean shouldDropItems(final DamageType type, final float amount) {
        return type != DamageType.EXPLOSION || amount < 20;
    }

    public void handleKeyPress(final Player source, final KeyTypes key,
            final boolean disableIndependentThrottle) {

    }

    /**
     * @return Stock Weight in Kg
     */
    public double getWeight() {
        return this.getDefinition().getWeight(gauge);
    }

    public double getMaxWeight() {
        return this.getDefinition().getWeight(gauge);
    }

    /*
     * Helpers
     */
    /*
     * TODO RENDER
     * 
     * @SideOnly(Side.CLIENT)
     * 
     * @Override public boolean isInRangeToRenderDist(double distance) { return
     * true; }
     * 
     * @Override public boolean shouldRenderInPass(int pass) { return false; }
     */

    public float soundScale() {
        if (this.getDefinition().shouldScalePitch()) {
            double scale = gauge.scale() * getDefinition().internal_model_scale;
            return (float) Math.sqrt(Math.sqrt(scale));
        }
        return 1;
    }

    public ISound createSound(final Identifier oggLocation, final boolean repeats,
            final double attenuationDistance, final Supplier<Float> category) {
        ISound snd = Audio.newSound(oggLocation, SoundCategory.MASTER, repeats,
                (float) (attenuationDistance * ConfigSound.soundDistanceScale * gauge.scale()),
                soundScale());
        return new ISound() {
            @Override
            public void play(final Vec3d pos) {
                snd.play(pos);
            }

            @Override
            public void stop() {
                snd.stop();
            }

            @Override
            public void setPosition(final Vec3d pos) {
                snd.setPosition(pos);
            }

            @Override
            public void setPitch(final float f) {
                snd.setPitch(f);
            }

            @Override
            public void setVelocity(final Vec3d vel) {
                snd.setVelocity(vel);
            }

            @Override
            public void setVolume(final float f) {
                snd.setVolume(f * category.get());
            }

            @Override
            public boolean isPlaying() {
                return snd.isPlaying();
            }
        };
    }

    public String getTexture() {
        return texture;
    }

    public Matrix4 getModelMatrix() {
        return this.modelMatrix.get(getPosition()).copy();
    }

    public boolean hasElectricalPower() {
        return false;
    }

    @TagSync
    @TagField(value = "controlPositions", mapper = ControlPositionMapper.class)
    protected Map<String, Pair<Boolean, Float>> controlPositions = new HashMap<>();

    public void onDragStart(final Control<?> control) {
        setControlPressed(control, true);
    }

    public void onDrag(final Control<?> control, final double newValue) {
        setControlPressed(control, true);
        setControlPosition(control, (float) newValue);
    }

    public void onDragRelease(final Control<?> control) {
        setControlPressed(control, false);

        if (control.toggle) {
            setControlPosition(control, Math.abs(getControlPosition(control) - 1));
        }
        if (control.press) {
            setControlPosition(control, 0);
        }
    }

    protected float defaultControlPosition(final Control<?> control) {
        return 0;
    }

    public Pair<Boolean, Float> getControlData(final String control) {
        return controlPositions.getOrDefault(control, Pair.of(false, 0f));
    }

    public Pair<Boolean, Float> getControlData(final Control<?> control) {
        return controlPositions.getOrDefault(control.controlGroup,
                Pair.of(false, defaultControlPosition(control)));
    }

    public boolean getControlPressed(final Control<?> control) {
        return getControlData(control).getLeft();
    }

    public void setControlPressed(final Control<?> control, final boolean pressed) {
        controlPositions.put(control.controlGroup, Pair.of(pressed, getControlPosition(control)));

    }

    public float getControlPosition(final Control<?> control) {
        return getControlData(control).getRight();
    }

    public float getControlPosition(final String control) {
        return getControlData(control).getRight();
    }

    public void setControlPosition(final Control<?> control, float val) {
        val = Math.min(1, Math.max(0, val));
        handleControlPositionEvent(control, val, controlPositions, getControlPressed(control));

    }

    public void setControlPosition(final String control, float val) {
        val = Math.min(1, Math.max(0, val));
        controlPositions.put(control, Pair.of(false, val));
    }

    public void setControlPositions(final ModelComponentType type, final float val) {
        getDefinition().getModel().getControls().stream().filter(x -> x.part.type == type)
                .forEach(c -> setControlPosition(c, val));
    }

    public boolean playerCanDrag(final Player player, final Control<?> control) {
        return control.part.type != ModelComponentType.INDEPENDENT_BRAKE_X
                || player.hasPermission(Permissions.BRAKE_CONTROL);
    }

    public void setEntityTag(final String tag) {
        this.tag = tag;
    }

    private static class ControlPositionMapper
            implements TagMapper<Map<String, Pair<Boolean, Float>>> {
        @Override
        public TagAccessor<Map<String, Pair<Boolean, Float>>> apply(
                final Class<Map<String, Pair<Boolean, Float>>> type, final String fieldName,
                final TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.setMap(fieldName, o, Function.identity(),
                            x -> new TagCompound().setBoolean("pressed", x.getLeft())
                                    .setFloat("pos", x.getRight())),
                    d -> d.getMap(fieldName, Function.identity(),
                            x -> Pair.of(x.hasKey("pressed") && x.getBoolean("pressed"),
                                    x.getFloat("pos"))));
        }
    }

    public int getAxleCount() {
        int i = 0;
        if (getDefinition().getComponents(ModelComponentType.BOGEY_FRONT_WHEEL_X) != null) {
            i += getDefinition().getComponents(ModelComponentType.BOGEY_FRONT_WHEEL_X).size();
        }
        if (getDefinition().getComponents(ModelComponentType.BOGEY_POS_WHEEL_X) != null) {
            i += getDefinition().getComponents(ModelComponentType.BOGEY_POS_WHEEL_X).size();
        }
        if (getDefinition().getComponents(ModelComponentType.BOGEY_REAR_WHEEL_X) != null) {
            i += getDefinition().getComponents(ModelComponentType.BOGEY_REAR_WHEEL_X).size();
        }
        if (getDefinition().getComponents(ModelComponentType.FRAME_WHEEL_X) != null) {
            i += getDefinition().getComponents(ModelComponentType.FRAME_WHEEL_X).size();
        }
        if (getDefinition().getComponents(ModelComponentType.WHEEL_DRIVER_POS_X) != null) {
            i += getDefinition().getComponents(ModelComponentType.WHEEL_DRIVER_POS_X).size();
        }
        if (getDefinition().getComponents(ModelComponentType.WHEEL_DRIVER_X) != null) {
            i += getDefinition().getComponents(ModelComponentType.WHEEL_DRIVER_X).size();
        }
        return i;
    }
}