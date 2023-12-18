package grill24.sizzlib.component.accessor;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface GetCommandArgumentValue {
    // Returns the new field value to be set, given a command context. This is used for setting the value of a field when a command is run.
    Object get(CommandContext context, String key) throws CommandSyntaxException;
}
