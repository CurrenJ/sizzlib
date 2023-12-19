package grill24.sizzlib.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import grill24.sizzlib.component.ComponentUtility;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class Persistable implements IPersistable {

    @Override
    public void fromJson(String jsonString) throws IllegalAccessException {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            field.setAccessible(true);
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
            field.setAccessible(true);
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();

            Object fieldValue = field.get(this);
            if (fieldValue != null) {
                Class<?> fieldType = field.getType();
                if (Map.class.isAssignableFrom(fieldType) || List.class.isAssignableFrom(fieldType)) {
                    // Oh-no! Java has compile-time type erasure on generics! Hacky workaround :)
                    Type type = TypeToken.getParameterized(field.getType(), persistsAnnotation.genericTypes()).getType();
                    jsonObject.add(key, gson.toJsonTree(fieldValue, type));
                }
                else {
                    // Default behaviour.
                    jsonObject.add(key, gson.toJsonTree(fieldValue, field.getType()));
                }
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
