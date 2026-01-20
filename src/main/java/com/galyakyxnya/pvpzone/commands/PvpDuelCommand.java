package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PvpDuelCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Random random = new Random();

    public PvpDuelCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            showHelp(player);
            return true;
        }

        String targetName = args[0];
        String zoneName = args.length > 1 ? args[1] : null;

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cИгрок не найден или оффлайн!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cНельзя вызвать самого себя!");
            return true;
        }

        // Если зона не указана, выбираем случайную
        if (zoneName == null) {
            zoneName = getRandomZoneName();
            if (zoneName == null) {
                player.sendMessage("§cНет доступных зон для дуэли!");
                return true;
            }
            player.sendMessage("§7Выбрана случайная зона: §e" + zoneName);
        }

        boolean success = plugin.getDuelManager().challengePlayer(player, target, zoneName);
        return true;
    }

    private String getRandomZoneName() {
        var zones = plugin.getZoneManager().getAllZones();
        if (zones.isEmpty()) {
            return null;
        }
        return zones.get(random.nextInt(zones.size())).getName();
    }

    private void showHelp(Player player) {
        player.sendMessage("§6══ Вызов на дуэль ══");
        player.sendMessage("§e/pvp <игрок> [зона]");
        player.sendMessage("§7Вызвать игрока на дуэль");
        player.sendMessage("");
        player.sendMessage("§c⚠ Ограничения:");
        player.sendMessage("§7• Нельзя вызывать находясь в PvP зоне");
        player.sendMessage("§7• Нельзя вызывать игрока в PvP зоне");
        player.sendMessage("");
        player.sendMessage("§7Примеры:");
        player.sendMessage("§e/pvp Notch §7- Вызвать в случайную зону");
        player.sendMessage("§e/pvp Notch arena1 §7- Вызвать в arena1");
        player.sendMessage("");
        player.sendMessage("§7Доступные зоны:");
        var zones = plugin.getZoneManager().getAllZones();
        if (zones.isEmpty()) {
            player.sendMessage("§cНет доступных зон!");
        } else {
            for (var zone : zones) {
                player.sendMessage("§e  • " + zone.getName());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // Онлайн игроки
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equals(sender.getName())) {
                    suggestions.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            // Зоны
            suggestions.addAll(plugin.getZoneManager().getZoneNames());
        }

        return suggestions;
    }
}