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
        
        // Если игрок был в PvP зоне, восстанавливаем инвентарь
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            plugin.getPlayerDataManager().restoreOriginalInventory(player);
            plugin.getZoneManager().removePlayerFromZone(player);
        }
        
        // Сохраняем данные игрока
        plugin.getPlayerDataManager().savePlayerData(
            plugin.getPlayerDataManager().getPlayerData(player)
        );
        
        // Удаляем из памяти
        plugin.getPlayerDataManager().removePlayerData(player.getUniqueId());
    }
}