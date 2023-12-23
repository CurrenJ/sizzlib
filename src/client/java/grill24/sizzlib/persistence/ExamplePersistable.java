package grill24.sizzlib.persistence;

import grill24.sizzlib.SizzLibClient;
import grill24.sizzlib.component.Command;
import grill24.sizzlib.component.CommandAction;
import grill24.sizzlib.component.CommandOption;
import grill24.sizzlib.component.ComponentUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Command(debug = true)
public class ExamplePersistable extends Persistable {

    @Persists
    @CommandOption
    protected String myStringField;

    @Persists
    @CommandOption
    protected SizzLibClient.MyEnum myEnumField;

    @Persists
    @CommandOption(readOnly = true)
    protected HashMap<BlockPos, Integer> myHashMapField;

    @Persists
    @CommandOption(debug = true)
    protected BlockPos pos;

    @Persists
    @CommandOption
    protected Item item;

    @Persists
    @CommandOption
    protected ExampleData exampleData;

    public ExamplePersistable() {
        myHashMapField = new HashMap<>();
        exampleData = new ExampleData();

        PersistenceManager.load(this);
    }

    @CommandAction
    public void populateMyHashMapField() {
        myHashMapField.put(BlockPos.ORIGIN.up(1).toImmutable(), 1);
        myHashMapField.put(BlockPos.ORIGIN.up(2).toImmutable(), 2);
        myHashMapField.put(BlockPos.ORIGIN.up(3).toImmutable(), 3);
        PersistenceManager.save(this);
    }

    @CommandAction
    public void view() {
        ComponentUtility.print(MinecraftClient.getInstance(), myHashMapField.toString());
        for (Map.Entry<BlockPos, Integer> entry : myHashMapField.entrySet())
            ComponentUtility.print(MinecraftClient.getInstance(), entry.toString());
    }

    @Override
    public File getFile() {
        return new File(PersistenceManager.getRelativeDirectoryInMinecraftDirectory(SizzLibClient.DATA_DIR).toFile(), "example_persistable.dat");
    }
}
