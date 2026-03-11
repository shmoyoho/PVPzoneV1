package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Команда создания набора: /pvpkit <название> — сохранить текущий инвентарь как набор.
 * /pvpkit list — список наборов.
 */
public class PvpKitCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public PvpKitCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpzone.admin")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "══ Создание набора ══");
            player.sendMessage(ChatColor.YELLOW + "Использование: /pvpkit <название>");
            player.sendMessage(ChatColor.GRAY + "  Соберите нужные предметы в инвентаре и введите команду — набор сохранится под указанным именем.");
            player.sendMessage(ChatColor.YELLOW + "/pvpkit list");
            player.sendMessage(ChatColor.GRAY + "  Показать список всех наборов.");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            var names = plugin.getKitManager().getKitNames();
            if (names.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Нет сохранённых наборов. Создайте: /pvpkit <название>");
                return true;
            }
            player.sendMessage(ChatColor.GOLD + "══ Наборы ══");
            player.sendMessage(ChatColor.WHITE + String.join(", ", names));
            return true;
        }

        String kitName = args[0].trim();
        if (kitName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Укажите название набора: /pvpkit <название>");
            return true;
        }

        boolean success = plugin.getKitManager().saveKit(kitName, player);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "✓ Набор '" + kitName + "' создан! Теперь привяжите его к зоне: /pvpsetkit <зона> " + kitName);
        } else {
            player.sendMessage(ChatColor.RED + "✗ Не удалось создать набор. Проверьте название.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("pvpzone.admin")) return out;

        if (args.length == 1) {
            out.add("list");
            out.addAll(plugin.getKitManager().getKitNames());
            return out.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return out;
    }
}
