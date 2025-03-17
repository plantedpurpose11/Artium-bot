package me.jtx.evobases.commands.impl;

import me.jtx.evobases.EvoBases;
import me.jtx.evobases.commands.Command;
import me.jtx.evobases.commands.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Move extends Command {

    private final EvoBases bot;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Move(EvoBases bot) {
        super("move", Permission.ADMINISTRATOR, null, "Move completed orders", null);
        this.bot = bot;
       // this.bot.getCommandManager().register(this);

    }

    private Runnable moveCompletedOrders(CommandContext e) {
        return () -> {
            TextChannel orderChannel = e.getGuild().getTextChannelById(bot.getOrderChannelId());
            TextChannel completedOrdersChannel = e.getGuild().getTextChannelById("1286019263655706634"); // Completed orders channel ID

            if (orderChannel != null && completedOrdersChannel != null) {
                orderChannel.getIterableHistory().forEachAsync(message -> {
                    List<MessageEmbed> embeds = message.getEmbeds();

                    if (!embeds.isEmpty()) {
                        MessageEmbed embed = embeds.get(0);

                        if (embed.getFields().stream().anyMatch(field -> field.getName().equalsIgnoreCase("Completed") && field.getValue().equalsIgnoreCase("true"))) {
                            EmbedBuilder embedBuilder = new EmbedBuilder(embed);
                            embedBuilder.setColor(Color.GREEN);

                            completedOrdersChannel.sendMessageEmbeds(embedBuilder.build()).queue(success -> {
                                message.delete().queue();
                            });
                        }
                    }
                    return true;
                });
            }
        };
    }

    @Override
    public void execute(CommandContext e) {
        boolean mod = e.getGuild().getMember(e.getUser()).getRoles().stream()
                .anyMatch(role -> role.getId().equals(bot.getModerationRoleId()));

        if (!mod) {
            EmbedBuilder noPermEB = new EmbedBuilder();
            noPermEB.setTitle("Error")
                    .setDescription("You do not have permission!")
                    .setColor(Color.RED)
                    .setFooter(bot.getEmbedDetails().footer);

            e.getSlashEvent().replyEmbeds(noPermEB.build()).setEphemeral(true).queue();
            return;
        }

        e.getSlashEvent().reply("Started moving completed orders every 5 seconds.").setEphemeral(true).queue();
        scheduler.scheduleAtFixedRate(moveCompletedOrders(e), 0, 5, TimeUnit.SECONDS);
    }
}
