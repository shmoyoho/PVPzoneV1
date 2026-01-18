package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.DuelData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {
    private final Main plugin;
    private final Map<UUID, DuelData> activeDuels = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerDuelMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public DuelManager(Main plugin) {
        this.plugin = plugin;
    }

    // ВЫЗОВ НА ДУЭЛЬ
    public boolean challengePlayer(Player challenger, Player target, String zoneName) {
        // Проверка кулдауна
        if (hasCooldown(challenger.getUniqueId())) {
            challenger.sendMessage("§cПодождите перед следующим вызовом!");
            return false;
        }

        // Проверка, не в дуэли ли уже
        if (isPlayerInDuel(challenger.getUniqueId())) {
            challenger.sendMessage("§cВы уже в дуэли!");
            return false;
        }

        if (isPlayerInDuel(target.getUniqueId())) {
            challenger.sendMessage("§cИгрок " + target.getName() + " уже в дуэли!");
            return false;
        }

        // Если зона не указана, берем первую доступную
        if (zoneName == null || zoneName.isEmpty()) {
            var zones = plugin.getZoneManager().getAllZones();
            if (zones.isEmpty()) {
                challenger.sendMessage("§cНет доступных PvP зон!");
                return false;
            }
            zoneName = zones.get(0).getName();
        }

        // Проверяем существование зоны
        var zone = plugin.getZoneManager().getZone(zoneName);
        if (zone == null) {
            challenger.sendMessage("§cЗона '" + zoneName + "' не найдена!");
            return false;
        }

        // Создаем дуэль
        DuelData duel = new DuelData(challenger, target, zoneName);

        activeDuels.put(duel.getDuelId(), duel);
        playerDuelMap.put(challenger.getUniqueId(), duel.getDuelId());
        playerDuelMap.put(target.getUniqueId(), duel.getDuelId());

        // Кулдаун 30 секунд
        setCooldown(challenger.getUniqueId(), 30000);

        // Сохраняем текущие локации игроков
        duel.setChallengerLocation(challenger.getLocation().clone());
        duel.setTargetLocation(target.getLocation().clone());

        // Отправляем уведомления
        sendChallengeNotification(duel);

        // Таймер отказа (60 секунд)
        startTimeoutTimer(duel);

        return true;
    }

    // ПРИНЯТИЕ ДУЭЛИ
    public boolean acceptDuel(Player player) {
        DuelData duel = getPlayerDuel(player.getUniqueId());
        if (duel == null || duel.getState() != DuelData.DuelState.PENDING) {
            player.sendMessage("§cУ вас нет активных вызовов!");
            return false;
        }

        // Проверяем, является ли игрок целью
        if (!duel.getTarget().getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cЭтот вызов не для вас!");
            return false;
        }

        // Начинаем дуэль
        startDuel(duel);
        return true;
    }

    // ОТКЛОНЕНИЕ ДУЭЛИ
    public boolean denyDuel(Player player) {
        DuelData duel = getPlayerDuel(player.getUniqueId());
        if (duel == null) {
            player.sendMessage("§cУ вас нет активных вызовов!");
            return false;
        }

        sendDenyNotification(duel, player);
        finishDuel(duel, DuelData.DuelState.CANCELLED);
        return true;
    }

    // НАЧАЛО ДУЭЛИ
    private void startDuel(DuelData duel) {
        duel.setState(DuelData.DuelState.ACTIVE);

        // Получаем зону
        var zone = plugin.getZoneManager().getZone(duel.getZoneName());
        if (zone == null) {
            sendMessageToPlayers(duel, "§cОшибка: зона не найдена!");
            finishDuel(duel, DuelData.DuelState.CANCELLED);
            return;
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

        // Телепортируем игроков
        teleportToDuel(duel.getChallenger(), loc1, duel.getTarget());
        teleportToDuel(duel.getTarget(), loc2, duel.getChallenger());

        // Применяем PvP набор
        plugin.getKitManager().applyZoneKit(duel.getChallenger(), duel.getZoneName());
        plugin.getKitManager().applyZoneKit(duel.getTarget(), duel.getZoneName());

        // Добавляем игроков в зону (для системы зон)
        plugin.getZoneManager().addPlayerToZone(duel.getChallenger());
        plugin.getZoneManager().addPlayerToZone(duel.getTarget());

        // Отправляем сообщения
        sendDuelStartNotification(duel);

        // Таймер окончания дуэли (5 минут)
        startDuelTimer(duel);
    }

    // ПОИСК БЕЗОПАСНОГО МЕСТА В ЗОНЕ
    private Location findSafeLocationInZone(ZoneManager.PvpZone zone) {
        Location pos1 = zone.getPos1();
        Location pos2 = zone.getPos2();
        World world = pos1.getWorld();

        if (world == null) return pos1;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        Random random = new Random();
        int attempts = 0;

        while (attempts < 100) {
            double x = minX + random.nextDouble() * (maxX - minX);
            double z = minZ + random.nextDouble() * (maxZ - minZ);

            // Ищем высоту (ищем поверхность)
            Location testLoc = new Location(world, x, world.getMaxHeight(), z);

            // Спускаемся вниз пока не найдем безопасный блок
            while (testLoc.getY() > 0) {
                Block block = testLoc.getBlock();
                Block blockAbove = testLoc.clone().add(0, 1, 0).getBlock();
                Block blockBelow = testLoc.clone().add(0, -1, 0).getBlock();

                // Условия безопасного места:
                // 1. Блок под ногами - твердый
                // 2. Блок на уровне ног - воздух или проходимый
                // 3. Блок над головой - воздух
                if (blockBelow.getType().isSolid() &&
                        !block.getType().isSolid() &&
                        !blockAbove.getType().isSolid()) {

                    // Проверяем, что это не опасный блок (лава, вода и т.д.)
                    if (!isDangerousBlock(blockBelow) &&
                            !isDangerousBlock(block) &&
                            !isDangerousBlock(blockAbove)) {

                        // Устанавливаем точку на поверхности
                        return new Location(world, x, testLoc.getY(), z);
                    }
                }

                testLoc.add(0, -1, 0);
            }

            attempts++;
        }

        // Если не нашли безопасное место, возвращаем центр зоны
        return zone.getCenter() != null ? zone.getCenter() : pos1;
    }

    private boolean isDangerousBlock(Block block) {
        Material type = block.getType();
        return type == Material.LAVA || type == Material.WATER ||
                type == Material.FIRE || type == Material.CACTUS ||
                type == Material.MAGMA_BLOCK || type == Material.SWEET_BERRY_BUSH;
    }

    // ТЕЛЕПОРТАЦИЯ НА ДУЭЛЬ
    private void teleportToDuel(Player player, Location location, Player opponent) {
        // Сохраняем ориентацию игрока (смотрит на противника)
        location.setYaw(getYawToPlayer(location, opponent.getLocation()));
        location.setPitch(0);

        player.teleport(location);

        // Эффекты телепортации
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.spawnParticle(Particle.PORTAL, player.getLocation(), 50);
    }

    private float getYawToPlayer(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        return (float) yaw;
    }

    // ТАЙМЕРЫ
    private void startTimeoutTimer(DuelData duel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (duel.getState() == DuelData.DuelState.PENDING) {
                    sendMessageToPlayers(duel, "§cВремя вышло! Дуэль отменена.");
                    finishDuel(duel, DuelData.DuelState.CANCELLED);
                }
            }
        }.runTaskLater(plugin, 20L * 60); // 60 секунд
    }

    private void startDuelTimer(DuelData duel) {
        new BukkitRunnable() {
            int timeLeft = 300; // 5 минут

            @Override
            public void run() {
                if (duel.getState() != DuelData.DuelState.ACTIVE) {
                    cancel();
                    return;
                }

                // Отправляем сообщение каждую минуту
                if (timeLeft == 180 || timeLeft == 120 || timeLeft == 60 || timeLeft <= 30) {
                    sendMessageToPlayers(duel, "§eДо конца дуэли: §6" + timeLeft + "§e секунд");

                    if (timeLeft <= 10) {
                        for (Player player : new Player[]{duel.getChallenger(), duel.getTarget()}) {
                            if (player.isOnline()) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                            }
                        }
                    }
                }

                // Завершение по времени
                if (timeLeft <= 0) {
                    sendMessageToPlayers(duel, "§eВремя дуэли истекло! Ничья.");
                    finishDuel(duel, DuelData.DuelState.FINISHED);
                    cancel();
                    return;
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 20L, 20L); // Каждую секунду
    }

    // ЗАВЕРШЕНИЕ ДУЭЛИ
    public void finishDuel(DuelData duel, DuelData.DuelState state) {
        duel.setState(state);

        // Возвращаем игроков на исходные позиции
        if (duel.getChallengerLocation() != null && duel.getChallenger().isOnline()) {
            duel.getChallenger().teleport(duel.getChallengerLocation());
            plugin.getZoneManager().removePlayerFromZone(duel.getChallenger());
            plugin.getPlayerDataManager().restoreOriginalInventory(duel.getChallenger());
        }

        if (duel.getTargetLocation() != null && duel.getTarget().isOnline()) {
            duel.getTarget().teleport(duel.getTargetLocation());
            plugin.getZoneManager().removePlayerFromZone(duel.getTarget());
            plugin.getPlayerDataManager().restoreOriginalInventory(duel.getTarget());
        }

        // Очищаем данные
        activeDuels.remove(duel.getDuelId());
        playerDuelMap.remove(duel.getChallenger().getUniqueId());
        playerDuelMap.remove(duel.getTarget().getUniqueId());
    }

    // УВЕДОМЛЕНИЯ
    private void sendChallengeNotification(DuelData duel) {
        Player challenger = duel.getChallenger();
        Player target = duel.getTarget();

        challenger.sendMessage("§6══════════════════════════════");
        challenger.sendMessage("§aВызов отправлен игроку " + target.getName());
        challenger.sendMessage("§7Зона: §e" + duel.getZoneName());
        challenger.sendMessage("§6══════════════════════════════");

        target.sendMessage("§6══════════════════════════════");
        target.sendMessage("§c⚔ Вам вызов на дуэль!");
        target.sendMessage("§7От: §a" + challenger.getName());
        target.sendMessage("§7Зона: §e" + duel.getZoneName());
        target.sendMessage("");
        target.sendMessage("§a/pvpaccept §7- Принять вызов");
        target.sendMessage("§c/pvpdeny §7- Отклонить вызов");
        target.sendMessage("§7(Автоотказ через 60 секунд)");
        target.sendMessage("§6══════════════════════════════");

        // Оповещение сервера
        Bukkit.broadcastMessage("§7[PvP] §e" + challenger.getName() +
                "§7 вызвал на дуэль §e" + target.getName());
    }

    private void sendDuelStartNotification(DuelData duel) {
        sendMessageToPlayers(duel, "§6══════════════════════════════");
        sendMessageToPlayers(duel, "§a⚔ Дуэль началась!");
        sendMessageToPlayers(duel, "§7Зона: §e" + duel.getZoneName());
        sendMessageToPlayers(duel, "§7Продолжительность: §e5 минут");
        sendMessageToPlayers(duel, "§6══════════════════════════════");

        // Эффекты
        for (Player player : new Player[]{duel.getChallenger(), duel.getTarget()}) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        }

        Bukkit.broadcastMessage("§7[PvP] §aДуэль между §e" + duel.getChallenger().getName() +
                "§a и §e" + duel.getTarget().getName() + "§a началась!");
    }

    private void sendDenyNotification(DuelData duel, Player denier) {
        String message = "§c" + denier.getName() + " отклонил дуэль!";

        if (duel.getChallenger().isOnline()) duel.getChallenger().sendMessage(message);
        if (duel.getTarget().isOnline() && !duel.getTarget().equals(denier)) {
            duel.getTarget().sendMessage(message);
        }

        Bukkit.broadcastMessage("§7[PvP] §cДуэль отменена: " +
                duel.getChallenger().getName() + " vs " + duel.getTarget().getName());
    }

    private void sendMessageToPlayers(DuelData duel, String message) {
        if (duel.getChallenger().isOnline()) duel.getChallenger().sendMessage(message);
        if (duel.getTarget().isOnline()) duel.getTarget().sendMessage(message);
    }

    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    public boolean isPlayerInDuel(UUID playerId) {
        return playerDuelMap.containsKey(playerId);
    }

    public DuelData getPlayerDuel(UUID playerId) {
        UUID duelId = playerDuelMap.get(playerId);
        return duelId != null ? activeDuels.get(duelId) : null;
    }

    private boolean hasCooldown(UUID playerId) {
        Long endTime = cooldowns.get(playerId);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    private void setCooldown(UUID playerId, long durationMs) {
        cooldowns.put(playerId, System.currentTimeMillis() + durationMs);
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}