package me.jtx.evobases.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jtx.evobases.EvoBases;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DailyOrderLimit {

    private static final String FILE_NAME = "DailyOrderLimit.json";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Map<String, Integer> dailyLimit;
    private LocalDateTime resetTime;
    private EvoBases bot;

    public DailyOrderLimit(EvoBases bot) {
        this.bot = bot;
        loadOrderDailyLimit();
    }

    public void loadOrderDailyLimit() {
        File tempFile = new File(FILE_NAME);
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
                dailyLimit = new HashMap<>();
                resetTime = LocalDateTime.now().plusDays(1); // Set default reset time???
                saveOrderDailyLimit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Map<String, Object> data = new Gson().fromJson(new FileReader(tempFile), Map.class);
                if (data == null) {
                    dailyLimit = new HashMap<>();
                    resetTime = LocalDateTime.now().plusDays(1); // Set default reset time if data is null??
                } else {
                    dailyLimit = new HashMap<>();
                    Map<String, Number> tempLimit = (Map<String, Number>) data.get("dailyOrderLimit");
                    if (tempLimit != null) {
                        for (Map.Entry<String, Number> entry : tempLimit.entrySet()) {
                            dailyLimit.put(entry.getKey(), entry.getValue().intValue());
                        }
                    }
                    String resetTimeStr = (String) data.get("resetTime");
                    resetTime = resetTimeStr != null ? LocalDateTime.parse(resetTimeStr, formatter) : LocalDateTime.now().plusDays(1);
                }
                saveOrderDailyLimit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveOrderDailyLimit() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("dailyOrderLimit", dailyLimit);
            data.put("resetTime", resetTime != null ? resetTime.format(formatter) : null);

            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(data);
            Path path = Paths.get(FILE_NAME);
            Files.write(path, jsonString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkOrderLimit(String date) {
        int limit = bot.getDailyOrderMaxLimit();
        int currentCount = dailyLimit.getOrDefault(date, 0);
        return currentCount < limit;
    }

    public void incrementOrderCount(String date) {
        int currentCount = dailyLimit.getOrDefault(date, 0);
        dailyLimit.put(date, currentCount + 1);
        saveOrderDailyLimit();
    }

    public void decrementOrderCount(String date) {
        int currentCount = dailyLimit.getOrDefault(date, 0);
        if (currentCount > 0) {
            dailyLimit.put(date, currentCount - 1);
            saveOrderDailyLimit();
        }
    }


    public void resetDailyLimit(String date) {
        dailyLimit.put(date, 0);
        saveOrderDailyLimit();
    }

    public boolean isResetTimePassed() {
        return resetTime != null && LocalDateTime.now().isAfter(resetTime);
    }

    public String getResetTime() {
        if (resetTime == null) {
            return "ERROR: No reset time set";
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, resetTime);

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    public void setResetTime(LocalDateTime resetTime) {
        this.resetTime = resetTime;
        saveOrderDailyLimit();
    }

    public int getCurrentOrderCount(String date) {
        return dailyLimit.getOrDefault(date, 0);
    }

    public boolean isDailyOrderMaxLimitEnabled() {
        String enabledStr = System.getenv("DAILY_ORDER_MAX_LIMIT_ENABLE");
        return "true".equalsIgnoreCase(enabledStr);
    }
}
