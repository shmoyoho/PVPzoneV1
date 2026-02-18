package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpFixCommand implements CommandExecutor {
    private final Main plugin;

    public PvpFixCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Lang.get(plugin, "player_only"));
            return true;
        }

        Player player = (Player) sender;

        // 1. Выходим из зоны
        plugin.getZoneManager().removePlayerFromZone(player);

        // 2. Восстанавливаем инвентарь
        plugin.getPlayerDataManager().restoreOriginalInventory(player);

        // 3. Сбрасываем бонусы
        player.setMaxHealth(20.0);
        player.setWalkSpeed(0.2f);

        // 4. Очищаем трекер зоны
        plugin.getPlayerMoveListener().removePlayer(player.getUniqueId());

        player.sendMessage(Lang.get(plugin, "fix_reset"));
        player.sendMessage(Lang.get(plugin, "fix_restored"));

        return true;
    }
}