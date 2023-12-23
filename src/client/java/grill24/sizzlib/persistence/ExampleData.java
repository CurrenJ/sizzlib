package grill24.sizzlib.persistence;

import com.google.gson.annotations.JsonAdapter;
import grill24.sizzlib.persistence.adapter.ItemTypeAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.io.File;

public class ExampleData extends Persistable {
    @Persists
    public Item item = Items.ACACIA_BOAT;
    public int testInt = 6;

    public ExampleData() {}

    @Override
    public File getFile() {
        return null;
    }
}
