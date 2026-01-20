package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.listeners.PlayerMoveListener;
import com.galyakyxnya.pvpzone.models.DuelData;
import com.galyakyxnya.pvpzone.utils.ChatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {
    private final Main plugin;
    private final Map<UUID, DuelData> activeDuels = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerDuelMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> freezeTasks = new ConcurrentHashMap<>();

    public DuelManager(Main plugin) {
        this.plugin = plugin;
    }

    // ВЫЗОВ НА ДУЭЛЬ
    public boolean challengePlayer(Player challenger, Player target, String zoneName) {
        // Проверяем кулдаун
        if (isOnCooldown(challenger.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Подождите перед отправкой следующего вызова!");
            return false;
        }

        // Проверяем, что оба игрока онлайн
        if (!challenger.isOnline() || !target.isOnline()) {
            challenger.sendMessage(ChatColor.RED + "Игрок оффлайн!");
            return false;
        }

        // === ЖЕСТКИЙ ЗАПРЕТ: НИКТО ИЗ ИГРОКОВ НЕ ДОЛЖЕН БЫТЬ В ЗОНЕ ===
        boolean challengerInZone = plugin.getZoneManager().isPlayerInZone(challenger);
        boolean targetInZone = plugin.getZoneManager().isPlayerInZone(target);

        if (challengerInZone) {
            challenger.sendMessage(ChatColor.RED + "══════════════════════════════");
            challenger.sendMessage(ChatColor.RED + "✗ НЕВОЗМОЖНО ВЫЗВАТЬ НА ДУЭЛЬ!");
            challenger.sendMessage(ChatColor.GRAY + "Вы находитесь в PvP зоне");
            challenger.sendMessage(ChatColor.GRAY + "Выйдите из зоны перед вызовом");
            challenger.sendMessage(ChatColor.GRAY + "Используйте: /pvpfix");
            challenger.sendMessage(ChatColor.RED + "══════════════════════════════");
            return false;
        }

        if (targetInZone) {
            challenger.sendMessage(ChatColor.RED + "══════════════════════════════");
            challenger.sendMessage(ChatColor.RED + "✗ НЕВОЗМОЖНО ВЫЗВАТЬ НА ДУЭЛЬ!");
            challenger.sendMessage(ChatColor.GRAY + "Игрок " + ChatColor.YELLOW + target.getName() +
                    ChatColor.GRAY + " находится в PvP зоне");
            challenger.sendMessage(ChatColor.GRAY + "Подождите пока игрок выйдет из зоны");
            challenger.sendMessage(ChatColor.RED + "══════════════════════════════");

            // Также сообщаем цели (если онлайн)
            if (target.isOnline()) {
                target.sendMessage(ChatColor.YELLOW + "══════════════════════════════");
                target.sendMessage(ChatColor.GRAY + "Игрок " + ChatColor.YELLOW + challenger.getName() +
                        ChatColor.GRAY + " хотел вызвать вас на дуэль");
                target.sendMessage(ChatColor.GRAY + "Но вы находитесь в PvP зоне");
                target.sendMessage(ChatColor.GRAY + "Выйдите из зоны чтобы принять вызов");
                target.sendMessage(ChatColor.GRAY + "Используйте: /pvpfix");
                target.sendMessage(ChatColor.YELLOW + "══════════════════════════════");
            }
            return false;
        }
        // === КОНЕЦ ПРОВЕРКИ ЗОНЫ ===

        // Проверяем, что игроки не в дуэли
        if (getPlayerDuel(challenger.getUniqueId()) != null) {
            challenger.sendMessage(ChatColor.RED + "Вы уже участвуете в дуэли!");
            return false;
        }

        if (getPlayerDuel(target.getUniqueId()) != null) {
            challenger.sendMessage(ChatColor.RED + target.getName() + " уже участвует в дуэли!");
            return false;
        }

        // Если зона указана - проверяем ее существование
        if (zoneName != null) {
            if (plugin.getZoneManager().getZone(zoneName) == null) {
                challenger.sendMessage(ChatColor.RED + "Зона '" + zoneName + "' не найдена!");
                return false;
            }
        }

        // Создаем дуэль
        DuelData duel = new DuelData(challenger, target, zoneName);

        // Сохраняем текущие локации игроков
        duel.setChallengerLocation(challenger.getLocation().clone());
        duel.setTargetLocation(target.getLocation().clone());

        // ВАЖНО: Сохраняем инвентари ОБОИХ игроков
        // Оба гарантированно НЕ в зоне (по нашей проверке выше)
        ItemStack[] challengerInventory = challenger.getInventory().getContents().clone();
        ItemStack[] challengerArmor = challenger.getInventory().getArmorContents().clone();
        duel.setChallengerOriginalInventory(challengerInventory);
        duel.setChallengerOriginalArmor(challengerArmor);

        ItemStack[] targetInventory = target.getInventory().getContents().clone();
        ItemStack[] targetArmor = target.getInventory().getArmorContents().clone();
        duel.setTargetOriginalInventory(targetInventory);
        duel.setTargetOriginalArmor(targetArmor);

        // Логируем для отладки
        plugin.getLogger().info("=== DUEL CHALLENGE ===");
        plugin.getLogger().info("Challenger: " + challenger.getName() + " (outside zone, items: " + countItems(challengerInventory) + ")");
        plugin.getLogger().info("Target: " + target.getName() + " (outside zone, items: " + countItems(targetInventory) + ")");

        activeDuels.put(duel.getDuelId(), duel);
        playerDuelMap.put(challenger.getUniqueId(), duel.getDuelId());
        playerDuelMap.put(target.getUniqueId(), duel.getDuelId());

        // Кулдаун 30 секунд
        setCooldown(challenger.getUniqueId(), 30000);

        // Отправляем уведомления с кликабельными кнопками
        sendInteractiveChallengeNotification(duel);

        // Таймер отказа (60 секунд)
        startTimeoutTimer(duel);

        return true;
    }

    private int countItems(ItemStack[] items) {
        if (items == null) return 0;
        int count = 0;
        for (ItemStack item : items) {
            if (item != null) count++;
        }
        return count;
    }

    // ИНТЕРАКТИВНОЕ УВЕДОМЛЕНИЕ О ВЫЗОВЕ
    private void sendInteractiveChallengeNotification(DuelData duel) {
        Player challenger = duel.getChallenger();
        Player target = duel.getTarget();
        String zoneName = duel.getZoneName();

        // Если зона не указана (null), показываем "случайная зона"
        String zoneDisplay = (zoneName != null) ? zoneName : "случайная зона";

        // Сообщение вызывающему
        challenger.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        challenger.sendMessage(ChatColor.GREEN + "✓ Вызов отправлен игроку " + target.getName());
        challenger.sendMessage(ChatColor.GRAY + "Зона: " + ChatColor.YELLOW + zoneDisplay);
        challenger.sendMessage(ChatColor.GRAY + "Ожидание ответа...");
        challenger.sendMessage(ChatColor.GOLD + "══════════════════════════════");

        // Интерактивное сообщение принимающему
        ChatUtils.sendClickableDuelInvite(target, challenger, zoneDisplay);

        // Оповещение сервера
        TextComponent broadcast = new TextComponent(ChatColor.GRAY + "[PvP] " +
                ChatColor.YELLOW + challenger.getName() +
                ChatColor.GRAY + " вызвал на дуэль " +
                ChatColor.YELLOW + target.getName());

        broadcast.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pvp " + target.getName() + " " + duel.getZoneName()));
        broadcast.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(ChatColor.YELLOW + "Нажмите чтобы тоже вызвать " + target.getName() + " на дуэль")));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(challenger) && !online.equals(target)) {
                online.spigot().sendMessage(broadcast);
            }
        }
    }

    // ПРИНЯТИЕ ДУЭЛИ
    public boolean acceptDuel(Player player) {
        DuelData duel = getPlayerDuel(player.getUniqueId());

        if (duel == null || duel.getState() != DuelData.DuelState.PENDING) {
            player.sendMessage(ChatColor.RED + "У вас нет активных вызовов!");
            return false;
        }

        if (!duel.getTarget().equals(player)) {
            player.sendMessage(ChatColor.RED + "Этот вызов не вам!");
            return false;
        }

        Player challenger = duel.getChallenger();

        if (!challenger.isOnline()) {
            player.sendMessage(ChatColor.RED + "Вызывающий игрок вышел!");
            return false;
        }

        // Начинаем дуэль
        startDuel(duel);
        return true;
    }

    // ОТКЛОНЕНИЕ ДУЭЛИ
    public boolean denyDuel(Player player) {
        DuelData duel = getPlayerDuel(player.getUniqueId());

        if (duel == null || duel.getState() != DuelData.DuelState.PENDING) {
            player.sendMessage(ChatColor.RED + "У вас нет активных вызовов!");
            return false;
        }

        Player challenger = duel.getChallenger();

        // Отправляем уведомления
        player.sendMessage(ChatColor.RED + "✗ Вы отклонили вызов на дуэль!");

        if (challenger.isOnline()) {
            challenger.sendMessage(ChatColor.RED + "✗ " + player.getName() + " отклонил ваш вызов!");
        }

        // Завершаем дуэль
        finishDuel(duel, DuelData.DuelState.CANCELLED);
        return true;
    }

    // НАЧАЛО ДУЭЛИ С ОТСЧЕТОМ
    private void startDuel(DuelData duel) {
        duel.setState(DuelData.DuelState.ACTIVE);

        // Получаем зону
        String zoneName = duel.getZoneName();
        ZoneManager.PvpZone zone;

        if (zoneName != null) {
            zone = plugin.getZoneManager().getZone(zoneName);
            if (zone == null) {
                sendMessageToPlayers(duel, "§cОшибка: зона не найдена!");
                finishDuel(duel, DuelData.DuelState.CANCELLED);
                return;
            }
        } else {
            // Если зона не указана, берем случайную
            List<ZoneManager.PvpZone> zones = plugin.getZoneManager().getAllZones();
            if (zones.isEmpty()) {
                sendMessageToPlayers(duel, "§cОшибка: нет доступных зон!");
                finishDuel(duel, DuelData.DuelState.CANCELLED);
                return;
            }
            zone = zones.get(new Random().nextInt(zones.size()));
            duel.setZoneName(zone.getName());
        }

        // ВАЖНО: Добавляем игроков в ZoneManager ПЕРЕД дуэлью
        // Это нужно для корректной работы систем защиты
        Player challenger = duel.getChallenger();
        Player target = duel.getTarget();

        plugin.getZoneManager().addPlayerToZone(challenger);
        plugin.getZoneManager().addPlayerToZone(target);

        plugin.getLogger().info("Added players to zone for duel: " +
                challenger.getName() + " and " + target.getName());

        // Находим безопасные места в зоне
        Location loc1 = findSafeLocationInZone(zone);
        Location loc2 = findSafeLocationInZone(zone);

        // Убедимся, что точки не слишком близко
        int attempts = 0;
        while (loc1.distance(loc2) < 10.0 && attempts < 20) {
            loc2 = findSafeLocationInZone(zone);
            attempts++;
        }

        // Телепортируем игроков и начинаем отсчет
        teleportAndStartCountdown(duel, loc1, loc2);
    }

    // ТЕЛЕПОРТАЦИЯ И ОТСЧЕТ
    private void teleportAndStartCountdown(DuelData duel, Location loc1, Location loc2) {
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();

        // Телепортируем
        teleportToDuel(player1, loc1, player2);
        teleportToDuel(player2, loc2, player1);

        // Замораживаем игроков на 3 секунды
        freezePlayer(player1, 60); // 3 секунды = 60 тиков
        freezePlayer(player2, 60);

        // Отправляем сообщение о начале отсчета
        sendMessageToPlayers(duel, "§6══════════════════════════════");
        sendMessageToPlayers(duel, "§a⚔ ДУЭЛЬ НАЧНЕТСЯ ЧЕРЕЗ:");

        // Отсчет 3... 2... 1... СТАРТ!
        new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (duel.getState() != DuelData.DuelState.ACTIVE) {
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    // Показываем отсчет
                    String number = getCountdownNumber(countdown);
                    sendMessageToPlayers(duel, "§e" + number);

                    // Звук отсчета
                    for (Player player : new Player[]{player1, player2}) {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                            player.spawnParticle(Particle.NOTE, player.getLocation().add(0, 2, 0), 10);
                        }
                    }

                    countdown--;
                } else {
                    // СТАРТ!
                    sendMessageToPlayers(duel, "§a⚔ СТАРТ!");

                    // Размораживаем игроков
                    unfreezePlayer(player1);
                    unfreezePlayer(player2);

                    // Звук старта
                    for (Player player : new Player[]{player1, player2}) {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
                            player.spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                            player.sendTitle("§a⚔ ДУЭЛЬ!", "§7Удачи!", 10, 40, 10);
                        }
                    }

                    // Применяем PvP набор для дуэли
                    // Используем набор из зоны дуэли
                    String duelKitName = "default"; // Можно использовать набор из зоны
                    if (duel.getZoneName() != null) {
                        ZoneManager.PvpZone duelZone = plugin.getZoneManager().getZone(duel.getZoneName());
                        if (duelZone != null) {
                            duelKitName = duelZone.getKitName();
                        }
                    }

                    // Очищаем инвентарь и применяем дуэльный набор
                    player1.getInventory().clear();
                    player1.getInventory().setArmorContents(new ItemStack[4]);
                    player2.getInventory().clear();
                    player2.getInventory().setArmorContents(new ItemStack[4]);

                    plugin.getKitManager().applyKit(duelKitName, player1);
                    plugin.getKitManager().applyKit(duelKitName, player2);

                    plugin.getLogger().info("Applied duel kit '" + duelKitName + "' to both players");

                    // Оповещение всего сервера
                    Bukkit.broadcastMessage("§7[PvP] §aДуэль между §e" + player1.getName() +
                            "§a и §e" + player2.getName() + "§a началась!");

                    // Таймер дуэли (5 минут)
                    startDuelTimer(duel);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Каждую секунду
    }

    private String getCountdownNumber(int number) {
        switch (number) {
            case 3: return "§c▆ ▆ ▆";
            case 2: return "§6▆ ▆";
            case 1: return "§e▆";
            default: return String.valueOf(number);
        }
    }

    // ЗАМОРАЖИВАНИЕ ИГРОКА
    private void freezePlayer(Player player, int ticks) {
        // Эффект замедления
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 255, false, false));

        // Запрещаем движение
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        player.setAllowFlight(false);

        // Сохраняем задачу для разморозки
        BukkitRunnable unfreezeTask = new BukkitRunnable() {
            @Override
            public void run() {
                unfreezePlayer(player);
            }
        };

        unfreezeTask.runTaskLater(plugin, ticks);
        freezeTasks.put(player.getUniqueId(), unfreezeTask);
    }

    private void unfreezePlayer(Player player) {
        // Убираем эффекты
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        // Восстанавливаем скорость
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        // Отменяем задачу если она есть
        BukkitRunnable task = freezeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    // ТЕЛЕПОРТАЦИЯ
    private void teleportToDuel(Player player, Location location, Player opponent) {
        // Сохраняем ориентацию игрока (смотрит на противника)
        location.setYaw(getYawToPlayer(location, opponent.getLocation()));
        location.setPitch(0);

        player.teleport(location);

        // Эффекты телепортации
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.spawnParticle(Particle.PORTAL, player.getLocation(), 50);

        // Сообщение игроку
        player.sendMessage("§7Вы телепортированы на дуэль. Подготовьтесь!");
    }

    // ТАЙМЕР ОТКАЗА (60 секунд)
    private void startTimeoutTimer(DuelData duel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (duel.getState() == DuelData.DuelState.PENDING) {
                    // Время вышло, автоматически отклоняем
                    Player target = duel.getTarget();
                    if (target.isOnline()) {
                        target.sendMessage(ChatColor.RED + "✗ Время на принятие дуэли истекло!");
                    }

                    Player challenger = duel.getChallenger();
                    if (challenger.isOnline()) {
                        challenger.sendMessage(ChatColor.RED + "✗ " + target.getName() + " не принял вызов вовремя!");
                    }

                    finishDuel(duel, DuelData.DuelState.CANCELLED);
                }
            }
        }.runTaskLater(plugin, 20L * 60); // 60 секунд
    }

    // ТАЙМЕР ДУЭЛИ (5 минут)
    private void startDuelTimer(DuelData duel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (duel.getState() == DuelData.DuelState.ACTIVE) {
                    // Время дуэли истекло - ничья
                    sendMessageToPlayers(duel, "§6══════════════════════════════");
                    sendMessageToPlayers(duel, "§e⚔ ВРЕМЯ ДУЭЛИ ИСТЕКЛО!");
                    sendMessageToPlayers(duel, "§7Результат: §6НИЧЬЯ");
                    sendMessageToPlayers(duel, "§6══════════════════════════════");

                    // Оповещение сервера
                    Bukkit.broadcastMessage("§7[PvP] §eДуэль между §a" + duel.getChallenger().getName() +
                            "§e и §a" + duel.getTarget().getName() + "§e завершилась ничьей (время истекло)");

                    finishDuel(duel, DuelData.DuelState.FINISHED);
                }
            }
        }.runTaskLater(plugin, 20L * 60 * 5); // 5 минут
    }

    // ПОИСК БЕЗОПАСНОГО МЕСТА В ЗОНЕ
    private Location findSafeLocationInZone(ZoneManager.PvpZone zone) {
        Location center = zone.getCenter();
        if (center == null) {
            return zone.getPos1();
        }

        Random random = new Random();
        Location pos1 = zone.getPos1();
        Location pos2 = zone.getPos2();

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        // Пробуем несколько раз найти безопасное место
        for (int attempt = 0; attempt < 50; attempt++) {
            double x = minX + random.nextDouble() * (maxX - minX);
            double z = minZ + random.nextDouble() * (maxZ - minZ);
            double y = center.getWorld().getHighestBlockYAt((int) x, (int) z) + 1;

            Location location = new Location(center.getWorld(), x, y, z);

            // Проверяем, что блок под ногами безопасный
            Block block = location.getBlock();
            Block blockBelow = location.clone().subtract(0, 1, 0).getBlock();

            if (block.getType().isAir() && !blockBelow.getType().isAir() &&
                    !blockBelow.isLiquid() && blockBelow.getType().isSolid()) {
                return location;
            }
        }

        // Если не нашли безопасное место, возвращаем центр
        return center.clone();
    }

    // РАССЧЕТ УГЛА ПОВОРОТА К ИГРОКУ
    private float getYawToPlayer(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double yaw = Math.atan2(dz, dx);
        return (float) Math.toDegrees(yaw) - 90;
    }

    // ОТПРАВКА СООБЩЕНИЙ ОБОИМ ИГРОКАМ
    private void sendMessageToPlayers(DuelData duel, String message) {
        Player player1 = duel.getChallenger();
        Player player2 = duel.getTarget();

        if (player1.isOnline()) {
            player1.sendMessage(message);
        }

        if (player2.isOnline()) {
            player2.sendMessage(message);
        }
    }

    // КУЛДАУНЫ
    private void setCooldown(UUID playerId, long cooldownMillis) {
        cooldowns.put(playerId, System.currentTimeMillis() + cooldownMillis);
    }

    private boolean isOnCooldown(UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);
        if (cooldownEnd == null) {
            return false;
        }

        if (System.currentTimeMillis() > cooldownEnd) {
            cooldowns.remove(playerId);
            return false;
        }

        return true;
    }

    // ПОЛУЧЕНИЕ ДУЭЛИ ИГРОКА
    public DuelData getPlayerDuel(UUID playerId) {
        UUID duelId = playerDuelMap.get(playerId);
        if (duelId == null) {
            return null;
        }
        return activeDuels.get(duelId);
    }

    // ЗАВЕРШЕНИЕ ДУЭЛИ
    // ЗАВЕРШЕНИЕ ДУЭЛИ
    public void finishDuel(DuelData duel, DuelData.DuelState state) {
        duel.setState(state);

        ZoneManager zoneManager = plugin.getZoneManager();
        PlayerMoveListener moveListener = plugin.getPlayerMoveListener();

        Player challenger = duel.getChallenger();
        Player target = duel.getTarget();

        // Восстанавливаем ВСЕХ игроков, независимо от зоны
        if (challenger.isOnline()) {
            unfreezePlayer(challenger);

            // Убираем из трекеров
            if (moveListener != null) {
                moveListener.removePlayer(challenger.getUniqueId());
            }

            // ВАЖНО: Удаляем из ZoneManager
            zoneManager.removePlayerFromZone(challenger);

            // Восстанавливаем инвентарь ИЗ DuelData (гарантированно не в зоне)
            restoreInventoryFromDuelData(challenger, duel.getChallengerOriginalInventory(),
                    duel.getChallengerOriginalArmor());

            // === ДОБАВЛЯЕМ: ПОКАЗЫВАЕМ СТАТИСТИКУ ===
            showPlayerStats(challenger);

            // Телепортируем
            if (duel.getChallengerLocation() != null) {
                challenger.teleport(duel.getChallengerLocation());
            }
        }

        if (target.isOnline()) {
            unfreezePlayer(target);

            // Убираем из трекеров
            if (moveListener != null) {
                moveListener.removePlayer(target.getUniqueId());
            }

            // ВАЖНО: Удаляем из ZoneManager
            zoneManager.removePlayerFromZone(target);

            // Восстанавливаем инвентарь ИЗ DuelData (гарантированно не в зоне)
            restoreInventoryFromDuelData(target, duel.getTargetOriginalInventory(),
                    duel.getTargetOriginalArmor());

            // === ДОБАВЛЯЕМ: ПОКАЗЫВАЕМ СТАТИСТИКУ ===
            showPlayerStats(target);

            // Телепортируем
            if (duel.getTargetLocation() != null) {
                target.teleport(duel.getTargetLocation());
            }
        }

        // Очищаем данные дуэли
        activeDuels.remove(duel.getDuelId());
        playerDuelMap.remove(challenger.getUniqueId());
        playerDuelMap.remove(target.getUniqueId());
    }

    // === ДОБАВЛЯЕМ НОВЫЙ МЕТОД ===
    private void showPlayerStats(Player player) {
        var playerData = plugin.getPlayerDataManager().getPlayerData(player);

        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Ваша статистика после дуэли:");
        player.sendMessage(ChatColor.GRAY + "Рейтинг: " + ChatColor.YELLOW + playerData.getRating() + " очков");
        player.sendMessage(ChatColor.GRAY + "Очки для покупок: " + ChatColor.YELLOW + playerData.getPoints());

        // Показываем купленные бонусы
        if (!playerData.getPurchasedBonuses().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Ваши бонусы:");
            for (var entry : playerData.getPurchasedBonuses().entrySet()) {
                String bonusName = getBonusName(entry.getKey());
                player.sendMessage(ChatColor.DARK_GRAY + "  • " + ChatColor.GRAY +
                        bonusName + ": " + ChatColor.YELLOW +
                        "уровень " + entry.getValue());
            }
        }
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════");
    }

    private String getBonusName(String bonusId) {
        switch (bonusId) {
            case "health": return "Дополнительное сердце";
            case "speed": return "Увеличение скорости";
            case "jump": return "Высокий прыжок";
            case "damage": return "Усиление урона";
            default: return bonusId;
        }
    }

    // ПРОСТОЙ МЕТОД: Восстановление инвентаря ИЗ DuelData
    private void restoreInventoryFromDuelData(Player player, ItemStack[] savedInventory, ItemStack[] savedArmor) {
        // Гарантированно восстанавливаем из DuelData (оба игрока не в зоне при вызове)
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        if (savedInventory != null && savedInventory.length > 0) {
            player.getInventory().setContents(savedInventory);
        }

        if (savedArmor != null && savedArmor.length > 0) {
            player.getInventory().setArmorContents(savedArmor);
        }

        player.updateInventory();
        player.sendMessage("§a✓ Инвентарь восстановлен");

        // Логируем для отладки
        ItemStack[] restored = player.getInventory().getContents();
        int itemCount = countItems(restored);
        plugin.getLogger().info("Restored " + itemCount + " items for " + player.getName() + " from DuelData");

        if (itemCount > 0 && restored[0] != null) {
            plugin.getLogger().info("First item: " + restored[0].getType() + " x" + restored[0].getAmount());
        }
    }

    // ОЧИСТКА КУЛДАУНОВ (ежеминутная)
    public void cleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, Long>> iterator = cooldowns.entrySet().iterator();
                long now = System.currentTimeMillis();

                while (iterator.hasNext()) {
                    Map.Entry<UUID, Long> entry = iterator.next();
                    if (now > entry.getValue()) {
                        iterator.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // Каждую минуту
    }
}