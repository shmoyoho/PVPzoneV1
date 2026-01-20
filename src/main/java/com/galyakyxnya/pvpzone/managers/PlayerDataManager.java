package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.database.DatabaseManager;
import com.galyakyxnya.pvpzone.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка загрузки данных игрока: " + data.getPlayerId(), e);
        }
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

        // Логируем что восстанавливаем
        plugin.getLogger().info("Restoring inventory for " + player.getName() +
                " - saved items: " + data.getOriginalInventory().length);

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