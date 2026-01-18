package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.DuelData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    private final Main plugin;

    public PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // ===== ПЕРВОЕ: ПРОВЕРКА ДУЭЛИ =====
        var duelManager = plugin.getDuelManager();
        var duel = duelManager.getPlayerDuel(victim.getUniqueId());

        if (duel != null && duel.getState() == DuelData.DuelState.ACTIVE) {
            // Это смерть в дуэли
            if (killer != null && duel.isPlayerInDuel(killer.getUniqueId())) {
                // Победа в дуэли
                handleDuelWin(duel, killer, victim, event);
            } else {
                // Смерть по другим причинам (падение и т.д.)
                handleDuelDeath(duel, victim, event);
            }

            // Отменяем стандартную обработку
            event.setDeathMessage(null);
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        // ===== ВТОРОЕ: ПРОВЕРКА PvP ЗОНЫ =====
        if (killer != null && killer != victim) {
            boolean killerInZone = plugin.getZoneManager().isPlayerInZone(killer);
            boolean victimInZone = plugin.getZoneManager().isPlayerInZone(victim);

            if (killerInZone && victimInZone) {
                handlePvpKill(killer, victim, event);
                return;
            }
        }

        // ===== ТРЕТЬЕ: ЕСЛИ ЖЕРТВА БЫЛА В ЗОНЕ =====
        if (plugin.getZoneManager().isPlayerInZone(victim)) {
            // Восстанавливаем PvP набор после респавна
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (victim.isOnline() && plugin.getZoneManager().isPlayerInZone(victim)) {
                    plugin.getKitManager().applyKitToPlayer(victim);
                }
            }, 1L);
        }
    }

    // ===== ОБРАБОТКА ПОБЕДЫ В ДУЭЛИ =====
    private void handleDuelWin(DuelData duel, Player winner, Player loser, PlayerDeathEvent event) {
        // Убираем стандартное сообщение о смерти
        event.setDeathMessage(null);

        // Отправляем сообщения игрокам
        winner.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        winner.sendMessage(ChatColor.GREEN + "⚔ ВЫ ПОБЕДИЛИ В ДУЭЛИ!");
        winner.sendMessage(ChatColor.GRAY + "Противник: " + ChatColor.RED + loser.getName());
        winner.sendMessage(ChatColor.GOLD + "══════════════════════════════");

        loser.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        loser.sendMessage(ChatColor.RED + "☠ ВЫ ПРОИГРАЛИ ДУЭЛЬ!");
        loser.sendMessage(ChatColor.GRAY + "Победитель: " + ChatColor.GREEN + winner.getName());
        loser.sendMessage(ChatColor.GOLD + "══════════════════════════════");

        // Награда победителю
        var winnerData = plugin.getPlayerDataManager().getPlayerData(winner);
        winnerData.addRating(3); // +3 к рейтингу за победу в дуэли
        winnerData.addPoints(5); // +5 очков за победу
        plugin.getPlayerDataManager().savePlayerData(winnerData);

        // Сообщение для проигравшего
        var loserData = plugin.getPlayerDataManager().getPlayerData(loser);
        loser.sendMessage(ChatColor.GRAY + "Ваш рейтинг: " + ChatColor.YELLOW + loserData.getRating());
        loser.sendMessage(ChatColor.GRAY + "Ваши очки: " + ChatColor.YELLOW + loserData.getPoints());

        // Оповещение всего сервера
        Bukkit.broadcastMessage(ChatColor.GOLD + "[PvP] " + ChatColor.GREEN + winner.getName() +
                ChatColor.GOLD + " победил в дуэли против " +
                ChatColor.RED + loser.getName());

        // Завершаем дуэль
        plugin.getDuelManager().finishDuel(duel, DuelData.DuelState.FINISHED);
    }

    // ===== ОБРАБОТКА СМЕРТИ В ДУЭЛИ (без убийцы) =====
    private void handleDuelDeath(DuelData duel, Player victim, PlayerDeathEvent event) {
        Player opponent = duel.getOpponent(victim.getUniqueId());

        // Убираем стандартное сообщение о смерти
        event.setDeathMessage(null);

        victim.sendMessage(ChatColor.RED + "☠ Вы умерли во время дуэли!");

        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(ChatColor.GREEN + "⚔ Ваш противник " +
                    ChatColor.RED + victim.getName() +
                    ChatColor.GREEN + " умер!");
            opponent.sendMessage(ChatColor.GRAY + "Дуэль завершена.");

            // Небольшая награда выжившему
            var opponentData = plugin.getPlayerDataManager().getPlayerData(opponent);
            opponentData.addRating(1);
            opponentData.addPoints(2);
            plugin.getPlayerDataManager().savePlayerData(opponentData);
            opponent.sendMessage(ChatColor.GREEN + "+1 к рейтингу, +2 очка за выживание в дуэли");
        }

        // Оповещение сервера
        Bukkit.broadcastMessage(ChatColor.GOLD + "[PvP] Дуэль завершена: " +
                ChatColor.RED + victim.getName() +
                ChatColor.GOLD + " умер");

        // Завершаем дуэль
        plugin.getDuelManager().finishDuel(duel, DuelData.DuelState.FINISHED);
    }

    // ===== ОБРАБОТКА ОБЫЧНОГО PvP УБИЙСТВА В ЗОНЕ =====
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
        killer.sendMessage(ChatColor.YELLOW + "+1 к рейтингу (Всего: " + killerData.getRating() + ")");
        killer.sendMessage(ChatColor.YELLOW + "+1 очко для покупок (Всего: " + killerData.getPoints() + ")");
        killer.sendMessage(ChatColor.GOLD + "══════════════════════════════");

        victim.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        victim.sendMessage(ChatColor.RED + "Вы проиграли в PvP!");
        victim.sendMessage(ChatColor.YELLOW + "Ваш рейтинг: " + victimData.getRating());
        victim.sendMessage(ChatColor.YELLOW + "Ваши очки: " + victimData.getPoints());
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

    // ===== ОБРАБОТКА КИЛЛСТРИКА =====
    private void handleKillStreak(Player killer, com.galyakyxnya.pvpzone.models.PlayerData killerData) {
        // Простая реализация: каждое 3е убийство дает дополнительное очко
        int currentKills = killerData.getRating(); // используем рейтинг как счетчик убийств

        if (currentKills % 3 == 0 && currentKills > 0) {
            killerData.addPoints(1); // дополнительное очко за каждые 3 убийства

            killer.sendMessage(ChatColor.GOLD + "══════════════════════════════");
            killer.sendMessage(ChatColor.LIGHT_PURPLE + "★ Киллстрик! ★");
            killer.sendMessage(ChatColor.YELLOW + "+1 дополнительное очко за " +
                    currentKills + " убийств подряд!");
            killer.sendMessage(ChatColor.GRAY + "Всего очков: " + ChatColor.YELLOW +
                    killerData.getPoints());
            killer.sendMessage(ChatColor.GOLD + "══════════════════════════════");

            plugin.getPlayerDataManager().savePlayerData(killerData);
        }
    }
}