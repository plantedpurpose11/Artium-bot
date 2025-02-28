package me.jtx.evobases;

import io.github.cdimascio.dotenv.Dotenv;
import me.jtx.evobases.commands.CommandManager;
import me.jtx.evobases.events.EventListener;
import me.jtx.evobases.utils.*;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 * Main class for the EvoBases Discord bot.
 * This class handles the initialization of the bot and manages all core components.
 * It follows the singleton pattern to ensure only one instance exists.
 */
public class EvoBases {
    /** Singleton instance of the EvoBases bot */
    public static EvoBases instance = null;

    // Core components
    private final CommandManager commandManager;
    private final OrderDetail orderDetail;
    private final Cooldown cooldown;
    private final GuildSettings guildSettings;
    private final DailyOrderLimit dailyOrderLimit;
    private final Global global;
    private final Msg embedDetails;
    private final OrderEmbedDetails orderEmbedDetails;

    // Environment configuration
    private final Dotenv dotenv = Dotenv.load();

    // Bot configuration from environment variables
    private final String token = dotenv.get("TOKEN");
    private final String baseShowcaseChannelId = dotenv.get("BASE_SHOWCASE_CHANNEL_ID");
    private final String orderChannelId = dotenv.get("ORDER_CHANNEL_ID");
    private final String customActivityMessage = dotenv.get("CUSTOM_STATUS_MESSAGE");
    private final String menuTitle = dotenv.get("MENU_TITLE");
    private final String menuDescription = dotenv.get("MENU_DESCRIPTION");
    private final String menuStartOrderButtonMessage = dotenv.get("MENU_START_ORDER_BUTTON_MESSAGE");
    private final String specialRoleId = dotenv.get("SPECIAL_ROLE_ID");
    private final String moderationRoleId = dotenv.get("MODERATION_ROLE_ID");
    private final int dailyOrderMaxLimit = Integer.parseInt(dotenv.get("DAILY_ORDER_MAX_LIMIT"));
    private final String orderMenuMessageId = dotenv.get("ORDER_MENU_MESSAGE_ID");
    private final String orderMenuChannelId = dotenv.get("ORDER_MENU_CHANNEL_ID");
    private final String baseDesignerRoleId = dotenv.get("BASE_DESIGNER_ROLE_ID");
    private final String orderCompletedTitle = dotenv.get("ORDER_COMPLETED_TITLE");
    private final String orderCompletedMessage = dotenv.get("ORDER_COMPLETED_MESSAGE");
    private final String orderCompletedEmbedImage = dotenv.get("ORDER_COMPLETED_EMBED_IMAGE");
    private final String orderCompletedEmbedColorHex = dotenv.get("ORDER_COMPLETED_EMBED_COLOR_HEX");
    private final String cooldownMessage = dotenv.get("COOLDOWN_MESSAGE");
    private final String orderCreatedMessage = dotenv.get("ORDER_CREATED_MESSAGE");
    private final String orderDeletedMessage = dotenv.get("ORDER_DELETED_MESSAGE");
    private final String orderCreatedTitle = dotenv.get("ORDER_CREATED_TITLE");
    private final String orderCreatedColor = dotenv.get("ORDER_CREATED_COLOR");
    private final String orderDeletedTitle = dotenv.get("ORDER_DELETED_TITLE");
    private final String orderDeletedColor = dotenv.get("ORDER_DELETED_COLOR");
    private final String updateOrderMessageId = dotenv.get("UPDATE_ORDER_MESSAGE_ID");
    private final String orderStartedTitle = dotenv.get("ORDER_STARTED_TITLE");
    private final String orderStartedMessage = dotenv.get("ORDER_STARTED_MESSAGE");
    private final String orderStartedColor = dotenv.get("ORDER_STARTED_COLOR");
    private final String reviewChannelID = dotenv.get("REVIEW_CHANNEL_ID");

