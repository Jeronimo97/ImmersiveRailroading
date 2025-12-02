package cam72cam.immersiverailroading.textfield.library;

import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.util.List;

public class StringListMapper implements TagMapper<List<String>> {

    @Override
    public TagAccessor<List<String>> apply(Class<List<String>> type, String fieldName, TagField tag) throws SerializationException {
        return new TagAccessor<List<String>>(
                (d, o) -> d.setList(fieldName, o, s -> new TagCompound().setString("s", s)),
                d -> d.getList(fieldName, l -> l.getString("s"))
        ) {
            @Override
            public boolean applyIfMissing() {
                return true;
            }
        };
    }
}
