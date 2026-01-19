package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import com.galyakyxnya.pvpzone.models.DuelData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final Main plugin;
    private final Map<UUID, String> playerZones = new HashMap<>();

    public PlayerMoveListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to == null) return;

        // Проверяем только если игрок перешел на другой блок
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // ===== ПРОВЕРКА ЗАМОРОЗКИ В ДУЭЛИ =====
        var duelManager = plugin.getDuelManager();
        var duel = duelManager.getPlayerDuel(player.getUniqueId());

        if (duel != null && duel.getState() == DuelData.DuelState.ACTIVE) {
            // Проверяем эффекты замедления (заморозки во время отсчета)
            if (player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                PotionEffect slowness = player.getPotionEffect(PotionEffectType.SLOWNESS);
                if (slowness != null && slowness.getAmplifier() >= 255) {
                    // Игрок заморожен (уровень 255+ = наш эффект заморозки)
                    // Блокируем движение, но позволяем поворачиваться
                    if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                        event.setCancelled(true);

                        // Можно позволить поворот головы
                        Location newLocation = from.clone();
                        newLocation.setYaw(to.getYaw());
                        newLocation.setPitch(to.getPitch());
                        player.teleport(newLocation);

                        return;
                    }
                }
            }
        }

        ZoneManager zoneManager = plugin.getZoneManager();
        ZoneManager.PvpZone currentZone = zoneManager.findZoneAtLocation(to);
        String currentZoneName = currentZone != null ? currentZone.getName() : null;

        String previousZoneName = playerZones.get(player.getUniqueId());

        // Если зона не изменилась - ничего не делаем
        if ((previousZoneName == null && currentZoneName == null) ||
                (previousZoneName != null && previousZoneName.equals(currentZoneName))) {
            return;
        }

        // Выход из предыдущей зоны
        if (previousZoneName != null) {
            ZoneManager.PvpZone previousZone = zoneManager.getZone(previousZoneName);
            if (previousZone != null) {
                handleExitZone(player, previousZone);
            }
        }

        // Вход в новую зону
        if (currentZone != null) {
            handleEnterZone(player, currentZone);
        }

        // Обновляем запись
        playerZones.put(player.getUniqueId(), currentZoneName);
    }

    private void handleEnterZone(Player player, ZoneManager.PvpZone zone) {
        // Сохраняем инвентарь
        plugin.getPlayerDataManager().saveOriginalInventory(player);

        // Применяем набор для зоны
        boolean kitApplied = plugin.getKitManager().applyZoneKit(player, zone.getName());

        if (!kitApplied) {
            player.sendMessage("§c⚠ Набор для этой зоны не настроен!");
            player.sendMessage("§7Используйте: §e/pvpzone kit save " + zone.getName());
        }

        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Вы вошли в PvP зону '" + zone.getName() + "'!");
        player.sendMessage(ChatColor.GRAY + "Набор: " + ChatColor.WHITE + zone.getKitName());
        player.sendMessage(ChatColor.GRAY + "Бонусы: " +
                (zone.isBonusesEnabled() ? ChatColor.GREEN + "ВКЛ" : ChatColor.RED + "ВЫКЛ"));

        // Показываем рейтинг
        showPlayerRating(player);
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");

        // Применяем бонусы если включены
        if (zone.isBonusesEnabled()) {
            applyPlayerBonuses(player);
        }
    }

    private void handleExitZone(Player player, ZoneManager.PvpZone zone) {
        // Восстанавливаем инвентарь
        plugin.getPlayerDataManager().restoreOriginalInventory(player);

        // Убираем бонусы
        removePlayerBonuses(player);

        // Отправляем сообщение
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Вы вышли из PvP зоны!");
        player.sendMessage(ChatColor.GRAY + "Название: " + ChatColor.WHITE + zone.getName());

        // Показываем итоговую статистику
        showExitStats(player);

        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
    }

    private void showPlayerRating(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);

        // Находим позицию в топе
        var topPlayers = plugin.getPlayerDataManager().getTopPlayers(100);
        int playerRank = -1;
        for (int i = 0; i < topPlayers.size(); i++) {
            if (topPlayers.get(i).getPlayerId().equals(player.getUniqueId())) {
                playerRank = i + 1;
                break;
            }
        }

        player.sendMessage(ChatColor.GRAY + "Ваш рейтинг: " +
                ChatColor.YELLOW + playerData.getRating() + " очков");

        if (playerRank > 0) {
            player.sendMessage(ChatColor.GRAY + "Место в топе: " +
                    ChatColor.YELLOW + playerRank + ChatColor.GRAY + "/" +
                    ChatColor.YELLOW + topPlayers.size());
        }

        player.sendMessage(ChatColor.GRAY + "Очки для покупок: " +
                ChatColor.YELLOW + playerData.getPoints());
    }

    private void showExitStats(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);

        player.sendMessage(ChatColor.GRAY + "Ваша статистика:");
        player.sendMessage(ChatColor.GRAY + "  Рейтинг: " + ChatColor.YELLOW +
                playerData.getRating() + " очков");
        player.sendMessage(ChatColor.GRAY + "  Очки для покупок: " + ChatColor.YELLOW +
                playerData.getPoints());

        if (!playerData.getPurchasedBonuses().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  Активные бонусы: " + ChatColor.YELLOW +
                    playerData.getPurchasedBonuses().size());
        }
    }

    private void applyPlayerBonuses(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);

        // Применяем бонусы здоровья
        double healthBonus = playerData.getHealthBonus();
        if (healthBonus > 0) {
            player.setMaxHealth(20.0 + healthBonus);
            player.setHealth(Math.min(player.getHealth() + healthBonus, player.getMaxHealth()));
        }

        // Применяем бонусы скорости
        double speedBonus = playerData.getSpeedBonus();
        if (speedBonus > 0) {
            float newSpeed = (float) Math.min(1.0, 0.2 + speedBonus);
            player.setWalkSpeed(newSpeed);
        }
    }

    private void removePlayerBonuses(Player player) {
        // Сбрасываем бонусы
        player.setMaxHealth(20.0);
        player.setWalkSpeed(0.2f);
    }

    // Метод для очистки при выходе игрока
    public void removePlayer(UUID playerId) {
        playerZones.remove(playerId);
    }
}