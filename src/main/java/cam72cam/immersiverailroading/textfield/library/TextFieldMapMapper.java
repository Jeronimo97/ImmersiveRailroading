package cam72cam.immersiverailroading.textfield.library;

import cam72cam.immersiverailroading.textfield.TextFieldConfig;
import cam72cam.mod.serialization.*;

import java.util.Map;

public class TextFieldMapMapper implements TagMapper<Map<String, TextFieldConfig>> {
    @Override
    public TagAccessor<Map<String, TextFieldConfig>> apply(Class type, String fieldName, TagField tag) throws SerializationException {
        return new TagAccessor<>(
                (d, o) -> d.setMap(fieldName, o, k -> k, m -> {
                    try {
                        TagCompound compound = new TagCompound();
                        TagSerializer.serialize(compound, m);
                        return compound;
                    } catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
                }),
                (d, w) -> d.getMap(fieldName, k -> k, v -> {
                    try {
                        TextFieldConfig config = new TextFieldConfig();
                        TagSerializer.deserialize(v, config);
                        return config;
                    } catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }
}
