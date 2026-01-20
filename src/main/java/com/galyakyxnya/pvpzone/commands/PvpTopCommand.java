package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PvpTopCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private static final int PLAYERS_PER_PAGE = 10;
    
    public PvpTopCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;
        
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Использование: /pvptop [страница]");
                return true;
            }
        }
        
        showTopPlayers(sender, page);
        return true;
    }

    private void showTopPlayers(CommandSender sender, int page) {
        var topPlayers = plugin.getPlayerDataManager().getTopPlayers(100);

        if (topPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "══ Топ игроков PvP ══");
            sender.sendMessage(ChatColor.GRAY + "Пока нет игроков в рейтинге");
            return;
        }

        int totalPages = (int) Math.ceil((double) topPlayers.size() / PLAYERS_PER_PAGE);
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, topPlayers.size());

        sender.sendMessage(ChatColor.GOLD + "══ Топ игроков PvP ══");
        sender.sendMessage(ChatColor.GRAY + "Страница " + page + " из " + totalPages);
        sender.sendMessage("");

        for (int i = startIndex; i < endIndex; i++) {
            var playerData = topPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(playerData.getPlayerId()).getName();

            if (playerName == null) {
                playerName = "Неизвестный игрок";
            }

            ChatColor color;
            String rankSymbol = "";

            if (i == 0) {
                color = ChatColor.GOLD;
                rankSymbol = "★";
            } else if (i == 1) {
                color = ChatColor.GRAY;
                rankSymbol = "☆";
            } else if (i == 2) {
                color = ChatColor.DARK_GRAY;
                rankSymbol = "✦";
            } else {
                color = ChatColor.WHITE;
            }

            String line = color.toString() + (i + 1) + ". " + rankSymbol + " " + playerName +
                    ChatColor.GRAY + " - " + ChatColor.YELLOW +
                    String.valueOf(playerData.getRating()) + " очков" +
                    ChatColor.DARK_GRAY + " (" + String.valueOf(playerData.getPoints()) + " для покупок)";

            // Если отправитель - игрок, и это он сам, выделяем его
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.getUniqueId().equals(playerData.getPlayerId())) {
                    line = ChatColor.YELLOW + "▶ " + ChatColor.GOLD + String.valueOf(i + 1) + ". " + playerName +
                            ChatColor.GRAY + " - " + ChatColor.YELLOW +
                            String.valueOf(playerData.getRating()) + " очков" +
                            ChatColor.DARK_GRAY + " (" + String.valueOf(playerData.getPoints()) + " для покупок)";
                }
            }

            sender.sendMessage(line);
        }

        if (page < totalPages) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GRAY + "Используйте " + ChatColor.YELLOW +
                    "/pvptop " + String.valueOf(page + 1) + ChatColor.GRAY + " для следующей страницы");
        }

        // Показываем статистику отправителя, если это игрок
        if (sender instanceof Player) {
            Player player = (Player) sender;
            var playerData = plugin.getPlayerDataManager().getPlayerData(player);

            // Находим позицию в рейтинге
            int playerRank = -1;
            for (int i = 0; i < topPlayers.size(); i++) {
                if (topPlayers.get(i).getPlayerId().equals(player.getUniqueId())) {
                    playerRank = i + 1;
                    break;
                }
            }

            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "════ Ваша статистика ════");

            if (playerRank > 0) {
                sender.sendMessage(ChatColor.GRAY + "Ваше место: " + ChatColor.YELLOW +
                        String.valueOf(playerRank) + ChatColor.GRAY + "/" + String.valueOf(topPlayers.size()));
            } else {
                sender.sendMessage(ChatColor.GRAY + "Ваше место: " + ChatColor.YELLOW + "не в топе");
            }

            sender.sendMessage(ChatColor.GRAY + "Рейтинг: " + ChatColor.YELLOW +
                    String.valueOf(playerData.getRating()) + " очков");
            sender.sendMessage(ChatColor.GRAY + "Очки для покупок: " + ChatColor.YELLOW +
                    String.valueOf(playerData.getPoints()));

            // Показываем купленные бонусы
            if (!playerData.getPurchasedBonuses().isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Ваши бонусы:");
                for (var entry : playerData.getPurchasedBonuses().entrySet()) {
                    String bonusName = getBonusName(entry.getKey());
                    sender.sendMessage(ChatColor.DARK_GRAY + "  • " + ChatColor.GRAY +
                            bonusName + ": " + ChatColor.YELLOW +
                            "уровень " + String.valueOf(entry.getValue()));
                }
            }

            // Сообщаем о эффекте лидера
            if (playerRank == 1) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + "★ ВЫ ЛИДЕР РЕЙТИНГА! ★");
                sender.sendMessage(ChatColor.GRAY + "За вами следуют огоньки славы");
            }
        }
    }
    
    private String getBonusName(String bonusId) {
        switch (bonusId) {
            case "health": return "Дополнительное сердце";
            case "speed": return "Увеличение скорости";
            case "jump": return "Высокий прыжок";
            case "damage": return "Усиление урона";
            default: return bonusId;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            suggestions.add("1");
            suggestions.add("2");
            suggestions.add("3");
        }
        
        return suggestions;
    }
}