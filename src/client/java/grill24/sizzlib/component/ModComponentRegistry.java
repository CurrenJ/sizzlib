package grill24.sizzlib.component;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import grill24.sizzlib.IDefaultPosArgumentMixin;
import grill24.sizzlib.component.accessor.GetCommandArgumentValue;
import grill24.sizzlib.component.accessor.GetFieldValue;
import grill24.sizzlib.component.accessor.SetNewFieldValue;
import grill24.sizzlib.persistence.IFileProvider;
import grill24.sizzlib.persistence.PersistenceManager;
import grill24.sizzlib.persistence.Persists;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import oshi.util.tuples.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ModComponentRegistry {
    private record ComponentDto(Object instance, Class<?> clazz) {
    }

    private List<ComponentDto> components;

    private record ClientTickMethodDto(Object instance, Method method, int tickRate) {
    }

    private List<ClientTickMethodDto> clientTickMethods;

    private record ScreenMethodDto(Object instance, Method method) {
    }

    private List<ScreenMethodDto> screenTickMethods;
    private List<ScreenMethodDto> screenInitMethods;

    private CommandTreeNode commandTreeRoot;
    private ComponentDto commandRootComponent;

    private record SupportedCommandArgumentType(Class parsedArgumentClass,
                                                Function<CommandRegistryAccess, ? extends ArgumentType> getArgumentType,
                                                GetCommandArgumentValue getArgumentValue) {
    }

    private HashMap<Class, SupportedCommandArgumentType> supportedArgumentTypes;

    private int tickCounter;

    private boolean isDebug;

    public static class CommandTreeNode {
        public String literal;
        public LiteralArgumentBuilder<FabricClientCommandSource> command;
        public HashMap<String, CommandTreeNode> children;

        public CommandTreeNode(String literal, LiteralArgumentBuilder<FabricClientCommandSource> command) {
            this.literal = literal;
            this.command = command;
            this.children = new HashMap<>();
        }

        public Optional<CommandTreeNode> getChildNode(String literal) {
            return children.containsKey(literal) ? Optional.of(children.get(literal)) : Optional.empty();
        }
    }

    public ModComponentRegistry(String commandRootString) {
        commandTreeRoot = new CommandTreeNode(commandRootString, ClientCommandManager.literal(commandRootString));
        initialize();
    }

    public ModComponentRegistry(Class<?> clazz) {
        commandRootComponent = new ComponentDto(null, clazz);
        initialize();
    }

    public ModComponentRegistry(Object instance) {
        commandRootComponent = new ComponentDto(instance, instance.getClass());
        initialize();
    }

    private void initialize() {
        components = new ArrayList<>();
        clientTickMethods = new ArrayList<>();
        screenTickMethods = new ArrayList<>();
        screenInitMethods = new ArrayList<>();

        supportedArgumentTypes = new HashMap<>();

        // ItemStackArgument
        addSupportedActionTargetType(new SupportedCommandArgumentType(
                ItemStackArgument.class,
                ItemStackArgumentType::itemStack,
                ItemStackArgumentType::getItemStackArgument));

        // Item
        addSupportedActionTargetType(new SupportedCommandArgumentType(
                Item.class,
                ItemStackArgumentType::itemStack,
                (commandContext, key) -> {
                    return ItemStackArgumentType.getItemStackArgument(commandContext, key).getItem();
                }));

        // String
        addSupportedActionTargetType(new SupportedCommandArgumentType(
                String.class,
                commandRegistryAccess -> StringArgumentType.string(),
                StringArgumentType::getString));

        // BlockPos
        addSupportedActionTargetType(new SupportedCommandArgumentType(
                BlockPos.class, commandRegistryAccess -> BlockPosArgumentType.blockPos(),
                (commandContext, key) -> {
                    PosArgument pos = (PosArgument) commandContext.getArgument(key, PosArgument.class);
                    if (pos instanceof DefaultPosArgument defaultPosArgument && commandContext.getSource() instanceof FabricClientCommandSource fabricClientCommandSource) {
                        Position position = ((IDefaultPosArgumentMixin) defaultPosArgument).toAbsolutePos(fabricClientCommandSource.getPosition());
                        BlockPos blockPos = BlockPos.ofFloored(position);

                        return blockPos;
                    }
                    return null;
                }));

        // Boolean
        addSupportedActionTargetType(new SupportedCommandArgumentType(
                Boolean.class, commandRegistryAccess -> BoolArgumentType.bool(),
                BoolArgumentType::getBool
        ));
    }

    public <T extends ArgumentType> void addSupportedActionTargetType(SupportedCommandArgumentType supportedCommandArgumentType) {
        supportedArgumentTypes.put(supportedCommandArgumentType.parsedArgumentClass, supportedCommandArgumentType);
    }

    public void registerComponent(Object component) {
        components.add(new ComponentDto(component, component.getClass()));
    }

    public void registerComponent(Class<?> clazz) {
        components.add(new ComponentDto(null, clazz));
    }

    public void registerComponents() {
        registerComponentCommands();
        registerTickEvents();
        registerScreenTickEvents();
    }

    private void registerComponentCommands() {
        commandTreeRoot = null;

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            if (commandTreeRoot == null && commandRootComponent != null) {
                LiteralArgumentBuilder<FabricClientCommandSource> rootCommand = buildCommandsFromAnnotations(commandRootComponent, registryAccess, commandTreeRoot, supportedArgumentTypes, isDebug);

                if (rootCommand != null) {
                    commandTreeRoot = new CommandTreeNode(ComponentUtility.getCommandKey(commandRootComponent.clazz), rootCommand);
                }
            }

            if (commandTreeRoot != null) {
                for (ComponentDto component : this.components) {
                    LiteralArgumentBuilder<FabricClientCommandSource> command = buildCommandsFromAnnotations(component, registryAccess, commandTreeRoot, supportedArgumentTypes, isDebug);

                    if (command != null) {
                        commandTreeRoot.command.then(command);

                        String commandKey = ComponentUtility.getCommandKey(component.clazz);
                        commandTreeRoot.children.put(commandKey, new CommandTreeNode(commandKey, command));
                    }
                }

                dispatcher.register(commandTreeRoot.command);
            }
        });
    }

    /**
     * Register tick events for all methods with {@link ClientTick} annotations.
     */
    private void registerTickEvents() {
        this.clientTickMethods = new ArrayList<>();
        for (ComponentDto component : this.components) {
            for (Method method : ComponentUtility.getClientTickMethods(component.clazz)) {
                this.clientTickMethods.add(new ClientTickMethodDto(component.instance, method, method.getAnnotation(ClientTick.class).value()));
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            tickCounter++;
            for (ClientTickMethodDto clientTickMethodDto : clientTickMethods) {
                doClientTick(clientTickMethodDto);
            }
            if (tickCounter == Integer.MAX_VALUE)
                tickCounter = 0;
        });
    }

    /**
     * Register screen tick events for all methods with {@link ScreenTick} or {@link ScreenInit} annotations..
     */
    private void registerScreenTickEvents() {
        this.screenInitMethods = new ArrayList<>();
        this.screenTickMethods = new ArrayList<>();

        for (ComponentDto component : this.components) {
            for (Method method : ComponentUtility.getScreenTickMethods(component.clazz)) {
                this.screenTickMethods.add(new ScreenMethodDto(component.instance, method));
            }

            for (Method method : ComponentUtility.getScreenInitMethods(component.clazz)) {
                this.screenInitMethods.add(new ScreenMethodDto(component.instance, method));
            }
        }

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?>) {
                registerScreenMethods(client, screen, screenInitMethods);

                // Repeatedly called while screen is being rendered (each tick).
                ScreenEvents.afterTick(screen).register((tickScreen) -> {
                    registerScreenMethods(client, screen, screenTickMethods);
                });
            }
        });
    }

    private void registerScreenMethods(MinecraftClient client, Screen screen, List<ScreenMethodDto> screenInitMethods) {
        for (ScreenMethodDto screenInitMethodDto : screenInitMethods) {
            try {
                screenInitMethodDto.method.setAccessible(true);
                Parameter[] parameters = screenInitMethodDto.method.getParameters();
                if (parameters.length == 2 && parameters[0].getType() == MinecraftClient.class && parameters[1].getType() == Screen.class)
                    screenInitMethodDto.method.invoke(screenInitMethodDto.instance, client, screen);
                else if (parameters.length == 1 && parameters[0].getType() == Screen.class)
                    screenInitMethodDto.method.invoke(screenInitMethodDto.instance, screen);
                else if (parameters.length == 1 && parameters[0].getType() == MinecraftClient.class)
                    screenInitMethodDto.method.invoke(screenInitMethodDto.instance, client);
                else if (parameters.length == 0)
                    screenInitMethodDto.method.invoke(screenInitMethodDto.instance);
                else
                    throw new Exception("Invalid parameters for subCommand action method: " + screenInitMethodDto.method.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Build command from an object component whose class has the {@link Command} annotation.
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommandsFromAnnotations(ComponentDto component, CommandRegistryAccess commandRegistryAccess, CommandTreeNode commandRoot, HashMap<Class, SupportedCommandArgumentType> supportedActionArgumentTypes, boolean isDebug) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client != null) {
            Class<?> clazz = component.clazz;

            if (ComponentUtility.hasCustomClassAnnotation(clazz, Command.class) && (!clazz.getAnnotation(Command.class).debug() || isDebug)) {
                String commandKey = ComponentUtility.getCommandKey(component.clazz);

                com.mojang.brigadier.Command<FabricClientCommandSource> printInstance = (context) -> {
                    ComponentUtility.print(context, ComponentUtility.getDebugString(component.instance, component.clazz));
                    return 1;
                };


                LiteralArgumentBuilder<FabricClientCommandSource> command = ComponentUtility.getCommandOrElse(commandRoot, commandKey, (key) -> ClientCommandManager.literal(commandKey)).executes(printInstance);

                for (Pair<CommandOption, LiteralArgumentBuilder<FabricClientCommandSource>> subCommandData : buildCommandsFromFields(component, commandRegistryAccess, supportedActionArgumentTypes, isDebug)) {
                    attachSubCommandToParentCommand(commandRoot, subCommandData.getA().parentKey(), command, subCommandData.getB());
                }

                for (Pair<CommandAction, LiteralArgumentBuilder<FabricClientCommandSource>> subCommandData : buildCommandsFromMethods(component, commandRegistryAccess, supportedActionArgumentTypes, isDebug)) {
                    attachSubCommandToParentCommand(commandRoot, subCommandData.getA().parentKey(), command, subCommandData.getB());
                }

                return command;
            }
        }
        return null;
    }

    private static void attachSubCommandToParentCommand(CommandTreeNode commandRoot, String parentOverrideKey, LiteralArgumentBuilder<FabricClientCommandSource> defaultParentCommand, LiteralArgumentBuilder<FabricClientCommandSource> subCommand) {
        if (parentOverrideKey.isEmpty())
            defaultParentCommand.then(subCommand);
        else {
            LiteralArgumentBuilder<FabricClientCommandSource> parentCommand = ComponentUtility.getCommandOrElse(commandRoot, parentOverrideKey, (key) -> ClientCommandManager.literal(parentOverrideKey));
            parentCommand.then(subCommand);
        }
    }

    /**
     * Build commands from fields with the {@link CommandOption} in an object's class.
     */
    private static List<Pair<CommandOption, LiteralArgumentBuilder<FabricClientCommandSource>>> buildCommandsFromFields(ComponentDto component, CommandRegistryAccess commandRegistryAccess, HashMap<Class, SupportedCommandArgumentType> supportedActionArgumentTypes, boolean isDebug) {
        Class<?> clazz = component.clazz;

        List<Pair<CommandOption, LiteralArgumentBuilder<FabricClientCommandSource>>> commands = new ArrayList<>();
        for (Field field : ComponentUtility.getFieldsWithAnnotation(clazz, CommandOption.class, (field) -> (!field.getAnnotation(CommandOption.class).debug() || isDebug))) {
            Class<?> fieldClass = field.getType();
            CommandOption optionAnnotation = field.getAnnotation(CommandOption.class);
            String optionKey = optionAnnotation.value().isEmpty() ? ComponentUtility.convertDeclarationToCamel(field.getName()) : optionAnnotation.value();

            // Setter method inside our component class, as specified by the annotation.
            final Method setterMethod;
            if (!optionAnnotation.setter().isEmpty()) {
                try {
                    setterMethod = clazz.getMethod(optionAnnotation.setter(), fieldClass);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else setterMethod = null;

            // Getter method inside our component class, as specified by the annotation.
            final Method getterMethod;
            if (!optionAnnotation.getter().isEmpty()) {
                try {
                    getterMethod = clazz.getMethod(optionAnnotation.getter());
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else getterMethod = null;

            ArgumentType<?> argumentType = null;
            SuggestionProvider<FabricClientCommandSource> suggestionProvider = null;
            final GetCommandArgumentValue getCommandArgumentValue;
            final SetNewFieldValue<FabricClientCommandSource> setNewFieldValue;

            // If not read-only, set up our command arguments.
            if (!optionAnnotation.readOnly()) {
                if (supportedActionArgumentTypes.containsKey(field.getType())) {
                    // Generic case :3 using our lovely SupportedCommandArgumentType infrastructure
                    SupportedCommandArgumentType supportedCommandArgumentType = supportedActionArgumentTypes.get(field.getType());
                    argumentType = supportedCommandArgumentType.getArgumentType.apply(commandRegistryAccess);
                    getCommandArgumentValue = supportedCommandArgumentType.getArgumentValue();
                } else if (fieldClass.isEnum()) {
                    // We can't check if our types are enums by comparing the hashed Class to Enum.class, unfortunately.
                    // So enums are a special case and are handled explicitly.
                    argumentType = StringArgumentType.string();
                    suggestionProvider = ComponentUtility.getSuggestionProviderForEnum(fieldClass);
                    getCommandArgumentValue = (context, key) -> ComponentUtility.getEnumValueFromCommandArgument(context, key, fieldClass);
                } else {
                    // We messed up!
                    getCommandArgumentValue = null;
                }
            } else {
                getCommandArgumentValue = null;
            }

            // Set the value of the field in the component instance via reflection, If a setter method is specified, use that instead.
            setNewFieldValue = (context, value) -> {
                try {
                    if (setterMethod != null) {
                        setterMethod.invoke(component.instance, value);
                    } else if (!optionAnnotation.readOnly()) {
                        field.setAccessible(true);
                        field.set(component.instance, value);
                    }

                    if (field.isAnnotationPresent(Persists.class) && component.instance instanceof IFileProvider fileProvider) {
                        PersistenceManager.save(fileProvider);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            };

            // Get the value of the field from the component instance via reflection. If a getter method is specified, use that instead.
            final GetFieldValue getFieldValue = context -> {
                try {
                    if (getterMethod != null)
                        return getterMethod.invoke(component.instance);
                    else {
                        field.setAccessible(true);
                        return field.get(component.instance);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            };

            // By default, print the value of the field when no specific option is provided.
            com.mojang.brigadier.Command<FabricClientCommandSource> noOptionProvidedFunc = (context -> {
                ComponentUtility.print(context, ComponentUtility.getDebugString(getFieldValue.run(component.instance), component.clazz));
                return 1;
            });

            if (field.getType() == boolean.class) {
                // If the option is a boolean, we want to toggle the value of the field when no specific option is provided.
                noOptionProvidedFunc = (context -> {
                    setNewFieldValue.set(context, !(boolean) getFieldValue.run(component.instance));
                    ComponentUtility.print(context, optionKey + "=" + getFieldValue.run(component.instance));
                    return 1;
                });
            }
            // Don't like this feature. Not useful in practice.
//            else if (field.getType().isEnum()) {
//                // If the option is an enum, increment the enum to the next value.
//                noOptionProvidedFunc = (context -> {
//                    try {
//                        setNewFieldValue.set(context, ComponentUtility.incrementEnum((Enum) getFieldValue.run(component.instance)));
//                        ComponentUtility.print(context, optionKey + "=" + getFieldValue.run(component.instance));
//                        return 1;
//                    } catch (IllegalAccessException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//            }


            LiteralArgumentBuilder<FabricClientCommandSource> subCommand = ClientCommandManager.literal(optionKey);
            subCommand = subCommand.executes(noOptionProvidedFunc);

            if (argumentType != null && getCommandArgumentValue != null && setNewFieldValue != null) {

                RequiredArgumentBuilder<FabricClientCommandSource, ?> argument = ClientCommandManager.argument(optionKey, argumentType);
                if (suggestionProvider != null)
                    argument = argument.suggests(suggestionProvider);

                argument.executes((context) -> {
                    setNewFieldValue.set(context, getCommandArgumentValue.get(context, optionKey));
                    return 1;
                });
                subCommand.then(argument);
            }

            commands.add(new Pair<>(optionAnnotation, subCommand));
        }
        return commands;
    }

    /**
     * Build commands from methods with the {@link CommandAction} in an object's class.
     */
    private static List<Pair<CommandAction, LiteralArgumentBuilder<FabricClientCommandSource>>> buildCommandsFromMethods(ComponentDto component, CommandRegistryAccess commandRegistryAccess, HashMap<Class, SupportedCommandArgumentType> supportedArgumentTypes, boolean isDebug) {
        Class<?> clazz = component.clazz;
        MinecraftClient client = MinecraftClient.getInstance();
        assert client != null;

        // Build command arguments
        List<Pair<CommandAction, LiteralArgumentBuilder<FabricClientCommandSource>>> commands = new ArrayList<>();
        for (Method method : ComponentUtility.getMethodsWithAnnotation(clazz, CommandAction.class, (field) -> !field.getAnnotation(CommandAction.class).debug() || isDebug)) {
            CommandAction actionAnnotation = method.getAnnotation(CommandAction.class);
            Parameter[] parameters = method.getParameters();
            String actionKey = actionAnnotation.value().isEmpty() ? ComponentUtility.convertDeclarationToCamel(method.getName()) : actionAnnotation.value();

            RequiredArgumentBuilder<FabricClientCommandSource, ?> commandArgument = null;
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Class argumentType = parameter.getType();
                String argumentName = parameter.getName();

                if (supportedArgumentTypes.containsKey(argumentType)) {
                    SupportedCommandArgumentType supportedCommandArgumentType = supportedArgumentTypes.get(argumentType);
                    commandArgument = ClientCommandManager.argument(argumentName, supportedCommandArgumentType.getArgumentType.apply(commandRegistryAccess));
                } else if (argumentType.isEnum()) {
                    commandArgument = ClientCommandManager.argument(argumentName, StringArgumentType.string()).suggests(ComponentUtility.getSuggestionProviderForEnum(argumentType));
                } else if (argumentType != MinecraftClient.class && argumentType != CommandContext.class) {
                    throw new RuntimeException("Unsupported argument type: " + argumentType.getName());
                }
            }

            // Build command execution function
            com.mojang.brigadier.Command<FabricClientCommandSource> action = (context) -> {
                try {
                    Object[] parameterInstances = new Object[parameters.length];
                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];

                        Class argType = parameter.getType();
                        String argName = parameter.getName();

                        if (parameter.getType() == MinecraftClient.class) {
                            parameterInstances[i] = client;
                        } else if (parameter.getType() == CommandContext.class) {
                            parameterInstances[i] = context;
                        } else if (supportedArgumentTypes.containsKey(argType)) {
                            parameterInstances[i] = supportedArgumentTypes.get(argType).getArgumentValue.get(context, argName);
                        } else if (argType.isEnum()) {
                            parameterInstances[i] = ComponentUtility.getEnumValueFromCommandArgument(context, argName, argType);
                        } else {
                            throw new Exception("Invalid parameters for subCommand action method: " + method.getName());
                        }
                    }

                    method.setAccessible(true);
                    method.invoke(component.instance, parameterInstances);
                    return 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            };

            // Build the command itself
            LiteralArgumentBuilder<FabricClientCommandSource> subCommand = ClientCommandManager.literal(actionKey);
            if (commandArgument != null)
                subCommand = subCommand.then(commandArgument.executes(action));
            else
                subCommand = subCommand.executes(action);

            commands.add(new Pair<>(actionAnnotation, subCommand));
        }
        return commands;
    }

    private void doClientTick(ClientTickMethodDto clientTickMethodDto) {
        if (tickCounter % clientTickMethodDto.tickRate == 0) {
            try {
                clientTickMethodDto.method.setAccessible(true);
                Parameter[] parameters = clientTickMethodDto.method.getParameters();
                if (parameters.length == 0)
                    clientTickMethodDto.method.invoke(clientTickMethodDto.instance);
                else if (parameters.length == 1 && parameters[0].getType() == MinecraftClient.class)
                    clientTickMethodDto.method.invoke(clientTickMethodDto.instance, MinecraftClient.getInstance());
                else
                    throw new Exception("Invalid parameters for client tick method: " + clientTickMethodDto.method.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setDebug(boolean isDebugEnabled) {
        this.isDebug = isDebugEnabled;
    }

    @Override
    public String toString() {
        if (commandTreeRoot != null)
            return this.getClass().getName() + ": " + commandTreeRoot.literal;
        else
            return super.toString();
    }
}
