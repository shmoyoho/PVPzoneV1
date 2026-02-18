package com.galyakyxnya.pvpzone.utils;

import com.galyakyxnya.pvpzone.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ChatUtils {

    public static void sendClickableDuelInvite(Main plugin, Player target, Player challenger, String zoneName) {
        String zoneDisplay = (zoneName != null) ? zoneName : Lang.get(plugin, "duel_random_zone");

        TextComponent mainMessage = new TextComponent(Lang.get(plugin, "zone_enter_title") + "\n");
        TextComponent inviteLine = new TextComponent(Lang.get(plugin, "duel_invite_line") + "\n");
        TextComponent fromLine = new TextComponent(Lang.get(plugin, "duel_invite_from") + ChatColor.GREEN + challenger.getName() + "\n");
        TextComponent zoneLine = new TextComponent(Lang.get(plugin, "duel_invite_zone") + ChatColor.YELLOW + zoneDisplay + "\n\n");

        TextComponent acceptButton = new TextComponent(Lang.get(plugin, "duel_accept_btn"));
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pvpaccept"));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Lang.get(plugin, "duel_accept_hover"))));

        TextComponent space = new TextComponent("  ");

        TextComponent denyButton = new TextComponent(Lang.get(plugin, "duel_deny_btn"));
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pvpdeny"));
        denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Lang.get(plugin, "duel_deny_hover"))));

        TextComponent separator = new TextComponent("\n" + Lang.get(plugin, "zone_enter_title"));

        target.spigot().sendMessage(mainMessage, inviteLine, fromLine, zoneLine, acceptButton, space, denyButton, separator);

        target.sendMessage(Lang.get(plugin, "duel_or_use"));
        target.sendMessage(Lang.get(plugin, "duel_accept_cmd"));
        target.sendMessage(Lang.get(plugin, "duel_deny_cmd"));
    }

    public static void sendClickableMessage(Player player, String message, String hoverText, String command) {
        TextComponent component = new TextComponent(message);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(hoverText)));
        player.spigot().sendMessage(component);
    }
}