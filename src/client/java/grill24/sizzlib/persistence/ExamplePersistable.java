package grill24.sizzlib.persistence;

import grill24.sizzlib.SizzLibClient;
import grill24.sizzlib.component.Command;
import grill24.sizzlib.component.CommandOption;

import java.io.File;

@Command
public class ExamplePersistable extends Persistable {

    @Persists
    @CommandOption
    private String myStringField;

    @Persists
    @CommandOption
    private SizzLibClient.MyEnum myEnumField;

    public ExamplePersistable() {
        PersistenceManager.load(this);
    }

    @Override
    public File getFile() {
        return new File(PersistenceManager.getRelativeDirectoryInMinecraftDirectory(SizzLibClient.DATA_DIR).toFile(), "example_persistable.dat");
    }
}
