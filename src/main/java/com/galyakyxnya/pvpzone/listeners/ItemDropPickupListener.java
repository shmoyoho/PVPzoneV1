package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemDropPickupListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Location> lastDropLocation = new HashMap<>();
    private final Set<UUID> markedItems = new HashSet<>();

    public ItemDropPickupListener(Main plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item item = event.getItemDrop();

        // ОПТИМИЗАЦИЯ: Быстрая проверка зоны через ZoneManager
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Нельзя выкидывать предметы находясь в PvP зоне!");
            return;
        }

        // Сохраняем место выброса
        lastDropLocation.put(player.getUniqueId(), item.getLocation());

        // Проверяем, выброшен ли предмет ИЗ зоны НАРУЖУ
        checkIfDroppedFromZone(player, item);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Item item = event.getItem();

        // ОПТИМИЗАЦИЯ: Быстрая проверка помеченных предметов
        if (markedItems.contains(item.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Этот предмет был выброшен из PvP зоны!");

            // Удаляем предмет через 2 тика
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (item.isValid()) {
                    item.remove();
                    markedItems.remove(item.getUniqueId());
                }
            }, 2L);
            return;
        }

        // ОПТИМИЗАЦИЯ: Быстрая проверка зоны
        if (plugin.getZoneManager().isPlayerInZone(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Нельзя подбирать предметы находясь в PvP зоне!");
        }
    }

    private void checkIfDroppedFromZone(Player player, Item item) {
        // ОПТИМИЗАЦИЯ: Получаем все зоны один раз
        var zones = plugin.getZoneManager().getAllZones();
        if (zones.isEmpty()) return;

        Location playerLocation = player.getLocation();

        // ОПТИМИЗАЦИЯ: Проверяем только зоны в том же мире
        for (var zone : zones) {
            if (!isPlayerNearZone(playerLocation, zone)) continue;

            // Помечаем предмет
            markedItems.add(item.getUniqueId());
            plugin.getLogger().info("Предмет помечен как выброшенный из зоны: " +
                    item.getItemStack().getType() + " игроком " + player.getName());

            // Удаляем через 30 секунд, если его не подобрали
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (markedItems.contains(item.getUniqueId()) && item.isValid()) {
                    item.remove();
                    markedItems.remove(item.getUniqueId());
                }
            }, 20L * 30);
            break;
        }
    }

    // ОПТИМИЗАЦИЯ: Упрощенная проверка близости к зоне
    private boolean isPlayerNearZone(Location location, com.galyakyxnya.pvpzone.managers.ZoneManager.PvpZone zone) {
        Location pos1 = zone.getPos1();
        Location pos2 = zone.getPos2();

        if (pos1 == null || pos2 == null) return false;
        if (!location.getWorld().equals(pos1.getWorld())) return false;

        double x = location.getX();
        double z = location.getZ();

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        // Проверяем с запасом в 3 блока
        return (x >= minX - 3.0 && x <= maxX + 3.0) &&
                (z >= minZ - 3.0 && z <= maxZ + 3.0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItemMonitor(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Item item = event.getItemDrop();

        // ОПТИМИЗАЦИЯ: Проверяем траекторию через 1 тик только для важных случаев
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!item.isValid()) return;

            Location itemLocation = item.getLocation();

            // Проверяем, вылетел ли предмет из зоны
            if (plugin.getZoneManager().findZoneAtLocation(itemLocation) == null) {
                Location playerLocationAtDrop = lastDropLocation.get(player.getUniqueId());
                if (playerLocationAtDrop != null &&
                        plugin.getZoneManager().findZoneAtLocation(playerLocationAtDrop) != null) {

                    markedItems.add(item.getUniqueId());
                    plugin.getLogger().info("Обнаружен выброс предмета из зоны наружу: " +
                            player.getName() + " -> " + item.getItemStack().getType());
                }
            }
        }, 1L);
    }

    // ОПТИМИЗАЦИЯ: Упрощенная очистка
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Очищаем устаревшие записи о местах выброса (старше 1 минуты)
            Iterator<Map.Entry<UUID, Location>> iterator = lastDropLocation.entrySet().iterator();
            while (iterator.hasNext()) {
                iterator.next();
                // ОПТИМИЗАЦИЯ: Простая очистка старых записей
                iterator.remove(); // Удаляем все старые записи
                break; // Удаляем по одной за выполнение
            }

            // Очищаем помеченные предметы, которые уже удалены
            Iterator<UUID> itemIterator = markedItems.iterator();
            while (itemIterator.hasNext()) {
                UUID itemId = itemIterator.next();
                Entity entity = plugin.getServer().getEntity(itemId);
                if (entity == null || !entity.isValid()) {
                    itemIterator.remove();
                }
            }
        }, 20L * 60, 20L * 60); // Каждую минуту вместо 5 минут
    }
}