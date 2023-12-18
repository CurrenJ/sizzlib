package grill24.sizzlib.persistence;

import grill24.sizzlib.component.ComponentUtility;
import net.minecraft.nbt.NbtCompound;

import java.lang.reflect.Field;
import java.util.HashMap;

public abstract class Persistable implements IPersistable {
    private record NbtAccessors(GetValueFromTag getValueFromTag, SetValueInTag setValueInTag) {
    }

    private static HashMap<Class<?>, NbtAccessors> supportedTypes;

    static {
        supportedTypes = new HashMap<>();
        supportedTypes.put(Boolean.class, new NbtAccessors((tag, key) -> tag.getBoolean(key), (tag, key, value) -> tag.putBoolean(key, (Boolean) value)));
        supportedTypes.put(String.class, new NbtAccessors((tag, key) -> tag.getString(key), ((tag, key, value) -> tag.putString(key, (String) value))));
        supportedTypes.put(Integer.class, new NbtAccessors((tag, key) -> tag.getInt(key), ((tag, key, value) -> tag.putInt(key, (int) value))));
        supportedTypes.put(Long.class, new NbtAccessors((tag, key) -> tag.getLong(key), ((tag, key, value) -> tag.putLong(key, (int) value))));
        supportedTypes.put(Float.class, new NbtAccessors((tag, key) -> tag.getFloat(key), ((tag, key, value) -> tag.putFloat(key, (int) value))));
        supportedTypes.put(Double.class, new NbtAccessors((tag, key) -> tag.getDouble(key), ((tag, key, value) -> tag.putDouble(key, (int) value))));
    }

    @Override
    public void readFromNBT(NbtCompound tag) throws IllegalAccessException {
        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();

            try {
                field.setAccessible(true);
                if (field.getType().isEnum()) {
                    setField(field, ComponentUtility.getEnumValueFromSerializedString(tag.getString(key), field.getType()));
                } else if (supportedTypes.containsKey(field.getType())) {
                    setField(field, supportedTypes.get(field.getType()).getValueFromTag.get(tag, key));
                }
            } catch (IllegalAccessException e) {
                throw e;
            }
        }
    }

    private void setField(Field field, Object value) throws IllegalAccessException {
        field.set(field.get(this), value);
    }

    @Override
    public NbtCompound writeToNBT() throws IllegalAccessException {
        NbtCompound tag = new NbtCompound();

        for (Field field : ComponentUtility.getFieldsWithAnnotation(this.getClass(), Persists.class)) {
            Persists persistsAnnotation = field.getAnnotation(Persists.class);
            String key = persistsAnnotation.value().isEmpty() ? field.getName() : persistsAnnotation.value();

            try {
                field.setAccessible(true);

                Object fieldValue = field.get(this);
                if (fieldValue != null) {
                    if (field.getType().isEnum()) {
                        tag.putString(key, ((Enum) fieldValue).name());
                    } else if (supportedTypes.containsKey(field.getType())) {
                        supportedTypes.get(field.getType()).setValueInTag.set(tag, key, fieldValue);
                    }
                }
            } catch (IllegalAccessException e) {
                throw e;
            }
        }

        return tag;
    }

    @Override
    public String toString() {
        try {
            return this.writeToNBT().toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
