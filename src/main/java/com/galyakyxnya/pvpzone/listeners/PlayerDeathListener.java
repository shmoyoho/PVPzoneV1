package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.DuelData;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
            Location killerLoc = killer.getLocation();
            Location victimLoc = victim.getLocation();

            var killerZone = plugin.getZoneManager().findZoneAtLocation(killerLoc);
            var victimZone = plugin.getZoneManager().findZoneAtLocation(victimLoc);

            // Проверяем, что оба в зонах
            if (killerZone != null && victimZone != null) {
                // Проверяем, что в одной зоне
                if (killerZone.getName().equals(victimZone.getName())) {
                    handlePvpKill(killer, victim, event);
                    return;
                }
            }
        }

        // ===== ТРЕТЬЕ: ЕСЛИ ЖЕРТВА БЫЛА В ЗОНЕ =====
        Location victimLoc = victim.getLocation();
        var victimZone = plugin.getZoneManager().findZoneAtLocation(victimLoc);
        if (victimZone != null) {
            // Восстанавливаем PvP набор после респавна
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (victim.isOnline()) {
                    // Проверяем, все ли еще в зоне
                    var currentZone = plugin.getZoneManager().findZoneAtLocation(victim.getLocation());
                    if (currentZone != null) {
                        plugin.getKitManager().applyZoneKit(victim, currentZone.getName());
                    }
                }
            }, 1L);
        }
    }

    // ===== ОБРАБОТКА ПОБЕДЫ В ДУЭЛИ =====
    private void handleDuelWin(DuelData duel, Player winner, Player loser, PlayerDeathEvent event) {
        // Убираем стандартное сообщение о смерти
        event.setDeathMessage(null);

        // Отправляем сообщения игрокам
        winner.sendMessage(Lang.get(plugin, "zone_enter_title"));
        winner.sendMessage(Lang.get(plugin, "death_duel_winner"));
        winner.sendMessage(Lang.get(plugin, "death_duel_loser_opponent") + ChatColor.RED + loser.getName());
        winner.sendMessage(Lang.get(plugin, "zone_enter_title"));

        loser.sendMessage(Lang.get(plugin, "zone_enter_title"));
        loser.sendMessage(Lang.get(plugin, "death_duel_loser"));
        loser.sendMessage(Lang.get(plugin, "death_duel_loser_opponent") + ChatColor.GREEN + winner.getName());
        loser.sendMessage(Lang.get(plugin, "zone_enter_title"));

        // Награда победителю
        var winnerData = plugin.getPlayerDataManager().getPlayerData(winner);
        winnerData.addRating(3); // +3 к рейтингу за победу в дуэли
        winnerData.addPoints(5); // +5 очков за победу
        plugin.getPlayerDataManager().savePlayerData(winnerData);
        if (plugin.getLeaderEffectManager() != null) plugin.getLeaderEffectManager().invalidateLeaderCache();

        // Сообщение для проигравшего
        var loserData = plugin.getPlayerDataManager().getPlayerData(loser);
        loser.sendMessage(Lang.get(plugin, "death_rating_loser") + ChatColor.YELLOW + loserData.getRating() + Lang.get(plugin, "death_rating_points"));
        loser.sendMessage(Lang.get(plugin, "join_points", "%points%", String.valueOf(loserData.getPoints())));

        Bukkit.broadcastMessage(Lang.get(plugin, "death_duel_broadcast", "%winner%", winner.getName(), "%loser%", loser.getName()));

        // Завершаем дуэль
        plugin.getDuelManager().finishDuel(duel, DuelData.DuelState.FINISHED);
    }

    // ===== ОБРАБОТКА СМЕРТИ В ДУЭЛИ (без убийцы) =====
    private void handleDuelDeath(DuelData duel, Player victim, PlayerDeathEvent event) {
        Player opponent = duel.getOpponent(victim.getUniqueId());

        // Убираем стандартное сообщение о смерти
        event.setDeathMessage(null);

        victim.sendMessage(Lang.get(plugin, "death_duel_death"));

        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(Lang.get(plugin, "death_duel_opponent_died", "%name%", victim.getName()));
            opponent.sendMessage(Lang.get(plugin, "death_duel_ended"));

            var opponentData = plugin.getPlayerDataManager().getPlayerData(opponent);
            opponentData.addRating(1);
            opponentData.addPoints(2);
            plugin.getPlayerDataManager().savePlayerData(opponentData);
            if (plugin.getLeaderEffectManager() != null) plugin.getLeaderEffectManager().invalidateLeaderCache();
            opponent.sendMessage(Lang.get(plugin, "death_opponent_reward"));
        }

        Bukkit.broadcastMessage(Lang.get(plugin, "death_duel_draw_broadcast", "%name%", victim.getName()));

        // Завершаем дуэль
        plugin.getDuelManager().finishDuel(duel, DuelData.DuelState.FINISHED);
    }

    // ===== ОБРАБОТКА ОБЫЧНОГО PvP УБИЙСТВА В ЗОНЕ =====
    private void handlePvpKill(Player killer, Player victim, PlayerDeathEvent event) {
        // Используем НАДЕЖНУЮ проверку зоны по координатам
        Location killerLoc = killer.getLocation();
        Location victimLoc = victim.getLocation();

        var killerZone = plugin.getZoneManager().findZoneAtLocation(killerLoc);
        var victimZone = plugin.getZoneManager().findZoneAtLocation(victimLoc);

        // Проверяем, что оба в одной зоне
        if (killerZone == null || victimZone == null) {
            return;
        }

        // Получаем данные игроков
        var killerData = plugin.getPlayerDataManager().getPlayerData(killer);
        var victimData = plugin.getPlayerDataManager().getPlayerData(victim);

        // Начисляем очки
        killerData.addRating(1); // +1 к рейтингу
        killerData.addPoints(1); // +1 очко для покупок

        // Сохраняем данные УБИЙЦЫ
        plugin.getPlayerDataManager().savePlayerData(killerData);
        if (plugin.getLeaderEffectManager() != null) plugin.getLeaderEffectManager().invalidateLeaderCache();

        event.setDeathMessage(Lang.get(plugin, "death_pvp_kill", "%victim%", victim.getName(), "%killer%", killer.getName()));

        killer.sendMessage(Lang.get(plugin, "zone_enter_title"));
        killer.sendMessage(Lang.get(plugin, "death_pvp_win"));
        killer.sendMessage(Lang.get(plugin, "death_pvp_win_rating", "%rating%", String.valueOf(killerData.getRating())));
        killer.sendMessage(Lang.get(plugin, "zone_enter_title"));

        victim.sendMessage(Lang.get(plugin, "zone_enter_title"));
        victim.sendMessage(Lang.get(plugin, "death_pvp_lose"));
        victim.sendMessage(Lang.get(plugin, "death_pvp_victim_rating", "%rating%", String.valueOf(victimData.getRating())));
        victim.sendMessage(Lang.get(plugin, "death_pvp_victim_points", "%points%", String.valueOf(victimData.getPoints())));
        victim.sendMessage(Lang.get(plugin, "zone_enter_title"));

        // Восстанавливаем PvP набор для жертвы после респавна
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isOnline()) {
                // Проверяем, все ли еще в зоне
                var currentZone = plugin.getZoneManager().findZoneAtLocation(victim.getLocation());
                if (currentZone != null) {
                    plugin.getKitManager().applyZoneKit(victim, currentZone.getName());
                }
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
        int currentRating = killerData.getRating();

        if (currentRating % 3 == 0 && currentRating > 0) {
            killerData.addPoints(1); // дополнительное очко за каждые 3 убийства

            killer.sendMessage(Lang.get(plugin, "zone_enter_title"));
            killer.sendMessage(Lang.get(plugin, "killstreak_title"));
            killer.sendMessage(Lang.get(plugin, "killstreak_bonus", "%count%", String.valueOf(currentRating)));
            killer.sendMessage(Lang.get(plugin, "killstreak_points", "%points%", String.valueOf(killerData.getPoints())));
            killer.sendMessage(Lang.get(plugin, "zone_enter_title"));

            // Сохраняем данные после начисления киллстрика
            plugin.getPlayerDataManager().savePlayerData(killerData);
        }
    }
}