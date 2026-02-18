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

        // Если игрок вышел, находясь в PvP-зоне: восстанавливаем мирской инвентарь и ставим флаг «восстановить при входе»
        // (мирской инвентарь уже сохранён в БД при входе в зону)
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            plugin.getPlayerDataManager().restoreOriginalInventory(player);
            plugin.getZoneManager().removePlayerFromZone(player);
            plugin.getPlayerDataManager().setRestoreInventoryOnJoinTrue(player.getUniqueId());
        } else {
            // Вышел не в зоне — инвентарем управляет сервер: очищаем сохранённый инвентарь и флаг
            plugin.getPlayerDataManager().clearPlayerInventoryAndRestoreFlag(player.getUniqueId());
        }

        // Удаляем из трекера зон
        PlayerMoveListener moveListener = plugin.getPlayerMoveListener();
        if (moveListener != null) {
            moveListener.removePlayer(player.getUniqueId());
        }

        // Сохраняем данные
        plugin.getPlayerDataManager().savePlayerData(
                plugin.getPlayerDataManager().getPlayerData(player)
        );

        // Сбрасываем кэш, чтобы при следующем входе инвентарь загрузился из БД (то, что только что сохранили),
        // а не подменялся старым «оригинальным» инвентарём из кэша (например, с момента входа в зону).
        plugin.getPlayerDataManager().removeCachedData(player.getUniqueId());
    }
}