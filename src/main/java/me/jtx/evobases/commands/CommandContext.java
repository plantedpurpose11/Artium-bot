package me.jtx.evobases.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Collections;

/**
 * Represents the context in which a command is executed.
 * Provides access to Discord entities and utilities needed for command execution.
 * Supports both slash commands and button interactions.
 */
public class CommandContext {
    private final Guild guild;
    private final TextChannel textChannel;
    private final JDA jda;
    private final User user;
    private final Member member;
    private boolean isSlash;
    private SlashCommandInteractionEvent slashEvent;
    private ButtonInteractionEvent buttonEvent;
    private String[] args = new String[]{};

    /**
     * Constructs a new CommandContext with the specified Discord entities.
     *
     * @param guild The guild where the command was executed
     * @param textChannel The text channel where the command was executed
     * @param jda The JDA instance
     * @param user The user who executed the command
     * @param member The guild member who executed the command
     */
    public CommandContext(Guild guild, TextChannel textChannel, JDA jda, User user, Member member) {
        this.guild = guild;
        this.textChannel = textChannel;
        this.jda = jda;
        this.user = user;
        this.member = member;
        this.isSlash = false;
        this.slashEvent = null;
        this.buttonEvent = null;
    }

    /**
     * Creates a CommandContext from a slash command interaction.
     *
     * @param e The slash command interaction event
     * @return A new CommandContext configured for slash command handling
     */
    public static CommandContext fromSlash(final SlashCommandInteractionEvent e) {
        CommandContext ctx = new CommandContext(e.getGuild(), e.getChannel().asTextChannel(), e.getJDA(), e.getUser(), e.getMember());
        ctx.slashEvent = e;
        ctx.isSlash = true;
        return ctx;
    }

    /**
     * Creates a CommandContext from a button interaction.
     *
     * @param e The button interaction event
     * @return A new CommandContext configured for button interaction handling
     */
    public static CommandContext fromButton(final ButtonInteractionEvent e) {
        CommandContext ctx = new CommandContext(e.getGuild(), e.getChannel().asTextChannel(), e.getJDA(), e.getUser(), e.getMember());
        ctx.buttonEvent = e;
        ctx.isSlash = false;
        return ctx;
    }

    /**
     * Replies to the command with a text message.
     *
     * @param content The text content to send
     * @return A RestAction representing the reply operation
     */
    public RestAction<?> reply(String content) {
        if (this.isSlash) {
            return this.slashEvent.reply(content).setAllowedMentions(Collections.emptySet());
        } else {
            return this.textChannel.sendMessage(content).setAllowedMentions(Collections.emptySet());
        }
    }

    /**
     * Replies to the command with a message create data.
     *
     * @param messageData The message data to send
     * @return A RestAction representing the reply operation
     */
    public RestAction<?> reply(MessageCreateData messageData) {
        if (this.isSlash) {
            return this.slashEvent.reply(messageData).setAllowedMentions(Collections.emptySet());
        } else {
            return this.textChannel.sendMessage(messageData).setAllowedMentions(Collections.emptySet());
        }
    }

    /**
     * Replies to the command with an embed message.
     *
     * @param embed The embed to send
     * @return A RestAction representing the reply operation
     */
    public RestAction<?> reply(MessageEmbed embed) {
        if (this.isSlash) {
            return this.slashEvent.replyEmbeds(embed);
        } else {
            return this.textChannel.sendMessageEmbeds(embed);
        }
    }

    /**
     * Checks if a specific option is present in the command input.
     *
     * @param input The option name to check
     * @return true if the option exists and has a value, false otherwise
     */
    public boolean hasInput(String input) {
        if (isSlash) {
            for (OptionMapping option : slashEvent.getOptions()) {
                if (option.getName().equalsIgnoreCase(input)) {
                    return slashEvent.getOption(input) != null;
                }
            }
        }
        return false;
    }

