package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final Main plugin;

    public PlayerQuitListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Если игрок был в зоне - восстанавливаем инвентарь и сохраняем его в БД для следующего входа
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            plugin.getPlayerDataManager().restoreOriginalInventory(player);
            plugin.getZoneManager().removePlayerFromZone(player);
        }

        // Всегда сохраняем текущий инвентарь в БД (на случай выхода из зоны или перезахода)
        plugin.getPlayerDataManager().saveInventoryToDatabase(player);

        // Удаляем из трекера зон
        PlayerMoveListener moveListener = plugin.getPlayerMoveListener();
        if (moveListener != null) {
            moveListener.removePlayer(player.getUniqueId());
        }

        // Сохраняем данные
        plugin.getPlayerDataManager().savePlayerData(
                plugin.getPlayerDataManager().getPlayerData(player)
        );
    }
}