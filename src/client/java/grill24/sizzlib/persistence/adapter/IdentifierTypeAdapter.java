package grill24.sizzlib.persistence.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.Identifier;

import java.io.IOException;

public class IdentifierTypeAdapter extends TypeAdapter<Identifier> {
    @Override
    public void write(JsonWriter out, Identifier value) throws IOException {
        out.beginObject();
        out.name("identifier").value(value.toString());
        out.endObject();
    }

    @Override
    public Identifier read(JsonReader in) throws IOException {
        String identifier = "";

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "identifier":
                    identifier = in.nextString();
            }
        }
        in.endObject();

        return new Identifier(identifier);
    }
}
