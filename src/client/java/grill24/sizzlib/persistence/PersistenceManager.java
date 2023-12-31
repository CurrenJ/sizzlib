package grill24.sizzlib.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import grill24.sizzlib.component.ComponentUtility;
import grill24.sizzlib.persistence.adapter.BlockPosTypeAdapter;
import grill24.sizzlib.persistence.adapter.IdentifierTypeAdapter;
import grill24.sizzlib.persistence.adapter.ItemTypeAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PersistenceManager {
    public static void save(IFileProvider fileProvider, Class<?> clazz) {
        File dir = new File(fileProvider.getFile().getAbsoluteFile().getParent());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(fileProvider.getFile())) {
            fileWriter.write(toJson(fileProvider, clazz).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save(IFileProvider fileProvider) {
        save(fileProvider, fileProvider.getClass());
    }

    public static void load(IFileProvider fileProvider, Class<?> clazz) {
        try {
            File file = fileProvider.getFile();
            if (file.exists()) {
                fromJson(fileProvider, clazz, new String(Files.readAllBytes(file.toPath())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load(IFileProvider fileProvider) {
        load(fileProvider, fileProvider.getClass());
    }

    private static void fromJson(Object obj, Class<?> clazz, String jsonString) throws IllegalAccessException {
        Gson gson = getGsonBuilder().create();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        for (Field field : ComponentUtility.getFieldsToPersist(clazz)) {
            field.setAccessible(true);
            String key = field.getName();
            JsonElement jsonElement = jsonObject.get(key);

            if (jsonElement != null) {
                Type type = getTypeToken(field).getType();
                setField(obj, field, gson.fromJson(jsonElement, type));
            }
        }
    }

    private static void setField(Object obj, Field field, Object value) throws IllegalAccessException {
        field.set(obj, value);
    }

    public static JsonObject toJson(Object obj, Class<?> clazz) throws IllegalAccessException {
        Gson gson = getGsonBuilder().create();
        JsonObject jsonObject = new JsonObject();

        for (Field field : ComponentUtility.getFieldsToPersist(clazz)) {
            field.setAccessible(true);
            String key = field.getName();

            Object fieldValue = field.get(obj);
            if (fieldValue != null) {
                Type type = getTypeToken(field).getType();
                jsonObject.add(key, gson.toJsonTree(fieldValue, type));
            }
        }

        return jsonObject;
    }

    public static Path getRelativeDirectoryInMinecraftDirectory(String relativeDataDir) {
        return Paths.get(String.valueOf(MinecraftClient.getInstance().runDirectory), relativeDataDir);
    }

    private static GsonBuilder getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(BlockPos.class, new BlockPosTypeAdapter())
                .registerTypeAdapter(Identifier.class, new IdentifierTypeAdapter())
                .registerTypeHierarchyAdapter(Item.class, new ItemTypeAdapter())
                .enableComplexMapKeySerialization()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);
        return gsonBuilder;
    }

    private static TypeToken getTypeToken(Field field) {
        if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
            Type[] genericTypes = parameterizedType.getActualTypeArguments();
            return TypeToken.getParameterized(field.getType(), genericTypes);
        }
        return TypeToken.get(field.getType());
    }

    public static String getDebugStringForField(Field field, Object value) {
        TypeToken typeToken = getTypeToken(field);
        return getGsonBuilder().create().toJson(value, typeToken.getType());
    }

    public static String getDebugString(Object obj, Class<?> clazz) {
        String str = "";
        if (str.isEmpty() || str.equals("{}")) {
            str = ComponentUtility.toStringStatic(clazz);
        }

        if (str.isEmpty() || str.equals("{}")) {
            str = getGsonBuilder().create().toJson(obj);
        }

        if ((str.isEmpty() || str.equals("{}")) && obj != null) {
            str = obj.toString();
        }

        return str;
    }
}

