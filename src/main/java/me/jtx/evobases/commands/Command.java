package me.jtx.evobases.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.HashSet;

/**
 * Base class for all Discord slash commands in the EvoBases bot.
 * Provides core functionality for command handling, including permissions,
 * aliases, and slash command options.
 */
public class Command {
    private final String name;
    private final Permission requiredPermission;
    private final String[] aliases;
    private final String description;
    private final HashSet<OptionData> slashOptions;
    private final int cooldown;

    /**
     * Constructs a new Command with the specified parameters.
     *
     * @param name               The primary name of the command
     * @param requiredPermission The permission required to use this command
     * @param aliases           Alternative names for the command
     * @param description       Brief description of what the command does
     * @param options           Set of options for the slash command
     */
    public Command(String name, Permission requiredPermission, String[] aliases, String description, HashSet<OptionData> options) {
        this.name = name;
        this.requiredPermission = requiredPermission;
        this.aliases = aliases;
        this.description = description;
        this.slashOptions = options == null ? new HashSet<>() : options;
        this.cooldown = 0;
    }

    /**
     * Converts this command into a set of JDA CommandData objects.
     * Creates command data for both the primary name and all aliases.
     *
     * @return A HashSet of CommandData objects representing this command and its aliases
     */
    public HashSet<CommandData> toCommand() {
        HashSet<CommandData> commandData = new HashSet<>();
        if (aliases != null && aliases.length > 0) {
            for (String alias : aliases) {
                commandData.add(slashOptions.isEmpty() 
                    ? Commands.slash(alias, description) 
                    : Commands.slash(alias, description).addOptions(slashOptions));
            }
        }
        commandData.add(slashOptions.isEmpty() 
            ? Commands.slash(name, description) 
            : Commands.slash(name, description).addOptions(slashOptions));
        return commandData;
    }

    /**
     * Executes the command logic.
     * This method should be overridden by specific command implementations.
     *
     * @param ctx The command context containing execution details
     */
    public void execute(CommandContext ctx) {
        // Default implementation - to be overridden by specific commands
    }

    /**
     * Gets the slash command options for this command.
     *
     * @return The set of option data for this command
     */
    public HashSet<OptionData> getSlashOptions() {
        return slashOptions;
    }

    /**
     * Gets the required permission to use this command.
     *
     * @return The permission required to execute this command
     */
    public Permission getRequiredPermission() {
        return requiredPermission;
    }

    /**
     * Gets the primary name of this command.
     *
     * @return The command's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the alternative names (aliases) for this command.
     *
     * @return Array of command aliases
     */
    public String[] getAliases() {
        return aliases;
    }
}
