package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestKitCommand implements CommandExecutor {
    private final Main plugin;

    public TestKitCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("apply")) {
            plugin.getKitManager().applyKitToPlayer(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("check")) {
            boolean isSet = plugin.getKitManager().isKitSet();
            player.sendMessage(ChatColor.YELLOW + "PvP набор установлен: " +
                    (isSet ? ChatColor.GREEN + "ДА" : ChatColor.RED + "НЕТ"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("test")) {
            // Убираем или заменяем
            player.sendMessage(ChatColor.YELLOW + "Тестовый набор не реализован в этой версии");
            // plugin.getKitManager().setTestKit(); // УБРАТЬ ЭТУ СТРОКУ
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Тестирование PvP набора:");
        player.sendMessage(ChatColor.GRAY + "/testkit apply" + ChatColor.WHITE + " - Применить набор");
        player.sendMessage(ChatColor.GRAY + "/testkit check" + ChatColor.WHITE + " - Проверить набор");

        return true;
    }
}