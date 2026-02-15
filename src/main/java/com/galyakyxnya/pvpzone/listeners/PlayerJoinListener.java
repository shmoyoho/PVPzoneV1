package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
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

        // Восстанавливаем инвентарь из БД на следующий тик (после полной загрузки игрока)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            var playerData = plugin.getPlayerDataManager().getPlayerData(player);
            boolean hasSavedInv = playerData.getOriginalInventory().length > 0;
            boolean hasSavedArmor = hasAnyItem(playerData.getOriginalArmor());
            if (hasSavedInv || hasSavedArmor) {
                plugin.getPlayerDataManager().restoreOriginalInventory(player);
            }
        }, 1L);

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

    private static boolean hasAnyItem(ItemStack[] armor) {
        if (armor == null || armor.length == 0) return false;
        for (ItemStack stack : armor) {
            if (stack != null && !stack.getType().isAir()) return true;
        }
        return false;
    }
}