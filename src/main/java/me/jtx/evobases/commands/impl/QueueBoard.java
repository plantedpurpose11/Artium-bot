package me.jtx.evobases.commands.impl;

import com.google.gson.JsonObject;
import me.jtx.evobases.EvoBases;
import me.jtx.evobases.commands.Command;
import me.jtx.evobases.commands.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class QueueBoard extends Command {

    private final EvoBases bot;
    private final ConcurrentMap<String, Integer> pageStates;
    private static final int ITEMS_PER_PAGE = 10;


    public QueueBoard(EvoBases bot) {
        super("queueboard", 
              Permission.ADMINISTRATOR, 
              null, 
              "Display and manage the queue board with incomplete orders", 
              null);

        this.bot = bot;
        this.pageStates = new ConcurrentHashMap<>();
        //this.bot.getCommandManager().register(this);
    }


    @Override
    public void execute(CommandContext ctx) {
        List<JsonObject> incompleteOrders = bot.getOrderDetail().getIncompleteOrders();

        if (incompleteOrders.isEmpty()) {
            ctx.getSlashEvent().reply("The queue is currently empty.").setEphemeral(true).queue();
            return;
        }

        //handleFullQueue(ctx, incompleteOrders, 1);
    }


//    public void handleFullQueue(CommandContext ctx, List<JsonObject> incompleteOrders, int pageNumber) {
//        int totalPages = (int) Math.ceil((double) incompleteOrders.size() / ITEMS_PER_PAGE);
//        int startIndex = (pageNumber - 1) * ITEMS_PER_PAGE;
//        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, incompleteOrders.size());
//
//        // Build the queue list for the current page
//        StringBuilder queueList = new StringBuilder();
//        for (int i = startIndex; i < endIndex; i++) {
//            JsonObject order = incompleteOrders.get(i);
//            String userId = order.get("userId").getAsString();
//            int queueNum = order.get("queueNum").getAsInt();
//            User user = ctx.getJDA().getUserById(userId);
//
//            if (user != null) {
//                queueList.append("**#").append(queueNum).append("** ")
//                        .append("<@").append(userId).append("> [")
//                        .append(user.getName()).append("]\n");
//            }
//        }
//
//        // Create and configure the embed
//        EmbedBuilder embed = new EmbedBuilder();
//        embed.setTitle("Current Queue - Page " + pageNumber + "/" + totalPages)
//             .setColor(Color.WHITE)
//             .setDescription(queueList.toString())
//             .setFooter(bot.getEmbedDetails().footer);
//
//        // Create navigation buttons
//        Button prevButton = Button.primary("prev", "Previous Page").withDisabled(pageNumber == 1);
//        Button nextButton = Button.primary("next", "Next Page").withDisabled(pageNumber == totalPages);
//
//        // Send or update the message based on interaction type
//        if (ctx.isSlash()) {
//            ctx.getSlashEvent().replyEmbeds(embed.build())
//               .addActionRow(prevButton, nextButton)
//               .queue();
//        } else if (ctx.getButtonEvent() != null) {
//            ctx.getButtonEvent().editMessageEmbeds(embed.build())
//               .setActionRow(prevButton, nextButton)
//               .queue();
//        }
//
//        // Store the current page state for this user
//        pageStates.put(ctx.getUser().getId(), pageNumber);
//    }
//
//
//    public int getPageState(String userId) {
//        return pageStates.getOrDefault(userId, 1);
//    }
//

//    public void updateQueueBoard() {
//        List<JsonObject> incompleteOrders = bot.getOrderDetail().getIncompleteOrders();
//        TextChannel queueChannel = bot.getJDA().getTextChannelById(bot.getQueueChannelId());
//
//        if (queueChannel != null) {
//            queueChannel.retrieveMessageById(bot.getQueueMessageId()).queue(message -> {
//                EmbedBuilder embed = new EmbedBuilder();
//                embed.setTitle("Current Queue")
//                     .setColor(Color.WHITE)
//                     .setDescription(buildQueueDescription(incompleteOrders))
//                     .setFooter(bot.getEmbedDetails().footer);
//
//                message.editMessageEmbeds(embed.build()).queue();
//            });
//        }
//    }
//

//    private String buildQueueDescription(List<JsonObject> incompleteOrders) {
//        StringBuilder queueList = new StringBuilder();
//
//        for (JsonObject order : incompleteOrders) {
//            String userId = order.get("userId").getAsString();
//            int queueNum = order.get("queueNum").getAsInt();
//            User user = bot.getJDA().getUserById(userId);
//
//            if (user != null) {
//                queueList.append("**#").append(queueNum).append("** ")
//                        .append("<@").append(userId).append("> [")
//                        .append(user.getName()).append("]\n");
//            }
//        }
//
//        return queueList.toString();
//    }
}
