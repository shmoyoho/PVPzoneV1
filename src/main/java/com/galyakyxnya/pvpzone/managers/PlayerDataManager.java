package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.database.DatabaseManager;
import com.galyakyxnya.pvpzone.models.PlayerData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.Base64;

public class PlayerDataManager {
    private static final Gson GSON = new Gson();
    private static final Type LIST_MAP_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

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

            // Загружаем сохранённый инвентарь (для восстановления после перезахода/краша)
            try (ResultSet invRs = databaseManager.getPlayerInventorySync(data.getPlayerId().toString())) {
                if (invRs.next()) {
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

    /** Сохраняет текущий инвентарь игрока в БД (для восстановления при следующем входе) */
    public void saveInventoryToDatabase(Player player) {
        if (player == null) return;
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        String invData = serializeItemStacks(contents);
        String armorData = serializeItemStacks(armor);
        if (!invData.isEmpty() || !armorData.isEmpty()) {
            databaseManager.savePlayerInventoryAsync(player.getUniqueId().toString(), invData, armorData);
        }
    }

    /** Сохраняет оригинальный инвентарь из PlayerData в БД (вызывать при входе в PvP зону) */
    public void saveOriginalInventoryToDatabase(Player player) {
        if (player == null) return;
        PlayerData data = getPlayerData(player);
        ItemStack[] inv = data.getOriginalInventory();
        ItemStack[] armor = data.getOriginalArmor();
        if ((inv == null || inv.length == 0) && (armor == null || armor.length == 0)) return;
        String invData = serializeItemStacks(inv != null ? inv : new ItemStack[0]);
        String armorData = serializeItemStacks(armor != null ? armor : new ItemStack[0]);
        databaseManager.savePlayerInventoryAsync(player.getUniqueId().toString(), invData, armorData);
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

    // Сохранение инвентаря при входе в зону (в память и в БД для защиты от краша)
    public void saveOriginalInventory(Player player) {
        PlayerData data = getPlayerData(player);
        ItemStack[] inv = player.getInventory().getContents().clone();
        ItemStack[] armor = player.getInventory().getArmorContents().clone();
        data.setOriginalInventory(inv);
        data.setOriginalArmor(armor);
        persistInventoryToDb(data.getPlayerId().toString(), inv, armor);
    }

    /** Сохраняет инвентарь оффлайн-игрока в кэш и БД (например, вышедшего во время дуэли). */
    public void saveOriginalInventoryForOffline(UUID playerId, ItemStack[] inv, ItemStack[] armor) {
        PlayerData data = playerDataCache.get(playerId);
        if (data == null) {
            data = new PlayerData(playerId);
            playerDataCache.put(playerId, data);
        }
        data.setOriginalInventory(inv != null ? inv : new ItemStack[0]);
        data.setOriginalArmor(armor != null ? armor : new ItemStack[0]);
        persistInventoryToDb(playerId.toString(), inv, armor);
    }

    private void persistInventoryToDb(String playerUuid, ItemStack[] inv, ItemStack[] armor) {
        String invStr = serializeInventory(inv != null ? inv : new ItemStack[0]);
        String armorStr = serializeInventory(armor != null ? armor : new ItemStack[4]);
        if (invStr != null && armorStr != null) {
            databaseManager.savePlayerInventoryAsync(playerUuid, invStr, armorStr);
        }
    }

    // Восстановление инвентаря при выходе из зоны. Использует клоны, чтобы не затронуть сохранённые данные.
    public void restoreOriginalInventory(Player player) {
        PlayerData data = getPlayerData(player);
        ItemStack[] inv = data.getOriginalInventory();
        ItemStack[] armor = data.getOriginalArmor();

        if (inv != null && inv.length > 0) {
            ItemStack[] clone = new ItemStack[inv.length];
            for (int i = 0; i < inv.length; i++) {
                clone[i] = inv[i] != null ? inv[i].clone() : null;
            }
            player.getInventory().setContents(clone);
        } else {
            player.getInventory().clear();
        }

        if (armor != null && armor.length > 0) {
            ItemStack[] armorClone = new ItemStack[armor.length];
            for (int i = 0; i < armor.length; i++) {
                armorClone[i] = armor[i] != null ? armor[i].clone() : null;
            }
            player.getInventory().setArmorContents(armorClone);
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

    private String serializeInventory(ItemStack[] arr) {
        if (arr == null) return "[]";
        List<Map<String, Object>> list = new ArrayList<>(arr.length);
        for (ItemStack item : arr) {
            if (item != null && item.getType() != Material.AIR && !item.isEmpty()) {
                list.add(item.serialize());
            } else {
                list.add(null);
            }
        }
        return GSON.toJson(list);
    }

    private ItemStack[] deserializeInventory(String json, int size) {
        if (json == null || json.isEmpty() || "[]".equals(json.trim())) return null;
        try {
            List<Map<String, Object>> list = GSON.fromJson(json, LIST_MAP_TYPE);
            if (list == null) return null;
            ItemStack[] out = new ItemStack[size];
            for (int i = 0; i < Math.min(list.size(), size); i++) {
                Map<String, Object> map = list.get(i);
                if (map != null && !map.isEmpty()) {
                    Map<String, Object> fixed = fixMapForDeserialize(map);
                    out[i] = ItemStack.deserialize(fixed);
                }
            }
            return out;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка десериализации инвентаря", e);
            return null;
        }
    }

    private Map<String, Object> fixMapForDeserialize(Map<String, Object> map) {
        Map<String, Object> out = new HashMap<>(map);
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getValue() instanceof Number && !(e.getValue() instanceof Integer)) {
                out.put(e.getKey(), ((Number) e.getValue()).intValue());
            }
        }
        return out;
    }
}