package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PlayerDataManager {
    private final Main plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private final File dataFolder;
    
    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        loadAllData();
    }
    
    public PlayerData getPlayerData(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, k -> {
            PlayerData data = new PlayerData(playerId);
            loadPlayerData(data);
            return data;
        });
    }
    
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    public void savePlayerData(PlayerData data) {
        File file = new File(dataFolder, data.getPlayerId().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("rating", data.getRating());
        config.set("points", data.getPoints());
        
        // Сохраняем купленные бонусы
        if (!data.getPurchasedBonuses().isEmpty()) {
            for (Map.Entry<String, Integer> entry : data.getPurchasedBonuses().entrySet()) {
                config.set("bonuses." + entry.getKey(), entry.getValue());
            }
        }
        
        // Сохраняем оригинальный инвентарь (базовая сериализация)
        if (data.getOriginalInventory() != null && data.getOriginalInventory().length > 0) {
            for (int i = 0; i < data.getOriginalInventory().length; i++) {
                if (data.getOriginalInventory()[i] != null) {
                    config.set("saved_inventory." + i, data.getOriginalInventory()[i]);
                }
            }
        }
        
        // Сохраняем оригинальную броню
        if (data.getOriginalArmor() != null && data.getOriginalArmor().length > 0) {
            for (int i = 0; i < data.getOriginalArmor().length; i++) {
                if (data.getOriginalArmor()[i] != null) {
                    config.set("saved_armor." + i, data.getOriginalArmor()[i]);
                }
            }
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить данные игрока: " + data.getPlayerId(), e);
        }
    }
    
    private void loadPlayerData(PlayerData data) {
        File file = new File(dataFolder, data.getPlayerId().toString() + ".yml");
        
        if (!file.exists()) {
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        data.setRating(config.getInt("rating", 0));
        data.setPoints(config.getInt("points", 0));
        
        // Загружаем бонусы
        if (config.contains("bonuses")) {
            for (String bonusId : config.getConfigurationSection("bonuses").getKeys(false)) {
                int level = config.getInt("bonuses." + bonusId);
                data.getPurchasedBonuses().put(bonusId, level);
            }
        }
        
        // Загружаем сохраненный инвентарь
        if (config.contains("saved_inventory")) {
            ItemStack[] inventory = new ItemStack[36];
            for (String key : config.getConfigurationSection("saved_inventory").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < 36) {
                        inventory[slot] = config.getItemStack("saved_inventory." + slot);
                    }
                } catch (NumberFormatException ignored) {}
            }
            data.setOriginalInventory(inventory);
        }
        
        // Загружаем сохраненную броню
        if (config.contains("saved_armor")) {
            ItemStack[] armor = new ItemStack[4];
            for (String key : config.getConfigurationSection("saved_armor").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < 4) {
                        armor[slot] = config.getItemStack("saved_armor." + slot);
                    }
                } catch (NumberFormatException ignored) {}
            }
            data.setOriginalArmor(armor);
        }
    }
    
    private void loadAllData() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName().replace(".yml", "");
                    UUID playerId = UUID.fromString(fileName);
                    
                    PlayerData data = new PlayerData(playerId);
                    loadPlayerData(data);
                    playerDataMap.put(playerId, data);
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неверное имя файла данных: " + file.getName());
                }
            }
        }
    }
    
    public void saveAllData() {
        for (PlayerData data : playerDataMap.values()) {
            savePlayerData(data);
        }
    }
    
    public void removePlayerData(UUID playerId) {
        PlayerData data = playerDataMap.remove(playerId);
        if (data != null) {
            savePlayerData(data);
        }
    }
    
    public void removeCachedData(UUID playerId) {
        playerDataMap.remove(playerId);
    }
    
    public Map<UUID, PlayerData> getAllPlayerData() {
        return new HashMap<>(playerDataMap);
    }
    
    public int getLoadedPlayersCount() {
        return playerDataMap.size();
    }
    
    // Метод для получения топ игроков по рейтингу
    public List<PlayerData> getTopPlayers(int limit) {
        List<PlayerData> allPlayers = new ArrayList<>(playerDataMap.values());
        
        // Сортируем по рейтингу (по убыванию)
        allPlayers.sort((p1, p2) -> Integer.compare(p2.getRating(), p1.getRating()));
        
        // Возвращаем первые limit игроков
        int size = Math.min(limit, allPlayers.size());
        return allPlayers.subList(0, size);
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
        playerDataMap.put(playerId, data);
        savePlayerData(data);
        
        File file = new File(dataFolder, playerId.toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
    
    // Метод для поиска игрока по имени (для админских функций)
    public UUID findPlayerIdByName(String playerName) {
        for (PlayerData data : playerDataMap.values()) {
            String name = Bukkit.getOfflinePlayer(data.getPlayerId()).getName();
            if (name != null && name.equalsIgnoreCase(playerName)) {
                return data.getPlayerId();
            }
        }
        return null;
    }
}