    /**
     * Gets the text value of a command option.
     *
     * @param optionName The name of the option
     * @return The text value of the option, or null if not found
     */
    public String textInput(String optionName) {
        if (isSlash) {
            for (OptionMapping option : slashEvent.getOptions()) {
                if (option.getName().equalsIgnoreCase(optionName)) {
                    OptionMapping optionValue = slashEvent.getOption(optionName);
                    return optionValue != null ? optionValue.getAsString() : null;
                }
            }
        }
        return null;
    }

    /**
     * Gets the integer value of a command option.
     *
     * @param optionName The name of the option
     * @return The integer value of the option, or -1 if not found
     */
    public float intInput(String optionName) {
        if (isSlash) {
            for (OptionMapping option : slashEvent.getOptions()) {
                if (option.getName().equalsIgnoreCase(optionName)) {
                    OptionMapping optionValue = slashEvent.getOption(optionName);
                    return optionValue != null ? (int) optionValue.getAsLong() : -1;
                }
            }
        }
        return -1;
    }

    /**
     * Gets all command arguments as a single string.
     *
     * @return The full command arguments string, or null if no arguments
     */
    public String fullArgs() {
        if (getArgs().length == 0) return null;
        StringBuilder builder = new StringBuilder(getArgs()[0]);
        for (int i = 1; i < args.length; i++) {
            builder.append(" ").append(getArgs()[i]);
        }
        return builder.toString();
    }

    /**
     * Gets either the command arguments or a specific option value.
     *
     * @param optionName The name of the option to get if in slash command mode
     * @return The arguments or option value, or null if not found
     */
    public String argsOrOption(String optionName) {
        if (!isSlash) {
            return fullArgs();
        }
        for (OptionMapping option : slashEvent.getOptions()) {
            if (option.getName().equalsIgnoreCase(optionName)) {
                OptionMapping optionValue = slashEvent.getOption(optionName);
                return optionValue != null ? optionValue.getAsString() : null;
            }
        }
        return null;
    }

    // Getters and setters with JavaDoc

    /**
     * Gets the guild where the command was executed.
     * @return The guild instance
     */
    public Guild getGuild() {
        return guild;
    }

    /**
     * Gets the command arguments array.
     * @return Array of command arguments
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * Sets the command arguments array.
     * @param args The new arguments array
     */
    public void setArgs(String[] args) {
        this.args = args;
    }

    /**
     * Gets the original message that triggered the command.
     * @return The message instance, or null if not applicable
     */
    public Message getMessage() {
        return null;
    }

    /**
     * Gets the slash command event if this is a slash command.
     * @return The slash command event, or null if not a slash command
     */
    public SlashCommandInteractionEvent getSlashEvent() {
        return slashEvent;
    }

    /**
     * Sets the slash command event.
     * @param slashEvent The new slash command event
     */
    public void setSlashEvent(SlashCommandInteractionEvent slashEvent) {
        this.slashEvent = slashEvent;
    }

    /**
     * Checks if this is a slash command context.
     * @return true if this is a slash command, false otherwise
     */
    public boolean isSlash() {
        return isSlash;
    }

    /**
     * Sets whether this is a slash command context.
     * @param slash true if this is a slash command, false otherwise
     */
    public void setSlash(boolean slash) {
        isSlash = slash;
    }

    /**
     * Gets the user who executed the command.
     * @return The user instance
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the JDA instance.
     * @return The JDA instance
     */
    public JDA getJDA() {
        return jda;
    }

    /**
     * Gets the text channel where the command was executed.
     * @return The text channel instance
     */
    public TextChannel getTextChannel() {
        return textChannel;
    }

    /**
     * Gets the guild member who executed the command.
     * @return The member instance
     */
    public Member getMember() {
        return member;
    }

    /**
     * Gets the button interaction event if this is a button interaction.
     * @return The button event, or null if not a button interaction
     */
    public ButtonInteractionEvent getButtonEvent() {
        return buttonEvent;
    }

    /**
     * Sets the button interaction event.
     * @param buttonEvent The new button event
     */
    public void setButtonEvent(ButtonInteractionEvent buttonEvent) {
        this.buttonEvent = buttonEvent;
    }
}
