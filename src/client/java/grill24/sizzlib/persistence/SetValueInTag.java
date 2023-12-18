package grill24.sizzlib.persistence;

import net.minecraft.nbt.NbtCompound;

@FunctionalInterface
public interface SetValueInTag {
    void set(NbtCompound tag, String key, Object value);
}
