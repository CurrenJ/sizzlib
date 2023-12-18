package grill24.sizzlib.persistence;

import net.minecraft.nbt.NbtCompound;

import java.io.File;

public interface IPersistable {
    void readFromNBT(NbtCompound tag) throws IllegalAccessException;

    NbtCompound writeToNBT() throws IllegalAccessException;

    File getFile();
}