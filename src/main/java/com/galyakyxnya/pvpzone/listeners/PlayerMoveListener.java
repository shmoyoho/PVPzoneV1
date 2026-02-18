package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import com.galyakyxnya.pvpzone.utils.Lang;
import com.galyakyxnya.pvpzone.models.DuelData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
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

        // ===== КРИТИЧЕСКАЯ ПРОВЕРКА: ИГРОК В ДУЭЛИ =====
        // Если игрок в дуэли (любого состояния), НЕ обрабатываем вход/выход из зоны!
        // Этим занимается DuelManager
        var duelManager = plugin.getDuelManager();
        var duel = duelManager.getPlayerDuel(player.getUniqueId());

        if (duel != null) {
            // Игрок в дуэли - НИКАК не обрабатываем смену зон
            // Это предотвращает любые конфликты с восстановлением инвентаря
            return;
        }
        // ===== КОНЕЦ ПРОВЕРКИ =====

        // Проверяем только если игрок перешел на другой блок
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
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
        if (currentZoneName != null) {
            playerZones.put(player.getUniqueId(), currentZoneName);
        } else {
            playerZones.remove(player.getUniqueId());
        }
    }

    /**
     * Применяет PvP-зону к игроку: сохраняет текущий инвентарь как «оригинал», выдаёт набор, добавляет в трекинг.
     * Вызывается при входе в зону (move) и при входе на сервер, если игрок заспавнился в зоне (join после выхода в зоне).
     */
    public void applyZoneToPlayer(Player player, ZoneManager.PvpZone zone) {
        if (plugin.getDuelManager().getPlayerDuel(player.getUniqueId()) != null) return;

        ItemStack[] originalInventory = player.getInventory().getContents().clone();
        ItemStack[] originalArmor = player.getInventory().getArmorContents().clone();

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        playerData.setOriginalInventory(originalInventory);
        playerData.setOriginalArmor(originalArmor);
        plugin.getPlayerDataManager().saveOriginalInventoryToDatabase(player);

        if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine("Игрок " + player.getName() + " вошел в зону " + zone.getName());
        }

        boolean kitApplied = plugin.getKitManager().applyKitOnly(zone.getKitName(), player);
        if (!kitApplied) {
            player.sendMessage(Lang.get(plugin, "zone_no_kit"));
            plugin.getPlayerDataManager().restoreOriginalInventory(player);
            return;
        }

        player.sendMessage(Lang.get(plugin, "zone_enter_title"));
        player.sendMessage(Lang.get(plugin, "zone_enter", "%zone%", zone.getName()));
        player.sendMessage(Lang.get(plugin, "zone_kit", "%kit%", zone.getKitName()));
        showPlayerRating(player);
        player.sendMessage(Lang.get(plugin, "zone_enter_title"));

        if (zone.isBonusesEnabled()) applyPlayerBonuses(player);

        plugin.getZoneManager().addPlayerToZone(player);
        playerZones.put(player.getUniqueId(), zone.getName());
    }

    private void handleEnterZone(Player player, ZoneManager.PvpZone zone) {
        applyZoneToPlayer(player, zone);
    }

    private void handleExitZone(Player player, ZoneManager.PvpZone zone) {
        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Проверяем что игрок не в дуэли
        if (plugin.getDuelManager().getPlayerDuel(player.getUniqueId()) != null) {
            return;
        }

        // Убираем игрока из ZoneManager
        plugin.getZoneManager().removePlayerFromZone(player);

        // Восстанавливаем инвентарь при выходе из зоны
        plugin.getPlayerDataManager().restoreOriginalInventory(player);

        // Убираем бонусы
        removePlayerBonuses(player);

        // Отправляем сообщение
        player.sendMessage(Lang.get(plugin, "zone_exit_title"));
        player.sendMessage(Lang.get(plugin, "zone_exit"));
        player.sendMessage(Lang.get(plugin, "zone_exit_name", "%zone%", zone.getName()));

        // Показываем итоговую статистику
        showExitStats(player);

        player.sendMessage(Lang.get(plugin, "zone_exit_title"));
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

        player.sendMessage(Lang.get(plugin, "zone_rating", "%rating%", String.valueOf(playerData.getRating())));

        if (playerRank > 0) {
            player.sendMessage(Lang.get(plugin, "zone_rank", "%rank%", String.valueOf(playerRank), "%total%", String.valueOf(topPlayers.size())));
        }

        player.sendMessage(Lang.get(plugin, "zone_stats_points", "%points%", String.valueOf(playerData.getPoints())));
    }

    private void showExitStats(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);

        player.sendMessage(Lang.get(plugin, "zone_stats"));
        player.sendMessage(Lang.get(plugin, "zone_stats_rating", "%rating%", String.valueOf(playerData.getRating())));
        player.sendMessage(Lang.get(plugin, "zone_stats_points", "%points%", String.valueOf(playerData.getPoints())));

        if (!playerData.getPurchasedBonuses().isEmpty()) {
            player.sendMessage(Lang.get(plugin, "zone_stats_bonuses", "%count%", String.valueOf(playerData.getPurchasedBonuses().size())));
        }
    }

    private void applyPlayerBonuses(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);

        // Применяем ВСЕ бонусы
        double healthBonus = playerData.getHealthBonus();
        double speedBonus = playerData.getSpeedBonus();
        double jumpBonus = playerData.getJumpBonus();
        double damageBonus = playerData.getDamageBonus();

        boolean hasAnyBonus = false;

        // Бонус здоровья
        if (healthBonus > 0) {
            int extraHearts = playerData.getBonusLevel("health");
            player.setMaxHealth(20.0 + healthBonus);
            player.setHealth(Math.min(player.getHealth() + healthBonus, player.getMaxHealth()));
            String key = extraHearts > 1 ? "zone_bonus_health_plural" : "zone_bonus_health";
            player.sendMessage(Lang.get(plugin, key, "%value%", String.valueOf(extraHearts)));
            hasAnyBonus = true;
        }

        // Бонус скорости
        if (speedBonus > 0) {
            float newSpeed = (float) Math.min(1.0, 0.2 + speedBonus);
            player.setWalkSpeed(newSpeed);
            player.sendMessage(Lang.get(plugin, "zone_bonus_speed", "%value%", String.format("%.0f", speedBonus * 100)));
            hasAnyBonus = true;
        }

        // Бонус прыжка (через эффект)
        if (jumpBonus > 0) {
            int jumpAmplifier = (int) (jumpBonus / 0.1); // 1 уровень = JUMP_BOOST I
            if (jumpAmplifier > 0) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.JUMP_BOOST,
                        Integer.MAX_VALUE, // Бесконечная длительность
                        jumpAmplifier - 1, // Уровень эффекта (0 = I, 1 = II и т.д.)
                        true, // Частицы
                        false // Амбиент
                ));
                player.sendMessage(Lang.get(plugin, "zone_bonus_jump", "%value%", String.format("%.0f", jumpBonus * 100)));
                hasAnyBonus = true;
            }
        }

        // Бонус урона (через эффект)
        if (damageBonus > 0) {
            int damageAmplifier = (int) (damageBonus / 0.05);
            if (damageAmplifier > 0) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        Integer.MAX_VALUE,
                        damageAmplifier - 1,
                        true,
                        false
                ));
                player.sendMessage(Lang.get(plugin, "zone_bonus_damage", "%value%", String.format("%.0f", damageBonus * 100)));
                hasAnyBonus = true;
            }
        }

        if (!hasAnyBonus) {
            player.sendMessage(Lang.get(plugin, "zone_bonus_none"));
        }
    }

    private void removePlayerBonuses(Player player) {
        // Сбрасываем бонусы
        player.setMaxHealth(20.0);
        player.setWalkSpeed(0.2f);

        // Убираем эффекты прыжка и урона
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.STRENGTH);

        player.sendMessage(Lang.get(plugin, "zone_bonuses_off"));
    }

    // Метод для очистки при выходе игрока
    public void removePlayer(UUID playerId) {
        playerZones.remove(playerId);
    }
}