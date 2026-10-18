package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.sound.ISound;
import cam72cam.mod.util.collection.ExpireableMap;

import java.util.UUID;

public class FlangeSound {
    private final Identifier def;
    private final boolean canLoop;
    private final float attenuationDistance;

    public FlangeSound(Identifier def, boolean canLoop, float attenuationDistance) {
        this.def = def;
        this.canLoop = canLoop;
        this.attenuationDistance = attenuationDistance;
    }

    private class Sound {
        private final EntityMoveableRollingStock stock;
        private final ISound sound;
        private final float sndRand;
        private float lastFlangeVolume;

        Sound(EntityMoveableRollingStock stock) {
            lastFlangeVolume = 0;
            sound = stock.createSound(def, canLoop, attenuationDistance, ConfigSound.SoundCategories.RollingStock::flange);
            this.stock = stock;
            this.sndRand = (float) Math.random() / 10;
        }

        void effects() {
            float yawDelta = stock.getAngle();
            double startingFlangeSpeed = 5;
            double kmh = Math.abs(stock.getCurrentSpeed().metric());
            double flangeMinYaw = stock.getDefinition().flange_min_yaw;
            // https://en.wikipedia.org/wiki/Minimum_railway_curve_radius#Speed_and_cant implies squared speed
            flangeMinYaw = flangeMinYaw / Math.sqrt(kmh) * Math.sqrt(startingFlangeSpeed);
            if (yawDelta > flangeMinYaw && kmh > 5) {
                if (!sound.isPlaying()) {
                    lastFlangeVolume = 0.1f;
                    sound.setVolume(lastFlangeVolume);
                    sound.play(stock.getPosition());
                }
                sound.setPitch(0.9f + (float)kmh / 600 + sndRand);
                float oscillation = (float)Math.sin((stock.getTickCount()/40f * sndRand * 40));
                double flangeFactor = (yawDelta - flangeMinYaw) / (90 - flangeMinYaw);
                float desiredVolume = (float)flangeFactor/2 * oscillation/4 + 0.25f;
                lastFlangeVolume = (lastFlangeVolume*4 + desiredVolume) / 5;
                sound.setVolume(lastFlangeVolume);
                sound.setPosition(stock.getPosition());
                sound.setVelocity(stock.getVelocity());
            } else {
                if (sound.isPlaying()) {
                    if (lastFlangeVolume > 0.1) {
                        lastFlangeVolume = (lastFlangeVolume*4 + 0) / 5;
                        sound.setVolume(lastFlangeVolume);
                        sound.setPosition(stock.getPosition());
                        sound.setVelocity(stock.getVelocity());
                    } else {
                        sound.stop();
                    }
                }
            }
        }

        public void removed() {
            sound.stop();
        }
    }
    private final ExpireableMap<UUID, Sound> sounds = new ExpireableMap<>((key, value) -> value.removed());

    public void effects(EntityMoveableRollingStock stock) {
    	Sound sound = sounds.get(stock.getUUID());
    	if (sound == null) {
    		sound = new Sound(stock);
    		sounds.put(stock.getUUID(), sound);
    	}
        sound.effects();
    }

    public void removed(EntityMoveableRollingStock stock) {
        sounds.remove(stock.getUUID());
    }
}
