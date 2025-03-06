package me.jtx.evobases.utils;

import com.google.gson.*;
import me.jtx.evobases.EvoBases;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class represents the details of an order.
 * It includes methods for adding orders, getting order details,
 * loading and saving orders, and other related functionalities.
 */
public class OrderDetail {

    /**
     * Default constructor that initializes the orders array and loads any existing orders.
     */
    public OrderDetail() {
        orders = new JsonArray();
        loadOrders();
    }

    private JsonArray orders;
    private int orderId;
    private int queueNum;
    private String userId;
    private boolean completed;
    private String messageId;


    /**
     * Constructor that initializes all fields of the OrderDetail object.
     *
     * @param orderId    The ID of the order.
     * @param queueNum   The queue number of the order.
     * @param userId     The ID of the user who placed the order.
     * @param completed  The status of the order, true if completed, false otherwise.
     * @param messageId  The ID of the message associated with the order.
     */
    public OrderDetail(int orderId, int queueNum, String userId, boolean completed, String messageId) {
        this.orderId = orderId;
        this.queueNum = queueNum;
        this.userId = userId;
        this.completed = completed;
        this.messageId = messageId;
    }

    /**
     * Adds a new order to the orders array.
     *
     * @param userId     The ID of the user who is placing the order.
     * @param messageId  The ID of the message associated with the order.
     */
    public void addOrder(String userId, String messageId, boolean hasSpecialRole) {
        this.orderId = getHighestOrderNum() + 1;
        this.userId = userId;
        this.completed = false;
        this.messageId = messageId;

        JsonObject order = new JsonObject();
        order.addProperty("orderId", this.orderId);
        order.addProperty("queueNum", this.queueNum);
        order.addProperty("userId", this.userId);
        order.addProperty("completed", this.completed);
        order.addProperty("messageId", this.messageId);

//        if (hasSpecialRole) {
//            insertPriorityOrder(order);
//        } else {
        orders.add(new Gson().toJsonTree(order));
        recalculateQueuePositions();
        //}
        saveOrder();
    }

