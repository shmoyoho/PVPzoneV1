package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpResetCommand implements CommandExecutor {
    private final Main plugin;

    public PvpResetCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        // Сбрасываем состояние
        plugin.getZoneManager().removePlayerFromZone(player);
        plugin.getPlayerDataManager().restoreOriginalInventory(player);

        // Сбрасываем бонусы
        player.setMaxHealth(20.0);
        player.setHealth(Math.min(player.getHealth(), 20.0));
        player.setWalkSpeed(0.2f);

        player.sendMessage(ChatColor.GREEN + "✓ Ваше PvP состояние сброшено!");
        player.sendMessage(ChatColor.GRAY + "Инвентарь восстановлен, бонусы убраны");

        return true;
    }
}