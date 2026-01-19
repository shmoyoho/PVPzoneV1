package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.DuelData;
import com.galyakyxnya.pvpzone.utils.ChatUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
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
        // Если зона не указана (null) - это нормально, выберем случайную позже

        // Создаем дуэль (передаем zoneName как есть, даже если null)
        DuelData duel = new DuelData(challenger, target, zoneName);

        activeDuels.put(duel.getDuelId(), duel);
        playerDuelMap.put(challenger.getUniqueId(), duel.getDuelId());
        playerDuelMap.put(target.getUniqueId(), duel.getDuelId());

        // Кулдаун 30 секунд
        setCooldown(challenger.getUniqueId(), 30000);

        // Сохраняем текущие локации игроков
        duel.setChallengerLocation(challenger.getLocation().clone());
        duel.setTargetLocation(target.getLocation().clone());

        // Отправляем уведомления с кликабельными кнопками
        sendInteractiveChallengeNotification(duel);

        // Таймер отказа (60 секунд)
        startTimeoutTimer(duel);

        return true;
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

                    // Применяем PvP набор
                    plugin.getKitManager().applyZoneKit(player1, duel.getZoneName());
                    plugin.getKitManager().applyZoneKit(player2, duel.getZoneName());

                    // Добавляем игроков в зону
                    plugin.getZoneManager().addPlayerToZone(player1);
                    plugin.getZoneManager().addPlayerToZone(player2);

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

        // Эффект невидимости (чтобы не видеть рук)
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks, 0, false, false));

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
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

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
    public void finishDuel(DuelData duel, DuelData.DuelState state) {
        duel.setState(state);

        // Размораживаем игроков если они заморожены
        Player challenger = duel.getChallenger();
        Player target = duel.getTarget();

        if (challenger.isOnline()) {
            unfreezePlayer(challenger);
            plugin.getZoneManager().removePlayerFromZone(challenger);
            plugin.getPlayerDataManager().restoreOriginalInventory(challenger);

            // Возвращаем на исходную позицию
            if (duel.getChallengerLocation() != null) {
                challenger.teleport(duel.getChallengerLocation());
            }
        }

        if (target.isOnline()) {
            unfreezePlayer(target);
            plugin.getZoneManager().removePlayerFromZone(target);
            plugin.getPlayerDataManager().restoreOriginalInventory(target);

            // Возвращаем на исходную позицию
            if (duel.getTargetLocation() != null) {
                target.teleport(duel.getTargetLocation());
            }
        }

        // Очищаем данные
        activeDuels.remove(duel.getDuelId());
        playerDuelMap.remove(challenger.getUniqueId());
        playerDuelMap.remove(target.getUniqueId());
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