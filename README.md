# SizzLib

SizzLib is a library designed to allow mod developers to easily create client-side only commands, features, and persistent data. The benefits of using this library are quick iteration speed while working on new features, and easy, world-associated local data persistence that functions even when connected to a vanilla server host.

## Developer Info

SizzLib uses annotations to denote methods and fields as part of commands or as data to be locally persisted. See the SizzLibClient and ExamplePersistable classes for practical examples of how the ModComponentRegistry works.

The save(...) and load(...) functions of PersistenceManager class can be invoked to serialize/deserialize an object to/from json and store it locally.


### Command System


@Command is a class-level annotation that, when the declaring class is registered with ModComponentRegistry, constructs a client-side brigadier command.

@CommandOption is a field annotation that constructs a sub-command allowing the user to read or modify the field, depending on how the annotation parameters are configured. It has support for parsing enums (and creating suggestions from the possible values), as well as a few of Minecraft's proprietary types, such as ItemStack and BlockPos.

@CommandAction is a method annotation that constructs a sub-command that executes the method. Supported types that are parameters in the method signature will be passed in when executing the method.


### Tick System

@ClientTick, @ScreenTick, @ScreenInit, are method annotations that hook into their respective registry events for convenience's sake.

@ClientTick executes at the end of every game-tick.

@ScreenTick executes at the end of every screen-tick. The Screen object will be passed as a parameter if it is called for in the annotated method's signature.

@ScreenInit executes on the initialization of a screen. For example, this would be called every time a container is opened.
