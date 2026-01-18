package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerDeathListener implements Listener {
    private final Main plugin;
    
    public PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Если убийца - другой игрок и оба в PvP зоне
        if (killer != null && killer != victim) {
            boolean killerInZone = plugin.getZoneManager().isPlayerInZone(killer);
            boolean victimInZone = plugin.getZoneManager().isPlayerInZone(victim);
            
            if (killerInZone && victimInZone) {
                handlePvpKill(killer, victim, event);
            }
        }
    }
    
    private void handlePvpKill(Player killer, Player victim, PlayerDeathEvent event) {
        // Получаем данные игроков
        var killerData = plugin.getPlayerDataManager().getPlayerData(killer);
        var victimData = plugin.getPlayerDataManager().getPlayerData(victim);
        
        // Начисляем очки
        killerData.addRating(1); // +1 к рейтингу
        killerData.addPoints(1); // +1 очко для покупок
        
        // Сохраняем данные
        plugin.getPlayerDataManager().savePlayerData(killerData);
        
        // Обновляем сообщение смерти
        event.setDeathMessage(ChatColor.GOLD + "[PvP] " + 
                            ChatColor.RED + victim.getName() + 
                            ChatColor.GRAY + " был убит игроком " + 
                            ChatColor.GREEN + killer.getName());
        
        // Отправляем сообщения
        killer.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        killer.sendMessage(ChatColor.GREEN + "Вы победили в PvP!");
        
        // Исправлено: явное преобразование чисел в строки
        killer.sendMessage(ChatColor.YELLOW + "+1 к рейтингу (Всего: " + String.valueOf(killerData.getRating()) + ")");
        killer.sendMessage(ChatColor.YELLOW + "+1 очко для покупок (Всего: " + String.valueOf(killerData.getPoints()) + ")");
        
        killer.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        
        victim.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        victim.sendMessage(ChatColor.RED + "Вы проиграли в PvP!");
        
        // Исправлено: явное преобразование чисел в строки
        victim.sendMessage(ChatColor.YELLOW + "Ваш рейтинг: " + String.valueOf(victimData.getRating()));
        victim.sendMessage(ChatColor.YELLOW + "Ваши очки: " + String.valueOf(victimData.getPoints()));
        
        victim.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        
        // Восстанавливаем PvP набор для жертвы после респавна
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline() && plugin.getZoneManager().isPlayerInZone(victim)) {
                plugin.getKitManager().applyKitToPlayer(victim);
            }
        }, 1L);
        
        // Отменяем дроп предметов в PvP зоне
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Отменяем потерю опыта
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        
        // Добавляем бонусы за серию убийств (киллстрик)
        handleKillStreak(killer, killerData);
    }
    
    private void handleKillStreak(Player killer, com.galyakyxnya.pvpzone.models.PlayerData killerData) {
        // Здесь можно добавить логику для киллстриков
        // Например, дополнительных очков за серию убийств
        
        // Простая реализация: каждое 3е убийство дает дополнительное очко
        int currentKills = killerData.getRating(); // используем рейтинг как счетчик убийств
        
        if (currentKills % 3 == 0 && currentKills > 0) {
            killerData.addPoints(1); // дополнительное очко за каждые 3 убийства
            
            // Исправлено: явное преобразование чисел в строки
            killer.sendMessage(ChatColor.GOLD + "══════════════════════════════");
            killer.sendMessage(ChatColor.LIGHT_PURPLE + "★ Киллстрик! ★");
            killer.sendMessage(ChatColor.YELLOW + "+1 дополнительное очко за " + 
                             String.valueOf(currentKills) + " убийств подряд!");
            killer.sendMessage(ChatColor.GRAY + "Всего очков: " + ChatColor.YELLOW + 
                             String.valueOf(killerData.getPoints()));
            killer.sendMessage(ChatColor.GOLD + "══════════════════════════════");
            
            plugin.getPlayerDataManager().savePlayerData(killerData);
        }
    }
}