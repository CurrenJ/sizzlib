package grill24.sizzlib.persistence;

import net.minecraft.nbt.NbtCompound;

@FunctionalInterface
public interface GetValueFromTag {
    Object get(NbtCompound tag, String key);
}