    /**
     * Constructor initializes the bot and all its components.
     * Sets up the JDA builder with necessary intents and listeners.
     */
    public EvoBases() {
        instance = this;

        // Initialize core components
        commandManager = new CommandManager(this);
        orderDetail = new OrderDetail();
        cooldown = new Cooldown();
        guildSettings = new GuildSettings();
        dailyOrderLimit = new DailyOrderLimit(this);
        global = new Global();
        embedDetails = new Msg();
        orderEmbedDetails = new OrderEmbedDetails();

        // Initialize command system
        commandManager.initialize();

        // Configure and build JDA
        JDABuilder.createLight(getToken(), 
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.DIRECT_MESSAGES, 
            GatewayIntent.GUILD_MESSAGE_REACTIONS,
            GatewayIntent.GUILD_VOICE_STATES, 
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_PRESENCES)
            .setStatus(OnlineStatus.ONLINE)
            .enableCache(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .enableCache(CacheFlag.MEMBER_OVERRIDES)
            .addEventListeners(new EventListener(this))
            .setActivity(Activity.customStatus(customActivityMessage))
            .build();
    }

    // Getter methods with JavaDoc
    
    /**
     * Gets the command manager instance.
     * @return The command manager handling all bot commands
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Gets the cooldown manager instance.
     * @return The cooldown system for managing command timeouts
     */
    public Cooldown getCooldown() {
        return cooldown;
    }

    /**
     * Gets the order detail manager instance.
     * @return The order detail system for managing order information
     */
    public OrderDetail getOrderDetail() {
        return orderDetail;
    }

    /**
     * Gets the guild settings manager instance.
     * @return The guild settings system for managing server configurations
     */
    public GuildSettings getGuildSettings() {
        return guildSettings;
    }

    /**
     * Gets the daily order limit manager instance.
     * @return The daily order limit system for managing order restrictions
     */
    public DailyOrderLimit getDailyOrderLimit() {
        return dailyOrderLimit;
    }

    /**
     * Gets the embed details manager instance.
     * @return The message embed system for creating formatted messages
     */
    public Msg getEmbedDetails() {
        return embedDetails;
    }

    /**
     * Gets the global utilities instance.
     * @return The global utility functions
     */
    public Global getGlobal() {
        return global;
    }

    // Environment variable getters with JavaDoc

    /**
     * Gets the bot token from environment variables.
     * @return The Discord bot token
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets the base showcase channel ID.
     * @return The channel ID for base showcases
     */
    public String getBaseShowcaseChannelId() {
        return baseShowcaseChannelId;
    }

    /**
     * Gets the order channel ID.
     * @return The channel ID for orders
     */
    public String getOrderChannelId() {
        return orderChannelId;
    }

    /**
     * Gets the menu start order button message.
     * @return The message for starting an order
     */
    public String getMenuStartOrderButtonMessage() {
        return menuStartOrderButtonMessage;
    }

    /**
     * Gets the menu description.
     * @return The description for the menu
     */
    public String getMenuDescription() {
        return menuDescription;
    }

    /**
     * Gets the menu title.
     * @return The title for the menu
     */
    public String getMenuTitle() {
        return menuTitle;
    }

    /**
     * Gets the special role ID.
     * @return The ID for the special role
     */
    public String getSpecialRoleId() {
        return specialRoleId;
    }

    /**
     * Gets the moderation role ID.
     * @return The ID for the moderation role
     */
    public String getModerationRoleId() {
        return moderationRoleId;
    }

    /**
     * Gets the daily order max limit.
     * @return The maximum number of orders allowed per day
     */
    public int getDailyOrderMaxLimit() {
        return dailyOrderMaxLimit;
    }

    /**
     * Gets the order menu message ID.
     * @return The ID for the order menu message
     */
    public String getOrderMenuMessageId() {
        return orderMenuMessageId;
    }

    /**
     * Gets the order menu channel ID.
     * @return The channel ID for the order menu
     */
    public String getOrderMenuChannelId() {
        return orderMenuChannelId;
    }

    /**
     * Gets the base designer role ID.
     * @return The ID for the base designer role
     */
    public String getBaseDesignerRoleId() {
        return baseDesignerRoleId;
    }

    /**
     * Gets the order completed message.
     * @return The message for completing an order
     */
    public String getOrderCompletedMessage() {
        return orderCompletedMessage;
    }

    /**
     * Gets the order completed embed image.
     * @return The image for the completed order embed
     */
    public String getOrderCompletedEmbedImage() {
        return orderCompletedEmbedImage;
    }

    /**
     * Gets the order completed embed color hex.
     * @return The color hex for the completed order embed
     */
    public String getOrderCompletedEmbedColorHex() {
        return orderCompletedEmbedColorHex;
    }

    /**
     * Gets the cooldown message.
     * @return The message for the cooldown system
     */
    public String getCooldownMessage() {
        return cooldownMessage;
    }

    /**
     * Gets the order deleted message.
     * @return The message for deleting an order
     */
    public String getOrderDeletedMessage() {
        return orderDeletedMessage;
    }

    /**
     * Gets the order created message.
     * @return The message for creating an order
     */
    public String getOrderCreatedMessage() {
        return orderCreatedMessage;
    }

    /**
     * Gets the order deleted title.
     * @return The title for deleting an order
     */
    public String getOrderDeletedTitle() {
        return orderDeletedTitle;
    }

    /**
     * Gets the order created color.
     * @return The color for the created order
     */
    public String getOrderCreatedColor() {
        return orderCreatedColor;
    }

    /**
     * Gets the order created title.
     * @return The title for creating an order
     */
    public String getOrderCreatedTitle() {
        return orderCreatedTitle;
    }

    /**
     * Gets the order deleted color.
     * @return The color for the deleted order
     */
    public String getOrderDeletedColor() {
        return orderDeletedColor;
    }

    /**
     * Gets the update order message ID.
     * @return The ID for updating an order
     */
    public String getUpdateOrderMessageId() {
        return updateOrderMessageId;
    }

    /**
     * Gets the order started message.
     * @return The message for starting an order
     */
    public String getOrderStartedMessage() {
        return orderStartedMessage;
    }

    /**
     * Gets the order started title.
     * @return The title for starting an order
     */
    public String getOrderStartedTitle() {
        return orderStartedTitle;
    }

    /**
     * Gets the order started color.
     * @return The color for the started order
     */
    public String getOrderStartedColor() {
        return orderStartedColor;
    }

    /**
     * Gets the order completed title.
     * @return The title for completing an order
     */
    public String getOrderCompletedTitle() {
        return orderCompletedTitle;
    }

    /**
     * Gets the review channel ID.
     * @return The channel ID for reviews
     */
    public String getReviewChannelID() {
        return reviewChannelID;
    }

    /**
     * Gets the order embed details manager instance.
     * @return The order embed details system for managing order-related embeds
     */
    public OrderEmbedDetails getOrderEmbedDetails() {
        return orderEmbedDetails;
    }

    /**
     * Main method to start the bot.
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        new EvoBases();
    }
}
