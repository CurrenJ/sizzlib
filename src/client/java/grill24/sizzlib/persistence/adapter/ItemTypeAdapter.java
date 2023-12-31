package grill24.sizzlib.persistence.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;

public class ItemTypeAdapter extends TypeAdapter<Item> {
    @Override
    public void write(JsonWriter out, Item value) throws IOException {
        out.beginObject();
        if (value != null) {
            out.name("id").value(String.valueOf(Registries.ITEM.getId(value)));
        }
        out.endObject();
    }

    @Override
    public Item read(JsonReader in) throws IOException {
        Identifier id = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "id":
                    id = new Identifier(in.nextString());
                    break;
            }
        }
        in.endObject();

        return Registries.ITEM.get(id);
    }
}
