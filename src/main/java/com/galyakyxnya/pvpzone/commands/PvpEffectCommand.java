package com.galyakyxnya.pvpzone.commands;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpEffectCommand implements CommandExecutor {
    private final Main plugin;

    public PvpEffectCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Lang.get(plugin, "effect_player_only"));
            return true;
        }
        Player p = (Player) sender;
        if (plugin.getLeaderEffectManager() == null) {
            p.sendMessage(Lang.get(plugin, "effect_disabled"));
            return true;
        }
        boolean nowDisabled = plugin.getLeaderEffectManager().toggleEffectFor(p);
        p.sendMessage(Lang.get(plugin, nowDisabled ? "effect_toggled_off" : "effect_toggled_on"));
        return true;
    }
}
