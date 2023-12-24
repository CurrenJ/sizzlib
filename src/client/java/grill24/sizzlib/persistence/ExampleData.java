package grill24.sizzlib.persistence;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class ExampleData {
    public Item item = Items.ACACIA_BOAT;
    public int testInt = 6;

    public HashMap<Item, Integer> map = new HashMap<>() {{
        put(Items.CACTUS, 1);
    }};

    public Identifier identifier = new Identifier("sizzlib:test");

    public ExampleData() {
    }
}
