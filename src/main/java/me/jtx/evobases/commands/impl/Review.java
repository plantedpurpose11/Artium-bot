package me.jtx.evobases.commands.impl;

import me.jtx.evobases.EvoBases;
import me.jtx.evobases.commands.Command;
import me.jtx.evobases.commands.CommandContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;

/**
 * Command implementation for submitting order reviews.
 * Creates a modal dialog with rating and feedback inputs.
 * Allows users to provide a star rating and written feedback for their orders.
 */
public class Review extends Command {

    private final EvoBases bot;
    private static final String RATING_FIELD_ID = "ratingNumber";
    private static final String REVIEW_FIELD_ID = "review";
    private static final String MODAL_ID = "reviewModal";

    /**
     * Constructs a new Review command.
     *
     * @param bot The EvoBases bot instance
     */
    public Review(EvoBases bot) {
        super("review", 
              Permission.UNKNOWN, 
              null, 
              "Submit a review for your completed order", 
              null);

        this.bot = bot;
        this.bot.getCommandManager().register(this);
    }

    /**
     * Executes the review command.
     * Creates and displays a modal with rating and feedback inputs.
     *
     * @param ctx The command context
     */
    @Override
    public void execute(CommandContext ctx) {
        // Create the star rating input field
        TextInput ratingInput = TextInput.create(RATING_FIELD_ID, "Rating", TextInputStyle.SHORT)
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(1)
                .setPlaceholder("Rate from 1 to 5 stars")
                .build();

        // Create the feedback text input field
        TextInput feedbackInput = TextInput.create(REVIEW_FIELD_ID, "Feedback", TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .setMinLength(10)
                .setMaxLength(1000)
                .setPlaceholder("Please share your experience with the order")
                .build();

        // Create and show the review modal
        Modal reviewModal = Modal.create(MODAL_ID, "Order Review")
                .addComponents(
                    ActionRow.of(ratingInput),
                    ActionRow.of(feedbackInput)
                )
                .build();

        ctx.getSlashEvent().replyModal(reviewModal).queue();
    }
}
