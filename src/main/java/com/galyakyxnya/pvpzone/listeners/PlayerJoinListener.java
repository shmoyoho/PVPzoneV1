package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoinListener implements Listener {
    private final Main plugin;

    public PlayerJoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Восстанавливаем инвентарь только если игрок вышел в PvP-зоне (флаг restore_inventory_on_join)
        // Затем, если он всё ещё в зоне — выдаём PvP-набор (задержка, чтобы игрок успел оказаться на своей позиции)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            var playerData = plugin.getPlayerDataManager().getPlayerData(player);
            boolean hasSavedInv = playerData.getOriginalInventory().length > 0;
            boolean hasSavedArmor = hasAnyItem(playerData.getOriginalArmor());
            if (hasSavedInv || hasSavedArmor) {
                plugin.getPlayerDataManager().restoreOriginalInventory(player);
                plugin.getPlayerDataManager().clearRestoreInventoryOnJoinFlag(player.getUniqueId());
                // Проверяем зону с небольшой задержкой, чтобы игрок уже был на своей позиции в мире
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    var zone = plugin.getZoneManager().findZoneAtLocation(player.getLocation());
                    if (zone != null) {
                        var moveListener = plugin.getPlayerMoveListener();
                        if (moveListener != null) moveListener.applyZoneToPlayer(player, zone);
                    }
                }, 20L);
            }
        }, 1L);

        // Уведомляем если игрок лидер
        if (plugin.getLeaderEffectManager() != null) {
            plugin.getLeaderEffectManager().onPlayerJoin(player);
        }

        // Можно добавить уведомление о топе
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData.getRating() > 0) {
            player.sendMessage(Lang.get(plugin, "join_rating", "%rating%", String.valueOf(playerData.getRating())));
            player.sendMessage(Lang.get(plugin, "join_points", "%points%", String.valueOf(playerData.getPoints())));
        }
    }

    private static boolean hasAnyItem(ItemStack[] armor) {
        if (armor == null || armor.length == 0) return false;
        for (ItemStack stack : armor) {
            if (stack != null && !stack.getType().isAir()) return true;
        }
        return false;
    }
}