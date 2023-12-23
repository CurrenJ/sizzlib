package grill24.sizzlib.component;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import grill24.sizzlib.persistence.PersistenceManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

public class ComponentUtility {

    public static boolean hasCustomClassAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return clazz.isAnnotationPresent(annotationClass);
    }

    public static String convertSnakeToCamel(String snakeCase) {
        StringBuilder camelCase = new StringBuilder();
        boolean capitalizeNext = false;

        for (char character : snakeCase.toLowerCase().toCharArray()) {
            if (character == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    camelCase.append(Character.toUpperCase(character));
                    capitalizeNext = false;
                } else {
                    camelCase.append(character);
                }
            }
        }

        return camelCase.toString();
    }

    public static String convertCamelToSnake(String camelCase) {
        StringBuilder snakeCase = new StringBuilder();

        for (char character : camelCase.toCharArray()) {
            if (Character.isUpperCase(character)) {
                snakeCase.append('_');
                snakeCase.append(Character.toLowerCase(character));
            } else {
                snakeCase.append(character);
            }
        }

        return snakeCase.toString();
    }

    public static String convertDeclarationToCamel(String declaration) {
        return Character.toLowerCase(declaration.charAt(0)) + declaration.substring(1);
    }

    public static Method[] getScreenTickMethods(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(ScreenTick.class) && hasCorrectScreenParameterSignature(method))
                .toArray(Method[]::new);
    }

    public static Method[] getScreenInitMethods(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(ScreenInit.class) && hasCorrectScreenParameterSignature(method))
                .toArray(Method[]::new);
    }

    public static Method getStaticToStringMethod(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(StaticToString.class))
                .findFirst()
                .orElse(null);
    }

    public static String toStringStatic(Class<?> clazz) {
        Method method = getStaticToStringMethod(clazz);
        if (method != null) {
            try {
                return method.invoke(null).toString();
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    private static boolean hasCorrectScreenParameterSignature(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        boolean correctParameters = parameterTypes.length == 0
                || (parameterTypes.length == 1 && parameterTypes[0] == MinecraftClient.class)
                || (parameterTypes.length == 1 && parameterTypes[0] == Screen.class)
                || (parameterTypes.length == 2 && parameterTypes[0] == MinecraftClient.class && parameterTypes[1] == Screen.class);
        if (!correctParameters)
            System.out.println("WARNING: Incorrect parameters for screen tick method: " + method.getName());
        return correctParameters;
    }

    public static Field[] getFieldsWithAnnotation(Class<?> componentClass, Class<? extends Annotation> annotationClass) {
        return getFieldsWithAnnotation(componentClass, annotationClass, (field) -> true);
    }

    public static Field[] getFieldsWithAnnotation(Class<?> componentClass, Class<? extends Annotation> annotationClass, Function<Field, Boolean> isValidField) {
        Field[] fields = componentClass.getDeclaredFields();
        return Arrays.stream(fields)
                .filter(field -> field.isAnnotationPresent(annotationClass) && isValidField.apply(field))
                .toArray(Field[]::new);
    }

    public static Method[] getMethodsWithAnnotation(Class<?> componentClass, Class<? extends Annotation> annotationClass) {
        return getMethodsWithAnnotation(componentClass, annotationClass, (method) -> true);
    }

    public static Method[] getMethodsWithAnnotation(Class<?> componentClass, Class<? extends Annotation> annotationClass, Function<Method, Boolean> isValidMethodSignature) {
        Method[] methods = componentClass.getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(annotationClass) && isValidMethodSignature.apply(method))
                .toArray(Method[]::new);
    }

    public static Method[] getClientTickMethods(Class<?> componentClass) {
        return getMethodsWithAnnotation(componentClass, ClientTick.class, ComponentUtility::hasCorrectClientTickParameterSignature);
    }

    private static boolean hasCorrectClientTickParameterSignature(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        boolean correctParameters = parameterTypes.length == 0
                || (parameterTypes.length == 1 && parameterTypes[0] == MinecraftClient.class);
        if (!correctParameters)
            System.out.println("WARNING: Incorrect parameters for client tick method: " + method.getName());
        return correctParameters;
    }

    public static String getCommandKey(Class<?> clazz) {
        if (hasCustomClassAnnotation(clazz, Command.class)) {
            Command annotation = clazz.getAnnotation(Command.class);
            return annotation.value().isEmpty() ? ComponentUtility.convertDeclarationToCamel(clazz.getSimpleName()) : annotation.value();
        }
        return "";
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> getCommandOrElse(ModComponentRegistry.CommandTreeNode commandTreeRoot, String commandKey, LiteralArgumentBuilderSupplier value) {
        LiteralArgumentBuilder<FabricClientCommandSource> command;
        if (commandTreeRoot != null && commandTreeRoot.getChildNode(commandKey).isPresent())
            return commandTreeRoot.getChildNode(commandKey).get().command;
        else {
            LiteralArgumentBuilder<FabricClientCommandSource> newCommand = value.run(commandKey);
            if (commandTreeRoot != null) {
                ModComponentRegistry.CommandTreeNode node = new ModComponentRegistry.CommandTreeNode(commandKey, newCommand);
                commandTreeRoot.children.put(commandKey, node);
            }
            return newCommand;
        }
    }

    public static void print(CommandContext<?> commandContext, String message) {
        if (commandContext.getSource() instanceof FabricClientCommandSource) {
            ((FabricClientCommandSource) commandContext.getSource()).sendFeedback(Text.literal(message));
        }
    }

    public static void print(MinecraftClient client, String message) {
        client.inGameHud.getChatHud().addMessage(Text.of(message));
    }

    public static Object getEnumValueFromCommandArgument(CommandContext commandContext, String argKey, Class enumClass) {
        return Enum.valueOf((Class<Enum>) enumClass, ComponentUtility.convertCamelToSnake((String) commandContext.getArgument(argKey, String.class)).toUpperCase());
    }

    public static Object getEnumValueFromSerializedString(String str, Class enumClass) {
        return Enum.valueOf((Class<Enum>) enumClass, str);
    }

    public static SuggestionProvider getSuggestionProviderForEnum(Class enumClass) {
        return (SuggestionProvider<FabricClientCommandSource>) (context, builder) -> {
            for (Object enumConstant : enumClass.getEnumConstants()) {
                builder.suggest(ComponentUtility.convertSnakeToCamel(enumConstant.toString()));
            }
            return builder.buildFuture();
        };
    }

    public static <T extends Enum<T>> T incrementEnum(T inputEnum) throws IllegalAccessException {
        if (inputEnum == null) {
            throw new IllegalAccessException("Iput enum cannot be null.");
        }

        T[] enumConstants = inputEnum.getDeclaringClass().getEnumConstants();
        int currentIndex = inputEnum.ordinal();
        int nextIndex = (currentIndex + 1) % enumConstants.length;


        return enumConstants[nextIndex];
    }

    public static String getDebugString(Object obj, Class<?> clazz) {
        String str = "";
        try {
            if (obj != null) {
                str = PersistenceManager.toJson(obj, clazz).toString();
            }

            if (str.isEmpty() || str.equals("{}")) {
                str = ComponentUtility.toStringStatic(clazz);
            }

            if ((str.isEmpty() || str.equals("{}")) && obj != null) {
                str = obj.toString();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return str;
    }
}
