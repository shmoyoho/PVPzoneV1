package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.DuelData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerQuitListener implements Listener {
    private final Main plugin;

    public PlayerQuitListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 1. Сначала проверяем дуэль: инвентарь хранится в DuelData, не в PlayerData
        var duelManager = plugin.getDuelManager();
        if (duelManager != null) {
            DuelData duel = duelManager.getPlayerDuel(player.getUniqueId());
            if (duel != null) {
                plugin.getLogger().warning("[QUIT] Игрок " + player.getName() + " вышел во время дуэли — завершаем дуэль и восстанавливаем инвентарь из DuelData");
                duelManager.finishDuel(duel, DuelData.DuelState.CANCELLED);
                saveAndCleanup(player);
                return;
            }
        }

        // 2. Игрок в зоне (не в дуэли) — восстанавливаем из PlayerData
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            plugin.getPlayerDataManager().restoreOriginalInventory(player);
            plugin.getZoneManager().removePlayerFromZone(player);
        } else {
            // 3. Не в зоне: очищаем сохранённый PvP-набор в PlayerData, чтобы не восстанавливать его потом по ошибке
            var playerData = plugin.getPlayerDataManager().getPlayerData(player);
            ItemStack[] savedInv = playerData.getOriginalInventory();
            ItemStack[] savedArmor = playerData.getOriginalArmor();
            if (savedInv != null && savedInv.length > 0 && isPvPKitApplied(savedInv, savedArmor != null ? savedArmor : new ItemStack[0])) {
                playerData.setOriginalInventory(new ItemStack[0]);
                playerData.setOriginalArmor(new ItemStack[0]);
            }
        }

        // Всегда сохраняем текущий инвентарь в БД (на случай выхода из зоны или перезахода)
        plugin.getPlayerDataManager().saveInventoryToDatabase(player);

        // 4. Удаляем из трекера зон
        PlayerMoveListener moveListener = plugin.getPlayerMoveListener();
        if (moveListener != null) {
            moveListener.removePlayer(player.getUniqueId());
        }

        saveAndCleanup(player);
    }

    private void saveAndCleanup(Player player) {
        try {
            plugin.getPlayerDataManager().savePlayerData(
                    plugin.getPlayerDataManager().getPlayerData(player)
            );
        } catch (Exception e) {
            plugin.getLogger().warning("[QUIT] Ошибка сохранения данных для " + player.getName() + ": " + e.getMessage());
        }
    }

    private boolean isPvPKitApplied(ItemStack[] inventory, ItemStack[] armor) {
        int total = 0, pvp = 0;
        for (ItemStack item : inventory) {
            if (item != null && item.getType() != Material.AIR) {
                total++;
                if (isPvPItem(item)) pvp++;
            }
        }
        for (ItemStack item : armor) {
            if (item != null && item.getType() != Material.AIR) {
                total++;
                if (isPvPItem(item)) pvp++;
            }
        }
        if (total == 0) return false;
        float ratio = (float) pvp / total;
        return ratio > 0.5f || (total <= 4 && pvp > 0);
    }

    private boolean isPvPItem(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        String name = type.name();
        // Оружие
        if (name.contains("SWORD") || name.contains("_AXE") || name.contains("BOW") || name.contains("CROSSBOW") || type == Material.TRIDENT)
            return true;
        // Броня
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS"))
            return true;
        // Зелья и прочее
        if (name.contains("POTION") || type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE
                || type == Material.SHIELD || type == Material.TOTEM_OF_UNDYING || type == Material.ENDER_PEARL)
            return true;
        return false;
    }
}
