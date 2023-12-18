package grill24.sizzlib;

import com.mojang.brigadier.context.CommandContext;
import grill24.sizzlib.component.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

@Command("sizzLib")
public class SizzLibClient implements ClientModInitializer {

    private static ModComponentRegistry modComponentRegistry;

    private enum MyEnum {A, B, C}

    ;
    @CommandOption
    private static MyEnum myEnumField;

    @CommandOption(readOnly = true)
    private static String myReadOnlyField = "READ-ONLY!";

    @Override
    public void onInitializeClient() {
        modComponentRegistry = new ModComponentRegistry(SizzLibClient.class);
        modComponentRegistry.registerComponents();
    }

    @CommandAction
    public static void item(CommandContext<FabricClientCommandSource> commandContext, Item item) {
        ComponentUtility.print(commandContext, item.toString());
    }

    @CommandAction
    public static void itemStackArgument(CommandContext<FabricClientCommandSource> commandContext, ItemStackArgument itemStack) {
        ComponentUtility.print(commandContext, itemStack.getItem().toString());
    }

    @CommandAction
    public static void stringArgument(CommandContext<FabricClientCommandSource> commandContext, String string) {
        ComponentUtility.print(commandContext, string);
    }

    @CommandAction
    public static void blockPos(CommandContext<FabricClientCommandSource> commandContext, BlockPos blockPos) {
        ComponentUtility.print(commandContext, blockPos.toString());
    }

    @CommandAction
    public static void myEnumAction(CommandContext<FabricClientCommandSource> commandContext, MyEnum myEnumInstance) {
        ComponentUtility.print(commandContext, myEnumInstance.toString());
    }

    @StaticToString
    public static String staticToString() {
        return "SizzLib provides client-side development utilities.";
    }
}