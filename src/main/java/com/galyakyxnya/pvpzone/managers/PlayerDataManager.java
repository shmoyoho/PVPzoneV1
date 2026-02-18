package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.database.DatabaseManager;
import com.galyakyxnya.pvpzone.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.Base64;

public class PlayerDataManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> playerDataCache;

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.databaseManager = new DatabaseManager(plugin);
        this.playerDataCache = new ConcurrentHashMap<>();
    }

    public PlayerData getPlayerData(UUID playerId) {
        return getPlayerData(playerId, null);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }

    private PlayerData getPlayerData(UUID playerId, String playerName) {
        // Проверяем кэш
        PlayerData cachedData = playerDataCache.get(playerId);
        if (cachedData != null) {
            return cachedData;
        }

        // Загружаем из базы данных
        PlayerData data = new PlayerData(playerId);
        loadPlayerDataFromDatabase(data, playerName);
        playerDataCache.put(playerId, data);

        return data;
    }

    private void loadPlayerDataFromDatabase(PlayerData data, String playerName) {
        try {
            // Загружаем основную информацию об игроке
            ResultSet playerRs = databaseManager.getPlayerSync(data.getPlayerId().toString());

            if (playerRs.next()) {
                data.setRating(playerRs.getInt("rating"));
                data.setPoints(playerRs.getInt("points"));
            } else {
                // Игрок не найден, создаем новую запись асинхронно
                if (playerName != null) {
                    databaseManager.savePlayerAsync(
                            data.getPlayerId().toString(),
                            playerName,
                            data.getRating(),
                            data.getPoints()
                    );
                }
            }

            // Загружаем бонусы
            ResultSet bonusesRs = databaseManager.getPlayerBonusesSync(data.getPlayerId().toString());
            while (bonusesRs.next()) {
                String bonusId = bonusesRs.getString("bonus_id");
                int level = bonusesRs.getInt("level");
                data.getPurchasedBonuses().put(bonusId, level);
            }

            // Загружаем сохранённый инвентарь только если флаг «восстановить при входе» (игрок вышел в PvP-зоне)
            try (ResultSet invRs = databaseManager.getPlayerInventorySync(data.getPlayerId().toString())) {
                if (invRs.next()) {
                    int restoreOnJoin = 0;
                    try {
                        restoreOnJoin = invRs.getInt("restore_inventory_on_join");
                    } catch (SQLException ignored) { }
                    if (restoreOnJoin != 1) return;
                    String invData = invRs.getString("inventory_data");
                    String armorData = invRs.getString("armor_data");
                    if (invData != null && !invData.isEmpty()) {
                        data.setOriginalInventory(deserializeItemStacks(invData));
                    }
                    if (armorData != null && !armorData.isEmpty()) {
                        data.setOriginalArmor(deserializeItemStacks(armorData));
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка загрузки данных игрока: " + data.getPlayerId(), e);
        }
    }

    /** Сериализация ItemStack[] в Base64 для хранения в БД */
    public static String serializeItemStacks(ItemStack[] items) {
        if (items == null || items.length == 0) return "";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    /** Десериализация ItemStack[] из Base64 */
    public static ItemStack[] deserializeItemStacks(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }

    /** Сохраняет оригинальный инвентарь из PlayerData в БД (вызывать при входе в PvP зону). Флаг restoreOnJoin = false. */
    public void saveOriginalInventoryToDatabase(Player player) {
        if (player == null) return;
        PlayerData data = getPlayerData(player);
        ItemStack[] inv = data.getOriginalInventory();
        ItemStack[] armor = data.getOriginalArmor();
        if ((inv == null || inv.length == 0) && (armor == null || armor.length == 0)) return;
        String invData = serializeItemStacks(inv != null ? inv : new ItemStack[0]);
        String armorData = serializeItemStacks(armor != null ? armor : new ItemStack[0]);
        databaseManager.savePlayerInventoryAsync(player.getUniqueId().toString(), invData, armorData, false);
    }

    /** Включить флаг «восстановить инвентарь при следующем входе» (игрок вышел, находясь в PvP-зоне). */
    public void setRestoreInventoryOnJoinTrue(UUID playerId) {
        databaseManager.setRestoreInventoryOnJoinTrue(playerId.toString());
    }

    /** Очистить сохранённый инвентарь и флаг (игрок вышел не в PvP-зоне — инвентарем управляет сервер). */
    public void clearPlayerInventoryAndRestoreFlag(UUID playerId) {
        databaseManager.clearPlayerInventoryAndRestoreFlag(playerId.toString());
    }

    /** Сбросить флаг после восстановления инвентаря при входе. */
    public void clearRestoreInventoryOnJoinFlag(UUID playerId) {
        databaseManager.clearRestoreInventoryOnJoinFlag(playerId.toString());
    }

    /** Загружает сохранённый инвентарь из БД (может вернуть пустые массивы) */
    public ItemStack[][] loadSavedInventoryFromDatabase(UUID playerId) {
        ItemStack[] inv = new ItemStack[0];
        ItemStack[] armor = new ItemStack[0];
        try (ResultSet rs = databaseManager.getPlayerInventorySync(playerId.toString())) {
            if (rs.next()) {
                String invData = rs.getString("inventory_data");
                String armorData = rs.getString("armor_data");
                if (invData != null && !invData.isEmpty()) inv = deserializeItemStacks(invData);
                if (armorData != null && !armorData.isEmpty()) armor = deserializeItemStacks(armorData);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка загрузки инвентаря: " + playerId, e);
        }
        return new ItemStack[][]{ inv, armor };
    }

    public void savePlayerData(PlayerData data) {
        // Асинхронное сохранение основный данных
        String playerName = Bukkit.getOfflinePlayer(data.getPlayerId()).getName();
        databaseManager.savePlayerAsync(
                data.getPlayerId().toString(),
                playerName != null ? playerName : "Unknown",
                data.getRating(),
                data.getPoints()
        );

        // Асинхронное сохранение бонусов
        for (Map.Entry<String, Integer> entry : data.getPurchasedBonuses().entrySet()) {
            databaseManager.savePlayerBonusAsync(
                    data.getPlayerId().toString(),
                    entry.getKey(),
                    entry.getValue()
            );
        }
    }

    public void saveAllData() {
        // Сохраняем всех игроков из кэша асинхронно
        for (PlayerData data : playerDataCache.values()) {
            savePlayerData(data);
        }
    }

    public void removeCachedData(UUID playerId) {
        playerDataCache.remove(playerId);
    }

    public int getLoadedPlayersCount() {
        return playerDataCache.size();
    }

    // Метод для получения топ игроков по рейтингу
    public List<PlayerData> getTopPlayers(int limit) {
        List<PlayerData> topPlayers = new ArrayList<>();

        try {
            ResultSet rs = databaseManager.getTopPlayersSync(limit);
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("uuid"));
                PlayerData data = getPlayerData(playerId);
                topPlayers.add(data);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка получения топ игроков", e);
        }

        return topPlayers;
    }

    // Метод для обновления инвентаря при входе в зону
    public void saveOriginalInventory(Player player) {
        PlayerData data = getPlayerData(player);
        // Клонируем массивы, чтобы не было ссылок
        data.setOriginalInventory(player.getInventory().getContents().clone());
        data.setOriginalArmor(player.getInventory().getArmorContents().clone());
    }

    // Метод для восстановления инвентаря при выходе из зоны
    public void restoreOriginalInventory(Player player) {
        PlayerData data = getPlayerData(player);

        if (data.getOriginalInventory().length > 0) {
            player.getInventory().setContents(data.getOriginalInventory());
        } else {
            player.getInventory().clear();
        }

        if (data.getOriginalArmor().length > 0) {
            player.getInventory().setArmorContents(data.getOriginalArmor());
        } else {
            player.getInventory().setArmorContents(new ItemStack[4]);
        }

        player.updateInventory();
    }

    // Метод для сброса всех данных игрока
    public void resetPlayerData(UUID playerId) {
        PlayerData data = new PlayerData(playerId);
        playerDataCache.put(playerId, data);
        savePlayerData(data);

        // Очищаем кэш
        removeCachedData(playerId);
    }

    public void closeDatabase() {
        databaseManager.closeConnection();
    }
}