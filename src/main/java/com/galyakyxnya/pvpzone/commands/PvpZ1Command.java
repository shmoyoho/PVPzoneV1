package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpZ1Command implements CommandExecutor {
    private final Main plugin;

    public PvpZ1Command(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько игрок может использовать эту команду!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpzone.admin")) {
            player.sendMessage("§cНет прав!");
            return true;
        }

        Location location = player.getLocation();

        // Сохраняем во временный конфиг
        plugin.getConfig().set("temp.pos1.world", location.getWorld().getName());
        plugin.getConfig().set("temp.pos1.x", location.getX());
        plugin.getConfig().set("temp.pos1.y", location.getY()); // Сохраняем Y для отображения
        plugin.getConfig().set("temp.pos1.z", location.getZ());
        plugin.saveConfig();

        player.sendMessage("§a══════════════════════════════");
        player.sendMessage("§aПервая точка установлена!");
        player.sendMessage("§7Координаты: §eX: " + String.format("%.1f", location.getX()) +
                " §7| §eY: " + String.format("%.1f", location.getY()) +
                " §7| §eZ: " + String.format("%.1f", location.getZ()));
        player.sendMessage("§7Мир: §e" + location.getWorld().getName());
        player.sendMessage("§a══════════════════════════════");
        player.sendMessage("§7Теперь установите вторую точку §e/pvpz2");
        player.sendMessage("§7Затем создайте зону: §e/pvpzone create <название>");

        return true;
    }
}