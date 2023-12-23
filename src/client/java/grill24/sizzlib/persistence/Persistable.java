package grill24.sizzlib.persistence;

import com.google.gson.*;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.sun.jdi.InvalidTypeException;
import grill24.sizzlib.component.ComponentUtility;
import grill24.sizzlib.persistence.adapter.BlockPosTypeAdapter;
import grill24.sizzlib.persistence.adapter.ItemTypeAdapter;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class Persistable implements IPersistable {

    @Override
    public void fromJson(String jsonString) throws IllegalAccessException {
        Gson gson = getGsonBuilder().create();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            field.setAccessible(true);
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();
            JsonElement jsonElement = jsonObject.get(key);

            if (jsonElement != null) {
                Type type = getTypeToken(field).getType();
                setField(field, gson.fromJson(jsonElement, type));
            }
        }
    }

    private void setField(Field field, Object value) throws IllegalAccessException {
        field.set(this, value);
    }

    @Override
    public JsonObject toJson() throws IllegalAccessException {
        Gson gson = getGsonBuilder().create();
        JsonObject jsonObject = new JsonObject();

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            field.setAccessible(true);
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();

            Object fieldValue = field.get(this);
            if (fieldValue != null) {
                Type type = getTypeToken(field).getType();
                jsonObject.add(key, gson.toJsonTree(fieldValue, type));
            }
        }

        return jsonObject;
    }

    private static GsonBuilder getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(BlockPos.class, new BlockPosTypeAdapter())
                .registerTypeHierarchyAdapter(Item.class, new ItemTypeAdapter())
                .enableComplexMapKeySerialization()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT);
        return gsonBuilder;
    }

    private static TypeToken getTypeToken(Field field) {
        if(field.getGenericType() instanceof ParameterizedType parameterizedType) {
            Type[] genericTypes = parameterizedType.getActualTypeArguments();
            return TypeToken.getParameterized(field.getType(), genericTypes);
        }
        return TypeToken.get(field.getType());
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
