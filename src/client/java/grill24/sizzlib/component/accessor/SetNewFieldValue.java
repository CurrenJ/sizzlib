package grill24.sizzlib.component.accessor;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface SetNewFieldValue<S> {
    // Sets the value of a field.
    void set(CommandContext<S> context, Object value) throws CommandSyntaxException;
}
