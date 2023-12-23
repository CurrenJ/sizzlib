package grill24.sizzlib.persistence;

import com.google.gson.annotations.JsonAdapter;
import grill24.sizzlib.persistence.adapter.ItemTypeAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class ExampleData {
    @JsonAdapter(ItemTypeAdapter.class)
    public Item item = Items.ACACIA_BOAT;
    public int testInt = 6;

    public ExampleData() {}
}
