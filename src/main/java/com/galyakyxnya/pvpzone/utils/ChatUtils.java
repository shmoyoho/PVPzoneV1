package com.galyakyxnya.pvpzone.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ChatUtils {

    public static void sendClickableDuelInvite(Player target, Player challenger, String zoneName) {
        // Если зона null, заменяем на текст
        String zoneDisplay = (zoneName != null) ? zoneName : "случайная зона";

        TextComponent mainMessage = new TextComponent(ChatColor.GOLD + "══════════════════════════════\n");

        // Основное сообщение
        TextComponent inviteLine = new TextComponent(ChatColor.YELLOW + "⚔ Вам вызов на дуэль!\n");
        TextComponent fromLine = new TextComponent(ChatColor.GRAY + "От: " + ChatColor.GREEN + challenger.getName() + "\n");
        TextComponent zoneLine = new TextComponent(ChatColor.GRAY + "Зона: " + ChatColor.YELLOW + zoneDisplay + "\n\n");

        // Кнопка Принять
        TextComponent acceptButton = new TextComponent(ChatColor.GREEN + "[✓ ПРИНЯТЬ]");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pvpaccept"));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(ChatColor.GREEN + "Нажмите чтобы принять вызов")));

        TextComponent space = new TextComponent("  ");

        // Кнопка Отклонить
        TextComponent denyButton = new TextComponent(ChatColor.RED + "[✗ ОТКЛОНИТЬ]");
        denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pvpdeny"));
        denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(ChatColor.RED + "Нажмите чтобы отклонить вызов")));

        TextComponent separator = new TextComponent(ChatColor.GOLD + "\n══════════════════════════════");

        // Отправляем все компоненты
        target.spigot().sendMessage(
                mainMessage,
                inviteLine,
                fromLine,
                zoneLine,
                acceptButton,
                space,
                denyButton,
                separator
        );

        // Также отправляем текстовую версию для старых клиентов
        target.sendMessage(ChatColor.GRAY + "Или используйте команды:");
        target.sendMessage(ChatColor.GREEN + "/pvpaccept" + ChatColor.GRAY + " - Принять вызов");
        target.sendMessage(ChatColor.RED + "/pvpdeny" + ChatColor.GRAY + " - Отклонить вызов");
    }

    public static void sendClickableMessage(Player player, String message, String hoverText, String command) {
        TextComponent component = new TextComponent(message);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(hoverText)));
        player.spigot().sendMessage(component);
    }
}