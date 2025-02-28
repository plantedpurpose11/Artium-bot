package me.jtx.evobases.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.jtx.evobases.EvoBases;
import me.jtx.evobases.commands.Command;
import me.jtx.evobases.commands.CommandContext;
import me.jtx.evobases.commands.impl.QueueList;
import me.jtx.evobases.utils.Msg;
import me.jtx.evobases.utils.OrderEmbedDetails;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EventListener extends ListenerAdapter {

    private final EvoBases bot;

    public EventListener(final EvoBases bot) {
        this.bot = bot;
    }

    @Override
    public final void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
        for (Command command : bot.getCommandManager().getCommands()) {
            if (command.getName().equalsIgnoreCase(event.getName())) {
                handleSlashCommand(event, command);
                break;
            }
            if (command.getAliases() != null) {
                for (String alias : command.getAliases()) {
                    if (alias.equalsIgnoreCase(event.getName())) {
                        handleSlashCommand(event, command);
                    }
                }
            }
        }
    }

    private void handleSlashCommand(SlashCommandInteractionEvent event, Command command) {
        if (!event.getMember().hasPermission(command.getRequiredPermission())) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Error")
                    .setDescription("You do not have permission!")
                    .setColor(Color.RED)
                    .setFooter(bot.getEmbedDetails().footer);
            ;
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            return;
        }
        CommandContext ctx = CommandContext.fromSlash(event);
        command.execute(ctx);
    }

    @Override
    public final void onButtonInteraction(final ButtonInteractionEvent event) {
        if (event.getComponentId().equals("startOrderFormModal")) {
            TextInput nameInput = TextInput.create("name", "What do you want your base to say?", TextInputStyle.SHORT)
                    .setPlaceholder("Enter a message for your base (minimum 2 characters)")
                    .setRequired(true)
                    .build();

            TextInput townhallLevel = TextInput.create("townHallLevel", "TownHall Level", TextInputStyle.SHORT)
                    .setPlaceholder("Supports Town Halls " + bot.getGuildSettings().displaySupportedLevels())
                    .setRequired(true)
                    .build();


            TextInput baseStyleInput = TextInput.create("baseStyle", "Base Style", TextInputStyle.SHORT)
                    .setPlaceholder("Enter the base style (e.g., Standard, Cursive)")
                    .setRequired(true)
                    .build();

            TextInput ornamentation = TextInput.create("ornamentation", "Ornamentation", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Enter the ornamentation (e.g., crown on top, underline)")
                    .setRequired(false)
                    .build();


            TextInput additionalNotes = TextInput.create("additionalNotes", "Additional Notes", TextInputStyle.PARAGRAPH)
                    .setRequired(false)
                    .build();


            Modal modal = Modal.create("orderForm", "Base Order Form")
                    .addComponents(ActionRow.of(nameInput), ActionRow.of(townhallLevel), ActionRow.of(baseStyleInput), ActionRow.of(ornamentation), ActionRow.of(additionalNotes))
                    .build();

            event.replyModal(modal).queue();
        } else if (event.getComponentId().equals("startOrderModal")) {
            if (!event.getGuild().getMember(event.getUser()).getRoles().stream()
                    .anyMatch(role -> role.getId().equals(bot.getBaseDesignerRoleId()))) {
                EmbedBuilder noPermEB = new EmbedBuilder();
                noPermEB.setTitle("Error")
                        .setDescription("You do not have permission!")
                        .setColor(Color.RED)
                        .setFooter(bot.getEmbedDetails().footer);
                ;
                event.replyEmbeds(noPermEB.build()).setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();

            String messageId = event.getMessage().getId();
            String userId = bot.getOrderDetail().getUserIdByMessageId(messageId);

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(bot.getOrderStartedTitle());
            eb.setDescription(bot.getOrderStartedMessage());
            eb.setColor(Color.decode(bot.getOrderStartedColor()));
            if (event.getJDA().getUserById(userId) != null) {
                event.getJDA().getUserById(userId).openPrivateChannel().flatMap(privateChannel ->
                        privateChannel.sendMessageEmbeds(eb.build())).queue();
            }


            event.getChannel().retrieveMessageById(messageId).queue(message -> {
                MessageEmbed embed = message.getEmbeds().get(0);
                EmbedBuilder updatedEmbed = new EmbedBuilder(embed)
                        .addField("Claimed By", event.getMember().getAsMention(), false);

                message.editMessageEmbeds(updatedEmbed.build()).queue();
            });

            event.getMessage().editMessageComponents(ActionRow.of(Button.success("startOrderModal", "Start Order").asDisabled())).queue();
        } else if (event.getComponentId().equals("deleteOrderModal")) {
            TextInput deletedReason = TextInput.create("reason", "What's the reason for removing this order?", TextInputStyle.SHORT)
                    .setPlaceholder("Invalid base.")
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("deleteForm", "Removing Order Form")
                    .addComponents(ActionRow.of(deletedReason))
                    .build();

            event.replyModal(modal).queue();
        }

        CommandContext ctx = CommandContext.fromButton(event);
        QueueList queueList = (QueueList) bot.getCommandManager().getCommand("queue");
        if (queueList != null) {
            int currentPage = queueList.getPageState(event.getUser().getId());
            if (event.getComponentId().equals("next")) {
                queueList.handleFullQueue(ctx, bot.getOrderDetail().getIncompleteOrders(), currentPage + 1);
            } else if (event.getComponentId().equals("prev")) {
                queueList.handleFullQueue(ctx, bot.getOrderDetail().getIncompleteOrders(), currentPage - 1);
            }
        }

        String messageId = event.getMessageId();
        String userId = event.getUser().getId();

        OrderEmbedDetails orderEmbedDetails = bot.getOrderEmbedDetails();

        JsonObject data = orderEmbedDetails.getJsonObjectByMessageId(messageId);

        System.out.println(data);

        final JsonObject finalData = data;

        final JsonArray uniqueUsers = data.getAsJsonArray("uniqueUsers");


        if (event.getComponentId().equals("link:")) {
            boolean isNewUser = true;

            for (JsonElement userElement : uniqueUsers) {
                if (userElement.getAsString().equals(userId)) {
                    isNewUser = false;
                    break;
                }
            }

            if (isNewUser) {
                uniqueUsers.add(userId);
                int downloadCount = finalData.get("downloadCount").getAsInt();
                finalData.addProperty("downloadCount", downloadCount + 1);
            }

            event.reply(bot.getOrderEmbedDetails().getBaseLinkByMessageId(messageId)).setEphemeral(true).queue(interactionHook -> {
                event.getMessage().editMessageComponents(
                        ActionRow.of(
                                Button.secondary("link:", "Link").withEmoji(Emoji.fromUnicode("U+1F517")),
                                Button.secondary("downloads:", "Downloads (" + finalData.get("downloadCount").getAsInt() + ")"))
                ).queue();

                orderEmbedDetails.saveEmbedData();
            });


        } else if (event.getComponentId().equals("downloads:")) {
            event.deferReply(true).queue(interactionHook -> {
                String userList = StreamSupport.stream(uniqueUsers.spliterator(), false)
                        .map(JsonElement::getAsString)
                        .map(id -> "<@" + id + ">")
                        .collect(Collectors.joining(", "));

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.WHITE);
                eb.setTitle("Base Downloads:");
                eb.setDescription(userList);

                interactionHook.sendMessageEmbeds(eb.build()).queue();

                orderEmbedDetails.saveEmbedData();
            });
        }


    }



    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("orderForm")) {

            String userId = event.getUser().getId();
            boolean hasSpecialRole = event.getGuild().getMember(event.getUser()).getRoles().stream()
                    .anyMatch(role -> role.getId().equals(bot.getSpecialRoleId()));

            if (bot.getCooldown().isOnCooldown(userId) /*&& !hasSpecialRole*/){
                long remainingTime = bot.getCooldown().getRemainingCooldownTime(userId);
                String formattedTime = bot.getCooldown().formatCooldownTime(remainingTime);

                event.reply(bot.getCooldownMessage().replace("%time-left%", formattedTime)).setEphemeral(true).queue();

                return;
            }


            String name = event.getValue("name") != null ? event.getValue("name").getAsString() : null;
            String baseStyle = event.getValue("baseStyle") != null ? event.getValue("baseStyle").getAsString() : null;
            String townHallLevelString = event.getValue("townHallLevel") != null ? event.getValue("townHallLevel").getAsString() : null;
            String ornamentation = event.getValue("ornamentation") != null ? event.getValue("ornamentation").getAsString() : null;
            String additionalNotes = event.getValue("additionalNotes") != null ? event.getValue("additionalNotes").getAsString() : null;

            Set<String> validBaseStyles = bot.getGuildSettings().getBaseTypes();


            if (name == null || baseStyle == null || townHallLevelString == null) {
                event.reply("Please fill out all required fields.").setEphemeral(true).queue();
                return;
            }

            if (name.length() < 2) {
                event.reply("The base message must be at least 2 characters long.").setEphemeral(true).queue();
                return;
            }

            if (!validBaseStyles.contains(baseStyle.toLowerCase())) {

                event.reply("Invalid base style. Please enter one of the following: " + bot.getGuildSettings().displayBaseTypes() + ".").setEphemeral(true).queue();
                return;
            }

            int townHallLevel;
            try {
                townHallLevel = Integer.parseInt(townHallLevelString);
                if (!bot.getGuildSettings().isSupportedTownHall(townHallLevel)) {
                    event.reply("Unsupported Town Hall level. Please choose one of the following: " + bot.getGuildSettings().displaySupportedLevels() + ".").setEphemeral(true).queue();
                    return;
                }
            } catch (NumberFormatException e) {
                event.reply("Please enter a valid number for the Town Hall level.").setEphemeral(true).queue();
                return;
            }


            if (bot.getDailyOrderLimit().isResetTimePassed()) {
                bot.getDailyOrderLimit().resetDailyLimit(bot.getGlobal().todayDate());
                bot.getDailyOrderLimit().setResetTime(LocalDateTime.now().plusDays(1));
            }

            if (/*!hasSpecialRole &&*/ bot.getDailyOrderLimit().getCurrentOrderCount(bot.getGlobal().todayDate()) >= bot.getDailyOrderMaxLimit()) {
                EmbedBuilder limitReachedEB = new EmbedBuilder();
                limitReachedEB.setTitle("Error")
                        .setDescription("Sorry, the daily limit has been reached " + bot.getDailyOrderLimit().getCurrentOrderCount(bot.getGlobal().todayDate())
                                + "/" + (bot.getDailyOrderMaxLimit()) + ". The daily limit resets in " + bot.getDailyOrderLimit().getResetTime() + ".")
                        .setColor(Color.RED)
                        .setFooter(bot.getEmbedDetails().footer);
                event.replyEmbeds(limitReachedEB.build()).setEphemeral(true).queue();
                return;
            }

            int orderNum =  bot.getOrderDetail().getHighestOrderNum() + 1;

            EmbedBuilder eb = new EmbedBuilder();

            if (!hasSpecialRole) {
                eb.setTitle("Order #" + orderNum)
                        .addField("Name", name, true)
                        .addField("Base Style", baseStyle, true)
                        .addField("Town Hall Level", townHallLevelString, true)
                        .addField("Ornamentation", ornamentation, true)
                        .addField("Additional Notes", additionalNotes, true)
                        .addField("User", event.getUser().getAsMention(), true)
                        .addField("User ID", userId, true)
                        .addField("Completed", String.valueOf(bot.getOrderDetail().isCompleted()), true)
                        .setColor(Color.WHITE)
                        .setFooter(bot.getEmbedDetails().footer + " | " + event.getMessage().getId());
                    event.deferReply().setEphemeral(true).queue();

            } else {
                eb.setTitle("Order #" + orderNum)
                        .addField("Name", name, true)
                        .addField("Base Style", baseStyle, true)
                        .addField("Town Hall Level", townHallLevelString, true)
                        .addField("Ornamentation", ornamentation, true)
                        .addField("Additional Notes", additionalNotes, false)
                        .addField("User", event.getUser().getAsMention(), true)
                        .addField("User ID", userId, true)
                        .addField("Completed", String.valueOf(bot.getOrderDetail().isCompleted()), true)
                        .setColor(Color.YELLOW)
                        .setFooter(bot.getEmbedDetails().footer + " | " + event.getMessage().getId());
                event.deferReply().setEphemeral(true).queue();

            }


            event.getJDA().getTextChannelById(bot.getOrderChannelId()).sendMessageEmbeds(eb.build()).setActionRow(
                    Button.success("startOrderModal", "Start Order"),
                    Button.danger("deleteOrderModal", "Delete Order")
            ).queue(message -> {
                bot.getOrderDetail().addOrder(userId, message.getId(), hasSpecialRole);

//                if (!hasSpecialRole) {
//                    bot.getDailyOrderLimit().incrementOrderCount(bot.getGlobal().todayDate());
//                }

                bot.getOrderDetail().saveOrder();

                EmbedBuilder eb2 = new EmbedBuilder();
                eb2.setTitle(bot.getOrderCreatedTitle());
                eb2.setDescription(bot.getOrderCreatedMessage());
                eb2.setFooter(bot.getEmbedDetails().footer);
                eb2.setColor(Color.decode(bot.getOrderCreatedColor()));


                if (event.getJDA().getUserById(bot.getOrderDetail().getUserId()) != null) {
                    event.getJDA().getUserById(bot.getOrderDetail().getUserId()).openPrivateChannel().flatMap(privateChannel ->
                            privateChannel.sendMessageEmbeds(eb2.build())
                    ).queue();
                }

                updateOrderMenuEmbed(event.getChannel().asTextChannel(), bot.getOrderMenuMessageId());
                updateOrderStatus(event);
                event.getHook().deleteOriginal().queue();
            });

            /**
             * TODO: fix board
             */
            //createQueueEmbed(event);

            bot.getCooldown().addCooldown(userId);
            bot.getCooldown().saveCooldown();


        }

        if (event.getModalId().equals("deleteForm")) {
            String reason = event.getValue("reason") != null ? event.getValue("reason").getAsString() : null;
            String messageId = event.getMessage().getId();

            boolean moderationRole = event.getGuild().getMember(event.getUser()).getRoles().stream()
                    .anyMatch(role -> role.getId().equals(bot.getModerationRoleId()));
            boolean baseDesignRole = event.getGuild().getMember(event.getUser()).getRoles().stream()
                    .anyMatch(role -> role.getId().equals(bot.getBaseDesignerRoleId()));

            if (!moderationRole && !baseDesignRole) {
                EmbedBuilder noPermEB = new EmbedBuilder();
                noPermEB.setTitle("Error")
                        .setDescription("You do not have permission!")
                        .setColor(Color.RED)
                        .setFooter(bot.getEmbedDetails().footer);

                event.replyEmbeds(noPermEB.build()).setEphemeral(true).queue();
                return;
            }


            // Retrieve the message to get the user ID
            event.getChannel().retrieveMessageById(messageId).queue(message -> {
                List<MessageEmbed> embeds = message.getEmbeds();
                if (embeds.isEmpty()) {
                    event.reply("Order not found.").setEphemeral(true).queue();
                    return;
                }

                MessageEmbed embed = embeds.get(0);
                String userId = null;
                for (MessageEmbed.Field field : embed.getFields()) {
                    if (field.getName().equalsIgnoreCase("User ID")) {
                        userId = field.getValue();
                        break;
                    }
                }

                if (userId == null) {
                    event.reply("User ID not found in the order embed.").setEphemeral(true).queue();
                    return;
                }

                User user = event.getJDA().getUserById(userId);

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle(bot.getOrderDeletedTitle());
                eb.setDescription(bot.getOrderDeletedMessage().replace("%reason%", reason));
                eb.setFooter(bot.getEmbedDetails().footer);
                eb.setColor(Color.decode(bot.getOrderDeletedColor()));


                if (user != null) {
                    user.openPrivateChannel().flatMap(privateChannel ->
                            privateChannel.sendMessageEmbeds(eb.build())
                    ).queue();

                    boolean hasSpecialRole = event.getGuild().getMemberById(userId).getRoles().stream()
                            .anyMatch(role -> role.getId().equals(bot.getSpecialRoleId()));

//                    if (!hasSpecialRole) {
//                        bot.getDailyOrderLimit().decrementOrderCount(bot.getGlobal().todayDate());
//                    }
                }


                bot.getOrderDetail().removeOrder(messageId);
                bot.getOrderDetail().saveOrder();
                message.delete().queue();
                event.reply("Order has been deleted!").setEphemeral(true).queue();
                bot.getCooldown().removeCooldown(userId);

                updateOrderMenuEmbed(event.getJDA().getTextChannelById(bot.getOrderMenuChannelId()), bot.getOrderMenuMessageId());
            });
        }

        if (event.getModalId().equals("reviewModal")) {
            event.deferReply().setEphemeral(true).queue();

            String stars = event.getValue("ratingNumber") != null ? event.getValue("ratingNumber").getAsString() : null;
            String review = event.getValue("review") != null ? event.getValue("review").getAsString() : null;

            if (stars == null || review == null) {
                event.reply("Please fill out all required fields.").setEphemeral(true).queue();
                return;
            }

            if (Integer.parseInt(stars) < 1 || Integer.parseInt(stars) > 5) {
                event.reply("Stars must be between 1 and 5").setEphemeral(true).queue();
                return;
            }

            String emoji = "⭐".repeat(Math.max(0, Integer.parseInt(stars)));

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Review from " + event.getUser().getName());
            eb.setDescription("**Rated:** " + emoji + "\n\n**Review:** " + review);
            eb.setColor(Color.YELLOW);
            eb.setFooter(bot.getEmbedDetails().footer);

            event.getJDA().getTextChannelById(bot.getReviewChannelID()).sendMessageEmbeds(eb.build()).queue();

            event.getHook().sendMessage("Thank you for your review!").setEphemeral(true).queue();
        }
    }



    @Override
    public final void onReady(ReadyEvent event) {
        System.out.println("Ready event called.");
        CommandListUpdateAction commands = event.getJDA().updateCommands();
        for (Command cmd : bot.getCommandManager().getCommands()) {
            commands.addCommands(cmd.toCommand());
            System.out.println("Registered a new command: " + cmd.getName());
        }
        commands.queue();

        if (bot.getDailyOrderLimit().getResetTime() == null) {
            System.out.println("I was reset good or bad???");
            bot.getDailyOrderLimit().setResetTime(LocalDateTime.now().plusDays(1));
        }
    }

    private void updateOrderMenuEmbed(TextChannel textChannel, String messageId) {
        String todayDate = bot.getGlobal().todayDate();
        int currentCount = bot.getDailyOrderLimit().getCurrentOrderCount(todayDate);
        int maxLimit = bot.getDailyOrderMaxLimit();

        textChannel.retrieveMessageById(messageId).queue(message -> {
            MessageEmbed embed = message.getEmbeds().get(0);
            EmbedBuilder updatedEmbed = new EmbedBuilder(embed)
                    .clearFields()
                    .addField("Current Count", currentCount + "/" + maxLimit, false);

            message.editMessageEmbeds(updatedEmbed.build()).queue();
        });
    }

    private void updateOrderStatus(ModalInteractionEvent e) {
        int incompleteOrderCount = bot.getOrderDetail().getIncompleteOrders().size();

        String status;

        String messageLink = "https://discord.com/channels/1254738644711903303/1254790165290029076";
        Color embedColor;

        if (incompleteOrderCount <= 30) {
            status = ":green_circle:- There is a short queue for bases right now! get your order in a sit tight!\n\nPlease be patient and keep a eye on" + messageLink+ " for your base!";
            embedColor = Color.GREEN;
        } else if (incompleteOrderCount <= 100) {
            status = ":orange_circle:- There is a average queue for your base if you have just ordered!\n\nPlease be patient and keep a eye on" + messageLink+ " for your base!";
            embedColor = Color.ORANGE;
        } else {
            status = ":red_circle:- There is a long wait for bases currently if you have just ordered!\n\nPlease be patient and keep a eye on" + messageLink+ " for your base!";
            embedColor = Color.red;
        }

        e.getJDA().getTextChannelById("1255080970323755079").retrieveMessageById(bot.getUpdateOrderMessageId()).queue(message -> {
            MessageEmbed embed = message.getEmbeds().get(0);
            EmbedBuilder updatedEmbed = new EmbedBuilder(embed)
                    .clearFields()
                    .setDescription(status)
                            .setColor(embedColor);

            message.editMessageEmbeds(updatedEmbed.build()).queue();
        });
    }

}
