package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Команда закрепления набора за зоной: /pvpsetkit <зона> <набор>
 */
public class PvpSetKitCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public PvpSetKitCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pvpzone.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.GOLD + "══ Закрепление набора за зоной ══");
            sender.sendMessage(ChatColor.YELLOW + "Использование: /pvpsetkit <зона> <набор>");
            sender.sendMessage(ChatColor.GRAY + "  Разные наборы можно привязать к разным зонам.");
            sender.sendMessage(ChatColor.GRAY + "  Наборы: /pvpkit list");
            sender.sendMessage(ChatColor.GRAY + "  Зоны: /pvpzone list");
            return true;
        }

        String zoneName = args[0];
        String kitName = args[1];

        ZoneManager zoneManager = plugin.getZoneManager();
        ZoneManager.PvpZone zone = zoneManager.getZone(zoneName);

        if (zone == null) {
            sender.sendMessage(ChatColor.RED + "Зона '" + zoneName + "' не найдена! Список: /pvpzone list");
            return true;
        }

        if (!plugin.getKitManager().isKitSet(kitName)) {
            sender.sendMessage(ChatColor.RED + "Набор '" + kitName + "' не найден! Создайте: /pvpkit " + kitName);
            sender.sendMessage(ChatColor.GRAY + "Список наборов: /pvpkit list");
            return true;
        }

        boolean success = zoneManager.setZoneKit(zoneName, kitName);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Для зоны '" + zoneName + "' установлен набор '" + kitName + "'.");
        } else {
            sender.sendMessage(ChatColor.RED + "✗ Не удалось установить набор.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("pvpzone.admin")) return out;

        String part = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length == 1) {
            out.addAll(plugin.getZoneManager().getZoneNames());
        } else if (args.length == 2) {
            out.addAll(plugin.getKitManager().getKitNames());
        }

        return out.stream()
                .filter(s -> s.toLowerCase().startsWith(part))
                .collect(Collectors.toList());
    }
}
