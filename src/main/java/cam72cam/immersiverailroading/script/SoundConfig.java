package cam72cam.immersiverailroading.script;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.*;

public class SoundConfig {
    @TagField
    public String location = "";
    @TagField
    public Vec3d pos = Vec3d.ZERO;
    @TagField
    public Vec3d motion = Vec3d.ZERO;
    @TagField
    public float volume = 1;
    @TagField
    public float pitch = 1;
    @TagField
    public boolean repeats = false;
    @TagField
    public int distance;
    public EntityRollingStock stock;
    @TagField
    public boolean isPlaying = false;

    public SoundConfig(EntityRollingStock stock, String location, boolean repeats, int distance) {
        this.stock = stock;
        this.location = location;
        this.repeats = repeats;
        this.distance = distance;
        new SoundPacket(SoundPacket.PacketType.NEW, this, stock).sendToObserving(stock);
    }

    public SoundConfig(TagCompound tag) {
        try {
            TagSerializer.deserialize(tag, this);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
    }

    /*
     * Server Only
     */
    public void setPitch(float pitch) {
        if (pitch != this.pitch) {
            this.pitch = pitch;
            new SoundPacket(SoundPacket.PacketType.PITCH, this, stock).sendToObserving(stock);
        }
    }

    public void setVolume(float volume) {
        if (volume != this.volume) {
            this.volume = volume;
            new SoundPacket(SoundPacket.PacketType.VOLUME, this, stock).sendToObserving(stock);
        }
    }

    public void setMotion(Vec3d motion) {
        if (!motion.equals(this.motion)) {
            this.motion = motion;
            new SoundPacket(SoundPacket.PacketType.VELOCITY, this, stock).sendToObserving(stock);
        }
    }

    public void setPos(Vec3d pos) {
        if (!pos.equals(this.pos)) {
            this.pos = pos;
            new SoundPacket(SoundPacket.PacketType.POS, this, stock).sendToObserving(stock);
        }
    }

    public void play(Vec3d pos) {
        this.pos = pos;
        isPlaying = true;
        new SoundPacket(SoundPacket.PacketType.PLAY, this, stock).sendToObserving(stock);
    }
    public void play() {
        new SoundPacket(SoundPacket.PacketType.PLAY, this, stock).sendToObserving(stock);
    }

    public void stop() {
        isPlaying = false;
        new SoundPacket(SoundPacket.PacketType.STOP, this, stock).sendToObserving(stock);
    }

    public TagCompound toTag() {
        TagCompound tag = new TagCompound();
        try {
            TagSerializer.serialize(tag, this);
        } catch (SerializationException e) {
            ImmersiveRailroading.catching(e);
        }
        return tag;
    }

    private static class TagMapper implements cam72cam.mod.serialization.TagMapper<SoundConfig> {

        @Override
        public TagAccessor<SoundConfig> apply(Class<SoundConfig> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> d.set(fieldName, o.toTag()),
                    d -> new SoundConfig(d.get(fieldName))
            );
        }
    }

    public static class SoundPacket extends Packet {
        @TagField
        private PacketType type;
        @TagField(mapper = TagMapper.class)
        private SoundConfig config;
        @TagField
        private EntityRollingStock stock;

        public SoundPacket() {}
        public SoundPacket(PacketType type, SoundConfig config, EntityRollingStock stock) {
            this.type = type;
            this.config = config;
            this.stock = stock;
        }

        @Override
        protected void handle() {
            switch (type) {
                case NEW:
                    stock.getDefinition().getModel().createServerSideSound(config, stock);
                    return;
                case PLAY:
                    stock.getDefinition().getModel().getServerSideSound(config).play(stock.getPosition().add(config.pos), stock);
                    break;
                case STOP:
                    stock.getDefinition().getModel().getServerSideSound(config).stop(stock);
                    break;
                case POS:
                    stock.getDefinition().getModel().getServerSideSound(config).setPosition(stock.getPosition().add(config.pos), stock);
                    break;
                case PITCH:
                    stock.getDefinition().getModel().getServerSideSound(config).setPitch(config.pitch, stock);
                    break;
                case VELOCITY:
                    stock.getDefinition().getModel().getServerSideSound(config).setVelocity(config.motion, stock);
                    break;
                case VOLUME:
                    stock.getDefinition().getModel().getServerSideSound(config).setVolume(config.volume, stock);
                    break;
            }

            stock.getDefinition().getModel().getServerSideSound(config).setConfig(stock, config);
        }

        public enum PacketType {
            NEW,
            PLAY,
            STOP,
            POS,
            PITCH,
            VELOCITY,
            VOLUME
        }
    }
}
