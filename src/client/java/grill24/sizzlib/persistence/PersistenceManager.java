package grill24.sizzlib.persistence;

import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PersistenceManager {
    public static void save(IPersistable persistable) {
        Path modDataDirectory = persistable.getFile().toPath();
        if (!modDataDirectory.toFile().exists()) {
            modDataDirectory.toFile().mkdirs();
        }

        try {
            File file = persistable.getFile();
            NbtCompound tag = persistable.writeToNBT();
            NbtIo.writeCompressed(tag, file.toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load(IPersistable persistable) {
        try {
            File file = persistable.getFile();
            if (file.exists()) {
                NbtCompound tag = NbtIo.readCompressed(file.toPath(), NbtTagSizeTracker.ofUnlimitedBytes());
                persistable.readFromNBT(tag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Path getRelativeDirectoryInMinecraftDirectory(String relativeDataDir) {
        return Paths.get(String.valueOf(MinecraftClient.getInstance().runDirectory), relativeDataDir);
    }
}

