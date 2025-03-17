package me.jtx.evobases.commands.impl;

import me.jtx.evobases.EvoBases;
import me.jtx.evobases.commands.Command;
import me.jtx.evobases.commands.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

/**
 * Command implementation for displaying the order menu.
 * Creates an embedded message with order information and a button to start the order process.
 * If daily order limits are enabled, also displays the current order count.
 */
public class OrderMenu extends Command {

    private final EvoBases bot;

    /**
     * Constructs a new OrderMenu command.
     *
     * @param bot The EvoBases bot instance
     */
    public OrderMenu(EvoBases bot) {
        super("ticketmenu", 
              Permission.ADMINISTRATOR,
              null, 
              "Display the order menu with current status and start button", 
              null);

        this.bot = bot;
        this.bot.getCommandManager().register(this);
    }

    /**
     * Executes the order menu command.
     * Creates and sends an embedded message containing:
     * - Menu title and description
     * - Current order count and limit (if enabled)
     * - Button to start a new order
     *
     * @param ctx The command context
     */
    @Override
    public void execute(CommandContext ctx) {
        // Create the embed builder with basic information
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(bot.getMenuTitle())
             .setDescription(bot.getMenuDescription())
             .setColor(Color.WHITE)
             .setFooter(bot.getEmbedDetails().footer);

        // Add daily order limit information if enabled
        if (bot.getDailyOrderLimit().isDailyOrderMaxLimitEnabled()) {
            String todayDate = bot.getGlobal().todayDate();
            int currentCount = bot.getDailyOrderLimit().getCurrentOrderCount(todayDate);
            int maxLimit = bot.getDailyOrderMaxLimit();

            embed.setDescription(String.format(bot.getMenuDescription(), maxLimit, "\n"));
            embed.addField("Current Order Count", currentCount + "/" + maxLimit, false);
        }

        // Acknowledge the slash command
        ctx.getSlashEvent().deferReply().setEphemeral(true).queue();

        // Send the menu with the order button
        ctx.getTextChannel().sendMessageEmbeds(embed.build())
            .addActionRow(Button.success("startOrderFormModal", bot.getMenuStartOrderButtonMessage()))
            .queue();
    }
}