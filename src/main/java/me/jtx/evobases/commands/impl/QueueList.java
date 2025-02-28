package me.jtx.evobases.commands.impl;

import me.jtx.evobases.EvoBases;
import me.jtx.evobases.commands.Command;
import me.jtx.evobases.commands.CommandContext;
import me.jtx.evobases.utils.Msg;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Command implementation for viewing the queue list.
 * Supports viewing the full queue with pagination or checking a specific user's position.
 * Provides interactive navigation through pages using buttons.
 */
public class QueueList extends Command {

    private final EvoBases bot;
    private final ConcurrentMap<String, Integer> pageStates;
    private static final int ITEMS_PER_PAGE = 10;

    /**
     * Constructs a new QueueList command.
     * Sets up the command with options for mentioning specific users.
     *
     * @param bot The EvoBases bot instance
     */
    public QueueList(EvoBases bot) {
        super("queue", 
              Permission.UNKNOWN, 
              null, 
              "View the current queue list or check a specific user's position", 
              null);

        this.bot = bot;
        this.pageStates = new ConcurrentHashMap<>();
        
        // Add option for mentioning a specific user
        this.getSlashOptions().add(new OptionData(
            OptionType.MENTIONABLE, 
            "usertag", 
            "View the user's queue place", 
            false));
            
        this.bot.getCommandManager().register(this);
    }

    /**
     * Executes the queue list command.
     * Handles both full queue view and specific user lookup.
     *
     * @param ctx The command context
     */
    @Override
    public void execute(CommandContext ctx) {
        List<JsonObject> incompleteOrders = bot.getOrderDetail().getIncompleteOrders();

        if (incompleteOrders.isEmpty()) {
            ctx.getSlashEvent().reply("The queue is currently empty.").setEphemeral(true).queue();
            return;
        }

        User mentionedUser = ctx.getSlashEvent().getOption("usertag") != null 
            ? ctx.getSlashEvent().getOption("usertag").getAsUser() 
            : null;
            
        if (mentionedUser != null) {
            handleUserQueue(ctx, incompleteOrders, mentionedUser);
        } else {
            handleFullQueue(ctx, incompleteOrders, 1);
        }
    }

    /**
     * Handles displaying queue information for a specific user.
     *
     * @param ctx The command context
     * @param incompleteOrders List of incomplete orders
     * @param mentionedUser The user to look up
     */
    private void handleUserQueue(CommandContext ctx, List<JsonObject> incompleteOrders, User mentionedUser) {
        StringBuilder queueInfo = new StringBuilder();

        // Find all orders for the mentioned user
        for (JsonObject order : incompleteOrders) {
            String userId = order.get("userId").getAsString();
            if (userId.equals(mentionedUser.getId())) {
                int queueNum = order.get("queueNum").getAsInt();
                queueInfo.append("**#").append(queueNum).append("** ")
                        .append("<@").append(userId).append("> [")
                        .append(mentionedUser.getName()).append("]\n");
            }
        }

        if (queueInfo.isEmpty()) {
            ctx.getSlashEvent().reply("The mentioned user has no orders in the queue.")
                .setEphemeral(true)
                .queue();
        } else {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(mentionedUser.getEffectiveName() + "'s Current Queue")
                 .setColor(Color.WHITE)
                 .setDescription(queueInfo.toString())
                 .setFooter(bot.getEmbedDetails().footer);
                 
            ctx.getSlashEvent().replyEmbeds(embed.build()).queue();
        }
    }

    /**
     * Handles displaying a page of the full queue.
     *
     * @param ctx The command context
     * @param incompleteOrders List of incomplete orders
     * @param pageNumber The page number to display
     */
    public void handleFullQueue(CommandContext ctx, List<JsonObject> incompleteOrders, int pageNumber) {
        int totalPages = (int) Math.ceil((double) incompleteOrders.size() / ITEMS_PER_PAGE);
        int startIndex = (pageNumber - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, incompleteOrders.size());

        if (startIndex < 0 || endIndex > incompleteOrders.size() || startIndex >= endIndex) {
            ctx.reply("Invalid page number. Please run /queue to start from the beginning.");
            return;
        }

        // Build the queue list for the current page
        StringBuilder queueList = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            JsonObject order = incompleteOrders.get(i);
            String userId = order.get("userId").getAsString();
            int queueNum = order.get("queueNum").getAsInt();
            User user = ctx.getJDA().retrieveUserById(userId).complete();
            
            if (user != null) {
                queueList.append("**#").append(queueNum).append("** ")
                        .append("<@").append(userId).append("> [")
                        .append(user.getName()).append("]\n");
            }
        }

        // Create and configure the embed
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Current Queue - Page " + pageNumber + "/" + totalPages)
             .setColor(Color.WHITE)
             .setDescription(queueList.toString())
             .setFooter(bot.getEmbedDetails().footer);

        // Create navigation buttons
        Button prevButton = Button.primary("prev", "Previous Page").withDisabled(pageNumber == 1);
        Button nextButton = Button.primary("next", "Next Page").withDisabled(pageNumber == totalPages);

        // Send or update the message based on interaction type
        if (ctx.isSlash()) {
            ctx.getSlashEvent().replyEmbeds(embed.build())
               .addActionRow(prevButton, nextButton)
               .queue();
        } else if (ctx.getButtonEvent() != null) {
            ctx.getButtonEvent().editMessageEmbeds(embed.build())
               .setActionRow(prevButton, nextButton)
               .queue();
        }

        // Store the current page state for this user
        pageStates.put(ctx.getUser().getId(), pageNumber);
    }

    /**
     * Gets the current page state for a user.
     *
     * @param userId The ID of the user
     * @return The current page number, defaulting to 1 if not set
     */
    public int getPageState(String userId) {
        return pageStates.getOrDefault(userId, 1);
    }
}
