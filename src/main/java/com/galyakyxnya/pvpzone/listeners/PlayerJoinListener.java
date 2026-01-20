package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final Main plugin;

    public PlayerJoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Уведомляем если игрок лидер
        if (plugin.getLeaderEffectManager() != null) {
            plugin.getLeaderEffectManager().onPlayerJoin(player);
        }

        // Можно добавить уведомление о топе
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData.getRating() > 0) {
            player.sendMessage("§7Ваш рейтинг: §e" + playerData.getRating() + " очков");
            player.sendMessage("§7Очки для покупок: §e" + playerData.getPoints());
        }
    }
}