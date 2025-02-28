package me.jtx.evobases.commands.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.jtx.evobases.EvoBases;
import me.jtx.evobases.commands.Command;
import me.jtx.evobases.commands.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.util.List;

/**
 * Command implementation for marking orders as completed.
 * Handles the completion process including:
 * - Updating order status
 * - Moving order to completed channel
 * - Creating base showcase post
 * - Sending completion notification to customer
 * Requires base designer role to execute.
 */
public class Completed extends Command {

    private final EvoBases bot;
    private static final String LINK_EMOJI = "U+1F517";

    /**
     * Constructs a new Completed command.
     * Sets up required command options for order completion.
     *
     * @param bot The EvoBases bot instance
     */
    public Completed(EvoBases bot) {
        super("completed", 
              Permission.UNKNOWN, 
              null, 
              "Mark an order as completed and process completion actions", 
              null);

        this.bot = bot;
        
        // Add required command options
        this.getSlashOptions().add(new OptionData(
            OptionType.INTEGER, 
            "orderid", 
            "The ID of the completed order", 
            true));
            
        this.getSlashOptions().add(new OptionData(
            OptionType.ATTACHMENT, 
            "image", 
            "The completed base image", 
            true));
            
        this.getSlashOptions().add(new OptionData(
            OptionType.STRING, 
            "baselink", 
            "Link to download the base", 
            true));
            
        this.bot.getCommandManager().register(this);
    }

    /**
     * Executes the completed command.
     * Processes the order completion workflow.
     *
     * @param ctx The command context
     */
    @Override
    public void execute(CommandContext ctx) {
        // Check if user has base designer role
        boolean hasBaseDesignerRole = ctx.getGuild().getMember(ctx.getUser()).getRoles().stream()
                .anyMatch(role -> role.getId().equals(bot.getBaseDesignerRoleId()));

        if (!hasBaseDesignerRole) {
            sendErrorMessage(ctx, "You do not have permission to complete orders!");
            return;
        }

        // Get command options
        int orderId = ctx.getSlashEvent().getOption("orderid").getAsInt();
        Message.Attachment image = ctx.getSlashEvent().getOption("image").getAsAttachment();
        String baseLink = ctx.getSlashEvent().getOption("baselink").getAsString();

        if (!bot.getOrderDetail().orderIdExists(orderId)) {
            ctx.getSlashEvent().reply("Order not found.").setEphemeral(true).queue();
            return;
        }

        // Process order completion
        String userId = bot.getOrderDetail().getUserIdByOrderId(orderId);
        TextChannel orderChannel = ctx.getSlashEvent().getJDA().getTextChannelById(bot.getOrderChannelId());
        
        // Mark order as completed
        bot.getOrderDetail().setCompleted(orderId);
        ctx.getSlashEvent().reply("Order #" + orderId + " completed successfully.").setEphemeral(true).queue();

        // Update and move original order message
        updateOriginalOrderMessage(ctx, orderId, orderChannel);

        // Create base showcase post
        createBaseShowcasePost(ctx, userId, image, baseLink);
    }

    /**
     * Sends an error message to the user.
     *
     * @param ctx The command context
     * @param errorMessage The error message to display
     */
    private void sendErrorMessage(CommandContext ctx, String errorMessage) {
        EmbedBuilder errorEmbed = new EmbedBuilder();
        errorEmbed.setTitle("Error")
                 .setDescription(errorMessage)
                 .setColor(Color.RED)
                 .setFooter(bot.getEmbedDetails().footer);

        ctx.getSlashEvent().replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
    }

    /**
     * Updates and moves the original order message to completed channel.
     *
     * @param ctx The command context
     * @param orderId The ID of the completed order
     * @param orderChannel The channel containing the original order
     */
    private void updateOriginalOrderMessage(CommandContext ctx, int orderId, TextChannel orderChannel) {
        String originalMessageId = bot.getOrderDetail().getMessageIdByOrderId(orderId);
        if (originalMessageId == null) return;

        orderChannel.retrieveMessageById(originalMessageId).queue(originalMessage -> {
            EmbedBuilder updatedEmbed = new EmbedBuilder(originalMessage.getEmbeds().get(0));
            updatedEmbed.clearFields();

            // Update completion status in embed fields
            originalMessage.getEmbeds().get(0).getFields().forEach(field -> {
                if (field.getName().equalsIgnoreCase("Completed")) {
                    updatedEmbed.addField("Completed", "true", true);
                } else {
                    updatedEmbed.addField(field);
                }
            });
            updatedEmbed.setColor(Color.GREEN);

            // Delete original and move to completed channel
            originalMessage.delete().queue();
            TextChannel completedChannel = ctx.getSlashEvent().getJDA().getTextChannelById("1286019263655706634");
            completedChannel.sendMessageEmbeds(updatedEmbed.build()).queue();
        });
    }

