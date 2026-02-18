package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import com.galyakyxnya.pvpzone.managers.ZoneManager;
import com.galyakyxnya.pvpzone.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemDropPickupListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Location> lastDropLocation = new ConcurrentHashMap<>();
    private final Set<UUID> markedItems = ConcurrentHashMap.newKeySet();
    private static final int MAX_DROP_LOCATIONS = 200;
    private static final long CLEANUP_INTERVAL_TICKS = 20L * 120; // 2 мин

    public ItemDropPickupListener(Main plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItemDrop();

        if (plugin.getZoneManager().isPlayerInZone(player)) {
            event.setCancelled(true);
            player.sendMessage(Lang.get(plugin, "drop_cannot_drop"));
            return;
        }

        if (lastDropLocation.size() < MAX_DROP_LOCATIONS) {
            lastDropLocation.put(player.getUniqueId(), item.getLocation());
        }
        checkIfDroppedFromZone(player, item);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Item item = event.getItem();

        if (markedItems.contains(item.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Lang.get(plugin, "drop_cannot_pickup_marked"));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (item.isValid()) {
                    item.remove();
                    markedItems.remove(item.getUniqueId());
                }
            }, 2L);
            return;
        }

        if (plugin.getZoneManager().isPlayerInZone(player)) {
            event.setCancelled(true);
            player.sendMessage(Lang.get(plugin, "drop_cannot_pickup"));
        }
    }

    private void checkIfDroppedFromZone(Player player, Item item) {
        var zones = plugin.getZoneManager().getAllZones();
        if (zones.isEmpty()) return;
        Location pl = player.getLocation();
        for (ZoneManager.PvpZone zone : zones) {
            if (!isNearZone(pl, zone)) continue;
            markedItems.add(item.getUniqueId());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (markedItems.contains(item.getUniqueId()) && item.isValid()) {
                    item.remove();
                    markedItems.remove(item.getUniqueId());
                }
            }, 20L * 30);
            break;
        }
    }

    private static boolean isNearZone(Location loc, ZoneManager.PvpZone zone) {
        Location p1 = zone.getPos1();
        Location p2 = zone.getPos2();
        if (p1 == null || p2 == null || !p1.getWorld().equals(loc.getWorld())) return false;
        double x = loc.getX(), z = loc.getZ();
        double minX = Math.min(p1.getX(), p2.getX()) - 3;
        double maxX = Math.max(p1.getX(), p2.getX()) + 3;
        double minZ = Math.min(p1.getZ(), p2.getZ()) - 3;
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 3;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItemMonitor(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Item item = event.getItemDrop();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!item.isValid()) return;
            if (plugin.getZoneManager().findZoneAtLocation(item.getLocation()) != null) return;
            Location atDrop = lastDropLocation.get(player.getUniqueId());
            if (atDrop != null && plugin.getZoneManager().findZoneAtLocation(atDrop) != null) {
                markedItems.add(item.getUniqueId());
            }
        }, 1L);
    }

    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Iterator<UUID> it = lastDropLocation.keySet().iterator(); it.hasNext(); ) {
                if (Bukkit.getPlayer(it.next()) == null) it.remove();
            }
            for (Iterator<UUID> it = markedItems.iterator(); it.hasNext(); ) {
                Entity e = plugin.getServer().getEntity(it.next());
                if (e == null || !e.isValid()) it.remove();
            }
        }, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
    }
}
