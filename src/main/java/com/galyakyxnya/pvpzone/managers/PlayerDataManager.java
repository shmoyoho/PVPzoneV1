package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.database.DatabaseManager;
import com.galyakyxnya.pvpzone.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class PlayerDataManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> playerDataCache;

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.databaseManager = new DatabaseManager(plugin);
        this.playerDataCache = new HashMap<>();

        // Миграция данных при первом запуске
        databaseManager.migrateFromYaml();
    }

    public PlayerData getPlayerData(UUID playerId) {
        return getPlayerData(playerId, null);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }

    private PlayerData getPlayerData(UUID playerId, String playerName) {
        // Проверяем кэш
        if (playerDataCache.containsKey(playerId)) {
            return playerDataCache.get(playerId);
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
            ResultSet playerRs = databaseManager.getPlayer(data.getPlayerId().toString());

            if (playerRs.next()) {
                data.setRating(playerRs.getInt("rating"));
                data.setPoints(playerRs.getInt("points"));
            } else {
                // Игрок не найден, создаем новую запись
                if (playerName != null) {
                    databaseManager.savePlayer(
                            data.getPlayerId().toString(),
                            playerName,
                            data.getRating(),
                            data.getPoints()
                    );
                }
            }

            // Загружаем бонусы
            ResultSet bonusesRs = databaseManager.getPlayerBonuses(data.getPlayerId().toString());
            while (bonusesRs.next()) {
                String bonusId = bonusesRs.getString("bonus_id");
                int level = bonusesRs.getInt("level");
                data.getPurchasedBonuses().put(bonusId, level);
            }

            // Загружаем сохраненный инвентарь
            ResultSet inventoryRs = databaseManager.getPlayerInventory(data.getPlayerId().toString());
            if (inventoryRs.next()) {
                String inventoryData = inventoryRs.getString("inventory_data");
                String armorData = inventoryRs.getString("armor_data");

                // Здесь нужно десериализовать инвентарь из строки
                // Пока оставим пустым, добавим позже
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки данных игрока из базы данных: " + data.getPlayerId(), e);
        }
    }

    public void savePlayerData(PlayerData data) {
        try {
            // Сохраняем основную информацию
            String playerName = Bukkit.getOfflinePlayer(data.getPlayerId()).getName();
            databaseManager.savePlayer(
                    data.getPlayerId().toString(),
                    playerName != null ? playerName : "Unknown",
                    data.getRating(),
                    data.getPoints()
            );

            // Сохраняем бонусы
            for (Map.Entry<String, Integer> entry : data.getPurchasedBonuses().entrySet()) {
                databaseManager.savePlayerBonus(
                        data.getPlayerId().toString(),
                        entry.getKey(),
                        entry.getValue()
                );
            }

            // Сохраняем инвентарь (если есть)
            if (data.getOriginalInventory() != null && data.getOriginalInventory().length > 0) {
                // Здесь нужно сериализовать инвентарь в строку
                String inventoryData = serializeInventory(data.getOriginalInventory());
                String armorData = serializeInventory(data.getOriginalArmor());

                if (inventoryData != null && armorData != null) {
                    databaseManager.savePlayerInventory(
                            data.getPlayerId().toString(),
                            inventoryData,
                            armorData
                    );
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения данных игрока в базу данных: " + data.getPlayerId(), e);
        }
    }

    private String serializeInventory(ItemStack[] items) {
        // Простая сериализация в Base64 (нужно реализовать)
        // Можно использовать BukkitObjectOutputStream
        return null; // Заглушка
    }

    private ItemStack[] deserializeInventory(String data) {
        // Десериализация из Base64
        return new ItemStack[0]; // Заглушка
    }

    public void saveAllData() {
        for (PlayerData data : playerDataCache.values()) {
            savePlayerData(data);
        }
    }

    public void removeCachedData(UUID playerId) {
        playerDataCache.remove(playerId);
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return new HashMap<>(playerDataCache);
    }

    public int getLoadedPlayersCount() {
        return playerDataCache.size();
    }

    // Метод для получения топ игроков по рейтингу
    public List<PlayerData> getTopPlayers(int limit) {
        List<PlayerData> topPlayers = new ArrayList<>();

        try {
            ResultSet rs = databaseManager.getTopPlayers(limit);
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("uuid"));
                PlayerData data = getPlayerData(playerId);
                topPlayers.add(data);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка получения топ игроков из базы данных", e);
        }

        return topPlayers;
    }

    // Метод для обновления инвентаря при входе в зону
    public void saveOriginalInventory(Player player) {
        PlayerData data = getPlayerData(player);
        data.setOriginalInventory(player.getInventory().getContents());
        data.setOriginalArmor(player.getInventory().getArmorContents());
        savePlayerData(data);
    }

    // Метод для восстановления инвентаря при выходе из зоны
    public void restoreOriginalInventory(Player player) {
        PlayerData data = getPlayerData(player);

        // Пока используем старую логику из кэша
        if (data.getOriginalInventory() != null && data.getOriginalInventory().length > 0) {
            player.getInventory().setContents(data.getOriginalInventory());
        } else {
            player.getInventory().clear();
        }

        if (data.getOriginalArmor() != null && data.getOriginalArmor().length > 0) {
            player.getInventory().setArmorContents(data.getOriginalArmor());
        } else {
            player.getInventory().setArmorContents(new ItemStack[4]);
        }

        player.updateInventory();

        // Очищаем сохраненный инвентарь
        data.setOriginalInventory(new ItemStack[0]);
        data.setOriginalArmor(new ItemStack[0]);
        savePlayerData(data);
    }

    // Метод для сброса всех данных игрока
    public void resetPlayerData(UUID playerId) {
        PlayerData data = new PlayerData(playerId);
        playerDataCache.put(playerId, data);
        savePlayerData(data);

        try {
            // Удаляем из базы данных
            databaseManager.removePlayerInventory(playerId.toString());
            // Удаляем бонусы
            ResultSet bonuses = databaseManager.getPlayerBonuses(playerId.toString());
            while (bonuses.next()) {
                databaseManager.removePlayerBonus(playerId.toString(), bonuses.getString("bonus_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сброса данных игрока: " + playerId, e);
        }
    }

    // Метод для поиска игрока по имени
    public UUID findPlayerIdByName(String playerName) {
        try {
            // Можно добавить поиск в базу данных
            for (PlayerData data : playerDataCache.values()) {
                String name = Bukkit.getOfflinePlayer(data.getPlayerId()).getName();
                if (name != null && name.equalsIgnoreCase(playerName)) {
                    return data.getPlayerId();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка поиска игрока по имени: " + playerName);
        }
        return null;
    }

    public void closeDatabase() {
        databaseManager.closeConnection();
    }
}