package com.galyakyxnya.pvpzone.listeners;

import com.galyakyxnya.pvpzone.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class ContainerProtectionListener implements Listener {
    private final Main plugin;

    public ContainerProtectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Проверяем, находится ли игрок в зоне
        if (!plugin.getZoneManager().isPlayerInZone(player)) {
            return;
        }

        // Запрещаем взаимодействие с контейнерами в зоне
        if (event.getInventory().getType() == InventoryType.CHEST ||
                event.getInventory().getType() == InventoryType.BARREL ||
                event.getInventory().getType() == InventoryType.SHULKER_BOX ||
                event.getInventory().getType() == InventoryType.HOPPER ||
                event.getInventory().getType() == InventoryType.DROPPER ||
                event.getInventory().getType() == InventoryType.DISPENSER) {

            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "✗ Нельзя использовать контейнеры в PvP зоне!");
        }

        // Проверяем, не пытается ли игрок переместить предмет из PvP набора
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType() != Material.AIR) {
            // Можно добавить проверку на предметы из PvP набора
            if (isPvpKitItem(currentItem)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "✗ Нельзя перемещать предметы PvP набора!");
            }
        }
    }

    private boolean isPvpKitItem(ItemStack item) {
        // Простая проверка - предметы с особыми названиями или зачарованиями
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            return name.contains("PvP") || name.contains("дуэль") || name.contains("арена");
        }
        return false;
    }
}