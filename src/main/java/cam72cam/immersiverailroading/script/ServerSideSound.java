package cam72cam.immersiverailroading.script;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.sound.SoundCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerSideSound<ENTITY extends EntityRollingStock> {
    public Map<UUID, SoundConfig> stockConfig = new HashMap<>();
    private final Map<UUID, ISound> sounds = new HashMap<>();
    private final Map<UUID, Boolean> shouldPlay = new HashMap<>();

    public void createSound(ENTITY stock) {
        SoundConfig config = stockConfig.get(stock.getUUID());
        if (config == null) return;

        try {
            ISound sound = Audio.newSound(new Identifier(config.location), SoundCategory.MASTER, config.repeats, (float) (config.distance * ConfigSound.soundDistanceScale), stock.soundScale());
            sounds.put(stock.getUUID(), sound);
        } catch (Exception e) {
            ModCore.catching(e, "An error occurred while creating sound %s. Maybe wrong location? Error: %s", config.location, e);
        }
    }

    public void setConfig(EntityRollingStock stock, SoundConfig config) {
        stockConfig.put(stock.getUUID(), config);
    }

    public void effects(ENTITY stock) {
        ISound toUpdate = null;
        SoundConfig config = stockConfig.get(stock.getUUID());
        ISound sound = sounds.get(stock.getUUID());

        if (sound != null) {
            if (config.repeats && !sound.isPlaying() && shouldPlay.getOrDefault(stock.getUUID(), false)) {
                sound.play(stock.getPosition().add(config.pos));
            }

            if (sound.isPlaying()) {
                toUpdate = sound;
            }

            sound.setPitch(config.pitch);
            sound.setVolume(config.volume);
        }

        if (toUpdate != null) {
            toUpdate.setPosition(stock.getPosition());
            toUpdate.setVelocity(stock.getVelocity());
        }
    }

    public void removed(ENTITY stock) {
        if (sounds.containsKey(stock.getUUID())) {
            sounds.get(stock.getUUID()).stop();
        }
    }

    public void play(Vec3d pos, EntityRollingStock stock) {
        if (sounds.containsKey(stock.getUUID())) {
            sounds.get(stock.getUUID()).play(pos);
            shouldPlay.put(stock.getUUID(), true);
        }
    }

    public void stop(EntityRollingStock stock) {
        if (sounds.containsKey(stock.getUUID())) {
            sounds.get(stock.getUUID()).stop();
            shouldPlay.put(stock.getUUID(), false);
        }
    }

    public void setPosition(Vec3d pos, EntityRollingStock stock) {
        if (sounds.containsKey(stock.getUUID())) {
            sounds.get(stock.getUUID()).setPosition(pos);
        }
    }

    public void setPitch(float f, EntityRollingStock stock) {
        if (sounds.containsKey(stock.getUUID())) {
            sounds.get(stock.getUUID()).setPitch(f);
        }
    }

    public void setVelocity(Vec3d vel, EntityRollingStock stock) {
        if (sounds.containsKey(stock.getUUID())) {
            sounds.get(stock.getUUID()).setVelocity(vel);
        }
    }

    public void setVolume(float f, EntityRollingStock stock) {
        if (sounds.containsKey(stock.getUUID())) {
            sounds.get(stock.getUUID()).setVolume(f);
        }
    }

    public boolean isPlaying(EntityRollingStock stock) {
        return sounds.get(stock.getUUID()).isPlaying();
    }
}
