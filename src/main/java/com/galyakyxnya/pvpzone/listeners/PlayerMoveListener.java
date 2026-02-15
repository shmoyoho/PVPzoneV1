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

    private void handleEnterZone(Player player, ZoneManager.PvpZone zone) {
        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Проверяем еще раз что игрок не в дуэли
        // Это дополнительная защита на случай race condition
        if (plugin.getDuelManager().getPlayerDuel(player.getUniqueId()) != null) {
            return;
        }

        // 1. Получаем текущий инвентарь ДО любых изменений
        ItemStack[] originalInventory = player.getInventory().getContents().clone();
        ItemStack[] originalArmor = player.getInventory().getArmorContents().clone();

        // 2. Очищаем инвентарь
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // 3. Сохраняем оригинальный инвентарь (который был до очистки) в память и в БД
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        playerData.setOriginalInventory(originalInventory);
        playerData.setOriginalArmor(originalArmor);
        plugin.getPlayerDataManager().saveOriginalInventoryToDatabase(player);

        // Логируем только важное (при первом входе в день или для админов)
        if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine("Игрок " + player.getName() + " вошел в зону " + zone.getName());
        }

        // 4. Применяем PvP набор
        boolean kitApplied = plugin.getKitManager().applyKitOnly(zone.getKitName(), player);

        if (!kitApplied) {
            player.sendMessage("§c⚠ Набор для этой зоны не настроен!");
            // Восстанавливаем сохраненный оригинальный инвентарь
            plugin.getPlayerDataManager().restoreOriginalInventory(player);
            return;
        }

        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Вы вошли в PvP зону '" + zone.getName() + "'!");
        player.sendMessage(ChatColor.GRAY + "Набор: " + ChatColor.WHITE + zone.getKitName());

        // Показываем рейтинг
        showPlayerRating(player);
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");

        // Применяем бонусы если включены
        if (zone.isBonusesEnabled()) {
            applyPlayerBonuses(player);
        }

        // Добавляем игрока в ZoneManager
        plugin.getZoneManager().addPlayerToZone(player);
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

        // Применяем ВСЕ бонусы
        double healthBonus = playerData.getHealthBonus();
        double speedBonus = playerData.getSpeedBonus();
        double jumpBonus = playerData.getJumpBonus();
        double damageBonus = playerData.getDamageBonus();

        boolean hasAnyBonus = false;

        // Бонус здоровья
        if (healthBonus > 0) {
            int extraHearts = playerData.getBonusLevel("health"); // Получаем количество сердец
            player.setMaxHealth(20.0 + healthBonus);
            player.setHealth(Math.min(player.getHealth() + healthBonus, player.getMaxHealth()));
            player.sendMessage(ChatColor.GREEN + "✓ Бонус здоровья: +" + extraHearts +
                    " сердце" + (extraHearts > 1 ? "ца" : ""));
            hasAnyBonus = true;
        }

        // Бонус скорости
        if (speedBonus > 0) {
            float newSpeed = (float) Math.min(1.0, 0.2 + speedBonus);
            player.setWalkSpeed(newSpeed);
            player.sendMessage(ChatColor.GREEN + "✓ Бонус скорости: +" +
                    String.format("%.0f", speedBonus * 100) + "%");
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
                player.sendMessage(ChatColor.GREEN + "✓ Бонус прыжка: +" +
                        String.format("%.0f", jumpBonus * 100) + "%");
                hasAnyBonus = true;
            }
        }

        // Бонус урона (через эффект)
        if (damageBonus > 0) {
            int damageAmplifier = (int) (damageBonus / 0.05); // 1 уровень = STRENGTH I
            if (damageAmplifier > 0) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        Integer.MAX_VALUE, // Бесконечная длительность
                        damageAmplifier - 1, // Уровень эффекта (0 = I, 1 = II и т.д.)
                        true, // Частицы
                        false // Амбиент
                ));
                player.sendMessage(ChatColor.GREEN + "✓ Бонус урона: +" +
                        String.format("%.0f", damageBonus * 100) + "%");
                hasAnyBonus = true;
            }
        }

        if (!hasAnyBonus) {
            player.sendMessage(ChatColor.GRAY + "У вас нет активных бонусов");
        }
    }

    private void removePlayerBonuses(Player player) {
        // Сбрасываем бонусы
        player.setMaxHealth(20.0);
        player.setWalkSpeed(0.2f);

        // Убираем эффекты прыжка и урона
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.STRENGTH);

        player.sendMessage(ChatColor.GRAY + "Все бонусы отключены");
    }

    // Метод для очистки при выходе игрока
    public void removePlayer(UUID playerId) {
        playerZones.remove(playerId);
    }
}