package com.galyakyxnya.pvpzone.managers;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.models.PlayerData;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderEffectManager {
    private static final long CACHE_TICKS = 60L;   // Обновлять топ раз в 3 сек
    private static final long AURA_INTERVAL_MS = 10000L;

    private final Main plugin;
    private final Set<UUID> currentLeaders = new HashSet<>();
    private final Set<UUID> effectDisabledByPlayer = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private BukkitRunnable effectTask;

    private UUID cachedLeaderId;
    private long cacheTime;
    private long lastAuraTime;

    public LeaderEffectManager(Main plugin) {
        this.plugin = plugin;
        loadEffectDisabled();
        startEffects();
    }

    private void loadEffectDisabled() {
        File f = new File(plugin.getDataFolder(), "leader_effect_disabled.yml");
        if (!f.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<String> list = cfg.getStringList("uuids");
        if (list != null) {
            for (String s : list) {
                try {
                    effectDisabledByPlayer.add(UUID.fromString(s));
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveEffectDisabled() {
        File f = new File(plugin.getDataFolder(), "leader_effect_disabled.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (UUID u : effectDisabledByPlayer) list.add(u.toString());
        cfg.set("uuids", list);
        try {
            cfg.save(f);
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось сохранить leader_effect_disabled: " + e.getMessage());
        }
    }

    public boolean isEffectDisabled(UUID playerId) {
        return effectDisabledByPlayer.contains(playerId);
    }

    public void setEffectDisabled(UUID playerId, boolean disabled) {
        if (disabled) effectDisabledByPlayer.add(playerId);
        else effectDisabledByPlayer.remove(playerId);
        saveEffectDisabled();
    }

    public boolean toggleEffectFor(Player player) {
        UUID id = player.getUniqueId();
        boolean now = !effectDisabledByPlayer.contains(id);
        setEffectDisabled(id, now);
        return now;
    }

    private void startEffects() {
        if (!isEnabledInConfig()) return;
        effectTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateLeaderEffects();
            }
        };
        effectTask.runTaskTimer(plugin, 40L, 40L); // Раз в 2 сек — меньше нагрузки
    }

    private boolean isEnabledInConfig() {
        try {
            return plugin.getConfig().getBoolean("leader-effects.enabled", true);
        } catch (Exception e) {
            return true;
        }
    }

    private void updateLeaderEffects() {
        long now = Bukkit.getCurrentTick();
        if (cachedLeaderId == null || (now - cacheTime) > CACHE_TICKS) {
            List<PlayerData> top = plugin.getPlayerDataManager().getTopPlayers(1);
            cachedLeaderId = top.isEmpty() ? null : top.get(0).getPlayerId();
            cacheTime = now;
        }

        Set<UUID> newLeaders = new HashSet<>();
        if (cachedLeaderId != null) {
            Player leader = Bukkit.getPlayer(cachedLeaderId);
            if (leader != null && leader.isOnline()) {
                newLeaders.add(cachedLeaderId);
                if (!isEffectDisabled(cachedLeaderId)) {
                    showLeaderFootsteps(leader);
                }
            }
        }

        for (UUID oldId : currentLeaders) {
            if (!newLeaders.contains(oldId)) {
                Player p = Bukkit.getPlayer(oldId);
                if (p != null && p.isOnline()) p.sendMessage(Lang.get(plugin, "leader_no_longer"));
                lastLocations.remove(oldId);
            }
        }
        currentLeaders.clear();
        currentLeaders.addAll(newLeaders);
    }

    private void showLeaderFootsteps(Player leader) {
        Location cur = leader.getLocation();
        Location last = lastLocations.get(leader.getUniqueId());
        lastLocations.put(leader.getUniqueId(), cur.clone());

        if (last != null && last.distanceSquared(cur) > 0.1) {
            int count = Math.min(2, plugin.getConfig().getInt("leader-effects.footsteps", 3));
            for (int i = 0; i < count; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 0.2;
                double x = cur.getX() + r * Math.cos(a);
                double z = cur.getZ() + r * Math.sin(a);
                double y = cur.getY() + 0.05;
                leader.getWorld().spawnParticle(Particle.FLAME,
                        new Location(cur.getWorld(), x, y, z), 1, 0, 0, 0, 0);
            }
        }

        long t = System.currentTimeMillis();
        if (t - lastAuraTime >= AURA_INTERVAL_MS) {
            lastAuraTime = t;
            for (double a = 0; a < Math.PI * 2; a += Math.PI / 6) {
                double r = 0.35;
                double x = cur.getX() + r * Math.cos(a);
                double z = cur.getZ() + r * Math.sin(a);
                double y = cur.getY() + 0.02;
                leader.getWorld().spawnParticle(Particle.FLAME,
                        new Location(cur.getWorld(), x, y, z), 1, 0, 0, 0, 0);
            }
        }
    }

    public void onPlayerJoin(Player player) {
        if (!currentLeaders.contains(player.getUniqueId())) return;
        if (isEffectDisabled(player.getUniqueId())) {
            player.sendMessage(Lang.get(plugin, "leader_effect_off_hint"));
        } else {
            player.sendMessage(Lang.get(plugin, "leader_effect_on_hint"));
        }
    }

    public void invalidateLeaderCache() {
        cachedLeaderId = null;
    }

    public void stopEffects() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
        currentLeaders.clear();
        lastLocations.clear();
    }
}
