package grill24.sizzlib.persistence;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PersistenceManager {
    public static void save(IPersistable persistable) {
        File dir = new File(persistable.getFile().getAbsoluteFile().getParent());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter fileWriter = new FileWriter(persistable.getFile())) {
            fileWriter.write(persistable.toJson().getAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load(IPersistable persistable) {
        try {
            File file = persistable.getFile();
            if (file.exists()) {
                persistable.fromJson(new String(Files.readAllBytes(file.toPath())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Path getRelativeDirectoryInMinecraftDirectory(String relativeDataDir) {
        return Paths.get(String.valueOf(MinecraftClient.getInstance().runDirectory), relativeDataDir);
    }
}

