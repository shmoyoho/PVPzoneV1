package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LeaderEffectManager {
    private final Main plugin;
    private final Set<UUID> currentLeaders = new HashSet<>();
    private BukkitRunnable effectTask;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public LeaderEffectManager(Main plugin) {
        this.plugin = plugin;
        startEffects();
    }

    private void startEffects() {
        effectTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateLeaderEffects();
            }
        };
        effectTask.runTaskTimer(plugin, 20L, 10L); // Проверка каждые 0.5 секунды
    }

    private void updateLeaderEffects() {
        // Получаем топ-1 игрока
        var topPlayers = plugin.getPlayerDataManager().getTopPlayers(1);
        Set<UUID> newLeaders = new HashSet<>();

        if (!topPlayers.isEmpty()) {
            PlayerData leaderData = topPlayers.get(0);
            Player leader = Bukkit.getPlayer(leaderData.getPlayerId());

            if (leader != null && leader.isOnline()) {
                newLeaders.add(leader.getUniqueId());
                showLeaderFootsteps(leader);
            }
        }

        // Убираем эффекты у тех, кто перестал быть лидером
        for (UUID oldLeaderId : currentLeaders) {
            if (!newLeaders.contains(oldLeaderId)) {
                Player oldLeader = Bukkit.getPlayer(oldLeaderId);
                if (oldLeader != null && oldLeader.isOnline()) {
                    oldLeader.sendMessage("§7Вы больше не лидер рейтинга");
                }
                lastLocations.remove(oldLeaderId);
            }
        }

        currentLeaders.clear();
        currentLeaders.addAll(newLeaders);
    }

    private void showLeaderFootsteps(Player leader) {
        Location currentLoc = leader.getLocation();
        Location lastLoc = lastLocations.get(leader.getUniqueId());

        // Сохраняем текущую позицию
        lastLocations.put(leader.getUniqueId(), currentLoc.clone());

        // Создаем огоньки только если игрок двигался
        if (lastLoc != null && lastLoc.distanceSquared(currentLoc) > 0.1) {
            // Огоньки под ногами (2-3 частицы)
            int flameCount = 2 + (int)(Math.random() * 2); // 2-3 огонька

            for (int i = 0; i < flameCount; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = 0.15 + Math.random() * 0.1; // Очень маленький радиус
                double x = currentLoc.getX() + radius * Math.cos(angle);
                double z = currentLoc.getZ() + radius * Math.sin(angle);
                double y = currentLoc.getY() + 0.05; // Чуть выше земли

                // Основной огонек
                leader.getWorld().spawnParticle(Particle.FLAME,
                        new Location(currentLoc.getWorld(), x, y, z),
                        1, 0, 0, 0, 0);

                // Маленькая искорка рядом (реже, используем CAMPFIRE_COSY_SMOKE или обычный дым)
                if (Math.random() < 0.2) {
                    double sparkX = x + (Math.random() - 0.5) * 0.05;
                    double sparkZ = z + (Math.random() - 0.5) * 0.05;
                    try {
                        // Пробуем разные варианты дыма для разных версий
                        leader.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                                new Location(currentLoc.getWorld(), sparkX, y + 0.1, sparkZ),
                                1, 0, 0, 0, 0.01);
                    } catch (Exception e) {
                        // Если нет такого particle, используем обычный
                        leader.getWorld().spawnParticle(Particle.SMOKE,
                                new Location(currentLoc.getWorld(), sparkX, y + 0.1, sparkZ),
                                1, 0, 0, 0, 0.01);
                    }
                }
            }
        }

        // Периодически показываем ауру на месте (раз в 5 секунд)
        long currentTime = System.currentTimeMillis();
        if (currentTime % 5000 < 50) {
            // Маленькая аура из огня вокруг игрока (на земле)
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double radius = 0.3;
                double x = currentLoc.getX() + radius * Math.cos(angle);
                double z = currentLoc.getZ() + radius * Math.sin(angle);
                double y = currentLoc.getY() + 0.02;

                leader.getWorld().spawnParticle(Particle.FLAME,
                        new Location(currentLoc.getWorld(), x, y, z),
                        1, 0, 0, 0, 0);
            }
        }
    }

    // Показываем эффект при входе лидера
    public void onPlayerJoin(Player player) {
        if (currentLeaders.contains(player.getUniqueId())) {
            player.sendMessage("§6══════════════════════════════");
            player.sendMessage("§e★ ВЫ ЛИДЕР РЕЙТИНГА! ★");
            player.sendMessage("§7За вами следуют огненные следы");
            player.sendMessage("§6══════════════════════════════");
        }
    }

    public void stopEffects() {
        if (effectTask != null) {
            effectTask.cancel();
        }
        currentLeaders.clear();
        lastLocations.clear();
    }
}