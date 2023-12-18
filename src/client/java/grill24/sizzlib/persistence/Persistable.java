package grill24.sizzlib.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import grill24.sizzlib.component.ComponentUtility;

import java.lang.reflect.Field;

public abstract class Persistable implements IPersistable {

    @Override
    public void fromJson(String jsonString) throws IllegalAccessException {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();
            JsonElement jsonElement = jsonObject.get(key);

            if (jsonElement != null) {
                setField(field, gson.fromJson(jsonElement, field.getType()));
            }
        }
    }

    private void setField(Field field, Object value) throws IllegalAccessException {
        field.set(this, value);
    }

    @Override
    public String toJson() throws IllegalAccessException {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();

            field.setAccessible(true);
            Object fieldValue = field.get(this);
            if (fieldValue != null) {
                jsonObject.add(key, gson.toJsonTree(fieldValue));
            }
        }

        return jsonObject.toString();
    }

    @Override
    public String toString() {
        try {
            return this.toJson().toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