    /**
     * Loads orders from a file into the orders array.
     * If the file doesn't exist, it creates a new one.
     * If the file does exist, it reads the file and loads the orders into the orders array.
     */
    public void loadOrders() {
        try {
            Path path = Paths.get("Orders.json");
            if (!Files.exists(path)) {
                Files.createFile(path);
                Files.write(path, "[]".getBytes());
            }

            File file = new File("Orders.json");
            JsonArray newOrder = new Gson().fromJson(new FileReader(file), JsonArray.class);
            if (newOrder != null) {
                orders = newOrder;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the current state of the orders array to a file.
     */
    public void saveOrder() {
        try {
            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(orders);
            Path path = Paths.get("Orders.json");
            Files.write(path, jsonString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void insertPriorityOrder(JsonObject order) {
        List<JsonObject> incompleteOrders = getIncompleteOrders();
        int insertPosition = 0;
        for (int i = 0; i < incompleteOrders.size(); i++) {
            JsonObject existingOrder = incompleteOrders.get(i);
            if (existingOrder.get("userId").getAsString().equals(this.userId)) {
                insertPosition = i + 1; // Insert after the last special role user
            }
        }

        List<JsonElement> orderList = new ArrayList<>();
        orders.forEach(orderList::add);
        orderList.add(insertPosition, order);
        orders = new JsonArray();
        orderList.forEach(orders::add);

        recalculateQueuePositions();
    }

    /**
     * Returns the highest position in the orders array.
     *
     * @return The highest position in the orders array.
     */
    private int getHighestPosition() {
        return orders.isEmpty() ? 0 : Collections.max(StreamSupport.stream(orders.spliterator(), false)
                .map(element -> element.getAsJsonObject().get("queueNum").getAsInt())
                .toList());
    }

    /**
     * Returns the highest order number in the orders array.
     *
     * @return The highest order number in the orders array.
     */
    public int getHighestOrderNum() {
        return orders.isEmpty() ? 0 : Collections.max(StreamSupport.stream(orders.spliterator(), false)
                .map(element -> element.getAsJsonObject().get("orderId").getAsInt())
                .toList());
    }

    /**
     * Returns a list of all order IDs in the orders array.
     *
     * @return A list of all order IDs in the orders array.
     */
    public List<Integer> getAllOrderIds() {
        return StreamSupport.stream(orders.spliterator(), false)
                .map(element -> element.getAsJsonObject().get("orderId").getAsInt())
                .collect(Collectors.toList());
    }

    /**
     * Checks if an order ID exists in the orders array.
     *
     * @param orderId The ID of the order to check.
     * @return true if the order ID exists, false otherwise.
     */
    public boolean orderIdExists(int orderId) {
        return getAllOrderIds().contains(orderId);
    }

    /**
     * Returns the user ID associated with a given order ID.
     *
     * @param orderId The ID of the order to check.
     * @return The user ID associated with the given order ID, or null if the order ID does not exist.
     */
    public String getUserIdByOrderId(int orderId) {
        for (JsonElement orderElement : orders) {
            JsonObject order = orderElement.getAsJsonObject();
            if (order.get("orderId").getAsInt() == orderId) {
                return order.get("userId").getAsString();
            }
        }
        return null;
    }

    /**
     * Returns the message ID associated with a given order ID.
     *
     * @param orderId The ID of the order to check.
     * @return The message ID associated with the given order ID, or null if the order ID does not exist.
     */
    public String getMessageIdByOrderId(int orderId) {
        for (JsonElement orderElement : orders) {
            JsonObject order = orderElement.getAsJsonObject();
            if (order.get("orderId").getAsInt() == orderId) {
                return order.get("messageId").getAsString();
            }
        }
        return null;
    }

    public String getUserIdByMessageId(String messageId) {
        for (JsonElement orderElement : orders) {
            JsonObject order = orderElement.getAsJsonObject();
            if (order.get("messageId").getAsString().equals(messageId)) {
                return order.get("userId").getAsString();
            }
        }
        return null;
    }

    /**
     * Checks if the order is completed.
     *
     * @return true if the order is completed, false otherwise.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Sets an order as completed and recalculates queue positions.
     *
     * @param orderId The ID of the order to mark as completed.
     */
    public void setCompleted(int orderId) {
        orders.forEach(order -> {
            JsonObject orderObj = order.getAsJsonObject();
            if (orderObj.get("orderId").getAsInt() == orderId) {
                orderObj.addProperty("completed", true);
            }
        });
        recalculateQueuePositions();
        saveOrder();
    }

    /**
     * Recalculates the queue positions of the orders.
     */
    public void recalculateQueuePositions() {
        int position = 1;
        for (JsonElement order : orders) {
            JsonObject orderObj = order.getAsJsonObject();
            if (!orderObj.get("completed").getAsBoolean()) {
                orderObj.addProperty("queueNum", position++);
            }
        }
        saveOrder();
    }

    /**
     * Returns a list of all incomplete orders.
     *
     * @return A list of all incomplete orders.
     */
    public List<JsonObject> getIncompleteOrders() {
        return StreamSupport.stream(orders.spliterator(), false)
                .map(element -> element.getAsJsonObject())
                .filter(order -> !order.get("completed").getAsBoolean())
                .collect(Collectors.toList());
    }

    /**
     * Removes an order from the orders array.
     *
     * @param messageId The ID of the message associated with the order to remove.
     */
    public void removeOrder(String messageId) {
        for (JsonElement orderElement : orders) {
            JsonObject order = orderElement.getAsJsonObject();
            if (order.has("messageId")) {
                String orderMessageId = order.get("messageId").getAsString();


                if (orderMessageId.equals(messageId)) {
                    if (order.has("completed") && !order.get("completed").getAsBoolean()) {
                        order.addProperty("completed", true);
                        saveOrder();
                        recalculateQueuePositions();
                    }

                    break;
                }
            }
        }
    }

    public void autoAdjustQueue() {
        List<JsonObject> incompleteOrders = getIncompleteOrders();


        incompleteOrders.sort(Comparator.comparingInt(o -> o.get("queueNum").getAsInt()));

        int expectedQueueNum = 1;


        for (JsonObject order : incompleteOrders) {
            System.out.println(order);
            int currentQueueNum = order.get("queueNum").getAsInt();
            System.out.println(currentQueueNum + " - Current");
            System.out.println("Expected - " + expectedQueueNum);


            if (currentQueueNum != expectedQueueNum) {
                order.addProperty("queueNum", expectedQueueNum);
                System.out.println("Not expected " + expectedQueueNum);
            }

            expectedQueueNum++;



        }

        saveOrder();
    }

    /**
     * Returns the user ID of the order.
     *
     * @return The user ID of the order.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Updates the message ID for a specific order.
     *
     * @param orderId The ID of the order to update
     * @param newMessageId The new message ID to set
     */
    public void updateMessageId(int orderId, String newMessageId) {
        for (JsonElement orderElement : orders) {
            JsonObject order = orderElement.getAsJsonObject();
            if (order.get("orderId").getAsInt() == orderId) {
                order.addProperty("messageId", newMessageId);
                break;
            }
        }
    }
}