    /**
     * Creates a showcase post for the completed base.
     *
     * @param ctx The command context
     * @param userId The ID of the user who ordered the base
     * @param image The completed base image
     * @param baseLink The download link for the base
     */
    private void createBaseShowcasePost(CommandContext ctx, String userId, Message.Attachment image, String baseLink) {
        TextChannel showcaseChannel = ctx.getSlashEvent().getJDA().getTextChannelById(bot.getBaseShowcaseChannelId());
        
        showcaseChannel.sendMessage("Base for <@" + userId + "> \nDesigned by: " + ctx.getUser().getEffectiveName())
                .addFiles(FileUpload.fromData(image.getProxy().download().join(), image.getFileName()))
                .addActionRow(
                    Button.secondary("link:", "Link").withEmoji(Emoji.fromUnicode(LINK_EMOJI)),
                    Button.secondary("downloads:", "Downloads")
                )
                .queue(message -> {
                    // Save base details
                    JsonObject baseData = new JsonObject();
                    baseData.addProperty("messageId", message.getId());
                    baseData.addProperty("downloadCount", 0);
                    baseData.addProperty("baseLink", baseLink);
                    baseData.add("uniqueUsers", new JsonArray());

                    bot.getOrderEmbedDetails().addEmbedData(message.getId(), baseData);
                    bot.getOrderEmbedDetails().saveEmbedData();

                    // Send completion notification to customer
                    sendCompletionNotification(ctx, userId, message, showcaseChannel);
                });
    }

    /**
     * Sends a completion notification to the customer.
     *
     * @param ctx The command context
     * @param userId The ID of the user to notify
     * @param showcaseMessage The showcase message
     * @param showcaseChannel The channel containing the showcase
     */
    private void sendCompletionNotification(CommandContext ctx, String userId, Message showcaseMessage, TextChannel showcaseChannel) {
        String messageLink = String.format("https://discord.com/channels/%s/%s/%s",
                showcaseChannel.getGuild().getId(),
                showcaseChannel.getId(),
                showcaseMessage.getId());

        EmbedBuilder notificationEmbed = new EmbedBuilder();
        notificationEmbed.setTitle(bot.getOrderCompletedTitle())
                        .setDescription(bot.getOrderCompletedMessage().replace("%base-link%", messageLink))
                        .setColor(Color.decode(bot.getOrderCompletedEmbedColorHex()))
                        .setImage(bot.getOrderCompletedEmbedImage());

        User customer = ctx.getSlashEvent().getJDA().getUserById(userId);
        if (customer != null) {
            customer.openPrivateChannel()
                   .flatMap(channel -> channel.sendMessageEmbeds(notificationEmbed.build()))
                   .queue();
        }
    }

    /**
     * Creates an embed displaying the current queue status.
     *
     * @param ctx The command context
     * @return The queue status embed
     */
    private MessageEmbed createQueueEmbed(CommandContext ctx) {
        List<JsonObject> incompleteOrders = bot.getOrderDetail().getIncompleteOrders();
        EmbedBuilder embed = new EmbedBuilder();

        if (incompleteOrders.isEmpty()) {
            return embed.setTitle("Current Queue")
                       .setColor(Color.WHITE)
                       .setDescription("The queue is currently empty.")
                       .setFooter(bot.getEmbedDetails().footer)
                       .build();
        }

        StringBuilder queueList = new StringBuilder();
        for (JsonObject order : incompleteOrders) {
            String userId = order.get("userId").getAsString();
            int queueNum = order.get("queueNum").getAsInt();
            User user = ctx.getUser().getJDA().getUserById(userId);

            queueList.append("**#").append(queueNum).append("** ")
                    .append("<@").append(userId).append("> [")
                    .append(user.getName()).append("]\n");
        }

        return embed.setTitle("Current Queue")
                   .setColor(Color.WHITE)
                   .setDescription(queueList.toString())
                   .setFooter(bot.getEmbedDetails().footer)
                   .build();
    }
}