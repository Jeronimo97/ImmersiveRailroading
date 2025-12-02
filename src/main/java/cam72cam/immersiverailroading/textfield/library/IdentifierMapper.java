package cam72cam.immersiverailroading.textfield.library;

import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

public class IdentifierMapper implements TagMapper<Identifier> {

    @Override
    public TagAccessor<Identifier> apply(Class<Identifier> type, String fieldName, TagField tag) throws SerializationException {
        return new TagAccessor<>(
                (d, o) -> d.setString(fieldName, o.toString()),
                d -> new Identifier(d.getString(fieldName))
        );
    }
}
