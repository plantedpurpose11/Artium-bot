package me.jtx.evobases.commands;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import me.jtx.evobases.EvoBases;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;

/**
 * Manages all Discord slash commands for the EvoBases bot.
 * Handles command registration, initialization, and lookup.
 * Uses reflection to automatically discover and register command implementations.
 */
public class CommandManager {
    private final EvoBases bot;
    private final HashSet<Command> commands;

    /**
     * Constructs a new CommandManager for the specified bot instance.
     *
     * @param bot The EvoBases bot instance
     */
    public CommandManager(EvoBases bot) {
        this.bot = bot;
        this.commands = new HashSet<>();
    }

    /**
     * Initializes the command system by scanning for and registering all command implementations.
     * Uses reflection to automatically discover and instantiate command classes.
     * Command classes must be in the 'me.jtx.evobases.commands.impl' package and have a constructor
     * that takes an EvoBases instance as parameter.
     */
    public void initialize() {
        Class<?> type = EvoBases.class;
        try (ScanResult result = new ClassGraph()
                .acceptPackages("me.jtx.evobases.commands.impl")
                .scan()) {
            
            for (ClassInfo cls : result.getAllClasses()) {
                try {
                    Class<?> loadClass = cls.loadClass();
                    Constructor<?> cons = loadClass.getConstructor(type);
                    cons.newInstance(bot);
                } catch (NoSuchMethodException | SecurityException | InstantiationException 
                        | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    System.err.println("Failed to initialize command: " + cls.getSimpleName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Finds and returns a command by its name or alias.
     * The search is case-insensitive.
     *
     * @param name The name or alias of the command to find
     * @return The matching Command instance, or null if not found
     */
    public Command getCommand(String name) {
        for (Command command : bot.getCommandManager().getCommands()) {
            if (command.getName().equalsIgnoreCase(name)) {
                return command;
            }
            
            String[] aliases = command.getAliases();
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias.equalsIgnoreCase(name)) {
                        return command;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Registers a new command with the manager.
     *
     * @param command The command to register
     */
    public void register(Command command) {
        this.commands.add(command);
    }

    /**
     * Gets the EvoBases bot instance associated with this command manager.
     *
     * @return The EvoBases bot instance
     */
    public EvoBases getBot() {
        return bot;
    }

    /**
     * Gets all registered commands.
     *
     * @return A HashSet containing all registered commands
     */
    public HashSet<Command> getCommands() {
        return commands;
    }
}
