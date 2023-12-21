package grill24.sizzlib.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import grill24.sizzlib.component.ComponentUtility;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class Persistable implements IPersistable {

    @Override
    public void fromJson(String jsonString) throws IllegalAccessException {
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            field.setAccessible(true);
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();
            JsonElement jsonElement = jsonObject.get(key);

            if (jsonElement != null) {
                if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                    // LOOK AT THIS CODE! It's beautiful. Allows us to access generic types (like K,V in a Map) at runtime via reflection.
                    Type[] genericTypes = parameterizedType.getActualTypeArguments();
                    Type type = TypeToken.getParameterized(field.getType(), genericTypes).getType();
                    setField(field, gson.fromJson(jsonElement, type));
                } else {
                    setField(field, gson.fromJson(jsonElement, field.getType()));
                }
            }
        }
    }

    private void setField(Field field, Object value) throws IllegalAccessException {
        field.set(this, value);
    }

    @Override
    public String toJson() throws IllegalAccessException {
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        JsonObject jsonObject = new JsonObject();

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            field.setAccessible(true);
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();

            Object fieldValue = field.get(this);
            if (fieldValue != null) {
                if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                    Type[] genericTypes = parameterizedType.getActualTypeArguments();
                    Type type = TypeToken.getParameterized(field.getType(), genericTypes).getType();
                    jsonObject.add(key, gson.toJsonTree(fieldValue, type));
                } else {
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
