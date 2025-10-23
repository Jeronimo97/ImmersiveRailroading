package cam72cam.immersiverailroading.textfield.library;

import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

import java.util.List;

public class IdentifierListMapper implements TagMapper<List<Identifier>> {

    @Override
    public TagAccessor<List<Identifier>> apply(Class<List<Identifier>> type, String fieldName, TagField tag) throws SerializationException {
        return new TagAccessor<List<Identifier>>(
                (d, o) -> d.setList(fieldName, o, s -> new TagCompound().setString("s", s.toString())),
                d -> d.getList(fieldName, l -> new Identifier(l.getString("s")))
        ) {
            @Override
            public boolean applyIfMissing() {
                return true;
            }
        };
    }
}
