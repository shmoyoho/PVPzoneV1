package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final Main plugin;
    private final Set<UUID> cooldown;
    
    public PlayerMoveListener(Main plugin) {
        this.plugin = plugin;
        this.cooldown = new HashSet<>();
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // Проверяем изменение блока (не каждое микродвижение)
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        // Проверяем кулдаун
        if (cooldown.contains(player.getUniqueId())) {
            return;
        }
        
        // Добавляем кулдаун
        cooldown.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                cooldown.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 5L);
        
        boolean wasInZone = plugin.getZoneManager().isPlayerInZone(player);
        boolean isInZone = plugin.getZoneManager().isInZone(to);
        
        // Игрок вошел в зону
        if (!wasInZone && isInZone) {
            onEnterZone(player);
        }
        // Игрок вышел из зоны
        else if (wasInZone && !isInZone) {
            onExitZone(player);
        }
    }
    
    private void onEnterZone(Player player) {
        plugin.getZoneManager().addPlayerToZone(player);
        
        // Сохраняем оригинальный инвентарь
        plugin.getPlayerDataManager().saveOriginalInventory(player);
        
        // Выдаем PvP набор
        plugin.getKitManager().applyKitToPlayer(player);
        
        // Показываем сообщение и топ
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Вы вошли в PvP зону!");
        player.sendMessage(ChatColor.GRAY + "Ваш инвентарь заменен на PvP набор");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        
        // Показываем топ игроков
        showTopPlayers(player);
        
        // Применяем бонусы
        applyBonuses(player);
    }
    
    private void onExitZone(Player player) {
        plugin.getZoneManager().removePlayerFromZone(player);
        
        // Восстанавливаем оригинальный инвентарь
        plugin.getPlayerDataManager().restoreOriginalInventory(player);
        
        // Убираем бонусы
        removeBonuses(player);
        
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Вы вышли из PvP зоны!");
        player.sendMessage(ChatColor.GRAY + "Ваш инвентарь восстановлен");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
    }
    
    private void showTopPlayers(Player player) {
        var topPlayers = plugin.getPlayerDataManager().getTopPlayers(10);
        
        player.sendMessage(ChatColor.GOLD + "══ Топ 10 игроков PvP ══");
        
        if (topPlayers.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Пока нет игроков в рейтинге");
            return;
        }
        
        for (int i = 0; i < Math.min(topPlayers.size(), 10); i++) {
            var playerData = topPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(playerData.getPlayerId()).getName();
            
            if (playerName == null) {
                playerName = "Неизвестный игрок";
            }
            
            ChatColor color;
            if (i == 0) color = ChatColor.GOLD;
            else if (i == 1) color = ChatColor.GRAY;
            else if (i == 2) color = ChatColor.DARK_GRAY;
            else color = ChatColor.WHITE;
            
            // Исправлено: явное преобразование чисел в строки
            String line = color.toString() + String.valueOf(i + 1) + ". " + playerName + 
                         ChatColor.GRAY + " - " + ChatColor.YELLOW + 
                         String.valueOf(playerData.getRating()) + " очков";
            
            player.sendMessage(line);
        }
    }
    
    private void applyBonuses(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        
        // Применяем бонус здоровья
        double healthBonus = playerData.getHealthBonus();
        if (healthBonus > 0) {
            player.setMaxHealth(20.0 + healthBonus);
            player.setHealth(Math.min(player.getHealth() + healthBonus, player.getMaxHealth()));
        }
        
        // Применяем бонус скорости
        double speedBonus = playerData.getSpeedBonus();
        if (speedBonus > 0) {
            float newSpeed = (float) Math.min(1.0, 0.2 + speedBonus);
            player.setWalkSpeed(newSpeed);
        }
        
        // Показываем информацию о бонусах
        if (healthBonus > 0 || speedBonus > 0) {
            player.sendMessage(ChatColor.GOLD + "════ Ваши бонусы ════");
            if (healthBonus > 0) {
                // Исправлено: явное преобразование чисел в строки
                player.sendMessage(ChatColor.GREEN + "• Дополнительное здоровье: " + 
                                 ChatColor.YELLOW + "+" + String.valueOf(healthBonus) + " сердец");
            }
            if (speedBonus > 0) {
                // Исправлено: явное преобразование чисел в строки
                player.sendMessage(ChatColor.GREEN + "• Увеличение скорости: " + 
                                 ChatColor.YELLOW + "+" + String.valueOf(speedBonus * 100) + "%");
            }
        }
    }
    
    private void removeBonuses(Player player) {
        // Возвращаем стандартные значения
        player.setMaxHealth(20.0);
        player.setWalkSpeed(0.2f);
        
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        
        player.sendMessage(ChatColor.GOLD + "════ Статистика боя ════");
        // Исправлено: явное преобразование чисел в строки
        player.sendMessage(ChatColor.GRAY + "Ваш рейтинг: " + ChatColor.YELLOW + 
                         String.valueOf(playerData.getRating()) + " очков");
        player.sendMessage(ChatColor.GRAY + "Ваши очки для покупок: " + ChatColor.YELLOW + 
                         String.valueOf(playerData.getPoints()));
    }
}