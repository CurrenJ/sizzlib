package grill24.sizzlib.persistence;

import com.google.gson.annotations.JsonAdapter;
import grill24.sizzlib.persistence.adapter.ItemTypeAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.io.File;
import java.util.HashMap;

public class ExampleData {
    public Item item = Items.ACACIA_BOAT;
    public int testInt = 6;

    public HashMap<Item, Integer> map = new HashMap<>() {{ put(Items.CACTUS, 1); }};

    public ExampleData() {}
}
